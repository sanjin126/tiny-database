package storage.page;

import annotation.UnsignedByte;
import annotation.UnsignedInt;
import config.DBConfig;
import util.BitUtils;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

/**
 * Directory page format:
 *  --------------------------------------------------------------------------------------
 * | MaxDepth (4) | GlobalDepth (4) | LocalDepths (512) | BucketPageIds(2048) | Free(1528)
 *  --------------------------------------------------------------------------------------
 *  Directory Page for extendible hash table.
 */
public class ExtendibleHTableDirectoryPage implements SerializablePageData {
    private transient final Logger logger = Logger.getLogger(this.getClass().getName());
    private static final int HTABLE_DIRECTORY_PAGE_METADATA_SIZE = Integer.BYTES * 2;

    /**
     * HTABLE_DIRECTORY_ARRAY_SIZE is the number of page_ids that can fit in the directory page of an extendible hash index.
     * This is 512 because the directory array must grow in powers of 2, and 1024 page_ids leaves zero room for
     * storage of the other member variables.
     */
    private static final int HTABLE_DIRECTORY_MAX_DEPTH = 9;
    private static final int HTABLE_DIRECTORY_ARRAY_SIZE = 1 << HTABLE_DIRECTORY_MAX_DEPTH;

    /**
     * The maximum depth the header page could handle
     * <p>
     * 2^maxDepth = 当前page所能够包含的entry的最大个数，
     * 而 2^globalDepth则表示当前所包含的entry个数
     * 类似于《数据库系统概念》中的位于addressTable上的<i>hash prefix “i”</i>
     * </p>
     * @see ExtendibleHTableDirectoryPage#globalDepth
     */
    private int maxDepth;
    /**
     * 	The current directory global depth
     */
    private int globalDepth;
    /**
     * 	An array of bucket page local depths
     * 	所有指向同一个bucket的entries，都有一个common hash prefix，
     * 	我们用这里的bucket depth来表示 the length of the common hash prefix
     */
    private final byte[] localDepths = new byte[HTABLE_DIRECTORY_ARRAY_SIZE];
    /**
     * An array of bucket page ids
     */
    private final int[] bucketPageIds = new int[HTABLE_DIRECTORY_ARRAY_SIZE];

    private ExtendibleHTableDirectoryPage() {} /**delete*/

    /**安全检查*/
    static {
        assert HTABLE_DIRECTORY_ARRAY_SIZE > 0; //防止有符号整数的溢出
        assert 4 + 4 + HTABLE_DIRECTORY_ARRAY_SIZE * (1 + 4) <= DBConfig.BUSTUB_PAGE_SIZE;
    }
    /**
     * After creating a new directory page from buffer pool, must call initialize
     * method to set default values
     * @param max_depth Max depth in the directory page
     */
    public void init(/**int max_depth = HTABLE_DIRECTORY_MAX_DEPTH*/) {
        this.init(HTABLE_DIRECTORY_MAX_DEPTH);
    }
    public void init(@UnsignedInt int max_depth) {
        this.maxDepth = max_depth;
        this.globalDepth = 0; //根据扩展哈希，最初为0，即只有一个表项，同时仅仅指向一个buket
        final int arraySize = MaxSize();
//        localDepths = new byte[arraySize];
//        bucketPageIds = new int[arraySize];
        // 对数组中的内容进行初始化
        byte initLocalDepth = 0; //初始化时，common hash prefix就是0
        Arrays.fill(localDepths, 0, arraySize, initLocalDepth);
        Arrays.fill(bucketPageIds, 0, arraySize, Page.INVALID_PAGE_ID); //初始化为invalid id
    }

    /**
     * Get the bucket index that the key is hashed to
     *
     * @param hash the hash of the key
     * @return bucket index current key is hashed to
     */
    public @UnsignedInt int HashToBucketIndex(@UnsignedInt int hash) /*const*/ {
        return BitUtils.highestNbits(hash, globalDepth); //这里使用globalDepth
    }


    /**
     * Lookup a bucket page using a directory index
     *
     * @param bucket_idx the index in the directory to lookup
     * @return bucket page_id corresponding to bucket_idx
     */
    public int GetBucketPageId(@UnsignedInt int bucket_idx) /**const*/ {
        return bucketPageIds[bucket_idx];
    }

    /**
     * Updates the directory index using a bucket index and page_id
     *
     * @param bucket_idx directory index at which to insert page_id
     * @param bucket_page_id page_id to insert
     */
    public void SetBucketPageId(@UnsignedInt int bucket_idx, int bucket_page_id) {
        bucketPageIds[bucket_idx] = bucket_page_id;
    }

    /**
     * Gets the split image of an index
     *
     * @param bucket_idx the directory index for which to find the split image
     * @return the directory index of the split image
     **/
    public @UnsignedInt int GetSplitImageIndex(@UnsignedInt int bucket_idx) /*const*/ {
        return 0; //TODO
    }

    /**
     * GetGlobalDepthMask - returns a mask of global_depth 1's and the rest 0's.
     *
     * In Extendible Hashing we map a key to a directory index
     * using the following hash + mask function.
     *
     * DirectoryIndex = Hash(key) & GLOBAL_DEPTH_MASK
     *
     * where GLOBAL_DEPTH_MASK is a mask with exactly GLOBAL_DEPTH 1's from LSB
     * upwards(Least Significant Bit"。在这种特定的上下文中，"LSB upwards" 通常意味着我们从最低有效位开始，逐渐向上到最高有效位).
     * For example, global depth 3 corresponds to 0x00000007 in a 32-bit
     * representation.
     *
     * @return mask of global_depth 1's and the rest 0's (with 1's from LSB upwards)
     */
    public @UnsignedInt int GetGlobalDepthMask() /*const*/ {
        return 1 << globalDepth - 1;
    }

