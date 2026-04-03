# IM连接抽象与发送抽象三层对照说明

这份文档只回答一个问题：

**为什么会觉得 `ImSessionConnection.sendText(...)` 这件事有点别扭，以及如果继续拆，应该怎么理解。**

本文不用太抽象的话，直接用你项目里现在的类来画。

---

## 1. 先说结论

你现在的结构不是错的，而且已经能工作。

问题只在于：

- “连接是什么”
- “怎么把消息发到连接里”

这两件事现在被放在了同一个接口里：

- `ImSessionConnection`

当底层只有 Spring WebSocket 时，这没那么明显。  
当你开始同时考虑：

- Spring WebSocket
- Netty

就会发现真正别扭的，主要不是：

- `getId()`
- `isOpen()`
- `close()`

而是：

- `sendText()`

因为两种底层框架在“发送模型”上差异更大。

---

## 2. 你现在的结构

你现在大概可以理解成下面这样：

```text
MessagePushServiceImpl / ConversationWindowPushServiceImpl
                |
                v
      ImConnectionRegistry
                |
                v
       ImSessionConnection
        - getId()
        - getUserId()
        - isOpen()
        - sendText()
        - close()
                |
        -------------------
        |                 |
        v                 v
SpringSessionConnection   NettySessionConnection
```

也就是说：

- 先从 registry 里拿到连接
- 然后业务/推送层直接对连接调用 `sendText(...)`

这个模型很好理解，也很自然。

---

## 3. 现在这套结构是怎么调用的

以消息推送为例，逻辑大概是：

```text
1. MessagePushServiceImpl 取出某个用户的所有连接
2. 遍历每条 ImSessionConnection
3. 调用 connection.sendText(payload)
4. 如果抛异常，就 unregister 这条连接
```

可以把它想成这种伪代码：

```java
List<ImSessionConnection> connections = connectionRegistry.getConnections(userId);
for (ImSessionConnection connection : connections) {
    try {
        connection.sendText(payload);
    } catch (Exception ex) {
        connectionRegistry.unregister(userId, connection.getId());
    }
}
```

这种写法最大的优点是：

- 很直观
- 很容易写
- Spring WebSocket 下很顺手

---

## 4. 现在这套结构哪里开始别扭

问题出在：

```java
connection.sendText(payload)
```

这句在 Spring 和 Netty 下面，底层感觉不一样。

### 4.1 在 Spring 下面

更像：

- 调用一个同步发送方法
- 返回时，大体上就知道成功还是失败

### 4.2 在 Netty 下面

底层天然更像：

- 先发起发送
- 结果稍后通过 `ChannelFuture` 回来

所以：

- Spring 的 `sendText()` 比较像“当场拿结果”
- Netty 的 `sendText()` 比较像“先发起，再等结果”

这就说明：

**连接对象本身很好统一，但“发送消息”这件事不那么容易统一。**

---

## 5. 我说的“拆法”是什么

我说的不是把你现在整个设计推翻。

而是把它从：

```text
连接对象自己负责发送
```

慢慢变成：

```text
连接对象负责表示“连接”
发送器负责表示“怎么发消息”
```

也就是拆成三层：

```text
业务推送层
    |
    v
发送抽象层
    |
    v
连接抽象层
```

---

## 6. 我说的三层到底是哪三层

### 第一层：业务推送层

这层你已经有了，比如：

- `MessagePushServiceImpl`
- `ConversationWindowPushServiceImpl`

这层关心的是：

- 我要给谁推
- 我要推什么内容
- 推送失败后怎么处理业务上的清理

它不应该太关心底层是 Spring 还是 Netty。

---

### 第二层：发送抽象层

这是现在你还没有明确抽出来的一层。

可以起类似这种名字：

- `ImConnectionSender`
- `ImOutboundSender`
- `ImTextMessageSender`

它负责的是：

- 给某条连接发送文本
- 决定发送语义
- 处理发送失败
- 记录发送日志/指标

也就是说：

