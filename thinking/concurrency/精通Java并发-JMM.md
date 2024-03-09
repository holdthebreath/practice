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

### 顺序一致性(模型)
第2段这里更是重量级！更是言简意赅凝练到了极致。
首先翻译一下，**如果所有（线程的线程间）操作发生的总顺序（执行顺序）与程序顺序一致，则这组操作具有顺序一致性**，后面的这段我理解是讲解顺序一致性模型的特点的。
所以理解这段话我们得首先弄明白什么是顺序一致性模型。
#### 顺序一致性模型
来自维基百科 https://en.wikipedia.org/wiki/Sequential_consistency
```markdown
1. Sequential consistency is a consistency model used in the domain of concurrent computing (e.g. in distributed shared memory, distributed transactions, etc.).
2. **the execution order of a program in the same processor (or thread) is the same as the program order, while the execution order of a program on different processors (or threads) is undefined.**
细品第2句。
```
顺序一致性是一个一致性模型，应用在并发计算领域。提出这个理论的是Leslie Lamport巨佬，在分布式时空领域的伟大名字(Lamport timestamps)，包括接下来的happens-before也是他提出的。
```markdown
1. A high-speed **processor may execute operations in a different order than is specified by the program.** 
   The correctness of the execution is guaranteed if the processor satisfies the following condition: 
   **the result of an execution is the same as if the operations had been executed in the order specified by the program.**
   A processor satisfying this condition will be called sequential.
2. **the result of any execution is the same as if the operations of all the processors were executed in some sequential order, and the operations of each individual processor appear in this sequence in the order specified by its program.**
   A multiprocessor satisfying this condition will be called sequentially consistent. 
   **The sequentiality of each individual processor does not guarantee that the multi-processor computer is sequentially consistent.**

How to Make a Multiprocessor Computer That Correctly Executes Multiprocess Programs
```
1. 处理器实际执行操作的顺序**有可能**与程序定义的顺序并不相同
2. 处理器如果执行结果与程序定义的顺序执行生成的结果相同，那么则称这个处理器具有顺序性(sequential)。
3. 多核处理器如果任何(实际)执行(顺序)的结果与所有处理器的操作以顺序性(sequential)方式执行相同，并且每个单独处理器的操作以其程序指定的顺序出现，则称这个多核处理器是顺序一致的。
4. 每个单核具有顺序性并不能保证多核具有顺序一致性
第4点是这篇论文讨论的核心，不过与我们理解顺序一执行模型并不相关，就不展开了。核心点是多核实现顺序一致性需要满足两个条件：
1. 每个处理器**发送**(issue)(这里的发送并不是执行的概念，注意含义区别，其实暗含了执行原子性实现写请求对后续操作可见)内存请求的顺序与程序定义的顺序相同
2. 所有处理器向单个内存模块**发送**的内存请求进入单个FIFO队列(已内存模块为维度的队列)进行顺序执行。**发送**内存请求的顺序与进入队列的顺序一致。

这个是巨佬的论文定义，我们通过下面的图快速理解这个模型：
```markdown
 Thread 1 |   Thread 2
    A1    |     B1 
    A2    |     B2
    A3    |     B3
假设有两个线程T1和T2，他们的操作分别为A和B，依赖关系为3->2->1(这里是必须的，否则不能体现线程内语义)
顺序一致性模型的图：
------    ------
| T1 |    | T2 |
------    ------
  |         |
        |  
  -------------
  |heap memory|
  -------------
```
大致的结构类似这样，最终通过内存开关连接不同的线程使两个线程的操作具有全局的一个顺序性，也因此具有每个写操作的结果对后续操作读可见特性。
还不够清晰的话，可以想象现在我们的左右手（每个线程）各有一堆牌（操作），然后你将他们交错互相插入最终形成一堆牌（总顺序）。
举几个例子
```markdown
A1 -> B1 -> A2 -> B2 -> A3 -> B3
A1 -> B1 -> B2 -> A2 -> B3 -> A3
B1 -> B2 -> B3 -> A1 -> A2 -> A3
```

上面三种可能的执行顺序都是符合顺序一致性的。
总结，**只要每个线程的操作的执行顺序，满足该线程程序顺序，即执行顺序符合1->2->3，两个线程任意实际总顺序都是符合顺序一致性的。**

