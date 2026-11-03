package law.imserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class IMServerApplication {

    public static void main(String[] args) {
        // 必须把 args 传下去，否则命令行参数（如 --websocket.port）不会生效
        SpringApplication.run(IMServerApplication.class, args);
    }
}
