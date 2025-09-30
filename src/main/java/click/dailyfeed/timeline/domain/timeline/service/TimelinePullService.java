package click.dailyfeed.timeline.domain.timeline.service;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.timeline.timeline.dto.TimelineDto;
import click.dailyfeed.code.global.cache.RedisKeyConstant;
import click.dailyfeed.code.global.web.page.DailyfeedPage;
import click.dailyfeed.code.global.web.page.DailyfeedScrollPage;
import click.dailyfeed.feign.domain.member.MemberFeignHelper;
import click.dailyfeed.feign.domain.post.PostFeignHelper;
import click.dailyfeed.pagination.mapper.PageMapper;
import click.dailyfeed.timeline.domain.post.document.PostActivity;
import click.dailyfeed.timeline.domain.post.entity.Post;
import click.dailyfeed.timeline.domain.post.mapper.TimelinePostMapper;
import click.dailyfeed.timeline.domain.post.repository.jpa.PostRepository;
import click.dailyfeed.timeline.domain.post.repository.mongo.PostActivityMongoRepository;
import click.dailyfeed.timeline.domain.timeline.redis.TimelinePostActivityRedisService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
@RequiredArgsConstructor
@Service
public class TimelinePullService {
    private final PostActivityMongoRepository postActivityMongoRepository;
    private final PostRepository postRepository;
    private final MemberFeignHelper memberFeignHelper;
    private final PostFeignHelper postFeignHelper;
    private final TimelinePostActivityRedisService timelinePostActivityRedisService;

