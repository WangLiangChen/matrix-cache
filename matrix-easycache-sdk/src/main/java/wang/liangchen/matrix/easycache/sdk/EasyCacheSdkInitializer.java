package wang.liangchen.matrix.easycache.sdk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * @author LiangChen.Wang
 */
@SpringBootApplication
@EnableCaching
public class EasyCacheSdkInitializer {
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(EasyCacheSdkInitializer.class);
        springApplication.run(args);
    }
}
