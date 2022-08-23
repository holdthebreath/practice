# ThreadLocal

## 目的

JDK1.2以来的一个java.lang包下的一个类，主要目的是提供线程本地的变量，也叫做线程本地变量。
用做数据隔离，填充的数据只属于当前线程，变量的数据对别的线程而言是相对隔离的，在多线程环境下，防止自己的变量被其它线程篡改。

### 实现

为每个线程都创建了一个本地变量，实际上是ThreadLocal为变量在每个线程中都创建了一个副本，使得每个线程都可以访问自己内部的副本变量
ThreadLocal并不是为了解决多线程共享变量同步的问题，而是为了让每个线程的变量不互相影响，相当于线程之间操纵的都是变量的副本

### 场景：Spring

采用ThreadLocal的方式保证单个线程中的数据库操作使用的是同一个数据库连接，同时，采用这种方式可以使业务层使用事务时不需要感知并管理
connection 对象，通过传播级别，管理多个事务配置之间的切换，挂起和恢复。

### 常用方法

    public T get() {}
	public void set(T value) {}
	public void remove() {}
	protected T initialValue() {}

#### ThreadLocalMap

并未实现 Map 接口，而且他的 Entry 是继承 WeakReference（弱引用）的，也没有看到 HashMap 中的 next，不存在链表了。
![](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/20210506110608.webp)

##### 为什么使用数组

一个线程可以有多个TreadLocal来存放不同类型的对象的，都将放到当前线程的ThreadLocalMap里

##### 插入过程

ThreadLocalMap给每一个ThreadLocal对象一个threadLocalHashCode，插入过程中根据ThreadLocal对象的hash值，定位到
table中的位置 i，**int i = key.threadLocalHashCode & (len-1)**。判断如果当前位置是空的，就初始化一个Entry对象放在位置i上。如果位置i不为空且Entry
对象的key正好是即将设置的key，则更新Entry中的value。如果位置i的不为空且key不相等，则继续找下一个位置，直到为空为止。

### ThreadLocal内存泄露

![](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/202208221734955.png)
弱引用: 如果一个对象是偶尔(很少)的使用，并且希望在使用时随时就能获取到，但又**不想影响此对象的垃圾收集**
，用WeakReference来记住此对象。
ThreadLocalMap使用ThreadLocal的弱引用作为key，如果一个ThreadLocal没有外部强引用来引用它，那么系统GC
时，这个ThreadLocal势必会被回收，这样一来，ThreadLocalMap中就会出现key为null的Entry，就没有办法访问这些key为null的Entry的value。
如果当前线程再迟迟不结束的话，这些key为null的Entry的value就会一直存在一条强引用链：**Thread
Ref -> Thread -> ThreadLocalMap -> Entry -> value永远无法回收，造成内存泄漏**。
其实，ThreadLocalMap的设计中已经考虑到这种情况，也加上了一些防护措施：在ThreadLocal的get(),set(),remove()
的时候都会清除线程ThreadLocalMap里所有key为null的value。 但是这些被动的预防措施并不能保证不会内存泄漏：

使用static的ThreadLocal，延长了ThreadLocal的生命周期，可能导致的内存泄漏
分配使用了ThreadLocal又不再调用get(),set(),remove()方法，那么就会导致内存泄漏 。

最后一定要用remove()把全部值清空

