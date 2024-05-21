package buffer;

import storage.disk.DiskManager;
import storage.disk.DiskScheduler;
import storage.page.BasicPageGuard;
import storage.page.Page;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * BufferPoolManager reads disk pages to and from its internal buffer pool.
 */
public class BufferPoolManager {

    private final int poolSize;
    private final Page[] pages;
    private final AtomicInteger nextPageId = new AtomicInteger(0);
    private DiskScheduler diskScheduler;
    private Map<Integer, Integer> pageTable;      // Map<page_id, frame_id>
    private Replacer replacer;
    /** List of free frames that don't have any pages on them. */
    private Deque<Integer> freeList;    // List<frame_id>
    /** This latch protects shared data structures. We recommend updating this comment to describe what it protects. */
    private Lock lock;


    /**
     * @brief Creates a new BufferPoolManager.
     * @param pool_size the size of the buffer pool
     * @param disk_manager the disk manager
     * @param replacer_k the LookBack constant k for the LRU-K replacer
     * @param log_manager the log manager (for testing only: nullptr = disable logging). Please ignore this for P1.
     */
    public BufferPoolManager(int poolSize, DiskManager diskManager) {
        this.poolSize = poolSize;
        this.pages = new Page[poolSize];
        this.diskScheduler = new DiskScheduler(diskManager);
        this.pageTable = new HashMap<>();
        this.replacer = new LRUReplacer(poolSize);
        this.freeList = new ArrayDeque<>();
        this.lock = new ReentrantLock(); // TODO

//        for (Page page : pages) {
//            page = new Page();
//        }
        for (int i = 0; i < pages.length; i++) {
            pages[i] = new Page();
        }
        for (int i = 0; i < poolSize; i++) { //将所有空的frame_id号填入freelist
            freeList.addLast(i);
        }
    }

