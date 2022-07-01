package wang.liangchen.matrix.easycache.cache;

import java.io.Serializable;

public final class NullValue implements Serializable {


    public static final Object INSTANCE = new NullValue();

    private static final long serialVersionUID = 1L;


    private NullValue() {
    }

    private Object readResolve() {
        return INSTANCE;
    }


    @Override
    public boolean equals(Object obj) {
        return (this == obj || obj == null);
    }

    @Override
    public int hashCode() {
        return NullValue.class.hashCode();
    }

    @Override
    public String toString() {
        return "null";
    }

}