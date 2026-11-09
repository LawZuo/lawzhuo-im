package law.imcommon.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 创建单例线程池
 */
@Slf4j
public class ThreadPoolExecutorFactory {

    /**
     * 池中允许的最大线程数
     */
    private static final int MAXIMUM_POOL_SIZE = 100;

    /**
     * 机器的CPU核数:Runtime.getRuntime().availableProcessors()
     * corePoolSize 池中所保存的线程数，包括空闲线程。
     * CPU 密集型：核心线程数 = CPU核数 + 1
     * IO 密集型：核心线程数 = CPU核数 * 2
     *
     * <p>注意：常量必须声明在依赖它的常量之前。之前 MAXIMUM_POOL_SIZE 写在下面，
     * 静态初始化时它还是 0，导致 Math.min(0, ...) = 0，核心线程数变成 0。
     */
    private static final int CORE_POOL_SIZE =
            Math.min(MAXIMUM_POOL_SIZE, Runtime.getRuntime().availableProcessors() * 2);

    /**
     * keepAliveTime -当线程数大于核心时，此为终止前多余的空闲线程等待新任务的最长时间
     */
    private static final int KEEP_ALIVE_TIME = 1000;

    /**
     * 线程池对象
     */
    private static volatile ScheduledThreadPoolExecutor threadPoolExecutor = null;

    private ThreadPoolExecutorFactory() {
    }

    /**
     * 双检锁创建线程安全的单例
     */
    public static ScheduledThreadPoolExecutor getThreadPoolExecutor() {
        if (null == threadPoolExecutor) {
            synchronized (ThreadPoolExecutorFactory.class) {
                if (null == threadPoolExecutor) {
                    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
                            //核心线程数
                            CORE_POOL_SIZE,
                            //拒绝策略
                            new ThreadPoolExecutor.CallerRunsPolicy());
                    executor.setKeepAliveTime(KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS);
                    // 任务取消后立即从队列移除，避免已取消任务长期占用队列
                    executor.setRemoveOnCancelPolicy(true);
                    threadPoolExecutor = executor;
                    log.info("线程池初始化完成, corePoolSize={}", CORE_POOL_SIZE);
                }
            }
        }
        return threadPoolExecutor;
    }

    /**
     * 关闭线程池
     */
    public static void shutDown() {
        if (threadPoolExecutor != null) {
            threadPoolExecutor.shutdown();
        }
    }

    public static void execute(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        // 不能用 threadPoolExecutor 字段：首次调用时它还是 null
        getThreadPoolExecutor().execute(runnable);
    }
}
