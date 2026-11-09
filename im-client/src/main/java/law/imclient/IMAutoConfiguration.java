package law.imclient;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * im-client 组件注册。
 *
 * <p>注意：这里只扫描 {@code law.imclient}，<b>不要</b>把 {@code law.imcommon} 也加进来。
 * im-common 的配置类（{@code law.imcommon.redis.RedisConfig} 等）通过
 * {@code META-INF/spring/...AutoConfiguration.imports} 注册；如果这里再扫一次
 * {@code law.imcommon}，它的 {@code RedisConfig} 会和 {@code law.implatform.config.RedisConfig}
 * 撞同一个 bean 名 {@code redisConfig}，启动直接报 {@code ConflictingBeanDefinitionException}。
 */
@Configuration
@ComponentScan(basePackages = {"law.imclient"})
public class IMAutoConfiguration {
}
