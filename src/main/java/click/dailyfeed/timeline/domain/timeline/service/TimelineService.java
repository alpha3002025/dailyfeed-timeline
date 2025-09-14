package click.dailyfeed.timeline.domain.timeline.service;

import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.timeline.timeline.dto.TimelineDto;
import click.dailyfeed.code.domain.timeline.timeline.predicate.PushPullPredicate;
import click.dailyfeed.code.global.web.response.DailyfeedScrollPage;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.feign.domain.member.MemberFeignHelper;
import click.dailyfeed.timeline.domain.post.repository.mongo.PostActivityMongoRepository;
import click.dailyfeed.timeline.domain.timeline.mapper.TimelineMapper;
import click.dailyfeed.timeline.domain.timeline.redis.TimelinePostActivityRedisService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class TimelineService {
    private final PostActivityMongoRepository postActivityMongoRepository;
    private final MemberFeignHelper memberFeignHelper;
    private final TimelinePostActivityRedisService timelinePostActivityRedisService;
    private final TimelinePullService timelinePullService;
    private final TimelineMapper timelineMapper;

    @Value("${dailyfeed.services.timeline.push-pull.limit}")
    private Integer pushPullLimit;

    public DailyfeedServerResponse<DailyfeedScrollPage<TimelineDto.TimelinePostActivity>> getMyFollowingMembersTimeline(Long memberId, Pageable pageable, String token, HttpServletResponse httpServletResponse) {
        MemberProfileDto.MemberProfile member = memberFeignHelper.getMemberProfileById(memberId, token, httpServletResponse);

        if(PushPullPredicate.PUSH.equals(checkPushOrPull(member.getFollowingsCount()))){
            // (1)
            // redis 에서 조회
            List<TimelineDto.TimelinePostActivity> topNResult = timelinePostActivityRedisService.topN(member.getId(), pageable);

            // (2)
            // 부족할 경우 pull 데이터로 보완
            if(topNResult.size() >= pageable.getPageSize()){
                List<TimelineDto.TimelinePostActivity> pullActivities = timelinePullService.listMyFollowingActivities(member.getId(), pageable.getPageNumber(), pageable.getPageSize(), 24, token, httpServletResponse);
                List<TimelineDto.TimelinePostActivity> timelinePostActivities = mergeFeedsWithoutDuplicate(topNResult, pullActivities, pageable.getPageSize());
                DailyfeedScrollPage<TimelineDto.TimelinePostActivity> slice = timelineMapper.fromTimelineList(timelinePostActivities, pageable);
                return DailyfeedServerResponse.<DailyfeedScrollPage<TimelineDto.TimelinePostActivity>>builder()
                        .data(slice)
                        .ok("Y")
                        .reason("SUCCESS")
                        .statusCode("200")
                        .build();
            }

            return DailyfeedServerResponse.<DailyfeedScrollPage<TimelineDto.TimelinePostActivity>>builder()
                    .data(timelineMapper.fromTimelineList(topNResult, pageable))
                    .ok("Y").reason("SUCCESS").statusCode("200")
                    .build();
        }
        else{ // 실제 데이터를 그대로 pull 해온다.
            List<TimelineDto.TimelinePostActivity> pullActivities = timelinePullService.listHeavyMyFollowingActivities(member, pageable, token, httpServletResponse);
            return DailyfeedServerResponse.<DailyfeedScrollPage<TimelineDto.TimelinePostActivity>>builder()
                    .data(timelineMapper.fromTimelineList(pullActivities, pageable))
                    .ok("Y").reason("SUCCESS").statusCode("200")
                    .build();
        }
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


}
