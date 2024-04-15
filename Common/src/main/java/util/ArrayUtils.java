package util;

public class ArrayUtils {
    public static void makeEmpty(char[] arr) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = Character.MIN_VALUE;
        }
    }

    public static void makeEmpty(byte[] arr) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = 0;
        }
    }
}
