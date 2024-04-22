package storage.disk;

import config.DBConfig;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class DiskManager { //TODO 待实现
    private String logName;
    private String fileName;
    private final Path logPath;
    private final Path filePath;
    private final SeekableByteChannel logChannel;
    private final SeekableByteChannel fileChannel;
    private int numFlushes;
    private int numWrites;
    private boolean flushLog;
    private Future<Void> flushLogFuture; //TODO
    private final Lock dbIOLock = new ReentrantLock();
    private final Logger logger = Logger.getLogger(getClass().getName());
    /**
     * Creates a new disk manager that writes to the specified database file.
     * @param db_file the file name of the database file to write to
     */
    public DiskManager(String dbFileName) {
        this.fileName = dbFileName;
        this.logName = "test.log";
        filePath = Paths.get(fileName);
        logPath = Paths.get(logName);
        try {
            logChannel = Files.newByteChannel(logPath);
            fileChannel = Files.newByteChannel(filePath);
        } catch (IOException e) {
            logger.severe(e.getMessage()+">>>>>>>>构造函数未成功创建");
            throw new RuntimeException(e);
        }
    }

    /**
     * Write a page to the database file.
     * @param page_id id of the page
     * @param page_data raw page data
     */
    public void writePage(int page_id, byte[] page_data) {

    }

    /**
     * Read a page from the database file.
     * @param page_id id of the page
     * @param[out] page_data output buffer
     * @return the data be read from disk
     */
    public byte[] readPage(int page_id) {
        byte[] ret = new byte[DBConfig.BUSTUB_PAGE_SIZE];
        byte[] src = "A test string.".getBytes();
        System.arraycopy(src, 0, ret, 0, src.length);
        return ret;
    }
    /**
     * Shut down the disk manager and close all the file resources.
     */
    public void shutDown() { //TODO 待实现
    }


    /**
     * Flush the entire log buffer into disk.
     * @param log_data raw log data
     * @param size size of log entry
     */
    void WriteLog(byte[] log_data, int size) {

    }

    /**
     * Read a log entry from the log file.
     * @param[out] log_data output buffer
     * @param size size of the log entry
     * @param offset offset of the log entry in the file
     * @return true if the read was successful, false otherwise
     */
    boolean ReadLog(byte[] log_data, int size, int offset) {
        return false;
    }

    /** @return the number of disk flushes */
    int GetNumFlushes() {
        return 0;
    }

    /** @return true iff the in-memory content has not been flushed yet */
    boolean GetFlushState() {
        return false;
    }

    /** @return the number of disk writes */
    int GetNumWrites() {
        return 0;
    }

    /**
     * Sets the future which is used to check for non-blocking flushes.
     * @param f the non-blocking flush check
     */
    void SetFlushLogFuture(Future<Void> f) {
        this.flushLogFuture = f;
    }

    /** Checks if the non-blocking flush future was set. */
    boolean HasFlushLogFuture() {
        return Objects.nonNull(this.flushLogFuture);
    }
}
