package wang.liangchen.matrix.easycache.sdk.override;

import java.time.Duration;
import java.util.Set;

/**
 * @author LiangChen.Wang 2021/3/22
 */
public interface Cache extends org.springframework.cache.Cache {

    Set<Object> keys();

    boolean containsKey(Object key);

    Duration getTtl();
}
