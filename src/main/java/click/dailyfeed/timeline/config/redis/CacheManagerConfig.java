package click.dailyfeed.timeline.config.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

@Configuration
public class CacheManagerConfig {
    @Getter
    @AllArgsConstructor
    public static class CacheProperty{
        String configAlias;
        Integer ttl;
    }

    private final List<CacheProperty> cacheProperties = List.of(
            new CacheProperty("cache1", 30),
            new CacheProperty("cache2", 42)
    );

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
            @Qualifier("polymorphicObjectMapper") ObjectMapper polymorphicObjectMapper
    ) {
        return builder -> {
            cacheProperties.forEach(prop -> {
                RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                        .disableCachingNullValues()
                        .serializeKeysWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(
                                        new StringRedisSerializer()
                                )
                        )
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(
                                        new GenericJackson2JsonRedisSerializer(polymorphicObjectMapper)
                                )
                        )
                        .entryTtl(Duration.ofSeconds(prop.getTtl()));

                builder.withCacheConfiguration(prop.getConfigAlias(), cacheConfig);
            });
        };
    }
}
