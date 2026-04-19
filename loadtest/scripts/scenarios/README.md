# k6 场景脚本

本目录存放可直接运行的 k6 场景脚本。每个脚本的具体参数以脚本中的 `__ENV`、`options`、自定义 metric 和运行 wrapper 为准。

| 脚本 | 用途 |
|---|---|
| `public_browse.js` | 公共浏览接口压测 |
| `login_burst.js` | 登录接口突发压测 |
| `authenticated_mix.js` | 登录后混合 HTTP 流量压测 |
| `ws_handshake.js` | WebSocket 建连、心跳和连接稳定性压测 |
| `im_ws_accepted.js` | IM WebSocket accepted 路径压测 |
| `im_ws_constant_rate.js` | IM WebSocket 固定速率发送压测 |
| `im_ws_online_pairs_constant_rate.js` | IM 在线用户对固定速率压测 |

分析报告时需要主动读取实际使用的场景脚本，提取 executor、VU/iteration、阈值、自定义指标、消息模型和发送频率，不要只按文件名推断。
