package wang.liangchen.matrix.cache.sdk.generator;

import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author Liangchen.Wang
 */
public class MatrixCacheKey implements Serializable {
    private final Object[] params;
    private transient final int hashCode;

    public MatrixCacheKey(Object target, Method method, Object... elements) {
        this.params = new Object[elements.length + 2];
        this.params[0] = target.getClass().getSimpleName();
        this.params[1] = method.getName();
        System.arraycopy(elements, 0, this.params, 2, elements.length);
        this.hashCode = Arrays.deepHashCode(this.params);
    }

    @Override
    public boolean equals(Object object) {
        return (this == object || (object instanceof MatrixCacheKey &&
                Arrays.deepEquals(this.params, ((MatrixCacheKey) object).params)));
    }

    @Override
    public final int hashCode() {
        return this.hashCode;
    }

    @Override
    public String toString() {
        return StringUtils.arrayToCommaDelimitedString(this.params);
    }
}
