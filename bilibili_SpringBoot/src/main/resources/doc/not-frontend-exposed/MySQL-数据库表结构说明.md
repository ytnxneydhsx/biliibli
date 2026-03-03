# MySQL 数据库表结构说明（bilibili_SpringBoot）

本文档基于 `bilibili_SpringBoot/src/main/resources/bilibili.sql`，说明每张表的用途、字段含义与索引定义。

## 1. 总体说明

- 数据库名：`bilibili`
- 字符集：`utf8mb4`
- 引擎：`InnoDB`
- 主键类型：`BIGINT`（注释标注为 snowflake id）
- 软删除/状态位：大量表使用 `status`（常见含义：`0=normal`, `1=deleted`）
- 关系约束：SQL 中未显式定义外键，采用业务层维护“逻辑外键”关系

## 2. 表清单

- `t_user` 用户账号表
- `t_user_info` 用户资料表
- `t_video` 视频主表
- `t_danmaku` 弹幕表
- `t_danmaku_like` 弹幕点赞关系表
- `t_video_like` 视频点赞关系表
- `t_comment` 评论表
- `t_comment_like` 评论点赞关系表
- `t_following` 关注关系表
- `t_video_upload_task` 分片上传任务表
- `t_tag` 标签表
- `t_video_tag` 视频-标签关联表

---

## 3. 分表说明

## 3.1 `t_user`

作用：存储登录账号及账号级状态。

字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 主键，snowflake id |
| `username` | `VARCHAR(32)` | 登录用户名 |
| `password` | `VARCHAR(100)` | 密码哈希 |
| `status` | `TINYINT(1)` | 状态（0 正常，1 删除） |
| `create_time` | `DATETIME` | 创建时间 |
| `update_time` | `DATETIME` | 更新时间（自动更新） |
| `delete_time` | `DATETIME` | 删除时间 |

索引（摘自 `bilibili.sql`）：

```sql
PRIMARY KEY (`id`),
UNIQUE KEY `uk_username` (`username`),
KEY `idx_create_time` (`create_time`),
KEY `idx_update_time` (`update_time`)
```

## 3.2 `t_user_info`

作用：存储用户公开资料与统计信息。

字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 主键 |
| `user_id` | `BIGINT` | 关联 `t_user.id` |
| `nickname` | `VARCHAR(32)` | 昵称 |
| `avatar_url` | `VARCHAR(255)` | 头像 URL |
| `sign` | `VARCHAR(255)` | 个性签名 |
| `following_count` | `INT` | 关注数 |
| `follower_count` | `INT` | 粉丝数 |
| `create_time` | `DATETIME` | 创建时间 |
| `update_time` | `DATETIME` | 更新时间 |

索引（摘自 `bilibili.sql`）：

```sql
PRIMARY KEY (`id`),
UNIQUE KEY `uk_user_id` (`user_id`),
KEY `idx_nickname` (`nickname`),
KEY `idx_create_time` (`create_time`),
KEY `idx_update_time` (`update_time`)
```

## 3.3 `t_video`

作用：视频核心数据（元信息 + 计数）。

字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 主键 |
| `user_id` | `BIGINT` | 作者 uid |
| `title` | `VARCHAR(100)` | 标题 |
| `description` | `TEXT` | 简介 |
| `cover_url` | `VARCHAR(255)` | 封面 URL |
| `video_url` | `VARCHAR(255)` | 视频 URL |
| `duration` | `BIGINT` | 时长（秒） |
| `view_count` | `BIGINT` | 播放数 |
| `like_count` | `BIGINT` | 点赞数 |
| `comment_count` | `BIGINT` | 评论数 |
| `status` | `TINYINT(1)` | 状态 |
| `create_time` | `DATETIME` | 创建时间 |
| `update_time` | `DATETIME` | 更新时间 |

索引（摘自 `bilibili.sql`）：

```sql
PRIMARY KEY (`id`),
KEY `idx_title` (`title`),
KEY `idx_update_time` (`update_time`),
KEY `idx_view_count` (`view_count`),
KEY `idx_like_count` (`like_count`),
KEY `idx_comment_count` (`comment_count`),
KEY `idx_status_create` (`status`, `create_time`),
KEY `idx_user_status_create` (`user_id`, `status`, `create_time`)
```

