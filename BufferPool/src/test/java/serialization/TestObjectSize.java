package serialization;

import annotation.UnsignedInt;
import impletation.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import storage.page.ExtendibleHTableBucketPage;
import util.TypeUtils;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

import static config.DBConfig.BUSTUB_PAGE_SIZE;

public class TestObjectSize {
    private static final @UnsignedInt int HTABLE_BUCKET_PAGE_METADATA_SIZE = Integer.BYTES * 2;
    private static @UnsignedInt int HTableBucketArraySize(@UnsignedInt int mapping_type_size) {
        return (BUSTUB_PAGE_SIZE - HTABLE_BUCKET_PAGE_METADATA_SIZE) / mapping_type_size;
    }
    private static ExtendibleHTableBucketPage getBucketPage(int sizeOfKeyType, int sizeOfValType) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<ExtendibleHTableBucketPage> constructor = ExtendibleHTableBucketPage.class.getDeclaredConstructor(Integer.TYPE, Integer.TYPE);
        constructor.setAccessible(true);
        ExtendibleHTableBucketPage bucketPage = constructor.newInstance(sizeOfKeyType, sizeOfValType);
        return bucketPage;
    }
    @Test
    public void testSizeOfExtendibleHTableBucketPage() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        int sizeOfKeyType = 1;
        int sizeOfValType = 1;
        ExtendibleHTableBucketPage<Byte, Byte> bucketPage = getBucketPage(sizeOfKeyType, sizeOfValType);
        // 获取一个新的对象后，先进行init
        bucketPage.init();
        try ( ByteArrayOutputStream bos = new ByteArrayOutputStream();
              ObjectOutputStream oos = new ObjectOutputStream(bos)
        ) {
            oos.writeObject(bucketPage);
            Assertions.assertEquals(HTableBucketArraySize(sizeOfKeyType + sizeOfValType), bos.size());
        }
    }


    @Test
    public void testGenericUseOfBucketPage() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ExtendibleHTableBucketPage<Byte, Integer> bucketPage = getBucketPage(1, 1);
        bucketPage.init();
        Pair<Byte, Integer> bytePair = bucketPage.EntryAt(0);
        System.out.println(bytePair);
        System.out.println(bucketPage.Insert((byte) 1, 2999, Byte::compare));
        System.out.println(bucketPage.EntryAt(0));
        ExtendibleHTableBucketPage<Byte, Byte> temp = TypeUtils.getAs(ExtendibleHTableBucketPage.class, bucketPage);
        Byte b = temp.ValueAt(0);
    }
}
