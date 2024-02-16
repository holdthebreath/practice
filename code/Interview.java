package code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Interview {
//  题目二：
//Question A：有100个任务需要分成10批执行，每批执行有顺序（注意注意注意：即第一批执行完执行第二批）。
//● 说明：10批任务有序执行，每批任务的10个任务要做到并发执行
//● 加分项：如果可以，写出多种不同原理或不同工具的实现方式
//请把main方法以及控制台打印的日志，也贴进来！


//    public static void main(String[] args) {
//        ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10));
//        AtomicInteger task = new AtomicInteger(1);
//        Object lock = new Object();
//        for (int j = 0; j < 10; j++) {
//            List<CompletableFuture<Void>> futureList = new ArrayList<>();
//            for (int i = 0; i < 10; i++) {
//                futureList.add(CompletableFuture.runAsync(() -> {
//                    synchronized (lock) {
//                        System.out.println(task.get());
//                        task.incrementAndGet();
//                    }
//                }, executor));
//            }
//            CompletableFuture<Void> allFuture = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
////            for (CompletableFuture<Void> e : futureList) {
////                allFuture = CompletableFuture.allOf(e);
////            }
//            allFuture.join();
//        }
//    }

//    题目三：
//
//Question A：有如下两张表，请统计各公司每天的营业额和各公司每天的累积营业额
//● 说明：累积营业额是指：比如第一天是100，第二天是200，那么第二天累积营业额就是300
//
//表：order表
//订单ID    交易日期    金额     门店ID
//order_id trade_date	amount	shop_id
//
//表：shop 门店表
//门店ID   公司ID
//shop_id	company_id
//
//QuestionB：如何建索引能提高查询速度

//    SQL开始：
//
//SELECT company_id, company_day_money,
//SUM(company_day_money) OVER (PARTITION BY company_id ORDER BY trade_date rows between unbound preceding and current row) AS company_leisi_money,
//trade_date
//FROM (
//  SELECT company_id,
//  SUM(amount) OVER(PARTITION BY company_id, trade_date ORDER BY company_id, trade_date) AS company_day_money,
//  trade_date
//FROM order a LEFT JOIN  shop b ON a.shop_id = b.shop_id
//) c;
//
//索引方案：
//ALTER TABLE order ADD PRIMARY KEY order_id;
//ALTER TABLE shop ADD PRIMARY KEY company_id;
//ALTAR TABLE order ADD INDEX ix_date(shop_id, trade_date);


//    public static void main(String[] args) {
//        ArrayList<String> list = new ArrayList<>();
//        String c = "a";
//        String a = "";
//        String d = "d";
//        String e = "c";
//        String b = "";
//        list.add(d);
//        list.add(e);
//        list.add(a);
//        list.add(b);
//        list.add(c);
////        sort(list);
//        Collections.sort(list);
//        for (String s : list) {
//            System.out.println(s);
//        }
//    }

    //题目四：
    //请说说这段代码错误在哪里，如何修改（写出修改后的代码）
    //
    //说明：输入的list 中的str串都不为null，但可能是"".
    //
//    public void sort(List<String> list) {
//        Collections.sort(list, (o1, o2) -> {
//            if ("".equals(o1) || "".equals(o2)){
//                return 0;
//            }
//            return o1.compareTo(o2);
//        });
//    }

    public static void sort(List<String> list) {
        Collections.sort(list, String::compareTo);
    }


//    题目一：
//    有字符串yujwedjhccdskdsewirewrwsadm,fnsdklwweqpq取出所有所有所有两个w之间的字符串并用归并排序进行排序 （字符串处理）。
//    请把main方法以及控制台打印的日志，也贴进来！
//    代码开始：

    public static void main(String[] args) {
        String a = "yujwedjhccdskdsewirewrwsadm,fnsdklwweqpq";
        Pattern pattern = Pattern.compile("(?<=w).*?(?=w)");
        Matcher matcher = pattern.matcher(a);
        List<String> index = new ArrayList<>();
        while (matcher.find()) {
            index.add(matcher.group());
        }
        String[] array = index.toArray(new String[index.size()]);
        mergeSort(array, array.length);
        for (String s : array) {
            System.out.println(s);
        }
    }

    public static void mergeSort(String[] array, int n){
        if (array == null || n < 2) {
            return;
        }
        divideSort(array, 0, n - 1);
    }

    public static void divideSort(String[] array, int left, int right){
        if (left >= right) {
            return;
        }
        int mid = (left + right) / 2;
        divideSort(array, left, mid);
        divideSort(array, mid + 1, right);
        binaryMerge(array, left, mid, right);
    }

    public static void binaryMerge(String[] array, int left, int mid, int right){
        String[] temp = new String[right - left + 1];
        int l = left;
        int r = mid + 1;
        int index = 0;
        while (l <= mid && r <= right) {
            if (array[l].compareTo(array[r]) < 0) {
                temp[index] = array[l];
                index++;
                l++;
            } else {
                temp[index] = array[r];
                index++;
                r++;
            }
        }
        while (l <= mid) {
            temp[index] = array[l];
            index++;
            l++;
        }
        while (r <= right) {
            temp[index] = array[r];
            index++;
            r++;
        }
        for (int t = 0; t < temp.length; t++) {
            array[left + t] = temp[t];
        }
    }

}
