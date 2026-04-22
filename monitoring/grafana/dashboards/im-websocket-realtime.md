# IM WebSocket 实时链路

对应仪表盘文件：`im-websocket-realtime.json`

## 监控范围

这个页面用于观察 IM WebSocket 连接、心跳、消息处理和异常输入。

监控对象：

- Spring Boot WebSocket 连接管理
- WebSocket 握手流程
- 心跳收发
- WebSocket 协议分发和下行发送
- 非法 payload、非法类型、未支持类型、过期 session 清理

## 采集接口

这些指标由 Spring Boot 应用内的 WebSocket Micrometer 埋点暴露。

Prometheus scrape 配置：

```yaml
job_name: spring-boot-app
metrics_path: /actuator/prometheus
targets:
  - app:8080
```

本机调试入口：

- Spring Boot metrics：`http://localhost:8080/actuator/prometheus`
- WebSocket 业务入口：通过 nginx 反代到应用的 `/ws/im`

## 图表说明

| 图表 | 监控对象 | 指标来源 | 含义 |
| --- | --- | --- | --- |
| IM WebSocket 在线状态 | WebSocket 连接注册表 | `im_ws_sessions_active`、`im_ws_users_online` | 当前活跃 session 数和在线用户数 |
| IM WebSocket 握手与心跳 | WebSocket 握手、心跳 | `im_ws_handshake_attempts_total`、`im_ws_handshake_success_total`、`im_ws_heartbeat_received_total`、`im_ws_heartbeat_ack_sent_total`、`im_ws_heartbeat_ack_failed_total` | 握手尝试/成功速率、心跳收发和失败速率 |
| IM WebSocket 消息处理延迟 | WebSocket 协议处理 | `im_ws_protocol_dispatch_seconds_*`、`im_ws_outbound_send_seconds_*` | 上行协议分发和下行发送平均耗时 |
| IM WebSocket 异常输入 | WebSocket 输入校验 | `im_ws_inbound_payload_invalid_total`、`im_ws_inbound_type_invalid_total`、`im_ws_inbound_type_unsupported_total`、`im_ws_cleanup_expired_sessions_total` | 非法输入、未支持类型和过期连接清理速率 |

## 注意事项

连接和心跳类指标在 WebSocket 建连后会出现；消息处理延迟需要真实收发消息后才会生成对应时间序列。
