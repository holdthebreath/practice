```text
理解JMM内存模型，并发才算稍有体系堪堪入门
```
阅前必读：
本文核心内容来自SE8的Java Language Specification(https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html#jls-17.4.2)，
包含原文及个人理解，不一定完全正确，如有疑问欢迎讨论及自行探讨。
# 什么是Java Memory Model
JLS 17.4
```markdown
1. A memory model describes, given a program and an execution trace of that program, whether the execution trace is a legal execution of the program. 
2. The Java programming language memory model works by examining each read in an execution trace and checking that the write observed by that read is valid according to certain rules.
3. The memory model describes possible behaviors of a program. An implementation is free to produce any code it likes, as long as all resulting executions of a program produce a result that can be predicted by the memory model.
4. This provides a great deal of freedom for the implementor to perform a myriad of code transformations, including the reordering of actions and removal of unnecessary synchronization.
```
1. 开宗明义，JMM的目标是第3句，约定程序任意实际执行的行为都必须符合JMM的预期的结果。 
   - 个人理解：JMM是一种屏蔽具体细节的底层CPU内存模型/具体实现的抽象，提供给上层java使用者一套统一视图，供其预测程序的执行行为。
   因此相当于是一套衡量并发编程的体系（比如共享变量的可见性）。
2. 设计思想是第4句，JMM在底层具体实现和使用者的简易性中取了平衡，给予底层充分的优化自由（比如JVM优化/硬件优化）
   - 个人理解: 基本可以概括为**只要符合JMM的规则，底层做任何优化都是允许的**（法无禁即自由？），从编程的角度看JMM就是一个抽象接口
3. 工作原理是第2句，检查执行过程中的每次读取以及**根据某些规则**检查该读取观察到的写入的值是否合法。
   - 个人理解：稍微有点拗口，简单理解为，核心校验每次读取值的合法性（比如读一个正确同步的共享变量，但读取到的值不是“最新”写入的，那么则是非法的），
   所以反过来理解，没有正确同步的共享变量，有可见性问题是完全合法的（法无禁即自由？）

```markdown
1. The memory model determines what values can be read at every point in the program. 
2. The actions of each thread in isolation must behave as governed by the semantics of that thread, with the exception that the values seen by each read are determined by the memory model.
When we refer to this, we say that the program obeys intra-thread semantics.
3. Intra-thread semantics are the semantics for single-threaded programs, and allow the complete prediction of the behavior of a thread based on the values seen by read actions within the thread. 
To determine if the actions of thread t in an execution are legal, we simply evaluate the implementation of thread t as it would be performed in a single-threaded context, as defined in the rest of this specification.
4. Each time the evaluation of thread t generates an inter-thread action, it must match the inter-thread action a of t that comes next in program order. 
If a is a read, then further evaluation of t uses the value seen by a as determined by the memory model.
```
1. 第1句，**内存模型确认程序在任意时间点可以读取到哪些值**（内存模型的最准确的使用描述）。
2. 第2句说明如果每个线程隔离的操作由当前线程的语义进行约束（读取的值由JMM控制除外），则程序遵守**线程内语义**。(每个程序当然遵守这一点，否则程序的结果完全无法预测)
3. 第3句说明什么是**线程内语义——就像（以当前线程为）单线程程序进行运行，并且可以通过线程内读取操作所看见的值完全预测线程的行为**(as-if-serial)，
以及如何评估一个线程执行的操作是否合法（通过假设当前线程在单线程上下文中执行操作）。
4. 第4句，每个线程内部生成的**线程间操作**必须(must)符合(match)程序顺序(program order)，如果该操作是一个读取动作，则这个操作具体看到的值由JMM模型确定。

这段话言简意赅，是JMM整体的要义核心。明确了JMM的抽象逻辑结构（堆内存共享），职责范围（只影响堆内存内的变量），触发时机（当出现线程间的读取操作时），以及**单线程的线程间操作具有程序顺序性**。

# 共享变量
JLS 17.4.1
```markdown
1. Memory that can be shared between threads is called shared memory or heap memory.
2. All instance fields, static fields, and array elements are stored in heap memory. In this chapter, we use the term variable to refer to both fields and array elements.
3. Local variables (§14.4), formal method parameters (§8.4.1), and exception handler parameters (§14.20) are never shared between threads and are unaffected by the memory model.
4. Two accesses to (reads of or writes to) the same variable are said to be conflicting if at least one of the accesses is a write.
```
1. 第1句描述了堆内存的定义——跨线程共享的内存。
2. 第2句描述了哪些变量在堆内存中存储（实例属性，静态属性，数组元素）。
3. 第3句描述了哪些变量不会跨线程共享因此不受JMM影响（本地变量，方法形参，异常处理程序参数）。
4. 第4句定义什么叫（操作）冲突（针对同一个共享变量的两次访问，且其中至少有一次是写操作）。

# 线程间操作(inter-thread actions)
JLS 17.4.2
```markdown
An inter-thread action is an action performed by one thread that can be detected or directly influenced by another thread
```
定义：如果一个线程执行的操作可以**被其他线程监测到(be detected)或者被(其他线程)直接影响(directly influenced by)**，那么这个操作叫线程间操作。注意与线程内操作(Intra-thread)英文的区分！
```markdown
There are several kinds of inter-thread action that a program may perform:
1. Read (normal, or non-volatile). Reading a variable.
2. Write (normal, or non-volatile). Writing a variable.
3. Synchronization actions, which are:
   - Volatile read. A volatile read of a variable.
   - Volatile write. A volatile write of a variable. 
   - Lock. Locking a monitor
   - Unlock. Unlocking a monitor.
   - The (synthetic) first and last action of a thread.
   - Actions that start a thread or detect that a thread has terminated (§17.4.4).
4. External Actions. An external action is an action that may be observable outside of an execution, and has a result based on an environment external to the execution.
5. Thread divergence actions (§17.4.9). A thread divergence action is only performed by a thread that is in an infinite loop in which no memory, synchronization, or external actions are performed. If a thread performs a thread divergence action, it will be followed by an infinite number of thread divergence actions.
   - Thread divergence actions are introduced to model how a thread may cause all other threads to stall and fail to make progress.
```
这里列举了程序具有的多种线程间操作，值得注意的是第4和第5种，外部调用和线程发散操作。这两个比较难理解，幸好有前辈早就问过这个问题(https://stackoverflow.com/questions/26788260/examples-for-thread-divergence-actions-and-external-actions-in-the-java-memory-m)。
摘抄一下里面的代码
```java
class ThreadDivergence {
  int foo = 0;
  void thread1() { 
    while (true); // thread-divergence action
    foo = 42; 
  } 

  void thread2() { 
    assert foo == 0; 
  } 
}

class Externalization {
   int foo = 0;
   void method() {
      jni(); // external action
      foo = 42;
   }

   native void jni(); /* { 
    assert foo == 0; 
  } */
}
```
核心概念就是**外部操作和线程发散操作也是不被允许重排序**,否则如上例程序会出现不符合预期的结果(断言不是永远为true)。

```markdown
1. This specification is only concerned with inter-thread actions. 
2. We do not need to concern ourselves with intra-thread actions (e.g., adding two local variables and storing the result in a third local variable). As previously mentioned, all threads need to obey the correct intra-thread semantics for Java programs. 
We will usually refer to inter-thread actions more succinctly as simply actions.
......
```
这段强调上面说的全部操作的规范只适用于线程间操作，而**任何Java程序都遵守正确的线程内操作语义**。

# 程序和程序顺序
JLS 17.4.3
```markdown
1. Among all the inter-thread actions performed by each thread t, the program order of t is a total order that reflects the order in which these actions would be performed according to the intra-thread semantics of t.
2. A set of actions is sequentially consistent if all actions occur in a total order (the execution order) that is consistent with program order, and furthermore, each read r of a variable v sees the value written by the write w to v such that:
    - w comes before r in the execution order, and
    - there is no other write w' such that w comes before w' and w' comes before r in the execution order.
3. Sequential consistency is a very strong guarantee that is made about visibility and ordering in an execution of a program. Within a sequentially consistent execution, there is a total order over all individual actions (such as reads and writes) which is consistent with the order of the program, and each individual action is atomic and is immediately visible to every thread.
4. **If a program has no data races, then all executions of the program will appear to be sequentially consistent.**
5. Sequential consistency and/or freedom from data races still allows errors arising from groups of operations that need to be perceived atomically and are not.
```
这段话是寥寥几句，但为精髓中的精髓。为什么这么说呢，因为在著名并发相关的《Java并发编程的艺术》一书中涉及到了这块知识，但直到我看到JLS，我才发现以前自己看那本书得到相关知识的理解是错误的。
让我们来逐字逐句推敲一下这段话。
### 程序顺序 
第一句就显得有一点云遮雾障，核心其实就是在说什么是程序顺序(program order)——在每个线程执行的所有**线程间操作**中，程序顺序是反映根据**线程内语义**执行这些操作的顺序的总顺序。
当然我相信这样翻译还是有点抽象，更具体一点来说。首先基于上文我们已知
1. 所有线程操作分为两类，分别是线程间和线程内操作。
2. 线程内操作满足线程内语义
由此可以看出，而线程间操作是在之前是没有规范的，而没有规范就意味无法预测结果，所以需要增加这一规则进行约束。
总结一下，**程序顺序是线程的线程间操作实际执行的顺序遵循线程内语意(在单线程程序下线程的行为可以通过读取操作看到的值完全预测)的总排序**

### 顺序一致性模型
第2段这里更是重量级！更是言简意赅凝练到了极致。首先翻译一下，顺序一致是


```markdown
If a program has no data races, then all executions of the program will appear to be sequentially consistent.
```

