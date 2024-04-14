package impletation;

import java.util.List;

public class TestCustomChannelImpl {


    public static void main(String[] args) {
        CustomChannel<String> channel = new CustomChannelImpl<>();
        List<String> list = List.of("hello", "world", "hello", "java");
        list.forEach(x -> {
            try {
                channel.put(x);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        for (int i = 0; i < list.size(); i++) {
            try {
                System.out.println(channel.get());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                    System.out.println("Thread向channel中添加了一个元素");
                    channel.put("exit");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();

        try {
            System.out.println(channel.get());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
