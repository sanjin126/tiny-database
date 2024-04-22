package util;

import java.nio.charset.StandardCharsets;

public class ByteUtils {
    public static byte[] fromU16ToU8(byte[] old) {
//        return new String(old, StandardCharsets.UTF_8).getBytes();
        return new String(old, StandardCharsets.UTF_16).getBytes(StandardCharsets.UTF_8);
//        return new String(old).getBytes(StandardCharsets.UTF_8);
        /**
         * 当使用 new String(byte[]) 时，如果不指定Charset，则使用的是defaultCharset，就是UTF8，
         * 所以会出现错误
         */
    }
}
