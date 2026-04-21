# 监控系统实施计划

> **给 agentic workers：** 必须使用子技能 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务执行本计划。每个步骤都使用 checkbox（`- [ ]`）语法，便于执行时逐项勾选。

**目标：** 新增一个独立、非侵入式的 `monitoring/` 监控栈，在不修改 Java 业务代码、不修改现有业务 compose 文件的前提下，观测现有 Spring Boot、RabbitMQ、Redis、MySQL、Docker 容器和宿主机指标。

**架构：** 在仓库根目录新增 `monitoring/`，作为独立 Docker Compose 工程。监控 compose 通过 `BUSINESS_DOCKER_NETWORK` 加入现有业务 Docker 网络，运行 Prometheus、Grafana 和各类 exporter，自动加载最小 Grafana dashboard，并提供启动与验证文档。目录和服务写法要保持可迁移，后续合并进业务 compose 时应主要只改网络和挂载路径。

**技术栈：** Docker Compose、Prometheus、Grafana provisioning、cAdvisor、node-exporter、redis-exporter、mysqld-exporter、RabbitMQ Prometheus plugin。

---

## 并行开发模型

使用以下独立工作线降低合并冲突：

- **工作线 A - Compose 与环境变量：** 只负责 `monitoring/docker-compose.yml`、`monitoring/.env.example`、`monitoring/.gitignore` 和 `monitoring/mysql/.my.cnf.example`。
- **工作线 B - Prometheus：** 只负责 `monitoring/prometheus/prometheus.yml`。
- **工作线 C - Grafana：** 只负责 `monitoring/grafana/provisioning/**` 和 `monitoring/grafana/dashboards/im-minimal-overview.json`。
- **工作线 D - 文档：** 只负责 `monitoring/README.md`。
- **工作线 E - 集成验证：** 默认不负责源码文件。除非验证发现明确配置不一致，否则不要直接改文件；修复应回到对应工作线负责的文件。

`monitoring/` 目录建立后，工作线 A、B、C、D 可以并行启动。工作线 E 在 A-D 完成后执行。

## 工作线接口契约

工作线 A、B、C 可以并行开发，但必须遵守下面这些固定接口。任何一条工作线需要修改这些接口时，必须同步通知其他工作线并更新本节。

### A 和 B 的接口：Compose 服务名与 Prometheus targets

工作线 A 在 `monitoring/docker-compose.yml` 中必须提供以下服务名和容器内端口，工作线 B 的 `prometheus.yml` 只能按这些名字抓取：

| 服务 | A 提供的 Compose service 名 | B 使用的 Prometheus target | metrics 路径 |
| --- | --- | --- | --- |
| Prometheus | `prometheus` | `prometheus:9090` | `/metrics` |
| Grafana | `grafana` | `grafana:3000` | `/metrics` |
| Spring Boot 业务 app | 业务网络中的 `app` | `app:8080` | `/actuator/prometheus` |
| RabbitMQ | 业务网络中的 `rabbitmq` | `rabbitmq:15692` | `/metrics` |
| Redis exporter | `redis-exporter` | `redis-exporter:9121` | `/metrics` |
| MySQL exporter | `mysqld-exporter` | `mysqld-exporter:9104` | `/metrics` |
| cAdvisor | `cadvisor` | `cadvisor:8080` | `/metrics` |
| node-exporter | `node-exporter` | `node-exporter:9100` | `/metrics` |

工作线 A 可以把这些服务额外映射到宿主机端口用于调试，但不能改变容器内端口和 Compose service 名，否则 B 的 scrape 配置会失效。

### A 和 C 的接口：Grafana 挂载路径

工作线 A 在 `grafana` 服务中必须挂载：

```yaml
volumes:
  - ./grafana/provisioning:/etc/grafana/provisioning:ro
  - ./grafana/dashboards:/var/lib/grafana/dashboards:ro
```

工作线 C 必须把 provisioning 文件和 dashboard JSON 写到这些目录下：

