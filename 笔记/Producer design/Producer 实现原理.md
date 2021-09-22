# Producer 实现原理

[Random walk](https://www.notion.so/Random-walk-cbf97f685b6d4390960910dd3cad3891)

[上文](http://mp.weixin.qq.com/s?__biz=MzA5NTkxNDA1Mw==&mid=2247483888&idx=1&sn=57d28fa6dcd57b71215c8045f97e13a5&chksm=90b950daa7ced9cc6e96206374ea46b6ab5a2bf0d8b3c62e0b339a4fd2dd66fe0e2c9dfa9452&scene=21#wechat_redirect) 介绍了 RocketMQ 中 name server 的实现原理，本文介绍 producer 的核心原理。

**注意：本系列文章中尽量不出现大段代码，通过文字及图片描述其核心逻辑与设计。具体解析源码参见：**

[GitHub - zhouwentong1993/rocketmq: RocketMQ 源码阅读](https://github.com/zhouwentong1993/rocketmq)

## Producer 简介

![RocketMQ 核心架构](../pics/RocketMQ%20structure.png)

Producer 负责生产消息，通常由业务系统负责。消息生产者会通过 name server 获取 broker 地址，然后将消息发送给 broker。发送方式主要包括同步发送、异步发送和单向发送三种。

## 生产消息 demo

发送消息代码比较简单。

- 构造 `DefaultMQProducer`
传入生产者组名（暂不涉及）传入 name server 地址（也可通过环境变量指定）后调用 `start` 便可启动一个 Producer。
- 构造消息并发送。
调用不同的 send 方法可以实现不同的发送消息逻辑。

```java
// 同步发送消息代码示例。
public static void main(String[] args) throws Exception {
    DefaultMQProducer producer = new DefaultMQProducer("group-name");
    producer.setNamesrvAddr("localhost:9876");
    producer.start();

    for (int i = 0; i < 100; i++) {
        Message message = new Message("TopicTest", "TagA", ("Hello RocketMQ" + i).getBytes(StandardCharsets.UTF_8));
        SendResult result = producer.send(message);
        System.out.println(result);
    }
    
    producer.shutdown();
}
```

## 获取路由信息

在构造 `Message` 对象时，只需要 `topic` 信息。但只有 `topic` 是不够的，我们需要借助 `name server` 获取与 `topic` 相关的 `broker` 和 `message queue` 数据，完成信息的投递动作。

路由对象是 `TopicRouteData` ，包含了该 `topic` 对应的 `broker` 和 `queue` 的数据。

```java
public class TopicRouteData {
    private String orderTopicConf;
    private List<QueueData> queueDatas;
    private List<BrokerData> brokerDatas;
}
```

当 `producer` 启动后，系统每隔 120s 向 `name server` 同步最新的 `topic` 路由信息，并缓存本地。

![Producer 同步路由数据](../pics/producer%20sync%20data.png)

代码详见：`MQClientInstance#updateTopicRouteInfoFromNameServer`

## 消息路由算法

当获取到路由信息后，需要根据算法来选择消息的目的地，RocketMQ 支持两种消息路由算法。

### 简单轮询算法

轮询实现比较简单，每个 producer 线程都会维护一个 ThreadLocal<Integer>，每次对 ThreadLocal  的数据进行自增操作，然后对 message queue 集合进行取模，下标对应的 message queue 就是路由的结果。

```java
if (lastBrokerName != null) {
    for (int i = 0; i < this.messageQueueList.size(); i++) {
        int index = this.sendWhichQueue.incrementAndGet();
        int pos = Math.abs(index) % this.messageQueueList.size();
        // 为什么要判断 < 0 呢，是因为当 Math.abs 的参数是 Integer.MIN_VALUE 时，返回值仍然是 Integer.MIN_VALUE
        if (pos < 0) {
            pos = 0;
        }
        MessageQueue mq = this.messageQueueList.get(pos);
        // 尽量不选择之前的 broker
        if (!mq.getBrokerName().equals(lastBrokerName)) {
            return mq;
        }
    }
}
return selectOneMessageQueue();
```

### 故障延迟算法

**一、算法介绍**

故障延迟是当 broker 出现故障（响应慢、网络出错）时，通过路由算法保证一段时间内不去调用 broker，达到延迟故障的目的。

RocketMQ 定义 `FaultItem` 类来记录 broker 请求的数据，包括调用耗时和能够提供服务的开始时间两个核心字段。

```java
class FaultItem implements Comparable<FaultItem> {
		// broker name
	  private final String name;
		// 调用 broker 的耗时
	  private volatile long currentLatency;
		// broker 能够提供服务的时间
	  private volatile long startTimestamp;
}
```

当每次请求 broker 之后，都会用 broker name 和调用耗时来更新 FaultItem 对象。`startTimestamp` 是根据 `currentLatency` 动态计算出的。

RocketMQ 定义了调用耗时和不可用时长的对应关系：调用耗时越长，不可用时长越长。当出现网络异常，调用耗时被设置为 30000 ms，也就是不可用时长为 600000 ms。

```java
private long[] latencyMax = {50L, 100L, 550L, 1000L, 2000L, 3000L, 15000L};
private long[] notAvailableDuration = {0L, 0L, 30000L, 60000L, 120000L, 180000L, 600000L};
```

**二、具体实现**

当 `MQFaultStrategy#sendLatencyFaultEnable` 被设置为 true 时，延迟故障算法启用。

对 message queue 列表轮询，判断选择到的 broker 是否可用（`System.currentTimeMillis() - startTimestamp) >= 0`）如果可用，则选择该队列。**如果全部不可用，则按照可用性对所有 broker 进行排序，在最好的前一半里随机挑选一个作为此次路由的结果。**

**为什么不选择可用性最优的节点作为路由结果呢？如果所有请求都发送给最优节点，可能会造成 broker 服务压力过大，并且流量会发生倾斜。因此通过随机因素来平衡流量。更具体的讨论可参见：**

[MQFaultStrategy optimize by alaric27 · Pull Request #686 · apache/rocketmq](https://github.com/apache/rocketmq/pull/686)

## 消息发送

当 client 经过路由算法明确了要发往的 broker 地址后，就可以进行消息发送的操作了。

### 发送前的准备

**一、vip channel**

broker 在启动时会暴露三个端口号：10911、10909 和 10912。10912 被用作主从复制，另外两个对外提供服务。

![broker 暴露的端口号](../pics/broker%20export%20ports.png)

如果 client 配置了 `vipChannelEnabled` 参数，client 会向 broker 的 10909 端口发送数据。

**vip channel 的意义是隔离读写操作。在消息 API 中最重要的就是支持高 RTT（Round-Trip Time）当消息出现大量堆积的时候，写操作会阻塞 Netty 的 IO 线程。这时，我们可以通过向 vip 通道发送消息来保证发送消息的 RTT。**

详见：

[Some question about "vip channel"? · Issue #1510 · apache/rocketmq](https://github.com/apache/rocketmq/issues/1510)

**二、消息压缩**

当消息 body 超过 4kb 时，client 会借助 `DeflaterOutputStream` 来对 body 进行压缩。

**三、执行钩子函数并组装请求体**

如果配置了 `CheckForbiddenHook` 或者 `SendMessageHook` 先执行其逻辑。

组装 `SendMessageRequestHeader` 设置消息类别（延时消息、事务消息）

### 发送后的处理

消息的发送方式包括同步、异步和 oneway 三种。这里我们着重介绍同步和异步消息的设计。

① 执行 RPC 请求前置 hook。

② 异步调用限流，尝试获取信号量。

③ 创建唯一标识 `opaque`，构造 `ResponseFuture` 对象，放入 `responseTable`

④ 通过 Netty client 异步发送请求，处理回调函数，设置 `ResponseFuture` 状态。

⑤ 同步调用通过 `CountDownLatch` 带超时阻塞获取执行结果，返回 command。

⑥ 异步调用由 Netty server 传回 `opaque` ，触发异步消息 `callback` 接口。

⑦ 每秒执行一次定时任务，扫描 `reponseTable` 中超时的 `ResponseFuture`，抛出异常。

⑧ 执行 RPC 请求后置 hook。

**同步消息的同步转异步，异步消息的回调机制**都是 RPC 框架常见的设计方案，值得借鉴。

## 总结

本文完整介绍了 Producer 发送消息的全流程。

- Producer 是消息生产者，通过 name server 获得 broker 地址并将消息投递到 broker 中。
- 整个流程包括路由信息获取、路由信息过滤、发送前准备以及发送后处理四步。
- 故障延迟算法、VIP 通道设计、同步异步机制都是我们值得借鉴的点，可以用到系统设计中。