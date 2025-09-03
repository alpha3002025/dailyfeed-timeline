package click.dailyfeed.timeline.domain.post.redis;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PostActivityRedisService {

    @Value("${infrastructure.redis.event-queue.post-activity-event.list-key}")
    private String redisKey;

    @Value("${infrastructure.redis.event-queue.post-activity-event.dead-letter-list-key}")
    private String deadLetterKey;

    @Value("${infrastructure.redis.event-queue.post-activity-event.batch-size}")
    private Integer batchSize;

    @Qualifier("postActivityEventRedisTemplate")
    private RedisTemplate<String, PostDto.PostActivityEvent> redisTemplate;

    public void rPushEvent(PostDto.PostActivityEvent postActivityEvent) {
        redisTemplate.opsForList().rightPush(redisKey, postActivityEvent);
    }

    public List<PostDto.PostActivityEvent> lPopList() {
        List<PostDto.PostActivityEvent> result = redisTemplate.opsForList().leftPop(redisKey, batchSize);
        return result != null? result : List.of();
    }

    public void rPushDeadLetterEvent(List<PostDto.PostActivityEvent> postActivityEvent) {
        redisTemplate.opsForList().rightPushAll(deadLetterKey, postActivityEvent);
    }

    public List<PostDto.PostActivityEvent> lPopDeadLetterList() {
        List<PostDto.PostActivityEvent> result = redisTemplate.opsForList().rightPop(deadLetterKey, batchSize);
        return result != null? result : List.of();
    }
}