```text
monitoring/grafana/provisioning/datasources/prometheus.yml
monitoring/grafana/provisioning/dashboards/dashboards.yml
monitoring/grafana/dashboards/im-minimal-overview.json
```

如果 A 修改 Grafana 容器内挂载路径，C 的 provisioning 会失效；如果 C 修改文件目录，A 的挂载路径会找不到 dashboard。

### B 和 C 的接口：Grafana datasource uid

工作线 C 的 datasource provisioning 必须使用固定 uid：

```yaml
uid: Prometheus
```

Dashboard JSON 中所有 Prometheus panel 也必须引用同一个 uid：

```json
"datasource": {
  "type": "prometheus",
  "uid": "Prometheus"
}
```

这个 uid 是 B/C 的集成接口。C 不应使用 Grafana 自动生成 uid，也不应把 datasource uid 改成小写或其他名称。

### A 和 D 的接口：环境变量与启动说明

工作线 A 的 `.env.example` 至少必须包含：

```dotenv
BUSINESS_DOCKER_NETWORK=bilibili_springboot_default
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=admin
GRAFANA_PORT=3000
PROMETHEUS_PORT=9090
REDIS_ADDR=redis://redis:6379
REDIS_PASSWORD=
MYSQL_EXPORTER_USER=exporter
MYSQL_EXPORTER_PASSWORD=exporter_password
```

工作线 D 的 README 必须以这些变量名为准，不能另起一套变量名。README 中的启动命令也必须以 A 提供的目录结构为准：

```bash
cp .env.example .env
cp mysql/.my.cnf.example mysql/.my.cnf
docker compose up -d
```

### B 和 D 的接口：Prometheus job 名称

README 和验证步骤中提到的 Prometheus targets 必须与 B 的 `job_name` 保持一致：

```text
prometheus
grafana
spring-boot-app
rabbitmq
redis
mysql
cadvisor
node
```

### C 和 D 的接口：Dashboard 名称与位置

README 和验证步骤中提到的 dashboard 必须与 C 的 JSON 保持一致：

```text
Grafana folder: Bilibili
Dashboard title: IM Minimal Overview
Dashboard file: monitoring/grafana/dashboards/im-minimal-overview.json
```

### 并行合并检查

工作线 A/B/C/D 合并后，工作线 E 必须先检查这些接口再启动容器：

```bash
rg -n "uid: Prometheus" monitoring/grafana/provisioning/datasources/prometheus.yml
rg -n '"uid": "Prometheus"' monitoring/grafana/dashboards/im-minimal-overview.json
rg -n "spring-boot-app|rabbitmq|redis|mysql|cadvisor|node" monitoring/prometheus/prometheus.yml
docker compose -f monitoring/docker-compose.yml config
```

## 文件结构

创建以下文件：

```text
monitoring/
  .env.example
  .gitignore
  docker-compose.yml
  README.md
  mysql/
    .my.cnf.example
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
```

职责划分：

- `monitoring/docker-compose.yml`：声明 Prometheus、Grafana、cAdvisor、node-exporter、redis-exporter、mysqld-exporter 服务，并以 external network 方式加入现有业务 Docker 网络。
- `monitoring/.env.example`：记录可配置的业务网络、Grafana、MySQL exporter、Redis exporter 和端口设置。
- `monitoring/prometheus/prometheus.yml`：定义 app Actuator、RabbitMQ metrics、Redis exporter、MySQL exporter、cAdvisor、node-exporter、Prometheus 自身和 Grafana 的 scrape jobs。
- `monitoring/grafana/provisioning/datasources/prometheus.yml`：自动把 Prometheus 配置成 Grafana 默认数据源。
- `monitoring/grafana/provisioning/dashboards/dashboards.yml`：自动从 `/var/lib/grafana/dashboards` 加载 JSON dashboards。
- `monitoring/grafana/dashboards/im-minimal-overview.json`：最小 dashboard，用于展示 app/container、RabbitMQ 队列、Redis、MySQL 的基础状态。
- `monitoring/README.md`：说明启动流程、业务网络配置、RabbitMQ 插件命令、MySQL exporter 账号 SQL、验证方式和后续迁移注意事项。

