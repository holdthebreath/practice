package code;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPrintln {
    private static ReentrantLock lock = new ReentrantLock();

    private static int index = 0;
    private static Condition conditionA = lock.newCondition();
    private static Condition conditionB = lock.newCondition();


    public static void main(String[] args) {


        Thread wen = new Thread(() -> {
            try {
                lock.lock();
                while (index <= 100) {
                    if (index % 2 != 0)
                        conditionA.await();
                    System.out.println("wen");
                    index++;
                    conditionB.signal();
                }
            } catch (InterruptedException exception) {
                exception.printStackTrace();
                // Restore interrupted state...
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });


        Thread di = new Thread(() -> {
            try {
                lock.lock();
                while (index <= 100) {
                    if (index % 2 != 1)
                        conditionB.await();
                    System.out.println("di");
                    index++;
                    conditionA.signal();
                }
            } catch (InterruptedException exception) {
                exception.printStackTrace();
                // Restore interrupted state...
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });

        wen.start();
        di.start();

    }
}
