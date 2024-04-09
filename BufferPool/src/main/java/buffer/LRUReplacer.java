package buffer;

import java.util.Optional;

// just like implement LRU algorithm
public class LRUReplacer implements Replacer{

    private class LRUCache {

    }

    // 返回frame_id
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
}
