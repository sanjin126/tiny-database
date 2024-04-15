package disk;

import impletation.CustomChannel;
import impletation.CustomChannelImpl;

import java.nio.channels.Channel;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * @brief The DiskScheduler schedules disk read and write operations.
 *
 * A request is scheduled by calling DiskScheduler::Schedule() with an appropriate DiskRequest object.
 * The scheduler maintains a background worker thread that processes the scheduled requests using the disk manager.
 * The background thread is created in the DiskScheduler constructor and joined in its destructor.
 * 由于java中的线程，即使在其创建线程结束时，其也会继续执行，所以无需担心destructor的问题（不同于c++）
 */
public class DiskScheduler {
    private final Logger logger = Logger.getLogger(DiskScheduler.class.getName());

    /**
     * @brief Represents a Write or Read request for the DiskManager to execute.
     * 必须是static：不依赖于当前class的实例而进行创建 且 public：包外可以访问得到
     */
    public static class DiskRequest {
        /** Flag indicating whether the request is a write or a read. */
        boolean isWrite;
        /**
         *  Pointer to the start of the memory location where a page is either:
         *   1. being read into from disk (on a read).
         *   2. being written out to disk (on a write).
         */
        byte[] data;
        /** ID of the page being read from / written to disk. */
        int pageId;
        /** Callback used to signal to the request issuer when the request has been completed. */
        Future<Boolean> callback;

        public DiskRequest(boolean isWrite, byte[] data, int pageId, Future<Boolean> callback) {
            this.isWrite = isWrite;
            this.data = data;
            this.pageId = pageId;
            this.callback = callback;
        }
    }

    private final CustomChannel<Optional<DiskRequest>> requestQueue;
    private final Optional<Thread> backgroundThread;
    private final DiskManager diskManager;

    public DiskScheduler(DiskManager diskManager) {
        this.diskManager = diskManager;
        this.requestQueue = new CustomChannelImpl<>(); // TODO 设计一个实现类
        Runnable taskOfWorker = this::startWorkerThread;
        backgroundThread = Optional.of(new Thread(taskOfWorker));
        backgroundThread.get().start();    // 启动background线程
    }

    // 该方法被BufferManager所调用，从而获取数据
    /**
     * 由于requestQueue是同步容器，所以本方法无需担心并发异常
     * 此方法是一个异步方法，其返回值是通过DiskRequest#Future来返回的
     * @param req 读请求或者写请求
     */
    public void schedule(DiskRequest req) {
        try {
            requestQueue.put(Optional.of(req));
        } catch (InterruptedException e) {
            logger.info("schedule方法被终止");
            throw new RuntimeException(e);
        }
    }

    //  TODO 缺少一个退出的机制,
    /**
     * 如果直接使用Interrupt的话，会导致Queue中的请求并未处理完，而被shutDown，即使
     * 使用
     * <pre>
     * @<code>
     *    if (requestQueue.size() == 0) { //若队列不为空，则不响应Interrupt
     *         if (Thread.interrupted()) {
     *            return;
     *         }
     *    }
     * </code>
     * </pre>
     * 当再一次get时，就会判断中断标志，而被中断，所以这种方法是不可行的
     */
    public void startWorkerThread() {
        Optional<DiskRequest> req;
        try {
            while ((req = requestQueue.get()).isPresent()) { // 获取新的请求，若队列为空，则阻塞
                processRequest(req);
                // 下面代码与catch和finally的作用相重合
//                if (requestQueue.size() == 0) { //若队列不为空，则不响应Interrupt
//                    if (Thread.interrupted()) {
//                        return;
//                    }
//                }
            }
        } catch (InterruptedException e) {
            logger.info("WorkerThread被ShutDown，队列中剩余："+requestQueue.size());
        } finally {
            while (requestQueue.size() != 0) {
                try {
                    processRequest(requestQueue.get());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println( ("finally执行完毕，队列中剩余："+requestQueue.size()) );
            logger.info("finally执行完毕，队列中剩余："+requestQueue.size()); //TODO 无法输出，在其之后的sout语句也无法输出
        }
    }

    private void processRequest(Optional<DiskRequest> req) {
        DiskRequest request = req.get();
        if ( !(request.callback instanceof CompletableFuture<Boolean>) ) {
            throw new RuntimeException("请检查Future的类型，使用工厂方法来进行获取");
        }
        // 判断是读请求 还是 写请求
        if (request.isWrite) {
            diskManager.writePage(request.pageId, request.data);
        } else {
            byte[] page = diskManager.readPage(request.pageId);
            // 不能直接替换request的data为page 而应该复制
            System.arraycopy(page, 0, request.data, 0, request.data.length);
        }
        // 设置此次操作的返回值，因为外部线程会调用Future#get从而阻塞
        ((CompletableFuture<Boolean>) request.callback).complete(true);
    }

    /**
     * 该方法用于关闭backgroundWorkerThread
     * 在原C++中的实现，是通过传入一个<i>NUllOpt</i>来终止
     */
    public void shutDown() {
        if (backgroundThread.isPresent()) {
            backgroundThread.get().interrupt();
        } else {
            logger.info("无法shutDown,Thread为空");
        }
    }

    /**
     *
     * @return
     */
    public static Future<Boolean> createFuture() {
        return new CompletableFuture<>();
    }
}