### 任务 1：Compose 基础结构

**文件：**
- 创建：`monitoring/.env.example`
- 创建：`monitoring/.gitignore`
- 创建：`monitoring/docker-compose.yml`
- 创建：`monitoring/mysql/.my.cnf.example`

- [ ] **步骤 1：创建监控环境变量模板**

创建 `monitoring/.env.example`，内容如下：

```dotenv
# 现有业务 Docker Compose 网络。
# 使用 docker network ls 查看。
BUSINESS_DOCKER_NETWORK=bilibili_springboot_default

# 本地监控使用的 Grafana 登录账号。
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=admin
GRAFANA_PORT=3000

# Prometheus 宿主机端口。
PROMETHEUS_PORT=9090

# Exporter 宿主机端口。暴露端口方便本地调试；Prometheus 使用容器 DNS 访问。
REDIS_EXPORTER_PORT=9121
MYSQLD_EXPORTER_PORT=9104
CADVISOR_PORT=8081
NODE_EXPORTER_PORT=9100

# 现有 Redis 服务连接。Redis 无密码时 REDIS_PASSWORD 保持为空。
REDIS_ADDR=redis://redis:6379
REDIS_PASSWORD=

# 现有 MySQL exporter 账号。
MYSQL_EXPORTER_USER=exporter
MYSQL_EXPORTER_PASSWORD=exporter_password
MYSQL_EXPORTER_HOST=mysql
MYSQL_EXPORTER_PORT=3306
MYSQL_EXPORTER_DATABASE=bilibili
```

- [ ] **步骤 2：创建独立 Docker Compose 文件**

创建 `monitoring/docker-compose.yml`，内容如下：

```yaml
services:
  prometheus:
    image: prom/prometheus:v2.55.1
    container_name: bilibili-prometheus
    restart: unless-stopped
    command:
      - --config.file=/etc/prometheus/prometheus.yml
      - --storage.tsdb.path=/prometheus
      - --web.console.libraries=/usr/share/prometheus/console_libraries
      - --web.console.templates=/usr/share/prometheus/consoles
    ports:
      - "${PROMETHEUS_PORT:-9090}:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus_data:/prometheus
    networks:
      - monitoring
      - business

  grafana:
    image: grafana/grafana:11.3.1
    container_name: bilibili-grafana
    restart: unless-stopped
    depends_on:
      - prometheus
    environment:
      GF_SECURITY_ADMIN_USER: ${GRAFANA_ADMIN_USER:-admin}
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_ADMIN_PASSWORD:-admin}
      GF_USERS_ALLOW_SIGN_UP: "false"
    ports:
      - "${GRAFANA_PORT:-3000}:3000"
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning:ro
      - ./grafana/dashboards:/var/lib/grafana/dashboards:ro
    networks:
      - monitoring

  cadvisor:
    image: gcr.io/cadvisor/cadvisor:v0.49.1
    container_name: bilibili-cadvisor
    restart: unless-stopped
    privileged: true
    ports:
      - "${CADVISOR_PORT:-8081}:8080"
    volumes:
      - /:/rootfs:ro
      - /var/run:/var/run:ro
      - /sys:/sys:ro
      - /var/lib/docker/:/var/lib/docker:ro
      - /dev/disk/:/dev/disk:ro
    networks:
      - monitoring

  node-exporter:
    image: prom/node-exporter:v1.8.2
    container_name: bilibili-node-exporter
    restart: unless-stopped
    command:
      - --path.rootfs=/host
    pid: host
    ports:
      - "${NODE_EXPORTER_PORT:-9100}:9100"
    volumes:
      - /:/host:ro,rslave
    networks:
      - monitoring

  redis-exporter:
    image: oliver006/redis_exporter:v1.66.0
    container_name: bilibili-redis-exporter
    restart: unless-stopped
    environment:
      REDIS_ADDR: ${REDIS_ADDR:-redis://redis:6379}
      REDIS_PASSWORD: ${REDIS_PASSWORD:-}
    ports:
      - "${REDIS_EXPORTER_PORT:-9121}:9121"
    networks:
      - monitoring
      - business

  mysqld-exporter:
    image: prom/mysqld-exporter:v0.16.0
    container_name: bilibili-mysqld-exporter
    restart: unless-stopped
    command:
      - --config.my-cnf=/etc/mysqld-exporter/.my.cnf
      - --collect.global_status
      - --collect.global_variables
      - --collect.info_schema.innodb_metrics
      - --collect.info_schema.processlist
      - --collect.perf_schema.eventsstatements
      - --collect.perf_schema.eventswaits
    ports:
      - "${MYSQLD_EXPORTER_PORT:-9104}:9104"
    volumes:
      - ./mysql/.my.cnf:/etc/mysqld-exporter/.my.cnf:ro
    networks:
      - monitoring
      - business

networks:
  monitoring:
    name: bilibili-monitoring
  business:
    external: true
    name: ${BUSINESS_DOCKER_NETWORK}

volumes:
  prometheus_data:
  grafana_data:
```

