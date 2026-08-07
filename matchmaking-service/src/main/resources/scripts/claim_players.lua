local key = KEYS[1]
local count = tonumber(ARGV[1])

local players = redis.call('ZRANGE', key, 0, count - 1)

if #players < count then
    return {}
end

for i, player in ipairs(players) do
    redis.call('ZREM', key, player)
end

return players