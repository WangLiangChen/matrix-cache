package wang.liangchen.matrix.easycache.sdk.consistency;

/**
 * @author Liangchen.Wang 2022-07-22 4:51
 */
public class CacheEvictMessage implements Message {
    private final String name;
    private final Action action;
    private final Object key;

    public CacheEvictMessage(String name, Action action, Object key) {
        this.name = name;
        this.action = action;
        this.key = key;
    }

    public static CacheEvictMessage newInstance(String name, Action action, Object key) {
        return new CacheEvictMessage(name, action, key);
    }

    public static CacheEvictMessage newInstance(String name, Action action) {
        return new CacheEvictMessage(name, action, null);
    }

    public enum Action {
        evict, clear;
    }

    @Override
    public String toString() {
        return "CacheEvictMessage{" +
                "name='" + name + '\'' +
                ", action=" + action +
                ", key=" + key +
                '}';
    }
}
