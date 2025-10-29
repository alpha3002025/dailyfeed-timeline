package click.dailyfeed.timeline.domain.timeline.service;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.content.comment.exception.CommentNotFoundException;
import click.dailyfeed.code.domain.content.comment.exception.ParentCommentNotFoundException;
import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.content.post.exception.PostNotFoundException;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.code.global.web.page.DailyfeedPage;
import click.dailyfeed.code.global.web.page.DailyfeedScrollPage;
import click.dailyfeed.feign.domain.member.MemberFeignHelper;
import click.dailyfeed.kafka.domain.activity.publisher.MemberActivityKafkaPublisher;
import click.dailyfeed.pagination.mapper.PageMapper;
import click.dailyfeed.timeline.domain.comment.entity.Comment;
import click.dailyfeed.timeline.domain.comment.projection.CommentLikeCountProjection;
import click.dailyfeed.timeline.domain.comment.projection.PostCommentCountProjection;
import click.dailyfeed.timeline.domain.comment.repository.jpa.CommentRepository;
import click.dailyfeed.timeline.domain.comment.repository.mongo.CommentLikeMongoAggregation;
import click.dailyfeed.timeline.domain.comment.repository.mongo.CommentLikeMongoRepository;
import click.dailyfeed.timeline.domain.comment.repository.mongo.CommentMongoAggregation;
import click.dailyfeed.timeline.domain.post.entity.Post;
import click.dailyfeed.timeline.domain.post.mapper.TimelinePostMapper;
import click.dailyfeed.timeline.domain.post.repository.jpa.PostRepository;
import click.dailyfeed.timeline.domain.post.repository.mongo.PostLikeMongoAggregation;
import click.dailyfeed.timeline.domain.post.repository.mongo.PostLikeMongoRepository;
import click.dailyfeed.timeline.domain.timeline.mapper.TimelineMapper;
import click.dailyfeed.timeline.domain.timeline.redis.TimelinePostsApiRedisService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeMongoRepository postLikeMongoRepository;

    private final CommentMongoAggregation commentMongoAggregation;
    private final CommentLikeMongoAggregation commentLikeMongoAggregation;
    private final PostLikeMongoAggregation postLikeMongoAggregation;

    private final MemberActivityKafkaPublisher memberActivityKafkaPublisher;
    private final MemberFeignHelper memberFeignHelper;
    private final TimelinePostsApiRedisService timelinePostsApiRedisService;

    private final PageMapper pageMapper;
    private final TimelinePostMapper timelinePostMapper;
    private final TimelineMapper timelineMapper;
    private final CommentLikeMongoRepository commentLikeMongoRepository;

    @Transactional(readOnly = true)
    public List<PostDto.Post> listMyFollowingActivities(Long memberId, int page, int size, String token, HttpServletResponse httpResponse) {
        List<MemberProfileDto.Summary> followingMembers = fetchMyFollowingMembers(token, httpResponse);
        Map<Long, MemberProfileDto.Summary> followingsMap = followingMembers.stream().collect(Collectors.toMap(s -> s.getMemberId(), s -> s));

        if (followingsMap.isEmpty()) {
            return List.of();
        }

        /// DB 조회 (size개 조회 - hasNext는 상위에서 판단)
        Pageable pageable = PageRequest.of(page, size);
        List<Post> posts = postRepository.findPostsByAuthorIdInAndNotDeletedOrderByCreatedDateDesc(followingsMap.keySet(), pageable);

        /// 통계정보 추출, 병합
        return withAuthorsAndStatistics(memberId, posts, token, httpResponse)
                .stream()
                .map(p -> {
                    MemberProfileDto.Summary author = followingsMap.get(p.getAuthorId());
                    return timelineMapper.toPostDto(p, p.getLiked(),author);
                })
                .collect(Collectors.toList());
    }

    public List<MemberProfileDto.Summary> fetchMyFollowingMembers(String token, HttpServletResponse httpResponse) {
        return memberFeignHelper.getMyFollowingMembers(token, httpResponse);
    }

    public List<PostDto.Post> listHeavyMyFollowingActivities(MemberProfileDto.MemberProfile member, int page, int size, String token, HttpServletResponse httpServletResponse) {
        final String key = "heavy_following_feed:" + member.getId() + ":" + page + ":" + size;

        List<PostDto.Post> cached = timelinePostsApiRedisService.getList(key, page, size);

        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        if (member.getFollowingsCount() < 10000){
            return listMyFollowingActivities(member.getId(), page, size, token, httpServletResponse);
        }
        else{
            Pageable pageable = PageRequest.of(page, size);
            return listSuperHeavyFollowingActivities(member, pageable, token, httpServletResponse);
        }
    }

    // TODO (SEASON2)
    private List<PostDto.Post> listSuperHeavyFollowingActivities(
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

    // 댓글이 많은 게시글 목록 (Slice 방식)
    @Transactional(readOnly = true)
    public List<PostDto.Post> getPostsOrderByCommentCount(Long memberId, int page, int size, String token, HttpServletResponse httpResponse) {
        // 댓글 많은 순 데이터 (size개 조회 - hasNext는 상위에서 판단)
        Pageable pageable = PageRequest.of(page, size);
        List<PostCommentCountProjection> statisticResult = commentMongoAggregation.findTopPostsByCommentCount(pageable);

        // 글 post id 키값 추출
        Set<Long> postPks = statisticResult.stream().map(p -> p.getPostPk()).collect(Collectors.toSet());

        // postMap
        List<Post> posts = postRepository.findPostsByIdsInNotDeletedOrderByCreatedDateDesc(postPks);
        Map<Long, PostDto.Post> postMap = withAuthorsAndStatistics(memberId, posts, token, httpResponse)
                .stream()
                .collect(Collectors.toMap(p -> p.getId(), p -> p));

        // 변환
        return statisticResult.stream()
                .map(projection -> {
                    PostDto.Post post = postMap.get(projection.getPostPk());
                    return timelineMapper.toPostDtoWithCountProjection(post, projection);
                })
                .collect(Collectors.toList());
    }

    // 인기 글 목록 (Slice 방식)
    @Transactional(readOnly = true)
    public List<PostDto.Post> getPopularPosts(Long requestedMemberId, int page, int size, String token, HttpServletResponse httpResponse) {
        Pageable pageable = PageRequest.of(page, size);
        Slice<Post> slice = postRepository.findPopularPostsNotDeleted(pageable);
        return withAuthorsAndStatistics(requestedMemberId, slice.getContent(), token, httpResponse);
    }

    // 최근 활동이 있는 글 조회 (Slice 방식)
    @Transactional(readOnly = true)
    public List<PostDto.Post> getPostsByRecentActivities(Long requestedMemberId, int page, int size, String token, HttpServletResponse httpResponse) {
        Pageable pageable = PageRequest.of(page, size);
        Slice<Post> slice = postRepository.findPostsByRecentActivities(pageable);
        return withAuthorsAndStatistics(requestedMemberId, slice.getContent(), token, httpResponse);
    }

    @Transactional(readOnly = true)
    public DailyfeedPage<PostDto.Post> getPostsByDateRange(Long requestedMemberId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable, String token, HttpServletResponse httpResponse) {
        Page<Post> page = postRepository.findByCreatedDateBetweenAndNotDeleted(startDate, endDate, pageable);
        List<PostDto.Post> result = withAuthorsAndStatistics(requestedMemberId, page.getContent(), token, httpResponse);
        return pageMapper.fromJpaPageToDailyfeedPage(page, result);
    }

    @Transactional(readOnly = true)
    public List<PostDto.Post> withAuthorsAndStatistics(Long memberId, List<Post> posts, String token, HttpServletResponse httpResponse) {
        // group by 및 키값 추출
        Map<Long, Post> postMap = posts.stream().collect(Collectors.toMap(p -> p.getId(), p -> p));
        Set<Long> authorIds = posts.stream().map(p -> p.getAuthorId()).collect(Collectors.toSet());

        // 작성자 상새 정보
        Map<Long, MemberProfileDto.Summary> authorsMap = memberFeignHelper.getMemberMap(authorIds, token, httpResponse);
        // 통계정보 (댓글수, 좋아요) 추출
        PostStatisticsInternal statistics = queryPostStatistics(PostDto.PostsBulkRequest.builder().ids(postMap.keySet()).build());
        // 좋아요 여부 체크
        Set<Long> likedPostPks = postLikeMongoRepository.findByPostPkInAndMemberId(postMap.keySet(), memberId).stream().map(d -> d.getPostPk()).collect(Collectors.toSet());

        // 작성자 상세정보, 통계 정보 병합
        return mergeAuthorAndStatistics(posts, likedPostPks, authorsMap, statistics);
    }

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

    public List<PostDto.Post> mergeAuthorAndStatistics(List<Post> posts, Set<Long> likedPostPks, Map<Long, MemberProfileDto.Summary> authorsMap, PostStatisticsInternal statistics){
        return posts.stream()
                .map(post -> {
                    Long commentCount = 0L;
                    Long likeCount = 0L;
                    if(statistics.commentCountStatisticsMap() != null && statistics.commentCountStatisticsMap().get(post.getId()) != null){
                        commentCount = statistics.commentCountStatisticsMap().get(post.getId()).getCommentCount();
                    }
                    if(statistics.postLikeCountStatisticsMap() != null && statistics.postLikeCountStatisticsMap().get(post.getId()) != null){
                        likeCount = statistics.postLikeCountStatisticsMap().get(post.getId()).getLikeCount();
                    }
                    return timelinePostMapper.toPostDto(post, likedPostPks.contains(post.getId()) ,authorsMap.get(post.getAuthorId()), commentCount, likeCount);
                })
                .collect(Collectors.toList());
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

    @Transactional(readOnly = true)
    public List<PostDto.Post> getMyPosts(MemberDto.Member requestedMember, int page, int size, String token, HttpServletResponse httpResponse) {
        Pageable pageable = PageRequest.of(page, size);
        Slice<Post> posts = postRepository.findByAuthorIdAndNotDeleted(requestedMember.getId(), pageable);
        return withAuthorsAndStatistics(requestedMember.getId(), posts.getContent(), token, httpResponse);
    }

    public PostDto.Post getPostById(MemberDto.Member member, Long postId, String token, HttpServletResponse httpResponse) {
        Post post = postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(PostNotFoundException::new);

        if (post.getAuthorId() != member.getId()) {
            // 조회수 증가 (조회수 필드 :: 필요에 의해 비정규화 상태 그대로 유지)
            post.incrementViewCount();
        }

        // 작성자 정보 조회
        MemberProfileDto.Summary authorSummary = memberFeignHelper.getMemberSummaryById(post.getAuthorId(), token, httpResponse);

        // 본문 통계 정보 조회
        PostStatisticsInternal statistics = queryPostStatistics(PostDto.PostsBulkRequest.builder().ids(Set.of(postId)).build());
        Map<Long, PostDto.PostLikeCountStatistics> postLikeCountStatisticsMap = statistics.postLikeCountStatisticsMap();
        Map<Long, PostDto.PostCommentCountStatistics> commentCountStatisticsMap = statistics.commentCountStatisticsMap();

        // liked 여부 표시를 위한 조회
        Set<Long> likedPostPks = postLikeMongoRepository.findByPostPkInAndMemberId(Set.of(postId), member.getId()).stream().map(d -> d.getPostPk()).collect(Collectors.toSet());

        // 글 조회 이벤트 발행
        memberActivityKafkaPublisher.publishPostReadEvent(member.getId(), postId);

        // return
        return timelineMapper.toPostDto(post, authorSummary, likedPostPks.contains(postId), postLikeCountStatisticsMap.get(postId), commentCountStatisticsMap.get(postId));
    }

    public DailyfeedScrollPage<PostDto.Post> getPostsByAuthor(Long authorId, Pageable pageable, String token, HttpServletResponse httpResponse) {
        MemberDto.Member author = memberFeignHelper.getMemberById(authorId, token, httpResponse);
        if (author == null) {
            throw new MemberNotFoundException(() -> "삭제된 사용자입니다");
        }

        Slice<Post> posts = postRepository.findByAuthorIdAndNotDeleted(author.getId(), pageable);
        return pageMapper.fromJpaSliceToDailyfeedScrollPage(posts, mergeAuthorAndCommentCount(posts.getContent(), token, httpResponse));
    }

    public PostStatisticsInternal queryPostStatistics(PostDto.PostsBulkRequest request){
        Map<Long, PostDto.PostLikeCountStatistics> postLikeCountStatisticsMap = postLikeMongoAggregation.countLikesByPostPks(request.getIds())
                .stream().map(timelineMapper::toPostLikeStatistics)
                .collect(Collectors.toMap(o -> o.getPostPk(), o -> o));

        Map<Long, PostDto.PostCommentCountStatistics> commentCountStatisticsMap = commentMongoAggregation.countCommentsByPostPks(request.getIds())
                .stream().map(timelineMapper::toPostCommentCountStatistics)
                .collect(Collectors.toMap(o -> o.getPostPk(), o -> o));

        return new PostStatisticsInternal(postLikeCountStatisticsMap, commentCountStatisticsMap);
    }

    @Transactional(readOnly = true)
    public DailyfeedScrollPage<CommentDto.Comment> getMyComments(Long memberId, Pageable pageable, String token, HttpServletResponse httpResponse) {
        // 1. 댓글 목록 조회
        Slice<Comment> comments = commentRepository.findByAuthorIdAndNotDeleted(memberId, pageable);
        if (comments.isEmpty()) {
            return pageMapper.emptyScrollPage();
        }

        // 2. 댓글 ID 목록 추출
        Set<Long> commentIds = comments.getContent().stream().map(click.dailyfeed.timeline.domain.comment.entity.Comment::getId).collect(Collectors.toSet());
        // 3. 작성자 정보 조회
        Set<Long> authorIds = comments.getContent().stream().map(c -> c.getAuthorId()).collect(Collectors.toSet());
        // 4. 통계 정보 조회
        ReplyStatistics replyStatistics = aggregateReplyStatistics(commentIds, authorIds, token, httpResponse);

        Set<Long> myLikeReplyIds = commentLikeMongoRepository.findByCommentPkIn(commentIds)
                .stream().filter(document -> document.getMemberId().equals(memberId))
                .map(document -> document.getCommentPk())
                .collect(Collectors.toSet());

        // 5. 변환
        List<CommentDto.Comment> result = comments.getContent().stream()
                .map(comment -> {
                    Long replyCount = replyStatistics.replyCountMap.getOrDefault(comment.getId(), 0L);
                    Long commentLikeCount = replyStatistics.commentLikeMap.getOrDefault(comment.getId(), 0L);
                    MemberProfileDto.Summary summary = replyStatistics.authorMap.get(comment.getAuthorId());
                    Boolean liked = myLikeReplyIds.contains(comment.getId());
                    return timelineMapper.toReplyCommentAtTopLevel(comment, liked, replyCount, commentLikeCount, summary);
                })
                .collect(Collectors.toList());

        // 6. DailyfeedScrollPage로 변환하여 반환
        return pageMapper.fromJpaSliceToDailyfeedScrollPage(comments, result);
    }

    public ReplyStatistics aggregateReplyStatistics(Set<Long> commentIds, Set<Long> authorIds, String token, HttpServletResponse httpResponse) {
        // 각 댓글의 대댓글 개수와 함께 조회
        List<CommentRepository.ReplyCountProjection> replyCounts =
                commentRepository.countRepliesByParentIds(commentIds);
        // 댓글 ID를 키로 하는 대댓글 개수 맵 생성
        Map<Long, Long> replyCountMap = replyCounts.stream()
                .collect(Collectors.toMap(
                        CommentRepository.ReplyCountProjection::getParentId,
                        CommentRepository.ReplyCountProjection::getReplyCount
                ));

        // 각 댓글에 대한 좋아요 개수 조회
        List<CommentLikeCountProjection> commentLikeCounts = commentLikeMongoAggregation.countLikesByCommentPks(commentIds);
        // 각 댓글 ID 를 키로 하는 댓글 좋아요 맵 생성
        Map<Long, Long> commentLikeMap = commentLikeCounts.stream()
                .collect(Collectors.toMap(
                        CommentLikeCountProjection::getCommentPk,
                        CommentLikeCountProjection::getLikeCount
                ));

        Map<Long, MemberProfileDto.Summary> authorMap = memberFeignHelper.getMemberMap(authorIds, token, httpResponse);

        return new ReplyStatistics(replyCountMap, commentLikeMap, authorMap);
    }

    /**
     * 특정 게시글의 최상위 댓글 목록을 대댓글 개수와 함께 조회
     * 각 댓글에 대댓글 개수(replyCount)가 포함된 Projection을 반환
     */
    public DailyfeedScrollPage<CommentDto.Comment> getCommentsByPostWithReplyCount(MemberProfileDto.Summary requestedMember, Long postId, Pageable pageable, String token, HttpServletResponse httpResponse) {
        // 1. 최상위 댓글 조회
        Slice<Comment> comments = commentRepository.findTopLevelCommentsByPostId(postId, pageable);
        if (comments.isEmpty()) {
            return pageMapper.emptyScrollPage();
        }

        // 2. 댓글 ID 목록 추출
        Set<Long> commentIds = comments.getContent().stream().map(click.dailyfeed.timeline.domain.comment.entity.Comment::getId).collect(Collectors.toSet());
        // 3. 작성자 정보 조회
        Set<Long> authorIds = comments.getContent().stream().map(c -> c.getAuthorId()).collect(Collectors.toSet());
        // 4. 통계 정보 조회
        ReplyStatistics replyStatistics = aggregateReplyStatistics(commentIds, authorIds, token, httpResponse);

        Set<Long> myLikeReplyIds = commentLikeMongoRepository.findByCommentPkIn(commentIds)
                .stream().filter(document -> requestedMember.getMemberId().equals(document.getMemberId()))
                .map(document -> document.getCommentPk())
                .collect(Collectors.toSet());

        // 5. 변환
        List<CommentDto.Comment> result = comments.getContent().stream()
                .map(comment -> {
                    Long replyCount = replyStatistics.replyCountMap.getOrDefault(comment.getId(), 0L);
                    Long commentLikeCount = replyStatistics.commentLikeMap.getOrDefault(comment.getId(), 0L);
                    MemberProfileDto.Summary summary = replyStatistics.authorMap.get(comment.getAuthorId());
                    Boolean liked = myLikeReplyIds.contains(comment.getId());
                    return timelineMapper.toReplyCommentAtTopLevel(comment, liked, replyCount, commentLikeCount, summary);
                })
                .collect(Collectors.toList());

        // 6. DailyfeedScrollPage로 변환하여 반환
        return pageMapper.fromJpaSliceToDailyfeedScrollPage(comments, result);
    }

    @Transactional(readOnly = true)
    public DailyfeedScrollPage<CommentDto.Comment> getCommentsByUser(Long memberId, Pageable pageable, String token, HttpServletResponse httpResponse) {
        // 1. 댓글 목록 조회
        Slice<Comment> comments = commentRepository.findByAuthorIdAndNotDeleted(memberId, pageable);
        if (comments.isEmpty()) {
            return pageMapper.emptyScrollPage();
        }

        // 2. 댓글 ID 목록 추출
        Set<Long> commentIds = comments.getContent().stream().map(click.dailyfeed.timeline.domain.comment.entity.Comment::getId).collect(Collectors.toSet());
        // 3. 작성자 정보 조회
        Set<Long> authorIds = comments.getContent().stream().map(c -> c.getAuthorId()).collect(Collectors.toSet());
        // 4. 통계 정보 조회
        ReplyStatistics replyStatistics = aggregateReplyStatistics(commentIds, authorIds, token, httpResponse);

        Set<Long> myLikeReplyIds = commentLikeMongoRepository.findByCommentPkIn(commentIds)
                .stream().filter(document -> memberId.equals(document.getMemberId()))
                .map(document -> document.getCommentPk())
                .collect(Collectors.toSet());

        // 5. 변환
        List<CommentDto.Comment> result = comments.getContent().stream()
                .map(comment -> {
                    Long replyCount = replyStatistics.replyCountMap.getOrDefault(comment.getId(), 0L);
                    Long commentLikeCount = replyStatistics.commentLikeMap.getOrDefault(comment.getId(), 0L);
                    MemberProfileDto.Summary summary = replyStatistics.authorMap.get(comment.getAuthorId());
                    Boolean liked = myLikeReplyIds.contains(comment.getId());
                    return timelineMapper.toReplyCommentAtTopLevel(comment, liked, replyCount, commentLikeCount, summary);
                })
                .collect(Collectors.toList());

        // 6. DailyfeedScrollPage로 변환하여 반환
        return pageMapper.fromJpaSliceToDailyfeedScrollPage(comments, result);
    }

    // 댓글 상세 조회
    @Transactional(readOnly = true)
    public CommentDto.Comment getCommentById(Long memberId, Long commentId, String token, HttpServletResponse httpResponse) {
        // 댓글 정보 조회
        Comment comment = commentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(CommentNotFoundException::new);

//        Long replyCount = commentMongoAggregation.countCommentsByPostPk(commentId);
        Long replyCount = commentRepository.countCommentByParentId(commentId);

        Long likeCount = commentLikeMongoAggregation.countLikesByCommentPk(commentId);
        Map<Long, MemberProfileDto.Summary> authorMap = memberFeignHelper.getMemberMap(Set.of(comment.getAuthorId()), token, httpResponse);

        Set<Long> myLikeReplyIds = commentLikeMongoRepository.findByCommentPkIn(Set.of(commentId))
                .stream().filter(document -> memberId.equals(document.getMemberId()))
                .map(document -> document.getCommentPk())
                .collect(Collectors.toSet());

        // 멤버 활동 기록
        memberActivityKafkaPublisher.publishCommentReadEvent(memberId, comment.getPost().getId(), commentId);
        return timelineMapper.toReplyCommentAtTopLevel(comment, myLikeReplyIds.contains(memberId) ,replyCount, likeCount, authorMap.get(comment.getAuthorId()));
    }

    @Transactional(readOnly = true)
    public DailyfeedScrollPage<CommentDto.Comment> getRepliesByParent(MemberProfileDto.Summary member, Long parentId, Pageable pageable, String token, HttpServletResponse httpResponse) {
        Comment parentComment = commentRepository.findByIdAndNotDeleted(parentId)
                .orElseThrow(ParentCommentNotFoundException::new);

        // 1. 댓글 목록 조회
        Slice<Comment> replies = commentRepository.findChildrenByParentSlice(parentComment, pageable);
        if (replies.isEmpty()) {
            return pageMapper.emptyScrollPage();
        }

        // 2. 댓글 ID 목록 추출
        Set<Long> commentIds = replies.getContent().stream().map(click.dailyfeed.timeline.domain.comment.entity.Comment::getId).collect(Collectors.toSet());
        // 3. 작성자 정보 조회
        Set<Long> authorIds = replies.getContent().stream().map(c -> c.getAuthorId()).collect(Collectors.toSet());
        // 4. 통계 정보 조회
        ReplyStatistics replyStatistics = aggregateReplyStatistics(commentIds, authorIds, token, httpResponse);

        Set<Long> myLikeReplyIds = commentLikeMongoRepository.findByCommentPkIn(commentIds)
                .stream().filter(document -> member.getMemberId().equals(document.getMemberId()))
                .map(document -> document.getCommentPk())
                .collect(Collectors.toSet());

        // 5. 변환
        List<CommentDto.Comment> result = replies.getContent().stream()
                .map(comment -> {
                    Long replyCount = replyStatistics.replyCountMap.getOrDefault(comment.getId(), 0L);
                    Long commentLikeCount = replyStatistics.commentLikeMap.getOrDefault(comment.getId(), 0L);
                    MemberProfileDto.Summary summary = replyStatistics.authorMap.get(comment.getAuthorId());
                    Boolean liked = myLikeReplyIds.contains(comment.getId());
                    return timelineMapper.toReplyComment(parentId, liked, comment, replyCount, commentLikeCount, summary);
                })
                .collect(Collectors.toList());

        // 6. DailyfeedScrollPage로 변환하여 반환
        return pageMapper.fromJpaSliceToDailyfeedScrollPage(replies, result);
    }

    private record PostStatisticsInternal(
            Map<Long, PostDto.PostLikeCountStatistics> postLikeCountStatisticsMap,
            Map<Long, PostDto.PostCommentCountStatistics> commentCountStatisticsMap
    ){}

    record ReplyStatistics(
            Map<Long, Long> replyCountMap,
            Map<Long, Long> commentLikeMap,
            Map<Long, MemberProfileDto.Summary> authorMap
    ){}
}
