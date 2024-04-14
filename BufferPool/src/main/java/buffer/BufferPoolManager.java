package buffer;

import disk.DiskManager;
import disk.DiskScheduler;
import disk.Page;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.logging.LogManager;

interface BufferPoolManagerInterface {
    Page newPage(int pageId);

    Page fetchPage(int pageId);

    boolean unpinPage(int pageId, boolean isDirty);

    boolean flushPage(int pageId);

    void flushAllPages();

    boolean deletePage(int pageId);
}

/**
 * BufferPoolManager reads disk pages to and from its internal buffer pool.
 */
public class BufferPoolManager {

    private final int poolSize;
    private final Page[] pages;
    private final AtomicInteger nextPageId = new AtomicInteger(0);
    private DiskScheduler diskScheduler;
    // Map<page_id, frame_id>
    private Map<Integer, Integer> pageTable;
    private Replacer replacer;
    // List<frame_id>
    /** List of free frames that don't have any pages on them. */
    private Deque<Integer> freeList;
    private Lock lock;

    public BufferPoolManager(int poolSize, DiskManager diskManager) {
        this.poolSize = poolSize;
        this.pages = new Page[poolSize];
        this.diskScheduler = new DiskScheduler(diskManager);
        this.pageTable = new HashMap<>();
        this.replacer = new LRUReplacer(poolSize);
        this.freeList = new ArrayDeque<>();
        this.lock = null; // TODO
    }

    /**
     * TODO(P1): Add implementation
     *
     * @brief Create a new page in the buffer pool. Set page_id to the new page's id, or nullptr if all frames
     * are currently in use and not evictable (in another word, pinned).
     *
     * You should pick the replacement frame from either the free list or the replacer (always find from the free list
     * first), and then call the AllocatePage() method to get a new page id. If the replacement frame has a dirty page,
     * you should write it back to the disk first. You also need to reset the memory and metadata for the new page.
     *
     * Remember to "Pin" the frame by calling replacer.SetEvictable(frame_id, false)
     * so that the replacer wouldn't evict the frame before the buffer pool manager "Unpin"s it.
     * Also, remember to record the access history of the frame in the replacer for the lru-k algorithm to work.
     *
     * <del>@param[out] page_id id of created page<del/>
     * @return nullptr if no new pages could be created, otherwise pointer to new page
     */
    Page newPage() {
        return null;
    }


    public int getPoolSize() {
        return poolSize;
    }

    public Page[] getPages() {
        return pages;
    }
}