##### 三种顺序概念整理
1. execution order：执行顺序，指程序实际执行操作的顺序。
2. order of program：程序的顺序，我们写代码时的先后顺序，体现了我们期望线程实现什么样的行为。
3. program order：程序顺序，符合单线程语义的顺序(单线程上下文下线程的行为可以完全预测)。反过来说，**只要线程的行为是符合单线程语义，那么任何执行顺序都是合法的(重排序)**。可以简单理解为，**program order是一种约束规则，使线程在效果上表现为按照order of program进行执行**。所以从结果角度可以简单认为program order就是order of program，但要清楚两者具体含义并不相同。

当理解了这一点，我们就可以一目了然的发现核心关键——**实际执行的具体线程的调度其实没有任何约束**。
从这里，我们隐隐约约感觉到，好像已经摸索到实际会出现并发相关问题的本质原因了——**对线程进行任意调度执行都是合法的**。
```markdown
1. Sequential consistency is a very strong guarantee that is made about visibility and ordering in an execution of a program. Within a sequentially consistent execution, there is a total order over all individual actions (such as reads and writes) which is consistent with the order of the program, and each individual action is atomic and is immediately visible to every thread.
2. If a program has no data races, then all executions of the program will appear to be sequentially consistent.
3. Sequential consistency and/or freedom from data races still allows errors arising from groups of operations that need to be perceived atomically and are not.
4. If we were to use sequential consistency as our memory model, many of the compiler and processor optimizations that we have discussed would be illegal.
```
1. 总结顺序一致性的特点——顺序一致性(模型)是一个非常强的可见性和程序的执行顺序的保证。在顺序一致性模型内执行，每个独立的操作都具有原子性而且总执行顺序符合**程序顺序**，以及每个独立操作具有原子性并对所有线程立即可见。
2. 总结顺序一致性和Java程序的关系——如果程序没有**数据竞争**，程序的全部操作执行（的效果）**表现的像顺序一致性**。
3. 顺序一致性模型的职责范围——无论是否存在数据竞争，都仍然**允许由一系列操作需要原子性（而实际没有）引发错误**。
这里根据上面的例子可以很好理解，意思是假设线程的结果必须要求依赖(1->2->3)三个操作为原子执行(中间不能穿插另一个线程的操作)，那么顺序一致性是允许这种情况出现错误。（因为顺序一致性模型并不具有多个操作原子性的保证）。从本质上来看，这种情况下**线程结果的正确性依赖了特定的线程调度顺序（竞态条件），而正如上文所说，对线程的调度不会存在任何约束。**
4. 补充说明了**JMM并没有采用顺序一致性模型**的原因——许多编译器和处理器的优化将会被认为是非法的。

# 同步顺序
JLS 17.4.4
```markdown
Every execution has a synchronization order. 
A synchronization order is a total order over all of the synchronization actions of an execution. 
For each thread t, the synchronization order of the synchronization actions (§17.4.2) in t is consistent with the program order (§17.4.3) of t.
```
这段说明了什么是同步顺序——同步顺序是执行全部**同步操作**的总排序。不过最重要的是最后一句，针对每个线程，**线程的同步顺序与程序顺序一致**。
```markdown
- Synchronization actions induce the synchronized-with relation on actions, defined as follows:
1. An unlock action on monitor m synchronizes-with all subsequent lock actions on m (where "subsequent" is defined according to the synchronization order).
2. A write to a volatile variable v (§8.3.1.4) synchronizes-with all subsequent reads of v by any thread (where "subsequent" is defined according to the synchronization order).
3. An action that starts a thread synchronizes-with the first action in the thread it starts.
4. The write of the default value (zero, false, or null) to each variable synchronizes-with the first action in every thread.
Although it may seem a little strange to write a default value to a variable before the object containing the variable is allocated, conceptually every object is created at the start of the program with its default initialized values.
5. The final action in a thread T1 synchronizes-with any action in another thread T2 that detects that T1 has terminated.
6. T2 may accomplish this by calling T1.isAlive() or T1.join().
If thread T1 interrupts thread T2, the interrupt by T1 synchronizes-with any point where any other thread (including T2) determines that T2 has been interrupted (by having an InterruptedException thrown or by invoking Thread.interrupted or Thread.isInterrupted).
```
同步操作包含了同步-与关系(synchronized-with)
1. monitor解锁(unlock) synchronized-with 后续同个锁的锁定(lock)
2. 写volatile变量 synchronized-with 后续任意线程读同一个变量
3. 启动一个线程的操作 synchronized-with 被启动线程的第一个操作
4. 写变量默认值的操作 synchronized-with 线程针对该变量的第一个操作
5. 线程T1最后一个操作 synchronized-with 线程T2中检测到T1已终止的任何操作
6. 如果线程T1中断了线程T2，则T1的中断操作 synchronized-with 任意时刻其他线程(包括T2)检测到T2已经被中断(通过抛出InterruptedException或调用Thread.interrupted或Thread.isInterrupted)
```markdown
The source of a synchronizes-with edge is called a release, and the destination is called an acquire.
```
同步关系发起的边界称为释放(release)，接收的目标称为获取(acquire)。
这段可能看起来有点拗口，本质上定义了Java程序中全部的同步操作。仔细想想，这也是我们实际在开发过程中一直有的思维定势，无非更书面化的定义，为下文的happens-before打好理论基础。