- [ ] **步骤 3：创建 MySQL exporter 配置样例并忽略本地密钥文件**

创建 `monitoring/mysql/.my.cnf.example`，内容如下：

```ini
[client]
user=exporter
password=exporter_password
host=mysql
port=3306
```

创建 `monitoring/.gitignore`，内容如下：

```gitignore
mysql/.my.cnf
```

- [ ] **步骤 4：验证 Compose 语法**

运行：

```bash
cd monitoring
cp .env.example .env
cp mysql/.my.cnf.example mysql/.my.cnf
docker compose config
```

预期：

```text
命令以状态码 0 退出，并打印解析后的 compose 配置。
```

- [ ] **步骤 5：提交工作线 A**

运行：

```bash
git add monitoring/.env.example monitoring/.gitignore monitoring/docker-compose.yml monitoring/mysql/.my.cnf.example
git commit -m "feat: add standalone monitoring compose"
```

### 任务 2：Prometheus 抓取配置

**文件：**
- 创建：`monitoring/prometheus/prometheus.yml`

- [ ] **步骤 1：创建 Prometheus scrape 配置**

创建 `monitoring/prometheus/prometheus.yml`，内容如下：

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    project: bilibili
    environment: local

scrape_configs:
  - job_name: prometheus
    static_configs:
      - targets:
          - prometheus:9090

  - job_name: grafana
    metrics_path: /metrics
    static_configs:
      - targets:
          - grafana:3000

  - job_name: spring-boot-app
    metrics_path: /actuator/prometheus
    static_configs:
      - targets:
          - app:8080
        labels:
          service: bilibili-app

  - job_name: rabbitmq
    metrics_path: /metrics
    static_configs:
      - targets:
          - rabbitmq:15692
        labels:
          service: bilibili-rabbitmq

  - job_name: redis
    metrics_path: /metrics
    static_configs:
      - targets:
          - redis-exporter:9121
        labels:
          service: bilibili-redis

  - job_name: mysql
    metrics_path: /metrics
    static_configs:
      - targets:
          - mysqld-exporter:9104
        labels:
          service: bilibili-mysql

  - job_name: cadvisor
    metrics_path: /metrics
    static_configs:
      - targets:
          - cadvisor:8080

  - job_name: node
    metrics_path: /metrics
    static_configs:
      - targets:
          - node-exporter:9100
```

- [ ] **步骤 2：通过 Docker 验证 Prometheus 配置**

运行：

```bash
docker run --rm \
  -v "$PWD/monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro" \
  prom/prometheus:v2.55.1 \
  --config.file=/etc/prometheus/prometheus.yml \
  --storage.tsdb.path=/tmp/prometheus-check \
  --web.listen-address=:19090
