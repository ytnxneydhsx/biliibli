# 视频模块 VRS（VideoRankService）接口与调用链路说明

## 1. 范围

本文只覆盖 VRS（`VideoRankService`）相关能力：

- 播放量增量写入
- 播放排行读取
- Redis -> MySQL 回刷链路

## 2. 接口与方法映射

| 接口 | 控制器方法 | 服务方法 |
| --- | --- | --- |
| `GET /videos/rank` | `VideoController.listVideoRank` | `VideoAppService.listVideoRank -> VideoRankService.listVideoViewRank` |
| `POST /videos/{videoId}/views` | `VideoController.increaseViewCount` | `VideoAppService.increaseViewCount -> VideoService.validateViewableVideo + VideoRankService.increaseVideoViewScore` |

## 3. 缓存键设计

| Key | 说明 |
| --- | --- |
| `rank:video:view` | ZSet，视频播放排行分数 |
| `video:view:delta:{videoId}` | String，某视频待回刷播放增量 |
| `video:view:dirty` | Set，待回刷视频 ID 集合 |

## 4. 关键调用链

## 4.1 播放增量 `POST /videos/{videoId}/views`

1. `VideoAppService` 先调用 `VideoService.validateViewableVideo(videoId)` 做视频可见性校验。
2. `VideoRankService.increaseVideoViewScore(videoId, 1)` 写入 Redis：
   - `deltaKey` 自增
   - `deltaKey` 设置过期时间（24h）
   - `dirty set` 加入视频 id
   - ZSet `rank:video:view` 分数 +1
3. Redis 异常被吞掉，不影响主请求链路返回。

## 4.2 排行查询 `GET /videos/rank`

1. 按分页计算 ZSet 范围 `[start, end]`。
2. 从 ZSet 倒序读取分数区间。
3. 若区间结果为空，直接返回空分页结果。
4. 若区间非空，根据 ZSet 的有序 `videoId` 批量回表查视频信息并组装 `VideoRankVO`。
5. 若 Redis 异常，返回空分页结果（不再回退 MySQL）。

## 4.3 回刷任务（最终一致）

`VideoViewSyncTask` 定时执行（固定延迟 50s）：

1. 读取 `video:view:dirty` 所有视频 id。
2. 逐个读取 `video:view:delta:{videoId}`。
3. 调用 `VideoMapper.updateViewCountByDelta(videoId, delta)` 累加到 `t_video.view_count`。
4. 回刷成功后扣减/清理 delta，并从 dirty set 移除。

## 5. 一致性与降级策略

- 播放数是“缓存实时 + 数据库最终一致”模型，短时允许差异。
- 排行是“纯 Redis 榜单”，不做 MySQL 预热灌榜，不回退 MySQL。
- Redis 冷启动或清空后，榜单会为空，直到出现新的播放增量。

## 6. 主要数据与组件

- Redis：ZSet + String + Set
- 表：`t_video`
- 组件：`VideoRankServiceImpl`、`VideoViewSyncTask`

## 7. 鉴权与错误码

- `GET /videos/rank` 与 `POST /videos/{videoId}/views` 均是公开接口。
- 常见错误主要来自参数非法或视频不存在（校验发生在 `validateViewableVideo`）。

## 8. VRS 链路图

```mermaid
flowchart TD
    A[POST /videos/{videoId}/views] --> B[validateViewableVideo]
    B --> C[increaseVideoViewScore]
    C --> D[(video:view:delta:{id})]
    C --> E[(video:view:dirty)]
    C --> F[(rank:video:view)]

    G[GET /videos/rank] --> H[read ZSet range]
    H --> I{empty?}
    I -->|yes| J[return empty page]
    I -->|no| K[build rank result]
    K --> L[Result<PageVO<VideoRankVO>>]

    M[VideoViewSyncTask every 50s] --> E
    M --> D
    M --> N[update t_video.view_count]
```
