package buffer;

import config.DBConfig;
import disk.DiskManager;
import disk.Page;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class TestBufferPoolManager {
    @Test
    public void BinaryDataTest() throws ExecutionException, InterruptedException {
        String dbName = "test.db";
        int bufferpoolSize = 10;
        DiskManager diskManager = new DiskManager();
        BufferPoolManager bufferPoolManager = new BufferPoolManager(bufferpoolSize,  diskManager);

        // 场景：缓冲池为空。我们应该能够创建一个新页面。
        Page page0 = bufferPoolManager.newPage();
        assertNotNull(page0);
        assertEquals(page0.getPageId(), 0);

        // 生成随机二进制数据
        char[] randomBinaryData = new char[DBConfig.BUSTUB_PAGE_SIZE];
        Random random = new Random();
        byte[] randomByte = new byte[DBConfig.BUSTUB_PAGE_SIZE];
        random.nextBytes(randomByte);
        for (int i = 0; i < randomByte.length; i++) {
            randomBinaryData[i] = (char) (randomByte[i] & 0xFF);;
        }
        // 在中间和结尾插入终端字符
        randomBinaryData[DBConfig.BUSTUB_PAGE_SIZE / 2] = '\0';
        randomBinaryData[DBConfig.BUSTUB_PAGE_SIZE - 1] = '\0';

        // 场景：有了页面后，我们应该能够读写内容。
        page0.setData(randomBinaryData);
        for (int i = 0; i < DBConfig.BUSTUB_PAGE_SIZE; i++) {
            assertEquals(page0.getData()[i], randomBinaryData[i]);
        }
        // 场景：在填充缓冲池之前，我们应该能够创建新页面。
        for (int i = 1; i < bufferpoolSize; ++i) {
            assertNotNull(bufferPoolManager.newPage());
        }

        // 场景：一旦缓冲池已满，我们就不能创建任何新页面。
        for (int i = bufferpoolSize; i < bufferpoolSize * 2; ++i) {
            assertNull(bufferPoolManager.newPage());
        }

        // 场景：取消固定页面{0，1，2，3，4}并固定另外5个新页面后，
        // 仍然有一个缓存帧可以读取第0页。
        for (int i = 0; i < 5; ++i) {
            assertEquals(true, bufferPoolManager.UnpinPage(i, true));
            bufferPoolManager.FlushPage(i);
        }

        for (int i = 0; i < 5; ++i) {
            Page page = bufferPoolManager.newPage();
            assertNotNull(page);
            bufferPoolManager.UnpinPage(page.getPageId(), false);
        }

        //场景：我们应该能够获取我们之前编写的数据。
        page0 = bufferPoolManager.FetchPage(0);
        for (int i = 0; i < DBConfig.BUSTUB_PAGE_SIZE; i++) {
            assertEquals(page0.getData()[i], randomBinaryData[i]);
        }
        File file = new File("test.db");
        file.delete();
        file = new File("test.log");
        file.delete();
    }
}
