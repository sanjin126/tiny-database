package impletation;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CustomChannelImpl<T> implements CustomChannel<T>{

    private final Queue<T> queue = new ArrayDeque<>(); //容量没有限制
    private final Lock lock;
    private final Condition notEmpty;
    private final Condition notFull;

    public CustomChannelImpl () {
        this(false);
    }

    public CustomChannelImpl(boolean fair) {
        lock = new ReentrantLock(fair);
        notEmpty = lock.newCondition();
        notFull = lock.newCondition();
    }

    /**
     * Null Elements is prohibited
     * @param element
     */
    @Override
    public void put(T element) throws InterruptedException{
        lock.lockInterruptibly();
        boolean status = queue.offer(element);
        assert status == true;
        notEmpty.signal();
        lock.unlock();
    }

    @Override
    public T get() throws InterruptedException {
        lock.lockInterruptibly();
        try {
           while ( queue.isEmpty() ) {
               notEmpty.await();
           }
           return queue.poll();
        } finally {
           lock.unlock();
        }
    }

    @Override
    public int size() {
        return queue.size();
    }
}
