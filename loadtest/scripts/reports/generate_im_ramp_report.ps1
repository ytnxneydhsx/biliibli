param(
    [string]$Root = '',
    [string]$OutputPath = ''
)

$ErrorActionPreference = 'Stop'

$loadtestRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
if ([string]::IsNullOrWhiteSpace($Root)) {
    $Root = Join-Path $loadtestRoot 'results\runs\im-online-pairs-ramp'
}
if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $loadtestRoot ('results\reports\im-online-pairs-ramp-analysis-{0}.md' -f (Get-Date -Format 'yyyyMMdd'))
}

$queuesOfInterest = @(
    'im.message.persist.queue',
    'im.message.conversation.queue',
    'im.message.conversation.redis.queue',
    'im.message.recent.cache.queue',
    'im.message.realtime.queue'
)

function Parse-Params([string]$Dir) {
    $map = @{}
    $path = Join-Path $Dir 'run-params.txt'
    if (!(Test-Path $path)) {
        return $map
    }
    Get-Content $path | ForEach-Object {
        if ($_ -match '^([^=]+)=(.*)$') {
            $map[$Matches[1]] = $Matches[2]
        }
    }
    return $map
}

function Read-JsonFile([string]$Path) {
    if (!(Test-Path $Path)) {
        return $null
    }
    $raw = Get-Content -Raw $Path
    if ([string]::IsNullOrWhiteSpace($raw)) {
        return $null
    }
    return ($raw | ConvertFrom-Json)
}

function Read-TextValue([string]$Path, [string]$DefaultValue = '-') {
    if (!(Test-Path $Path)) {
        return $DefaultValue
    }
    $raw = Get-Content -Raw $Path
    if ([string]::IsNullOrWhiteSpace($raw)) {
        return $DefaultValue
    }
    return $raw.Trim()
}

function Read-MetricMap([string]$Path, [string]$MetricName, [string[]]$LabelNames) {
    $result = @{}
    if (!(Test-Path $Path)) {
        return $result
    }
    $escapedMetric = [regex]::Escape($MetricName)
    foreach ($line in Get-Content $Path) {
        if ($line -match "^$escapedMetric\{([^}]*)\}\s+([0-9.Ee+-]+)") {
            $labelText = $Matches[1]
            $value = [double]$Matches[2]
            $labels = @{}
            foreach ($m in [regex]::Matches($labelText, '(\w+)="([^"]*)"')) {
                $labels[$m.Groups[1].Value] = $m.Groups[2].Value
            }
            $parts = foreach ($name in $LabelNames) {
                if ($labels.ContainsKey($name)) {
                    $labels[$name]
                } else {
                    ''
                }
            }
            $key = ($parts -join '|')
            $result[$key] = $value
        }
    }
    return $result
}

function DeltaMetric($Before, $After, [string]$Key) {
    $beforeValue = 0.0
    $afterValue = 0.0
    if ($Before.ContainsKey($Key)) {
        $beforeValue = [double]$Before[$Key]
    }
    if ($After.ContainsKey($Key)) {
        $afterValue = [double]$After[$Key]
    }
    return ($afterValue - $beforeValue)
}

function Format-Num($Value, [int]$Digits = 1) {
    if ($null -eq $Value) {
        return '-'
    }
    $number = [double]$Value
    if ([double]::IsNaN($number)) {
        return '-'
    }
    return $number.ToString("N$Digits")
}

function MarkdownEscape($Value) {
    if ($null -eq $Value) {
        return ''
    }
    return (($Value.ToString() -replace '\|', '\|' -replace "`r?`n", ' ')).Trim()
}

function Shorten($Value, [int]$MaxLength = 180) {
    $text = MarkdownEscape $Value
    if ($text.Length -gt $MaxLength) {
        return ($text.Substring(0, $MaxLength) + '...')
    }
    return $text
}

$directStageDirs = @(Get-ChildItem -Path $Root -Directory -Filter 'pairs-*-rate-*' -ErrorAction SilentlyContinue |
    Where-Object { Test-Path (Join-Path $_.FullName 'summary.json') })

