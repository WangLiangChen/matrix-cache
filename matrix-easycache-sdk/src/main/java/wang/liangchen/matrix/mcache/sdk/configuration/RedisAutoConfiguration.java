package wang.liangchen.matrix.mcache.sdk.configuration;


import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import wang.liangchen.matrix.framework.commons.utils.PrettyPrinter;
import wang.liangchen.matrix.framework.springboot.context.ConfigurationContext;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * @author LiangChen.Wang
 */
public class RedisAutoConfiguration extends org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration {
    //通过@Primary注解覆盖掉通过配置文件生成的RedisProperties
    @Bean
    @Primary
    public RedisProperties loadRedisProperties() {
        PrettyPrinter.INSTANCE.buffer("create primary 'RedisProperties' from 'redis.properties'");
        org.apache.commons.configuration2.Configuration configuration = ConfigurationContext.INSTANCE.resolve("redis.properties");
        String[] nodes = configuration.getStringArray("cluster.nodes");
        configuration.clearProperty("cluster.nodes");
        //动态绑定参数
        MapConfigurationPropertySource source = new MapConfigurationPropertySource();
        Iterator<String> keys = configuration.getKeys();
        keys.forEachRemaining(k -> source.put(k, configuration.getProperty(k)));
        if (nodes.length == 1) {
            String node = nodes[0];
            int index = node.indexOf(':');
            source.put("host", node.substring(0, index));
            source.put("port", Integer.parseInt(node.substring(index + 1)));
        } else {
            source.put("cluster.nodes", Arrays.stream(nodes).collect(Collectors.joining(",")));
        }

        Binder binder = new Binder(source);
        RedisProperties redisProperties = binder.bind(ConfigurationPropertyName.EMPTY, Bindable.of(RedisProperties.class)).get();
        return redisProperties;
    }

    @Bean
    public RedisTemplate<String, Object> stringKeyRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }
}
