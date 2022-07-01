package wang.liangchen.matrix.easycache.cache;

public abstract class AbstractValueAdaptingCache implements Cache {
    private final boolean allowNullValues;

    protected AbstractValueAdaptingCache(boolean allowNullValues) {
        this.allowNullValues = allowNullValues;
    }

    public final boolean isAllowNullValues() {
        return this.allowNullValues;
    }

    @Override
    public ValueWrapper get(Object key) {
        Object lookup = lookup(key);
        return toValueWrapper(lookup);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
        Object lookup = lookup(key);
        Object value = fromStoreValue(lookup);
        if (value != null && type != null && !type.isInstance(value)) {
            throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + value);
        }
        return (T) value;
    }

    protected abstract Object lookup(Object key);


    protected Object fromStoreValue(Object storeValue) {
        // 缓存中存储的值 转化为真实值
        if (this.allowNullValues && storeValue == NullValue.INSTANCE) {
            return null;
        }
        return storeValue;
    }

    protected Object toStoreValue(Object userValue) {
        // 真实值 转化为缓存要存储的值
        if (userValue == null) {
            if (this.allowNullValues) {
                return NullValue.INSTANCE;
            }
            throw new IllegalArgumentException("Cache '" + getName() + "' is configured to not allow null values but null was provided");
        }
        return userValue;
    }

    protected ValueWrapper toValueWrapper(Object storeValue) {
        return (storeValue != null ? new SimpleValueWrapper(fromStoreValue(storeValue)) : null);
    }
}
