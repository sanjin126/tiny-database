package buffer;

import java.util.Optional;

public class LRUKReplacer implements Replacer{
    @Override
    public Optional<Integer> victim() {
        return Optional.empty();
    }

    @Override
    public void pin(int frame_id) {

    }

    @Override
    public void unpin(int frame_id) {

    }

    @Override
    public int getSize() {
        return 0;
    }

    /**
     * TODO 可以使用动态代理来修改 BufferPoolManager的行为，来进行对于页面访问的记录，同时不影响Replacer
     * @param frameId
     */
    public void recordAccess(int frameId) {

    }
}
