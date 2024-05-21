package storage.page;

import buffer.BufferPoolManager;
import serialization.MyOIS;
import util.SerializeUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * 是否需要在此类的方法中 pin一个Page，不需要，因为在fetch和new一个新的page的时候就已经pin了
 * 使用的时候在new之后，应该立即将其update成read和write从而加上读锁和写锁
 */
public class BasicPageGuard{
    private BufferPoolManager bufferPoolManager;
    private Page page;
    private boolean isDirty = false;
    private static final Logger logger = Logger.getLogger(BasicPageGuard.class.getName());

    /**
     * 谨慎使用，因为其使用时并未加锁
     * 创建之后要立即转为read和write
     * @param poolManager
     * @param page
     */
    public BasicPageGuard(BufferPoolManager poolManager, Page page) {
        this.bufferPoolManager = poolManager;
        this.page = page;
    }

    //TODO 对c++中移动构造函数的模拟 是否有用？
    public BasicPageGuard(BasicPageGuard pageGuard) {
        this(pageGuard.bufferPoolManager, pageGuard.page);
        pageGuard.page = null;
    }

    /** TODO(P1): Add implementation
     *
     * @brief Drop a page guard
     *
     * Dropping a page guard should clear all contents
     * (so that the page guard is no longer useful), and
     * it should tell the BPM that we are done using this page,
     * per the specification in the writeup.
     * drop之后就不能再使用此类了
     */
    private void drop() {
        if (checkIfBasicPageGuardValid()) { //如果不再有效，说明已经被其他guard给释放过了，无需进行处理
            bufferPoolManager.unpinPage(page.getPageId(), isDirty);
            this.page = null;
            this.bufferPoolManager = null;
        }
    }
// 不可能通过BasicPageGuard修改数据，所以drop的时候就无需考虑
//    private  <T> void drop(T outerPage) {
//        checkIfBasicPageGuardValid();
//
//        if (isDirty) { //TODO 是否需要这样做
//            try {
//                byte[] updatedData = SerializeUtils.deserialize(outerPage);
//                // invariants: mayUpdatedData.length <= basicPageGuard.page.length(which equal DBConfig.PAGE_SIZE)
//                System.arraycopy(updatedData, 0, this.page.getData(), 0, updatedData.length);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        bufferPoolManager.unpinPage(page.getPageId(), isDirty);
//        this.page = null;
//        this.bufferPoolManager = null;
//    }

//    @Override
//    public void close() {
//        /**
//         * 如果通过BasicPageGuard进行AutoCLose时，要保证页没有被修改，如果修改页，就是用WritePageGuard来进行操作。
//         */
//        assert !isDirty;
//        drop();
//    }

    /**
     * 保证bufferPoolManager和page不为空
     * @throws RuntimeException
     */
    private boolean checkIfBasicPageGuardValid() {
        if (Objects.isNull(bufferPoolManager) || Objects.isNull(page)) {
            logger.info("此pageGuard已经不可以使用");
            return false;
//            throw new RuntimeException("此pageGuard已经不可以使用,此前已经drop过");
        }
        return true;
    }

    private static void checkIfBasicPageGuardValid(BasicPageGuard basicPageGuard) {
        if (Objects.isNull(basicPageGuard.bufferPoolManager) || Objects.isNull(basicPageGuard.page)) {
            basicPageGuard.logger.info("此pageGuard已经不可以使用");
        }
    }

    /** TODO(P1): Add implementation
     *
     * @brief Upgrade a BasicPageGuard to a ReadPageGuard
     *
     * The protected page is not evicted from the buffer pool during the upgrade,
     * and the basic page guard should be made invalid after calling this function.
     *
     * @return an upgraded ReadPageGuard
     */
    public ReadPageGuard UpgradeRead() {
        checkIfBasicPageGuardValid();
        page.rLatch();
        ReadPageGuard readPageGuard = new ReadPageGuard(this.bufferPoolManager, this.page);
        this.page = null;
        this.bufferPoolManager = null;
        return readPageGuard;
    }

    /** TODO(P1): Add implementation
     *
     * @brief Upgrade a BasicPageGuard to a WritePageGuard
     *
     * The protected page is not evicted from the buffer pool during the upgrade,
     * and the basic page guard should be made invalid after calling this function.
     *
     * @return an upgraded WritePageGuard
     */
    public WritePageGuard UpgradeWrite() {
        checkIfBasicPageGuardValid();
        page.wLatch();
        WritePageGuard writePageGuard = new WritePageGuard(this.bufferPoolManager, this.page);
        this.page = null;
        this.bufferPoolManager = null;
        return writePageGuard;
    }

    public int getPageId() { return page.getPageId(); }

    /**
     * 返回一个不可更改的数组，目前通过clone实现
     * @return
     */
    private byte[] getData() { return page.getData().clone(); } //TODO 对于Clone方法的讨论

