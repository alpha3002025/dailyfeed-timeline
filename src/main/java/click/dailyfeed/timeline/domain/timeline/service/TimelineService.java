package click.dailyfeed.timeline.domain.timeline.service;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.timeline.timeline.dto.TimelineDto;
import click.dailyfeed.code.domain.timeline.timeline.predicate.PushPullPredicate;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.page.DailyfeedPage;
import click.dailyfeed.code.global.web.page.DailyfeedScrollPage;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.timeline.domain.comment.projection.CommentWithReplyCount;
import click.dailyfeed.timeline.domain.timeline.mapper.TimelineMapper;
import click.dailyfeed.timeline.domain.timeline.redis.TimelinePostActivityRedisService;
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
    private final TimelinePostActivityRedisService timelinePostActivityRedisService;
    private final TimelinePullService timelinePullService;
    private final TimelineMapper timelineMapper;

    @Value("${dailyfeed.services.timeline.push-pull.limit}")
    private Integer pushPullLimit;

    public DailyfeedServerResponse<DailyfeedScrollPage<TimelineDto.TimelinePostActivity>> getMyFollowingMembersTimeline(MemberProfileDto.MemberProfile member, Pageable pageable, String token, HttpServletResponse httpServletResponse) {
        if(PushPullPredicate.PUSH.equals(checkPushOrPull(member.getFollowingsCount()))){
            // (1)
            // redis 에서 조회
            List<TimelineDto.TimelinePostActivity> topNResult = timelinePostActivityRedisService.topN(member.getId(), pageable);

            // (2)
            // 부족할 경우 pull 데이터로 보완
            if(topNResult.size() < pageable.getPageSize()){
                List<TimelineDto.TimelinePostActivity> pullActivities = timelinePullService.listMyFollowingActivities(member.getId(), pageable.getPageNumber(), pageable.getPageSize(), 24, token, httpServletResponse);
                List<TimelineDto.TimelinePostActivity> timelinePostActivities = mergeFeedsWithoutDuplicate(topNResult, pullActivities, pageable.getPageSize());
                DailyfeedScrollPage<TimelineDto.TimelinePostActivity> slice = timelineMapper.fromTimelineList(timelinePostActivities, pageable);
                return DailyfeedServerResponse.<DailyfeedScrollPage<TimelineDto.TimelinePostActivity>>builder()
                        .data(slice)
                        .result(ResponseSuccessCode.SUCCESS)
                        .status(HttpStatus.OK.value())
                        .build();
            }

            return DailyfeedServerResponse.<DailyfeedScrollPage<TimelineDto.TimelinePostActivity>>builder()
                    .data(timelineMapper.fromTimelineList(topNResult, pageable))
                    .result(ResponseSuccessCode.SUCCESS)
                    .status(HttpStatus.OK.value())
                    .build();
        }
        else{ // 실제 데이터를 그대로 pull 해온다.
            List<TimelineDto.TimelinePostActivity> pullActivities = timelinePullService.listHeavyMyFollowingActivities(member, pageable, token, httpServletResponse);
            return DailyfeedServerResponse.<DailyfeedScrollPage<TimelineDto.TimelinePostActivity>>builder()
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
    public DailyfeedScrollPage<CommentDto.CommentSummary> getMyComments(Long id, Pageable pageable, String authorizationHeader, HttpServletResponse httpResponse) {
        return timelinePullService.getMyComments(id, pageable, authorizationHeader, httpResponse);
    }

    public DailyfeedScrollPage<CommentDto.Comment> getCommentsByPostWithPaging(Long postId, Pageable pageable, String token, HttpServletResponse httpResponse) {
        return timelinePullService.getCommentsByPostWithPaging(postId, pageable, token, httpResponse);
    }

    public DailyfeedScrollPage<CommentWithReplyCount> getCommentsByPostWithReplyCount(Long postId, Pageable pageable, String token, HttpServletResponse httpResponse){
        return timelinePullService.getCommentsByPostWithReplyCount(postId, pageable, token, httpResponse);
    }

    public DailyfeedScrollPage<CommentDto.CommentSummary> getCommentsByUser(Long memberId, Pageable pageable, String token, HttpServletResponse httpResponse) {
        return timelinePullService.getCommentsByUser(memberId, pageable, token, httpResponse);
    }

    public CommentDto.Comment getCommentById(Long memberId, Long commentId, String token, HttpServletResponse httpResponse) {
        return timelinePullService.getCommentById(memberId, commentId,token,httpResponse);
    }

    public DailyfeedScrollPage<CommentDto.Comment> getRepliesByParent(Long commentId, Pageable pageable, String token, HttpServletResponse httpResponse) {
        return timelinePullService.getRepliesByParent(commentId, pageable, token, httpResponse);
    }

    private PushPullPredicate checkPushOrPull(Long followingCount) {
        if(followingCount == null || followingCount <= 0){
            return PushPullPredicate.PUSH;
        }

        if(followingCount < pushPullLimit){
            return PushPullPredicate.PUSH;
        }
        return  PushPullPredicate.PULL;
    }

    private List<TimelineDto.TimelinePostActivity> mergeFeedsWithoutDuplicate(
            List<TimelineDto.TimelinePostActivity> feed1,
            List<TimelineDto.TimelinePostActivity> feed2,
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
