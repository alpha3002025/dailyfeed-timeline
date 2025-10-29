package click.dailyfeed.timeline.domain.timeline.api;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.timeline.timeline.dto.TimelineDto;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.page.DailyfeedPage;
import click.dailyfeed.code.global.web.page.DailyfeedScrollPage;
import click.dailyfeed.code.global.web.response.DailyfeedPageResponse;
import click.dailyfeed.code.global.web.response.DailyfeedScrollResponse;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.feign.config.web.annotation.AuthenticatedMember;
import click.dailyfeed.feign.config.web.annotation.AuthenticatedMemberProfile;
import click.dailyfeed.feign.config.web.annotation.AuthenticatedMemberProfileSummary;
import click.dailyfeed.timeline.domain.timeline.service.TimelineService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/timeline")
public class TimelineController {
    private final TimelineService timelineService;

    @GetMapping("/posts/followings")
    public DailyfeedScrollResponse<DailyfeedScrollPage<PostDto.Post>> getMyFollowingMembersTimeline(
            @AuthenticatedMemberProfile MemberProfileDto.MemberProfile memberProfile,
            @RequestHeader("Authorization") String token,
            HttpServletResponse response,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ){
        return timelineService.getMyFollowingMembersTimeline(memberProfile, pageable, token, response);
    }

    @GetMapping("/posts/most-commented")
    public DailyfeedScrollResponse<DailyfeedScrollPage<PostDto.Post>> getMostCommentedMembersTimeline(
            @AuthenticatedMemberProfile MemberProfileDto.MemberProfile member,
            @RequestHeader("Authorization") String token,
            HttpServletResponse response,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable
    ){
        DailyfeedScrollPage<PostDto.Post> scrollPage = timelineService.getPostsOrderByCommentCount(member.getMemberId(), pageable, token, response);
        return DailyfeedScrollResponse.<DailyfeedScrollPage<PostDto.Post>>builder()
                .data(scrollPage)
                .result(ResponseSuccessCode.SUCCESS)
                .status(HttpStatus.OK.value())
                .build();
    }

