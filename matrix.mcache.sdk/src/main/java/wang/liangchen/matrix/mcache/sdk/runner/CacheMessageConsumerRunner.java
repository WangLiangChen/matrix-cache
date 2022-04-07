package wang.liangchen.matrix.mcache.sdk.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import wang.liangchen.matrix.framework.commons.exception.Assert;
import wang.liangchen.matrix.framework.commons.json.JsonUtil;
import wang.liangchen.matrix.mcache.sdk.mlc.MultilevelCache;
import wang.liangchen.matrix.mcache.sdk.mlc.MultilevelCacheManager;
import wang.liangchen.matrix.mcache.sdk.redis.CacheMessage;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * @author LiangChen.Wang at 2021/3/28 17:28
 */
public class CacheMessageConsumerRunner implements ApplicationRunner, DisposableBean {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public static final String EXPIRE_CHANNEL = "channel:stream:expire";
    public static final String EXPIRE_GROUP = "group:expire";
    private final StreamMessageListenerContainer<String, ObjectRecord<String, CacheMessage>> container;

    public CacheMessageConsumerRunner(Executor taskExecutor, CacheManager cacheManager) {
        if (!(cacheManager instanceof MultilevelCacheManager)) {
            this.container = null;
            return;
        }
        MultilevelCacheManager multilevelCacheManager = (MultilevelCacheManager) cacheManager;
        StringRedisTemplate stringRedisTemplate = multilevelCacheManager.getStringRedisTemplate();

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, CacheMessage>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .targetType(CacheMessage.class)
                        .batchSize(10)
                        .executor(taskExecutor)
                        .pollTimeout(Duration.ZERO)
                        .build();
        StreamMessageListenerContainer<String, ObjectRecord<String, CacheMessage>> container = StreamMessageListenerContainer.create(stringRedisTemplate.getConnectionFactory(), options);
        prepareChannelAndGroup(stringRedisTemplate);
        container.receiveAutoAck(Consumer.from(EXPIRE_GROUP, "CacheMessageConsumer"), StreamOffset.create(EXPIRE_CHANNEL, ReadOffset.lastConsumed()), new StreamMessageListener(multilevelCacheManager));
        this.container = container;
    }


    @Override
    public void run(ApplicationArguments args) {
        if (null == this.container) {
            return;
        }
        this.container.start();
    }

    @Override
    public void destroy() {
        if (null == this.container) {
            return;
        }
        this.container.stop();
    }

    static class StreamMessageListener implements StreamListener<String, ObjectRecord<String, CacheMessage>> {
        private final Logger logger = LoggerFactory.getLogger(this.getClass());
        private final MultilevelCacheManager multilevelCacheManager;

        public StreamMessageListener(MultilevelCacheManager multilevelCacheManager) {
            this.multilevelCacheManager = multilevelCacheManager;
        }

        @Override
        public void onMessage(ObjectRecord<String, CacheMessage> message) {
            CacheMessage cacheMessage = message.getValue();
            logger.debug("received CacheMessage:{}", JsonUtil.INSTANCE.toJsonString(cacheMessage));
            String cacheName = cacheMessage.getName();
            Cache cache = multilevelCacheManager.getCache(cacheName);
            if (!(cache instanceof MultilevelCache)) {
                return;
            }
            MultilevelCache multilevelCache = (MultilevelCache) cache;
            CacheMessage.Action action = cacheMessage.getAction();
            switch (action) {
                case evict:
                    multilevelCache.evictLocal(cacheMessage.getKey());
                    break;
                case clear:
                    multilevelCache.clearLocal();
                    break;
                default:
                    break;
            }
            // redisTemplate.opsForStream().acknowledge(EXPIRE_GROUP, message);
        }
    }

    /**
     * 在初始化容器时，如果key对应的stream或者group不存在时会抛出异常，需要提前检查并且初始化
     */
    private void prepareChannelAndGroup(StringRedisTemplate stringRedisTemplate) {
        String status = "OK";
        StreamOperations<String, Object, Object> streamOperations = stringRedisTemplate.opsForStream();
        try {
            StreamInfo.XInfoGroups groups = streamOperations.groups(EXPIRE_CHANNEL);
            if (groups.stream().noneMatch(group -> EXPIRE_GROUP.equals(group.groupName()))) {
                status = streamOperations.createGroup(EXPIRE_CHANNEL, EXPIRE_GROUP);
            }
        } catch (Exception exception) {
            ObjectRecord<String, CacheMessage> record = StreamRecords.newRecord().ofObject(CacheMessage.newInstance("name", CacheMessage.Action.none)).withStreamKey(EXPIRE_CHANNEL);
            RecordId initialRecord = streamOperations.add(record);
            Assert.INSTANCE.notNull(initialRecord, "Cannot initialize stream with key '" + EXPIRE_CHANNEL + "'");
            status = streamOperations.createGroup(EXPIRE_CHANNEL, ReadOffset.from(initialRecord), EXPIRE_GROUP);
        } finally {
            Assert.INSTANCE.isTrue("OK".equals(status), "Cannot create group with name '" + EXPIRE_GROUP + "'");
        }
    }
}
