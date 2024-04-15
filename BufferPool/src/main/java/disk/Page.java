package disk;

import config.DBConfig;
import util.ArrayUtils;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Page is the basic unit of storage within the database system. Page provides a wrapper for actual data pages being
 * held in main memory. Page also contains book-keeping information that is used by the buffer pool manager, e.g.
 * pin count, dirty flag, page id, etc.
 */
public class Page {
    public static final int INVALID_PAGE_ID = -1;
    /** The actual data that is stored within a page. */
    // Usually this should be stored as `char data_[BUSTUB_PAGE_SIZE]{};`. But to enable ASAN to detect page overflow,
    // we store it as a ptr.
    private final byte[] data = new byte[DBConfig.BUSTUB_PAGE_SIZE];
    /** The ID of this page. */
    private int pageId = INVALID_PAGE_ID;
    /** The pin count of this page. */
    private int pinCount = 0;
    /** True if the page is dirty, i.e. it is different from its corresponding page on disk. */
    private boolean isDirty = false;
    /** Page latch. */
    private final ReadWriteLock rwlock;

    protected static final int SIZE_PAGE_HEADER = 8;
    protected static final int OFFSET_PAGE_START = 0;
    protected static final int OFFSET_LSN = 4;

    public Page() { //TODO resetMemory
        this.rwlock = new ReentrantReadWriteLock(); //这里使用的可重入的读写锁的实现
    }

    public byte[] getData() {
        return data;
    }

    public int getPageId() {
        return pageId;
    }
    public int getPinCount() {
        return pinCount;
    }
    public boolean isDirty() {
        return isDirty;
    }

    public void wLatch() {
        wlock().lock();
    }
    public void wUnLatch() {
        wlock().unlock();
    }
    public void rLatch() {
        rlock().lock();
    }
    public void rUnLatch() {
        rlock().unlock();
    }

    private Lock wlock() {
        return rwlock.writeLock();
    }
    private Lock rlock() {
        return rwlock.readLock();
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    public void reset() {
        this.isDirty = false;
        this.pageId = INVALID_PAGE_ID;
        this.pinCount = 0;
        ArrayUtils.makeEmpty(data);
    }
    public void reset(int pageId) {
        this.isDirty = false;
        this.pageId = pageId;
        this.pinCount = 0;
        ArrayUtils.makeEmpty(data);
    }

    /**
     * 将buf复制到data，而非直接替换
     * @param buf
     */
    public void setData(byte[] buf) {
        assert buf.length == DBConfig.BUSTUB_PAGE_SIZE;
        System.arraycopy(buf, 0, data, 0, DBConfig.BUSTUB_PAGE_SIZE);
    }

    public void decrPinCount() {
        assert pinCount > 0;
        pinCount --;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public void incrPinCount() {
        pinCount ++;
    }
    //TODO LSN
}
