local rankKey = KEYS[1]
local dirtyKey = KEYS[2]
local cardKey = KEYS[3]
local cardIndexKey = KEYS[4]

local videoId = ARGV[1]
local nowMillis = ARGV[2]
local rankSize = tonumber(ARGV[3])
local cardKeyPrefix = ARGV[4]

local newViewCount = redis.call('HINCRBY', cardKey, 'viewCount', 1)
redis.call('HSET', cardKey, 'lastViewAt', nowMillis)
redis.call('SADD', dirtyKey, videoId)
redis.call('SADD', cardIndexKey, videoId)

local existingScore = redis.call('ZSCORE', rankKey, videoId)
if existingScore then
    redis.call('ZADD', rankKey, newViewCount, videoId)
    redis.call('HSET', cardKey, 'scope', 'top')
    return newViewCount
end

local rankCount = redis.call('ZCARD', rankKey)
if rankCount < rankSize then
    redis.call('ZADD', rankKey, newViewCount, videoId)
    redis.call('HSET', cardKey, 'scope', 'top')
    return newViewCount
end

local thresholdTuple = redis.call('ZRANGE', rankKey, 0, 0, 'WITHSCORES')
if thresholdTuple == nil or #thresholdTuple < 2 then
    redis.call('ZREMRANGEBYRANK', rankKey, 0, 0)
    redis.call('ZADD', rankKey, newViewCount, videoId)
    redis.call('HSET', cardKey, 'scope', 'top')
    return newViewCount
end

local thresholdVideoId = thresholdTuple[1]
local thresholdScore = tonumber(thresholdTuple[2])
if newViewCount > thresholdScore then
    redis.call('ZADD', rankKey, newViewCount, videoId)
    redis.call('HSET', cardKey, 'scope', 'top')
    redis.call('HSET', cardKeyPrefix .. thresholdVideoId, 'scope', 'ephemeral')
    redis.call('ZREMRANGEBYRANK', rankKey, 0, 0)
else
    redis.call('HSET', cardKey, 'scope', 'ephemeral')
end

return newViewCount
