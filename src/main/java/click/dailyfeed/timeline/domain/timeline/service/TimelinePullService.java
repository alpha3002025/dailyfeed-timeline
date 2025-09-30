package click.dailyfeed.timeline.domain.timeline.service;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.content.comment.exception.CommentNotFoundException;
import click.dailyfeed.code.domain.content.comment.exception.ParentCommentNotFoundException;
import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.content.post.exception.PostNotFoundException;
import click.dailyfeed.code.domain.member.member.code.MemberExceptionCode;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.member.member.exception.MemberException;
import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.code.domain.timeline.timeline.dto.TimelineDto;
import click.dailyfeed.code.global.cache.RedisKeyConstant;
import click.dailyfeed.code.global.web.page.DailyfeedPage;
import click.dailyfeed.code.global.web.page.DailyfeedScrollPage;
import click.dailyfeed.feign.domain.member.MemberFeignHelper;
import click.dailyfeed.feign.domain.post.PostFeignHelper;
import click.dailyfeed.pagination.mapper.PageMapper;
import click.dailyfeed.timeline.domain.comment.entity.Comment;
import click.dailyfeed.timeline.domain.comment.projection.PostCommentCountProjection;
import click.dailyfeed.timeline.domain.comment.repository.jpa.CommentRepository;
import click.dailyfeed.timeline.domain.comment.repository.mongo.CommentMongoAggregation;
import click.dailyfeed.timeline.domain.comment.repository.mongo.CommentMongoRepository;
import click.dailyfeed.timeline.domain.post.document.PostActivity;
import click.dailyfeed.timeline.domain.post.entity.Post;
import click.dailyfeed.timeline.domain.post.mapper.TimelinePostMapper;
import click.dailyfeed.timeline.domain.post.repository.jpa.PostRepository;
import click.dailyfeed.timeline.domain.post.repository.mongo.PostActivityMongoRepository;
import click.dailyfeed.timeline.domain.post.repository.mongo.PostLikeMongoRepository;
import click.dailyfeed.timeline.domain.timeline.mapper.TimelineMapper;
import click.dailyfeed.timeline.domain.timeline.redis.TimelinePostActivityRedisService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class TimelinePullService {
    private final PostActivityMongoRepository postActivityMongoRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeMongoRepository postLikeMongoRepository;
    private final CommentMongoRepository commentMongoRepository;

    private final CommentMongoAggregation commentMongoAggregation;

    private final MemberFeignHelper memberFeignHelper;
    private final PostFeignHelper postFeignHelper;
    private final TimelinePostActivityRedisService timelinePostActivityRedisService;

    private final PageMapper pageMapper;
    private final TimelinePostMapper timelinePostMapper;
    private final TimelineMapper timelineMapper;

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
//        Map<Long, PostDto.Post> postMap = postFeignHelper.getPostMap(request, token, httpResponse);
        Map<Long, PostDto.Post> postMap = getPostListByIdsIn(request, token, httpResponse).stream().collect(Collectors.toMap(p -> p.getId(), p -> p));

        return activities.stream()
                .map(activity -> {
                    final PostDto.Post p = postMap.get(activity.getPostId());
                    MemberProfileDto.Summary author = followingsMap.get(p.getAuthorId());

                    if (p == null) { // 애플리케이션 재기동시 MySQL 날라갔을때 증상
                        // Return a minimal activity object when post is not found

                        return TimelineDto.TimelinePostActivity
                                .builder()
                                .id(activity.getPostId())
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
                            .id(activity.getPostId())
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
        List<PostCommentCountProjection> statisticResult = commentMongoAggregation.findTopPostsByCommentCount(pageable);
        Set<Long> postPks = statisticResult.stream().map(p -> p.getPostPk()).collect(Collectors.toSet());

        Map<Long, PostDto.Post> postMap = getPostListByIdsIn(PostDto.PostsBulkRequest.builder().ids(postPks).build(), token, httpResponse)
                .stream().collect(Collectors.toMap(p -> p.getId(), p -> p));

        List<PostDto.Post> result = statisticResult.stream()
                .map(projection -> {
                    PostDto.Post post = postMap.get(projection.getPostPk());
                    return PostDto.Post.builder()
                            .commentCount(projection.getCommentCount())
                            .viewCount(post != null ? post.getViewCount() : 0)
                            .likeCount(post != null ? post.getLikeCount() : 0)
                            .id(post != null ? post.getId() : null)
                            .authorId(post != null ? post.getAuthorId() : null)
                            .authorName(post != null ? post.getAuthorName() : "Unknown")
                            .authorHandle(post != null ? post.getAuthorHandle() : "unknown")
                            .authorAvatarUrl(post != null ? post.getAuthorAvatarUrl() : null)
                            .content(post.getContent())
                            .createdAt(post.getCreatedAt())
                            .updatedAt(post.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return DailyfeedScrollPage.<PostDto.Post>builder()
                .content(result)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
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

    @Cacheable(value = RedisKeyConstant.PostService.INTERNAL_LIST_GET_POST_LIST_BY_IDS_IN, keyGenerator = "postIdsKeyGenerator", cacheManager = "redisCacheManager")
    public List<PostDto.Post> getPostListByIdsIn(PostDto.PostsBulkRequest request, String token, HttpServletResponse httpResponse) {
        // Set이 비어있는 경우 빈 리스트 반환
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return List.of();  // 빈 List 반환
        }

        List<Post> result = postRepository.findPostsByIdsInNotDeletedOrderByCreatedDateDesc(request.getIds());
        Set<Long> authorIds = result.stream().map(p -> p.getAuthorId()).collect(Collectors.toSet());

        Map<Long, MemberProfileDto.Summary> authorMap = memberFeignHelper
                .getMembersList(authorIds, token, httpResponse)
                .stream()
                .collect(Collectors.toMap(s -> s.getMemberId(), s -> s));

        PostStatisticsInternal statistics = queryPostStatistics(request);
        Map<Long, PostDto.PostLikeCountStatistics> postLikeCountStatisticsMap = statistics.postLikeCountStatisticsMap();
        Map<Long, PostDto.PostCommentCountStatistics> commentCountStatisticsMap = statistics.commentCountStatisticsMap();

        return result.stream().map(post -> timelineMapper.toPostDto(post, authorMap.get(post.getAuthorId()), postLikeCountStatisticsMap.get(post.getId()), commentCountStatisticsMap.get(post.getId()))).toList();
    }

    public DailyfeedPage<PostDto.Post> getMyPosts(MemberDto.Member requestedMember, Pageable pageable, String token, HttpServletResponse httpResponse) {
        Page<Post> page = postRepository.findByAuthorIdAndNotDeleted(requestedMember.getId(), pageable);
        Set<Long> postPks = page.getContent().stream().map(Post::getId).collect(Collectors.toSet());

        MemberProfileDto.Summary memberSummary = memberFeignHelper.getMemberSummaryById(requestedMember.getId(), token, httpResponse);

        PostStatisticsInternal statistics = queryPostStatistics(PostDto.PostsBulkRequest.builder().ids(postPks).build());
        Map<Long, PostDto.PostLikeCountStatistics> postLikeCountStatisticsMap = statistics.postLikeCountStatisticsMap();
        Map<Long, PostDto.PostCommentCountStatistics> commentCountStatisticsMap = statistics.commentCountStatisticsMap();

        List<PostDto.Post> content = page.getContent().stream().map(p -> timelineMapper.toPostDto(p, memberSummary, postLikeCountStatisticsMap.get(p.getId()), commentCountStatisticsMap.get(p.getId()))).toList();
        return pageMapper.fromJpaPageToDailyfeedPage(page, content);
    }

    @Cacheable(value = RedisKeyConstant.PostService.WEB_GET_POST_BY_ID, key = "#postId", cacheManager = "redisCacheManager")
    public PostDto.Post getPostById(MemberDto.Member member, Long postId, String token, HttpServletResponse httpResponse) {
        Post post = postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(PostNotFoundException::new);

        // 조회수 증가 (카프카 기반으로 변경 예정 todo)
        post.incrementViewCount();

        // 작성자 정보 조회
        MemberProfileDto.Summary authorSummary = memberFeignHelper.getMemberSummaryById(post.getAuthorId(), token, httpResponse);

        PostStatisticsInternal statistics = queryPostStatistics(PostDto.PostsBulkRequest.builder().ids(Set.of(postId)).build());
        Map<Long, PostDto.PostLikeCountStatistics> postLikeCountStatisticsMap = statistics.postLikeCountStatisticsMap();
        Map<Long, PostDto.PostCommentCountStatistics> commentCountStatisticsMap = statistics.commentCountStatisticsMap();

        return timelineMapper.toPostDto(post, authorSummary, postLikeCountStatisticsMap.get(postId), commentCountStatisticsMap.get(postId));
    }

    public DailyfeedPage<PostDto.Post> getPostsByAuthor(Long authorId, Pageable pageable, String token, HttpServletResponse httpResponse) {
        MemberDto.Member author = memberFeignHelper.getMemberById(authorId, token, httpResponse);
        if (author == null) {
            throw new MemberNotFoundException(() -> "삭제된 사용자입니다");
        }

        Page<Post> posts = postRepository.findByAuthorIdAndNotDeleted(author.getId(), pageable);
        return pageMapper.fromJpaPageToDailyfeedPage(posts, mergeAuthorAndCommentCount(posts.getContent(), token, httpResponse));
    }

    /// helpers (with transactional)
    /// 글 조회
    public Post getPostByIdOrThrow(Long postId) {
        return postRepository.findByIdAndNotDeleted(postId).orElseThrow(click.dailyfeed.code.domain.content.comment.exception.PostNotFoundException::new);
    }

    public PostStatisticsInternal queryPostStatistics(PostDto.PostsBulkRequest request){
        Map<Long, PostDto.PostLikeCountStatistics> postLikeCountStatisticsMap = postLikeMongoRepository.countLikesByPostPks(request.getIds())
                .stream().map(timelineMapper::toPostLikeStatistics)
                .collect(Collectors.toMap(o -> o.getPostPk(), o -> o));

        Map<Long, PostDto.PostCommentCountStatistics> commentCountStatisticsMap = commentMongoAggregation.countCommentsByPostPks(request.getIds())
                .stream().map(timelineMapper::toPostCommentCountStatistics)
                .collect(Collectors.toMap(o -> o.getPostPk(), o -> o));

        return new PostStatisticsInternal(postLikeCountStatisticsMap, commentCountStatisticsMap);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.CommentService.WEB_GET_COMMENTS_BY_MEMBER_ID, key = "'memberId_'+#memberId+'_page_'+#page+'_size_'+#size")
    public DailyfeedPage<CommentDto.CommentSummary> getMyComments(Long memberId, Pageable pageable, String token, HttpServletResponse httpResponse) {
        Page<Comment> comments = commentRepository.findByAuthorIdAndNotDeleted(memberId, pageable);
        return mergeAuthorData(comments, token, httpResponse);
    }

    public DailyfeedPage<CommentDto.Comment> getCommentsByPostWithPaging(Long postId, Pageable pageable, String token, HttpServletResponse httpResponse) {
        Post post = getPostByIdOrThrow(postId);
        Page<Comment> comments = commentRepository.findTopLevelCommentsByPostWithPaging(post, pageable);
        return mergeAuthorDataRecursively(comments, token, httpResponse);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.CommentService.WEB_GET_COMMENTS_BY_MEMBER_ID, key = "'memberId_'+#memberId+'_page_'+#pageable.getPageNumber() +'_size_'+#pageable.getPageSize()")
    public DailyfeedPage<CommentDto.CommentSummary> getCommentsByUser(Long memberId, Pageable pageable, String token, HttpServletResponse httpResponse) {
        Page<Comment> comments = commentRepository.findByAuthorIdAndNotDeleted(memberId, pageable);
        return mergeAuthorData(comments, token, httpResponse);
    }

    // 댓글 상세 조회
    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.CommentService.WEB_GET_COMMENT_BY_ID, key = "#commentId")
    public CommentDto.Comment getCommentById(Long commentId, String token, HttpServletResponse httpResponse) {
        Comment comment = commentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(CommentNotFoundException::new);

        CommentDto.Comment commentDto = timelineMapper.toCommentNonRecursive(comment);
        mergeAuthorAtCommentList(List.of(commentDto), token, httpResponse);
        return commentDto;
    }

    // 대댓글 목록 조회
    @Transactional(readOnly = true)
    @Cacheable(value = RedisKeyConstant.CommentService.WEB_GET_COMMENTS_BY_PARENT_ID, key = "'parentId_'+#parentId+'_page_'+#page+'_size_'+#size")
    public DailyfeedPage<CommentDto.Comment> getRepliesByParent(Long parentId, Pageable pageable, String token, HttpServletResponse httpResponse) {
        Comment parentComment = commentRepository.findByIdAndNotDeleted(parentId)
                .orElseThrow(ParentCommentNotFoundException::new);

        Page<Comment> replies = commentRepository.findChildrenByParentWithPaging(parentComment, pageable);

        List<CommentDto.Comment> commentList = replies.getContent().stream()
                .map(timelineMapper::toCommentNonRecursive)
                .collect(Collectors.toList());

        mergeAuthorAtCommentList(commentList, token, httpResponse);

        return pageMapper.fromJpaPageToDailyfeedPage(replies, commentList);
    }

    private record PostStatisticsInternal(
            Map<Long, PostDto.PostLikeCountStatistics> postLikeCountStatisticsMap,
            Map<Long, PostDto.PostCommentCountStatistics> commentCountStatisticsMap
    ){}

    /// helpers (internal)
    public DailyfeedPage<CommentDto.CommentSummary> mergeAuthorData(Page<Comment> commentsPage, String token, HttpServletResponse httpResponse) {
        List<CommentDto.CommentSummary> summaries = commentsPage.getContent().stream().map(timelineMapper::toCommentSummary).collect(Collectors.toList());
        if (summaries.isEmpty()) return pageMapper.emptyPage();

        Set<Long> authorIds = summaries.stream()
                .map(CommentDto.CommentSummary::getAuthorId)
                .collect(Collectors.toSet());

        Map<Long, MemberProfileDto.Summary> authorMap = memberFeignHelper.getMemberMap(authorIds, token, httpResponse);

        summaries.forEach(summary -> {
            MemberProfileDto.Summary author = authorMap.get(summary.getAuthorId());
            if (author != null) {
                summary.updateAuthor(author);
            }
        });

        return pageMapper.fromJpaPageToDailyfeedPage(commentsPage, summaries);
    }

    // 계층구조 댓글에 작성자 정보 추가 (재귀적)
    private DailyfeedPage<CommentDto.Comment> mergeAuthorDataRecursively(Page<Comment> commentsPage, String token, HttpServletResponse httpResponse) {
        if(commentsPage.isEmpty()) return pageMapper.emptyPage();

        List<CommentDto.Comment> commentList = commentsPage.getContent().stream().map(timelineMapper::toCommentNonRecursive).collect(Collectors.toList());

        Set<Long> authorIds = commentList.stream()
                .map(CommentDto.Comment::getAuthorId)
                .collect(Collectors.toSet());

        Map<Long, MemberProfileDto.Summary> authorMap = memberFeignHelper.getMemberMap(authorIds, token, httpResponse);

        commentList.forEach(comment -> {
            MemberProfileDto.Summary author = authorMap.get(comment.getAuthorId());
            if (author != null) {
                comment.updateAuthorRecursively(authorMap);
            }
        });

        return pageMapper.fromJpaPageToDailyfeedPage(commentsPage, commentList);
    }

    private void mergeAuthorAtCommentList(List<CommentDto.Comment> comments, String token, HttpServletResponse httpResponse){
        if (comments.isEmpty()) return;

        Set<Long> authorIds = comments.stream()
                .map(CommentDto.Comment::getAuthorId)
                .collect(Collectors.toSet());

        try {
            Map<Long, MemberProfileDto.Summary> authorMap = memberFeignHelper.getMemberMap(authorIds, token, httpResponse);

            comments.forEach(comment -> {
                MemberProfileDto.Summary author = authorMap.get(comment.getAuthorId());
                if (author != null) {
                    comment.updateAuthor(author);
                }
            });
        } catch (Exception e) {
            log.warn("Failed to fetch author info: {}", e.getMessage());
            throw new MemberException(MemberExceptionCode.MEMBER_API_CONNECTION_ERROR);
        }
    }
}
