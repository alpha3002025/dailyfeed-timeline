package click.dailyfeed.timeline.domain.timeline.api;

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
    public DailyfeedServerResponse<DailyfeedScrollPage<TimelineDto.TimelinePostActivity>> getMyFollowingMembersTimeline(
            @AuthenticatedMemberProfile MemberProfileDto.MemberProfile memberProfile,
            @RequestHeader("Authorization") String token,
            HttpServletResponse response,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable
    ){
        return timelineService.getMyFollowingMembersTimeline(memberProfile, pageable, token, response);
    }

    @GetMapping("/posts/most-commented")
    public DailyfeedScrollResponse<DailyfeedScrollPage<PostDto.Post>> getMostCommentedMembersTimeline(
            @AuthenticatedMemberProfile MemberProfileDto.MemberProfile memberProfile,
            @RequestHeader("Authorization") String token,
            HttpServletResponse response,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable
    ){
        DailyfeedScrollPage<PostDto.Post> scrollPage = timelineService.getPostsOrderByCommentCount(pageable, token, response);
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
            @RequestHeader("Authorization") String token,
            HttpServletResponse httpResponse,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {

        DailyfeedScrollPage<PostDto.Post> result = timelineService.getPopularPosts(pageable, token, httpResponse);
        return DailyfeedScrollResponse.<DailyfeedScrollPage<PostDto.Post>>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    // 최근 활동이 있는 게시글 조회
    // (timeline 으로 이관 예정 (/timeline/feed/latest))
    @GetMapping("/posts/recent-activity")
    public DailyfeedScrollResponse<DailyfeedScrollPage<PostDto.Post>> getPostsByRecentActivity(
            @RequestHeader("Authorization") String token,
            HttpServletResponse httpResponse,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {

        DailyfeedScrollPage<PostDto.Post> result = timelineService.getPostsByRecentActivity(pageable, token, httpResponse);
        return DailyfeedScrollResponse.<DailyfeedScrollPage<PostDto.Post>>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    // 특정 기간 내 게시글 조회
    @GetMapping("/posts/date-range")
    public DailyfeedPageResponse<PostDto.Post> getPostsByDateRange(
            @RequestHeader("Authorization") String token,
            HttpServletResponse httpResponse,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {

        DailyfeedPage<PostDto.Post> result = timelineService.getPostsByDateRange(startDate, endDate, pageable, token, httpResponse);
        return DailyfeedPageResponse.<PostDto.Post>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(result)
                .build();
    }

    /// my posts
    @GetMapping("/posts")
    @Operation(summary = "내가 쓴 개시글 목록 조회", description = "내가 쓴 개시글 목록을 조회합니다.")
    public DailyfeedPageResponse<PostDto.Post> getPosts(
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
        DailyfeedPage<PostDto.Post> result = timelineService.getMyPosts(member, pageable, token, httpResponse);
        return DailyfeedPageResponse.<PostDto.Post>builder()
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
    public DailyfeedPageResponse<PostDto.Post> getPostsByAuthor(
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

        DailyfeedPage<PostDto.Post> result = timelineService.getPostsByAuthor(authorId, pageable, token, httpResponse);
        return DailyfeedPageResponse.<PostDto.Post>builder()
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
}
