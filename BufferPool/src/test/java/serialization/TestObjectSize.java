package serialization;

import org.junit.jupiter.api.Test;
import storage.page.ExtendibleHTableBucketPage;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class TestObjectSize {
    @Test
    public void testSizeOfExtendibleHTableBucketPage() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        Constructor<ExtendibleHTableBucketPage> defaultConstructor = ExtendibleHTableBucketPage.class.getDeclaredConstructor();
        defaultConstructor.setAccessible(true);
        ExtendibleHTableBucketPage bucketPage = defaultConstructor.newInstance();
//        bucketPage.init(Integer.BYTES, Integer.BYTES);
        bucketPage.init( 1, 1);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(byteArrayOutputStream);
        oos.writeObject(bucketPage);
        System.out.println("byteArrayOutputStream.size() = " + byteArrayOutputStream.size());
    }
}
