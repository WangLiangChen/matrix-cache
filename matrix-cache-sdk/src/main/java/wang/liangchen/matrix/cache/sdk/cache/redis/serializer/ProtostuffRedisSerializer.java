package wang.liangchen.matrix.cache.sdk.cache.redis.serializer;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * @author Liangchen.Wang 2022-10-06 11:30
 */
public class ProtostuffRedisSerializer<T> implements RedisSerializer<T> {
    private static final Schema<ProtostuffWrapper> PROTOSTUFF_WRAPPER_SCHEMA = RuntimeSchema.getSchema(ProtostuffWrapper.class);

    @Override
    public byte[] serialize(T t) throws SerializationException {
        return protostuffSerializer(t);
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        return protostuffDeserializer(bytes);
    }

    private byte[] protostuffSerializer(T object) {
        ProtostuffWrapper<T> wrapper = new ProtostuffWrapper<>();
        wrapper.setObject(object);
        LinkedBuffer allocate = LinkedBuffer.allocate();
        try {
            return ProtostuffIOUtil.toByteArray(wrapper, PROTOSTUFF_WRAPPER_SCHEMA, allocate);
        } finally {
            allocate.clear();
        }
    }

    private T protostuffDeserializer(byte[] bytes) {
        ProtostuffWrapper<T> wrapper = new ProtostuffWrapper<>();
        ProtostuffIOUtil.mergeFrom(bytes, wrapper, PROTOSTUFF_WRAPPER_SCHEMA);
        return wrapper.getObject();
    }

    static class ProtostuffWrapper<T> {
        private T object;

        public T getObject() {
            return object;
        }

        public void setObject(T object) {
            this.object = object;
        }
    }
}
