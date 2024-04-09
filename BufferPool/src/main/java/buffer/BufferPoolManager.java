package buffer;

import disk.Page;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

interface BufferPoolManagerInterface {
    Page newPage(int pageId);

    Page fetchPage(int pageId);

    boolean unpinPage(int pageId, boolean isDirty);

    boolean flushPage(int pageId);

    void flushAllPages();

    boolean deletePage(int pageId);
}

public class BufferPoolManager {

    private static final int BUFFER_SIZE = 10;
    private final Page[] pages= new Page[BUFFER_SIZE];
    private int size = 0;
    private final AtomicInteger nextPageId = new AtomicInteger(0);





}