```

预期：

```text
Prometheus 无 YAML 解析错误并成功启动。启动日志显示服务已准备接收 Web 请求后，用 Ctrl+C 停止。
```

- [ ] **步骤 3：提交工作线 B**

运行：

```bash
git add monitoring/prometheus/prometheus.yml
git commit -m "feat: add prometheus scrape config"
```

### 任务 3：Grafana Provisioning 与最小 Dashboard

**文件：**
- 创建：`monitoring/grafana/provisioning/datasources/prometheus.yml`
- 创建：`monitoring/grafana/provisioning/dashboards/dashboards.yml`
- 创建：`monitoring/grafana/dashboards/im-minimal-overview.json`

- [ ] **步骤 1：创建 Grafana 数据源 provisioning**

创建 `monitoring/grafana/provisioning/datasources/prometheus.yml`，内容如下：

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    uid: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
```

- [ ] **步骤 2：创建 Grafana dashboard provisioning**

创建 `monitoring/grafana/provisioning/dashboards/dashboards.yml`，内容如下：

```yaml
apiVersion: 1

providers:
  - name: Bilibili Monitoring
    orgId: 1
    folder: Bilibili
    type: file
    disableDeletion: false
    editable: true
    options:
      path: /var/lib/grafana/dashboards
```

- [ ] **步骤 3：创建最小总览 Dashboard JSON**

创建 `monitoring/grafana/dashboards/im-minimal-overview.json`，内容如下：

