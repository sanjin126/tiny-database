package serialization;

import annotation.UnsignedInt;
import config.DBConfig;
import impletation.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import storage.page.ExtendibleHTableBucketPage;
import storage.page.ExtendibleHTableDirectoryPage;
import storage.page.ExtendibleHTableHeaderPage;
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
        int sizeOfKeyType = 8;
        int sizeOfValType = 1;
        ExtendibleHTableBucketPage<Long, Byte> bucketPage = getBucketPage(sizeOfKeyType, sizeOfValType);
        // 获取一个新的对象后，先进行init
        bucketPage.init();
        for (int i = 0; i < HTableBucketArraySize(sizeOfKeyType+sizeOfValType); i++) {
            bucketPage.Insert((long) i, (byte) 1,Long::compare);
        }
        System.out.println(bucketPage.IsFull());
        try ( ByteArrayOutputStream bos = new ByteArrayOutputStream();
              ObjectOutputStream oos = new MyOOS(bos)
        ) {
            oos.writeObject(bucketPage);
            Assertions.assertEquals(BUSTUB_PAGE_SIZE, bos.size());
            MyOIS<ExtendibleHTableBucketPage> ois = new MyOIS<ExtendibleHTableBucketPage>(new ByteArrayInputStream(bos.toByteArray()), ExtendibleHTableBucketPage.class, getBucketPage(sizeOfKeyType, sizeOfValType));
            ExtendibleHTableBucketPage bucketPage1 = ois.readObjectOverride();
            System.out.println(bucketPage1.Size());
        }
    }

    @Test
    public void testSizeOfExtendibleHTableDirectory() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<ExtendibleHTableDirectoryPage> constructor = ExtendibleHTableDirectoryPage.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        ExtendibleHTableDirectoryPage instance = constructor.newInstance();
        instance.init();
        try ( ByteArrayOutputStream bos = new ByteArrayOutputStream();
              ObjectOutputStream oos = new MyOOS(bos)
        ) {
            oos.writeObject(instance);
            Assertions.assertEquals(BUSTUB_PAGE_SIZE, bos.size());
            MyOIS<ExtendibleHTableDirectoryPage> ois = new MyOIS<ExtendibleHTableDirectoryPage>(new ByteArrayInputStream(bos.toByteArray()), ExtendibleHTableDirectoryPage.class, ExtendibleHTableDirectoryPage.class.getConstructor().newInstance());
            ExtendibleHTableDirectoryPage bucketPage1 = ois.readObjectOverride();
            System.out.println(bucketPage1.Size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void testSizeofExtendibleHTableHeader() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<ExtendibleHTableHeaderPage> constructor = ExtendibleHTableHeaderPage.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        ExtendibleHTableHeaderPage instance = constructor.newInstance();
        instance.init();
        try ( ByteArrayOutputStream bos = new ByteArrayOutputStream();
              ObjectOutputStream oos = new MyOOS(bos)
        ) {
            oos.writeObject(instance);
            Assertions.assertEquals(BUSTUB_PAGE_SIZE, bos.size());
            MyOIS<ExtendibleHTableHeaderPage> ois = new MyOIS<ExtendibleHTableHeaderPage>(new ByteArrayInputStream(bos.toByteArray()), ExtendibleHTableHeaderPage.class, ExtendibleHTableHeaderPage.class.getConstructor().newInstance());
            ExtendibleHTableHeaderPage bucketPage1 = ois.readObjectOverride();
            System.out.println(bucketPage1.maxSize());
        } catch (IOException e) {
            throw new RuntimeException(e);
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
//        ExtendibleHTableBucketPage<Byte, Byte> temp = TypeUtils.getAs(ExtendibleHTableBucketPage.class, bucketPage);
//        Byte b = temp.ValueAt(0);
    }
}
