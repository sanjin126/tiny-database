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
 */
public class BasicPageGuard{
    private BufferPoolManager bufferPoolManager;
    private Page page;
    private boolean isDirty = false;
    private static final Logger logger = Logger.getLogger(BasicPageGuard.class.getName());

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
        checkIfBasicPageGuardValid();
        bufferPoolManager.unpinPage(page.getPageId(), isDirty);
        this.page = null;
        this.bufferPoolManager = null;
    }

    private <T> void drop(T outerPage) {
        checkIfBasicPageGuardValid();

        if (isDirty) { //TODO 是否需要这样做
            try {
                byte[] updatedData = SerializeUtils.deserialize(outerPage);
                // invariants: mayUpdatedData.length <= basicPageGuard.page.length(which equal DBConfig.PAGE_SIZE)
                System.arraycopy(updatedData, 0, this.page.getData(), 0, updatedData.length);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

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
    public byte[] getData() { return page.getData().clone(); } //TODO 对于Clone方法的讨论


    public <T> T As(T mock, Class<T> cl) {
        byte[] data = getData();
        try { //TODO exception 应该传播出去 还是本层处理？
            return SerializeUtils.serialize(mock, cl, data);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public byte[] GetDataMut() {
        isDirty = true;
        return page.getData();
    }

    public <T> T AsMut(T mock, Class<T> cl) {
        byte[] data = GetDataMut();
        try {
            return SerializeUtils.serialize(mock, cl, data);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
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
            basicPageGuard.page.rUnLatch(); //TODO 内部类可以直接访问外部类的private属性
            // 接着清理basicPageGuard
            basicPageGuard.drop();
        }

        int PageId()  { return basicPageGuard.getPageId(); }

        byte[] GetData() { return basicPageGuard.getData(); }

        public <T> T As(T mock, Class<T> cl) {
            return basicPageGuard.As(mock, cl);
        }
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
        public <T extends SerializablePageData> void drop(T outerPage) {
            checkIfBasicPageGuardValid(basicPageGuard);
            if (basicPageGuard.isDirty) {
                try {
                    byte[] updatedData = SerializeUtils.deserialize(outerPage);
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

        int PageId()  { return basicPageGuard.getPageId(); }

        byte[] GetDataMut() { return basicPageGuard.GetDataMut(); }

        public <T> T As(T mock, Class<T> cl) {
            return basicPageGuard.As(mock, cl);
        }

        public <T> T AsMut(T mock, Class<T> cl) {
            return basicPageGuard.AsMut(mock, cl);
        }


    }

}
