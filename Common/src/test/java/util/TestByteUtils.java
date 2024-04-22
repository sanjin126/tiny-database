package util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TestByteUtils {
    @Test
    public void testFromUTF16TOUTF8() {
        byte[] bytes = new String("ab").getBytes(StandardCharsets.UTF_16);
        System.out.println(Arrays.toString(bytes));
        System.out.println(Arrays.toString(ByteUtils.fromU16ToU8(bytes)));
        System.out.println(new String(bytes, StandardCharsets.UTF_16));
    }
}
