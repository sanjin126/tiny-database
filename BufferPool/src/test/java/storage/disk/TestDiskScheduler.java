package storage.disk;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static config.DBConfig.BUSTUB_PAGE_SIZE;

public class TestDiskScheduler {
//    TEST(DiskSchedulerTest, DISABLED_ScheduleWriteReadPageTest) {
//        char buf[BUSTUB_PAGE_SIZE] = {0};
//        char data[BUSTUB_PAGE_SIZE] = {0};
//
//        auto dm = std::make_unique<DiskManagerUnlimitedMemory>();
//        auto disk_scheduler = std::make_unique<DiskScheduler>(dm.get());
//
//        std::strncpy(data, "A test string.", sizeof(data));
//
//        auto promise1 = disk_scheduler->CreatePromise();
//        auto future1 = promise1.get_future();
//        auto promise2 = disk_scheduler->CreatePromise();
//        auto future2 = promise2.get_future();
//
//        disk_scheduler->Schedule({/*is_write=*/true, data, /*page_id=*/0, std::move(promise1)});
//        disk_scheduler->Schedule({/*is_write=*/false, buf, /*page_id=*/0, std::move(promise2)});
//
//        ASSERT_TRUE(future1.get());
//        ASSERT_TRUE(future2.get());
//        ASSERT_EQ(std::memcmp(buf, data, sizeof(buf)), 0);
//
//        disk_scheduler = nullptr;  // Call the DiskScheduler destructor to finish all scheduled jobs.
//        dm->ShutDown();
//    }
    @Test
    public void testScheduleWriteReadPageTest() throws ExecutionException, InterruptedException {
        byte[] buf = new byte[BUSTUB_PAGE_SIZE];
        byte[] data = new byte[BUSTUB_PAGE_SIZE];

        DiskManager diskManager = new DiskManager(); //TODO
        DiskScheduler diskScheduler = new DiskScheduler(diskManager);

        byte[] testArray = "A test string.".getBytes();
        for (int i = 0; i < testArray.length; i++) {
            data[i] = testArray[i];
        }

        Future<Boolean> future1 = DiskScheduler.createFuture();
        diskScheduler.schedule(new DiskScheduler.DiskRequest(true, data, 0, future1));
        Future<Boolean> future2 = DiskScheduler.createFuture();
        diskScheduler.schedule(new DiskScheduler.DiskRequest(false, buf, 0, future2));

        Assertions.assertTrue(future1.get());
        Assertions.assertTrue(future2.get());

        Assertions.assertTrue(Arrays.equals(data, buf) == true);

        diskScheduler.shutDown();
        diskManager.shutDown();

    }

    @Test
    void testGetCharArrayLength() {
        char[] arr1 = "Hello".toCharArray();
        System.out.println("getCharArrayLength(arr1) = " + getCharArrayLength(arr1));
        char[] arr2 = new char[8];
        System.out.println("getCharArrayLength(arr2) = " + getCharArrayLength(arr2));
        char[] arr3 = new char[8];
        arr3[0] = '1';
        System.out.println("getCharArrayLength(arr3) = " + getCharArrayLength(arr3));
    }

    static int getCharArrayLength(char[] arr) {
        int len = 0;
        for ( ; len != arr.length && arr[len] != '\0'; len ++ )
            ;
        return len;
    }
}
