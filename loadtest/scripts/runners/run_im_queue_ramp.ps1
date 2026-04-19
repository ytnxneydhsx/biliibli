param(
    [int[]]$PairsList = @(50, 100, 150, 200, 250, 300, 350, 400),
    [int]$DurationSeconds = 60,
    [int]$ReceiverWarmupMs = 5000,
    [int]$AcceptTimeoutMs = 10000,
    [int]$SampleIntervalSeconds = 5,
    [string]$BaseUrl = "http://host.docker.internal:8080",
    [string]$RunRoot = ""
)

$ErrorActionPreference = "Stop"

$loadtestRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if ([string]::IsNullOrWhiteSpace($RunRoot)) {
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $RunRoot = Join-Path $loadtestRoot "results\runs\im-online-pairs-ramp\im-online-pairs-ramp-$timestamp"
}
New-Item -ItemType Directory -Force -Path $RunRoot | Out-Null
$RunRoot = (Resolve-Path $RunRoot).Path
if (-not $RunRoot.StartsWith($loadtestRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "RunRoot must be under loadtest root so Docker k6 can write summary files: $RunRoot"
}
$runRootRelative = [System.IO.Path]::GetRelativePath($loadtestRoot, $RunRoot).Replace('\', '/')

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
}

function Purge-ImQueues {
    foreach ($queue in $queueNames) {
        docker exec bilibili-rabbitmq rabbitmqctl purge_queue $queue | Out-Null
    }
}

function Save-Queues {
    param([string]$Path)
    docker exec bilibili-rabbitmq rabbitmqctl list_queues name messages messages_ready messages_unacknowledged consumers --formatter json | Out-File -Encoding utf8 $Path
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
LIMIT 30;
"@
    Invoke-MySqlRoot $digestSql | Out-File -Encoding utf8 (Join-Path $StageDir "mysql-digest.txt")

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
LIMIT 30;
"@
    Invoke-MySqlRoot $waitSql | Out-File -Encoding utf8 (Join-Path $StageDir "mysql-waits.txt")

    $slowSql = @"
SELECT
  db,
  COUNT(*) AS cnt,
  ROUND(SUM(TIME_TO_SEC(query_time)), 4) AS total_s,
  ROUND(AVG(TIME_TO_SEC(query_time))*1000, 4) AS avg_ms,
  ROUND(MAX(TIME_TO_SEC(query_time))*1000, 4) AS max_ms,
  LEFT(sql_text, 180) AS sql_sample
FROM mysql.slow_log
WHERE db = 'bilibili'
GROUP BY db, LEFT(sql_text, 180)
ORDER BY total_s DESC
LIMIT 30;
"@
    Invoke-MySqlRoot $slowSql | Out-File -Encoding utf8 (Join-Path $StageDir "mysql-slow-group.txt")
}

function Run-Stage {
    param([int]$Pairs)

    $rate = $Pairs * 2
    $stageName = "pairs-$Pairs-rate-$rate"
    $stageDir = Join-Path $RunRoot $stageName
    New-Item -ItemType Directory -Force -Path $stageDir | Out-Null

    "pairs=$Pairs`nrate=$rate`ndurationSeconds=$DurationSeconds`nreceiverWarmupMs=$ReceiverWarmupMs`nacceptTimeoutMs=$AcceptTimeoutMs`nsampleIntervalSeconds=$SampleIntervalSeconds`nbaseUrl=$BaseUrl" |
        Out-File -Encoding utf8 (Join-Path $stageDir "run-params.txt")

    Reset-Redis
    Purge-ImQueues
    Reset-MySqlPerf

    Save-Queues (Join-Path $stageDir "queues-before.json")
    Save-Metrics (Join-Path $stageDir "metrics-before.prom")

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
        "/work/scripts/scenarios/im_ws_online_pairs_constant_rate.js"
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
    $process.ExitCode | Out-File -Encoding utf8 (Join-Path $stageDir "k6-exit-code.txt")

    Save-Queues (Join-Path $stageDir "queues-after-k6.json")
    Save-Metrics (Join-Path $stageDir "metrics-after-k6.prom")
    Start-Sleep -Seconds 20
    Save-Queues (Join-Path $stageDir "queues-after-20s.json")
    Save-Metrics (Join-Path $stageDir "metrics-after-20s.prom")
    Save-MySqlReports $stageDir
}

foreach ($pairs in $PairsList) {
    Run-Stage -Pairs $pairs
}

Invoke-MySqlRoot "SET GLOBAL slow_query_log=OFF; SET GLOBAL long_query_time=10;" |
    Out-File -Encoding utf8 (Join-Path $RunRoot "mysql-slow-log-restored.txt")

New-Item -ItemType Directory -Force -Path (Join-Path $loadtestRoot "results\latest") | Out-Null
$RunRoot | Out-File -Encoding utf8 (Join-Path $loadtestRoot "results\latest\latest_im_online_pairs_ramp_run.txt")
Write-Output $RunRoot
