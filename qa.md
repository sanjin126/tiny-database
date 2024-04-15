1. 为什么page中的数据存取使用byte[]而不是char[]?

    最开始，我参考bustub中的实现，使用的就是char数组。
    之后，我了解到char是utf-16编码，且其为2字节固定字节。同时，
    在进行文件IO的API中，使用的多是byte数组，而非char数组。
2. 实现PageGuard时，是否可以使用静态代理或者动态代理？