package wang.liangchen.matrix.mcache.sdk.annotation;

import org.apache.commons.configuration2.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import wang.liangchen.matrix.framework.commons.exception.AssertUtil;
import wang.liangchen.matrix.framework.commons.exception.MatrixInfoException;
import wang.liangchen.matrix.framework.commons.network.NetUtil;
import wang.liangchen.matrix.framework.commons.utils.PrettyPrinter;
import wang.liangchen.matrix.framework.commons.utils.StringUtil;
import wang.liangchen.matrix.framework.springboot.context.ConfigurationContext;
import wang.liangchen.matrix.mcache.sdk.configuration.RedisAutoConfiguration;
import wang.liangchen.matrix.mcache.sdk.enumeration.CacheStatus;

import java.lang.annotation.*;

/**
 * @author LiangChen.Wang
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({EnableRedis.RedisImportSelector.class})
public @interface EnableRedis {
    class RedisImportSelector implements ImportSelector {
        private final int TIMEOUT = 30 * 1000;
        private final Configuration configuration = ConfigurationContext.INSTANCE.resolve("redis.properties");
        private static boolean loaded = false;

        @Override
        public String[] selectImports(AnnotationMetadata annotationMetadata) {
            if (loaded) {
                return new String[0];
            }
            PrettyPrinter.INSTANCE.buffer("@EnableRedis match: {}", annotationMetadata.getClassName());
            //判断是否集群配置
            String[] nodes = configuration.getStringArray("cluster.nodes");
            AssertUtil.INSTANCE.notEmpty(nodes, "The configuration 'nodes' is required!");
            PrettyPrinter.INSTANCE.buffer("Redis is {} mode......", nodes.length > 1 ? "Cluster" : "Standalone");
            for (String node : nodes) {
                int index = node.indexOf(':');
                connectableCheck(node.substring(0, index), Integer.parseInt(node.substring(index + 1)));
            }
            String[] imports = new String[]{RedisAutoConfiguration.class.getName()};
            PrettyPrinter.INSTANCE.buffer("connect Redis success");
            loaded = true;
            // 设置全局redis状态
            CacheStatus.INSTANCE.setDistributedCacheEnable(true);
            PrettyPrinter.INSTANCE.flush();
            return imports;
        }

        private void connectableCheck(String host, int port) {
            if (StringUtil.INSTANCE.isBlank(host) || port <= 0) {
                throw new MatrixInfoException("Redis configuration error,Please check,host;{},port:{}", host, port);
            }
            boolean connectable = NetUtil.INSTANCE.isConnectable(host, port, TIMEOUT);
            if (!connectable) {
                throw new MatrixInfoException("connect redis failure，Please check，host;{},port:{}", host, port);
            }
        }
    }
}
