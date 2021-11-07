package wang.liangchen.matrix.mcache.sdk.mlc;

import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import wang.liangchen.matrix.mcache.sdk.redis.CacheMessage;
import wang.liangchen.matrix.mcache.sdk.override.AbstractCacheManager;
import wang.liangchen.matrix.mcache.sdk.override.Cache;
import wang.liangchen.matrix.mcache.sdk.override.CacheManager;
import wang.liangchen.matrix.mcache.sdk.runner.CacheMessageConsumerRunner;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

/**
 * @author LiangChen.Wang 2021/3/22
 */
public class MultilevelCacheManager extends AbstractCacheManager {
    private final CacheManager localCacheManager, distributedCacheManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final StreamOperations<String, Object, Object> streamOperations;

    public MultilevelCacheManager(CacheManager localCacheManager, CacheManager distributedCacheManager, StringRedisTemplate stringRedisTemplate) {
        this.localCacheManager = localCacheManager;
        this.distributedCacheManager = distributedCacheManager;
        this.stringRedisTemplate = stringRedisTemplate;
        this.streamOperations = stringRedisTemplate.opsForStream();
    }

    @Override
    protected Collection<? extends Cache> loadCaches() {
        return Collections.emptySet();
    }

    @Nullable
    @Override
    protected Cache getMissingCache(String name, long ttl) {
        return new MultilevelCache(name, ttl, this);
    }

    public Cache getLocalCache(String name, long ttl) {
        return localCacheManager.getCache(name, ttl);
    }

    public Cache getDistributedCache(String name, long ttl) {
        return distributedCacheManager.getCache(name, ttl);
    }

    public StringRedisTemplate getStringRedisTemplate() {
        return stringRedisTemplate;
    }

    public void sendCacheMessage(CacheMessage cacheMessage) {
        ObjectRecord<String, CacheMessage> record = StreamRecords.newRecord().ofObject(cacheMessage).withStreamKey(CacheMessageConsumerRunner.EXPIRE_CHANNEL);
        this.streamOperations.add(record);
    }
}
