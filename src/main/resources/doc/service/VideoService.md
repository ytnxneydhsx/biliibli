# 视频服务拆分方案 (Service Split Strategy)

**页面对应**：`视频详情页.md`, `首页与列表页.md`  
**阶段口径**：V1 先走 MySQL 主链路，接口结构稳定后再做缓存增强。

---

## 1. 静态基础信息 (Static Base)
> **特征**：高复用、低频变化。  
> **V1 查询策略**：直接查 MySQL。

| 本次查询字段    | 来源表        | 建议查询方式 |
| :-------------- | :------------ | :----------- |
| `id`            | `t_video`     | `selectById` |
| `title`         | `t_video`     | `selectById` |
| `videoUrl`      | `t_video`     | `selectById` |
| `coverUrl`      | `t_video`     | `selectById` |
| `desc`          | `t_video`     | `selectById` |
| `author.uid`    | `t_video`     | `from t_video.user_id` |
| `author.name`   | `t_user_info` | `join by user_id` |
| `author.avatar` | `t_user_info` | `join by user_id` |
| `tags`          | `t_video_tag` + `t_tag` | `join` |

---

## 2. 动态统计计数 (Dynamic Stats)
> **特征**：变化快，但 V1 先保证正确性。  
> **V1 查询策略**：实时查 MySQL。

| 本次查询字段   | 来源表      | 建议查询方式 |
| :------------- | :---------- | :----------- |
| `viewCount`    | `t_video`   | `selectById` |
| `likeCount`    | `t_video`   | `selectById` |
| `danmakuCount` | `t_danmaku` | `count(*) where video_id=? and status=0` |
| `replyCount`   | `t_comment` | `count(*) where video_id=?` |

---

## 3. 用户个性化交互 (User Interaction)
> **特征**：因人而异，必须依赖 `currentUserId`。  
> **V1 查询策略**：实时查 MySQL。

| 本次查询字段 | 来源表         | 建议查询方式 |
| :----------- | :------------- | :----------- |
| `isLiked`    | `t_video_like` | `exists(video_id=?, user_id=?, status=0)` |
| `isFollowed` | `t_following`  | `exists(user_id=me, following_user_id=upId, status=0)` |

---

## 4. 投稿与管理 (Content Management)
**页面对应**：`投稿与创作页.md`、`后台管理页.md`

| 操作           | 涉及字段/表                      | V1 说明 |
| :------------- | :------------------------------- | :------ |
| **上传视频**   | `file` + `t_video.video_url`     | 先用本地目录存储，路径可配置 |
| **上传封面**   | `cover` + `t_video.cover_url`    | 先用本地目录存储，路径可配置 |
| **元数据存储** | `title`, `description`, `duration` | 入库到 `t_video` |
| **标签关联**   | `t_tag`, `t_video_tag`           | 维护标签字典与视频标签关系 |
| **状态管理**   | `t_video.status`                 | `0:已发布, 1:已下架`（V1） |

---

## 5. V2 可选增强

- Redis 缓存热门视频、播放/点赞计数。
- 审核流状态扩展（如“审核中/驳回”）在后续分支引入，不影响 V1 字段结构。
