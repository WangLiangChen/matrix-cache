package wang.liangchen.matrix.cache.sdk.generator;

import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;

/**
 * @author Liangchen.Wang
 */
public class MatrixKeyGenerator implements KeyGenerator {
    @Override
    public Object generate(Object target, Method method, Object... params) {
        return generateKey(params);
    }

    public static Object generateKey(Object... params) {
        if (params.length == 0) {
            return MatrixCacheKey.EMPTY;
        }
        if (params.length == 1) {
            Object param = params[0];
            if (param != null && !param.getClass().isArray()) {
                return param;
            }
        }
        return new MatrixCacheKey(params);
    }
}
