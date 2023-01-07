package wang.liangchen.matrix.cache.sdk.cache.caffeine;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MatrixRemovalListener implements RemovalListener<Object, Object> {
    private RemovalListener<Object, Object> delegate;

    @Override
    public void onRemoval(@Nullable Object key, @Nullable Object value, RemovalCause cause) {
        if (null == delegate) {
            return;
        }
        delegate.onRemoval(key, value, cause);
    }

    protected void registerDelegate(RemovalListener<Object, Object> delegate) {
        this.delegate = delegate;
    }
}
