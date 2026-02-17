# 互动服务拆分方案 (Service Split Strategy)

**页面对应**：`视频详情页.md` (评论区)、`搜索页.md`  
**阶段口径**：V1 优先 MySQL 闭环，先把点赞/评论/弹幕流程稳定跑通。

---

## 1. 评论/弹幕内容流 (Content Stream)
> **特征**：列表读取为主，支持分页。  
> **V1 查询策略**：先查评论/弹幕，再批量补齐用户信息。

| 步骤       | 操作         | 说明 |
| :--------- | :----------- | :--- |
| **Step 1** | 分页查评论   | `select * from t_comment where video_id=? order by create_time desc limit ?,?` |
| **Step 2** | 提取 `userIds` | 汇总当前页作者 ID |
| **Step 3** | 批量查用户信息 | 查询 `t_user_info` 填充头像与昵称 |

---

## 2. 评论交互状态 (Comment Interaction)
> **特征**：一页评论需要批量判定“我是否点赞”。  
> **V1 查询策略**：批量查 `t_comment_like`，避免 N+1 查询。

| 本次查询字段 | 来源表           | 查询方式 |
| :----------- | :--------------- | :------- |
| `isLiked`    | `t_comment_like` | `select comment_id from t_comment_like where user_id=? and status=0 and comment_id in (...)` |
| `likeCount`  | `t_comment`      | 评论主查询时直接返回 |

---

## 3. 写入操作 (V1)
> **特征**：先保证正确性与可维护性，再做高并发增强。

| 操作       | V1 建议策略 |
| :--------- | :---------- |
| **点赞**   | `t_video_like/t_comment_like/t_danmaku_like` 用唯一索引防重，`status` 做点赞/取消切换 |
| **发评论** | 写入 `t_comment`，`parent_id` 支持楼中楼 |
| **发弹幕** | 直接写 `t_danmaku`，按 `show_time` 排序读取 |

---

## 4. V2 可选增强

- Redis 热数据计数（播放/点赞）与热点缓存。
- MQ/异步回写等高并发链路优化。
