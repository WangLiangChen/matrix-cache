package wang.liangchen.matrix.mcache.sdk.redis;

/**
 * @author LiangChen.Wang
 */
public class CacheMessage {
    private String name;
    private Action action;
    private Object key;

    public static CacheMessage newInstance(String name, Action action) {
        return newInstance(name, action, null);
    }

    public static CacheMessage newInstance(String name, Action action, Object key) {
        CacheMessage cacheMessage = new CacheMessage();
        cacheMessage.name = name;
        cacheMessage.action = action;
        cacheMessage.key = key;
        return cacheMessage;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Object getKey() {
        return key;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public enum Action {
        //
        none, clear, put, evict
    }
}
