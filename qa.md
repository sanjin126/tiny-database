1. 为什么page中的数据存取使用byte[]而不是char[]?

    最开始，我参考bustub中的实现，使用的就是char数组。
    之后，我了解到char是utf-16编码，且其为2字节固定字节。同时，
    在进行文件IO的API中，使用的多是byte数组，而非char数组。
2. 实现PageGuard时，是否可以使用静态代理或者动态代理？
3. 无符号左移和有符号右移？
   https://stackoverflow.com/questions/19058859/what-does-mean-in-java
4. 关于日志设置为static或是？
   https://stackoverflow.com/questions/3842823/should-logger-be-private-static-or-not
5. c++中的sizeOf，在java中如何处理？
    Integer.Size Integer.Bytes
    泛型类型如何处理：通过构造函数传入，见ExtendibleHTableBucketPage
6. c++中的asMut中的reinterpret_cast实现？
```c++
    BasicPageGuard guard = bpm->NewPageGuarded(&bucket_page_id);
    auto bucket_page = guard.AsMut<ExtendibleHTableBucketPage<GenericKey<8>, RID, GenericComparator<8>>>();
```
此方法可以将BufferPool中的Page中的data转换为实际的类型：例如DirectoryPage，或者
BucketPage等等，但是java中如何转换呢？
Data存储类型是byte[]，所以如何将byte字节转换为java中的类实例，如果这个字节序列是
7. 序列化的对象，比实际所拥有的变量的size总和要更大
   https://stackoverflow.com/questions/45299136/why-is-the-serialized-size-of-my-class-larger-then-the-sum-of-its-variables

    https://docs.oracle.com/javase/8/docs/platform/serialization/spec/protocol.html#a10258
    如果我们要使用序列化来存入page中，就要解决这个问题。只序列化 field 其他信息不参与序列化

   同时，因为ExtendibleHTable**Page中存在数组，如果我们对其进行序列化时：

   - 序列化时存储其数组大小，但是这样就使得序列化后的字节变大
   - :accept: 。不存储数组大小，采用一个fixed-size，需要在编译时期就可以进行确定的大小

8. Integer.type和Integer.class的区别

   当你想通过反射获取一个构造函数时，你应该基于你正在处理的是包装类还是基本类型来决定是使用`Integer.class`还是`Integer.TYPE`。 如果你正在处理的是包装类`Integer`，你应该使用`Integer.class`。例如，假设你有一个类的构造函数接受一个`Integer`参数，那么你可以这样获取这个构造函数： ```java Constructor constructor = yourClass.getConstructor(Integer.class); ``` 如果你正在处理的是基本类型`int`，你应该使用`Integer.TYPE`。例如，假设你有一个类的构造函数接受一个`int`参数，那么你可以这样获取这个构造函数： ```java Constructor constructor = yourClass.getConstructor(Integer.TYPE); ``` 因此，选择`Integer.class`还是`Integer.TYPE`完全取决于你正在处理的是包装类还是基本类型。在处理反射时，需要注意这两者之间的差别，否则可能会找不到正确的构造函数或方法。

9. 泛型的强制转换

```java
ExtendibleHTableBucketPage<Byte, Integer> bucketPage = getBucketPage(1, 1);
ExtendibleHTableBucketPage<Byte, Byte> temp = TypeUtils.getAs(ExtendibleHTableBucketPage.class, bucketPage);
```

