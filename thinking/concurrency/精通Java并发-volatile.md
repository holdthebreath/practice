```text
还记得第一次面试的时候，面试官问了这个，而我都没有看到过这个关键字...
```

### volatile的重要性

1. volatile是JUC的基石
2. 面试时让你手撕DCL单例

## volatile内存语义

```text
volatile是轻量级的synchronized，它在多处理器开发中保证了共享变量的“可见性”。
可见性的意思是当一个线程修改一个共享变量时，另外一个线程能读到这个修改的值。
如果volatile变量修饰符使用恰当的话，它比synchronized的使用和执行成本更低，因为它不会引起线程上下文的切换和调度。

方腾飞,魏鹏,程晓明. Java并发编程的艺术 (Java核心技术系列) (Chinese Edition) (p. 29). 机械工业出版社. Kindle Edition. 
```

核心只要抓住一点，只保证**可见性**。而可见性是线程安全问题中最不好理解的一个概念，
因为习惯了**happens-before**和**as-if-serial**语义给我们营造幻觉中的我们潜意识有了太多“**理所当然**“。
但真实的计算机世界并非如此，**正如生活一样**。
（以我浅薄的经验，最快理解可见性并不是通过什么内存语义/线程通信这种学术角度，而是通过计算机的组成原理，比如CSAPP的第一章）

### 线程通信

线程A写一个volatile变量，实质上是线程A向接下来将要读这个volatile变量的某个线程发出了（其对共享变量所做修改的）消息。
线程B读一个volatile变量，实质上是线程B接收了之前某个线程发出的（在写这个volatile变量之前对共享变量所做修改的）消息。
线程A写一个volatile变量，随后线程B读这个volatile变量，这个过程实质上是线程A通过主内存向线程B发送消息。

### 如何实现可见性

```text
通过查IA32架构软件开发者手册可知，Lock前缀的指令在多核处理器下会引发了两件事情。
1）将当前处理器缓存行的数据写回到系统内存。
2）这个写回内存的操作会使在其他CPU里缓存了该内存地址的数据无效。
为了提高处理速度，处理器不直接和内存进行通信，而是先将系统内存的数据读到内部缓存（L1，L2或其他）后再进行操作，但操作完不知道何时会写到内存。
如果对声明了volatile的变量进行写操作，JVM就会向处理器发送一条Lock前缀的指令，将这个变量所在缓存行的数据写回到系统内存。
但是，就算写回到内存，如果其他处理器缓存的值还是旧的，再执行计算操作就会有问题。
所以，在多处理器下，为了保证各个处理器的缓存是一致的，就会实现缓存一致性协议，每个处理器通过嗅探在总线上传播的数据来检查自己缓存的值是不是过期了，
当处理器发现自己缓存行对应的内存地址被修改，就会将当前处理器的缓存行设置成无效状态，当处理器对这个数据进行修改操作的时候，会重新从系统内存中把数据读到处理器缓存里。
...
1）Lock前缀指令会引起处理器缓存回写到内存。Lock前缀指令导致在执行指令期间，声言处理器的LOCK#信号。
在多处理器环境中，LOCK#信号确保在声言该信号期间，处理器可以独占任何共享内存。
但是，在最近的处理器里，LOCK＃信号一般不锁总线，而是锁缓存，毕竟锁总线开销的比较大。
在8.1.4节有详细说明锁定操作对处理器缓存的影响，对于Intel486和Pentium处理器，在锁操作时，总是在总线上声言LOCK#信号。
但在P6和目前的处理器中，如果访问的内存区域已经缓存在处理器内部，则不会声言LOCK#信号。
相反，它会锁定这块内存区域的缓存并回写到内存，并使用缓存一致性机制来确保修改的原子性，此操作被称为“缓存锁定”，缓存一致性机制会阻止同时修改由两个以上处理器缓存的内存区域数据。
2）一个处理器的缓存回写到内存会导致其他处理器的缓存无效。IA32处理器和Intel64处理器使用MESI（修改、独占、共享、无效）控制协议去维护内部缓存和其他处理器缓存的一致性。
在多核处理器系统中进行操作的时候，IA32和Intel64处理器能嗅探其他处理器访问系统内存和它们的内部缓存。处理器使用嗅探技术保证它的内部缓存、系统内存和其他处理器的缓存的数据在总线上保持一致。
例如，在Pentium和P6family处理器中，如果通过嗅探一个处理器来检测其他处理器打算写内存地址，而这个地址当前处于共享状态，那么正在嗅探的处理器将使它的缓存行无效，在下次访问相同内存地址时，强制执行缓存行填充。

方腾飞,魏鹏,程晓明. Java并发编程的艺术 (Java核心技术系列) (Chinese Edition) (pp. 30-31). 机械工业出版社. Kindle Edition. 
```

这里采用《并发编程艺术》的解释，毕竟没有实际研究过这部分害怕误人子弟。

### 防止指令重排序

最基础的八股文，就会告诉我们volatile能防止指令重排序。具体的规则如下：

