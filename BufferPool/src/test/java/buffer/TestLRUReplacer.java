package buffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.rmi.dgc.Lease;
import java.util.Optional;

public class TestLRUReplacer {
    @Test
    public void overflowTest() {
        LRUReplacer lruReplacer = new LRUReplacer(2);

        // unpin(3) should fail
        lruReplacer.unpin(0);
        lruReplacer.unpin(1);
        lruReplacer.unpin(3);

        Assertions.assertEquals(2, lruReplacer.getSize());
    }

    @Test
    public void invalidTest() {
        LRUReplacer lru_replacer = new LRUReplacer(3);
        lru_replacer.unpin(1);
        lru_replacer.unpin(-1);
        lru_replacer.unpin(3);

        Assertions.assertEquals(2, lru_replacer.getSize());

        lru_replacer.pin(0);
        lru_replacer.pin(-1);
        lru_replacer.pin(3);
        lru_replacer.pin(1);

        Assertions.assertEquals(0, lru_replacer.getSize());
    }

    @Test
    public void SampleTest() {
        LRUReplacer lru_replacer = new LRUReplacer(7);

        // Scenario: unpin six elements, i.e. add them to the replacer.
        lru_replacer.unpin(1);
        lru_replacer.unpin(2);
        lru_replacer.unpin(3);
        lru_replacer.unpin(4);
        lru_replacer.unpin(5);
        lru_replacer.unpin(6);
        lru_replacer.unpin(1);
        Assertions.assertEquals(6, lru_replacer.getSize());

        // Scenario: get three victims from the lru.
        Optional<Integer> value;
        value = lru_replacer.victim();
        Assertions.assertEquals(1, value.get());
        value = lru_replacer.victim();
        Assertions.assertEquals(2, value.get());
        value = lru_replacer.victim();
        Assertions.assertEquals(3, value.get());

        // Scenario: pin elements in the replacer.
        // Note that 3 has already been victimized, so pinning 3 should have no effect.
        lru_replacer.pin(3);
        lru_replacer.pin(4);
        Assertions.assertEquals(2, lru_replacer.getSize());

        // Scenario: unpin 4. We expect that the reference bit of 4 will be set to 1.
        lru_replacer.unpin(4);

        // Scenario: continue looking for victims. We expect these victims.
        value = lru_replacer.victim();
        Assertions.assertEquals(5, value.get());
        value = lru_replacer.victim();
        Assertions.assertEquals(6, value.get());
        value = lru_replacer.victim();
        Assertions.assertEquals(4, value.get());

        // find a victim from an empty lru, should have no effects
        value = lru_replacer.victim();
        Assertions.assertFalse(value.isPresent());
    }

    @Test
    public void MultiThreadPinUnpinTest() throws InterruptedException {
        LRUReplacer lru_replacer = new LRUReplacer(1024);

        class UnpinThread extends Thread {
            final int frameId;

            public UnpinThread(int id) {
                this.frameId = id;
            }

            @Override
            public void run() {
                // NOLINTNEXTLINE
                System.out.printf("Thread %s tries to unpin the frames starting from id = %d%n", String.valueOf(this.getId())
                        // NOLINTNEXTLINE
                        , frameId);
                for (int i = frameId; i < frameId + 256; i++) {
                    lru_replacer.unpin(i);
                }
            }
        }

        UnpinThread unpinThread1 = new UnpinThread(0);
        UnpinThread unpinThread2 = new UnpinThread(256);
        UnpinThread unpinThread3 = new UnpinThread(512);
        UnpinThread unpinThread4 = new UnpinThread(768);


        unpinThread1.start();
        unpinThread2.start();
        unpinThread3.start();
        unpinThread4.start();
        unpinThread1.join();
        unpinThread2.join();
        unpinThread3.join();
        unpinThread4.join();

        Assertions.assertEquals(1024, lru_replacer.getSize());

        class PinThread extends Thread {
            final int frameId;

            public PinThread(int id) {
                this.frameId = id;
            }

            @Override
            public void run() {
                // NOLINTNEXTLINE
                System.out.printf("Thread %s tries to pin the frames starting from id = %d%n", String.valueOf(this.getId())
                        // NOLINTNEXTLINE
                        , frameId);
                for (int i = frameId; i < frameId + 256; i++) {
                    lru_replacer.pin(i);
                }
            }
        }


        PinThread pinThread1 = new PinThread(0);
        PinThread pinThread2 = new PinThread(256);
        PinThread pinThread3 = new PinThread(512);
        PinThread pinThread4 = new PinThread(768);


        pinThread1.start();
        pinThread2.start();
        pinThread3.start();
        pinThread4.start();
        pinThread1.join();
        pinThread2.join();
        pinThread3.join();
        pinThread4.join();

        Assertions.assertEquals(0, lru_replacer.getSize());
    }
}
