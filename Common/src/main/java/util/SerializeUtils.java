package util;

import serialization.MyOIS;
import serialization.MyOOS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SerializeUtils {
    /**
     *
     * @param mock 为一个模拟的实例；如果为primitive及其包装类，则可为null;
     * @param cl
     * @param bytes
     * @return
     * @param <T>
     * @throws IOException
     */
    public static <T> T serialize(T mock, Class<T> cl, byte[] bytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             MyOIS<T> ois = new MyOIS<>(bais, cl, mock)){
            return ois.readObjectOverride();
        }
    }

    public static byte[] deserialize(Object object) throws IOException {
        if (object == null) {
            throw new NullPointerException();
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MyOOS oos = new MyOOS(baos)){
            oos.writeObjectOverride(object);
            return baos.toByteArray();
        }
    }
}
