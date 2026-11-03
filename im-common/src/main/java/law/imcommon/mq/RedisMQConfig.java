package law.imcommon.mq;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisMQConfig {

    @Bean
    public RedisMQTemplate redisMQTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisMQTemplate redisMQTemplate = new RedisMQTemplate();
        redisMQTemplate.setConnectionFactory(redisConnectionFactory);
        // 设置值（value）的序列化采用FastJsonRedisSerializer
        redisMQTemplate.setValueSerializer(genericJackson2JsonRedisSerializer());
        redisMQTemplate.setHashValueSerializer(genericJackson2JsonRedisSerializer());
        // 设置键（key）的序列化采用StringRedisSerializer。
        redisMQTemplate.setKeySerializer(new StringRedisSerializer());
        redisMQTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisMQTemplate.afterPropertiesSet();
        return redisMQTemplate;
    }

    @Bean
    public GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 支持 LocalDateTime 等 Java 8 时间类型
        objectMapper.registerModule(new JavaTimeModule());
        // 日期不写成时间戳，写成 ISO 字符串
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 反序列化时遇到未知字段不报错（可选，按需）
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 默认类型信息（GenericJackson2JsonRedisSerializer 内部会用到）
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL);
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
