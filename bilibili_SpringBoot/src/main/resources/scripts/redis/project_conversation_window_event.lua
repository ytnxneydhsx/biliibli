local listKey = KEYS[1]
local metaKey = KEYS[2]
local initKey = KEYS[3]
local processedKey = KEYS[4]

local conversationId = ARGV[1]
local targetId = ARGV[2]
local lastMessage = ARGV[3]
local lastMessageTime = ARGV[4]
local serverMessageId = ARGV[5]
local incrementUnread = ARGV[6]
local ttlSeconds = tonumber(ARGV[7])
local score = tonumber(ARGV[8])

local function normalizeId(value)
    if value == nil then
        return nil
    end
    local text = tostring(value)
    if string.find(text, "[eE]") then
        return nil
    end
    text = string.gsub(text, "^%s*(.-)%s*$", "%1")
    text = string.gsub(text, "^0+", "")
    if text == "" then
        return "0"
    end
    return text
end

local function idGreater(left, right)
    left = normalizeId(left)
    right = normalizeId(right)
    if left == nil then
        return false
    end
    if right == nil then
        return true
    end
    if string.len(left) ~= string.len(right) then
        return string.len(left) > string.len(right)
    end
    return left > right
end

if redis.call("EXISTS", initKey) == 0 then
    return nil
end

local window = {}
local rawWindow = redis.call("HGET", metaKey, conversationId)
if rawWindow ~= false and rawWindow ~= nil and rawWindow ~= "" then
    local ok, decoded = pcall(cjson.decode, rawWindow)
    if ok and type(decoded) == "table" then
        window = decoded
    end
end

local changed = false
local currentLast = normalizeId(window["lastServerMessageId"])
local baseline = window["unreadBaselineServerMessageIdText"]

if incrementUnread == "1" and (baseline == nil or idGreater(serverMessageId, baseline)) then
    local added = redis.call("SADD", processedKey, serverMessageId)
    if added == 1 then
        local unread = tonumber(window["unreadCount"]) or 0
        if unread < 0 then
            unread = 0
        end
        window["unreadCount"] = unread + 1
        changed = true
    end
end

if idGreater(serverMessageId, currentLast) then
    window["conversationId"] = conversationId
    window["targetId"] = tonumber(targetId)
    window["lastMessage"] = lastMessage
    window["lastMessageTime"] = lastMessageTime
    window["lastServerMessageId"] = serverMessageId
    if window["unreadCount"] == nil then
        window["unreadCount"] = 0
    end
    if window["isMuted"] == nil then
        window["isMuted"] = 0
    end
    changed = true
end

if not changed then
    redis.call("EXPIRE", listKey, ttlSeconds)
    redis.call("EXPIRE", metaKey, ttlSeconds)
    redis.call("EXPIRE", initKey, ttlSeconds)
    redis.call("EXPIRE", processedKey, ttlSeconds)
    return nil
end

if window["conversationId"] == nil then
    window["conversationId"] = conversationId
end
if window["targetId"] == nil then
    window["targetId"] = tonumber(targetId)
end
if window["unreadCount"] == nil then
    window["unreadCount"] = 0
end
if window["isMuted"] == nil then
    window["isMuted"] = 0
end

local encoded = cjson.encode(window)
redis.call("HSET", metaKey, conversationId, encoded)
redis.call("ZADD", listKey, score, conversationId)
redis.call("EXPIRE", listKey, ttlSeconds)
redis.call("EXPIRE", metaKey, ttlSeconds)
redis.call("EXPIRE", initKey, ttlSeconds)
redis.call("EXPIRE", processedKey, ttlSeconds)
return encoded
