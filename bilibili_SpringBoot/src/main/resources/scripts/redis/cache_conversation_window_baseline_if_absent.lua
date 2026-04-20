local listKey = KEYS[1]
local metaKey = KEYS[2]
local initKey = KEYS[3]

local conversationId = ARGV[1]
local baselineJson = ARGV[2]
local score = tonumber(ARGV[3])
local ttlSeconds = tonumber(ARGV[4])

local existing = redis.call("HGET", metaKey, conversationId)
if existing ~= false and existing ~= nil and existing ~= "" then
    redis.call("EXPIRE", listKey, ttlSeconds)
    redis.call("EXPIRE", metaKey, ttlSeconds)
    redis.call("EXPIRE", initKey, ttlSeconds)
    return existing
end

redis.call("HSET", metaKey, conversationId, baselineJson)
redis.call("ZADD", listKey, score, conversationId)
redis.call("EXPIRE", listKey, ttlSeconds)
redis.call("EXPIRE", metaKey, ttlSeconds)
redis.call("EXPIRE", initKey, ttlSeconds)
return baselineJson
