package storage.page;

import buffer.BufferPoolManager;

import java.util.Objects;
import java.util.logging.Logger;

public class BasicPageGuard{
    private BufferPoolManager bufferPoolManager;
    private Page page;
    private boolean isDirty = false;
    private final Logger logger = Logger.getLogger(BasicPageGuard.class.getName());

    public BasicPageGuard(BufferPoolManager poolManager, Page page) {
        this.bufferPoolManager = poolManager;
        this.page = page;
    }

    //对c++中移动构造函数的模拟
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
     */
    public void drop() {
        checkIfBasicPageGuardValid();
        bufferPoolManager.unpinPage(page.getPageId(), isDirty);
        this.page = null;
        this.bufferPoolManager = null;
    }

    private void checkIfBasicPageGuardValid() {
        if (Objects.isNull(bufferPoolManager) || Objects.isNull(page)) {
            logger.info("此pageGuard已经不可以使用");
        }
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
    ReadPageGuard UpgradeRead() {
        return null;
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
    WritePageGuard UpgradeWrite() {
        return null;
    }

    public int getPageId() { return page.getPageId(); }

    /**
     * 返回一个不可更改的数组，目前通过clone实现
     * @return
     */
    public byte[] getData() { return page.getData().clone(); }


//    public <T> T[] As(T e) {
//        return reinterpret_cast<const T *>(GetData());
//    }

    public byte[] GetDataMut() {
        isDirty = true;
        return page.getData();
    }
//
//    template <class T>
//    auto AsMut() -> T * {
//        return reinterpret_cast<T *>(GetDataMut());
//    }
    public static class ReadPageGuard {
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
            basicPageGuard.page.rUnLatch();
            // 接着清理basicPageGuard
            basicPageGuard.drop();
        }

        int PageId()  { return basicPageGuard.getPageId(); }

        byte[] GetData() { return basicPageGuard.getData(); }

//        template <class T>
//        auto As() -> const T * {
//            return guard_.As<T>();
//        }
    }

    public static class WritePageGuard {
        private BasicPageGuard basicPageGuard;

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
            // 必须先释放写锁，再drop，因为basicPageGuard drop之后，会删除page的引用
            basicPageGuard.page.wUnLatch();
            basicPageGuard.drop();
        }

        int PageId()  { return basicPageGuard.getPageId(); }

        byte[] GetDataMut() { return basicPageGuard.GetDataMut(); }

//        template <class T>
//        auto As() -> const T * {
//            return guard_.As<T>();
//        }
//        template <class T>
//        auto AsMut() -> T * {
//            return guard_.AsMut<T>();
//        }


    }

}
