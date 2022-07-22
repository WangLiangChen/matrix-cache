package wang.liangchen.matrix.easycache.sdk.spring.boot.starter;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
