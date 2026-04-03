# IM WebSocket 耦合分析与 Netty 迁移前重构建议

## 1. 背景与结论

当前 IM 实时链路是基于 Spring WebSocket 实现的。后续如果希望切换到底层更灵活的 Netty，那么现在这套代码里不能继续让业务层、连接管理层、消息推送层直接依赖 Spring 的：

- `WebSocketSession`
- `TextWebSocketHandler`
- `HandshakeInterceptor`

先给结论：

- 当前项目的业务主链路还没有完全绑死在 Spring WebSocket 上
- 但是连接管理层和出站推送层与 Spring WebSocket 的耦合已经比较深
- 如果继续在当前边界上堆群聊、ACK、已读、重试等能力，未来切 Netty 的迁移成本会快速上升

换句话说，问题的重点不是“现在能不能用 Spring WebSocket 做功能”，而是“现在的接口边界有没有给未来切 Netty 留空间”。

## 2. 当前耦合点梳理

### 2.1 传输入口耦合

当前 WebSocket 入口直接建立在 Spring WebSocket 生命周期之上。

代表位置：

- `com.bilibili.config.web.ImWebSocketConfig`
- `com.bilibili.im.websocket.handler.ImWebSocketHandler`
- `com.bilibili.im.websocket.interceptor.ImWebSocketHandshakeInterceptor`

现状表现：

- `ImWebSocketConfig` 直接通过 Spring 的 `WebSocketConfigurer` 注册 handler 和 interceptor
- `ImWebSocketHandler` 直接继承 `TextWebSocketHandler`
- `ImWebSocketHandshakeInterceptor` 直接实现 `HandshakeInterceptor`

这意味着：

- 当前传输层入口完全是 Spring WebSocket 的编程模型
- 如果以后换成 Netty，入口注册、握手认证、连接建立回调都会整体重写

这部分本身不算最危险，因为任何传输实现都会有自己的入口适配层。真正危险的是后面的会话管理和推送逻辑也跟着一起用了 Spring 类型。

### 2.2 会话模型耦合

当前最核心的耦合点，是会话注册表接口直接暴露了 Spring 的 `WebSocketSession`。

代表位置：

- `com.bilibili.im.websocket.session.ImWebSocketSessionRegistry`
- `com.bilibili.im.websocket.session.impl.InMemoryImWebSocketSessionRegistry`

现状表现：

- `ImWebSocketSessionRegistry` 的 `register`、`getSessions`、`removeExpiredSessions` 都直接使用 `WebSocketSession`
- `InMemoryImWebSocketSessionRegistry` 内部 `SessionRecord` 里保存的也是 `WebSocketSession`
- 在线连接数、过期清理、活跃时间更新，全部围绕 Spring Session 对象展开

这意味着：

- 上层代码无法依赖“自己的连接抽象”，只能依赖 Spring Session
- 以后如果切 Netty，注册表接口本身就得改
- 一旦接口改动，上层所有读取连接并发消息的地方也都要跟着改

这一层是当前最应该优先抽象的地方。

### 2.3 出站推送耦合

当前消息推送和会话窗口推送，并没有依赖统一的发送抽象，而是直接对 Spring Session 发消息。

代表位置：

- `com.bilibili.im.websocket.service.impl.MessagePushServiceImpl`
- `com.bilibili.im.websocket.service.impl.ConversationWindowPushServiceImpl`

现状表现：

- 先通过 `ImWebSocketSessionRegistry.getSessions(userId)` 拿到 `List<WebSocketSession>`
- 再直接 `session.sendMessage(new TextMessage(...))`
- 发送失败时直接基于 Spring Session 的 `session.getId()` 做 unregister

这意味着：

- 推送层不是依赖“连接能力”，而是依赖 Spring WebSocket 的具体发送 API
- 后续如果切 Netty，推送层不是简单换个底层实现，而是业务推送代码本身也要改

这一点风险很高，因为未来群聊、批量分发、在线通知、会话更新推送都会重复走这里。

### 2.4 清理与关闭耦合

心跳清理任务当前也直接操作 Spring 的 `WebSocketSession`。

代表位置：

- `com.bilibili.im.websocket.task.ImWebSocketHeartbeatCleanupTask`

现状表现：

- `removeExpiredSessions(...)` 返回的是 `List<WebSocketSession>`
- 清理任务里直接判断 `session.isOpen()`
- 关闭时直接调用 Spring 的 `expiredSession.close(...)`

