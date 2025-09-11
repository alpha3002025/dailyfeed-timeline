package click.dailyfeed.timeline.domain.timeline.api;

import click.dailyfeed.code.domain.timeline.timeline.dto.TimelineDto;
import click.dailyfeed.code.global.web.response.DailyfeedScrollResponse;
import click.dailyfeed.timeline.domain.timeline.service.TimelineService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/timeline")
public class TimelineController {
    private final TimelineService timelineService;

    @GetMapping("/posts/followings")
    public DailyfeedScrollResponse<TimelineDto.TimelinePostActivity> getMyFollowingMembersTimeline(
            @RequestHeader("Authorization") String token,
            HttpServletResponse response,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable
    ){
        return timelineService.getMyFollowingMembersTimeline(pageable, token, response);
    }


    // 추천피드 이런거 하고 싶은데, 지금 시간이 많지 않다. 프론트엔드 까지 개발 완료 후에 검토해보기

}
