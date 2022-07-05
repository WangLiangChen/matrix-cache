package wang.liangchen.matrix.easycache.sdk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import wang.liangchen.matrix.easycache.sdk.override.EnableMatrixCaching;

/**
 * @author LiangChen.Wang
 */
@SpringBootApplication
public class EasyCacheSdkInitializer {
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(EasyCacheSdkInitializer.class);
        springApplication.run(args);
    }
}
