package click.dailyfeed.timeline.domain.timeline.redis;

import click.dailyfeed.code.domain.timeline.timeline.dto.TimelineDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class TimelinePostActivityRedisService {

    @Qualifier("timelinePostActivityRedisTemplate")
    private final RedisTemplate<String, TimelineDto.TimelinePostActivity> redisTemplate;

    // TODO  구현 단계에서만 잠시 하드코딩으로. (application yaml 에는 이름이 확정됐을때...)
    private final static String TIMELINE_KEY_PREFIX = "timeline-post:member:";

    public List<TimelineDto.TimelinePostActivity> topN(Long memberId, Pageable pageable) {
        String key = TIMELINE_KEY_PREFIX + memberId;
        ZSetOperations<String, TimelineDto.TimelinePostActivity> zSetOperations = redisTemplate.opsForZSet();

        long start = (long) pageable.getPageNumber() * pageable.getPageSize();
        long end = start + pageable.getPageSize() - 1;

        Set<TimelineDto.TimelinePostActivity> topN = zSetOperations.reverseRange(key, start, end);

        if (topN == null || topN.isEmpty()){
            return List.of();
        }

        return topN.stream().toList();
    }

    public List<TimelineDto.TimelinePostActivity> getList(String key, int pageNumber, int pageSize) {
        return redisTemplate.opsForList().range(key, pageNumber, pageSize);
    }
}