    /** @brief Return the size (number of frames) of the buffer pool. */
    public int getPoolSize() {
        return poolSize;
    }
    /** @brief Return the pointer to all the pages in the buffer pool. */
    // TODO 考虑将pages发布之后的线程安全问题，是否可以改为clone发布
    public Page[] getPages() {
        return pages;
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
     * <del>@param[out] page_id id of created page<del/> 由于java无法操作指针类型，所以对于pageId我们通过返回Page 而携带id
     * @return nullptr if no new pages could be created, otherwise pointer to new page
     */
    public Page newPage() {
        Integer availableFrameId;
        // 先检查freeList是否可用
        if (freeList.size() > 0) {
            availableFrameId = freeList.getFirst();
            freeList.removeFirst();
        } else {        // 如果不可用，则replace一个
            Optional<Integer> optional = replacer.victim();
            if (optional.isEmpty())
                return null; //all frames are currently in use and not evictable
            Integer frameIdOfVictimer = optional.get();
            Page page = pages[frameIdOfVictimer]; // 需要被替换的页
            if (page.isDirty()) { //需要写回
                flushPage(page.getPageId());
            }
            pageTable.remove(page.getPageId()); //需要移除
            availableFrameId = frameIdOfVictimer;
        }
        replacer.pin(availableFrameId); //"Pin" the frame, 因为Page被返回了，说明其被其他线程使用中
        Page availablePage = pages[availableFrameId];
        int newPageId = allocatePage();
        availablePage.reset(newPageId);
        pageTable.put(newPageId, availableFrameId);
        availablePage.incrPinCount(); //
        return availablePage;
    }

    /**
     * TODO(P1): Add implementation
     *
     * @brief PageGuard wrapper for NewPage
     *
     * Functionality should be the same as NewPage, except that
     * instead of returning a pointer to a page, you return a
     * BasicPageGuard structure.
     *
     * @param[out] page_id, the id of the new page
     * @return BasicPageGuard holding a new page
     */
    public BasicPageGuard newPageGuarded() {
        Page page = newPage();
        if (Objects.isNull(page))
            return null;
        return new BasicPageGuard(this, page);
    }

    /**
     * TODO(P1): Add implementation
     *
     * @brief Fetch the requested page from the buffer pool. Return nullptr if page_id needs to be fetched from the disk
     * but all frames are currently in use and not evictable (in another word, pinned).
     *
     * First search for page_id in the buffer pool. If not found, pick a replacement frame from either the free list or
     * the replacer (always find from the free list first), read the page from disk by scheduling a read DiskRequest with
     * disk_scheduler_->Schedule(), and replace the old page in the frame. Similar to NewPage(), if the old page is dirty,
     * you need to write it back to disk and update the metadata of the new page
     *
     * In addition, remember to disable eviction and record the access history of the frame like you did for NewPage().
     *
     * @param page_id id of page to be fetched
     * @param access_type type of access to the page, only needed for leaderboard tests.
     * @return nullptr if page_id cannot be fetched, otherwise pointer to the requested page
     */
    public Page fetchPage(int page_id)  {
        assert page_id >= 0;
        if (pageTable.containsKey(page_id)) {
            Integer frameId = pageTable.get(page_id);
            replacer.unpin(frameId);
            pages[frameId].incrPinCount();
            return pages[frameId];
        }

        Integer availableFrameId;
        // 先检查freeList是否可用
        if (freeList.size() > 0) {
            availableFrameId = freeList.getFirst();
            freeList.removeFirst();
        } else {        // 如果不可用，则replace一个
            Optional<Integer> optional = replacer.victim();
            if (optional.isEmpty())
                return null; //all frames are currently in use and not evictable
            Integer frameIdOfVictimer = optional.get();
            Page page = pages[frameIdOfVictimer]; // 需要被替换的页
            if (page.isDirty()) { //需要写回
                flushPage(page.getPageId());
            }
            pageTable.remove(page.getPageId());
            availableFrameId = frameIdOfVictimer;
        }
        replacer.pin(availableFrameId); //TODO 其顺序先后，是否会存在并发异常
        // 除了构造函数中使用new Page()，其他地方均只是修改Page，而不能重新赋值
        Page availablePage = pages[availableFrameId];
        availablePage.reset(page_id); // 必须在前，否则后续的getData()会被覆盖
        // 从disk中read数据到buffer中
//        char[] data = new char[DBConfig.BUSTUB_PAGE_SIZE];
        Future<Boolean> future = DiskScheduler.createFuture();
        /**
         * @see DiskScheduler#processRequest(Optional) 
         */
        diskScheduler.schedule(new DiskScheduler.DiskRequest(false, availablePage.getData(), page_id, future));
        try {
            future.get() ;
        } catch (InterruptedException e) { //异常在此层捕获即可，因为该方法的返回值标识了是否成功执行，传播出去也没有什么作用
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
//        availablePage.setData(data); //在request中直接复制
        pageTable.put(page_id, availableFrameId);
        availablePage.incrPinCount();
        return availablePage;
    }

    /**
     * TODO(P1): Add implementation
     *
     * @brief PageGuard wrappers for FetchPage
     *
     * Functionality should be the same as FetchPage, except
     * that, depending on the function called, a guard is returned.
     * If FetchPageRead or FetchPageWrite is called, it is expected that
     * the returned page already has a read or write latch held, respectively.
     *
     * @param page_id, the id of the page to fetch
     * @return PageGuard holding the fetched page
     */
    public BasicPageGuard fetchPageBasic(int page_id)  {
        Page page = fetchPage(page_id);
        if (Objects.isNull(page))
            return null;
        return new BasicPageGuard(this, page);
    }
    public BasicPageGuard.ReadPageGuard FetchPageRead(int page_id) {
        Page page = fetchPage(page_id);
        if (Objects.isNull(page))
            return null;
        page.rLatch(); //获取读锁
        return new BasicPageGuard.ReadPageGuard(this, page);
    }
    public BasicPageGuard.WritePageGuard FetchPageWrite(int page_id)  {
        Page page = fetchPage(page_id);
        if (Objects.isNull(page))
            return null;
        page.wLatch(); //获取写锁
        return new BasicPageGuard.WritePageGuard(this, page); //因为其实例化不能依靠BasicPageGuard的实例，所以需要为static
    }

    /**
     * TODO(P1): Add implementation
     *
     * @brief Unpin the target page from the buffer pool. If page_id is not in the buffer pool or its pin count is already
     * 0, return false.
     *
     * Decrement the pin count of a page. If the pin count reaches 0, the frame should be evictable by the replacer.
     * Also, set the dirty flag on the page to indicate if the page was modified.
     *
     * @param page_id id of page to be unpinned
     * @param is_dirty true if the page should be marked as dirty, false otherwise
     * @param access_type type of access to the page, only needed for leaderboard tests.
     * @return false if the page is not in the page table or its pin count is <= 0 before this call, true otherwise
     */
    public boolean unpinPage(int page_id, boolean is_dirty) {
        if (checkIfPageNotExist(page_id))
            return false;
        Page page = getPage(page_id);
        page.rLatch();
        try {
            if (page.getPinCount() <= 0) {
                return false;
            }
        } finally { //如果不使用finally，在上面return后会导致锁不能正确的释放
            page.rUnLatch();
        }
        page.wLatch();
        try {
            if (is_dirty)
                page.setDirty(true);
            page.decrPinCount();
            if (page.getPinCount() == 0)
                replacer.unpin(pageTable.get(page_id)); //If the pin count reaches 0, the frame should be evictable by the replacer.
        } finally {
            page.wUnLatch();
        }
        return true;
    }

    /**
     * TODO(P1): Add implementation
     *
     * @brief Flush the target page to disk.
     *
     * Use the DiskManager::WritePage() method to flush a page to disk, REGARDLESS of the dirty flag.
     * Unset the dirty flag of the page after flushing.
     *
     * @param page_id id of page to be flushed, cannot be INVALID_PAGE_ID
     * @return false if the page could not be found in the page table, true otherwise
     */
    public boolean flushPage(int pageId) {
        if (checkIfPageNotExist(pageId))
            return false;
        Page page = getPage(pageId);
        Future<Boolean> future = DiskScheduler.createFuture();
        page.rLatch();
        DiskScheduler.DiskRequest request = new DiskScheduler.DiskRequest(true, page.getData(), pageId, future);
        page.rUnLatch();
        diskScheduler.schedule(request);
        try {
            future.get();
        } catch (InterruptedException e) { //本层捕获即可，返回值表示是否成功执行
            e.printStackTrace();
            return false;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        }
        page.wLatch();
        page.setDirty(false); //Unset the dirty flag
        page.wUnLatch();
        return true;
    }

    /**
     * 通过pageId找到frameId，从而找到Page
     * @param pageId 必须提供存在的pageId
     * @return 从pages数组中根据对应的frameId提取出Page
     */
    private Page getPage(int pageId) {
        Integer frameId = pageTable.get(pageId);
        Page page = pages[frameId];
        return page;
    }

    private boolean checkIfPageNotExist(int pageId) {
        return !pageTable.containsKey(pageId);
    }

    /**
     * TODO(P1): Add implementation
     *
     * @brief Flush all the pages in the buffer pool to disk.
     */
    public void flushAllPages() {
        lock.lock(); //TODO 是否可以切换为pageTable的monitor锁
        try {
            //pageTable不是线程安全的
            for (Integer pageId : pageTable.keySet()) {
                flushPage(pageId);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * TODO(P1): Add implementation
     *
     * @brief Delete a page from the buffer pool. If page_id is not in the buffer pool, do nothing and return true. If the
     * page is pinned and cannot be deleted, return false immediately.
     *
     * After deleting the page from the page table, stop tracking the frame in the replacer and add the frame
     * back to the free list. Also, reset the page's memory and metadata. Finally, you should call DeallocatePage() to
     * imitate freeing the page on the disk.
     *
     * @param page_id id of page to be deleted
     * @return false if the page exists but could not be deleted, true if the page didn't exist or deletion succeeded
     */
    public boolean deletePage(int page_id) {
        if (checkIfPageNotExist(page_id)) {
            return true;
        }
        Integer frameId = pageTable.get(page_id);
        if (pages[frameId].getPinCount() > 0)
            return false;
        // delete the page
        pageTable.remove(page_id); //TODO 是否有并发风险
        replacer.pin(frameId); // delete from replacer
        freeList.addLast(frameId);  //TODO 是否有并发风险
        // reset the page
        Page page = pages[frameId];
        page.wLatch();
        page.reset();
        page.wUnLatch();
        deallocatePage(page_id);
        return true;
    }

    /**
     * @brief Allocate a page on disk. Caller should acquire the latch before calling this function.
     * @return the id of the allocated page
     */
    private int allocatePage() { //TODO
        lock.lock();
        try {
            return nextPageId.getAndIncrement();
        } finally {
            lock.unlock();
        }
    }

    /**
     * @brief Deallocate a page on disk. Caller should acquire the latch before calling this function.
     * @param page_id id of the page to deallocate
     */
    private void deallocatePage(int page_id) {
        // This is a no-nop right now without a more complex data structure to track deallocated pages
    }
}
