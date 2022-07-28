package wang.liangchen.matrix.cache.sdk.generator;

import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Liangchen.Wang
 */
public class MatrixCacheKey implements Serializable {
    public static final MatrixCacheKey EMPTY = new MatrixCacheKey();
    private final Object[] params;
    private final int hashCode;

    public MatrixCacheKey(Object... elements) {
        this.params = new Object[elements.length];
        System.arraycopy(elements, 0, this.params, 0, elements.length);
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
        return getClass().getSimpleName() + " [" + StringUtils.arrayToCommaDelimitedString(this.params) + "]";
    }
}
