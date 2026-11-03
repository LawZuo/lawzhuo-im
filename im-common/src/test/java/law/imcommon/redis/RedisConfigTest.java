package law.imcommon.redis;

import law.imcommon.model.IMUserInfo;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * 验证 Redis 序列化配置：不需要真实 Redis 服务即可运行。
 */
class RedisConfigTest {

    private final RedisConfig redisConfig = new RedisConfig();
    private final RedisTemplate<String, Object> template =
            redisConfig.redisTemplate(mock(RedisConnectionFactory.class));

    @Test
    void valueSerializerShouldKeepReadableJson() {
        byte[] bytes = valueSerializer().serialize(new IMUserInfo(1001L, 1));
        String json = new String(bytes, StandardCharsets.UTF_8);

        // 存进 Redis 的是可读 JSON，而不是 JDK 序列化的乱码
        assertTrue(json.contains("\"id\":1001"), json);
        assertTrue(json.contains("\"@class\""), json);
    }

    @Test
    void valueSerializerShouldRestoreOriginalType() {
        IMUserInfo user = new IMUserInfo(1001L, 2);

        Object restored = valueSerializer().deserialize(valueSerializer().serialize(user));

        assertInstanceOf(IMUserInfo.class, restored);
        assertEquals(1001L, ((IMUserInfo) restored).getId());
        assertEquals(2, ((IMUserInfo) restored).getTerminal());
    }

    @Test
    void valueSerializerShouldSupportJavaTime() {
        Map<String, Object> value = new HashMap<>();
        LocalDateTime now = LocalDateTime.of(2026, 10, 28, 12, 30, 15);
        value.put("lastLoginTime", now);

        Object restored = valueSerializer().deserialize(valueSerializer().serialize(value));

        assertInstanceOf(Map.class, restored);
        assertEquals(now, ((Map<?, ?>) restored).get("lastLoginTime"));
    }

    @Test
    void keySerializerShouldBePlainString() {
        byte[] bytes = stringKeySerializer().serialize("im:user:1001");

        assertEquals("im:user:1001", new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    void redisUtilsBeanShouldBeProvided() {
        RedisUtils redisUtils = redisConfig.redisUtils(template);

        assertNotNull(redisUtils);
        assertEquals(template, redisUtils.getRedisTemplate());
    }

    @SuppressWarnings("unchecked")
    private RedisSerializer<Object> valueSerializer() {
        return (RedisSerializer<Object>) template.getValueSerializer();
    }

    @SuppressWarnings("unchecked")
    private RedisSerializer<String> stringKeySerializer() {
        return (RedisSerializer<String>) template.getKeySerializer();
    }
}
