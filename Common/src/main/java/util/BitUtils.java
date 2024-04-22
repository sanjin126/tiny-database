package util;

import annotation.UnsignedInt;

public class BitUtils {
    /**
     * 使用的是无符号右移，对于正数是正确的，对于负数待验证 TODO verify
     * @param number the processing number
     * @param nBits is how many highest bits you want
     * @return
     */
    public static @UnsignedInt int highestNbits(@UnsignedInt int number, int nBits) {
        assert nBits > 0 && Integer.SIZE > nBits;
        return number >>> (Integer.SIZE - nBits);
    }

    public static void main(String[] args) {
        System.out.println(BitUtils.highestNbits(-1, 1));
    }
}
