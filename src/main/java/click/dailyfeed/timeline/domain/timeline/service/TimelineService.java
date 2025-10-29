package click.dailyfeed.timeline.domain.timeline.service;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.timeline.timeline.dto.TimelineDto;
import click.dailyfeed.code.domain.timeline.timeline.predicate.PushPullPredicate;
import click.dailyfeed.code.global.cache.RedisKeyPrefix;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.page.DailyfeedPage;
import click.dailyfeed.code.global.web.page.DailyfeedScrollPage;
import click.dailyfeed.code.global.web.response.DailyfeedScrollResponse;
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

    @Value("${dailyfeed.services.timeline.push-pull.limit}")
    private Integer pushPullLimit;

    public DailyfeedScrollResponse<DailyfeedScrollPage<PostDto.Post>> getMyFollowingMembersTimeline(MemberProfileDto.MemberProfile member, Pageable pageable, String token, HttpServletResponse httpServletResponse) {
        if(PushPullPredicate.PUSH.equals(checkPushOrPull(member.getFollowingsCount()))){
            // (1)
            // redis 에서 조회
            final String redisKey = RedisKeyPrefix.TIMELINE_API_FOLLOWINGS_RECENT_POSTS.getKeyPrefix() + member.getMemberId();
            List<PostDto.Post> redisResult = timelinePostsApiRedisService.topN(redisKey, pageable);

            // (2)
            // 부족할 경우 pull 데이터로 보완
            if(redisResult.size() < pageable.getPageSize()){
                List<PostDto.Post> dbData = timelinePullService.listMyFollowingActivities(member.getId(), pageable, token, httpServletResponse);
                List<PostDto.Post> timelinePostActivities = mergeFeedsWithoutDuplicate(redisResult, dbData, pageable.getPageSize());
                DailyfeedScrollPage<PostDto.Post> slice = timelineMapper.fromTimelineList(timelinePostActivities, pageable);
                return DailyfeedScrollResponse.<DailyfeedScrollPage<PostDto.Post>>builder()
                        .data(slice)
                        .result(ResponseSuccessCode.SUCCESS)
                        .status(HttpStatus.OK.value())
                        .build();
            }

            return DailyfeedScrollResponse.<DailyfeedScrollPage<PostDto.Post>>builder()
                    .data(timelineMapper.fromTimelineList(redisResult, pageable))
                    .result(ResponseSuccessCode.SUCCESS)
                    .status(HttpStatus.OK.value())
                    .build();
        }
        else{ // 실제 데이터를 그대로 pull 해온다.
            List<PostDto.Post> pullActivities = timelinePullService.listHeavyMyFollowingActivities(member, pageable, token, httpServletResponse);
            return DailyfeedScrollResponse.<DailyfeedScrollPage<PostDto.Post>>builder()
                    .data(timelineMapper.fromTimelineList(pullActivities, pageable))
                    .result(ResponseSuccessCode.SUCCESS)
                    .status(HttpStatus.OK.value())
                    .build();
        }
    }

    public DailyfeedScrollPage<PostDto.Post> getPostsOrderByCommentCount(Long memberId, Pageable pageable, String token, HttpServletResponse httpResponse){
        DailyfeedScrollPage<PostDto.Post> result = timelinePullService.getPostsOrderByCommentCount(memberId, pageable, token, httpResponse);

        /// 댓글이 하나도 없을 경우 (기본옵션은 댓글이 없을 경우 표시x)
//        if(result.getContent().isEmpty()){ // 댓글이 달린 글이 없을 경우
//            return timelinePullService.getPopularPosts(pageable, token, httpResponse);
//        }

        return result;
    }

    public DailyfeedScrollPage<PostDto.Post> getMyPosts(MemberDto.Member member, Pageable pageable, String token, HttpServletResponse httpResponse) {
        return timelinePullService.getMyPosts(member, pageable, token, httpResponse);
    }

    public PostDto.Post getPostById(MemberDto.Member member, Long postId, String token, HttpServletResponse httpResponse) {
        return timelinePullService.getPostById(member, postId, token, httpResponse);
    }

    public DailyfeedScrollPage<PostDto.Post> getPostsByAuthor(Long authorId, Pageable pageable, String token, HttpServletResponse httpResponse) {
        return timelinePullService.getPostsByAuthor(authorId, pageable, token, httpResponse);
    }

    public DailyfeedScrollPage<PostDto.Post> getPopularPosts(Long requestedMemberId, Pageable pageable, String token, HttpServletResponse httpResponse) {
        return timelinePullService.getPopularPosts(requestedMemberId, pageable, token, httpResponse);
    }

    public DailyfeedScrollPage<PostDto.Post> getPostsByRecentActivities(Long requestedMemberId, Pageable pageable, String token, HttpServletResponse httpResponse) {
        return timelinePullService.getPostsByRecentActivities(requestedMemberId, pageable, token, httpResponse);
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
