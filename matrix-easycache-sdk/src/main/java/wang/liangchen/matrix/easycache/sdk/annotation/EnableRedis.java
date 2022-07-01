package wang.liangchen.matrix.easycache.sdk.annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import wang.liangchen.matrix.easycache.sdk.enumeration.CacheStatus;

import java.lang.annotation.*;

/**
 * @author LiangChen.Wang
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({EnableRedis.RedisImportSelector.class})
public @interface EnableRedis {
    class RedisImportSelector implements ImportSelector, EnvironmentAware {
        private final Logger logger = LoggerFactory.getLogger(RedisImportSelector.class);
        private Environment environment;

        @Override
        public String[] selectImports(AnnotationMetadata annotationMetadata) {
            logger.info("@EnableRedis match: {}", annotationMetadata.getClassName());
            //判断是否集群配置
            String nodes = environment.getProperty("cluster.nodes");


            String[] imports = new String[]{RedisAutoConfiguration.class.getName()};
            // 设置全局redis状态
            CacheStatus.INSTANCE.setDistributedCacheEnable(true);
            return imports;
        }

        @Override
        public void setEnvironment(Environment environment) {
            this.environment = environment;
        }
    }
}
