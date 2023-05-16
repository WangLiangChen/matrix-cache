package wang.liangchen.matrix.cache.sdk.cache;

import org.springframework.cache.Cache;

import java.time.Duration;
import java.util.Set;

/**
 * @author LiangChen.Wang 2021/3/22
 */
public interface MatrixCache extends Cache {

    Set<Object> keys();

    boolean containsKey(Object key);

    Duration getTtl();
}
