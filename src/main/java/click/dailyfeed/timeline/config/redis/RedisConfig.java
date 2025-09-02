package click.dailyfeed.timeline.config.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Primary
    @Bean
    RedisTemplate<String, Object> polymorphicRedisTemplate(
            RedisConnectionFactory redisConnectionFactory,
            @Qualifier("polymorphicObjectMapper") ObjectMapper polymorphicObjectMapper
    ){
        RedisTemplate<String, Object> polymorphicRedisTemplate = new RedisTemplate<>();
        polymorphicRedisTemplate.setConnectionFactory(redisConnectionFactory);
        polymorphicRedisTemplate.setKeySerializer(new StringRedisSerializer());
        polymorphicRedisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer(polymorphicObjectMapper));
        return polymorphicRedisTemplate;
    }

//    @Bean
//    RedisTemplate<String, BookRankingCache> bookRankingRedisTemplate(
//            RedisConnectionFactory redisConnectionFactory,
//            @Qualifier("bookObjectMapper") ObjectMapper bookObjectMapper
//    ) {
//        RedisTemplate<String, BookRankingCache> bookRedisTemplate = new RedisTemplate<>();
//        bookRedisTemplate.setConnectionFactory(redisConnectionFactory);
//        bookRedisTemplate.setKeySerializer(new StringRedisSerializer());
//        bookRedisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(bookObjectMapper, Book.class));
//        return bookRedisTemplate;
//    }
}
