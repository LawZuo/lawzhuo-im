package law.implatform.aspect;

import cn.hutool.core.util.StrUtil;
import law.implatform.annotation.RedisLock;
import law.implatform.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 分布式锁切面。
 *
 * <p>顺序说明：{@code @Order(0)} 比 {@code @Transactional}（默认 LOWEST_PRECEDENCE）更靠外，
 * 保证"先加锁 → 再开事务 → 事务提交后再释放锁"。
 *
 * <p>tryLock 语义：
 * <ul>
 *   <li>{@code waitTime} = 拿不到锁最多等多久，超时抛业务异常</li>
 *   <li>{@code leaseTime} = 锁多久自动释放，-1 表示看门狗自动续期</li>
 * </ul>
 */
@Slf4j
@Aspect
@Order(0)
@Component
@RequiredArgsConstructor
public class RedisLockAspect {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer DISCOVERER = new DefaultParameterNameDiscoverer();

    private final RedissonClient redissonClient;

    @Around("@annotation(redisLock)")
    public Object around(ProceedingJoinPoint joinPoint, RedisLock redisLock) throws Throwable {
        String key = parseKey(joinPoint, redisLock);
        String lockKey = StrUtil.isEmpty(key) ? redisLock.prefixKey()
                : StrUtil.join(":", redisLock.prefixKey(), key);

        RLock lock = redissonClient.getLock(lockKey);
        boolean locked;
        try {
            // 注意：lock(leaseTime, unit) 的第一个参数是"租期"而不是"等待时间"，必须用 tryLock
            locked = lock.tryLock(redisLock.waitTime(), redisLock.leaseTime(), redisLock.unit());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GlobalException("获取分布式锁被中断，请稍后重试");
        }
        if (!locked) {
            log.warn("获取分布式锁失败, key={}, waitTime={}", lockKey, redisLock.waitTime());
            throw new GlobalException(redisLock.message());
        }

        try {
            return joinPoint.proceed();
        } finally {
            // 锁可能因租期到期已被自动释放，直接 unlock 会抛 IllegalMonitorStateException
            if (lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    log.warn("释放分布式锁失败, key={}", lockKey, e);
                }
            }
        }
    }

    private String parseKey(ProceedingJoinPoint joinPoint, RedisLock redisLock) {
        String expression = redisLock.key();
        if (StrUtil.isEmpty(expression)) {
            return null;
        }
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String[] params = DISCOVERER.getParameterNames(method);
        if (params == null) {
            // 参数名拿不到时如果静默降级，key 会退化成"所有人共用一把锁"，很难排查
            throw new IllegalStateException("解析分布式锁 key 失败：获取不到方法参数名，请确认编译时开启了 -parameters，method="
                    + method.getName());
        }
        StandardEvaluationContext context = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < params.length && i < args.length; i++) {
            context.setVariable(params[i], args[i]);
        }
        try {
            return PARSER.parseExpression(expression).getValue(context, String.class);
        } catch (Exception e) {
            throw new IllegalStateException("解析分布式锁 key 失败, expression=" + expression, e);
        }
    }
}