```json
{
  "annotations": {
    "list": [
      {
        "builtIn": 1,
        "datasource": {
          "type": "grafana",
          "uid": "-- Grafana --"
        },
        "enable": true,
        "hide": true,
        "iconColor": "rgba(0, 211, 255, 1)",
        "name": "Annotations & Alerts",
        "type": "dashboard"
      }
    ]
  },
  "editable": true,
  "fiscalYearStartMonth": 0,
  "graphTooltip": 0,
  "id": null,
  "links": [],
  "panels": [
    {
      "datasource": {
        "type": "prometheus",
        "uid": "Prometheus"
      },
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisBorderShow": false,
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisLabel": "",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "insertNulls": false,
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              }
            ]
          },
          "unit": "percentunit"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 0,
        "y": 0
      },
      "id": 1,
      "options": {
        "legend": {
          "calcs": [
            "lastNotNull"
          ],
          "displayMode": "table",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "single",
          "sort": "none"
        }
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "Prometheus"
          },
          "editorMode": "code",
          "expr": "sum by (name) (rate(container_cpu_usage_seconds_total{name=~\"bilibili-app|bilibili-mysql|bilibili-redis|bilibili-rabbitmq\"}[1m]))",
          "legendFormat": "{{name}}",
          "range": true,
          "refId": "A"
        }
      ],
      "title": "核心容器 CPU 使用率",
      "type": "timeseries"
    },
    {
      "datasource": {
        "type": "prometheus",
        "uid": "Prometheus"
      },
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisBorderShow": false,
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "insertNulls": false,
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              }
            ]
          },
          "unit": "bytes"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 12,
        "y": 0
      },
      "id": 2,
      "options": {
        "legend": {
          "calcs": [
            "lastNotNull"
          ],
          "displayMode": "table",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "single",
          "sort": "none"
        }
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "Prometheus"
          },
          "editorMode": "code",
          "expr": "container_memory_working_set_bytes{name=~\"bilibili-app|bilibili-mysql|bilibili-redis|bilibili-rabbitmq\"}",
          "legendFormat": "{{name}}",
          "range": true,
          "refId": "A"
        }
      ],
      "title": "核心容器内存",
      "type": "timeseries"
    },
    {
      "datasource": {
        "type": "prometheus",
        "uid": "Prometheus"
      },
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisBorderShow": false,
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "insertNulls": false,
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 0,
        "y": 8
      },
      "id": 3,
      "options": {
        "legend": {
          "calcs": [
            "lastNotNull"
          ],
          "displayMode": "table",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "single",
          "sort": "none"
        }
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "Prometheus"
          },
          "editorMode": "code",
          "expr": "rabbitmq_queue_messages_ready{queue=~\"im\\\\.message\\\\..*\"}",
          "legendFormat": "{{queue}} ready",
          "range": true,
          "refId": "A"
        },
        {
          "datasource": {
            "type": "prometheus",
            "uid": "Prometheus"
          },
          "editorMode": "code",
          "expr": "rabbitmq_queue_messages_unacked{queue=~\"im\\\\.message\\\\..*\"}",
          "legendFormat": "{{queue}} unacked",
          "range": true,
          "refId": "B"
        }
      ],
      "title": "RabbitMQ IM 队列积压",
      "type": "timeseries"
    },
    {
      "datasource": {
        "type": "prometheus",
        "uid": "Prometheus"
      },
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisBorderShow": false,
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "insertNulls": false,
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              }
            ]
          },
          "unit": "ops"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 12,
        "y": 8
      },
      "id": 4,
      "options": {
        "legend": {
          "calcs": [
            "lastNotNull"
          ],
          "displayMode": "table",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "single",
          "sort": "none"
        }
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "Prometheus"
          },
          "editorMode": "code",
          "expr": "rate(redis_commands_processed_total[1m])",
          "legendFormat": "redis commands/sec",
          "range": true,
          "refId": "A"
        }
      ],
      "title": "Redis 命令吞吐",
      "type": "timeseries"
    },
    {
      "datasource": {
        "type": "prometheus",
        "uid": "Prometheus"
      },
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisBorderShow": false,
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "insertNulls": false,
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 0,
        "y": 16
      },
      "id": 5,
      "options": {
        "legend": {
          "calcs": [
            "lastNotNull"
          ],
          "displayMode": "table",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "single",
          "sort": "none"
        }
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "Prometheus"
          },
          "editorMode": "code",
          "expr": "mysql_global_status_threads_connected",
          "legendFormat": "threads connected",
          "range": true,
          "refId": "A"
        },
        {
          "datasource": {
            "type": "prometheus",
            "uid": "Prometheus"
          },
          "editorMode": "code",
          "expr": "mysql_global_status_threads_running",
          "legendFormat": "threads running",
          "range": true,
          "refId": "B"
        }
      ],
      "title": "MySQL 连接状态",
      "type": "timeseries"
    },
    {
      "datasource": {
        "type": "prometheus",
        "uid": "Prometheus"
      },
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisBorderShow": false,
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "insertNulls": false,
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              }
            ]
          },
          "unit": "ops"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 12,
        "y": 16
      },
      "id": 6,
      "options": {
        "legend": {
          "calcs": [
            "lastNotNull"
          ],
          "displayMode": "table",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "single",
          "sort": "none"
        }
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "Prometheus"
          },
          "editorMode": "code",
          "expr": "rate(mysql_global_status_questions[1m])",
          "legendFormat": "questions/sec",
          "range": true,
          "refId": "A"
        }
      ],
      "title": "MySQL 查询吞吐",
      "type": "timeseries"
    }
  ],
  "preload": false,
  "refresh": "10s",
  "schemaVersion": 40,
  "tags": [
    "bilibili",
    "im",
    "monitoring"
  ],
  "templating": {
    "list": []
  },
  "time": {
    "from": "now-30m",
    "to": "now"
  },
  "timepicker": {},
  "timezone": "browser",
  "title": "IM Minimal Overview",
  "uid": "im-minimal-overview",
  "version": 1,
  "weekStart": ""
}
```

- [ ] **步骤 4：验证 Dashboard JSON 语法**

运行：

```bash
jq empty monitoring/grafana/dashboards/im-minimal-overview.json
```

预期：

```text
无输出，状态码为 0。
```

- [ ] **步骤 5：提交工作线 C**

运行：

```bash
git add monitoring/grafana/provisioning/datasources/prometheus.yml monitoring/grafana/provisioning/dashboards/dashboards.yml monitoring/grafana/dashboards/im-minimal-overview.json
git commit -m "feat: add grafana provisioning and minimal dashboard"
```

### 任务 4：监控 README

**文件：**
- 创建：`monitoring/README.md`

- [ ] **步骤 1：创建使用文档**

创建 `monitoring/README.md`，内容如下：

```markdown
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
```

- [ ] **步骤 2：手工检查 Markdown 链接与代码块**

运行：

```bash
sed -n '1,260p' monitoring/README.md
```