## 3.4 `t_danmaku`

作用：视频弹幕内容。

字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 主键 |
| `video_id` | `BIGINT` | 视频 id |
| `user_id` | `BIGINT` | 发送者 uid |
| `content` | `TEXT` | 弹幕内容 |
| `show_time` | `BIGINT` | 出现时间点（毫秒） |
| `like_count` | `BIGINT` | 点赞数 |
| `status` | `TINYINT(1)` | 状态（0 正常，1 删除） |
| `create_time` | `DATETIME` | 创建时间 |

索引（摘自 `bilibili.sql`）：

```sql
PRIMARY KEY (`id`),
KEY `idx_video_id` (`video_id`),
KEY `idx_user_id` (`user_id`),
KEY `idx_like_count` (`like_count`),
KEY `idx_create_time` (`create_time`)
```

## 3.5 `t_danmaku_like`

作用：弹幕点赞关系（用户-弹幕）。

字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 主键 |
| `danmaku_id` | `BIGINT` | 弹幕 id |
| `user_id` | `BIGINT` | 用户 uid |
| `status` | `TINYINT(1)` | 状态 |
| `create_time` | `DATETIME` | 创建时间 |
| `update_time` | `DATETIME` | 更新时间 |

索引（摘自 `bilibili.sql`）：

```sql
PRIMARY KEY (`id`),
UNIQUE KEY `uk_user_danmaku` (`user_id`, `danmaku_id`),
KEY `idx_danmaku_id` (`danmaku_id`),
KEY `idx_create_time` (`create_time`),
KEY `idx_update_time` (`update_time`)
```

## 3.6 `t_video_like`

作用：视频点赞关系（用户-视频）。

字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 主键 |
| `video_id` | `BIGINT` | 视频 id |
| `user_id` | `BIGINT` | 用户 uid |
| `status` | `TINYINT(1)` | 状态 |
| `create_time` | `DATETIME` | 创建时间 |
| `update_time` | `DATETIME` | 更新时间 |

索引（摘自 `bilibili.sql`）：

```sql
PRIMARY KEY (`id`),
UNIQUE KEY `uk_user_video` (`user_id`, `video_id`),
KEY `idx_video_id` (`video_id`),
KEY `idx_create_time` (`create_time`),
KEY `idx_update_time` (`update_time`)
```

## 3.7 `t_comment`

作用：评论与一级回复数据。

字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 主键 |
| `video_id` | `BIGINT` | 视频 id |
| `user_id` | `BIGINT` | 评论者 uid |
| `content` | `TEXT` | 评论内容 |
| `parent_id` | `BIGINT` | 父评论 id（根评论通常为 0） |
| `root_id` | `BIGINT` | 根评论 id（根评论通常为 0） |
| `like_count` | `BIGINT` | 点赞数 |
| `reply_count` | `INT` | 直接回复数 |
| `status` | `TINYINT(1)` | 状态（0 正常，1 删除） |
| `create_time` | `DATETIME` | 创建时间 |

索引（摘自 `bilibili.sql`）：

```sql
PRIMARY KEY (`id`),
KEY `idx_user_id` (`user_id`),
KEY `idx_like_count` (`like_count`),
KEY `idx_video_status_parent_create` (`video_id`, `status`, `parent_id`, `create_time`),
KEY `idx_root_create` (`root_id`, `create_time`)
```

## 3.8 `t_comment_like`

作用：评论点赞关系（用户-评论）。

字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 主键 |
| `comment_id` | `BIGINT` | 评论 id |
| `user_id` | `BIGINT` | 用户 uid |
| `status` | `TINYINT(1)` | 状态 |
| `create_time` | `DATETIME` | 创建时间 |
| `update_time` | `DATETIME` | 更新时间 |

索引（摘自 `bilibili.sql`）：

```sql
PRIMARY KEY (`id`),
UNIQUE KEY `uk_user_comment` (`user_id`, `comment_id`),
KEY `idx_comment_id` (`comment_id`),
KEY `idx_create_time` (`create_time`),
KEY `idx_update_time` (`update_time`)
```

