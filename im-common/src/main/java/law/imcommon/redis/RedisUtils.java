package law.imcommon.redis;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis 常用操作封装。
 *
 * <p>由 {@link RedisConfig} 注册为 bean，业务侧直接注入即可：
 * <pre>{@code
 * private final RedisUtils redisUtils;
 * }</pre>
 *
 * <p>值统一走 JSON 序列化，读取时能还原成写入时的原始类型。
 */
public class RedisUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisUtils(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 需要执行封装之外的原生命令时（如 Lua、scan）可通过它拿到模板。 */
    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

    // ==================== 键值 ====================

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, Duration timeout) {
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    /** 仅当 key 不存在时写入，常用于分布式锁、防重复提交。 */
    public boolean setIfAbsent(String key, Object value, Duration timeout) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, timeout));
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 带类型的读取。类型信息在写入时已存进 JSON，这里只做一次断言式转换。
     *
     * @throws ClassCastException 当实际存储的类型与期望类型不一致时（快速失败，避免脏数据被静默当成 null）
     */
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        return value == null ? null : type.cast(value);
    }

    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    public long delete(Collection<String> keys) {
        Long count = redisTemplate.delete(keys);
        return count == null ? 0L : count;
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // ==================== 过期时间 ====================

    public boolean expire(String key, Duration timeout) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout));
    }

    /** 返回 null 表示 key 不存在或未设置过期时间。 */
    public Duration getExpire(String key) {
        Long seconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return (seconds == null || seconds < 0) ? null : Duration.ofSeconds(seconds);
    }

    // ==================== Hash ====================

    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    public <T> T hGet(String key, String field, Class<T> type) {
        Object value = hGet(key, field);
        return value == null ? null : type.cast(value);
    }

    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    public boolean hHasKey(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    public long hDelete(String key, Object... fields) {
        Long count = redisTemplate.opsForHash().delete(key, fields);
        return count == null ? 0L : count;
    }
}
