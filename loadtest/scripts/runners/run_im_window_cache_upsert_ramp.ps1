param(
    [int[]]$PairsList = @(100, 200, 300, 400, 600, 800),
    [int]$DurationSeconds = 60,
    [int]$ReceiverWarmupMs = 5000,
    [int]$AcceptTimeoutMs = 10000,
    [int]$SampleIntervalSeconds = 5,
    [int]$DrainWaitSeconds = 20,
    [string]$BaseUrl = "http://host.docker.internal:8080",
    [bool]$PrewarmInitKeys = $true,
    [string]$RunRoot = ""
)

$ErrorActionPreference = "Stop"

$loadtestRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if ([string]::IsNullOrWhiteSpace($RunRoot)) {
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $RunRoot = Join-Path $loadtestRoot "results\runs\im-window-cache-upsert-ramp\im-window-cache-upsert-ramp-$timestamp"
}
New-Item -ItemType Directory -Force -Path $RunRoot | Out-Null
$RunRoot = (Resolve-Path $RunRoot).Path
if (-not $RunRoot.StartsWith($loadtestRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "RunRoot must be under loadtest root so Docker k6 can write summary files: $RunRoot"
}
$loadtestRootUri = [System.Uri]((Join-Path $loadtestRoot '') -replace '\\', '/')
$runRootUri = [System.Uri]($RunRoot -replace '\\', '/')
$runRootRelative = [System.Uri]::UnescapeDataString(
    $loadtestRootUri.MakeRelativeUri($runRootUri).ToString()
).Replace('\', '/')

$queueNames = @(
    "im.message.persist.queue",
    "im.message.conversation.queue",
    "im.message.conversation.redis.queue",
    "im.message.recent.cache.queue",
    "im.message.realtime.queue"
)

function Invoke-MySqlRoot {
    param([string]$Sql)
    docker exec bilibili-mysql mysql -uroot -proot -e "$Sql"
}

function Reset-MySqlPerf {
    $sql = @"
SET GLOBAL log_output='TABLE';
SET GLOBAL slow_query_log=ON;
SET GLOBAL long_query_time=0.001;
TRUNCATE TABLE mysql.slow_log;
TRUNCATE TABLE performance_schema.events_statements_summary_by_digest;
TRUNCATE TABLE performance_schema.events_statements_summary_global_by_event_name;
TRUNCATE TABLE performance_schema.events_waits_summary_global_by_event_name;
TRUNCATE TABLE performance_schema.table_io_waits_summary_by_table;
TRUNCATE TABLE performance_schema.table_io_waits_summary_by_index_usage;
TRUNCATE TABLE performance_schema.table_lock_waits_summary_by_table;
TRUNCATE TABLE performance_schema.file_summary_by_event_name;
TRUNCATE TABLE performance_schema.file_summary_by_instance;
SELECT 'mysql_perf_reset_done' AS status;
"@
    Invoke-MySqlRoot $sql | Out-Null
}

function Reset-Redis {
    docker exec bilibili-redis redis-cli FLUSHDB | Out-Null
    docker exec bilibili-redis redis-cli CONFIG RESETSTAT | Out-Null
}

function Prewarm-RedisConversationInitKeys {
    param([int]$Pairs)
    if (-not $PrewarmInitKeys) {
        return
    }
    $ttlSeconds = 86400
    $startUserId = 1
    $endUserId = $Pairs * 2 + 5000
    $script = "for i=tonumber(ARGV[1]),tonumber(ARGV[2]) do redis.call('SET','im:conv:init:'..i,'1','EX',ARGV[3]) end return ARGV[2]-ARGV[1]+1"
    docker exec bilibili-redis redis-cli EVAL $script 0 $startUserId $endUserId $ttlSeconds |
        Out-File -Encoding utf8 (Join-Path $stageDir "redis-initkey-prewarm.txt")
}

function Purge-ImQueues {
    foreach ($queue in $queueNames) {
        docker exec bilibili-rabbitmq rabbitmqctl purge_queue $queue | Out-Null
    }
}

function Save-Queues {
    param([string]$Path)
    docker exec bilibili-rabbitmq rabbitmqctl list_queues name messages messages_ready messages_unacknowledged consumers --formatter json |
        Out-File -Encoding utf8 $Path
}

function Save-Metrics {
    param([string]$Path)
    try {
        Invoke-WebRequest -UseBasicParsing http://localhost:8080/actuator/prometheus -TimeoutSec 10 |
            Select-Object -ExpandProperty Content |
            Out-File -Encoding utf8 $Path
    } catch {
        $_.Exception.Message | Out-File -Encoding utf8 $Path
    }
}

function Save-RedisReports {
    param(
        [string]$StageDir,
        [string]$Prefix
    )

    docker exec bilibili-redis redis-cli INFO stats |
        Out-File -Encoding utf8 (Join-Path $StageDir "redis-$Prefix-stats.txt")
    docker exec bilibili-redis redis-cli INFO commandstats |
        Out-File -Encoding utf8 (Join-Path $StageDir "redis-$Prefix-commandstats.txt")
    docker exec bilibili-redis redis-cli INFO keyspace |
        Out-File -Encoding utf8 (Join-Path $StageDir "redis-$Prefix-keyspace.txt")
    docker exec bilibili-redis redis-cli DBSIZE |
        Out-File -Encoding utf8 (Join-Path $StageDir "redis-$Prefix-dbsize.txt")
    docker exec bilibili-redis sh -lc "redis-cli --scan --pattern 'im:conv:*' | wc -l" |
        Out-File -Encoding utf8 (Join-Path $StageDir "redis-$Prefix-im-conv-key-count.txt")
}

function Save-MySqlReports {
    param([string]$StageDir)

    $digestSql = @"
SELECT
  DIGEST_TEXT,
  COUNT_STAR,
  ROUND(SUM_TIMER_WAIT/1000000000000, 4) AS total_s,
  ROUND(AVG_TIMER_WAIT/1000000000, 4) AS avg_ms,
  ROUND(MAX_TIMER_WAIT/1000000000, 4) AS max_ms
FROM performance_schema.events_statements_summary_by_digest
WHERE SCHEMA_NAME = 'bilibili'
ORDER BY SUM_TIMER_WAIT DESC
LIMIT 50;
"@
    Invoke-MySqlRoot $digestSql | Out-File -Encoding utf8 (Join-Path $StageDir "mysql-digest.txt")

    $conversationDigestSql = @"
SELECT
  DIGEST_TEXT,
  COUNT_STAR,
  ROUND(SUM_TIMER_WAIT/1000000000000, 4) AS total_s,
  ROUND(AVG_TIMER_WAIT/1000000000, 4) AS avg_ms,
  ROUND(MAX_TIMER_WAIT/1000000000, 4) AS max_ms
FROM performance_schema.events_statements_summary_by_digest
WHERE SCHEMA_NAME = 'bilibili'
  AND DIGEST_TEXT LIKE '%chat_conversation%'
ORDER BY SUM_TIMER_WAIT DESC
LIMIT 30;
"@
    Invoke-MySqlRoot $conversationDigestSql | Out-File -Encoding utf8 (Join-Path $StageDir "mysql-conversation-digest.txt")

    $waitSql = @"
SELECT
  EVENT_NAME,
  COUNT_STAR,
  ROUND(SUM_TIMER_WAIT/1000000000000, 4) AS total_s,
  ROUND(AVG_TIMER_WAIT/1000000000, 4) AS avg_ms,
  ROUND(MAX_TIMER_WAIT/1000000000, 4) AS max_ms
FROM performance_schema.events_waits_summary_global_by_event_name
WHERE SUM_TIMER_WAIT > 0
ORDER BY SUM_TIMER_WAIT DESC
LIMIT 50;
"@
    Invoke-MySqlRoot $waitSql | Out-File -Encoding utf8 (Join-Path $StageDir "mysql-waits.txt")

    $slowSql = @"
SELECT
  db,
  COUNT(*) AS cnt,
  ROUND(SUM(TIME_TO_SEC(query_time)), 4) AS total_s,
  ROUND(AVG(TIME_TO_SEC(query_time))*1000, 4) AS avg_ms,
  ROUND(MAX(TIME_TO_SEC(query_time))*1000, 4) AS max_ms,
  LEFT(sql_text, 220) AS sql_sample
FROM mysql.slow_log
WHERE db = 'bilibili'
GROUP BY db, LEFT(sql_text, 220)
ORDER BY total_s DESC
LIMIT 50;
"@
    Invoke-MySqlRoot $slowSql | Out-File -Encoding utf8 (Join-Path $StageDir "mysql-slow-group.txt")
}

function Run-Stage {
    param([int]$Pairs)

    $rate = $Pairs * 2
    $stageName = "pairs-$Pairs-rate-$rate"
    $stageDir = Join-Path $RunRoot $stageName
    New-Item -ItemType Directory -Force -Path $stageDir | Out-Null

    "pairs=$Pairs`nrate=$rate`ndurationSeconds=$DurationSeconds`nreceiverWarmupMs=$ReceiverWarmupMs`nacceptTimeoutMs=$AcceptTimeoutMs`nsampleIntervalSeconds=$SampleIntervalSeconds`ndrainWaitSeconds=$DrainWaitSeconds`nbaseUrl=$BaseUrl`nprewarmInitKeys=$PrewarmInitKeys`nscenario=im_ws_online_pairs_window_cache.js" |
        Out-File -Encoding utf8 (Join-Path $stageDir "run-params.txt")

    Reset-Redis
    Prewarm-RedisConversationInitKeys -Pairs $Pairs
    Purge-ImQueues
    Reset-MySqlPerf

    Save-Queues (Join-Path $stageDir "queues-before.json")
    Save-Metrics (Join-Path $stageDir "metrics-before.prom")
    Save-RedisReports $stageDir "before"

    $k6Args = @(
        "run", "--rm",
        "-e", "BASE_URL=$BaseUrl",
        "-e", "PAIRS=$Pairs",
        "-e", "RATE=$rate",
        "-e", "DURATION_SECONDS=$DurationSeconds",
        "-e", "RECEIVER_WARMUP_MS=$ReceiverWarmupMs",
        "-e", "ACCEPT_TIMEOUT_MS=$AcceptTimeoutMs",
        "-v", "${loadtestRoot}:/work",
        "grafana/k6:0.49.0",
        "run",
        "--summary-export", "/work/$runRootRelative/$stageName/summary.json",
        "/work/scripts/scenarios/im_ws_online_pairs_window_cache.js"
    )

    $sampleFile = Join-Path $stageDir "queue-samples.jsonl"
    $process = Start-Process -FilePath "docker" -ArgumentList $k6Args -NoNewWindow -PassThru `
        -RedirectStandardOutput (Join-Path $stageDir "k6.log") `
        -RedirectStandardError (Join-Path $stageDir "k6.err.log")

    while (-not $process.HasExited) {
        $now = Get-Date -Format "o"
        $queues = docker exec bilibili-rabbitmq rabbitmqctl list_queues name messages messages_ready messages_unacknowledged consumers --formatter json
        [pscustomobject]@{
            ts = $now
            queues = ($queues | ConvertFrom-Json)
        } | ConvertTo-Json -Depth 6 -Compress | Add-Content -Encoding utf8 $sampleFile
        Start-Sleep -Seconds $SampleIntervalSeconds
    }

    $process.WaitForExit()
    $process.Refresh()
    $exitCode = -1
    try {
        $exitCode = [int]$process.ExitCode
    } catch {
        $_.Exception.Message | Out-File -Encoding utf8 (Join-Path $stageDir "k6-exit-code-error.txt")
    }
    [string]$exitCode | Out-File -Encoding utf8 (Join-Path $stageDir "k6-exit-code.txt")

    Save-Queues (Join-Path $stageDir "queues-after-k6.json")
    Save-Metrics (Join-Path $stageDir "metrics-after-k6.prom")
    Save-RedisReports $stageDir "after-k6"
    Start-Sleep -Seconds $DrainWaitSeconds
    Save-Queues (Join-Path $stageDir "queues-after-20s.json")
    Save-Metrics (Join-Path $stageDir "metrics-after-20s.prom")
    Save-RedisReports $stageDir "after-drain"
    Save-MySqlReports $stageDir
}

foreach ($pairs in $PairsList) {
    Run-Stage -Pairs $pairs
}

Invoke-MySqlRoot "SET GLOBAL slow_query_log=OFF; SET GLOBAL long_query_time=10;" |
    Out-File -Encoding utf8 (Join-Path $RunRoot "mysql-slow-log-restored.txt")

New-Item -ItemType Directory -Force -Path (Join-Path $loadtestRoot "results\latest") | Out-Null
$RunRoot | Out-File -Encoding utf8 (Join-Path $loadtestRoot "results\latest\latest_im_window_cache_upsert_ramp_run.txt")
Write-Output $RunRoot
