import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;

import static java.nio.file.StandardOpenOption.*;

public class TestFileChannel {
    @Test
    public void testCreate() {
        try {
            ByteBuffer data = ByteBuffer.allocate(10);
            for (int i = 0; i < 10; i++) {
                byte[] putData = String.valueOf((char) ('a' + i)).getBytes(StandardCharsets.UTF_16);
                System.out.println(Arrays.toString(putData));
                data.put(putData);
            }
            data.flip();
            SeekableByteChannel channel = Files.newByteChannel(Path.of("test.txt"), EnumSet.of(CREATE,READ,WRITE));
            channel.write(data);
            System.out.println(channel.position());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test() throws IOException {
        /**
         * 使用FileWriter进行写入的时候，默认编码格式是Charset.defaultCharset()，即UTF-8
         */
        FileWriter writer = new FileWriter("test.txt");
        System.out.println(Charset.defaultCharset());
        writer.write(new char[]{'a','b'});
        writer.close();
    }
}
