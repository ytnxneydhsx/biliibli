# 监控系统改造设计

日期：2026-04-21

## 背景

当前 IM 压测复盘依赖手工采集 RabbitMQ 队列状态、应用 `/actuator/prometheus`、MySQL Performance Schema / slow log、`docker stats`、`jstack` 等材料。现有方式可以完成单次定位，但采样分散、时间线不统一，容易漏掉压测过程中的瞬时峰值，也不利于长期对比版本趋势。

本设计面向第一版监控系统，目标是先建立一个可独立开发、可独立启动、非侵入式的监控栈。第一版不修改 Java 业务代码，不新增业务 Micrometer 埋点，不修改现有 `bilibili_SpringBoot/docker-compose.yml`。

## 目标

- 在仓库根目录新增独立 `monitoring/` 工程。
- 基于 `Prometheus + Grafana` 建立统一监控入口。
- 覆盖 Spring Boot Actuator、RabbitMQ、Redis、MySQL、Docker 容器、宿主机基础指标。
- 提供一个最小 Grafana dashboard，验证 app / RabbitMQ / Redis / MySQL / 容器资源的可观测链路。
- 保持监控栈与业务栈解耦，便于和后端业务代码并行开发。
- 保持后续可合并进业务 `docker-compose.yml` 的结构，避免未来迁移成本过高。

## 非目标

- 第一版不补充 Java 业务埋点。
- 第一版不设计业务指标契约文档。
- 第一版不做告警规则。
- 第一版不做完整 dashboard 美化。
- 第一版不替换现有压测脚本，压测原始文件仍作为补充证据保留。
- 第一版不改现有业务 compose 文件。

## 架构边界

监控栈作为旁路工程落在仓库根目录，与业务工程平级：

```text
bilibili_SpringBoot/
loadtest/
monitoring/
```

启动顺序：

```text
1. 先启动 bilibili_SpringBoot/docker-compose.yml
2. 再启动 monitoring/docker-compose.yml
```

`monitoring/docker-compose.yml` 通过外部 Docker 网络加入现有业务 compose 的默认网络。业务网络名通过环境变量配置：

```text
BUSINESS_DOCKER_NETWORK=bilibili_springboot_default
```

Prometheus 通过业务容器服务名访问指标源：

```text
app:8080
mysql:3306
redis:6379
rabbitmq:15692
```

如果后续决定把监控服务合并进业务 compose，主要改动应限定在网络配置和挂载路径上。服务定义、Prometheus scrape job、Grafana provisioning 文件应尽量保持可搬迁。

## 组件

第一版 `monitoring/` 包含：

```text
monitoring/
  docker-compose.yml
  .env.example
  prometheus/
    prometheus.yml
  grafana/
    provisioning/
      datasources/
        prometheus.yml
      dashboards/
        dashboards.yml
    dashboards/
      im-minimal-overview.json
  README.md
```

组件职责：

- `prometheus`：统一抓取各类 `/metrics`。
- `grafana`：读取 Prometheus 数据并展示最小 IM 总览 dashboard。
- `cadvisor`：采集 Docker 容器 CPU、内存、网络、Block IO。
- `node-exporter`：采集宿主机 CPU、内存、load、磁盘等基础资源。
- `redis-exporter`：连接现有 Redis，导出 Redis ops、内存、client、keyspace、commandstats。
- `mysqld-exporter`：连接现有 MySQL，导出连接、查询、InnoDB、锁等待等指标。
- `RabbitMQ Prometheus metrics`：优先使用 RabbitMQ 自带 `rabbitmq_prometheus` 插件暴露队列和 broker 指标。

## 数据流

数据流为旁路采集：

```text
Spring Boot /actuator/prometheus
RabbitMQ /metrics
Redis exporter /metrics
MySQL exporter /metrics
cAdvisor /metrics
node-exporter /metrics
        ↓
Prometheus
        ↓
Grafana
```

Prometheus scrape jobs：

