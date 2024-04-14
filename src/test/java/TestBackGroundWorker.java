import java.util.ArrayList;
import java.util.List;

public class TestBackGroundWorker {

    private String str = "a string";
    private List<String> list = new ArrayList<>();

    public TestBackGroundWorker() {
        list.add(str);
        Runnable task = this::startWorker;
        new Thread(task,"线程2").start();
    }

    public void startWorker() {
        while (true) {
            System.out.println(list);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    /*
    即使 main线程退出，其他线程也会继续执行
     */
    public static void main(String[] args) {
        TestBackGroundWorker worker = new TestBackGroundWorker();
        new Thread(()->{
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("click");
            }
        }, "线程1").start();
        worker = null;
        System.out.println("main exit");
    }
}
