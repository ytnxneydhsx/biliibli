# Bilibili 监控栈

本目录包含一套面向现有 Spring Boot 项目的独立、非侵入式监控栈。

它不修改 Java 业务代码，也不要求修改 `bilibili_SpringBoot/docker-compose.yml`。

## 包含组件

- Prometheus
- Grafana
- cAdvisor
- node-exporter
- redis-exporter
- mysqld-exporter

## 启动顺序

先启动业务栈：

```bash
cd ../bilibili_SpringBoot
docker compose up -d
```

再启动监控栈：

```bash
cd ../monitoring
cp .env.example .env
cp mysql/.my.cnf.example mysql/.my.cnf
docker compose up -d
```

## Docker 网络

监控栈会加入现有业务 Docker 网络。

查看业务网络名：

```bash
docker network ls
```

本仓库默认网络名通常类似：

```text
bilibili_springboot_default
```

如果你的网络名不同，修改：

```dotenv
BUSINESS_DOCKER_NETWORK=your_network_name
```

## MySQL Exporter 账号

创建专用 MySQL 监控账号：

```bash
docker exec -it bilibili-mysql mysql -uroot -proot
```

执行：

```sql
CREATE USER IF NOT EXISTS 'exporter'@'%' IDENTIFIED BY 'exporter_password';
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'%';
FLUSH PRIVILEGES;
```

如果你使用了不同密码，同步修改 `monitoring/mysql/.my.cnf`。

## RabbitMQ 指标

RabbitMQ 指标需要启用 `rabbitmq_prometheus` 插件：

```bash
docker exec bilibili-rabbitmq rabbitmq-plugins enable rabbitmq_prometheus
```

插件启用前，Prometheus 中的 RabbitMQ target 会显示为 `down`。

## URLs

- Prometheus: <http://localhost:9090>
- Grafana: <http://localhost:3000>
- cAdvisor: <http://localhost:8081>

Grafana 默认登录账号来自 `.env`：

```text
admin / admin
```

## 验证

打开 Prometheus targets 页面：

```text
http://localhost:9090/targets
```

预期 up 的 targets：

- `spring-boot-app`
- `redis`
- `mysql`
- `cadvisor`
- `node`
- 启用 RabbitMQ 插件后的 `rabbitmq`

打开 Grafana：

```text
http://localhost:3000
```

预期：

- Prometheus 数据源自动完成 provisioning。
- `IM Minimal Overview` dashboard 出现在 `Bilibili` 文件夹下。
- 至少 app 容器 CPU/内存，以及 Redis 或 MySQL 指标能显示实时数据。

## 排查问题

### Docker 网络不存在

运行：

```bash
docker network ls
```

把 `.env` 中的 `BUSINESS_DOCKER_NETWORK` 设置为真实业务网络名。

### Spring Boot target 为 down

检查业务 app：

```bash
docker ps --filter name=bilibili-app
curl http://localhost:8080/actuator/prometheus
```

如果宿主机没有暴露本地端口，从 Prometheus 容器内部检查：

```bash
docker exec bilibili-prometheus wget -qO- http://app:8080/actuator/prometheus
```

### MySQL target 为 down

检查 exporter 日志：

```bash
docker logs bilibili-mysqld-exporter
```

确认 `monitoring/mysql/.my.cnf` 与 MySQL exporter 账号一致。

### Redis target 为 down

检查 exporter 日志：

```bash
docker logs bilibili-redis-exporter
```

确认 Redis 可以从业务 Docker 网络访问。

### Grafana dashboard 面板为空

先打开 Prometheus targets 页面：

```text
http://localhost:9090/targets
```

底层 scrape target 为 down，或者指标尚未产生时，Dashboard 面板会为空。

## 后续合并进业务 Compose

本监控栈刻意使用标准 Compose service 写法。后续如果要合并进 `bilibili_SpringBoot/docker-compose.yml`：

- 把监控服务移动到业务 compose 文件中。
- 删除 external `business` 网络配置。
- 尽量保持相同的 Prometheus target 服务名。
- 如果配置文件位置变化，调整相对挂载路径。

这个迁移不需要修改 Java 业务代码。
