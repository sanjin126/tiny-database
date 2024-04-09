package disk;

public class Page {
    private static final int PAGE_SIZE = 4096;
    private final char[] data = new char[PAGE_SIZE];
    private int pageId;
    private int pinCount = 0;
    private boolean isDirty = false;


}
