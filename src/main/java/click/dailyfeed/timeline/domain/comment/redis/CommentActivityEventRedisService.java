package click.dailyfeed.timeline.domain.comment.redis;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CommentActivityEventRedisService {
    @Value("${infrastructure.redis.event-queue.comment-activity-event.list-key}")
    private String redisKey;

    @Value("${infrastructure.redis.event-queue.comment-activity-event.dead-letter-list-key}")
    private String deadLetterKey;

    @Value("${infrastructure.redis.event-queue.comment-activity-event.batch-size}")
    private Integer batchSize;

    @Qualifier("commentActivityEventRedisTemplate")
    private final RedisTemplate<String, CommentDto.CommentActivityEvent> redisTemplate;


    public void rPushEvent(CommentDto.CommentActivityEvent commentActivityEvent) {
        redisTemplate.opsForList().rightPush(redisKey, commentActivityEvent);
    }

    public List<CommentDto.CommentActivityEvent> lPopList() {
        List<CommentDto.CommentActivityEvent> result = redisTemplate.opsForList().leftPop(redisKey, batchSize);
        return result != null? result : List.of();
    }

    public void rPushDeadLetterEvent(List<CommentDto.CommentActivityEvent> postActivityEvent) {
        redisTemplate.opsForList().rightPushAll(deadLetterKey, postActivityEvent);
    }

    public List<CommentDto.CommentActivityEvent> lPopDeadLetterList() {
        List<CommentDto.CommentActivityEvent> result = redisTemplate.opsForList().rightPop(deadLetterKey, batchSize);
        return result != null? result : List.of();
    }

    public Long getQueueSize() {
        Long size = redisTemplate.opsForList().size(redisKey);
        return size != null ? size : 0L;
    }

    public Long getDeadLetterQueueSize() {
        Long size = redisTemplate.opsForList().size(deadLetterKey);
        return size != null ? size : 0L;
    }
}
