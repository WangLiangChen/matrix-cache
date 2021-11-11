package wang.liangchen.matrix.mcache.sdk.override;

import java.util.Set;

/**
 * @author LiangChen.Wang 2021/3/22
 */
public interface Cache extends org.springframework.cache.Cache {

    Set<Object> keys();

    boolean containsKey(Object key);

    long getTtl();
}
