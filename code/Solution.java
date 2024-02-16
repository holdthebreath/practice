package code;


import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 1个小时，java 实现下
 * 生产消费模式：
 * 1、一个线程模拟生产者，每100ms生产10个元素的字符串加入到ArrayList中，并通知消费者线程消费；
 * 2、两个线程模拟消费者，从ArrayList每次消费一个元素的字符串，将字符串转换为大写以后，重新放入ArrayList队首；
 * 3、消费者线程不能重复处理同一个元素，如果程序没有终止，且ArrayList没有新加入的值，则消费者线程处于等待ArrayList加入新值状态；
 * 4、ArrayList是非线程安全的集合，操作ArrayList应考虑加锁；
 * 5、如果ArrayList中的字符串个数超过100个，程序终止；
 **/

public class Solution {
    private static final int MAX_CAPACITY = 100;

    private class ListNode {
        int val;
    }

    public static void main(String[] args) {
        Arrays.sort(new int[]{}, (a, b) -> a[0] - b[0]);
        PriorityQueue<ListNode> queue = new PriorityQueue<>(10);
        ReentrantLock lock = new ReentrantLock();
        Condition needConsume = lock.newCondition();
        List<String> list = new ArrayList<>();

        int[] ints = {1};
        list.addAll(Collections.list(ints));

        Thread producer = new Thread(() -> {
            while (true) {
                lock.lock();
                try {
                    if (list.size() > MAX_CAPACITY) {
                        System.exit(0);
                    }
                    for (int i = 0; i < 10; i++) {
                        list.add("flypig");
                    }
                    needConsume.signalAll();
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        });


        Thread consumerA = new Thread(() -> {
            while (true) {
                lock.lock();
                try {
                    while (list.size() == 0) {
                        needConsume.await();
                    }
                    String element = list.remove(list.size() - 1);
                    list.add(0, element.toUpperCase());
                    if (list.size() > MAX_CAPACITY) {
                        System.exit(0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        });

        Thread consumerB = new Thread(() -> {
            while (true) {
                lock.lock();
                try {
                    while (list.size() == 0) {
                        needConsume.await();
                    }
                    String element = list.remove(list.size() - 1);
                    list.add(0, element.toUpperCase());
                    if (list.size() > MAX_CAPACITY) {
                        System.exit(0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        });
        producer.start();
        consumerA.start();
        consumerB.start();
    }


    public int[] sortArray(int[] nums) {
        List<Integer> list = new LinkedList<>();
        for (int e : nums) {
            list.add(e);
        }
        List<Integer> integers = quickSort(list);
        return integers.toArray();
    }

    private List<Integer> quickSort(List<Integer> nums) {
        if (nums.size() < 2) {
            return nums;
        }
        int index = nums.get(0);
        List<Integer> greater = new LinkedList<>();
        List<Integer> less = new LinkedList<>();
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] <= index) {
                less.add(nums[i]);
            } else {
                greater.add(nums[i]);
            }
        }
        List<Integer> result = quickSort(left);
        result.add(index);
        result.addAll(quickSort(greater));
        return result;
    }

}
