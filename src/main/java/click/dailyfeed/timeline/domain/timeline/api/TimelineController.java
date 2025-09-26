package click.dailyfeed.timeline.domain.timeline.api;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.timeline.timeline.dto.TimelineDto;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.page.DailyfeedPage;
import click.dailyfeed.code.global.web.page.DailyfeedScrollPage;
import click.dailyfeed.code.global.web.response.DailyfeedPageResponse;
import click.dailyfeed.code.global.web.response.DailyfeedScrollResponse;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.feign.config.web.annotation.AuthenticatedMemberProfile;
import click.dailyfeed.timeline.domain.timeline.service.TimelineService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

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

}