这意味着：

- 连接关闭语义没有被抽象成平台无关的能力
- 清理逻辑和连接实现绑在一起

如果以后换成 Netty，这里不仅要换关闭调用方式，还要重写过期连接的载体类型。

### 2.5 协议处理与传输耦合

`ImWebSocketHandler` 当前同时承担了太多职责。

代表位置：

- `com.bilibili.im.websocket.handler.ImWebSocketHandler`

现状表现：

- 接收 Spring `TextMessage`
- 读取 Session 属性中的用户信息
- 解析 JSON 为 `ImWebSocketInboundMessageDTO`
- 判断消息类型
- 处理心跳
- 处理 `send_message`
- 组装错误回包和成功回包

这意味着：

- 传输适配层、协议层、业务调度层耦合在同一个类里
- 将来如果切 Netty，不只是改连接接入逻辑，而是协议分发和业务入口也会被一起牵动

当前它还不算“全是业务逻辑”，但已经不是一个纯适配层了。

## 3. 风险分析

### 3.1 未来直接切 Netty 时，最痛的地方不在握手

现在最容易误判的点是：好像只要把 Spring 的 WebSocket 入口换成 Netty 的 WebSocket 入口就行。

实际上最痛的地方通常不是：

- `ImWebSocketConfig`
- `ImWebSocketHandshakeInterceptor`

而是：

- `ImWebSocketSessionRegistry` 接口直接出现 `WebSocketSession`
- 推送服务直接操作 Spring Session
- 清理任务直接关闭 Spring Session
- Handler 同时兼做协议层和调度层

也就是说，真正的迁移成本主要集中在“连接管理与消息收发边界”，不是“入口注册语法”。

### 3.2 当前最危险的三个耦合点

当前最值得优先处理的是：

1. `WebSocketSession` 出现在 registry 接口里
2. 推送服务直接 `session.sendMessage(...)`
3. `ImWebSocketHandler` 同时负责传输适配、协议解析和业务调度

这三点决定了：

- 业务层是否能和底层连接实现解耦
- 以后新增 Netty 时是否只需要增加 adapter
- 还是必须连推送和调度逻辑一起重写

### 3.3 如果继续在现有边界上扩功能，迁移成本会继续上升

如果后面继续在当前结构上叠加：

- 群聊
- ACK
- 已读回执
- 重试机制
- 多端同步

那么这些能力很容易继续依赖：

- Spring Session 的在线状态
- Spring Session 的发送方法
- 当前 handler 中的协议分支

这样以后切 Netty 时，就不再是“替换一层适配器”，而会变成“重写一圈业务边界”。

## 4. 分阶段重构建议

这里采用“分阶段最小改造”的思路，不做一次性大重构。

目标是：

- 先把接口边界抽出来
- 保持现有协议和行为不变
- 先支持未来单机下从 Spring 切到 Netty
- 暂时不把多机路由和分布式会话共享放进主方案

### 4.1 第一阶段：抽连接抽象

这一阶段最重要。

建议新增自己的连接抽象，例如：

- `ImSessionConnection`

建议职责：

- 暴露连接 id
- 暴露用户 id
- 判断是否在线
- 发送文本
- 关闭连接

同时把当前 registry 抽成自己的连接注册表，例如：

- `ImConnectionRegistry`

重构目标：

- `Registry` 不再对外暴露 `WebSocketSession`
- `Registry` 对外返回自己的连接抽象
- Spring WebSocket 只是连接抽象的一种实现

这一阶段做完后，业务层和推送层就不需要认识 Spring Session 了。

### 4.2 第二阶段：抽发送能力

当前最大的问题之一是推送服务直接发 Spring 消息。

这一阶段建议把发送动作统一收口。

可以有两种方式：

1. 由连接抽象自身负责 `sendText(...)`
2. 单独定义发送器，例如 `ImConnectionSender`

无论选哪种，目标都一致：

- `MessagePushServiceImpl`
- `ConversationWindowPushServiceImpl`

只依赖自己的连接发送能力，不再依赖 Spring `TextMessage` 和 `session.sendMessage(...)`

这一阶段做完后，推送层和 Spring WebSocket 的发送 API 就可以脱钩。

### 4.3 第三阶段：拆协议处理层

当前 `ImWebSocketHandler` 兼做太多事情，建议继续拆。

更合理的分层是：

