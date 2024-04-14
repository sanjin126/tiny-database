package disk;

import config.DBConfig;

public class DiskManager { //TODO 待实现
    /**
     * Write a page to the database file.
     * @param page_id id of the page
     * @param page_data raw page data
     */
    public void writePage(int page_id, char[] page_data) {

    }

    /**
     * Read a page from the database file.
     * @param page_id id of the page
     * @param[out] page_data output buffer
     * @return the data be read from disk
     */
    public char[] readPage(int page_id) {
        char[] ret = new char[DBConfig.BUSTUB_PAGE_SIZE];
        char[] src = "A test string.".toCharArray();
        System.arraycopy(src, 0, ret, 0, src.length);
        return ret;
    }

    public void shutDown() { //TODO 待实现
    }
}
