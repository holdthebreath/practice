package code;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

public class Volatile {
    public static void main(String[] args) {
        test();
    }

    private static void test() {
        Test test = new Test();
        Thread t1 = new Thread(test::write);

        Thread t2 = new Thread(test::read);
        t1.start();
        t2.start();
        ReentrantLock lock = new ReentrantLock();
        System.out.println(test.i);
    }


    private static class Test {
        private int i = 0;
        private volatile boolean flag = false;

        public Test() {
        }

        public void write() {
            i++;
            flag = true;
        }

        public void read() {
            if (flag) {
                i = i * i;
            }
        }

    }
}
