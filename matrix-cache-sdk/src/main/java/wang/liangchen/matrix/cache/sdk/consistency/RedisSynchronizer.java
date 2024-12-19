package wang.liangchen.matrix.cache.sdk.consistency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import wang.liangchen.matrix.cache.sdk.cache.mlc.MultilevelMatrixCacheManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Liangchen.Wang 2022-07-21 14:49
 */
public enum RedisSynchronizer {
    INSTANCE;
    private final Logger logger = LoggerFactory.getLogger(RedisSynchronizer.class);
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2, new CustomizableThreadFactory("mx-sync-"));
    private final DateTimeFormatter EVICT_QUEUE_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private MultilevelMatrixCacheManager multilevelCacheManager;
    private RedisTemplate<Object, Object> redisTemplate;

    public static final String EVICT_MESSAGE_TOPIC = "MatrixCacheEvictMessage";
    private static final String EMPTY_STRING = "";
    public static final String EVICT_MESSAGE_SPLITTER = "-|-";
    private static final long PULL_DELAY_SECONDS = 5;

    private Long offset;
    private BoundListOperations<Object, Object> evictQueue;


    public void init(MultilevelMatrixCacheManager multilevelCacheManager) {
        this.multilevelCacheManager = multilevelCacheManager;
        this.redisTemplate = multilevelCacheManager.getRedisTemplate();
        if (null == this.redisTemplate) {
            return;
        }
        // 初始化队列
        initQueue();
        // 启动5S定时器
        startEvictQueueTimer();
    }

    private void initQueue() {
        String evictQueueKey = LocalDateTime.now().format(EVICT_QUEUE_KEY_FORMATTER);
        this.evictQueue = this.redisTemplate.boundListOps(evictQueueKey);
        this.offset = this.evictQueue.size();
        logger.debug("inited evictQueueKey:{}, offset:{}", evictQueueKey, offset);
    }

    private void startEvictQueueTimer() {
        executorService.scheduleWithFixedDelay(this::pullEvictQueue, 0, PULL_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private synchronized void pullEvictQueue() {
        String evictQueueKey = switchQueue();
        // 拉取当前队列
        Long size = this.evictQueue.size();
        logger.debug("pull from evictQueue, key:{}, offset:{}, size:{}", evictQueueKey, this.offset, size);
        if (size > this.offset) {
            List<Object> messages = this.evictQueue.range(offset, size - 1);
            logger.debug("pulled from evictQueue, key:{}, messages:{}", evictQueueKey, messages);
            multilevelCacheManager.handleEvictedKeys(messages);
            this.offset = size;
        }
    }

    private synchronized String switchQueue() {
        // 当前年月日
        String newKey = LocalDateTime.now().format(EVICT_QUEUE_KEY_FORMATTER);
        String evictQueueKey = this.evictQueue.getKey().toString();
        // 无需切换队列
        if (newKey.equals(evictQueueKey)) {
            logger.debug("skip switch queue from:{} to:{}", evictQueueKey, newKey);
            return evictQueueKey;
        }
        // 切换队列
        logger.debug("switch queue from:{} to:{}", evictQueueKey, newKey);
        startPreviousEvictQueueTimer(evictQueue, offset);
        this.evictQueue = this.redisTemplate.boundListOps(newKey);
        this.offset = 0L;
        return newKey;
    }

    private void startPreviousEvictQueueTimer(BoundListOperations<Object, Object> previousEvictQueue, Long offset) {
        String previousEvictQueueKey = previousEvictQueue.getKey().toString();
        AtomicLong atomicOffset = new AtomicLong(offset);
        // 开启定时
        Future<?>[] futures = new ScheduledFuture[1];
        futures[0] = executorService.scheduleWithFixedDelay(() -> {
            Long size = previousEvictQueue.size();
            long previousOffset = atomicOffset.get();
            logger.debug("pull from previousEvictQueue, key:{}, offset:{}, size:{}", previousEvictQueueKey, previousOffset, size);
            if (size > previousOffset) {
                List<Object> messages = previousEvictQueue.range(previousOffset, size - 1);
                logger.debug("pulled from previousEvictQueue, key:{}, messages:{}", previousEvictQueueKey, messages);
                multilevelCacheManager.handleEvictedKeys(messages);
                atomicOffset.getAndSet(size);
            }
            // 延时拉取30S后,停止timer
            LocalDateTime zero = LocalDate.parse(previousEvictQueueKey, EVICT_QUEUE_KEY_FORMATTER).atStartOfDay();
            zero = zero.plusDays(1).plusSeconds(30);
            if (LocalDateTime.now().isAfter(zero)) {
                logger.debug("delete previousEvictQueue, key:{}, offset:{}, size:{}", previousEvictQueueKey, previousOffset, size);
                this.redisTemplate.delete(previousEvictQueueKey);
                // 停止timer
                futures[0].cancel(true);
            }
        }, 0, PULL_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    public void sendEvictMessage(String name) {
        sendEvictMessage(name, null);
    }

    public void sendEvictMessage(String name, String key) {
        // 切换队列
        String evictQueueKey = switchQueue();
        String message = null == key ? name : String.format("%s%s%s", name, EVICT_MESSAGE_SPLITTER, key);
        // 写入队列
        logger.debug("send message,evictQueueKey:{}, message:{}", evictQueueKey, message);
        evictQueue.rightPush(message);
        // 发送Pub/Sub消息
        redisTemplate.convertAndSend(EVICT_MESSAGE_TOPIC, EMPTY_STRING);
    }

    public void handleMessage() {
        logger.debug("Receive Pub/Sub message");
        pullEvictQueue();
    }

}
