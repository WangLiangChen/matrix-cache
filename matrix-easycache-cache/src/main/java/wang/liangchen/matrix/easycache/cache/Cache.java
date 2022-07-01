package wang.liangchen.matrix.easycache.cache;

import org.springframework.lang.Nullable;

import java.util.concurrent.Callable;

/**
 * @author Liangchen.Wang
 */
public interface Cache {

    String getName();

    Object getNativeCache();

    ValueWrapper get(Object key);


    <T> T get(Object key, Class<T> type);


    <T> T get(Object key, Callable<T> valueLoader);


    void put(Object key, Object value);


    default ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper existingValue = get(key);
        if (existingValue == null) {
            put(key, value);
        }
        return existingValue;
    }


    void evict(Object key);


    default boolean evictIfPresent(Object key) {
        evict(key);
        return false;
    }


    void clear();


    default boolean invalidate() {
        clear();
        return false;
    }


    @FunctionalInterface
    interface ValueWrapper {
        Object value();
    }

    class SimpleValueWrapper implements ValueWrapper {
        private final Object value;

        public SimpleValueWrapper(Object value) {
            this.value = value;
        }

        @Override
        public Object value() {
            return this.value;
        }
    }

    class ValueRetrievalException extends RuntimeException {
        private final Object key;

        public ValueRetrievalException(Object key, Callable<?> loader, Throwable ex) {
            super(String.format("Value for key '%s' could not be loaded using '%s'", key, loader), ex);
            this.key = key;
        }

        public Object getKey() {
            return this.key;
        }
    }


}
