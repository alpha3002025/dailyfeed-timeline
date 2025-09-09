package click.dailyfeed.timeline.config.redis;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:26379}")
    private Integer redisPort;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(redisHost);
        configuration.setPort(redisPort);
        return new LettuceConnectionFactory(configuration);
    }

//    @Bean
//    RedisTemplate<String, Object> polymorphicRedisTemplate(
//            RedisConnectionFactory redisConnectionFactory,
//            @Qualifier("polymorphicObjectMapper") ObjectMapper polymorphicObjectMapper
//    ){
//        RedisTemplate<String, Object> polymorphicRedisTemplate = new RedisTemplate<>();
//        polymorphicRedisTemplate.setConnectionFactory(redisConnectionFactory);
//        polymorphicRedisTemplate.setKeySerializer(new StringRedisSerializer());
//        polymorphicRedisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer(polymorphicObjectMapper));
//        return polymorphicRedisTemplate;
//    }

    @Bean
    RedisTemplate<String, PostDto.PostActivityEvent>  postActivityEventRedisTemplate(
            RedisConnectionFactory redisConnectionFactory,
            @Qualifier("postActivityEventObjectMapper") ObjectMapper postActivityEventObjectMapper
    ){
        RedisTemplate<String, PostDto.PostActivityEvent> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Jackson2JsonRedisSerializer 설정
        Jackson2JsonRedisSerializer<PostDto.PostActivityEvent> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(
                        postActivityEventObjectMapper,
                        PostDto.PostActivityEvent.class
                );

        // Key는 String으로, Value는 JSON으로 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
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
