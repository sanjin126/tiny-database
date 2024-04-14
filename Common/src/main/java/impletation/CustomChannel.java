package impletation;

// TODO 实现此类
public interface CustomChannel<T> {
    void put(T element) throws InterruptedException;

    T get() throws InterruptedException;

    int size();
}
