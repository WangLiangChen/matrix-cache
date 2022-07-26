package wang.liangchen.matrix.cache.sdk.spring.boot.starter;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author LiangChen.Wang
 */
@SpringBootApplication
public class MatrixCacheSdkInitializer {
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(MatrixCacheSdkInitializer.class);
        springApplication.run(args);
    }
}
