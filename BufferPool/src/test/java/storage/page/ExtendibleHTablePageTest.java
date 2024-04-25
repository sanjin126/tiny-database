package storage.page;

import buffer.BufferPoolManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import storage.disk.DiskManager;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static storage.page.Page.INVALID_PAGE_ID;

public class ExtendibleHTablePageTest {
    @Test
    public void BucketPageSampleTest() throws ExecutionException, InterruptedException {
        var disk_mgr = new DiskManager("test.dbf");
        var bpm = new BufferPoolManager(5, disk_mgr);

        int bucket_page_id = INVALID_PAGE_ID;
        {
            BasicPageGuard guard = bpm.NewPageGuarded();
            bucket_page_id = guard.getPageId();

            ExtendibleHTableBucketPage<Integer, Integer> bucket_page = guard.AsMut(new ExtendibleHTableBucketPage<Integer, Integer>(4, 4), ExtendibleHTableBucketPage.class );
            bucket_page.init(10);

            int index_key;
            int rid;
            Comparator<Integer> comparator = Integer::compare;

            // insert a few (key, value) pairs
            for (int i = 0; i < 10; i++) {
                index_key = i;
                rid = i;
               Assertions.assertTrue(bucket_page.Insert(index_key, rid, comparator));
            }

            index_key = 11;
            rid = 11;
            Assertions.assertTrue(bucket_page.IsFull());
            Assertions.assertFalse(bucket_page.Insert(index_key, rid, comparator));

            // check for the inserted pairs
            for (int i = 0; i < 10; i++) {
                index_key = i;
                Optional<Integer> ridOptional = bucket_page.lookup(index_key, comparator);
                Assertions.assertEquals(rid, ridOptional.get());
            }

            // remove a few pairs
            for (int i = 0; i < 10; i++) {
                if (i % 2 == 1) {
                    index_key = i;
                    Assertions.assertTrue(bucket_page.Remove(index_key, comparator));
                }
            }

            for (int i = 0; i < 10; i++) {
                if (i % 2 == 1) {
                    // remove the same pairs again
                    index_key = i;
                    Assertions.assertFalse(bucket_page.Remove(index_key, comparator));
                } else {
                    index_key = i;
                    Assertions.assertTrue(bucket_page.Remove(index_key, comparator));
                }
            }

            Assertions.assertTrue(bucket_page.IsEmpty());
        }  // page guard dropped
    }

}
