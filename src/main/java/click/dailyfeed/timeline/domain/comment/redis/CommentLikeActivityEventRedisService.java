package click.dailyfeed.timeline.domain.comment.redis;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CommentLikeActivityEventRedisService {
    @Value("${infrastructure.redis.event-queue.comment-like-activity-event.list-key}")
    private String redisKey;

    @Value("${infrastructure.redis.event-queue.comment-like-activity-event.dead-letter-list-key}")
    private String deadLetterKey;

    @Value("${infrastructure.redis.event-queue.comment-like-activity-event.batch-size}")
    private Integer batchSize;

    @Qualifier("commentLikeActivityEventRedisTemplate")
    private final RedisTemplate<String, CommentDto.LikeActivityEvent> redisTemplate;

    public void rPushEvent(CommentDto.LikeActivityEvent likeActivityEvent) {
        redisTemplate.opsForList().rightPush(redisKey, likeActivityEvent);
    }

    public List<CommentDto.LikeActivityEvent> lPopList() {
        List<CommentDto.LikeActivityEvent> result = redisTemplate.opsForList().leftPop(redisKey, batchSize);
        return result != null? result : List.of();
    }

    public void rPushDeadLetterEvent(List<CommentDto.LikeActivityEvent> postActivityEvent) {
        redisTemplate.opsForList().rightPushAll(deadLetterKey, postActivityEvent);
    }

    public List<CommentDto.LikeActivityEvent> lPopDeadLetterList() {
        List<CommentDto.LikeActivityEvent> result = redisTemplate.opsForList().rightPop(deadLetterKey, batchSize);
        return result != null? result : List.of();
    }
}
