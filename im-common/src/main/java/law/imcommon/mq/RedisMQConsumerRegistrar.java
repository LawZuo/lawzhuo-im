package law.imcommon.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Redis 队列消费者注册器。
 *
 * <p>{@link RedisMQListener} 只是标记注解，真正"定时拉取 + 回调"由这里完成：
 * <ol>
 *   <li>容器启动后收集所有 {@link RedisMQConsumer} 类型的 bean</li>
 *   <li>读取类上的 {@link RedisMQListener}（队列、批量大小、轮询间隔）</li>
 *   <li>按间隔 {@code leftPop} 拉取，并回调 {@code onMessage}</li>
 * </ol>
 *
 * <p>生产端用 {@code rightPush}，这里用 {@code leftPop}，保证 FIFO；
 * 每个 server 的队列 key 会带上自己的 serverId（见 {@code AbstractPullMessageTask}），
 * 所以多实例之间不会互抢消息。
 */
@Slf4j
public class RedisMQConsumerRegistrar implements SmartLifecycle {

    private final ApplicationContext applicationContext;
    private final RedisMQTemplate redisMQTemplate;

    private volatile boolean running = false;
    private ScheduledExecutorService scheduler;

    public RedisMQConsumerRegistrar(ApplicationContext applicationContext, RedisMQTemplate redisMQTemplate) {
        this.applicationContext = applicationContext;
        this.redisMQTemplate = redisMQTemplate;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        Map<String, RedisMQConsumer> consumers = applicationContext.getBeansOfType(RedisMQConsumer.class);
        if (consumers.isEmpty()) {
            log.info("【redis-mq】没有需要消费的队列");
            running = true;
            return;
        }

        scheduler = Executors.newScheduledThreadPool(Math.max(1, consumers.size()), runnable -> {
            Thread thread = new Thread(runnable, "redis-mq-consumer");
            thread.setDaemon(true);
            return thread;
        });

        for (Map.Entry<String, RedisMQConsumer> entry : consumers.entrySet()) {
            RedisMQConsumer<?> consumer = entry.getValue();
            RedisMQListener listener = consumer.getClass().getAnnotation(RedisMQListener.class);
            if (listener == null) {
                log.warn("【redis-mq】bean {} 没有 @RedisMQListener 注解，跳过", entry.getKey());
                continue;
            }
            int batchSize = Math.max(1, listener.batchSize());
            long period = Math.max(1, listener.period());
            scheduler.scheduleWithFixedDelay(
                    () -> poll(consumer, batchSize), period, period, TimeUnit.MILLISECONDS);
            // 注意：不要在这里打印 generateKey()。im-server 的 key 会拼 :{serverId}，
            // 而 SmartLifecycle 早于 CommandLineRunner，此刻 serverId 还是 0，打出来会误导。
            log.info("【redis-mq】注册队列消费者: bean={}, queue={}, batchSize={}, period={}ms",
                    entry.getKey(), listener.queue(), batchSize, period);
        }
        running = true;
    }

    /**
     * 一次拉取 + 回调。任何异常都必须在这里吞掉并记录，
     * 否则定时任务会静默终止（ScheduledExecutorService 的已知行为）。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void poll(RedisMQConsumer consumer, int batchSize) {
        try {
            if (!Boolean.TRUE.equals(consumer.isReady())) {
                // 服务未就绪（例如 Netty 端口还没绑定成功）时先不消费，避免"消费了却推不出去"
                return;
            }
            String key = consumer.generateKey();
            List<Object> messages = redisMQTemplate.opsForList().leftPop(key, batchSize);
            if (messages == null || messages.isEmpty()) {
                return;
            }
            if (supportsBatch(consumer)) {
                consumer.onMessage(messages);
            } else {
                // 子类只覆写了单条回调时逐条分发，避免消息被父类的空实现吞掉
                for (Object message : messages) {
                    consumer.onMessage(message);
                }
            }
        } catch (Throwable e) {
            log.error("【redis-mq】消费队列失败, consumer={}", consumer.getClass().getSimpleName(), e);
        }
    }

    /**
     * 只有子类真正覆写了 {@code onMessage(List)} 才走批量回调。
     */
    private boolean supportsBatch(RedisMQConsumer<?> consumer) {
        try {
            Method method = consumer.getClass().getMethod("onMessage", List.class);
            return method.getDeclaringClass() != RedisMQConsumer.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @Override
    public synchronized void stop() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
            log.info("【redis-mq】队列消费者已停止");
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