# happens-before order
17.4.5
相信happens-before规则大家都是了然于胸，很多八股文列了十几条具体的规则，这个纯靠背的话感觉就有点难度了。难道真的要一条一条去硬记吗？万幸jls告诉我们并不需要。
```markdown
**Two actions can be ordered by a happens-before relationship. If one action happens-before another, then the first is visible to and ordered before the second.**
If we have two actions x and y, we write hb(x, y) to indicate that x happens-before y.
1. If x and y are actions of the same thread and x comes before y in program order, then hb(x, y).
2. There is a happens-before edge from the end of a constructor of an object to the start of a finalizer (§12.6) for that object.
3. If an action x synchronizes-with a following action y, then we also have hb(x, y).
4. If hb(x, y) and hb(y, z), then hb(x, z).
```
如果已经对happens-before有深刻理解，只是想了解规范如何定义整套规则的，那么只需要看这段就足够了。
- 两个操作可以通过happens-before排序。如果一个操作happens-before另一个，则第一个操作对第二个操作是可见的，并且在第二个操作之前发生。
接下来列的是4条规则，没错，实际的happens-before只需要4条，前提是我们明白什么是synchronizes-with关系。
总结而言，**单线程的操作happens-before基于程序顺序，多线程的happens-before基于synchronizes-with，happens-before具有传递性**。
我想经过上面的“训练”，已经形成条件反射了——法无禁即自由。对没错，任意两个操作如果没有happens-before关系，则发生顺序与可见性完全没有约束，一切都是合法的。
```markdown
1. The wait methods of class Object (§17.2.1) have lock and unlock actions associated with them; their happens-before relationships are defined by these associated actions.
2. It should be noted that the presence of a happens-before relationship between two actions does not necessarily imply that they have to take place in that order in an implementation. If the reordering produces results consistent with a legal execution, it is not illegal.
For example, the write of a default value to every field of an object constructed by a thread need not happen before the beginning of that thread, as long as no read ever observes that fact.
3. More specifically, if two actions share a happens-before relationship, they do not necessarily have to appear to have happened in that order to any code with which they do not share a happens-before relationship.
   Writes in one thread that are in a data race with reads in another thread may, for example, appear to occur out of order to those reads.
```
1. 在Object类中wait方法关联了lock/unlock操作，它们的happens-before关系由关联这两个操作定义。(为什么wait方法在Object类中？我相信这里是这道八股文的起源^_^)
2. 需要注意的是**两个操作具有happens-before关系并不意味着它们必须在实际中按这个顺序执行**。如果重排序的结果与合法执行一致，则(该顺序)不非法。
3. 更具体的说，**如果两个操作共享(share)happens-before关系，对于不共享happens-before关系的其他代码，它们并不需要必须(do not necessarily have to)表现按照这个顺序发生**。这句有点拗口对不对，举个实际的栗子，文中说的线程写变量的冲突。
比如线程1有2个操作，1. 写共享变量a, 2. 读取共享变量a，则根据单线程原则，1和2共享happens-before关系(1必须在2之前发生)。但对于另一个线程2假设只有一个操作读取a，在没有同步关系(synchronized-with)时，则无论是操作1还是2都没有happens-before关系，因此它观察到线程1的实际执行效果可能为先执行2，后执行1。(可见性问题，详细点说，有可能的情况为操作1先写在了线程1私有内存，没有刷新到主存，然后执行操作2。在这两个操作之间。线程2读取了变量，这时线程2观察到的结果与线程1先执行操作2后操作1的结果一致)
```markdown
1. The happens-before relation defines when data races take place.
2. A set of synchronization edges, S, is **sufficient** if it is the minimal set such that the **transitive closure** of S with the program order determines all of the happens-before edges in the execution. This set is unique.
It follows from the above definitions that:
- An unlock on a monitor happens-before every subsequent lock on that monitor.
- A write to a volatile field (§8.3.1.4) happens-before every subsequent read of that field.
- A call to start() on a thread happens-before any actions in the started thread.
- All actions in a thread happen-before any other thread successfully returns from a join() on that thread.
- The default initialization of any object happens-before any other actions (other than default-writes) of a program.
3. When a program contains two conflicting accesses (§17.4.1) that are not ordered by a happens-before relationship, it is said to contain a data race.
4. The semantics of operations other than inter-thread actions, such as reads of array lengths (§10.7), executions of checked casts (§5.5, §15.16), and invocations of virtual methods (§15.12), are not directly affected by data races.
   Therefore, a data race cannot cause incorrect behavior such as returning the wrong length for an array.

```
1. happens-before关系的目的在这句，**happens-before定义何时存在数据冲突**。
2. 这段有两个术语，充分的(sufficient)/传递闭包(transitive closure)，读起来不直观，然后我就问了GPT。(当然应该是自己不懂图论)
```markdown
传递闭包（Transitive Closure）在这个上下文中，是指从一组同步边（Synchronization Edges）出发，通过递归地应用传递性（如果A happens-before B，且B happens-before C，则A happens-before C），来获得所有可能的 happens-before 关系的集合。这个概念来自图论，其中**传递闭包用于找到图中所有可达的顶点对。**
具体到并发编程和内存模型的上下文，传递闭包帮助我们确定在程序中可能的所有 happens-before 关系。在这种情况下，同步边集合 S 通过程序的执行顺序（Program Order）和已定义的 happens-before 规则，定义了一组原始的 happens-before 关系。
如果你能够通过这组同步边 S 和程序的执行顺序，推导出执行中所有可能的 happens-before 关系（即，没有额外的 happens-before 关系可以被推导出，但未被 S 捕捉），那么我们说这组同步边 S 是充分的（Sufficient）。
简单来说，在这个上下文中，传递闭包就是通过不断应用 happens-before 的定义，找到所有间接的 happens-before 关系，确保能覆盖程序执行中所有可能的 happens-before 关系。这是确保程序正确同步的一个关键步骤，因为它涵盖了所有必须被同步的操作，以避免数据竞争和其他并发问题。
```
3. 说明了数据冲突的定义——当程序存在两个**冲突访问**且没有通过happens-before关系排序，则称为程序存在数据冲突。
4. 解释了数据冲突语义的适用范围——除了线程间操作外的操作的语义，如数组长度的读取、检查型转换的执行和虚方法的调用，并不直接受数据竞争的影响。(**仅适用于线程间操作**)
```markdown
1. A program is correctly synchronized if and only if all sequentially consistent executions are free of data races.
2. If a program is correctly synchronized, then all executions of the program will appear to be sequentially consistent (§17.4.3).
This is an extremely strong guarantee for programmers. Programmers do not need to reason about reorderings to determine that their code contains data races. Therefore they do not need to reason about reorderings when determining whether their code is correctly synchronized. Once the determination that the code is correctly synchronized is made, the programmer does not need to worry that reorderings will affect his or her code.
A program must be correctly synchronized to avoid the kinds of counterintuitive behaviors that can be observed when code is reordered. The use of correct synchronization does not ensure that the overall behavior of a program is correct. However, its use does allow a programmer to reason about the possible behaviors of a program in a simple way; the behavior of a correctly synchronized program is much less dependent on possible reorderings. Without correct synchronization, very strange, confusing and counterintuitive behaviors are possible.
```
这段阐述了程序**正确同步**(correctly synchronized)的定义：
1. 当且仅当程序所有顺序一致性执行(顺序的集合)不存在数据竞争，程序是正确同步的。
2. 如果程序是正确同步的，那么所有的执行(顺序的集合)表现为顺序一致性。
下面补充说明了这两条规则，简单概括为：
**只要正确同步的程序就可以忽略重排序，判断是否正确同步就看程序的实际执行顺序能否表现为顺序一致性，但正确同步的程序并不意味着程序的全部行为是正确的(我理解这里的正确指的是符合我们的期望，比如volatile++是正确同步的，但可能不符合我们期望的正确)，使用正确同步允许我们用简单的判断程序的行为，因为正确同步的程序更少的依赖重排序。**
在层层铺垫后在这里我们终于得到了如何判断程序是否正确同步的方法，以及如何定义正确同步与程序行为正确的关系。

   