    private final PageMapper pageMapper;
    private final TimelinePostMapper timelinePostMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.TimelinePullService.WEB_GET_TIMELINE_ITEMS_DEFAULT, key="#userId + '_' + #page + '_' + #size + '_' + #hours", unless = "#result.isEmpty()")
    public List<TimelineDto.TimelinePostActivity> listMyFollowingActivities(Long userId, int page, int size, int hours, String token, HttpServletResponse httpResponse) {
        List<MemberProfileDto.Summary> followingMembers = fetchMyFollowingMembers(token, httpResponse); /// 여기서 MemberDto.Summary 또는 FollowDto.Following 으로 들고오면, 뒤에서 MemberMap API 로 구할 필요가 없다.
        Map<Long, MemberProfileDto.Summary> followingsMap = followingMembers.stream().collect(Collectors.toMap(s -> s.getMemberId(), s -> s));

        if (followingsMap.isEmpty()) {
            return List.of();
        }

        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        Pageable pageable = PageRequest.of(page, size);

        Page<PostActivity> activities = postActivityMongoRepository.findFollowingActivitiesWhereFollowingIdsIn(followingsMap.keySet(), since, pageable);
        if (activities.isEmpty()) {
            return List.of();
        }

        ///  get Post Map (id = PostId)
        Set<Long> postIds = activities.getContent().stream().map(PostActivity::getPostId).collect(Collectors.toSet());
        PostDto.PostsBulkRequest request = PostDto.PostsBulkRequest.builder().ids(postIds).build();
        Map<Long, PostDto.Post> postMap = postFeignHelper.getPostMap(request, token, httpResponse);

        return activities.stream()
                .map(activity -> {
                    final PostDto.Post p = postMap.get(activity.getPostId());
                    MemberProfileDto.Summary author = followingsMap.get(p.getAuthorId());

                    if (p == null) { // 애플리케이션 재기동시 MySQL 날라갔을때 증상
                        // Return a minimal activity object when post is not found

                        return TimelineDto.TimelinePostActivity
                                .builder()
                                .id(activity.getId().toString())
                                .postId(activity.getPostId())
                                .authorId(activity.getMemberId())
                                .authorName(author != null ? author.getDisplayName() : "Unknown")
                                .authorHandle(author != null ? author.getMemberHandle() : "unknown")
                                .authorAvatarUrl(author != null ? author.getAvatarUrl() : null)
                                .activityType(activity.getPostActivityType().getActivityName())
                                .createdAt(activity.getCreatedAt())
                                .title("[Post not found]")
                                .content("")
                                .build();
                    }
                    return TimelineDto.TimelinePostActivity
                            .builder()
                            .id(activity.getId().toString())
                            .postId(activity.getPostId())
                            .authorId(activity.getMemberId())
                            .authorName(author != null ? author.getDisplayName() : "Unknown")
                            .authorHandle(author != null ? author.getMemberHandle() : "unknown")
                            .authorAvatarUrl(author != null ? author.getAvatarUrl() : null)
                            .activityType(activity.getPostActivityType().getActivityName())
                            .createdAt(activity.getCreatedAt())
                            .title(p.getTitle())
                            .content(p.getContent())
                            .build();
                }).toList();
    }

    public List<MemberProfileDto.Summary> fetchMyFollowingMembers(String token, HttpServletResponse httpResponse) {
        return memberFeignHelper.getMyFollowingMembers(token, httpResponse);
    }

    public List<TimelineDto.TimelinePostActivity> listHeavyMyFollowingActivities(MemberProfileDto.MemberProfile member, Pageable pageable, String token, HttpServletResponse httpServletResponse) {
        final String key = "heavy_following_feed:" + member.getId() + ":" + pageable.getPageNumber() + ":" + pageable.getPageSize();

        List<TimelineDto.TimelinePostActivity> cached = timelinePostActivityRedisService.getList(key, pageable.getPageNumber(), pageable.getPageSize());

        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        if (member.getFollowingsCount() < 10000){ // following 이 2000 명 이하면 일단은 그래도 캐시를 적용했으니 그냥 pull
            return listMyFollowingActivities(member.getId(), pageable.getPageNumber(), pageable.getPageSize(), 24, token, httpServletResponse);
        }
        else{ // 10000 명 이상이면 super heavy 로 판정 (팔로잉을 10000명 이상 한다는 것은 비정상 유저일수도 있고, 인플루언서의 인맥이 넓을 경우 등 일수도 있지만, 호날두는 605명... ㅋㅋ 😆😆)
            return listSuperHeavyFollowingActivities(member, pageable, token, httpServletResponse);
        }
    }

    // TODO (SEASON2)
    private List<TimelineDto.TimelinePostActivity> listSuperHeavyFollowingActivities(
            MemberProfileDto.MemberProfile member,
            Pageable pageable,
            String token,
            HttpServletResponse httpResponse) {
        List<MemberProfileDto.Summary> members = fetchMyFollowingMembers(token, httpResponse);

        // 최근 3일간 활동한 팔로잉 사용자만 필터링
        LocalDateTime since = LocalDateTime.now().minusDays(3);

        // 각 팔로잉 사용자별로 최근 활동 확인
//        List<Long> activeFollowingIds = followingIds.parallelStream()
//                .filter(followingId -> hasRecentActivity(followingId, since))
//                .limit(50) // 최대 50명만
//                .toList();

        return null;
    }

    // 댓글이 많은 게시글 목록
    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.TimelinePullService.WEB_SEARCH_TIMELINE_ORDER_BY_COMMENT_COUNT_DESC, key = "'__page:'+#page+'_size:'+#size", cacheManager = "redisCacheManager")
    public DailyfeedScrollPage<PostDto.Post> getPostsOrderByCommentCount(Pageable pageable, String token, HttpServletResponse httpResponse) {
        Page<Post> page = postRepository.findMostCommentedPosts(pageable);
        List<PostDto.Post> result = mergeAuthorAndCommentCount(page.getContent(), token, httpResponse);
        return pageMapper.fromJpaPageToDailyfeedScrollPage(page, result);
    }

    // 인기 글 목록
    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.TimelinePullService.WEB_SEARCH_TIMELINE_ORDER_BY_POPULAR_DESC, key = "'__page:'+#page+'_size:'+#size", cacheManager = "redisCacheManager")
    public DailyfeedScrollPage<PostDto.Post> getPopularPosts(Pageable pageable, String token, HttpServletResponse httpResponse) {
        Page<Post> page = postRepository.findPopularPostsNotDeleted(pageable);
        List<PostDto.Post> result = mergeAuthorAndCommentCount(page.getContent(), token, httpResponse);
        return pageMapper.fromJpaPageToDailyfeedScrollPage(page, result);
    }

    // 최근 활동이 있는 글 조회
    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.TimelinePullService.WEB_SEARCH_TIMELINE_RECENT_ACTIVITY_DESC, key = "'__page:'+#page+'_size:'+#size", cacheManager = "redisCacheManager")
    public DailyfeedScrollPage<PostDto.Post> getPostsByRecentActivity(Pageable pageable, String token, HttpServletResponse httpResponse) {
        Page<Post> page = postRepository.findPostsByRecentActivity(pageable);
        List<PostDto.Post> result = mergeAuthorAndCommentCount(page.getContent(), token, httpResponse);
        return pageMapper.fromJpaPageToDailyfeedScrollPage(page, result);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.TimelinePullService.WEB_SEARCH_TIMELINE_DATE_RANGE, key = "'__page:'+#page+'_size:'+#size", cacheManager = "redisCacheManager")
    public DailyfeedPage<PostDto.Post> getPostsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable, String token, HttpServletResponse httpResponse) {
        Page<Post> posts = postRepository.findByCreatedDateBetweenAndNotDeleted(startDate, endDate, pageable);
        return pageMapper.fromJpaPageToDailyfeedPage(posts, mergeAuthorAndCommentCount(posts.getContent(), token, httpResponse));
    }

    public List<PostDto.Post> mergeAuthorAndCommentCount(List<Post> posts, String token, HttpServletResponse httpResponse){
        // (1) 작성자 id 추출
        Set<Long> authorIds = posts.stream()
                .map(Post::getAuthorId)
                .collect(Collectors.toSet());

        // (2) 작성자 상세 정보
        Map<Long, MemberProfileDto.Summary> authorsMap = memberFeignHelper.getMemberMap(authorIds, token, httpResponse);

        return posts.stream()
                .map(post -> {
                    return timelinePostMapper.toPostDto(post, authorsMap.get(post.getAuthorId()), Long.parseLong(String.valueOf(post.getCommentsCount())));
                })
                .collect(Collectors.toList());
    }
}
