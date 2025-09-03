package click.dailyfeed.timeline.config.redis;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisObjectMapperConfig {

//    @Bean(name = "polymorphicObjectMapper")
//    public ObjectMapper polymorphicObjectMapper() {
//        PolymorphicTypeValidator polymorphicTypeValidator = BasicPolymorphicTypeValidator
//                .builder()
//                .allowIfSubType(Object.class)
//                .build();
//
//        return new ObjectMapper()
//                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
////                .registerModule(new JavaTimeModule())
//                .activateDefaultTyping(polymorphicTypeValidator, ObjectMapper.DefaultTyping.NON_FINAL)
//                .disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);
//    }

    @Bean(name = "postActivityEventObjectMapper")
    public ObjectMapper postActivityEventObjectMapper(){
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

//    @Bean
//    public ObjectMapper bookObjectMapper(){
//        return new ObjectMapper()
//                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
//                .registerModule(new JavaTimeModule())
//                .disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);
//    }
}
