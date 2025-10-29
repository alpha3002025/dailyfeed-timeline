package click.dailyfeed.timeline.domain.timeline.service;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.timeline.timeline.predicate.PushPullPredicate;
import click.dailyfeed.code.global.cache.RedisKeyPrefix;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.page.DailyfeedPage;
import click.dailyfeed.code.global.web.page.DailyfeedScrollPage;
import click.dailyfeed.code.global.web.response.DailyfeedScrollResponse;
import click.dailyfeed.pagination.slice.HasMoreComponent;
import click.dailyfeed.timeline.domain.timeline.mapper.TimelineMapper;
import click.dailyfeed.timeline.domain.timeline.redis.TimelinePostsApiRedisService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class TimelineService {
    private final TimelinePostsApiRedisService timelinePostsApiRedisService;
    private final TimelinePullService timelinePullService;
    private final TimelineMapper timelineMapper;
    private final HasMoreComponent hasMoreComponent;

    @Value("${dailyfeed.services.timeline.push-pull.limit}")
    private Integer pushPullLimit;

    public DailyfeedScrollResponse<DailyfeedScrollPage<PostDto.Post>> getMyFollowingMembersTimeline(MemberProfileDto.MemberProfile member, int page, int size, String token, HttpServletResponse httpServletResponse) {
        if(PushPullPredicate.PUSH.equals(checkPushOrPull(member.getFollowingsCount()))){
            // (1)
            // redis 에서 조회 (size + 1 개를 조회하여 hasNext 판단)
            final String redisKey = RedisKeyPrefix.TIMELINE_API_POSTS_FOLLOWINGS_RECENT_POSTS.getKeyPrefix() + member.getMemberId();
            List<PostDto.Post> redisResult = timelinePostsApiRedisService.topN(redisKey, page, size + 1);

            // hasNext 판단
            Boolean hasMore = hasMoreComponent.hasMore(redisResult, size);
            List<PostDto.Post> content = hasMoreComponent.toList(redisResult, size);

            // (2)
            // 부족할 경우 pull 데이터로 보완
            if(content.size() < size){
                List<PostDto.Post> dbData = timelinePullService.listMyFollowingActivities(member.getId(), page, size + 1, token, httpServletResponse);
                List<PostDto.Post> merged = mergeFeedsWithoutDuplicate(content, dbData, size + 1);

                Boolean mergedHasMore = hasMoreComponent.hasMore(merged, size);
                List<PostDto.Post> mergedContent = hasMoreComponent.toList(merged, size);

                DailyfeedScrollPage<PostDto.Post> slice = timelineMapper.toScrollPage(mergedContent, page, size, mergedHasMore);
                return DailyfeedScrollResponse.<DailyfeedScrollPage<PostDto.Post>>builder()
                        .data(slice)
                        .result(ResponseSuccessCode.SUCCESS)
                        .status(HttpStatus.OK.value())
                        .build();
            }

            return DailyfeedScrollResponse.<DailyfeedScrollPage<PostDto.Post>>builder()
                    .data(timelineMapper.toScrollPage(content, page, size, hasMore))
                    .result(ResponseSuccessCode.SUCCESS)
                    .status(HttpStatus.OK.value())
                    .build();
        }
        else{ // 실제 데이터를 그대로 pull 해온다.
            List<PostDto.Post> pullActivities = timelinePullService.listHeavyMyFollowingActivities(member, page, size + 1, token, httpServletResponse);
            Boolean hasMore = hasMoreComponent.hasMore(pullActivities, size);
            List<PostDto.Post> content = hasMoreComponent.toList(pullActivities, size);

            return DailyfeedScrollResponse.<DailyfeedScrollPage<PostDto.Post>>builder()
                    .data(timelineMapper.toScrollPage(content, page, size, hasMore))
                    .result(ResponseSuccessCode.SUCCESS)
                    .status(HttpStatus.OK.value())
                    .build();
        }
    }

    public DailyfeedScrollResponse<DailyfeedScrollPage<PostDto.Post>> getPostsOrderByCommentCount(Long memberId, int page, int size, String token, HttpServletResponse httpResponse){
        // (1)
        // redis 에서 조회 (size + 1 개를 조회하여 hasNext 판단)
        final String redisKey = RedisKeyPrefix.TIMELINE_API_POSTS_MOST_COMMENTED.getKeyPrefix() + memberId;
        List<PostDto.Post> redisResult = timelinePostsApiRedisService.topN(redisKey, page, size + 1);

        // hasNext 판단
        Boolean hasMore = hasMoreComponent.hasMore(redisResult, size);
        List<PostDto.Post> content = hasMoreComponent.toList(redisResult, size);

        // (2)
        // 부족할 경우 pull 데이터로 보완
        if(content.size() < size){
            List<PostDto.Post> dbData = timelinePullService.getPostsOrderByCommentCount(memberId, page, size + 1, token, httpResponse);
            List<PostDto.Post> merged = mergeFeedsWithoutDuplicate(content, dbData, size + 1);

            Boolean mergedHasMore = hasMoreComponent.hasMore(merged, size);
            List<PostDto.Post> mergedContent = hasMoreComponent.toList(merged, size);

            DailyfeedScrollPage<PostDto.Post> slice = timelineMapper.toScrollPage(mergedContent, page, size, mergedHasMore);
            return DailyfeedScrollResponse.<DailyfeedScrollPage<PostDto.Post>>builder()
                    .data(slice)
                    .result(ResponseSuccessCode.SUCCESS)
                    .status(HttpStatus.OK.value())
                    .build();
        }

        return DailyfeedScrollResponse.<DailyfeedScrollPage<PostDto.Post>>builder()
                .data(timelineMapper.toScrollPage(content, page, size, hasMore))
                .result(ResponseSuccessCode.SUCCESS)
                .status(HttpStatus.OK.value())
                .build();
    }

    public DailyfeedScrollResponse<DailyfeedScrollPage<PostDto.Post>> getMyPosts(MemberDto.Member member, int page, int size, String token, HttpServletResponse httpResponse) {
        // size + 1개 조회하여 hasNext 판단
        List<PostDto.Post> result = timelinePullService.getMyPosts(member, page, size + 1, token, httpResponse);

        // hasNext 판단
        Boolean hasMore = hasMoreComponent.hasMore(result, size);
        List<PostDto.Post> content = hasMoreComponent.toList(result, size);

        return DailyfeedScrollResponse.<DailyfeedScrollPage<PostDto.Post>>builder()
                .data(timelineMapper.toScrollPage(content, page, size, hasMore))
                .result(ResponseSuccessCode.SUCCESS)
                .status(HttpStatus.OK.value())
                .build();
    }

    public PostDto.Post getPostById(MemberDto.Member member, Long postId, String token, HttpServletResponse httpResponse) {
        return timelinePullService.getPostById(member, postId, token, httpResponse);
    }

    public DailyfeedScrollPage<PostDto.Post> getPostsByAuthor(Long authorId, Pageable pageable, String token, HttpServletResponse httpResponse) {
        return timelinePullService.getPostsByAuthor(authorId, pageable, token, httpResponse);
    }

    public DailyfeedScrollResponse<DailyfeedScrollPage<PostDto.Post>> getPopularPosts(Long requestedMemberId, int page, int size, String token, HttpServletResponse httpResponse) {
        // (1)
        // redis 에서 조회 (size + 1 개를 조회하여 hasNext 판단)
        final String redisKey = RedisKeyPrefix.TIMELINE_API_POSTS_POPULAR_POSTS.getKeyPrefix() + requestedMemberId;
        List<PostDto.Post> redisResult = timelinePostsApiRedisService.topN(redisKey, page, size + 1);

        // hasNext 판단
        Boolean hasMore = hasMoreComponent.hasMore(redisResult, size);
        List<PostDto.Post> content = hasMoreComponent.toList(redisResult, size);

        // (2)
        // 부족할 경우 pull 데이터로 보완
        if(content.size() < size){
            List<PostDto.Post> dbData = timelinePullService.getPopularPosts(requestedMemberId, page, size + 1, token, httpResponse);
            List<PostDto.Post> merged = mergeFeedsWithoutDuplicate(content, dbData, size + 1);

            Boolean mergedHasMore = hasMoreComponent.hasMore(merged, size);
            List<PostDto.Post> mergedContent = hasMoreComponent.toList(merged, size);

            DailyfeedScrollPage<PostDto.Post> slice = timelineMapper.toScrollPage(mergedContent, page, size, mergedHasMore);
            return DailyfeedScrollResponse.<DailyfeedScrollPage<PostDto.Post>>builder()
                    .data(slice)
                    .result(ResponseSuccessCode.SUCCESS)
                    .status(HttpStatus.OK.value())
                    .build();
        }

        return DailyfeedScrollResponse.<DailyfeedScrollPage<PostDto.Post>>builder()
                .data(timelineMapper.toScrollPage(content, page, size, hasMore))
                .result(ResponseSuccessCode.SUCCESS)
                .status(HttpStatus.OK.value())
                .build();
    }

    public DailyfeedScrollResponse<DailyfeedScrollPage<PostDto.Post>> getPostsByRecentActivities(Long requestedMemberId, int page, int size, String token, HttpServletResponse httpResponse) {
        // size + 1개 조회하여 hasNext 판단
        List<PostDto.Post> result = timelinePullService.getPostsByRecentActivities(requestedMemberId, page, size + 1, token, httpResponse);

        // hasNext 판단
        Boolean hasMore = hasMoreComponent.hasMore(result, size);
        List<PostDto.Post> content = hasMoreComponent.toList(result, size);

        return DailyfeedScrollResponse.<DailyfeedScrollPage<PostDto.Post>>builder()
                .data(timelineMapper.toScrollPage(content, page, size, hasMore))
                .result(ResponseSuccessCode.SUCCESS)
                .status(HttpStatus.OK.value())
                .build();
    }

    public DailyfeedPage<PostDto.Post> getPostsByDateRange(Long requestedMemberId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable, String token, HttpServletResponse httpResponse) {
        return timelinePullService.getPostsByDateRange(requestedMemberId, startDate, endDate, pageable, token, httpResponse);
    }

    /// comments
    public DailyfeedScrollPage<CommentDto.Comment> getMyComments(Long id, Pageable pageable, String authorizationHeader, HttpServletResponse httpResponse) {
        return timelinePullService.getMyComments(id, pageable, authorizationHeader, httpResponse);
    }

    public DailyfeedScrollPage<CommentDto.Comment> getCommentsByPostWithReplyCount(MemberProfileDto.Summary requestedMember, Long postId, Pageable pageable, String token, HttpServletResponse httpResponse){
        return timelinePullService.getCommentsByPostWithReplyCount(requestedMember, postId, pageable, token, httpResponse);
    }

    public DailyfeedScrollPage<CommentDto.Comment> getCommentsByUser(Long memberId, Pageable pageable, String token, HttpServletResponse httpResponse) {
        return timelinePullService.getCommentsByUser(memberId, pageable, token, httpResponse);
    }

    public CommentDto.Comment getCommentById(Long memberId, Long commentId, String token, HttpServletResponse httpResponse) {
        return timelinePullService.getCommentById(memberId, commentId,token,httpResponse);
    }

    public DailyfeedScrollPage<CommentDto.Comment> getRepliesByParent(MemberProfileDto.Summary member, Long commentId, Pageable pageable, String token, HttpServletResponse httpResponse) {
        return timelinePullService.getRepliesByParent(member, commentId, pageable, token, httpResponse);
    }

    /**
     * PUSH : 팔로잉 멤버 수가 pushPullLimit 미만일 경우
     * PULL : 팔로잉 멤버 수가 pushPullLimit 이상일 경우
     */
    private PushPullPredicate checkPushOrPull(Long followingCount) {
        if(followingCount == null || followingCount <= 0){
            return PushPullPredicate.PUSH;
        }

        if(followingCount < pushPullLimit){
            return PushPullPredicate.PUSH;
        }
        return  PushPullPredicate.PULL;
    }

    private List<PostDto.Post> mergeFeedsWithoutDuplicate(
            List<PostDto.Post> feed1,
            List<PostDto.Post> feed2,
            int size
    ) {
        return Stream.concat(feed1.stream(), feed2.stream())
                .distinct()
                .sorted((a,b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(size)
                .toList();
    }

    public List<PostDto.Post> getPostListByIdsIn(PostDto.PostsBulkRequest request, String token, HttpServletResponse httpResponse) {
        return timelinePullService.getPostListByIdsIn(request, token, httpResponse);
    }
}
