# IM WebSocket Handler 收缩与协议层拆分说明

这份文档专门解释一件事：

- 为什么现在还要继续收缩 `ImWebSocketHandler`
- 这里说的“收缩”到底是什么意思
- 为什么要拆成 `InboundAdapter`、`ProtocolDispatcher`、`ProtocolCodec`
- 以后如果切 Netty，这样拆到底能帮到什么

本文尽量用你当前项目里的代码来解释，不用太抽象的话。

---

## 1. 先说结论

当前的 `ImWebSocketHandler` 已经比最开始更好了，因为它不再把 `WebSocketSession` 直接往注册表、推送层传播。

但是它现在仍然同时负责了很多事情：

- 接收 Spring 的 `TextMessage`
- 从 Spring `WebSocketSession` 里取用户信息
- 解析 JSON
- 判断消息类型
- 处理心跳
- 处理 `send_message`
- 组装错误回包
- 把对象序列化成 JSON 再发回去

这就说明：

- 它还不是“纯入口适配层”
- 它还是一个“入口 + 协议 + 部分业务调度”混在一起的类

所以这里说的“继续收缩 `ImWebSocketHandler`”，意思不是把它删掉，而是：

**让它只负责 Spring WebSocket 的接入适配，不再承担协议解析和业务路由这些职责。**

---

## 2. 现在的 `ImWebSocketHandler` 到底做了多少事

当前这个类：

- `src/main/java/com/bilibili/im/websocket/handler/ImWebSocketHandler.java`

它现在做的事情可以按流程拆成下面几步。

### 2.1 连接建立时

- Spring 调 `afterConnectionEstablished(...)`
- 这里从 `WebSocketSession` 里拿出用户 id
- 把它包装成 `SpringSessionConnection`
- 注册进 `ImConnectionRegistry`

这一段其实是比较合理的，因为它确实属于“Spring 接入层”。

### 2.2 收到文本消息时

Spring 调：

- `handleTextMessage(WebSocketSession session, TextMessage message)`

然后当前类继续做：

1. 取用户信息
2. 更新连接活跃时间
3. 把 `message.getPayload()` 取出来
4. 用 `objectMapper.readValue(...)` 解析 JSON
5. 判断 `type`
6. 如果是 `heartbeat` 就直接回 `heartbeat_ack`
7. 如果是 `send_message` 就直接调用应用服务
8. 如果不支持就直接回 error

你可以看到，这里面其实已经混了 3 类职责：

- Spring 接入职责
- 协议解析职责
- 业务分发职责

### 2.3 回包时

当前类里还有：

- `sendError(...)`
- `sendSimpleMessage(...)`
- `sendJsonMessage(...)`

这里又做了：

- 出站 DTO 组装
- JSON 序列化
- 发送到底层连接

所以它还在承担出站协议层的一部分。

---

## 3. 为什么这会影响以后切 Netty

因为现在如果你以后把底层从 Spring WebSocket 换成 Netty，Netty 的入口不会再给你：

- `TextWebSocketHandler`
- `WebSocketSession`
- `TextMessage`

但你现在的 `ImWebSocketHandler` 里，不只是“接收 Spring 消息”依赖这些东西，连下面这些也夹在里面：

- JSON 解析
- 类型判断
- 心跳处理
- 发送消息分发
- 错误回包

这就意味着：

- 以后不是简单把“入口注册方式”换一下就结束
- 而是要把整段协议处理逻辑再搬一遍

换句话说：

**当前的问题不是 `ImWebSocketHandler` 存在，而是它里面塞了太多不属于 Spring 入口层的逻辑。**

---

## 4. 什么叫“入口适配层”

你可以把系统分成两层看：

### 4.1 入口适配层

这一层负责：

- 接住 Spring 或 Netty 给你的原始连接和原始消息
- 转成你系统内部统一认识的东西
- 再把处理结果发回去

这一层是“和底层框架打交道”的。

### 4.2 协议/业务层

这一层负责：

- JSON 怎么解析
- 消息类型怎么判断
- 心跳怎么处理
- `send_message` 该调用哪个应用服务

