# HashMap

原理：
![](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/20210506112429.webp)

1. 数组 + 链表（hash 有可能会一样）
2. 公式---> index = HashCode（Key） & （Length - 1）(扩容需要重新 hash)
3. Java8 尾插法（7 多线程头插法有可能会导致环形链表）但还是线程不安全，get/put 没有加同步锁，无法保证上一秒 put 的值，下一秒
   get 的时候还是原值。
4. 默认初始化位运算值为16位与运算比算数计算的效率高了很多。选择 16，是为了服务将 Key 映射到 index 的算法。因为在使用不是 2
   的幂的数字时，Length-1 的值是所有二进制位全为 1，这种情况下，index 的结果等同于 HashCode 后几位的值。只要输入的 HashCode
   本身分布均匀，Hash 算法的结果就是均匀的。
5. 重写 equals 方法的时候需要重写 hashCode：保证相同的对象返回相同的 hash 值，不同的对象返回不同的 hash 值
6. Hashmap 中的链表大小超过 8 个时会自动转化为红黑树，当删除小于 6 时重新变为链表：根据泊松分布，在负载因子默认为 0.75
   的时候，单个 hash 槽内元素个数为 8 的概率小于百万分之一，所以将 7 作为一个分水岭，等于 7 的时候不转换，大于等于 8
   的时候才进行转换，小于等于 6 的时候就化为链表.

## 1.8 的 ConcurrentHashMap

1. 采用了 CAS + synchronized 来保证并发安全性。把之前的 HashEntry 改成了 Node，但是作用不变，把值和 next 采用了 volatile
   去修饰，保证了可见性，并且也引入了红黑树，在链表大于一定值的时候会转换（默认是 8）
2. Put 过程：首先计算 hash；判断是否需要初始化；否则为当前 key 定位出的 Node，如果为空表示当前位置可以写入数据，利用 CAS
   尝试写入，失败则自旋保证成功；如果当前位置的 hashcode == MOVED == -1,则需要进行扩容。如果都不满足，则利用 synchronized
   锁写入数据。如果数量大于 TREEIFY_THRESHOLD 则要转换为红黑树。
3. Get 过程：根据计算出来的 hashcode 寻址，如果就在桶上那么直接返回值。如果是红黑树那就按照树的方式获取值。就不满足那就按照链表的方式遍历获取值。