- 传输适配层：接入连接、接收原始文本、回写原始文本
- 协议编解码层：原始 JSON 与 DTO/命令之间转换
- 协议调度层：根据 `heartbeat`、`send_message` 等类型做分发
- 应用层：真正执行业务逻辑

这一阶段的目标不是改协议，而是让 `ImWebSocketHandler` 退回为“Spring 适配层”。

做完后：

- 换 Netty 时主要重写适配层
- 协议分发和业务入口可以尽量复用

### 4.4 第四阶段：适配层并存

当前保留 Spring WebSocket 适配实现，未来新增 Netty 适配实现。

目标形态是：

- Spring adapter 实现连接抽象
- Netty adapter 也实现同一套连接抽象
- 上层业务层、推送层、会话层不关心底层到底是 Spring 还是 Netty

只有做到这一步，未来“切换到 Netty”才更接近“替换底层实现”，而不是“大规模重写”。

## 5. 未来要保留与替换的边界

### 5.1 建议保留的业务边界

这些可以尽量不动：

- 消息应用服务
- 会话窗口推送语义
- 心跳协议
- 出站 DTO 格式
- 当前 IM 消息类型定义

换句话说，底层实现可以变，但上层“系统怎么理解一条心跳、一条消息、一条会话更新”可以先保持不变。

### 5.2 建议抽象出来的边界

这些是未来切 Netty 前必须先收口的：

- 会话连接对象
- 会话注册表
- 文本发送能力
- 连接关闭语义

如果这四个边界还继续直接使用 Spring 类型，那么以后大概率还会痛一次。

### 5.3 暂时不处理的范围

这份建议里暂时不把下面这些一起做：

- 多机路由
- 分布式 Session 共享
- Netty 的具体实现细节
- 协议格式大改

原因不是它们不重要，而是现在最核心的问题是“先把单机下的传输实现边界抽出来”。

## 6. 代表性位置说明

为了后续自己改造时有抓手，这里列出最值得反复回看的代表位置：

### 6.1 `ImWebSocketHandler`

文件：

- `src/main/java/com/bilibili/im/websocket/handler/ImWebSocketHandler.java`

这里主要体现：

- Spring 传输生命周期耦合
- 协议解析与业务调度耦合
- 部分回包逻辑和 Session 使用耦合

### 6.2 `ImWebSocketSessionRegistry`

文件：

- `src/main/java/com/bilibili/im/websocket/session/ImWebSocketSessionRegistry.java`

这里主要体现：

- Registry 接口直接暴露 `WebSocketSession`
- 是目前最核心的技术边界耦合点

### 6.3 `MessagePushServiceImpl`

文件：

- `src/main/java/com/bilibili/im/websocket/service/impl/MessagePushServiceImpl.java`

这里主要体现：

- 出站推送直接操作 Spring Session
- 推送能力还没抽象成平台无关的发送接口

### 6.4 次级参考位置

如果后面继续细化，可以补看：

- `src/main/java/com/bilibili/im/websocket/service/impl/ConversationWindowPushServiceImpl.java`
- `src/main/java/com/bilibili/im/websocket/session/impl/InMemoryImWebSocketSessionRegistry.java`
- `src/main/java/com/bilibili/im/websocket/task/ImWebSocketHeartbeatCleanupTask.java`

它们分别代表：

- 会话窗口推送的同类耦合
- Session 持有方式与活跃时间管理
- 心跳超时清理与关闭语义耦合

## 7. 后续重构完成后的验证项

这份文档本身不需要测试，但未来如果开始做这轮解耦，建议至少验证下面这些点：

- Spring WebSocket 现有握手、心跳、发消息行为保持不变
- 推送服务不再直接依赖 `WebSocketSession`
- SessionRegistry 对外接口不再暴露 Spring 类型
- 心跳清理不再依赖 Spring Session 作为唯一连接载体
- 新增 Netty 适配时，不需要改消息应用服务和推送业务服务

## 8. 总结

当前这套 WebSocket 实现并不是完全不可迁移，但它的问题也已经比较清楚：

- 入口层使用 Spring WebSocket 没关系
- 真正需要优先处理的是连接管理和推送发送边界

如果现在先把：

- 连接抽象
- 注册表抽象
- 发送抽象
- 协议调度层

这几层收好，那么以后切 Netty 的动作就会从“大改业务链路”降成“补一个新的传输适配实现”。

这也是当前阶段最值得做、风险最低的一条路。
