package wang.liangchen.matrix.mcache.sdk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author LiangChen.Wang
 */
@SpringBootApplication
public class MCacheInitializer {
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(MCacheInitializer.class);
        springApplication.run(args);
    }
}