    /**
     * GetLocalDepthMask - same as global depth mask, except it
     * uses the local depth of the bucket located at bucket_idx
     *
     * @param bucket_idx the index to use for looking up local depth
     * @return mask of local 1's and the rest 0's (with 1's from LSB upwards)
     */
    public @UnsignedInt int GetLocalDepthMask(@UnsignedInt int bucket_idx) /*const*/ {
        return 1 << localDepths[bucket_idx] - 1;
    }

    /**
     * Get the global depth of the hash table directory
     *
     * @return the global depth of the directory
     */
    public @UnsignedInt int GetGlobalDepth() /*const*/  {
        return globalDepth;
    }

    public @UnsignedInt int GetMaxDepth() /*const*/ {
        return maxDepth;
    }

    /**
     * Increment the global depth of the directory
     */
    public void IncrGlobalDepth()  {
        ++ globalDepth;
        assert globalDepth <= maxDepth;
    }

    /**
     * Decrement the global depth of the directory
     */
    public void DecrGlobalDepth() {
        -- globalDepth;
        assert globalDepth > 0;
    }

    /**
     * Only shrink the directory if the local depth of every bucket is strictly less than the global depth of the directory.
     * @return true if the directory can be shrunk
     */
    public boolean CanShrink() {
        // split的条件是 当前插入的bucket的 localDepth == globalDepth（仅有一个entry指向此bucket），并且这个bucket已经满了
        // shrink的条件是 所有的bucket都不能会split===> 对于任一bucket，其localDepth < globalDepth, <del>并且这个bucket还没满</del>
        for (byte localDepth : localDepths) {
            if (localDepth >= globalDepth)
                return false;
        }
        return true;
    }

    /**
     * @return the current directory size
     */
    public @UnsignedInt int Size() /*const*/ {
        return 1 << this.globalDepth;
    }

    /**
     * @return the max directory size
     */
    public @UnsignedInt int MaxSize() /*const*/ {
        return 1 << this.maxDepth;
    }

    /**
     * Gets the local depth of the bucket at bucket_idx
     *
     * @param bucket_idx the bucket index to lookup
     * @return the local depth of the bucket at bucket_idx
     */
   public @UnsignedInt int GetLocalDepth(@UnsignedInt int bucket_idx) /*const*/  {
        return localDepths[bucket_idx];
    }

    /**
     * Set the local depth of the bucket at bucket_idx to local_depth
     *
     * @param bucket_idx bucket index to update
     * @param local_depth new local depth
     */
    public void SetLocalDepth(@UnsignedInt int bucket_idx, @UnsignedByte byte local_depth) {
        localDepths[bucket_idx] = local_depth;
    }

    /**
     * Increment the local depth of the bucket at bucket_idx
     * @param bucket_idx bucket index to increment
     */
    public void IncrLocalDepth(@UnsignedInt int bucket_idx) {
        ++ localDepths[bucket_idx];
    }

    /**
     * Decrement the local depth of the bucket at bucket_idx
     * @param bucket_idx bucket index to decrement
     */
    public void DecrLocalDepth(@UnsignedInt int bucket_idx) {
        if (localDepths[bucket_idx] <= 0)
            return;
        -- localDepths[bucket_idx];
    }

    /**
     * VerifyIntegrity
     *
     * Verify the following invariants:
     * (1) All LD <= GD.
     * (2) Each bucket has precisely 2^(GD - LD) pointers pointing to it.
     * (3) The LD is the same at each index with the same bucket_page_id
     * <p>
     *     在任一时刻，bucketPageIds数组中都是指向有效的bucketPageId；所以，无需判断是否有效
     *     (3)指向同一个bucket的entry的local depth都应该相同
     * </p>
     */
    public void VerifyIntegrity() /*const*/ {
        class Pair {
            int localDepth; //指向当前bucket的localDepth
            int numOfPointer;
            public Pair(int localDepth) {
                this.localDepth = localDepth;
                this.numOfPointer = 1;
            }
            public boolean checkIf_OtherLD_eq_ThisLd(int otherLD) {
                return this.localDepth == otherLD;
            }
            public void incrNumOfPointer() {
                ++ numOfPointer;
            }
        }
        final HashMap<Integer, Pair> pageId2numOfPointer = new HashMap<>();// 每一个bucket有多少指针指向它
        for (int idx = 0; idx < bucketPageIds.length; idx++) {
            int bucketPageId = bucketPageIds[idx];
            byte localDepth = localDepths[idx];
            if (localDepth > globalDepth) {
                logger.info("VerifyIntegrity fail, because localDepths["+idx+"] > globalDepth");
                return;
            }
            Pair flag = pageId2numOfPointer.putIfAbsent(bucketPageId, new Pair(localDepth));//如果absent的话，就会返回null
            if (Objects.nonNull(flag)) { //证明之前已经插入了
                if (!flag.checkIf_OtherLD_eq_ThisLd(localDepth)) {
                    logger.info("VerifyIntegrity fail, because OtherLD:"+flag.localDepth+"_not_eq_ThisLd:"+localDepth);
                }
                flag.incrNumOfPointer();
            }
        }
        // test (2) Each bucket has precisely 2^(GD - LD) pointers pointing to it.
        for (Pair pair : pageId2numOfPointer.values()) {
            if ( pair.numOfPointer != Math.pow(2, globalDepth - pair.localDepth) ) {
                logger.info("VerifyIntegrity fail, because pair.numOfPointer != Math.pow(2, globalDepth - pair.localDepth)");
                return;
            }
        }
    }

    /**
     * Prints the current directory
     */
    public void PrintDirectory() /*const*/ {

    }


}