1. 当第二个操作是volatile写时，不管第一个操作是什么，都不能重排序。
   这个规则确保**volatile写之前的操作不会被编译器重排序到volatile写之后**。
2. 当第一个操作是volatile读时，不管第二个操作是什么，都不能重排序。
   这个规则确保**volatile读之后的操作不会被编译器重排序到volatile读之前**。
3. **当第一个操作是volatile写，第二个操作是volatile读时，不能重排序**。

#### DCL创建单例为什么变量需要volatile

```java
Object=new Object();
```

创建对象的Java代码分解成指令为下面三步

1. 分配对象内存
2. 初始化对象
3. 将对象分配给变量

当然这个顺序是我们**符合直觉**的想法（as-if-serial），实际的执行过程中由于重排序的存在，而步骤2/3之间不存在**数据依赖性**
，因此是可以进行重排序的。而普通的判断对象是初始化完成是通过”变量是否为null“，所以假设先完成3（写入变量），在完成2之前被另一个线程执行判断（读取变量），这期间会导致单例对象
**逃逸**，被不正确的发布，即读取的那个线程拿到了没有完全完成初始化的对象。
那么加上volatile呢？

根据volatile的重排序规则，线程A的步骤1和2不被允许排序到步骤3后（因为步骤3是volatile的写），而线程B的读取变量（volatile读），则会视调度时机。
如果在步骤3后，那么读取到的一定是完全初始化的对象，符合线程安全的构造单例，如果在步骤3前，那么读取到的变量一定为null，也符合线程安全的构造单例。

因此理解volatile的内存语义是非常重要的，有助于我们通过语义去推导为什么。

让我们再来看一个例子

#### AQS中应用volatile设置exclusiveOwnerThread

AQS是毫无疑问的Java并发最佳教材，这里并不详谈核心内容，聊一下“旁枝末节”。

```java
public abstract class AbstractOwnableSynchronizer
        implements java.io.Serializable {
    private transient Thread exclusiveOwnerThread;

    protected final void setExclusiveOwnerThread(Thread thread) {
        exclusiveOwnerThread = thread;
    }
}

// ReentrantLock
abstract static class Sync extends AbstractQueuedSynchronizer {
    @ReservedStackAccess
    protected final boolean tryRelease(int releases) {
        int c = getState() - releases;
        if (getExclusiveOwnerThread() != Thread.currentThread())
            throw new IllegalMonitorStateException();
        boolean free = (c == 0);
        if (free)
            setExclusiveOwnerThread(null);
        setState(c);
        return free;
    }
}

static final class NonfairSync extends Sync {
    protected final boolean tryAcquire(int acquires) {
        if (getState() == 0 && compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(Thread.currentThread());
            return true;
        }
        return false;
    }
}
```

根据上面摘录的这些代码，简单同步一下背景。

1. AQS继承了AbstractOwnableSynchronizer这个类，而这个类里有个属性exclusiveOwnerThread，值为当前持有AQS的线程。
2. state是个volatile变量，在ReentrantLock里值为当前持有锁线程的重入次数
3. tryAcquire()和tryRelease()分别为请求和释放锁方法

可以看到，exclusiveOwnerThread这个属性是没有volatile修饰的，而AQS作为并发框架，很显然是会被并发访问的，那么是如何保证这个值的可见性的呢？
答案让我们从ReentrantLock里阅读。
可以看到，在tryRelease()中，针对state变量的写入是最后进行的（写入exclusiveOwnerThread在次之前），而在tryAcquire()
中，却是首先读取state变量。如果对volatile内存语义不够了解，可能会有点不明白为什么要强调这两个顺序。我们来用volatile语义推导一下
假设在tryRelease先写入volatile变量后写入普通变量（exclusiveOwnerThread），那么会导致，设置exclusiveOwnerThread这个操作被安排在写入state之后，
而可以与读取volatile变量之前的任意操作进行指令重排序，就会存在另一个线程B在线程A执行tryRelease中释放了state后还能看到exclusiveOwnerThread这个值是线程A的情况，
这个很明显不符合tryRelease方法的期望。因此在tryRelease方法中针对普通变量的写入必须要在setState(c)之前，这样才符合释放锁的完整业务语义。
而为什么tryAcquire方法必须先读取volatile变量后设置普通变量也是同理。

```text
公平锁在释放锁的最后写volatile变量state，在获取锁时首先读这个volatile变量。
根据volatile的happens-before规则，释放锁的线程在写volatile变量之前可见的共享变量，在获取锁的线程读取同一个volatile变量后将立即变得对获取锁的线程可见。

方腾飞,魏鹏,程晓明. Java并发编程的艺术 (Java核心技术系列) (Chinese Edition) (pp. 120-121). 机械工业出版社. Kindle Edition. 
```

正是完美灵活的运用volatile，尽可能的节约性能（比如简单粗暴的把exclusiveOwnerThread也声明成volatile），才构建出了AQS这么精妙的框架。这也是我理解为什么volatile是JUC体系的基石。


