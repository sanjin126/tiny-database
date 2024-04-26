package storage.page;

import annotation.UnsignedInt;
import config.DBConfig;

import java.io.Serializable;


/**
 * static first-level directory page. 不涉及扩展哈希
 *
 *  * Header page format:
 *  *  ---------------------------------------------------
 *  * | DirectoryPageIds(2048) | MaxDepth (4) | Free(2044)
 *  *  ---------------------------------------------------
 *  TODO 为什么这里size只是2048？？还有2044的空间不适用，但是下面的assert又与BustubPageSize进行了比较呢？
 */
public class ExtendibleHTableHeaderPage implements SerializablePageData {
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
    private final int[] directoryPageIds = new int[HTABLE_HEADER_ARRAY_SIZE]; //初始化放在构造函数中
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
    public ExtendibleHTableHeaderPage(){}

    /**
     * After creating a new header page from buffer pool, must call initialize
     * method to set default values
     * @param max_depth Max depth in the header page
     */
    public void init(/*int max_depth = HTABLE_HEADER_MAX_DEPTH*/) { //默认参数的变相实现
        init(HTABLE_HEADER_MAX_DEPTH);
    }
    public void init(int maxDepth) {
        this.maxDepth = maxDepth;
        final int arraySize = maxSize();
//        directoryPageIds = new int[arraySize]; TODO:删除
        assert (/*int array*/4*arraySize+/*int*/4) <= DBConfig.BUSTUB_PAGE_SIZE; //arraySize改变，需重新进行检查
        for (int i = 0; i < arraySize; i++) { //设置为无效的PageID
            directoryPageIds[i] = Page.INVALID_PAGE_ID;
        }
    }

    /**
     * Get the directory index that the key is hashed to
     * 为什么使用long 而不是原来的int 作为参数，因为java没有无符号数，只能使用long作为妥协。
     * 返回值，可以使用int，只要保证int大于0即可
     * // TODO 是否可以用long作为返回值？或者有必要吗 ？因为数组的大小可能不会超过int能表示的最大的整数
     * @param hash the hash of the key
     * @return directory index the key is hashed to
     *
     */
    public @UnsignedInt int hashToDirectoryIndex(@UnsignedInt long hash) /*const*/ {
        // 这里的策略是取hash的高 n bit 作为索引，其相当于扩展哈希中的hash策略
        hash = hash & 0x00000000ffffffffL; //去掉高32位
        int idx = (int) (hash >>> (Integer.SIZE - maxDepth));
        assert idx >= 0;
        return idx;
        // 例如 00000000000000001000000000000000 >>> (32 - 2) = ...00 = 0
    }

    /**
     * Get the directory page id at an index
     *
     * @param directory_idx index in the directory page id array
     * @return directory page_id at index
     */
    public @UnsignedInt int getDirectoryPageId(@UnsignedInt int directory_idx) /*const*/ {
        return directoryPageIds[directory_idx];
    }

    /**
     * @brief Set the directory page id at an index
     *
     * @param directory_idx index in the directory page id array
     * @param directory_page_id page id of the directory
     */
    public void setDirectoryPageId(@UnsignedInt int directory_idx, int directory_page_id) {
        directoryPageIds[directory_idx] = directory_page_id;
    }

    /**
     * @brief Get the maximum number of directory page ids the header page could handle
     */
    public @UnsignedInt int maxSize() /*const*/ {
        return 1 << maxDepth;
    }

    /**
     * Prints the header's occupancy information
     */
    public void PrintHeader() /*const*/ {

    }

}
