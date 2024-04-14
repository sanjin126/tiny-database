package buffer;

import com.sun.tools.javac.Main;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 *  LRUReplacer implements the lru replacement policy, which approximates the Least Recently Used policy.
 *
 */
// TODO 修改为细粒度的锁
public class LRUReplacer implements Replacer{
    private final Logger logger = Logger.getLogger(LRUReplacer.class.getName());
    private int size;
    private final int capacity;

    //业务的实现
    //Map<frameId, Node>
    private final Map<Integer, LinkedDeque<Integer>.Node> cache;
    //<frameId>
    private final LinkedDeque<Integer> victimList = new LinkedDeque<>();

    // TODO
    class LinkedDeque<T> {



        class Node {
            T item;
            Node prev;
            Node next;

            public Node(T item, Node prev, Node next) {
                this.item = item;
                this.prev = prev;
                this.next = next;
            }
        }
        Node dummy;
        int size;
        public LinkedDeque() {
            this.dummy = new Node(null, null, null);
            dummy.prev = dummy.next = dummy;
            this.size = 0;
        }

        public Optional<Node> getFirst() {
            return Optional.ofNullable(dummy.next);
        }

        public Optional<Node> getLast() {
            return Optional.ofNullable(dummy.prev);
        }

        // 支持任意位置的O（1）删除，配合Map，实现O（1）的victim策略
        public void remove(Node n) {
            n.prev.next = n.next;
            n.next.prev = n.prev;

            size --;
        }

        public void moveToHead(Node node) {
            remove(node);
            addFirst(node);
        }

        private void add(Node node, Node nodeTobeAdd) {
            nodeTobeAdd.prev = node;
            nodeTobeAdd.next = node.next;
            node.next.prev = nodeTobeAdd;
            node.next = nodeTobeAdd;
            size ++;
        }
        public void addFirst(Node node) {
            add(dummy, node);
        }

    }

    public LRUReplacer(int num_pages) {
        this.size = 0;
        this.capacity = num_pages;
        this.cache = new HashMap<>();
    }

    // 返回frame_id
    @Override
    public synchronized Optional<Integer> victim() {
        Optional<LinkedDeque<Integer>.Node> last = victimList.getLast();
        if (last.isPresent()) {
            cache.remove(last.get().item);
            victimList.remove(last.get());
            size --;
        }
        return last.map(node -> node.item); //if node null, return null Optional. Otherwise, return Optional<frameId>
    }

    // 删除frame_id
    @Override
    public synchronized void pin(int frame_id) {
        if (cache.containsKey(frame_id)) {
            LinkedDeque<Integer>.Node nodeTobeRemove = cache.get(frame_id);
            victimList.remove(nodeTobeRemove);
            cache.remove(frame_id); // node can be garbage collect
            size --;
        }
    }

    // 如果一个页被重新fetch的话（在BufferManage中），那么其一定会被pin，所以当其不被使用后，其就会重新unpin，这样就可以更新其在lru中的位置
    @Override
    public synchronized void unpin(int frame_id) {
        if (frame_id < 0) return;
        if (cache.containsKey(frame_id)) {
            return;
        }
        // 检查是否超出容量限制
        if (capacity == getSize()) {
            logger.log(Level.WARNING, "超出cache的容量限制");
            return;
        }
        LinkedDeque<Integer>.Node nodeTobeAdd = victimList.new Node(frame_id, null, null);
        cache.put(frame_id, nodeTobeAdd);
        victimList.addFirst(nodeTobeAdd);
        size ++;
    }

    @Override
    public int getSize() {
        return size;
    }
}
