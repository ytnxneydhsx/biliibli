# k6 独立压测项目

这个 `loadtest/` 目录现在是独立项目。

它不依赖 `bilibili_SpringBoot` 的源码结构，也不会去读取后端代码。它和业务系统只通过两件事连接：

- `BASE_URL`
- 可选的数据文件 `data/user_accounts.json`

所以以后你可以直接把整个 `loadtest/` 目录复制到别的仓库里继续用。

## 目录说明

- `docker-compose.yml`：用 Docker 跑 `k6`
- `scripts/lib/common.js`：通用请求、阈值、休眠、环境变量解析
- `scripts/scenarios/`：当前仓库可直接使用的场景脚本
- `scripts/templates/http_smoke_template.js`：迁移到其他项目时的最小模板
- `data/`：放测试账号文件，不进 Git
- `results/`：放 `k6` 结果，不进 Git

## 设计原则

- 压测项目独立存在
- 业务系统只作为“被压目标”
- 换项目时优先改 `BASE_URL` 和场景脚本，不改压测框架本身
- 账号、种子数据、压测结果都放在 `loadtest/` 自己目录里

## 如何迁移到其他项目

把整个 `loadtest/` 目录复制过去，然后做这几件事：

1. 修改 `loadtest/.env`
2. 修改或新增 `loadtest/scripts/scenarios/*.js`
3. 如果有登录态，准备 `loadtest/data/user_accounts.json`
4. 直接运行 `docker compose -f loadtest/docker-compose.yml run --rm ...`

如果是新项目，最先参考：

- `loadtest/scripts/lib/common.js`
- `loadtest/scripts/templates/http_smoke_template.js`

## 当前仓库怎么用

你这个仓库当前对外暴露的是：

`http://<服务机IP>:8080`

不要在压测机上把 `BASE_URL` 写成 `http://127.0.0.1:8080`，那只会打到压测机自己。

### 第一步：准备目标环境

先确保目标服务已经能被别的电脑访问：

1. 服务机已经启动当前项目
2. `8080` 端口对压测机可达
3. 防火墙或云安全组已经放行

### 第二步：准备压测数据

如果只压公共接口，这一步可以跳过。

如果要跑登录态场景，建议先造一批专门给压测用的数据，不要直接拿真实用户数据。

当前仓库里已经有一个造数工具，这部分只是“当前项目的辅助工具”，不是 `loadtest/` 的依赖：

```bash
python3 bilibili_SpringBoot/tools/simulator/seed_baseline_data.py \
  --base-url http://<服务机IP>:8080 \
  --media-base-url http://<服务机IP>:8080/media \
  --user-count 200 \
  --video-count 100 \
  --output-dir bilibili_SpringBoot/tools/simulator/output
```

然后把账号文件复制到压测目录：

```bash
cp bilibili_SpringBoot/tools/simulator/output/user_accounts.json loadtest/data/user_accounts.json
```

### 第三步：配置压测参数

先复制环境变量模板：

```bash
cp loadtest/.env.example loadtest/.env
```

重点改这几个值：

- `BASE_URL`：改成目标服务地址，例如 `http://192.168.1.20:8080`
- `K6_STAGES`：压测阶段，格式是 `时长:目标VU,时长:目标VU`
- `HTTP_P95_MS`：你希望接口 `p95` 控制在多少毫秒

如果要跑 WebSocket 握手场景，还可以额外设置：

- `WS_BASE_URL`：可选，直接写成 `ws://` 或 `wss://` 地址
- `WS_PATH`：默认 `/ws/im`
- `WS_TOKEN`：可选，若提供则所有 VU 共用这个 token，不再走登录
- `WS_SESSION_DURATION_MS`：每个连接保持多久，默认 `15000`
- `WS_HEARTBEAT_INTERVAL_MS`：心跳间隔，默认 `5000`，填 `0` 表示只测握手不发心跳
- `WS_P95_CONNECT_MS`：握手耗时 `p95` 阈值，默认 `1000`

示例：

```env
BASE_URL=http://192.168.1.20:8080
K6_STAGES=30s:10,2m:50,30s:0
HTTP_P95_MS=1200
```

登录态 HTTP 压测脚本固定读取：

`loadtest/data/user_accounts.json`

如果没有显式配置 `WS_BASE_URL`，脚本会按下面的规则自动推导：

- `http://host:8080` -> `ws://host:8080/ws/im`
- `https://host` -> `wss://host/ws/im`

### 第四步：开始跑

公共接口压测：

```bash
docker compose -f loadtest/docker-compose.yml run --rm \
  k6 run --summary-export /work/results/public-summary.json \
  scripts/scenarios/public_browse.js
```

登录接口压测：

```bash
docker compose -f loadtest/docker-compose.yml run --rm \
  k6 run --summary-export /work/results/login-summary.json \
  scripts/scenarios/login_burst.js
```

登录后混合流量压测：

```bash
docker compose -f loadtest/docker-compose.yml run --rm \
  k6 run --summary-export /work/results/auth-summary.json \
  scripts/scenarios/authenticated_mix.js
```

WebSocket 握手与心跳压测：

```bash
docker compose -f loadtest/docker-compose.yml run --rm \
  k6 run --summary-export /work/results/ws-handshake-summary.json \
  scripts/scenarios/ws_handshake.js
```

如果只是想先快速验证单 token 的建连稳定性，可以在 `loadtest/.env` 里先填：

```env
WS_TOKEN=你的登录token
K6_STAGES=30s:10,1m:50,30s:0
WS_SESSION_DURATION_MS=10000
WS_HEARTBEAT_INTERVAL_MS=5000
```

如果要更贴近真实在线用户，建议准备 `loadtest/data/ws_accounts.json`，让每个 VU 先登录再发起握手。

## 推荐顺序

1. 先用 `public_browse.js` 跑通链路
2. 再用 `login_burst.js` 单压登录
3. 再用 `ws_handshake.js` 单独看 WebSocket 建连、心跳和稳定连接数
4. 最后跑 `authenticated_mix.js` 做更接近真实用户行为的混合流量
5. 每次只提高一档并发，观察应用日志、MySQL、Redis、RabbitMQ、MinIO、Nginx

## 常见注意点

- 压测机和服务机不要写成同一台，避免资源互相影响
- 压测尽量打测试环境，不要直接打生产
- 如果要压“真实入口”，就打 `nginx` 暴露的 `8080`
- 如果要单独看 Spring Boot 本体能力，再额外把 `app` 端口映射出来单独压
- `loadtest/data/user_accounts.json` 不要提交到 Git
