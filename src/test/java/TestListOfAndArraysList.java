import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class TestListOfAndArraysList {
    @Test
    public void testListOf() {
        Integer[] arr = new Integer[]{1,2,3};
        List<Integer> list = List.of(arr);
        for (int i = 0; i < 3; i++) {
            Assertions.assertTrue(arr[i] == list.get(i));
        }
//        list.set(0,2);
        arr[0] = arr[1] = arr[2];
        System.out.println("Arrays.toString(arr) = " + Arrays.toString(arr));
        System.out.println("list = " + list);
    }

    @Test
    public void testArraysList() {
        Integer[] arr = new Integer[]{1,2,3};
        List<Integer> list = Arrays.asList(arr);
        for (int i = 0; i < 3; i++) {
            Assertions.assertTrue(arr[i] == list.get(i));
        }
        list.set(2,7);
        arr[0] = arr[1] = arr[2];
        System.out.println("Arrays.toString(arr) = " + Arrays.toString(arr));
        System.out.println("list = " + list);
    }

    @Test
    public void testAssert() {
        assert true;
        assert  false;
    }
}
