# Dubbo

![](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/20210506203206.webp)

提供者**Provider** 启动然后向注册中心注册自己所能提供的服务。消费者 **Consumer** 启动向注册中心订阅自己所需的服务，然后注册中心将提供者元信息通知给 Consumer， 之后 Consumer 因为已经从注册中心获取提供者的地址，因此可以通过负载均衡选择一个 **Provider** 直接调用 。后续服务提供方元数据变更的话注册中心会把变更推送给服务消费者。服务提供者和消费者都会在内存中记录着调用的次数和时间，然后定时的发送统计数据到监控中心。

**dubbo架构**

![](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/20210506203256.webp)

- Service，业务层，就是开发的业务逻辑层。
- Config，配置层，主要围绕 ServiceConfig 和 ReferenceConfig，初始化配置信息。
- Proxy，代理层，服务提供者还是消费者都会生成一个代理类，使得服务接口透明化，代理层做远程调用和返回结果。
- Register，注册层，封装了服务注册和发现。
- Cluster，路由和集群容错层，负责选取具体调用的节点，处理特殊的调用要求和负责远程调用失败的容错措施。
- Monitor，监控层，负责监控统计调用时间和次数。
- Portocol，远程调用层，主要是封装 RPC 调用，主要负责管理 Invoker，Invoker代表一个抽象封装了的执行体，之后再做详解。
- Exchange，信息交换层，用来封装请求响应模型，同步转异步。
- Transport，网络传输层，抽象了网络传输的统一接口，这样用户想用 Netty 就用 Netty，想用 Mina 就用 Mina。
- Serialize，序列化层，将数据序列化成二进制流，当然也做反序列化。

SPI：JDK 内置的一个服务发现机制，它使得接口和具体实现完全解耦。我们只声明接口，具体的实现类在配置中选择。表现为定义了一个接口，然后在META-INF/services目录下放置一个与接口同名的文本文件，文件的内容为接口的实现类，多个实现类用换行符分隔。

调用过程：

![](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/20210506203350.webp)

1. 服务暴露：Provider 启动，通过 Proxy 组件根据具体的协议 Protocol 将需要暴露出去的接口封装成 Invoker，Invoker 是 Dubbo 一个很核心的组件，代表一个可执行体。再通过 Exporter 包装一下，这是为了在注册中心暴露自己套的一层，然后将 Exporter 通过 Registry 注册到注册中心。这就是整体服务暴露过程。
2. 消费过程：消费者启动会向注册中心拉取服务提供者的元信息，然后调用流程也是从 Proxy 开始，毕竟都需要代理才能无感知。Proxy 持有一个 Invoker 对象，调用 invoke 之后需要通过 Cluster 先从 Directory 获取所有可调用的远程服务的 Invoker 列表，如果配置了某些路由规则，比如某个接口只能调用某个节点的那就再过滤一遍 Invoker 列表。剩下的 Invoker 再通过 LoadBalance 做负载均衡选取一个。然后再经过 Filter 做一些统计什么的，再通过 Client 做数据传输，比如用 Netty 来传输。传输需要经过 Codec 接口做协议构造，再序列化。最终发往对应的服务提供者。服务提供者接收到之后也会进行 Codec 协议处理，然后反序列化后将请求扔到线程池处理。某个线程会根据请求找到对应的 Exporter ，而找到 Exporter 其实就是找到了 Invoker，但是还会有一层层 Filter，经过一层层过滤链之后最终调用实现类然后原路返回结果。

Dubbo默认底层走的是Netty**长链接**。

## Dubbo的服务暴露过程：

Dubbo 采用 URL 的方式来作为约定的参数类型，被称为公共契约。

URL 具体的参数如下：

- protocol：指的是 dubbo 中的各种协议，如：dubbo thrift http
- username/password：用户名/密码
- host/port：主机/端口
- path：接口的名称
- parameters：参数键值对

**Dubbo服务暴露全过程**：

![](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/20210506204648.png)

1. 检测配置，如果有些配置空的话会默认创建，并且组装成 URL
2. 暴露服务，包括暴露到本地的服务和远程的服务
3. 注册服务至注册中心

![](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/20210506204710.png)

从**对象构建转换的角度**看可以分为两个步骤：

1. 将服务实现类转成 Invoker
2. 将 Invoker 通过具体的协议转换成 Exporter

源码分析：
![](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/20210506205524.png)

service 标签其实就是对应 ServiceBean，实现了 `ApplicationListener<ContextRefreshedEvent>`，这样就会**在 Spring IOC 容器刷新完成后调用 `onApplicationEvent` 方法，而这个方法里面做的就是服务暴露**，这就是服务暴露的启动点：

![](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/20210506205631.png)

如果不是延迟暴露、并且还没暴露过、并且支持暴露的话就执行 export 方法，而 export 最终会调用父类的 export 方法：

![](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/20210506205730.png)

主要就是检查了一下配置，确认需要暴露的话就暴露服务， doExport 这个方法很长，不过都是一些检测配置的过程，主要看doExportUrls方法：

![](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/20210506210431.png)

Dubbo 支持多注册中心，并且支持多个协议，一个服务如果有多个协议那么就都需要暴露，比如同时支持 dubbo 协议和 hessian 协议，那么需要将这个服务用两种协议分别向多个注册中心（如果有多个的话）暴露注册。loadRegistries就是根据配置组装成注册中心相关的 URL。

最终Url格式：

```java
registry://127.0.0.1:2181/com.alibaba.dubbo.registry.RegistryService?application=demo-provider&dubbo=2.0.2&pid=7960&qos.port=22222&registry=zookeeper&timestamp=1598624821286
```

