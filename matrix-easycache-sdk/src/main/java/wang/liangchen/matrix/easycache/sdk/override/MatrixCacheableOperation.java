package wang.liangchen.matrix.easycache.sdk.override;

import java.time.Duration;

/**
 * @author LiangChen.Wang 2021/3/19
 */
class MatrixCacheableOperation extends org.springframework.cache.interceptor.CacheableOperation {

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
        protected StringBuilder getOperationDescription() {
            StringBuilder sb = super.getOperationDescription();
            sb.append(" | ttl='");
            sb.append(this.ttl);
            sb.append("'");
            return sb;
        }

        @Override
        public MatrixCacheableOperation build() {
            return new MatrixCacheableOperation(this);
        }
    }

}