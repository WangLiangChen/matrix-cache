package wang.liangchen.matrix.cache.sdk.override;

import org.springframework.lang.NonNull;

import java.time.Duration;

/**
 * @author LiangChen.Wang 2021/3/19
 */
public class MatrixCacheableOperation extends org.springframework.cache.interceptor.CacheableOperation {

    private final Duration ttl;

    public MatrixCacheableOperation(Builder builder) {
        super(builder);
        this.ttl = builder.ttl;
    }
    public Duration getTtl() {
        return ttl;
    }

    public static class Builder extends org.springframework.cache.interceptor.CacheableOperation.Builder {

        private Duration ttl;

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }

        @Override
        @NonNull
        protected StringBuilder getOperationDescription() {
            StringBuilder sb = super.getOperationDescription();
            sb.append(" | ttl='");
            sb.append(this.ttl);
            sb.append("'");
            return sb;
        }

        @Override
        @NonNull
        public MatrixCacheableOperation build() {
            return new MatrixCacheableOperation(this);
        }
    }

}