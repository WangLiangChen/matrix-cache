package wang.liangchen.matrix.easycache.sdk.enumeration;

/**
 * @author LiangChen.Wang
 */
public enum CacheStatus {
    //
    INSTANCE;
    private boolean distributedCacheEnable;

    public boolean isDistributedCacheEnable() {
        return distributedCacheEnable;
    }

    public boolean isNotDistributedCacheEnable() {
        return !distributedCacheEnable;
    }

    public void setDistributedCacheEnable(boolean distributedCacheEnable) {
        this.distributedCacheEnable = distributedCacheEnable;
    }
}
