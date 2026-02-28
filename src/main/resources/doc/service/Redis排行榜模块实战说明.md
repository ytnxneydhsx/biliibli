# Redis Video Rank Module (Orchestration Version)

## 1. Goal

This module avoids high-frequency MySQL writes for view counts.

Design:
1. Request-time: write view increments to Redis.
2. Schedule-time: flush Redis increments back to MySQL.
3. Ranking reads from Redis ZSet first.

---

## 2. Redis structures

1. Delta counter (`String`)
- key: `video:view:delta:{videoId}`
- value: unsynced increment count
- commands: `INCR`, `DECRBY`, `EXPIRE`

2. Rank set (`ZSet`)
- key: `rank:video:view`
- member: `videoId`
- score: hot score (double)
- command: `ZINCRBY`

No dirty set in this V1. The task uses `SCAN` on `video:view:delta:*`.

---

## 3. Data flow

```mermaid
flowchart TD
    A[Client POST /videos/{videoId}/views] --> B[VideoController]
    B --> C[VideoAppService.increaseViewCount]
    C --> D[VideoService.increaseViewCount check exists]
    C --> E[VideoRankService.increaseVideoViewScore]
    E --> F[INCR video:view:delta:{videoId}]
    E --> G[EXPIRE video:view:delta:{videoId} 24h]
    E --> H[ZINCRBY rank:video:view +1]

    I[@Scheduled every 5s] --> J[SCAN video:view:delta:*]
    J --> K[Read delta]
    K --> L[MySQL update view_count += delta]
    L --> M[DECRBY same delta]
    M --> N{remain <= 0}
    N -- yes --> O[DEL key]

    P[Client GET /videos/rank] --> Q[VideoRankService.listVideoViewRank]
    Q --> R[Read ZSet TopN]
    R --> S[Batch load video info from MySQL]
    S --> T[Return rank list]
```

---

## 4. Why score is double

Redis ZSet score type is floating-point by design.
Even if views are integers now, using `double` is correct and future-proof.

---

## 5. Key files

1. Orchestration service
- `src/main/java/com/bilibili/service/impl/VideoAppServiceImpl.java`

2. Video domain service
- `src/main/java/com/bilibili/service/impl/VideoServiceImpl.java`

3. Rank service
- `src/main/java/com/bilibili/service/impl/VideoRankServiceImpl.java`

4. Scheduled flush task
- `src/main/java/com/bilibili/task/VideoViewSyncTask.java`

5. DB delta update SQL
- `src/main/resources/mapper/VideoMapper.xml`

6. API entry
- `src/main/java/com/bilibili/controller/VideoController.java`

---

## 6. Current limits (V1)

1. Uses `SCAN` (no dirty set index).
2. Optimized for single instance first.
3. Can be upgraded later with distributed lock and stronger retry/recovery.
