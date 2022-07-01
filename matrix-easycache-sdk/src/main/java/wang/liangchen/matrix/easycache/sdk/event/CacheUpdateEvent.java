package wang.liangchen.matrix.easycache.sdk.event;

import org.springframework.context.ApplicationEvent;

/**
 * @author LiangChen.Wang 2020/9/23
 */
public class CacheUpdateEvent extends ApplicationEvent {
    private final String cacheName;
    private final String key;
    private final String value;
    private final String methodName;

    public CacheUpdateEvent(Object source, String cacheName, String key, String value, String methodName) {
        super(source);
        this.cacheName = cacheName;
        this.key = key;
        this.value = value;
        this.methodName = methodName;
    }

    public String getCacheName() {
        return cacheName;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getMethodName() {
        return methodName;
    }
}