    private  <T> T As(T mock) {
        return As(mock, (Class<T>)mock.getClass());
    }

    private <T> T As(T mock, Class<T> cl) {
        byte[] data = getData();
        try { //TODO exception 应该传播出去 还是本层处理？
            return SerializeUtils.serialize(mock, cl, data);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private byte[] GetDataMut() {
        isDirty = true;
        return page.getData();
    }

    private  <T> T AsMut(T mock) {
        byte[] data = GetDataMut();
        Class<T> cl = (Class<T>) mock.getClass();
        try {
            return SerializeUtils.serialize(mock, cl, data);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected  <T> T AsMut(T mock, Class<T> cl) {
        byte[] data = GetDataMut();
        try {
            return SerializeUtils.serialize(mock, cl, data);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }



    public static class ReadPageGuard implements AutoCloseable{
        private BasicPageGuard basicPageGuard;

        public ReadPageGuard(BufferPoolManager poolManager, Page page) {
            this.basicPageGuard = new BasicPageGuard(poolManager, page);
        }
        //移动构造函数
        public ReadPageGuard(ReadPageGuard readPageGuard) {
            this.basicPageGuard = readPageGuard.basicPageGuard;
            readPageGuard.basicPageGuard = null;
        }
        /** TODO(P1): Add implementation
         *
         * @brief Drop a ReadPageGuard
         *
         * ReadPageGuard's Drop should behave similarly to BasicPageGuard,
         * except that ReadPageGuard has an additional resource - the latch!
         * However, you should think VERY carefully about in which order you
         * want to release these resources.
         */
        public void drop() {
            checkIfBasicPageGuardValid(basicPageGuard);
            //首先 释放读锁
            basicPageGuard.page.rUnLatch(); //TODO 内部类可以直接访问外部类的private属性
            // 接着清理basicPageGuard
            basicPageGuard.drop();
        }

        int getPageId()  { return basicPageGuard.getPageId(); }

        byte[] GetData() { return basicPageGuard.getData(); }

        public <T> T As(T mock) {
            return As(mock, (Class<T>) mock.getClass());
        }

        public <T> T As(T mock, Class<T> cl) {
            return basicPageGuard.As(mock, cl);
        }

        @Override
        public void close() {
            drop();
        }
    }

    public static class WritePageGuard implements AutoCloseable{
        private BasicPageGuard basicPageGuard;
        private SerializablePageData indexPage = null; //配合close机制使用,将转化的IndexPage保存下来，之后drop的时候，就可以直接使用

        public WritePageGuard(BufferPoolManager poolManager, Page page) {
            this.basicPageGuard = new BasicPageGuard(poolManager, page);
        }
        //移动构造函数
        public WritePageGuard(WritePageGuard writePageGuard) {
            this.basicPageGuard = writePageGuard.basicPageGuard;
            writePageGuard.basicPageGuard = null;
        }
        /** TODO(P1): Add implementation
         *
         * @brief Drop a WritePageGuard
         *
         * WritePageGuard's Drop should behave similarly to BasicPageGuard,
         * except that WritePageGuard has an additional resource - the latch!
         * However, you should think VERY carefully about in which order you
         * want to release these resources.
         */
        public void drop() {
            checkIfBasicPageGuardValid(basicPageGuard);
            if (basicPageGuard.isDirty) { //TODO 将反序列化的数据写回Page中
                try {
                    byte[] updatedData = SerializeUtils.deserialize(indexPage);
                    // invariants: mayUpdatedData.length <= basicPageGuard.page.length(which equal DBConfig.PAGE_SIZE)
                    System.arraycopy(updatedData, 0, basicPageGuard.page.getData(), 0, updatedData.length);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            // 必须先释放写锁，再drop，因为basicPageGuard drop之后，会删除page的引用
            basicPageGuard.page.wUnLatch();
            basicPageGuard.drop();
        }

        public int getPageId()  { return basicPageGuard.getPageId(); }

        byte[] GetDataMut() { return basicPageGuard.GetDataMut(); }

        private  <T extends SerializablePageData> T As(T mock, Class<T> cl) {
            if (indexPage != null) {
                throw new RuntimeException("多次进行转换");
            }
            T instance = basicPageGuard.As(mock, cl);
            this.indexPage = instance;
            return instance;
        }

        public <T extends SerializablePageData> T AsMut(T mock) {
            return AsMut(mock, ( Class<T> ) mock.getClass());
        }

        public <T extends SerializablePageData> T AsMut(T mock, Class<T> cl) {
            if (indexPage != null) {
                throw new RuntimeException("多次进行转换");
            }
            T instance =  basicPageGuard.AsMut(mock, cl);
            this.indexPage = instance;
            return instance;
        }

        @Override
        public void close() {
            drop();
        }
    }

}
