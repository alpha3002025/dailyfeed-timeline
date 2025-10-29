package click.dailyfeed.timeline.domain.timeline.redis;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
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
public class TimelinePostsApiRedisService {

    @Qualifier("postDtoPostRedisTemplate")
    private final RedisTemplate<String, PostDto.Post> redisTemplate;

    public List<PostDto.Post> topN(String redisKey, Pageable pageable) {
        ZSetOperations<String, PostDto.Post> zSetOperations = redisTemplate.opsForZSet();

        long start = (long) pageable.getPageNumber() * pageable.getPageSize();
        long end = start + pageable.getPageSize() - 1;

        Set<PostDto.Post> topN = zSetOperations.reverseRange(redisKey, start, end);

        if (topN == null || topN.isEmpty()){
            return List.of();
        }

        return topN.stream().toList();
    }

    public List<PostDto.Post> getList(String redisKey, int pageNumber, int pageSize) {
        return redisTemplate.opsForList().range(redisKey, pageNumber, pageSize);
    }
}
