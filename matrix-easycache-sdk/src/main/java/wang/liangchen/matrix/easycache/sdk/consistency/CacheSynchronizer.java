package wang.liangchen.matrix.easycache.sdk.consistency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import wang.liangchen.matrix.easycache.sdk.cache.mlc.MultilevelCacheManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
    private final DateTimeFormatter evictQueueKeyFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    private MultilevelCacheManager multilevelCacheManager;
    private RedisTemplate<Object, Object> redisTemplate;

    public static final String EVICT_MESSAGE_TOPIC = "MatrixCacheEvictMessage";
    private final String EMPTY_STRING = "";
    public static final String EVICT_MESSAGE_SPLITTER = "-|-";

    private Long previousOffset;
    private String previousEvictQueueKey;
    private BoundListOperations<Object, Object> previousEvictQueue;

    private Long offset;
    private String evictQueueKey;
    private BoundListOperations<Object, Object> evictQueue;

    private long pullTimestamp;
    private long pushTimestamp;


    public void init(MultilevelCacheManager multilevelCacheManager) {
        this.multilevelCacheManager = multilevelCacheManager;
        this.redisTemplate = multilevelCacheManager.getRedisTemplate();
        if (null == this.redisTemplate) {
            return;
        }
        // 初始化队列
        initQueue();
        // 启动5S定时器
        executorService.scheduleWithFixedDelay(() -> {
            pullMessage();
        }, 5, 5, TimeUnit.SECONDS);
    }

    public void sendMessage(String name) {
        sendMessage(name, null);
    }

    public void sendMessage(String name, String key) {
        // 写入队列
        if (null == key) {
            evictQueue.rightPush(name);
        } else {
            evictQueue.rightPush(String.format("%s%s%s", name, EVICT_MESSAGE_SPLITTER, key));
        }
        // 发送Pub/Sub消息
        redisTemplate.convertAndSend(EVICT_MESSAGE_TOPIC, EMPTY_STRING);
    }

    public void handleMessage() {
        logger.debug("receive Pub/Sub message");
        pullMessage();
        pushTimestamp = System.currentTimeMillis();
    }

    private void initQueue() {
        this.evictQueueKey = LocalDateTime.now().format(evictQueueKeyFormatter);
        this.evictQueue = this.redisTemplate.boundListOps(this.evictQueueKey);
        this.offset = this.evictQueue.size();
        logger.debug("inited offset:{}", offset);
    }

    private synchronized void pullMessage() {
        String dateKey = LocalDateTime.now().format(evictQueueKeyFormatter);
        logger.debug("evictQueueKey:{}, dateKey:{}", this.evictQueueKey, dateKey);
        // 切换队列
        if (!dateKey.equals(evictQueueKey)) {
            logger.debug("switch queue from:{} to:{}", this.evictQueueKey, dateKey);
            this.previousEvictQueueKey = this.evictQueueKey;
            this.previousEvictQueue = this.evictQueue;
            this.previousOffset = this.offset;
            this.evictQueueKey = dateKey;
            this.evictQueue = this.redisTemplate.boundListOps(dateKey);
            this.offset = 0L;
        }
        // 继续拉取上一个队列
        if (null != this.previousEvictQueue) {
            Long size = this.previousEvictQueue.size();
            logger.debug("pull from previousEvictQueue, offset:{}, size:{}", this.previousOffset, size);
            if (size > this.previousOffset) {
                List<Object> keys = this.previousEvictQueue.range(offset, size-1);
                logger.debug("pulled from previousEvictQueue, messages:{}", keys.toString());
                multilevelCacheManager.handleEvictedKeys(keys);
                previousOffset = size;
            }
            // 拉取延时20S后,删除上一个队列
            Instant instant = Instant.parse(this.evictQueueKey);
            if (ChronoUnit.SECONDS.between(instant, Instant.now()) > 20) {
                this.redisTemplate.delete(this.previousEvictQueueKey);
                this.previousEvictQueueKey = null;
                this.previousEvictQueue = null;
                this.previousOffset = null;
            }
        }
        // 拉取当前队列
        Long size = this.evictQueue.size();
        logger.debug("pull from evictQueue, offset:{}, size:{}", this.offset, size);
        if (size > this.offset) {
            List<Object> keys = this.evictQueue.range(offset, size-1);
            logger.debug("pulled from evictQueue, messages:{}", keys.toString());
            multilevelCacheManager.handleEvictedKeys(keys);
            this.offset = size;
        }
        pullTimestamp = System.currentTimeMillis();
    }

}
