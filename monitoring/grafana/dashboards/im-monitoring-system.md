# IM 监控系统自身

对应仪表盘文件：`im-monitoring-system.json`

## 监控范围

这个页面用于判断监控系统本身是否正常。

监控对象：

- Prometheus
- Grafana

## 采集接口

Prometheus 自身指标：

```yaml
job_name: prometheus
targets:
  - prometheus:9090
```

Grafana 自身指标：

```yaml
job_name: grafana
metrics_path: /metrics
targets:
  - grafana:3000
```

本机调试入口：

- Prometheus UI：`http://localhost:19090`
- Prometheus metrics：`http://localhost:19090/metrics`
- Grafana UI：`http://localhost:3000`
- Grafana metrics：`http://localhost:3000/metrics`

## 图表说明

| 图表 | 监控对象 | 指标来源 | 含义 |
| --- | --- | --- | --- |
| Prometheus 与 Grafana 自身 | Prometheus、Grafana | `up`、`prometheus_tsdb_head_samples_appended_total`、`grafana_stat_active_users` | 监控目标是否存活、Prometheus 写入速率、Grafana 活跃用户数 |

## 注意事项

如果这个页面的 `up{job=~"prometheus|grafana"}` 异常，说明监控系统本身可能有问题，其他业务页面的数据可信度也要一起检查。
