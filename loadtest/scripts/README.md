# 压测脚本目录

本目录存放 k6 压测脚本、运行封装脚本和报告生成脚本。脚本说明文档与脚本放在同一目录：

- `runners/run_im_queue_ramp.ps1` / `runners/run_im_queue_ramp.md`：IM 在线用户对阶梯压测 runner。
- `reports/generate_im_ramp_report.ps1` / `reports/generate_im_ramp_report.md`：IM 阶梯压测 Markdown 报告生成器。
- `scenarios/`：k6 场景脚本，见该目录下的 `README.md`。
- `lib/`：k6 公共函数，见该目录下的 `README.md`。
- `sql/`：数据库统计和重置辅助 SQL，见该目录下的 `README.md`。
- `templates/`：迁移到其他项目时可复用的脚本模板，见该目录下的 `README.md`。

生成的压测结果报告不放在这里，应放在对应结果目录或项目约定的结果报告目录中。
