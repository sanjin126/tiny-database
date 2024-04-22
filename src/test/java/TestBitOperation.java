import org.junit.jupiter.api.Test;

public class TestBitOperation {
    @Test
    public void testInteger() {
        System.out.println(Integer.BYTES);
        System.out.println(Integer.bitCount(Integer.MAX_VALUE));
    }

    @Test
    public void testValue() {
        System.out.println(1 << 9); //512
        System.out.println(1 << Integer.SIZE - 1); //发生溢出
    }

    @Test
    public void testByte2Integer() {
        byte a = -127;
        int b = (int )a;
        System.out.println("b = " + b);
    }
}
