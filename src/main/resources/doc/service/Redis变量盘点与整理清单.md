# Redis Variables Inventory And Cleanup

## 1. What has been consolidated

Redis-related hardcoded values used by business code are now centralized.

### Key names and prefixes

File: `src/main/java/com/bilibili/config/redis/RedisViewCacheKeys.java`

- `VIDEO_VIEW_RANK_KEY = "rank:video:view"`
- `VIDEO_VIEW_DELTA_KEY_PREFIX = "video:view:delta:"`
- `VIDEO_VIEW_DIRTY_KEY = "video:view:dirty"`
- Helper: `buildVideoViewDeltaKey(videoId)`

### Runtime tuning constants

File: `src/main/java/com/bilibili/config/redis/RedisViewCacheTuning.java`

- `VIDEO_VIEW_DELTA_EXPIRE_SECONDS = 86400`
- `VIDEO_VIEW_RANK_WARMUP_LIMIT = 200`
- `VIDEO_VIEW_SYNC_FIXED_DELAY_MS = 5000`

## 2. Files switched to centralized constants

### Main code

- `src/main/java/com/bilibili/service/impl/VideoRankServiceImpl.java`
- `src/main/java/com/bilibili/task/VideoViewSyncTask.java`

### Tests

- `src/test/java/com/bilibili/service/impl/VideoRankServiceImplTest.java`
- `src/test/java/com/bilibili/task/VideoViewSyncTaskTest.java`

## 3. Current Redis configuration source

Connection settings are already externalized in:

File: `src/main/resources/db.properties`

- `redis.host`
- `redis.port`
- `redis.password`
- `redis.database`

Loaded by:

File: `src/main/java/com/bilibili/config/data/RedisConfig.java`

## 4. Remaining optimization candidates (next step)

These are still constants, not property-driven:

- `VIDEO_VIEW_DELTA_EXPIRE_SECONDS`
- `VIDEO_VIEW_RANK_WARMUP_LIMIT`
- `VIDEO_VIEW_SYNC_FIXED_DELAY_MS`

Recommended next optimization:

1. Add to `db.properties`:
- `redis.video.view.deltaExpireSeconds`
- `redis.video.rank.warmupLimit`
- `redis.video.view.syncFixedDelayMs`

2. Replace static constants with `@Value` / typed properties bean.

3. Keep `RedisViewCacheKeys` as constant keys (or make key prefix configurable only if needed).
