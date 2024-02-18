package code;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Test {
    private static volatile boolean ready = false; // volatile变量
    private static int number = 0; // 普通变量

    public static void main(String[] args) throws InterruptedException {
        // 修改变量的线程
        Thread thread1 = new Thread(() -> {
            number = 100; // 修改普通变量
            ready = true; // 写入volatile变量，通知其他线程变更已发生
        });

        // 读取变量的线程
        Thread thread2 = new Thread(() -> {
            while (!ready) {
                // 在ready不为true时循环等待
            }
            System.out.println("Thread 2 reads number: " + number); // 读取普通变量
        });

        thread2.start();
        thread1.start();

        thread1.join();
        thread2.join();
    }
}

