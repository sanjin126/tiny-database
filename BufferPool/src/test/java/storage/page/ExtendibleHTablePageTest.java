package storage.page;

import buffer.BufferPoolManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import storage.disk.DiskManager;

import java.util.Comparator;
import java.util.Optional;

import static storage.page.Page.INVALID_PAGE_ID;

public class ExtendibleHTablePageTest {
    @Test
    public void BucketPageSampleTest() {
        var disk_mgr = new DiskManager("test.dbf");
        var bpm = new BufferPoolManager(5, disk_mgr);

        int bucket_page_id = INVALID_PAGE_ID;
        {
            BasicPageGuard guard = bpm.newPageGuarded();
            bucket_page_id = guard.getPageId();

            ExtendibleHTableBucketPage<Integer, Integer> bucket_page = guard.AsMut(new ExtendibleHTableBucketPage<Integer, Integer>(4, 4), ExtendibleHTableBucketPage.class );
            bucket_page.init(10, 4, 4);

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
                Assertions.assertEquals(i, ridOptional.get());
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

    @Test
    public void HeaderDirectoryPageSampleTest() {
        var disk_mgr = new DiskManager("test.dbf");
        var bpm = new BufferPoolManager(5, disk_mgr);

        int header_page_id = INVALID_PAGE_ID;
        int directory_page_id = INVALID_PAGE_ID;
        int bucket_page_id_1 = INVALID_PAGE_ID;
        int bucket_page_id_2 = INVALID_PAGE_ID;
        int bucket_page_id_3 = INVALID_PAGE_ID;
        int bucket_page_id_4 = INVALID_PAGE_ID;
        {
            /************************ HEADER PAGE TEST ************************/
            BasicPageGuard header_guard = bpm.newPageGuarded();
            header_page_id = header_guard.getPageId();
            var header_page = header_guard.AsMut(new ExtendibleHTableHeaderPage(), ExtendibleHTableHeaderPage.class);
            header_page.init(2);

    /* Test hashes for header page
    00000000000000001000000000000000 - 32768
    01000000000000001000000000000000 - 1073774592
    10000000000000001000000000000000 - 2147516416
    11000000000000001000000000000000 - 3221258240
    */

            // ensure we are hashing into proper bucket based on upper 2 bits
            long[] hashes = new long[]{32768, 1073774592, 2147516416l, 3221258240l};
            for (int i = 0; i < 4; i++) {
                 Assertions.assertEquals(header_page.hashToDirectoryIndex(hashes[i]), i);
            }

            header_guard.drop();

            /************************ DIRECTORY PAGE TEST ************************/
            BasicPageGuard directory_guard = bpm.newPageGuarded();
            directory_page_id = directory_guard.getPageId();
            var directory_page = directory_guard.AsMut(new ExtendibleHTableDirectoryPage(), ExtendibleHTableDirectoryPage.class);
            directory_page.init(3);

            BasicPageGuard bucket_guard_1 = bpm.newPageGuarded();
            bucket_page_id_1 = bucket_guard_1.getPageId();
            var bucket_page_1 = bucket_guard_1.AsMut(new ExtendibleHTableBucketPage<Integer, Integer>(4, 4), ExtendibleHTableBucketPage.class );
            bucket_page_1.init(10, 4, 4);

            BasicPageGuard bucket_guard_2 = bpm.newPageGuarded();
            bucket_page_id_2 = bucket_guard_2.getPageId();
            var bucket_page_2 = bucket_guard_2.AsMut(new ExtendibleHTableBucketPage<Integer, Integer>(4, 4), ExtendibleHTableBucketPage.class );;
            bucket_page_2.init(10, 4, 4);

            BasicPageGuard bucket_guard_3 = bpm.newPageGuarded();
            bucket_page_id_3 = bucket_guard_3.getPageId();
            var bucket_page_3 = bucket_guard_3.AsMut(new ExtendibleHTableBucketPage<Integer, Integer>(4, 4), ExtendibleHTableBucketPage.class );;
            bucket_page_3.init(10, 4, 4);

            BasicPageGuard bucket_guard_4 = bpm.newPageGuarded();
            bucket_page_id_4 = bucket_guard_4.getPageId();
            var bucket_page_4 = bucket_guard_4.AsMut(new ExtendibleHTableBucketPage<Integer, Integer>(4, 4), ExtendibleHTableBucketPage.class );;
            bucket_page_4.init(10, 4, 4);

            directory_page.SetBucketPageId(0, bucket_page_id_1);

    /*
    ======== DIRECTORY (global_depth_: 0) ========
    | bucket_idx | page_id | local_depth |
    |    0    |    2    |    0    |
    ================ END DIRECTORY ================
    */

            directory_page.VerifyIntegrity();
             Assertions.assertEquals(directory_page.Size(), 1);
             Assertions.assertEquals(directory_page.GetBucketPageId(0), bucket_page_id_1);

            // grow the directory, local depths should change!
            directory_page.SetLocalDepth(0,  1);
            directory_page.IncrGlobalDepth();
            directory_page.SetBucketPageId(1, bucket_page_id_2);
            directory_page.SetLocalDepth(1, 1);

    /*
    ======== DIRECTORY (global_depth_: 1) ========
    | bucket_idx | page_id | local_depth |
    |    0    |    2    |    1    |
    |    1    |    3    |    1    |
    ================ END DIRECTORY ================
    */

            directory_page.VerifyIntegrity();
             Assertions.assertEquals(directory_page.Size(), 2);
             Assertions.assertEquals(directory_page.GetBucketPageId(0), bucket_page_id_1);
             Assertions.assertEquals(directory_page.GetBucketPageId(1), bucket_page_id_2);

            for (int i = 0; i < 100; i++) {
                 Assertions.assertEquals(directory_page.HashToBucketIndex(i), i % 2);
            }

            directory_page.SetLocalDepth(0, 2);
            directory_page.IncrGlobalDepth(); //开始分裂，增大table size为原来的2倍
            directory_page.SetBucketPageId(2, bucket_page_id_3);

    /*
    ======== DIRECTORY (global_depth_: 2) ========
    | bucket_idx | page_id | local_depth |
    |    0    |    2    |    2    |
    |    1    |    3    |    1    |
    |    2    |    4    |    2    |
    |    3    |    3    |    1    |
    ================ END DIRECTORY ================
    */

            directory_page.VerifyIntegrity();
             Assertions.assertEquals(directory_page.Size(), 4);
             Assertions.assertEquals(directory_page.GetBucketPageId(0), bucket_page_id_1);
             Assertions.assertEquals(directory_page.GetBucketPageId(1), bucket_page_id_2);
             Assertions.assertEquals(directory_page.GetBucketPageId(2), bucket_page_id_3);
             Assertions.assertEquals(directory_page.GetBucketPageId(3), bucket_page_id_2);

            for (int i = 0; i < 100; i++) {
                 Assertions.assertEquals(directory_page.HashToBucketIndex(i), i % 4);
            }

            directory_page.SetLocalDepth(0, 3);
            directory_page.IncrGlobalDepth();
            directory_page.SetBucketPageId(4, bucket_page_id_4);

    /*
    ======== DIRECTORY (global_depth_: 3) ========
    | bucket_idx | page_id | local_depth |
    |    0    |    2    |    3    |
    |    1    |    3    |    1    |
    |    2    |    4    |    2    |
    |    3    |    3    |    1    |
    |    4    |    5    |    3    |
    |    5    |    3    |    1    |
    |    6    |    4    |    2    |
    |    7    |    3    |    1    |
    ================ END DIRECTORY ================
    */
            directory_page.VerifyIntegrity();
             Assertions.assertEquals(directory_page.Size(), 8);
             Assertions.assertEquals(directory_page.GetBucketPageId(0), bucket_page_id_1);
             Assertions.assertEquals(directory_page.GetBucketPageId(1), bucket_page_id_2);
             Assertions.assertEquals(directory_page.GetBucketPageId(2), bucket_page_id_3);
             Assertions.assertEquals(directory_page.GetBucketPageId(3), bucket_page_id_2);
             Assertions.assertEquals(directory_page.GetBucketPageId(4), bucket_page_id_4);
             Assertions.assertEquals(directory_page.GetBucketPageId(5), bucket_page_id_2);
             Assertions.assertEquals(directory_page.GetBucketPageId(6), bucket_page_id_3);
             Assertions.assertEquals(directory_page.GetBucketPageId(7), bucket_page_id_2);

            for (int i = 0; i < 100; i++) {
                 Assertions.assertEquals(directory_page.HashToBucketIndex(i), i % 8);
            }

            // uncommenting this code line below should cause an "Assertion failed"
            // since this would be exceeding the max depth we initialized
            // directory_page.IncrGlobalDepth();

            // at this time, we cannot shrink the directory since we have ld = gd = 3
             Assertions.assertEquals(directory_page.CanShrink(), false);

            directory_page.SetLocalDepth(0, 2);
            directory_page.SetLocalDepth(4, 2);
            directory_page.SetBucketPageId(0, bucket_page_id_4);

    /*
    ======== DIRECTORY (global_depth_: 3) ========
    | bucket_idx | page_id | local_depth |
    |    0    |    5    |    2    |
    |    1    |    3    |    1    |
    |    2    |    4    |    2    |
    |    3    |    3    |    1    |
    |    4    |    5    |    2    |
    |    5    |    3    |    1    |
    |    6    |    4    |    2    |
    |    7    |    3    |    1    |
    ================ END DIRECTORY ================
    */

             Assertions.assertEquals(directory_page.CanShrink(), true);
            directory_page.DecrGlobalDepth();

    /*
    ======== DIRECTORY (global_depth_: 2) ========
    | bucket_idx | page_id | local_depth |
    |    0    |    5    |    2    |
    |    1    |    3    |    1    |
    |    2    |    4    |    2    |
    |    3    |    3    |    1    |
    ================ END DIRECTORY ================
    */

             directory_page.VerifyIntegrity();
             Assertions.assertEquals(directory_page.Size(), 4);
             Assertions.assertEquals(directory_page.CanShrink(), false);
        }  // page guard dropped
    }

}
