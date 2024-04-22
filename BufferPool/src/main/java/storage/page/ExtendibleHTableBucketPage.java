package storage.page;

import annotation.UnsignedInt;
import impletation.Pair;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Optional;

import static config.DBConfig.BUSTUB_PAGE_SIZE;

/**
 * Bucket page for extendible hash table.
 */
public class ExtendibleHTableBucketPage<KeyType, ValueType, KeyComparator> implements Serializable {
    private static final @UnsignedInt int HTABLE_BUCKET_PAGE_METADATA_SIZE = Integer.BYTES * 2;
    private static @UnsignedInt int HTableBucketArraySize(@UnsignedInt int mapping_type_size) {
        return (BUSTUB_PAGE_SIZE - HTABLE_BUCKET_PAGE_METADATA_SIZE) / mapping_type_size;
    };

    /**
     * The number of key-value pairs the bucket is holding
     */
    private @UnsignedInt int size;
    /**
     * The maximum number of key-value pairs the bucket can handle
     */
    private @UnsignedInt int maxSize;
    /**
        MappingType array_[HTableBucketArraySize(sizeof(MappingType))];
        #define MappingType std::pair<KeyType, ValueType>
        用于计算array的大小
     */
    private transient int sizeOfPair; //TODO 不应该有该变量，其会占据空间，从而导致ArraySize的大小计算错误

    private Pair<KeyType, ValueType>[] array /* = new Pair[sizeOfPair]*/; //TODO 注意类的加载顺序

    private ExtendibleHTableBucketPage(){
    } // = delete();

    /**
     * After creating a new bucket page from buffer pool, must call initialize
     * method to set default values
     * @param max_size Max size of the bucket array
     */
    public void init(/*int max_size = HTableBucketArraySize(sizeof(MappingType))*/int sizeOfKeyType, int sizeOfValueType) {
        this.sizeOfPair = sizeOfKeyType + sizeOfValueType;
        this.init(HTableBucketArraySize( this.sizeOfPair ), sizeOfKeyType, sizeOfValueType);
    }
    public void init(@UnsignedInt int max_size , int sizeOfKeyType, int sizeOfValueType) {
        this.maxSize = max_size;
        this.sizeOfPair = sizeOfKeyType + sizeOfValueType;
        array = new Pair[maxSize]; //TODO 检验是否被正确的计算
        this.size = 0;
        assert maxSize <= HTableBucketArraySize( this.sizeOfPair ); // 保证其大小在一个page内
    }

    /**
     * Lookup a key
     *
     * @param key key to lookup
     * @param[out] value value to set
     * @param cmp the comparator
     * @return null 表示没有找到 <del>true if the key and value are present, false if not found.</del> 
     */
    public Optional<ValueType> lookup(/*const*/ KeyType key, /*ValueType value,*/ /*const KeyComparator*/ Comparator<KeyType> cmp) /*const*/ {
        for (Pair<KeyType, ValueType> pair : array) {
            KeyType currentKey = pair.first;
            int result = cmp.compare(currentKey, key);
            if (result == 0) {
                return Optional.of(pair.second); //返回找到的value
            }
        }
        return Optional.empty(); //表示没有找到
    }

    /**
     * Attempts to insert a key and value in the bucket.
     *
     * @param key key to insert
     * @param value value to insert
     * @param cmp the comparator to use
     * @return true if inserted, false if bucket is full or the same key is already present
     */
    public boolean Insert(/*const*/ KeyType key, /*const*/ ValueType value, /*const*/ Comparator<KeyType> cmp) {
        if (size >= maxSize) //full
            return false;
        Optional<ValueType> optionalValue = lookup(key, cmp);
        if (optionalValue.isPresent()) { //表示已经存在
            return false;
        }
        array[size] = new Pair<KeyType, ValueType>(key, value);
        ++ size;
        return true;
    }

    /**
     * Removes a key and value.
     *
     * @return true if removed, false if not found
     */
    public boolean Remove(/*const*/ KeyType key, /*const*/ Comparator<KeyType> cmp) {
        int bucketIdx = -1;
        for (int i = 0; i < array.length; i++) {
            Pair<KeyType, ValueType> pair = array[i];
            KeyType currentKey = pair.first;
            int result = cmp.compare(currentKey, key);
            if (result == 0) {
                bucketIdx = i;
                break;
            }
        }
        if (bucketIdx != -1) {
            RemoveAt(bucketIdx);
        }
        return false;
    }

    public void RemoveAt(@UnsignedInt int bucket_idx) { //TODO 可以优化
        for (int idx = bucket_idx; idx < size - 1 ; idx++) {
            array[idx] = array[idx + 1];
        }
        size --;
    }

    /**
     * @brief Gets the key at an index in the bucket.
     *
     * @param bucket_idx the index in the bucket to get the key at
     * @return key at index bucket_idx of the bucket
     */
    public KeyType KeyAt(@UnsignedInt int bucket_idx) /*const*/ {
        return array[bucket_idx].first;
    }

    /**
     * Gets the value at an index in the bucket.
     *
     * @param bucket_idx the index in the bucket to get the value at
     * @return value at index bucket_idx of the bucket
     */
    public ValueType ValueAt(@UnsignedInt int bucket_idx) /*const*/  {
        return array[bucket_idx].second;
    }

    /**
     * Gets the entry at an index in the bucket.
     *
     * @param bucket_idx the index in the bucket to get the entry at
     * @return entry at index bucket_idx of the bucket
     */
    public Pair<KeyType, ValueType> EntryAt(@UnsignedInt int bucket_idx) /*const*/ {
        return array[bucket_idx];
    }

    /**
     * @return number of entries in the bucket
     */
    public @UnsignedInt int Size() /*const*/  {
        return size;
    }

    /**
     * @return whether the bucket is full
     */
    public boolean IsFull() /*const*/  {
        return size >= maxSize;
    }

    /**
     * @return whether the bucket is empty
     */
    public boolean IsEmpty() /*const*/  {
        return size == 0;
    }

    /**
     * Prints the bucket's occupancy information
     */
    public void PrintBucket() /*const*/ {

    }

}
