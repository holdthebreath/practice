package code;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPrintlnSynchronized {
    static int index = 0;
    public static void main(String[] args) {
        Object lock = new Object();
        Thread wenThread = new Thread(() -> {
            try {
                synchronized (lock) {
                    while (index <= 100) {
                        while (index % 2 != 0) {
                            lock.wait();
                        }
                        System.out.println("wen");
                        index++;
                        lock.notify();
                    }
                }
            } catch (Exception e) {

            }
        });


        Thread diThread = new Thread(() -> {
            try {
                synchronized (lock) {
                    while (index <= 100) {
                        while (index % 2 == 0) {
                            lock.wait();
                        }
                        System.out.println("di");
                        index++;
                        lock.notify();
                    }
                }
            } catch (Exception e) {

            }
        });
        wenThread.start();
        diThread.start();
    }
}