```text
spring-boot-app -> app:8080/actuator/prometheus
rabbitmq -> rabbitmq:15692/metrics
redis -> redis-exporter:9121/metrics
mysql -> mysqld-exporter:9104/metrics
cadvisor -> cadvisor:8080/metrics
node -> node-exporter:9100/metrics
```

Redis 和 MySQL exporter 不修改 Redis/MySQL 服务本身。它们作为独立容器连接现有中间件，再把采集结果暴露给 Prometheus。

RabbitMQ 当前业务镜像为 `rabbitmq:3.13-management`。第一版不修改业务 compose，因此 README 需要提供一次性命令启用插件：

```bash
docker exec bilibili-rabbitmq rabbitmq-plugins enable rabbitmq_prometheus
```

启用后 Prometheus 才能抓取 `rabbitmq:15692/metrics`。如果用户不启用插件，其他指标仍可运行，RabbitMQ target 会显示 down。

## 配置

`monitoring/.env.example` 提供默认配置模板：

```text
BUSINESS_DOCKER_NETWORK=bilibili_springboot_default
MYSQL_EXPORTER_USER=exporter
MYSQL_EXPORTER_PASSWORD=exporter_password
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=admin
```

`monitoring/docker-compose.yml` 使用外部网络：

```text
networks:
  business:
    external: true
    name: ${BUSINESS_DOCKER_NETWORK}
```

MySQL exporter 使用独立账号连接 MySQL。README 需要给出创建最小权限账号的 SQL。该账号只用于读取监控所需状态和性能指标，不用于业务读写。

## 最小 Dashboard

第一版 dashboard 名称为 `IM Minimal Overview`，只验证基础观测链路，不追求完整分析能力。

面板范围：

- Spring Boot target 是否可抓取。
- `bilibili-app` 容器 CPU / 内存。
- RabbitMQ 核心队列 ready / unacked。
- Redis ops/sec / 内存。
- MySQL 连接数 / 查询吞吐。
- 容器级基础资源趋势。

如果某类 exporter 未成功接入，对应面板可以为空，但不能影响 Grafana 启动和其他面板展示。

## 错误处理

- Docker 网络不存在：`docker compose up` 失败；README 说明使用 `docker network ls` 查网络名并设置 `BUSINESS_DOCKER_NETWORK`。
- Spring Boot 抓不到：Prometheus targets 中 `spring-boot-app` 显示 down；README 提醒检查业务 app 是否启动，以及 `/actuator/prometheus` 是否可访问。
- RabbitMQ 抓不到：Prometheus targets 中 `rabbitmq` 显示 down；README 提醒启用 `rabbitmq_prometheus` 插件。
- MySQL exporter 失败：通过 `mysqld-exporter` 容器日志定位认证或连接错误；README 提供 exporter 账号 SQL。
- Redis exporter 失败：检查 Redis 地址、密码和 Docker 网络连通性。
- Grafana 没有图：先检查 Prometheus targets 是否 up，再检查 dashboard 查询是否有数据。

## 验证标准

第一版通过以下手工验证：

- `monitoring/docker-compose.yml` 能启动。
- Prometheus targets 至少看到 `spring-boot-app`、`redis`、`mysql`、`cadvisor`、`node` up。
- 启用 RabbitMQ 插件后，`rabbitmq` target up。
- Grafana 能登录。
- Grafana 自动加载 Prometheus 数据源。
- Grafana 自动加载 `IM Minimal Overview` dashboard。
- Dashboard 至少能看到 app 容器 CPU/内存，以及 Redis 或 MySQL 任一组实时数据。

因为第一版不改 Java 业务代码，不要求新增或调整后端单元测试。

## 后续扩展

后续如果观测结果显示业务链路细节不足，再按真实瓶颈补充 Java Micrometer 埋点，例如窗口缓存 baseline hit/miss、Redis Lua 投影耗时、WebSocket push 细分耗时等。业务埋点补强作为独立后续工作，不纳入第一版监控栈交付范围。
