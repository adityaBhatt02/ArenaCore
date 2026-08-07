local key = KEYS[1]
local count = tonumber(ARGV[1])
local maxRange = tonumber(ARGV[2])

-- Get everyone in the queue, with their scores (MMR), lowest to highest
local all = redis.call('ZRANGE', key, 0, -1, 'WITHSCORES')

if #all == 0 then
    return {}
end

-- 'all' is a flat list: [player1, score1, player2, score2, ...]
-- Convert it into pairs for easier handling
local players = {}
local scores = {}
local total = #all / 2

for i = 1, total do
    players[i] = all[(i - 1) * 2 + 1]
    scores[i] = tonumber(all[(i - 1) * 2 + 2])
end

-- Anchor = the lowest MMR player currently in queue
local anchorScore = scores[1]

-- Collect players within maxRange of the anchor
local candidates = {}
for i = 1, total do
    if (scores[i] - anchorScore) <= maxRange then
        table.insert(candidates, players[i])
    else
        break  -- since sorted by score, once we're out of range, everyone after is too
    end
end

-- Not enough players within range to form a match
if #candidates < count then
    return {}
end

-- Take exactly 'count' players from the candidates and claim them atomically
local claimed = {}
for i = 1, count do
    table.insert(claimed, candidates[i])
    redis.call('ZREM', key, candidates[i])
end

return claimed