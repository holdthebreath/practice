### JAVA内存模型

```markdown
在Java中，所有实例域、静态域和数组元素都存储在堆内存中，堆内存在线程之间共享（本章用“共享变量”这个术语代指实例域，静态域和数组元素）。
局部变量（LocalVariables），方法定义参数（Java语言规范称之为FormalMethodParameters）和异常处理器参数（ExceptionHandlerParameters）
不会在线程之间共享，它们不会有内存可见性问题，也不受内存模型的影响。

方腾飞,魏鹏,程晓明. Java并发编程的艺术 (Java核心技术系列) (Chinese Edition) (p. 55). 机械工业出版社. Kindle Edition. 
```

![](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/202302131206011.png)

#### A-B线程通信

1. A把本地内存中更新过的共享变量刷新到主内存中
2. B到中内存中读取A之前已更新过的共享变量

#### 重排序类型

1. 编辑器重排序-在不改变单线程程序语义的前提下，可以重新安排语句的执行顺序。
2. 指令级重排序-现代处理器采用了指令级并行技术（InstructionLevelParallelism，ILP）来将多条指令重叠执行。如果不存在数据依赖性，处理器可以改变语句对应机器指令的执行顺序。
3. 内存系统重排序-由于处理器使用缓存和读/写缓冲区，这使得加载和存储操作看上去可能是在乱序执行。

1属于编译器重排序，2和3属于处理器重排序。这些重排序可能会导致多线程程序出现内存可见性问题。对于编译器，JMM的编译器重排序规则会禁止特定类型的编译器重排序（不是所有的编译器重排序都要禁止）。对于处理器重排序，JMM的处理器重排序规则会要求Java编译器在生成指令序列时，插入特定类型的内存屏障（MemoryBarriers，Intel称之为MemoryFence）指令，
**通过内存屏障指令来禁止特定类型的处理器重排序**。

#### 现代处理器操作内存

现代的处理器使用写缓冲区临时保存向内存写入的数据。写缓冲区可以保证指令流水线持续运行，它可以避免由于处理器停顿下来等待向内存写入数据而产生的延迟。同时，通过以批处理的方式刷新写缓冲区，以及合并写缓冲区中对同一内存地址的多次写，减少对内存总线的占用。虽然写缓冲区有这么多好处，但每个处理器上的写缓冲区，仅仅对它所在的处理器可见。这个特性会对内存操作的执行顺序产生重要的影响：
**处理器对内存的读/写操作的执行顺序，不一定与内存实际发生的读/写操作顺序一致**
。由于写缓冲区仅对自己的处理器可见，它会导致处理器执行内存操作的顺序可能会与内存实际的操作执行顺序不一致。由于现代的处理器都会使用写缓冲区，因此现代的处理器都会允许对写读操作进行重排序。

#### 内存屏障

JMM把内存屏障指令分为4类

方腾飞,魏鹏,程晓明. Java并发编程的艺术 (Java核心技术系列) (Chinese Edition) (p. 64). 机械工业出版社. Kindle Edition.
![](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/202302131232161.png)

#### happens-before

从JDK5开始，Java使用新的JSR133内存模型，JSR133使用happens-before的概念来阐述操作之间的内存可见性。在JMM中，如果一个操作执行的结果需要对另一个操作可见，那么这两个操作之间必须要存在happensbefore关系。这里提到的
**两个操作既可以是在一个线程之内，也可以是在不同线程之间**。

happens-before规则：

1. 程序顺序规则：一个线程中的每个操作，happens-before于该线程中的任意后续操作。
2. 监视器锁规则：对一个锁的解锁，happens-before于随后对这个锁的加锁。
3. volatile变量规则：对一个volatile域的写，happens-before于任意后续对这个volatile域的读。
4. 传递性：如果A happens-before B，且B happens-before C，那么A happens-before C。

**两个操作之间具有happens-before关系，并不意味着前一个操作必须要在后一个操作之前执行！happens-before仅仅要求前一个操作（执行的结果）对后一个操作可见，且前一个操作按顺序排在第二个操作之前（the
first is visible to and ordered before the second）。**

![](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/202302131239500.png)