- 不是业务层直接纠结 `sendText`
- 而是专门有一层来做“发送”

---

### 第三层：连接抽象层

这一层就是你现在已经有的：

- `ImSessionConnection`
- `ImConnectionRegistry`

这一层更适合只表达：

- 我是谁
- 我还活着吗
- 我能不能关

例如：

- `getId()`
- `getUserId()`
- `isOpen()`
- `close()`

---

## 7. 拆完以后结构长什么样

如果按这个思路继续拆，大概会变成这样：

```text
MessagePushServiceImpl / ConversationWindowPushServiceImpl
                |
                v
         ImConnectionSender
                |
                v
       ImSessionConnection
        - getId()
        - getUserId()
        - isOpen()
        - close()
                |
        -------------------
        |                 |
        v                 v
SpringSessionConnection   NettySessionConnection
```

也就是说：

- 业务层不直接 `connection.sendText(...)`
- 而是：
  - 业务层决定“要推什么”
  - sender 决定“怎么发”
  - connection 只提供“这条连接是什么”

---

## 8. 拆完以后分别怎么调用

### 8.1 现在的调用方式

```text
MessagePushServiceImpl
    -> connectionRegistry.getConnections(userId)
    -> for each connection
    -> connection.sendText(payload)
```

### 8.2 拆完后的调用方式

```text
MessagePushServiceImpl
    -> connectionRegistry.getConnections(userId)
    -> for each connection
    -> connectionSender.sendText(connection, payload)
```

伪代码会变成：

```java
List<ImSessionConnection> connections = connectionRegistry.getConnections(userId);
for (ImSessionConnection connection : connections) {
    connectionSender.sendText(connection, payload);
}
```

也就是说：

- `connection` 不再自己承担全部发送语义
- `connectionSender` 成了中间那层“专门发消息的人”

---

## 9. 为什么这样拆会更顺

因为这样以后：

### 9.1 Spring 和 Netty 的差异会被压在 sender 层

例如：

- Spring sender 可以偏同步处理
- Netty sender 可以偏异步处理

而不是强迫 `ImSessionConnection.sendText(...)` 必须完美统一两种不同风格。

---

### 9.2 连接对象会更稳定

连接对象本身最稳定的职责通常就是：

- 标识
- 状态
- 生命周期

这些在 Spring 和 Netty 下差异没那么大。

所以这部分更适合留在最底层抽象里。

---

### 9.3 日志、指标、失败处理更容易集中

如果以后发送逻辑集中到 sender 层，你会更容易统一：

- 发送成功率
- 发送失败率
- 失败后是否 unregister
- 发送日志

而不是让每个业务推送类都自己处理一遍。

---

## 10. 这是不是说明你现在的设计错了

不是。

你现在的设计属于：

- 第一阶段很合理
- 先把 Spring 的 `WebSocketSession` 从业务层里抽掉
- 先把连接统一起来

这一步非常值钱。

现在讨论的“要不要把发送再拆一层”，是更后一步的优化，不是说你前面做错了。

也就是说：

- 你现在这套能继续往前走
- 只是当你开始同时考虑 Spring 和 Netty 时
- 会更清楚看到 `sendText(...)` 这个点有点重

---

## 11. 现在到底要不要立刻拆

我的建议是：

### 如果你现在目标是先把 Netty 接进来

那不用立刻大拆。

先继续保留：

- `ImSessionConnection.sendText(...)`

让 Netty 骨架先落地。

### 如果你后面发现这两个问题越来越明显

- Spring 和 Netty 的发送模型越来越别扭
- 推送失败/日志/指标处理开始到处散

那再把 sender 层单独抽出来，会更顺。

---

## 12. 最后用一句最简单的话总结

你现在没听懂的点，本质上就是这句：

**连接对象回答“这条连接是什么”，发送抽象回答“怎么把消息发到这条连接里”。**

你现在是把这两个问题先放在了一起。  
这不是错，只是当你开始同时兼容 Spring 和 Netty 时，这两个问题开始显得不像同一层了。
