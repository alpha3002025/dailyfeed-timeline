package click.dailyfeed.timeline.domain.statistics.api;

import click.dailyfeed.code.domain.timeline.statistics.TimelineStatisticsDto;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.timeline.domain.statistics.service.TimelineStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/timeline/statistics")
public class TimelineStatisticsController {
    private final TimelineStatisticsService timelineStatisticsService;

    @GetMapping("/posts/{postId}")
    public DailyfeedServerResponse<TimelineStatisticsDto.PostItemCounts> getPostItemCounts(
            @PathVariable("postId") Long postId
    ) {
        TimelineStatisticsDto.PostItemCounts data = timelineStatisticsService.getPostDetailCounts(postId);
        return DailyfeedServerResponse.<TimelineStatisticsDto.PostItemCounts>builder()
                .data(data)
                .result(ResponseSuccessCode.SUCCESS)
                .status(HttpStatus.OK.value())
                .build();
    }
}