预期：

```text
文件中的 fenced code block 成对闭合，没有占位符文本。
```

- [ ] **步骤 3：提交工作线 D**

运行：

```bash
git add monitoring/README.md
git commit -m "docs: add monitoring stack guide"
```

### 任务 5：集成验证

**文件：**
- 默认不负责源码文件。如果验证发现配置不匹配，应回到对应任务负责的文件中修复。

- [ ] **步骤 1：确认工作区和已提交工作线**

运行：

```bash
git status --short --branch
git log --oneline -5
```

预期：

```text
分支中包含工作线 A-D 的提交。工作区中可能仍存在用户已有的无关改动，不能回滚这些改动。
```

- [ ] **步骤 2：验证所有监控配置文件存在**

运行：

```bash
test -f monitoring/docker-compose.yml
test -f monitoring/.env.example
test -f monitoring/.gitignore
test -f monitoring/mysql/.my.cnf.example
test -f monitoring/prometheus/prometheus.yml
test -f monitoring/grafana/provisioning/datasources/prometheus.yml
test -f monitoring/grafana/provisioning/dashboards/dashboards.yml
test -f monitoring/grafana/dashboards/im-minimal-overview.json
test -f monitoring/README.md
```

预期：

```text
所有命令均以状态码 0 退出。
```

- [ ] **步骤 3：验证 Compose 和 Dashboard 语法**

运行：

```bash
cd monitoring
cp .env.example .env
cp mysql/.my.cnf.example mysql/.my.cnf
docker compose config >/tmp/bilibili-monitoring-compose.yml
cd ..
jq empty monitoring/grafana/dashboards/im-minimal-overview.json
```

预期：

```text
`docker compose config` 状态码为 0，`jq` 状态码为 0。
```

- [ ] **步骤 4：基于运行中的业务栈验证**

运行：

```bash
cd bilibili_SpringBoot
docker compose up -d
cd ../monitoring
docker compose up -d
```

预期：

```text
监控容器成功启动。如果业务 Docker 网络名不同，修改 monitoring/.env 后重新运行 docker compose up -d。
```

- [ ] **步骤 5：RabbitMQ 运行时启用 metrics 插件**

运行：

```bash
docker exec bilibili-rabbitmq rabbitmq-plugins enable rabbitmq_prometheus
docker restart bilibili-rabbitmq
```

预期：

```text
RabbitMQ 成功重启，并在 Docker 网络内通过 rabbitmq:15692 暴露 metrics。
```

- [ ] **步骤 6：MySQL 运行时创建 exporter 账号**

运行：

```bash
docker exec -i bilibili-mysql mysql -uroot -proot <<'SQL'
CREATE USER IF NOT EXISTS 'exporter'@'%' IDENTIFIED BY 'exporter_password';
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'%';
FLUSH PRIVILEGES;
SQL
```

预期：

```text
命令状态码为 0。重启后 mysqld-exporter 日志中没有认证失败。
```

- [ ] **步骤 7：检查 Prometheus targets**

打开：

```text
http://localhost:9090/targets
```

预期：

```text
spring-boot-app、redis、mysql、cadvisor、node 为 up。启用插件后 rabbitmq 也为 up。
```

- [ ] **步骤 8：检查 Grafana provisioning**

打开：

```text
http://localhost:3000
```

预期：

```text
使用 .env 中的账号密码可以登录。Prometheus 数据源存在。Bilibili 文件夹中包含 IM Minimal Overview。
```

- [ ] **步骤 9：检查最小 Dashboard**

打开 `IM Minimal Overview` dashboard。

预期：

```text
至少容器 CPU/内存，以及 Redis 或 MySQL 面板能显示实时数据。RabbitMQ 插件启用且 IM 队列存在前，RabbitMQ 面板可以为空。
```

- [ ] **步骤 10：提交验证阶段发现的必要修正**

如果任务 5 需要修正某个已归属文件，只提交这项修正：

```bash
git add monitoring
git commit -m "fix: align monitoring verification config"
```

预期：

```text
提交中不包含任何无关用户改动。
```
