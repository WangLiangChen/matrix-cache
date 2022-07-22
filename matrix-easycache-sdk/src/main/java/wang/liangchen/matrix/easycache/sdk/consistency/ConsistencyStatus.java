package wang.liangchen.matrix.easycache.sdk.consistency;

/**
 * @author Liangchen.Wang 2022-07-21 14:49
 */
public enum ConsistencyStatus {
    INSTANCE;
    /**
     * 消息队列的偏移量
     */
    private int offset;
    /**
     * push方式
     * 最末接收通知时间
     */
    private long pushTimestamp;
    /**
     * pull方式
     * 最末拉取队列消息时间
     */
    private long pullTimestamp;

}
