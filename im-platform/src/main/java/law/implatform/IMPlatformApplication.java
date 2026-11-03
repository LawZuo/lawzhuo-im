package law.implatform;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 启动类位于 law.implatform，默认只扫描本包；
 * im-client 的 IMClient / IMSender 在 law.imclient 下，必须显式加进来，否则注入 IMClient 会失败。
 * （law.imcommon 下的 RedisConfig / RedisMQConfig 走 META-INF 的自动配置注册，不走包扫描）
 */
@Slf4j
@EnableAspectJAutoProxy(exposeProxy = true)
@ComponentScan(basePackages = {"law.implatform", "law.imclient"})
@MapperScan(basePackages = {"law.implatform.mapper"})
@SpringBootApplication
public class IMPlatformApplication {
    public static void main(String[] args) {
        // 必须把 args 传下去，否则 --server.port / --spring.flyway.enabled 等启动参数全部失效
        SpringApplication.run(IMPlatformApplication.class, args);
    }
}