    // 인기 게시글 조회
    // (timeline 으로 이관 예정(/timeline/feed/popular))
    @GetMapping("/posts/most-popular")
    public DailyfeedScrollResponse<DailyfeedScrollPage<PostDto.Post>> getPopularPosts(
            @AuthenticatedMemberProfileSummary MemberProfileDto.Summary member,
            @RequestHeader("Authorization") String token,
            HttpServletResponse httpResponse,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {

        DailyfeedScrollPage<PostDto.Post> result = timelineService.getPopularPosts(member.getMemberId(), pageable, token, httpResponse);
        return DailyfeedScrollResponse.<DailyfeedScrollPage<PostDto.Post>>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    // 최근 활동이 있는 게시글 조회
    // (timeline 으로 이관 예정 (/timeline/feed/latest))
    @GetMapping("/posts/recent-activities")
    public DailyfeedScrollResponse<DailyfeedScrollPage<PostDto.Post>> getPostsByRecentActivity(
            @AuthenticatedMemberProfileSummary MemberProfileDto.Summary member,
            @RequestHeader("Authorization") String token,
            HttpServletResponse httpResponse,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {

        DailyfeedScrollPage<PostDto.Post> result = timelineService.getPostsByRecentActivities(member.getMemberId(), pageable, token, httpResponse);
        return DailyfeedScrollResponse.<DailyfeedScrollPage<PostDto.Post>>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    /// my posts
    @GetMapping("/posts")
    @Operation(summary = "내가 쓴 개시글 목록 조회", description = "내가 쓴 개시글 목록을 조회합니다.")
    public DailyfeedScrollResponse<DailyfeedScrollPage<PostDto.Post>> getPosts(
            @AuthenticatedMember MemberDto.Member member,
            HttpServletResponse httpResponse,
            @RequestHeader("Authorization") String token,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        DailyfeedScrollPage<PostDto.Post> result = timelineService.getMyPosts(member, pageable, token, httpResponse);
        return DailyfeedScrollResponse.<DailyfeedScrollPage<PostDto.Post>>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    // 게시글 상세 조회
    @Operation(summary = "게시글 상세 조회", description = "특정 게시글의 상세 정보를 조회합니다.")
    @GetMapping("/posts/{postId}")
    public DailyfeedServerResponse<PostDto.Post> getPost(
            @AuthenticatedMember MemberDto.Member member,
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletResponse httpResponse,
            @PathVariable Long postId) {

        PostDto.Post result = timelineService.getPostById(member, postId, token, httpResponse);
        return DailyfeedServerResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    @Operation(summary = "특정 사용자의 게시글 목록 조회", description = "특정 사용자가 작성한 게시글을 페이징하여 조회합니다.")
    @GetMapping("/posts/authors/{authorId}")
    public DailyfeedScrollResponse<DailyfeedScrollPage<PostDto.Post>> getPostsByAuthor(
            @AuthenticatedMember MemberDto.Member member,
            HttpServletResponse httpResponse,
            @RequestHeader("Authorization") String token,
            @PathVariable Long authorId,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        DailyfeedScrollPage<PostDto.Post> result = timelineService.getPostsByAuthor(authorId, pageable, token, httpResponse);
        return DailyfeedScrollResponse.<DailyfeedScrollPage<PostDto.Post>>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    /// query
    /// 특수목적 or internal
    /// 특정 post Id 리스트에 해당되는 글 목록
    @Operation(summary = "특정 post id 리스트에 해당하는 글 목록", description = "글 id 목록에 대한 글 데이터 목록을 조회합니다.")
    @PostMapping  ("/posts/query/list")
    public DailyfeedServerResponse<List<PostDto.Post>> getPostListByIdsIn(
            @AuthenticatedMember MemberDto.Member member,
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletResponse httpResponse,
            @RequestBody PostDto.PostsBulkRequest request
    ){
        List<PostDto.Post> result = timelineService.getPostListByIdsIn(request, token, httpResponse);
        return DailyfeedServerResponse.<List<PostDto.Post>>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }


    /// comments
    // 내 댓글 목록
    @GetMapping("/comments")
    public DailyfeedScrollResponse<DailyfeedScrollPage<CommentDto.Comment>> getMyComments(
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletResponse httpResponse,
            @AuthenticatedMember MemberDto.Member requestedMember,
            @PageableDefault(page = 0, size = 20, sort = "createdAt") Pageable pageable) {
        DailyfeedScrollPage<CommentDto.Comment> result = timelineService.getMyComments(requestedMember.getId(), pageable, authorizationHeader, httpResponse);
        return DailyfeedScrollResponse.<DailyfeedScrollPage<CommentDto.Comment>>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    ///  /comments/post/{postId}    ///
    // 특정 게시글의 댓글 목록을 페이징으로 조회
    @GetMapping("/posts/{postId}/comments")
    public DailyfeedScrollResponse<DailyfeedScrollPage<CommentDto.Comment>> getCommentsByPost(
            @AuthenticatedMemberProfileSummary MemberProfileDto.Summary requestedMember,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletResponse httpResponse,
            @PathVariable Long postId,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        DailyfeedScrollPage<CommentDto.Comment> result = timelineService.getCommentsByPostWithReplyCount(requestedMember, postId, pageable, authorizationHeader, httpResponse);
        return DailyfeedScrollResponse.<DailyfeedScrollPage<CommentDto.Comment>>builder()
                .result(ResponseSuccessCode.SUCCESS)
                .status(HttpStatus.OK.value())
                .data(result)
                .build();
    }

    /// /comments/member/{memberId}     ///
    // 특정 사용자의 댓글 목록
    @GetMapping("/members/{memberId}/comments")
    public DailyfeedScrollResponse<DailyfeedScrollPage<CommentDto.Comment>> getCommentsByUser(
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletResponse httpResponse,
            @PathVariable Long memberId,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        DailyfeedScrollPage<CommentDto.Comment> result = timelineService.getCommentsByUser(memberId, pageable, authorizationHeader, httpResponse);
        return DailyfeedScrollResponse.<DailyfeedScrollPage<CommentDto.Comment>>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    // 댓글 상세 조회
    @GetMapping("/comments/{commentId}")
    public DailyfeedServerResponse<CommentDto.Comment> getComment(
            @AuthenticatedMemberProfileSummary MemberProfileDto.Summary requestedMember,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletResponse httpResponse,
            @PathVariable Long commentId) {

        CommentDto.Comment result = timelineService.getCommentById(requestedMember.getId(), commentId, authorizationHeader, httpResponse);
        return DailyfeedServerResponse.<CommentDto.Comment>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    @GetMapping("/comments/{commentId}/replies")
    public DailyfeedScrollResponse<DailyfeedScrollPage<CommentDto.Comment>> getRepliesByParent(
            @AuthenticatedMemberProfileSummary MemberProfileDto.Summary member,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletResponse httpResponse,
            @PathVariable Long commentId,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        DailyfeedScrollPage<CommentDto.Comment> result = timelineService.getRepliesByParent(member, commentId, pageable, authorizationHeader, httpResponse);
        return DailyfeedScrollResponse.<DailyfeedScrollPage<CommentDto.Comment>>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    /// ADMIN (SEASON 2)
    // (admin) SEASON2 특정 기간 내 게시글 조회
    @GetMapping("/posts/date-range")
    public DailyfeedPageResponse<PostDto.Post> getPostsByDateRange(
            @AuthenticatedMemberProfileSummary MemberProfileDto.Summary member,
            @RequestHeader("Authorization") String token,
            HttpServletResponse httpResponse,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {

        DailyfeedPage<PostDto.Post> result = timelineService.getPostsByDateRange(member.getMemberId(), startDate, endDate, pageable, token, httpResponse);
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }
}
