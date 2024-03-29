```text
理解JMM内存模型，并发才算稍有体系堪堪入门
```
阅前必读：
本文核心内容来自SE8的Java Language Specification(https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html#jls-17.4.2)，
包含原文及个人理解，不一定完全正确，如有疑问欢迎讨论及自行探讨。

# Memory (consistency) Model
作为一切的开头，如果是和我一样在此之前对内存模型没有概念的，强烈推荐阅读下面这篇Tutorial，当作下面的知识背景。
https://www.cs.utexas.edu/~bornholt/post/memory-models.html (~~真学CS还得看老外~~)

```markdown
A memory consistency model is a **contract** between the hardware and software. The hardware promises to only reorder operations in ways allowed by the model, and in return, the software acknowledges that all such reorderings are possible and that it needs to account for them.
```
contract，如此简洁准确而又优雅的描述，看到这句描述有种振聋发聩的美感。
1. 硬件承诺只重排序内存模型允许的方式进行重排序
2. 软件承认所有此类重排序都是可能的，并且需要考虑他们

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
第一句就显得有一点云遮雾障，核心其实就是在说什么是程序顺序(program order)——在每个线程执行的所有**线程间操作**中，程序顺序是反映根据**t的线程内语义将被执行**(would be performed)这些操作的顺序的**全序**。
当然我相信这样翻译还是有点抽象，更具体一点来说。首先基于上文我们已知
1. 所有线程操作分为两类，分别是线程间和线程内操作。
2. 线程内操作满足线程内语义
由此可以看出，而线程间操作是在之前是没有规范的，而没有规范就意味无法预测结果，所以需要增加这一规则进行约束。
总结一下，**程序顺序是线程遵循线程内语意执行线程间操作的全序**

### 顺序一致性(模型)
第2段这里更是重量级！更是言简意赅凝练到了极致。
首先翻译一下，**如果所有（线程的线程间）操作执行的全序（执行顺序）与（各个线程的）程序顺序一致，则这组操作是顺序一致的**。
后面的这段我理解是讲解顺序一致性模型的特点的，所以理解这段话我们得首先弄明白什么是顺序一致性模型，当然对于菜鸟的我，还需要再首先一下，什么是内存模型？(memory model)

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
2. **the result of an execution is the same as if the operations had been executed in the order specified by the program.**
   A processor satisfying this condition will be called sequential.
3. **the result of any execution is the same as if the operations of all the processors were executed in some sequential order, and the operations of each individual processor appear in this sequence in the order specified by its program.**
   A multiprocessor satisfying this condition will be called sequentially consistent. 
4. **The sequentiality of each individual processor does not guarantee that the multi-processor computer is sequentially consistent.**

How to Make a Multiprocessor Computer That Correctly Executes Multiprocess Programs
```
1. 处理器实际执行操作的顺序**有可能**与程序定义的顺序并不相同(~~有可能 == 基本上(不是)~~)
2. 处理器如果执行结果与程序定义的顺序执行生成的结果相同，那么则称这个处理器具有顺序性(sequential)。
3. 多核处理器如果任何(实际)执行(顺序)的结果与所有处理器的操作以顺序性(sequential)方式执行相同，并且每个单独处理器的操作以其程序指定的顺序出现，则称这个多核处理器是顺序一致的。
4. 每个单核具有顺序性并不能保证多核具有顺序一致性
第4点是这篇论文讨论的核心，不过与我们理解顺序一执行模型并不相关，就不展开了。核心点是多核实现顺序一致性需要满足两个条件：
- 每个处理器**发送**(issue)(这里的发送并不是执行的概念，注意含义区别，其实暗含了执行原子性实现写请求对后续操作可见)内存请求的顺序与**程序定义的顺序**相同
- 所有处理器向单个内存模块**发送**的内存请求进入单个FIFO队列(已内存模块为维度的队列)进行顺序执行。**发送**内存请求的顺序与进入队列的顺序一致。

这个是巨佬的论文定义，我们通过下面的图快速理解这个模型：
```markdown
 Thread 1 |   Thread 2
    A1    |     B1 
    A2    |     B2
    A3    |     B3
假设有两个线程T1和T2，他们的操作分别为A和B
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
大致的结构类似这样，最终**通过内存开关连接不同的线程使两个线程的操作具有全局的一个顺序性(全序)，也因此具有单个操作的原子性以及每个写操作的结果对后续操作读可见特性。**
还不够清晰的话，可以**想象现在我们的左右手(每个线程)各有一堆牌(一系列操作)，然后将他们交错互相插入最终形成一堆牌(程序执行两个线程全部操作的全序)。**
举几个例子
```markdown
A1 -> B1 -> A2 -> B2 -> A3 -> B3
A1 -> B1 -> B2 -> A2 -> B3 -> A3
B1 -> B2 -> B3 -> A1 -> A2 -> A3
```

上面三种可能的执行顺序都是符合顺序一致性的。
总结，**只要每个线程的操作的执行顺序，满足该线程的程序顺序(即执行顺序中该线程的偏序符合1->2->3)且执行效果为原子性并对其他线程立即可见，两个线程任意实际执行顺序都是符合顺序一致性的。**

##### 程序顺序与执行顺序 (~~F**k八股文~~)
1. execution order：(程序的)执行顺序，指**程序实际执行操作的顺序，是程序全部操作的全序**。
2. program order：(线程的)程序顺序，指**线程符合单线程语义的将被执行的顺序**。非正式的，**程序顺序就是我们写代码的顺序**。

这里原本是3个顺序，但在论文以及上面的开篇的Tutorial的交叉印证下，我思考了很久才算理清楚程序顺序的含义。
由于各类优化的存在(重排序/乱序执行等)，实际上execution order与program order并不相同，而现代cpu都是**顺序的**，即只保证单线程下重排序后的执行行为与按照程序顺序执行一致。
当理解了这一点，我们就可以一目了然的发现核心关键——**实际执行时具体线程的调度其实没有任何约束，对线程进行任意调度执行都是合法的**。

```markdown
1. Sequential consistency is a very strong guarantee that is made about visibility and ordering in an execution of a program. Within a sequentially consistent execution, there is a total order over all individual actions (such as reads and writes) which is consistent with the order of the program, and each individual action is atomic and is immediately visible to every thread.
2. If a program has no data races, then all executions of the program will appear to be sequentially consistent.
3. Sequential consistency and/or freedom from data races still allows errors arising from groups of operations that need to be perceived atomically and are not.
4. If we were to use sequential consistency as our memory model, many of the compiler and processor optimizations that we have discussed would be illegal.
```
1. 总结顺序一致性的特点——顺序一致性(模型)是一个非常强的可见性和程序的执行顺序的保证。**以顺序一致性模型执行(sequentially consistent execution)，存在一个全部独立操作的全序并与程序顺序一致，每个独立操作具有原子性并对所有线程立即可见。**
2. 总结顺序一致性和Java程序的关系——**如果程序没有数据竞争，程序的全部执行表现为顺序一致性**。
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
这段说明了什么是同步顺序——同步顺序是程序(某次实际)执行的全部**同步操作**的全序。不过最重要的是最后一句，针对每个线程，**线程的同步顺序与程序顺序一致**。
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
总结而言，**单线程操作的happens-before基于程序顺序，多线程操作的happens-before基于synchronizes-with，happens-before具有传递性**。
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
1. happens-before关系的目的在这句，**happens-before定义何时存在数据竞争**。
2. 这段有两个术语，充分的(sufficient)/传递闭包(transitive closure)，读起来不直观，然后我就问了GPT。(当然应该是自己不懂图论)
```markdown
传递闭包（Transitive Closure）在这个上下文中，是指从一组同步边（Synchronization Edges）出发，通过递归地应用传递性（如果A happens-before B，且B happens-before C，则A happens-before C），来获得所有可能的 happens-before 关系的集合。这个概念来自图论，其中**传递闭包用于找到图中所有可达的顶点对。**
具体到并发编程和内存模型的上下文，传递闭包帮助我们确定在程序中可能的所有 happens-before 关系。在这种情况下，同步边集合 S 通过程序的执行顺序（Program Order）和已定义的 happens-before 规则，定义了一组原始的 happens-before 关系。
如果你能够通过这组同步边 S 和程序的执行顺序，推导出执行中所有可能的 happens-before 关系（即，没有额外的 happens-before 关系可以被推导出，但未被 S 捕捉），那么我们说这组同步边 S 是充分的（Sufficient）。
简单来说，在这个上下文中，传递闭包就是通过不断应用 happens-before 的定义，找到所有间接的 happens-before 关系，确保能覆盖程序执行中所有可能的 happens-before 关系。这是确保程序正确同步的一个关键步骤，因为它涵盖了所有必须被同步的操作，以避免数据竞争和其他并发问题。
```
3. 说明了数据竞争的定义——**当程序存在两个冲突访问且没有通过happens-before关系排序，则称为程序存在数据竞争**。
4. 解释了数据竞争语义的适用范围——除了线程间操作外的操作的语义，如数组长度的读取、检查型转换的执行和虚方法的调用，并不直接受数据竞争的影响。(**仅适用于线程间操作**)
```markdown
1. A program is correctly synchronized if and only if all sequentially consistent executions are free of data races.
2. If a program is correctly synchronized, then all executions of the program will appear to be sequentially consistent (§17.4.3).
This is an extremely strong guarantee for programmers. Programmers do not need to reason about reorderings to determine that their code contains data races. Therefore they do not need to reason about reorderings when determining whether their code is correctly synchronized. Once the determination that the code is correctly synchronized is made, the programmer does not need to worry that reorderings will affect his or her code.
A program must be correctly synchronized to avoid the kinds of counterintuitive behaviors that can be observed when code is reordered. The use of correct synchronization does not ensure that the overall behavior of a program is correct. However, its use does allow a programmer to reason about the possible behaviors of a program in a simple way; the behavior of a correctly synchronized program is much less dependent on possible reorderings. Without correct synchronization, very strange, confusing and counterintuitive behaviors are possible.
```
这段阐述了程序**正确同步**(correctly synchronized)的定义：
1. **当且仅当程序所有(以)顺序一致性(方式)执行不存在数据竞争，程序是正确同步的。**
2. **如果程序是正确同步的，那么程序的所有执行表现为顺序一致性。**
在层层铺垫后在这里我们终于得到了如何判断程序是否正确同步的方法，以及如何定义正确同步与程序行为正确的关系——**正确同步的程序的行为不一定符合我们的期望(原子性问题)，但没有正确同步的程序的行为一定会使我们感到困惑和反直觉。**
```markdown
1. We say that a read r of a variable v is allowed to observe a write w to v if, in the happens-before partial order of the execution trace:
- r is not ordered before w (i.e., it is not the case that hb(r, w)), and
- there is no intervening write w' to v (i.e. no write w' to v such that hb(w, w') and hb(w', r)).
Informally, a read r is allowed to see the result of a write w if there is no happens-before ordering to prevent that read.
2. A set of actions A is happens-before consistent if for all reads r in A, where W(r) is the write action seen by r, it is not the case that either hb(r, W(r)) or that there exists a write w in A such that w.v = r.v and hb(W(r), w) and hb(w, r).
In a happens-before consistent set of actions, each read sees a write that it is allowed to see by the happens-before ordering.
```
## happens-before consistency
1. 这段是happens-before的数学化定义，讲解了在某个读请求在什么情况下被允许(is allowed)看到某个写请求(的结果)。
   -  按照执行追踪happens-before偏序中
      1. 读请求没有被排在这个写请求前面(即不存在hb(r,w))
      2. 这两者之中没有插入另一个写请求w'(即不存在hb(w,w')和hb(w', r))
      稍微有点难理解的是这句"Informally, a read r is allowed to see the result of a write w if there is no happens-before ordering to prevent that read."，但我认为也是最快速掌握happens-before实际应用的窍门。
      这句说的是，如果一个读请求和一个写请求在没有happens-before关系**阻止**的情况下，这个读请求是**被允许**看到这个写请求的结果的。
      比如上面这个例子，如果这三个操作互相存在happens-before关系，即w -> w' -> r(->是hb关系的符号)，那么很明显r只能看到w'的结果而看不到w的结果(这就意味着happens-before**阻止**了r看到w的结果(的情况))。但在没有相应的hb关系的情况下，那么r看到w的结果是合法的。
      所以这也给了我们启发，**在判断某个读取可能看到的情况时，从存在哪些happens-before关系阻止(看到哪些写请求)的角度进行判断**。因为在很多时候，我们其实并不需要确定某个读请求(在不同执行顺序下)可以看到哪些合法的值，而是期望通过判断(在实际执行中)看到某个值是否合法，进而确认程序的行为是否符合我们的预期(程序正确)。
2. 第二段是定义什么是happens-before consistent操作集合，其实就是上面两条的数学方式定义全部情况的集合。
   - 操作集合A中如果全部的读r，看到的写W(r)
   - **既不存在**hb(r, W(r))(没有违反hb关系看到未来的写)**也不存在**针对同一个共享变量v的另一个写请求w有hb(W(r), w)和hb(w, r)(没有违反hb关系看到过时的写)
   - 则这个操作集合是happens-before consistent的
**在happens-before consistent操作集合中，每个读请求都看到它根据happens-before排序被允许看到的写入。**
需要注意**操作集合判断是否符合happens-before consistent是必须基于有hb关系的**。
当然jls这里也有个很好的例子，告诉我们什么是happens-before consistent
```markdown
 initially A == B == 0 
 Thread 1         |   Thread 2 
    B = 1;        |     A = 2;
    r2 = A;	  |     r1 = B;
```
```markdown
Since there is no synchronization, **each read can see either the write of the initial value or the write by the other thread**
```
1. B = 1;
2. A = 2;
3. r2 = A;  // sees initial write of 0
4. r1 = B;  // sees initial write of 0
这个情况下符合happens-before consistent，这是很好理解的。


1. r2 = A;  // sees write of A = 2
2. r1 = B;  // sees write of B = 1
3. B = 1;
4. A = 2;
在这个情况下，同样符合happens-before consistent，可以细品一下这个例子。
```markdown
In this execution, **the reads see writes that occur later in the execution order. This may seem counterintuitive, but is allowed by happens-before consistency**. Allowing reads to see later writes can sometimes produce unacceptable behaviors.
```
情况2是如此的令人难以理解，我也不确定自己的理解是否正确。
发生这种情况，本质的原因是，其实在实际情况下硬件的执行不是(顺序一致的)而是并发的，因此存在后发起的请求先执行完成的情况。
比如1先发出从主内存读取变量的r2的请求，但是由于某些原因(IO阻塞)，在这等待过程中，后发起的写r2请求先完成了，所以请求1最终读取到了后写入的值。
还不能理解的话参考上面顺序一致性部分，实现顺序一致性的条件1，关于**发送**的解释，那里的发送是原子性的，这里的发送是非原子性的。
当然这个是实际发生这种情况的猜测，不过这不重要。
通过这个例子我认为想表述的是，**只要没有明确的happens-before关系，那么跨线程的读取看到任何写操作都是合法的，这个操作集合就是happens-before consistency的**。
结合jls学术性的看这个例子，推理逻辑大概是这样：
1. 整体背景：程序每个线程必须符合单线程语义
2. 程序顺序：线程的程序顺序是该线程全部线程间操作基于单线程语义将被执行的顺序
3. 推理规则：单线程操作的happens-before基于程序顺序，多线程操作的happens-before基于synchronizes-with，happens-before具有传递性
在这个例子中，两个线程的**程序顺序**分别为，(1 -> 3)和(2 -> 4)，即hb(1, 3)和hb(2, 4)，但由于没有**正确同步**(程序存在**数据竞争**)，因此程序不会表现出**顺序一致性**。
具体点说，**线程间操作**1和2读取的值是**共享变量**，因此具体看到的值应该由JMM确定，而两个线程针对同一个**共享变量**的读/写没有**同步操作**，因此这两个操作不存在**synchronized-with**关系，在情况2这样的**执行顺序**下，不存在相应的**happens-before order关系阻止**读看到写的结果(所以操作集依然是happens-before consistency)，因此JMM认为是合法的。程序存在**数据竞争**不是**正确同步**，将不会表现为**顺序一致性**。

# Executions 执行
jls 17.4.6
```markdown
An execution E is described by a tuple <P, A, po, so, W, V, sw, hb>, comprising:
- P - a program
- A - a set of actions
- po - program order, which for each thread t, is a total order over all actions performed by t in A
- so - synchronization order, which is a total order over all synchronization actions in A
- W - a write-seen function, which for each read r in A, gives W(r), the write action seen by r in E.
- V - a value-written function, which for each write w in A, gives V(w), the value written by w in E.
- sw - synchronizes-with, a partial order over synchronization actions
- hb - happens-before, a partial order over actions
Note that the synchronizes-with and happens-before elements are uniquely determined by the other components of an execution and the rules for well-formed executions (§17.4.7).
An execution is happens-before consistent if its set of actions is happens-before consistent (§17.4.5).
```
这章可说的不多，定义了用符号表示执行的概念，E = <P, A, po, so, W, V, sw, hb>，如果操作集是happens-before consistency的，则执行也是。
这里执行的概念的指的是**程序的某次执行**(an execution of this program)，从上面的元组我们可以看出，so/sw/hb这三个值是“动态”的(随着每次调度的不同而变化)，所以才有Executions的这个概念。
# Well-Formed Executions
17.4.7
```markdown
We only consider well-formed executions. An execution E = < P, A, po, so, W, V, sw, hb > is well formed if the following are true:
1. Each read sees a write to the same variable in the execution.
   All reads and writes of volatile variables are volatile actions. For all reads r in A, we have W(r) in A and W(r).v = r.v. The variable r.v is volatile if and only if r is a volatile read, and the variable w.v is volatile if and only if w is a volatile write.
2. The happens-before order is a partial order. 
   The happens-before order is given by the transitive closure of synchronizes-with edges and program order. It must be a valid partial order: reflexive, transitive and antisymmetric.
3. The execution obeys intra-thread consistency. 
   For each thread t, the actions performed by t in A are the same as would be generated by that thread in program-order in isolation, with each write w writing the value V(w), given that each read r sees the value V(W(r)). Values seen by each read are determined by the memory model. The program order given must reflect the program order in which the actions would be performed according to the intra-thread semantics of P.
4. The execution is happens-before consistent (§17.4.6).
5. The execution obeys synchronization-order consistency. 
   For all volatile reads r in A, it is not the case that either so(r, W(r)) or that there exists a write w in A such that w.v = r.v and so(W(r), w) and so(w, r).
```
1. 这里定义了什么是正确构造(well-formed)的执行，严谨。
2. We only consider well-formed executions.意味我们程序的所有执行都是正确构造的(当然如此)，严谨。
#  Executions and Causality Requirements
jls 17.4.8
```markdown
1. A well-formed execution E = < P, A, po, so, W, V, sw, hb > is validated by committing actions from A. If all of the actions in A can be committed, then the execution satisfies the causality requirements of the Java programming language memory model.
Starting with the empty set as C0, we perform a sequence of steps where we take actions from the set of actions A and add them to a set of committed actions Ci to get a new set of committed actions Ci+1. To demonstrate that this is reasonable, for each Ci we need to demonstrate an execution E containing Ci that meets certain conditions.
Formally, an execution E satisfies the causality requirements of the Java programming language memory model if and only if there exist:
- Sets of actions C0, C1, ... such that:
  - C0 is the empty set
  - Ci is a proper subset of Ci+1
  - A = ∪ (C0, C1, ...)
  If A is finite, then the sequence C0, C1, ... will be finite, ending in a set Cn = A.
  If A is infinite, then the sequence C0, C1, ... may be infinite, and it must be the case that the union of all elements of this infinite sequence is equal to A.
- Well-formed executions E1, ..., where Ei = < P, Ai, poi, soi, Wi, Vi, swi, hbi >.
Given these sets of actions C0, ... and executions E1, ... , every action in Ci must be one of the actions in Ei.
```
**正确构造的执行通过提交(committing)操作集合A中的操作(actions)进行校验，如果A中全部的操作都可以被提交，则说明这个执行满足(satisfies)JMM因果依赖(causality requirements of the Java programming language memory model)**。
定义了如何校验某个执行是否是正确构造的，以及新增一个概念，**因果依赖**。
原文下面是每一步提交操作集合(Ci)相关规则正式的(Formally)的定义，目前感觉自己理解的也未必很准确，建议直接翻原文。
不过可以通过jls的例子，明白这个因果依赖是什么意思。
```markdown
Happens-before consistency is a necessary, but not sufficient, set of constraints. Merely enforcing happens-before consistency would allow for unacceptable behaviors - those that violate the requirements we have established for programs. For example, happens-before consistency allows values to appear "out of thin air". This can be seen by a detailed examination of the trace in Table 17.4.8-A.
Table 17.4.8-A. Happens-before consistency is not sufficient

Thread 1               |   Thread 2
r1 = x;                |     r2 = y;
if (r1 != 0) y = 1;    |     if (r2 != 0) x = 1;

The code shown in Table 17.4.8-A is correctly synchronized. This may seem surprising, since it does not perform any synchronization actions. Remember, however, that a program is correctly synchronized if, when it is executed in a sequentially consistent manner, there are no data races. If this code is executed in a sequentially consistent way, each action will occur in program order, and neither of the writes will occur. Since no writes occur, there can be no data races: the program is correctly synchronized.
Since this program is correctly synchronized, the only behaviors we can allow are sequentially consistent behaviors. However, there is an execution of this program that is happens-before consistent, but not sequentially consistent:
r1 = x;  // sees write of x = 1
y = 1;
r2 = y;  // sees write of y = 1
x = 1;
This result is happens-before consistent: there is no happens-before relationship that prevents it from occurring. However, it is clearly not acceptable: there is no sequentially consistent execution that would result in this behavior. The fact that we allow a read to see a write that comes later in the execution order can sometimes thus result in unacceptable behaviors.
Although allowing reads to see writes that come later in the execution order is sometimes undesirable, it is also sometimes necessary. As we saw above, the trace in Table 17.4.5-A requires some reads to see writes that occur later in the execution order. Since the reads come first in each thread, the very first action in the execution order must be a read. If that read cannot see a write that occurs later, then it cannot see any value other than the initial value for the variable it reads. This is clearly not reflective of all behaviors.
We refer to the issue of when reads can see future writes as causality, because of issues that arise in cases like the one found in Table 17.4.8-A. In that case, the reads cause the writes to occur, and the writes cause the reads to occur. There is no "first cause" for the actions. Our memory model therefore needs a consistent way of determining which reads can see writes early.
Examples such as the one found in Table 17.4.8-A demonstrate that the specification must be careful when stating whether a read can see a write that occurs later in the execution (bearing in mind that if a read sees a write that occurs later in the execution, it represents the fact that the write is actually performed early).
The memory model takes as input a given execution, and a program, and determines whether that execution is a legal execution of the program. It does this by gradually building a set of "committed" actions that reflect which actions were executed by the program. Usually, the next action to be committed will reflect the next action that can be performed by a sequentially consistent execution. However, to reflect reads that need to see later writes, we allow some actions to be committed earlier than other actions that happen-before them.
Obviously, some actions may be committed early and some may not. If, for example, one of the writes in Table 17.4.8-A were committed before the read of that variable, the read could see the write, and the "out-of-thin-air" result could occur. Informally, we allow an action to be committed early if we know that the action can occur without assuming some data race occurs. In Table 17.4.8-A, we cannot perform either write early, because the writes cannot occur unless the reads see the result of a data race.
```
我认为这个例子就是JMM规则的综合应用，建议细品。
这个例子有几个注意点：
1. 核心结论：**Happens-before consistency is a necessary, but not sufficient, set of constraints. Merely enforcing happens-before consistency would allow for unacceptable behaviors - those that violate the requirements we have established for programs. For example, happens-before consistency allows values to appear "out of thin air".**
2. 程序是正确同步的，原因是以顺序一致性的方式执行不存在写操作，所以没有数据冲突。
3. 但这个例子没有遵循正确同步程序的第二条，正确同步的程序应该只表现为顺序一致性。(there is an execution of this program **that is happens-before consistent, but not sequentially consistent**)
4. This result is happens-before consistent: **there is no happens-before relationship that prevents it from occurring.** However, it is clearly not acceptable: there is no sequentially consistent execution that would result in this behavior. The fact that we allow a read to see a write that comes later in the execution order can sometimes thus result in unacceptable behaviors.
5. bearing in mind that **if a read sees a write that occurs later in the execution, it represents the fact that the write is actually performed early**
6. The memory model takes as input a given execution, and a program, and determines whether that execution is a legal execution of the program. It does this by gradually building a set of "committed" actions that reflect which actions were executed by the program. Usually, the next action to be committed will reflect the next action that can be performed by a sequentially consistent execution. However, to reflect reads that need to see later writes, we allow some actions to be committed earlier than other actions that happen-before them.
综上，这个例子就是为了说明Happens-before consistency是必要的(necessary)但并不充分(sufficient)，只有这个会导致问题，比如凭空出现(out of thin air)。
当然在结尾也说，JMM不会允许这种情况出现。因此，我个人理解，JMM的实际规则是：
   **Happens-before consistency + causality requirements**

# Observable Behavior and Nonterminating Executions
jls 17.4.9
```markdown
For programs that always terminate in some bounded finite period of time, their behavior can be understood (informally) simply in terms of their allowable executions. For programs that can fail to terminate in a bounded amount of time, more subtle issues arise.
The observable behavior of a program is defined by the finite sets of external actions that the program may perform. A program that, for example, simply prints "Hello" forever is described by a set of behaviors that for any non-negative integer i, includes the behavior of printing "Hello" i times.
Termination is not explicitly modeled as a behavior, but a program can easily be extended to generate an additional external action executionTermination that occurs when all threads have terminated.
We also define a special hang action. If behavior is described by a set of external actions including a hang action, it indicates a behavior where after the external actions are observed, the program can run for an unbounded amount of time without performing any additional external actions or terminating. Programs can hang if all threads are blocked or if the program can perform an unbounded number of actions without performing any external actions.
A thread can be blocked in a variety of circumstances, such as when it is attempting to acquire a lock or perform an external action (such as a read) that depends on external data.
An execution may result in a thread being blocked indefinitely and the execution's not terminating. In such cases, the actions generated by the blocked thread must consist of all actions generated by that thread up to and including the action that caused the thread to be blocked, and no actions that would be generated by the thread after that action.
```
这篇定义程序可观测的行为，以及在程序永不终止的执行下，程序的行为如何定义。
1. 程序的行为表现为程序**可能执行外部调用**的有限集合
2. 程序终止(Termination)和挂起(hang)并没有被明确定义为行为，我们可以把这两者定义为一种外部调用。
3. 程序挂起后可以运行无限长的时间而没有任何外部调用或者终止
4. 一次执行可能会导致线程无限期地阻塞并且执行不会终止。 在这种情况下，被阻止的线程生成的操作必须包含该线程生成的所有操作（包括导致线程被阻止的操作），并且在该操作之后线程不会生成任何操作。

# 思考与总结

1. 为什么需要内存模型：理解并发核心点在于理解“偏序”和“全序”，全序是最符合直觉的(所以定义了顺序一致性)，但真实世界里其实是无序的，因此需要定义不同的内存模型(契约)，约束程序的行为，在语言角度，这个契约就是JMM。
2. 并发与并行：并发与并行，区别点在于并发强调的是时间点的概念，而并行强调的是时间段。计算机世界，我们应该忘记传统时间这个概念，改为理解并接受并发。
3. 为什么有hb和po：program order构建一套单线程偏序顺序，但多线程环境下并非是全序，所以还需要happens-before规则(当然其实还有因果依赖)，目标使多线程交互形成全序。
4. 如何运用happens-before：不再采用直觉(传统时间观)判断能否看到某个写，而改为是否存在相应的happens-before规则阻止看到某个写
5. 正确同步：在此之上，我们比较容易理解程序正确同步的含义，没有数据竞争后，相当于整体的操作指令是全序的，程序表现为顺序一致性。
6. 并发问题的本质：归根结底为可见性及原子性，上面说的全部规则解决的是可见性问题，而原子性的问题，也是语言角度无法解决的，而这部分需要程序员们对并发有理解后才能正确判断。


