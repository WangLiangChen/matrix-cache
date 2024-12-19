package wang.liangchen.matrix.cache.sdk.cache.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wang.liangchen.matrix.cache.sdk.consistency.RedisSynchronizer;


public class RedisMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(RedisMessageListener.class);

    public void handleMessage(Object message) {
        logger.debug("Received the cache clearing message published by Redis");
        RedisSynchronizer.INSTANCE.handleMessage();
    }
}
