package wang.liangchen.matrix.easycache.sdk.consistency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import wang.liangchen.matrix.easycache.sdk.cache.mlc.MultilevelCacheManager;
import wang.liangchen.matrix.easycache.sdk.cache.redis.MatrixRedisCache;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Liangchen.Wang 2022-07-21 14:49
 */
public enum CacheSynchronizer {
    INSTANCE;
    private final Logger logger = LoggerFactory.getLogger(CacheSynchronizer.class);
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private MultilevelCacheManager multilevelCacheManager;
    private RedisTemplate<Object, Object> redisTemplate;
    private BoundListOperations<Object, Object> evictQueue;

    /**
     * 消息队列的偏移量
     */
    private int offset;
    /**
     * pull方式
     * 最末拉取队列消息时间
     */
    private long pullTimestamp;
    /**
     * 被 push方式
     * 最末接收通知时间
     */
    private long pushedTimestamp;


    public void init(MultilevelCacheManager multilevelCacheManager) {
        this.multilevelCacheManager = multilevelCacheManager;
        this.redisTemplate = multilevelCacheManager.getRedisTemplate();
        if (null == this.redisTemplate) {
            return;
        }

        this.evictQueue = this.redisTemplate.boundListOps(MatrixRedisCache.EVICT_MESSAGE_TOPIC);
        // 启动时同步offset初始化
        this.offset = evictQueue.size().intValue();
        this.pullTimestamp = this.pushedTimestamp = System.currentTimeMillis();
        // 启动5S定时器
        executorService.scheduleWithFixedDelay(() -> {
            // Pub/Sub 断线重连
            reSubscribe();
            // pull keys
            pullEvictedKeys("pull");
            // 凌晨5点获取锁后清空队列
            // ConsistencyStatus.INSTANCE.setOffset(-1);
        }, 5, 5, TimeUnit.SECONDS);
    }

    private void reSubscribe() {
        if (pullTimestamp - pushedTimestamp > 5 * 1000) {

        }
    }

    private void pullEvictedKeys(String action) {
        int size = evictQueue.size().intValue();
        logger.debug("start pull evicted keys, action: {}", action);
        if (offset < size) {
            List<Object> keys = evictQueue.range(offset, size);
            multilevelCacheManager.handleEvictedKeys(keys);
            offset = size;
            pullTimestamp = System.currentTimeMillis();
        }
    }

    public void handleMessage() {
        // 接收到消息
        pullEvictedKeys("pushed");
        pushedTimestamp = System.currentTimeMillis();
    }

}
