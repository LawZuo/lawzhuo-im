package law.implatform.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 */
@Retention(RetentionPolicy.RUNTIME)//运行时生效
@Target(ElementType.METHOD)//作用在方法上
public @interface RedisLock {
    /**
     * key的前缀,prefixKey+key就是redis的key
     */
    String prefixKey() ;

    /**
     * spel 表达式，例如 {@code #userId+':'+#friendId}
     */
    String key() default "";

    /**
     * 获取锁的最长等待时间，超时直接抛业务异常。
     * 0 = 不等待（立刻失败）；-1 = 一直等待。
     */
    int waitTime() default 3000;

    /**
     * 锁的租期（leaseTime）。-1 表示启用 Redisson 看门狗，按业务执行时长自动续期（推荐）。
     * 注意：这是"锁多久后自动释放"，不是"等待多久"，别和 waitTime 混用。
     */
    int leaseTime() default -1;

    /**
     * 时间单位，默认毫秒
     */
    TimeUnit unit() default TimeUnit.MILLISECONDS;

    /**
     * 获取锁失败时的提示信息
     */
    String message() default "操作太频繁，请稍后重试";
}