这一层不应该关心：

- 当前底层到底是 Spring 还是 Netty

所以理想状态是：

- Spring 适配层只负责接消息和回消息
- 协议/业务层只负责解释消息内容和分发逻辑

---

## 5. 为什么建议拆成 3 层

这里建议拆成：

1. `SpringWebSocketInboundAdapter`
2. `ImProtocolDispatcher`
3. `ImProtocolCodec`

不是因为名字好看，而是因为这 3 层刚好对应了 3 种不同职责。

---

## 6. 第一层：`SpringWebSocketInboundAdapter`

这层可以理解成未来 `ImWebSocketHandler` 的瘦身版本。

### 6.1 它应该负责什么

它只负责这些事情：

- 接住 Spring 的 `WebSocketSession`
- 接住 Spring 的 `TextMessage`
- 取连接上的用户信息
- 把 Spring Session 包装成 `SpringSessionConnection`
- 把原始文本 payload 交给下层协议调度器
- 把下层返回的出站消息再写回连接

### 6.2 它不应该再负责什么

它不应该再做这些事情：

- 自己手写 JSON 解析
- 自己判断 `heartbeat` 还是 `send_message`
- 自己决定调用哪个业务方法
- 自己决定怎么组装各种 error DTO

### 6.3 为什么这一层以后还要存在

因为即使以后换 Netty，这种“入口适配层”也一定还会有，只不过名字可能变成：

- `NettyWebSocketInboundAdapter`

它们的职责一样，只是对接的底层框架不同。

所以这层的本质不是“Spring 专用逻辑”，而是“传输层适配逻辑”。

---

## 7. 第二层：`ImProtocolDispatcher`

这一层是整个拆分里最关键的。

### 7.1 它应该负责什么

它负责：

- 接收已经解析好的入站消息对象
- 看消息类型是什么
- 决定调用哪条处理分支

比如：

- `heartbeat`
- `send_message`
- 未来的 `ack`
- 未来的 `read`

### 7.2 为什么要把“类型判断”抽出去

因为“根据消息类型分发逻辑”根本不是 Spring WebSocket 的事情。

它属于：

- IM 协议本身的事情

如果把这层留在 `ImWebSocketHandler` 里，那么以后 Netty 也得重新写一套同样的 if/else。

如果把它抽成：

- `ImProtocolDispatcher`

那以后：

- Spring 入口把消息交给它
- Netty 入口也把消息交给它

这层就能复用。

### 7.3 它和业务服务是什么关系

它不是业务服务本身。

它更像一个“协议层路由器”：

- `heartbeat` -> 调心跳处理器
- `send_message` -> 调发送消息应用服务
- 不支持的类型 -> 生成协议错误响应

所以它应该站在：

- 入口适配层之下
- 应用服务之上

---

## 8. 第三层：`ImProtocolCodec`

这一层是“协议编解码层”。

### 8.1 编码是什么意思

编码就是：

- Java 对象
- 变成 JSON 文本

比如：

- `ImWebSocketOutboundMessageDTO`
- 变成一个字符串

### 8.2 解码是什么意思

解码就是：

- JSON 文本
- 变成 Java 对象

比如：

- `{"type":"heartbeat"}`
- 变成 `ImWebSocketInboundMessageDTO`

### 8.3 为什么要单独抽出来

因为现在这些动作散在 `ImWebSocketHandler` 里：

- `objectMapper.readValue(...)`
- `objectMapper.writeValueAsString(...)`

如果以后继续扩协议，编码/解码规则会越来越多。

把它抽出来后，好处是：

- 入口适配层不再关心 JSON 细节
- 协议调度层不再关心 ObjectMapper
- 以后如果协议格式要调整，改 codec 就更集中

---

## 9. 用一条消息走一遍未来的流程

这里用 `heartbeat` 举例。

### 9.1 现在的流程

当前大概是：