if ($directStageDirs.Count -gt 0) {
    $stageDirs = $directStageDirs
} else {
    $stageDirs = Get-ChildItem -Path $Root -Directory |
        Where-Object { $_.Name -like 'im-online-pairs-ramp-*' } |
        ForEach-Object { Get-ChildItem -Path $_.FullName -Directory -Filter 'pairs-*-rate-*' } |
        Where-Object { Test-Path (Join-Path $_.FullName 'summary.json') }
}

$stageDirs = $stageDirs | Sort-Object {
    $params = Parse-Params $_.FullName
    if ($params.ContainsKey('rate')) {
        [int]$params.rate
    } else {
        0
    }
}

$stages = @()

foreach ($dir in $stageDirs) {
    $params = Parse-Params $dir.FullName
    $summary = Read-JsonFile (Join-Path $dir.FullName 'summary.json')
    if ($null -eq $summary) {
        continue
    }

    $lat = $summary.metrics.'im_ws_accepted_latency'
    $checks = $summary.metrics.checks
    $queueStats = @{}
    foreach ($queueName in $queuesOfInterest) {
        $queueStats[$queueName] = [ordered]@{
            max             = 0
            samplesGe100    = 0
            afterK6         = 0
            after20s        = 0
            consumerSamples = 0
            minConsumers    = [int]::MaxValue
            maxConsumers    = 0
            lastConsumers   = 0
        }
    }

    $samplePath = Join-Path $dir.FullName 'queue-samples.jsonl'
    if (Test-Path $samplePath) {
        foreach ($line in Get-Content $samplePath) {
            if ([string]::IsNullOrWhiteSpace($line)) {
                continue
            }
            $sample = $line | ConvertFrom-Json
            foreach ($queue in $sample.queues.value) {
                if ($queueStats.ContainsKey($queue.name)) {
                    $messages = [int]$queue.messages
                    if ($messages -gt $queueStats[$queue.name].max) {
                        $queueStats[$queue.name].max = $messages
                    }
                    if ($messages -ge 100) {
                        $queueStats[$queue.name].samplesGe100++
                    }
                    $consumerCount = [int]$queue.consumers
                    $queueStats[$queue.name].consumerSamples++
                    if ($consumerCount -lt $queueStats[$queue.name].minConsumers) {
                        $queueStats[$queue.name].minConsumers = $consumerCount
                    }
                    if ($consumerCount -gt $queueStats[$queue.name].maxConsumers) {
                        $queueStats[$queue.name].maxConsumers = $consumerCount
                    }
                    $queueStats[$queue.name].lastConsumers = $consumerCount
                }
            }
        }
    }

    foreach ($snapName in @('queues-after-k6.json', 'queues-after-20s.json')) {
        $snapPath = Join-Path $dir.FullName $snapName
        $field = if ($snapName -eq 'queues-after-k6.json') { 'afterK6' } else { 'after20s' }
        $snap = Read-JsonFile $snapPath
        if ($null -ne $snap) {
            foreach ($queue in $snap) {
                if ($queueStats.ContainsKey($queue.name)) {
                    $queueStats[$queue.name][$field] = [int]$queue.messages
                }
            }
        }
    }

    $beforeProm = Join-Path $dir.FullName 'metrics-before.prom'
    $afterProm = Join-Path $dir.FullName 'metrics-after-20s.prom'

    $dbCountBefore = Read-MetricMap $beforeProm 'im_db_operation_duration_seconds_count' @('operation', 'status')
    $dbCountAfter = Read-MetricMap $afterProm 'im_db_operation_duration_seconds_count' @('operation', 'status')
    $dbSumBefore = Read-MetricMap $beforeProm 'im_db_operation_duration_seconds_sum' @('operation', 'status')
    $dbSumAfter = Read-MetricMap $afterProm 'im_db_operation_duration_seconds_sum' @('operation', 'status')
    $dbMetrics = @()
    foreach ($key in (($dbCountBefore.Keys + $dbCountAfter.Keys) | Sort-Object -Unique)) {
        if (!$key.EndsWith('|success')) {
            continue
        }
        $countDelta = DeltaMetric $dbCountBefore $dbCountAfter $key
        $sumDelta = DeltaMetric $dbSumBefore $dbSumAfter $key
        if ($countDelta -le 0) {
            continue
        }
        $parts = $key -split '\|'
        $dbMetrics += [pscustomobject]@{
            operation = $parts[0]
            count     = [int][math]::Round($countDelta)
            avgMs     = ($sumDelta / $countDelta * 1000.0)
            totalS    = $sumDelta
        }
    }

    $mqCountBefore = Read-MetricMap $beforeProm 'im_mq_consumer_duration_seconds_count' @('consumer', 'queue', 'status')
    $mqCountAfter = Read-MetricMap $afterProm 'im_mq_consumer_duration_seconds_count' @('consumer', 'queue', 'status')
    $mqSumBefore = Read-MetricMap $beforeProm 'im_mq_consumer_duration_seconds_sum' @('consumer', 'queue', 'status')
    $mqSumAfter = Read-MetricMap $afterProm 'im_mq_consumer_duration_seconds_sum' @('consumer', 'queue', 'status')
    $mqMetrics = @()
    foreach ($key in (($mqCountBefore.Keys + $mqCountAfter.Keys) | Sort-Object -Unique)) {
        if (!$key.EndsWith('|success')) {
            continue
        }
        $countDelta = DeltaMetric $mqCountBefore $mqCountAfter $key
        $sumDelta = DeltaMetric $mqSumBefore $mqSumAfter $key
        if ($countDelta -le 0) {
            continue
        }
        $parts = $key -split '\|'
        $mqMetrics += [pscustomobject]@{
            consumer = $parts[0]
            queue    = $parts[1]
            count    = [int][math]::Round($countDelta)
            avgMs    = ($sumDelta / $countDelta * 1000.0)
            totalS   = $sumDelta
        }
    }

    $digest = @()
    $digestPath = Join-Path $dir.FullName 'mysql-digest.txt'
    if (Test-Path $digestPath) {
        $digest = Import-Csv -Path $digestPath -Delimiter "`t" | Select-Object -First 8
    }

    $slow = @()
    $slowPath = Join-Path $dir.FullName 'mysql-slow-group.txt'
    if (Test-Path $slowPath) {
        $slow = Import-Csv -Path $slowPath -Delimiter "`t" | Select-Object -First 8
    }

    $waits = @()
    $waitsPath = Join-Path $dir.FullName 'mysql-waits.txt'
    if (Test-Path $waitsPath) {
        $waits = Import-Csv -Path $waitsPath -Delimiter "`t" |
            Where-Object { $_.EVENT_NAME -ne 'idle' } |
            Select-Object -First 8
    }

    $stages += [pscustomobject]@{
        dir                   = $dir.FullName
        pairs                 = [int]$params.pairs
        rate                  = [int]$params.rate
        durationSeconds       = [int]$params.durationSeconds
        receiverWarmupMs      = [int]$params.receiverWarmupMs
        acceptTimeoutMs       = [int]$params.acceptTimeoutMs
        sampleIntervalSeconds = [int]$params.sampleIntervalSeconds
        baseUrl               = $params.baseUrl
        exitCode              = Read-TextValue (Join-Path $dir.FullName 'k6-exit-code.txt')
        sent                  = [int]$summary.metrics.im_ws_sent.count
        accepted              = [int]$summary.metrics.im_ws_accepted.count
        receiver              = [int]$summary.metrics.im_ws_receiver_message_received.count
        senderReceived        = [int]$summary.metrics.im_ws_sender_message_received.count
        checksFails           = [int]$checks.fails
        acceptedAvgMs         = [double]$lat.avg
        acceptedP95Ms         = [double]$lat.'p(95)'
        acceptedMaxMs         = [double]$lat.max
        queueStats            = $queueStats
        dbMetrics             = $dbMetrics
        mqMetrics             = $mqMetrics
        digest                = $digest
        slow                  = $slow
        waits                 = $waits
    }
}

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add('# IM 在线用户对阶梯压测记录')
$lines.Add('')
$lines.Add(('生成时间：{0} Asia/Shanghai' -f (Get-Date -Format 'yyyy-MM-dd')))
$lines.Add('')
$lines.Add('## 数据范围')
$lines.Add('')
$lines.Add('本文档汇总本次阶梯压测每个挡位的结果数据。每个挡位包含 k6 汇总、RabbitMQ 队列快照和采样、应用侧 Prometheus 指标，以及 MySQL Performance Schema / slow log 导出的统计。')
$lines.Add('')
$lines.Add('原始结果目录：')
foreach ($runDir in ($stageDirs | ForEach-Object { Split-Path $_.FullName -Parent } | Sort-Object -Unique)) {
    $lines.Add(('- `{0}`' -f $runDir))
}
$lines.Add('')
$lines.Add('每个挡位目录内包含的原始文件：`summary.json`、`k6.log`、`k6.err.log`、`k6-exit-code.txt`、`queues-before.json`、`queue-samples.jsonl`、`queues-after-k6.json`、`queues-after-20s.json`、`metrics-before.prom`、`metrics-after-k6.prom`、`metrics-after-20s.prom`、`mysql-digest.txt`、`mysql-slow-group.txt`、`mysql-waits.txt`。')
$lines.Add('')
$lines.Add('## 压测场景')
$lines.Add('')
$lines.Add('- 按挡位增加 sender/receiver 用户对数量。')
$lines.Add('- 每个 sender 每 0.5 秒发送 1 条消息，所以目标吞吐为 `用户对数量 * 2 msg/s`。')
$lines.Add('- 每个挡位在 receiver 预热后持续发送 60 秒。')
$lines.Add('- 每个挡位开始前会清理 Redis、清空 IM 队列、重置 MySQL Performance Schema / slow log 统计表，然后分别采集压测前后快照。')
$lines.Add('- 应用侧 Prometheus 指标是累计 counter，所以本文档中的每挡位数据按 `metrics-after-20s - metrics-before` 计算。')
$lines.Add('')
$lines.Add('## K6 汇总')
$lines.Add('')
$lines.Add('| 用户对 | 目标 msg/s | 已发送 | Accepted | 接收方收到 | 发送方回执 | Check 失败 | Accepted 平均 ms | Accepted P95 ms | 最大 ms | Exit |')
$lines.Add('|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|')
foreach ($stage in $stages) {
    $lines.Add("| $($stage.pairs) | $($stage.rate) | $($stage.sent) | $($stage.accepted) | $($stage.receiver) | $($stage.senderReceived) | $($stage.checksFails) | $(Format-Num $stage.acceptedAvgMs 1) | $(Format-Num $stage.acceptedP95Ms 1) | $(Format-Num $stage.acceptedMaxMs 1) | $($stage.exitCode) |")
}
$lines.Add('')
$lines.Add('## RabbitMQ 队列积压汇总')
$lines.Add('')
$lines.Add('`最大积压` 是 `queue-samples.jsonl` 里该队列 `messages` 的最大值。`>=100 采样次数` 表示采样时该队列积压至少 100 条的次数。`Consumer 最小/最大/最后` 来自压测期间 RabbitMQ 队列采样，用于观察 Spring AMQP 动态扩容情况。`k6 结束后` 和 `20s 后` 来自对应的队列快照文件。')
$lines.Add('')
$lines.Add('| 用户对 | 速率 | 队列 | Consumer 最小 | Consumer 最大 | Consumer 最后 | 最大积压 | >=100 采样次数 | k6 结束后 | 20s 后 |')
$lines.Add('|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|')
foreach ($stage in $stages) {
    foreach ($queueName in $queuesOfInterest) {
        $stats = $stage.queueStats[$queueName]
        $minConsumers = if ($stats.consumerSamples -gt 0) { $stats.minConsumers } else { 0 }
        $lines.Add(('| {0} | {1} | `{2}` | {3} | {4} | {5} | {6} | {7} | {8} | {9} |' -f $stage.pairs, $stage.rate, $queueName, $minConsumers, $stats.maxConsumers, $stats.lastConsumers, $stats.max, $stats.samplesGe100, $stats.afterK6, $stats.after20s))
    }
}
$lines.Add('')
$lines.Add('## 应用侧 DB 操作指标')
$lines.Add('')
$lines.Add('以下数据来自 `im_db_operation_duration_seconds_*`，按每个挡位的前后差值计算。')
$lines.Add('')
$lines.Add('| 用户对 | 速率 | 操作 | 次数 | 平均 ms | 总耗时 s |')
$lines.Add('|---:|---:|---|---:|---:|---:|')
foreach ($stage in $stages) {
    foreach ($metric in ($stage.dbMetrics | Sort-Object operation)) {
        $lines.Add(('| {0} | {1} | `{2}` | {3} | {4} | {5} |' -f $stage.pairs, $stage.rate, $metric.operation, $metric.count, (Format-Num $metric.avgMs 3), (Format-Num $metric.totalS 3)))
    }
}
$lines.Add('')
$lines.Add('## 应用侧 MQ Consumer 指标')
$lines.Add('')
$lines.Add('以下数据来自 `im_mq_consumer_duration_seconds_*`，按每个挡位的前后差值计算。')
$lines.Add('')
$lines.Add('| 用户对 | 速率 | Consumer | 队列 | 次数 | 平均 ms | 总耗时 s |')
$lines.Add('|---:|---:|---|---|---:|---:|---:|')
foreach ($stage in $stages) {
    foreach ($metric in ($stage.mqMetrics | Sort-Object queue, consumer)) {
        $lines.Add(('| {0} | {1} | `{2}` | `{3}` | {4} | {5} | {6} |' -f $stage.pairs, $stage.rate, $metric.consumer, $metric.queue, $metric.count, (Format-Num $metric.avgMs 3), (Format-Num $metric.totalS 3)))
    }
}
$lines.Add('')
$lines.Add('## MySQL Digest Top 语句')
$lines.Add('')
$lines.Add('以下 Top 行来自每个挡位的 `mysql-digest.txt`。')
foreach ($stage in $stages) {
    $lines.Add('')
    $lines.Add("### 用户对 $($stage.pairs)，速率 $($stage.rate) msg/s")
    $lines.Add('')
    $lines.Add('| 次数 | 总耗时 s | 平均 ms | 最大 ms | Digest |')
    $lines.Add('|---:|---:|---:|---:|---|')
    foreach ($row in $stage.digest) {
        $lines.Add(('| {0} | {1} | {2} | {3} | `{4}` |' -f $row.COUNT_STAR, $row.total_s, $row.avg_ms, $row.max_ms, (Shorten $row.DIGEST_TEXT)))
    }
}
$lines.Add('')
$lines.Add('## MySQL Waits Top 事件')
$lines.Add('')
$lines.Add('以下 Top 行来自每个挡位的 `mysql-waits.txt`，已排除 `idle`。')
foreach ($stage in $stages) {
    $lines.Add('')
    $lines.Add("### 用户对 $($stage.pairs)，速率 $($stage.rate) msg/s")
    $lines.Add('')
    $lines.Add('| 事件 | 次数 | 总耗时 s | 平均 ms | 最大 ms |')
    $lines.Add('|---|---:|---:|---:|---:|')
    foreach ($row in $stage.waits) {
        $lines.Add(('| `{0}` | {1} | {2} | {3} | {4} |' -f $row.EVENT_NAME, $row.COUNT_STAR, $row.total_s, $row.avg_ms, $row.max_ms))
    }
}
$lines.Add('')
$lines.Add('## 原始文件索引')
$lines.Add('')
foreach ($stage in $stages) {
    $lines.Add("### 用户对 $($stage.pairs)，速率 $($stage.rate) msg/s")
    $lines.Add('')
    $lines.Add(('目录：`{0}`' -f $stage.dir))
    $lines.Add('')
    foreach ($fileName in @(
        'summary.json',
        'k6.log',
        'k6.err.log',
        'k6-exit-code.txt',
        'queues-before.json',
        'queue-samples.jsonl',
        'queues-after-k6.json',
        'queues-after-20s.json',
        'metrics-before.prom',
        'metrics-after-k6.prom',
        'metrics-after-20s.prom',
        'mysql-digest.txt',
        'mysql-slow-group.txt',
        'mysql-waits.txt'
    )) {
        $path = Join-Path $stage.dir $fileName
        $length = if (Test-Path $path) { (Get-Item $path).Length } else { 0 }
        $lines.Add(('- `{0}` ({1} bytes): `{2}`' -f $fileName, $length, $path))
    }
    $lines.Add('')
}

New-Item -ItemType Directory -Force -Path (Split-Path $OutputPath -Parent) | Out-Null
Set-Content -Path $OutputPath -Value $lines -Encoding UTF8
Write-Host $OutputPath
