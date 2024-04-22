import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class TestMap {
    @Test
    public void testPutIfAbsent() {
        HashMap<Integer, Integer> map = new HashMap<>();
        map.put(1, 1);
        System.out.println(map.putIfAbsent(1, 2));
        System.out.println(map.putIfAbsent(2, 2));
    }
}
