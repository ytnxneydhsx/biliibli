# 通信安全第一梯队待办

日期：2026-04-25

## 背景

当前项目已经具备本地开发和联调能力，但如果要作为可对外访问的服务运行，通信安全上还有几项高优先级问题需要先处理。

这份待办只收口第一梯队事项，后续第二梯队和内部链路安全可以再单独展开。

## 待办清单

### 1. 接入 HTTPS / WSS

- [ ] 为前台和后台 nginx 增加 TLS 终止能力
- [ ] 前台站点从 `http://` 切到 `https://`
- [ ] IM WebSocket 从 `ws://` 切到 `wss://`
- [ ] 为生产环境开启 HSTS
- [ ] 明确证书来源和续期方式（如 ACME / Let's Encrypt）

## 验收标准

- 前台和后台均只能通过 HTTPS 访问
- 浏览器访问 HTTP 时自动跳转到 HTTPS
- IM 实时通道可在 WSS 下正常握手、收发消息

### 2. 调整 Token 传输与存储方式

- [ ] HTTP 接口登录态从 `localStorage` 迁移到 `HttpOnly + Secure + SameSite` Cookie
- [ ] 前台移除 `bilibili_token` 的本地持久化
- [ ] 后台移除 `bilibili_admin_token` 的本地持久化
- [ ] WebSocket 握手移除 query string 中的 `token`
- [ ] 后端停止接受 URL query 中的 `token` 作为鉴权来源
- [ ] 评估 WebSocket 鉴权方案，优先复用 Cookie；如无法复用，再引入短期握手票据

## 验收标准

- 浏览器地址栏、日志、代理层中不再出现 WebSocket `token` 查询参数
- 前后台页面不再依赖 `localStorage` 保存长期 JWT
- 登录成功后 API 和 WebSocket 均可在 Cookie 模式下正常鉴权

### 3. 收紧 JWT 配置

- [ ] 生产环境禁止使用默认 `jwt.secret`
- [ ] 应用启动时校验 `JWT_SECRET`，缺失或使用默认值直接失败
- [ ] 将 access token 有效期从 7 天缩短到更合理范围（建议 15 分钟到 2 小时）
- [ ] 设计 refresh token 机制
- [ ] 增加 token 吊销或失效控制能力（至少支持登出和高风险场景失效）

## 验收标准

- 生产环境未配置合法 `JWT_SECRET` 时服务无法启动
- access token 过期时间符合新策略
- 刷新与吊销链路具备最小可用实现

### 4. 收缩外部暴露面

- [ ] 生产环境关闭 Swagger UI 和公开 API 文档
- [ ] 生产环境不通过公网 nginx 暴露 `/actuator/**`
- [ ] 明确监控和运维访问入口，仅允许内网或受控来源访问

## 验收标准

- 公网环境无法直接访问 `swagger-ui`、`v3/api-docs`、`/actuator/**`
- 开发和测试环境仍可按配置开启这些能力

### 5. 补齐 nginx 响应安全头

- [ ] 增加 `Strict-Transport-Security`
- [ ] 增加 `Content-Security-Policy`
- [ ] 增加 `X-Frame-Options`
- [ ] 增加 `X-Content-Type-Options`
- [ ] 增加 `Referrer-Policy`
- [ ] 按前台和后台页面实际资源来源校准 CSP，避免先写成过宽策略

## 验收标准

- 前后台首页和主要业务页面响应头包含上述安全头
- CSP 不影响现有页面正常加载与 API 调用

## 建议实施顺序

1. HTTPS / WSS
2. Token 存储与 WebSocket 鉴权改造
3. JWT 生命周期与 secret 管控
4. 暴露面收缩
5. nginx 安全头
