package wang.liangchen.matrix.cache.sdk.consistency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import wang.liangchen.matrix.cache.sdk.cache.mlc.MultilevelCacheManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Liangchen.Wang 2022-07-21 14:49
 */
public enum CacheSynchronizer {
    INSTANCE;
    private final Logger logger = LoggerFactory.getLogger(CacheSynchronizer.class);
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2, new CustomizableThreadFactory("mx-sync-"));
    private final DateTimeFormatter EVICT_QUEUE_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private MultilevelCacheManager multilevelCacheManager;
    private RedisTemplate<Object, Object> redisTemplate;

    public static final String EVICT_MESSAGE_TOPIC = "MatrixCacheEvictMessage";
    private final String EMPTY_STRING = "";
    public static final String EVICT_MESSAGE_SPLITTER = "-|-";
    private final long PULL_DELAY_SECONDS = 5;

    private Long offset;
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
        startEvictQueueTimer();
    }

    public void sendMessage(String name) {
        sendMessage(name, null);
    }

    public void sendMessage(String name, String key) {
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
        pushTimestamp = System.currentTimeMillis();
    }

    private void initQueue() {
        String evictQueueKey = LocalDateTime.now().format(EVICT_QUEUE_KEY_FORMATTER);
        this.evictQueue = this.redisTemplate.boundListOps(evictQueueKey);
        this.offset = this.evictQueue.size();
        logger.debug("inited evictQueueKey:{}, offset:{}", evictQueueKey, offset);
    }

    private void startEvictQueueTimer() {
        executorService.scheduleWithFixedDelay(() -> {
            pullEvictQueue();
        }, 0, 5, TimeUnit.SECONDS);
    }


    private synchronized String switchQueue() {
        // 当前年月日
        String nowKey = LocalDateTime.now().format(EVICT_QUEUE_KEY_FORMATTER);
        String evictQueueKey = this.evictQueue.getKey().toString();
        // 无需切换队列
        if (nowKey.equals(evictQueueKey)) {
            logger.debug("skip switch queue from:{} to:{}", evictQueueKey, nowKey);
            return evictQueueKey;
        }
        // 切换队列
        logger.debug("switch queue from:{} to:{}", evictQueueKey, nowKey);
        startPreviousEvictQueueTimer(evictQueue, offset);
        evictQueueKey = nowKey;
        this.evictQueue = this.redisTemplate.boundListOps(evictQueueKey);
        this.offset = 0L;
        return evictQueueKey;
    }

    private synchronized void pullEvictQueue() {
        String evictQueueKey = switchQueue();
        // 拉取当前队列
        Long size = this.evictQueue.size();
        logger.debug("pull from evictQueue, key:{}, offset:{}, size:{}", evictQueueKey, this.offset, size);
        if (size > this.offset) {
            List<Object> messages = this.evictQueue.range(offset, size - 1);
            logger.debug("pulled from evictQueue, key:{}, messages:{}", evictQueueKey, messages.toString());
            multilevelCacheManager.handleEvictedKeys(messages);
            this.offset = size;
        }
        pullTimestamp = System.currentTimeMillis();
    }

    private void startPreviousEvictQueueTimer(BoundListOperations<Object, Object> evictQueue, Long offset) {
        BoundListOperations<Object, Object> previousEvictQueue = evictQueue;
        String previousEvictQueueKey = previousEvictQueue.getKey().toString();
        AtomicLong atomicOffset = new AtomicLong(offset);
        // 开启定时
        ScheduledFuture<?>[] scheduledFutures = {null};
        scheduledFutures[0] = executorService.scheduleWithFixedDelay(() -> {
            Long size = previousEvictQueue.size();
            long previousOffset = atomicOffset.get();
            logger.debug("pull from previousEvictQueue, key:{}, offset:{}, size:{}", previousEvictQueueKey, previousOffset, size);
            if (size > previousOffset) {
                List<Object> messages = previousEvictQueue.range(previousOffset, size - 1);
                logger.debug("pulled from previousEvictQueue, key:{}, messages:{}", previousEvictQueueKey, messages.toString());
                multilevelCacheManager.handleEvictedKeys(messages);
                atomicOffset.getAndSet(size);
            }
            // 延时拉取20S后,停止timer
            LocalDateTime zero = LocalDate.parse(previousEvictQueueKey, EVICT_QUEUE_KEY_FORMATTER).atStartOfDay();
            zero = zero.plusDays(1).plusSeconds(20);
            if (LocalDateTime.now().isAfter(zero)) {
                logger.debug("delete previousEvictQueue, key:{}, offset:{}, size:{}", previousEvictQueueKey, previousOffset, size);
                this.redisTemplate.delete(previousEvictQueueKey);
                // 停止timer
                scheduledFutures[0].cancel(true);
            }
        }, 0, PULL_DELAY_SECONDS, TimeUnit.SECONDS);
    }

}
