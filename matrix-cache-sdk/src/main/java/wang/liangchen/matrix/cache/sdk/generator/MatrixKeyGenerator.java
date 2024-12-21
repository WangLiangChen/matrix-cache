package wang.liangchen.matrix.cache.sdk.generator;

import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;

/**
 * @author Liangchen.Wang
 */
public class MatrixKeyGenerator implements KeyGenerator {
    @Override
    public Object generate(Object target, Method method, Object... params) {
        return new MatrixCacheKey(target, method, params);
    }
}
