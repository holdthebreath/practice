### 并发的理解

```markdown
Concurrency, Time, and Relativity
It may seem that two operations should be called concurrent if they occur “at the same time”—but in fact, **it is not
important whether they literally overlap in time**.
Because of problems with clocks in distributed systems, it is actually quite difficult to tell whether two things
happened at exactly the same time—an issue
we will discuss in more detail in Chapter 8.
For defining concurrency, exact time doesn’t matter: we simply call
**two operations concurrent if they are both unaware of each other, regardless of the physical time at which they
occurred**.
People sometimes make a connection between this principle and the special theory of relativity in physics [54], which
introduced the idea that information cannot travel faster than the speed of light.
Consequently, two events that occur some distance apart cannot possibly affect each other if the time between the events
is shorter than the time it takes light to travel the distance between them.
In computer systems, two operations might be concurrent even though the speed of light would in principle have allowed
one operation to affect the other.
For example, if the network was slow or interrupted at the time, two operations can occur some time apart and still be
concurrent, because the network problems prevented one operation from being able to know about the other.

Kleppmann, Martin. Designing Data-Intensive Applications (p. 297). O'Reilly Media. Kindle Edition. 
```

看到这段突然意识到自己以前对并发的理解还是有点太狭隘了，太过拘泥于"时间"的概念。
只停留在用户端"同时"发送请求的这个场景，但其实有太多场景比如提到的网络延迟，比如我现在想到两个请求线程池并发处理，甚至cpu的线程时钟片轮转快慢？
这些其实都不是分布式系统特有，只是分布式系统毫无疑问的放大了这种概率（不可靠的网络/不可靠的时钟），也许传统意义的"时间"
在计算机的世界确实没有意义。
在理解了这个基础上，就能明白写冲突解决方案的也是最容易想到的LWW（Last write wins (discarding concurrent writes)
并不适合数据不丢失的场景，
比如分布式数据库，但用在数据库缓存就是很适合的（缓存本来就被允许丢失，方案实现成本很低）。

### The “happens-before” relationship and concurrency

```markdown
An operation A happens before another operation B if B knows about A, or depends on A, or builds upon A in some way.
Whether one operation happens before another operation is the key to defining what concurrency means.
In fact, we can simply say that two operations are concurrent if neither happens before the other (i.e., neither knows
about the other) [54].

Kleppmann, Martin. Designing Data-Intensive Applications (p. 295). O'Reilly Media. Kindle Edition. 
```

说的比较清楚，两个操作存在"happens-before"关系则两者不是并发，否则就是并发。（操作是否互相透明）
基于这点，推出了以下逻辑，如果是"happens-before"关系，后的操作重写前操作，但如果是并发，就需要**解决冲突**。

```markdown
If one operation happened before another, the later operation should overwrite the earlier operation,
but if the operations are concurrent, we have a conflict that needs to be resolved.

Kleppmann, Martin. Designing Data-Intensive Applications (p. 296). O'Reilly Media. Kindle Edition. 
```

### 分布式思维

现在越来越觉得分布式系统太有意思也太有挑战了。其中我觉得很重要的一点就是"分布式"思维。
比如常用的consumer-provider模型，那某一个节点挂掉，这种都比较好理解。
但真的到了分布式数据库，思想的转变或者说开阔还挺有挑战的。
从数据库基础的编解码格式需要实现向前兼容/向后兼容开始，对我来说就理解了一段时间。
接着走入分布式的大门第一章，备份。
从这里开始，单主节点，主从同步这些有点概念还可以（mysql/zookeeper选举），多数据中心，相关的感觉也还行。
然后到了这个无主节点的集群，一开始感觉还可以，无主相比较有主的优（不需要故障转移）劣（写冲突），直到到了并发导致写冲突开始，
就开始变得越来越有趣了。
并发写冲突检测这小节一共才15页，但光里面的happen-before算法就理解了好长一段时间。
接着到了最后一小部分内容，版本向量（Version vectors），这部分很短。
但我光看原文已经不明白**The collection of version numbers from all the replicas is called a version vector**这是个什么样的数据结构。
甚至我下意识的以为类似是版本号+前缀，前缀指定节点的那种形式，但是我看了后面感觉不太对劲，又返回来看，还是没这个概念，而且仔细想了一下，如果请求处理都需要
根据前缀匹配特定节点，那不是就失去了无主集群的优势了吗？（万一前缀的那个节点挂了呢？）
那么为什么我说分布式思维还需要锻炼呢？
原文**The version vector structure ensures that it is safe to read from one replica and subsequently write back to another replica.**
我看懂这句话但一时间根本不明白为什么版本向量可以保证从一个副本读另一副本写是安全的。
我下意识的还停留在"备份"这个字面意思上，备份只是备份，不会有任何写功能。
