package wang.liangchen.matrix.mcache.sdk.override;

/**
 * @author LiangChen.Wang 2021/3/19
 */
public class CachePutOperation extends org.springframework.cache.interceptor.CachePutOperation {

    private final long ttl;

    public CachePutOperation(Builder builder) {
        super(builder);
        this.ttl = builder.ttl;
    }

    public long getTtl() {
        return ttl;
    }

    public static class Builder extends org.springframework.cache.interceptor.CachePutOperation.Builder {

        private long ttl;

        public void setTtl(long ttl) {
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
        public CachePutOperation build() {
            return new CachePutOperation(this);
        }
    }

}