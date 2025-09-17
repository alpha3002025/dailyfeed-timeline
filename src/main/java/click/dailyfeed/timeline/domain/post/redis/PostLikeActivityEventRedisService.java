package click.dailyfeed.timeline.domain.post.redis;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class PostLikeActivityEventRedisService {
    @Value("${infrastructure.redis.event-queue.post-like-activity-event.list-key}")
    private String redisKey;

    @Value("${infrastructure.redis.event-queue.post-like-activity-event.dead-letter-list-key}")
    private String deadLetterKey;

    @Value("${infrastructure.redis.event-queue.post-like-activity-event.batch-size}")
    private Integer batchSize;

    @Qualifier("postLikeActivityEventRedisTemplate")
    private final RedisTemplate<String, PostDto.LikeActivityEvent> redisTemplate;

    public void rPushEvent(PostDto.LikeActivityEvent likeActivityEvent) {
        redisTemplate.opsForList().rightPush(redisKey, likeActivityEvent);
    }

    public List<PostDto.LikeActivityEvent> lPopList() {
        List<PostDto.LikeActivityEvent> result = redisTemplate.opsForList().leftPop(redisKey, batchSize);
        return result != null? result : List.of();
    }

    public void rPushDeadLetterEvent(List<PostDto.LikeActivityEvent> postActivityEvent) {
        redisTemplate.opsForList().rightPushAll(deadLetterKey, postActivityEvent);
    }

    public List<PostDto.LikeActivityEvent> lPopDeadLetterList() {
        List<PostDto.LikeActivityEvent> result = redisTemplate.opsForList().rightPop(deadLetterKey, batchSize);
        return result != null? result : List.of();
    }
}