## 3.9 `t_following`

作用：关注关系（A 关注 B）。

字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 主键 |
| `user_id` | `BIGINT` | 发起关注的用户 |
| `following_user_id` | `BIGINT` | 被关注用户 |
| `status` | `TINYINT(1)` | 状态 |
| `create_time` | `DATETIME` | 创建时间 |
| `update_time` | `DATETIME` | 更新时间 |

索引（摘自 `bilibili.sql`）：

```sql
PRIMARY KEY (`id`),
UNIQUE KEY `uk_user_following` (`user_id`, `following_user_id`),
KEY `idx_user_status_following` (`user_id`, `status`, `following_user_id`),
KEY `idx_following_status_user` (`following_user_id`, `status`, `user_id`)
```

## 3.10 `t_video_upload_task`

作用：分片上传会话与状态机。

字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 主键 |
| `upload_id` | `VARCHAR(64)` | 上传会话 id（对外） |
| `user_id` | `BIGINT` | 上传者 uid |
| `file_name` | `VARCHAR(255)` | 原始文件名 |
| `content_type` | `VARCHAR(100)` | MIME 类型 |
| `file_size` | `BIGINT` | 文件总字节数 |
| `chunk_size` | `INT` | 分片大小 |
| `total_chunks` | `INT` | 分片总数 |
| `status` | `TINYINT(1)` | 状态（0 上传中，1 合并中，2 完成，3 过期，4 失败） |
| `temp_dir` | `VARCHAR(255)` | 临时目录相对路径 |
| `final_video_id` | `BIGINT` | 最终视频 id |
| `final_video_url` | `VARCHAR(255)` | 最终视频 URL |
| `error_msg` | `VARCHAR(255)` | 错误信息 |
| `expire_time` | `DATETIME` | 过期时间 |
| `create_time` | `DATETIME` | 创建时间 |
| `update_time` | `DATETIME` | 更新时间 |

索引（摘自 `bilibili.sql`）：

```sql
PRIMARY KEY (`id`),
UNIQUE KEY `uk_upload_id` (`upload_id`),
KEY `idx_user_status` (`user_id`, `status`),
KEY `idx_expire_time` (`expire_time`)
```

## 3.11 `t_tag`

作用：标签主数据。

字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 主键 |
| `name` | `VARCHAR(32)` | 标签名 |
| `use_count` | `INT` | 使用次数 |
| `status` | `TINYINT(1)` | 状态 |
| `create_time` | `DATETIME` | 创建时间 |
| `update_time` | `DATETIME` | 更新时间 |

索引（摘自 `bilibili.sql`）：

```sql
PRIMARY KEY (`id`),
UNIQUE KEY `uk_name` (`name`),
KEY `idx_create_time` (`create_time`),
KEY `idx_update_time` (`update_time`)
```

## 3.12 `t_video_tag`

作用：视频与标签的多对多关系表。

字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 主键 |
| `video_id` | `BIGINT` | 视频 id |
| `tag_id` | `BIGINT` | 标签 id |
| `status` | `TINYINT(1)` | 状态 |
| `create_time` | `DATETIME` | 创建时间 |
| `update_time` | `DATETIME` | 更新时间 |

索引（摘自 `bilibili.sql`）：

```sql
PRIMARY KEY (`id`),
KEY `idx_video_id` (`video_id`),
KEY `idx_tag_id` (`tag_id`)
```

---

## 4. 关系速览（逻辑关系）

- `t_user` 1:1 `t_user_info`（`t_user_info.user_id`）
- `t_user` 1:N `t_video`（`t_video.user_id`）
- `t_video` 1:N `t_comment`、`t_danmaku`
- `t_user` N:N `t_video`（经 `t_video_like`）
- `t_user` N:N `t_comment`（经 `t_comment_like`）
- `t_user` N:N `t_danmaku`（经 `t_danmaku_like`）
- `t_user` N:N `t_user`（经 `t_following`）
- `t_video` N:N `t_tag`（经 `t_video_tag`）
- `t_video_upload_task.final_video_id` 回写到 `t_video.id`
