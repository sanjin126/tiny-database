package storage.page;

import annotation.UnsignedInt;
import config.DBConfig;


/**
 * static first-level directory page. 不涉及扩展哈希
 *
 *  * Header page format:
 *  *  ---------------------------------------------------
 *  * | DirectoryPageIds(2048) | MaxDepth (4) | Free(2044)
 *  *  ---------------------------------------------------
 *  TODO 为什么这里size只是2048？？还有2044的空间不适用，但是下面的assert又与BustubPageSize进行了比较呢？
 */
public class ExtendibleHTableHeaderPage {
//    { //static assert
//        assert sizeof(ExtendibleHTableHeaderPage) <= BUSTUB_PAGE_SIZE;
//    }
    /**
     * 使用static变量进行保存，实例中就不会占据空间，所以不用考虑这部分的大小会影响Page的大小
     */
    private static final int HTABLE_HEADER_PAGE_METADATA_SIZE = Integer.BYTES;
    private static final int HTABLE_HEADER_MAX_DEPTH = 9;
    private static final int HTABLE_HEADER_ARRAY_SIZE = 1 << HTABLE_HEADER_MAX_DEPTH;

    /**存储pageId，An array of directory page ids*/
    private int[] directoryPageIds; //初始化放在构造函数中
    /**
     * The maximum depth the header page could handle
     * <p>
     * 2^maxDepth = 当前page所包含的entry的个数，
     * 例如，maxDepth = 2, 则当前page包含四个表项，分别是00 01 10 11
     * 由于headerPage是static的，所以在最初的时候就创建出所有的表项，
     * 与Directory Page和BucketPage进行区分
     * </p>
     */
    private int maxDepth;

    /**
     * 一些类型大小检查
     */
    static {
        assert HTABLE_HEADER_ARRAY_SIZE > 0; //防止溢出,如 1 << Integer.SIZE - 1 = -2147483648
    }
    static {
        assert (/*int array*/4*HTABLE_HEADER_ARRAY_SIZE+/*int*/4) <= DBConfig.BUSTUB_PAGE_SIZE;
        //保证当前类可以存入一个page中，而不会有溢出的情况
    }
    // Delete all constructor
    private ExtendibleHTableHeaderPage(){}

    /**
     * After creating a new header page from buffer pool, must call initialize
     * method to set default values
     * @param max_depth Max depth in the header page
     */
    void init(/*int max_depth = HTABLE_HEADER_MAX_DEPTH*/) { //默认参数的变相实现
        init(HTABLE_HEADER_MAX_DEPTH);
    }
    void init(int maxDepth) {
        this.maxDepth = maxDepth;
        final int arraySize = 1 << maxDepth;
        directoryPageIds = new int[arraySize];
        assert (/*int array*/4*arraySize+/*int*/4) <= DBConfig.BUSTUB_PAGE_SIZE; //arraySize改变，需重新进行检查
        for (int i = 0; i < directoryPageIds.length; i++) { //设置为无效的PageID
            directoryPageIds[i] = Page.INVALID_PAGE_ID;
        }
    }

    /**
     * Get the directory index that the key is hashed to
     *
     * @param hash the hash of the key
     * @return directory index the key is hashed to
     */
    @UnsignedInt int hashToDirectoryIndex(@UnsignedInt int hash) /*const*/ {
        // 这里的策略是取hash的高 n bit 作为索引，其相当于扩展哈希中的hash策略
        return hash >>> (Integer.SIZE - maxDepth);
        // 例如 00000000000000001000000000000000 >>> (32 - 2) = ...00 = 0
    }

    /**
     * Get the directory page id at an index
     *
     * @param directory_idx index in the directory page id array
     * @return directory page_id at index
     */
    @UnsignedInt int getDirectoryPageId(@UnsignedInt int directory_idx) /*const*/ {
        return directoryPageIds[directory_idx];
    }

    /**
     * @brief Set the directory page id at an index
     *
     * @param directory_idx index in the directory page id array
     * @param directory_page_id page id of the directory
     */
    void setDirectoryPageId(@UnsignedInt int directory_idx, int directory_page_id) {
        directoryPageIds[directory_idx] = directory_page_id;
    }

    /**
     * @brief Get the maximum number of directory page ids the header page could handle
     */
    @UnsignedInt int maxSize() /*const*/ {
        assert directoryPageIds.length == 1 << maxDepth; //后续可删除
        return directoryPageIds.length;
    }

    /**
     * Prints the header's occupancy information
     */
    void PrintHeader() /*const*/ {

    }

}