1. Spring 调 `ImWebSocketHandler.handleTextMessage(...)`
2. handler 自己取 payload
3. handler 自己 `readValue`
4. handler 自己判断 `type=heartbeat`
5. handler 自己组装 `heartbeat_ack`
6. handler 自己序列化
7. handler 自己发回去

### 9.2 拆分后的流程

未来更合理的流程是：

1. `SpringWebSocketInboundAdapter`
   - 接到 Spring 的文本消息
2. `ImProtocolCodec`
   - 把 JSON 解码成 `ImWebSocketInboundMessageDTO`
3. `ImProtocolDispatcher`
   - 判断是 `heartbeat`
   - 走心跳处理逻辑
   - 返回一个出站消息对象
4. `ImProtocolCodec`
   - 把出站对象编码成 JSON
5. `SpringWebSocketInboundAdapter`
   - 通过连接抽象把文本发回去

这样每一层职责都更单纯。

---

## 10. 如果以后换 Netty，会复用哪些层

这才是这次拆分最重要的价值。

如果未来换 Netty：

### 10.1 需要换的

- Spring 入口适配层
- Spring 连接适配实现

也就是：

- `SpringWebSocketInboundAdapter`
- `SpringSessionConnection`

### 10.2 不需要重写的

如果拆得好，这些应该能继续复用：

- `ImProtocolDispatcher`
- `ImProtocolCodec`
- `ImConnectionRegistry`
- `InMemoryImConnectionRegistry`
- 推送服务
- 会话清理逻辑
- 应用服务

也就是说，Netty 来了以后，只要补：

- `NettyWebSocketInboundAdapter`
- `NettySessionConnection`

协议和业务层基本不用跟着重写。

这就是为什么我前面说：

**Netty 入口只需要复用同一套 dispatcher / codec，不用把协议逻辑再写一遍。**

---

## 11. 你现在最容易误解的点

### 11.1 不是把 `ImWebSocketHandler` 删掉

不是。

而是让它从：

- “大而全的入口类”

变成：

- “只负责 Spring 接入的薄适配器”

### 11.2 不是现在就把所有类拆得很碎

也不是。

你现在最值得先拆出来的，通常只有一个优先级最高的类：

- `ImProtocolDispatcher`

因为“消息类型判断和业务分发”是当前最不该继续留在 handler 里的那层逻辑。

`ImProtocolCodec` 可以一起拆，也可以稍后再拆。

### 11.3 不是为了好看而拆

这不是纯粹重构洁癖。

它真正解决的是：

- 以后换 Netty 时，哪些代码能复用
- 哪些代码只应该在 Spring 适配层里存在

---

## 12. 推荐的实际拆分顺序

如果按你当前项目现状，我建议按这个顺序来：

### 第一步：先抽 `ImProtocolDispatcher`

先把 `heartbeat` / `send_message` / unsupported 这些分发逻辑从 `ImWebSocketHandler` 拿出去。

目标：

- handler 不再自己决定走哪条业务分支

### 第二步：再抽 `ImProtocolCodec`

把：

- `readValue`
- `writeValueAsString`

这些协议编解码逻辑集中起来。

目标：

- handler 不再直接依赖 ObjectMapper 去理解协议细节

### 第三步：最后把 `ImWebSocketHandler` 改名或定位成 adapter

这一步可以不急着改名字，但职责上要收缩成：

- 只接 Spring 输入
- 只调用 codec / dispatcher
- 只把结果发出去

等这一步完成后，它本质上就已经是 `SpringWebSocketInboundAdapter` 了。

---

## 13. 一句话总结

“继续收缩 `ImWebSocketHandler`” 的本质不是把类切碎，而是把它里面不属于“Spring 入口适配层”的职责搬出去。

最合理的分层是：

- `SpringWebSocketInboundAdapter`
  - 只负责 Spring 接入
- `ImProtocolDispatcher`
  - 只负责按消息类型分发
- `ImProtocolCodec`
  - 只负责 JSON 与 DTO 的编解码

这样以后如果切 Netty，你主要是替换：

- 入口适配层
- 连接适配层

而不是把协议处理和业务调度整套再写一遍。
