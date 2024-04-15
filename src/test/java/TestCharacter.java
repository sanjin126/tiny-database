import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class TestCharacter {
    @Test
    public void testChinese() {
        char[] arr = new char[10];
        arr[0] = '你';
        arr[1] = '\u15e3';
        String str = "\uD83D\uDE0A";
        System.out.println(Arrays.toString(arr));
        System.out.println(str);
    }
}
