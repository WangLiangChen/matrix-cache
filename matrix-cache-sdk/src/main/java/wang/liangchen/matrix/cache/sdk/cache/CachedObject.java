package wang.liangchen.matrix.cache.sdk.cache;

import java.io.Serializable;

/**
 * @author Liangchen.Wang 2022-10-03 22:37
 */
public interface CachedObject extends Serializable {
    Object cacheKey();
}
