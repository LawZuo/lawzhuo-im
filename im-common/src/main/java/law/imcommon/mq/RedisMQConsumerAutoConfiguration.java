package law.imcommon.mq;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Redis 队列消费者自动配置。
 *
 * <p>im-common 不在各服务的组件扫描范围内，因此这里通过
 * {@code META-INF/spring/...AutoConfiguration.imports} 注册。
 */
@AutoConfiguration
@ConditionalOnClass(RedisMQTemplate.class)
public class RedisMQConsumerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedisMQConsumerRegistrar redisMQConsumerRegistrar(ApplicationContext applicationContext,
                                                             RedisMQTemplate redisMQTemplate) {
        return new RedisMQConsumerRegistrar(applicationContext, redisMQTemplate);
    }
}
