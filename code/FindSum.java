package code;

import java.util.*;

public class FindSum {


    static List<List<Integer>> lists = new ArrayList<>();
    static LinkedList<Integer> result = new LinkedList<>();


    public static List<List<Integer>> test(Integer n, Integer sum) {
        int arr[] = new int[sum + 1];
        for (int i = 0; i < sum + 1; i++) {
            arr[i] = i;
        }
        for (int i = 0; i < sum + 1; i++) {
            List<Integer> result = new ArrayList<>();
            result.add(i);
            find(result, i, sum, n - 1);

        }
        return lists;
    }

    public static void main(String[] args) {
        for (List list : test(3, 5)) {
            System.out.println(list);
        }
    }

    public static boolean find(List<Integer> result, int nowSum, int sum, int n) {
        if (n == 1) {
            result.add(sum - nowSum);
            lists.add(result);
            return true;
        } else {
            List<Integer> index = new ArrayList<>();
            index.addAll(result);
            for (int i = 0; i < sum; i++) {
                nowSum += i;
                if (nowSum < sum) {
                    result.add(i);
                    if (find(result, nowSum, sum, n - 1)) {
                        result.clear();
                        result.addAll(index);
                    }
                }
            }
        }
        return false;
    }

}
