package com.arenacore.matchmaking.service;

import com.arenacore.matchmaking.model.QueuedPlayer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class QueueService {

    private static final String QUEUE_KEY = "matchmaking:queue";

    private final RedisTemplate<String, QueuedPlayer> redisTemplate;

    public void joinQueue(QueuedPlayer player) {
        ZSetOperations<String, QueuedPlayer> zSetOps = redisTemplate.opsForZSet();
        zSetOps.add(QUEUE_KEY, player, player.getMmr());
    }

    public Set<QueuedPlayer> viewQueue() {
        ZSetOperations<String, QueuedPlayer> zSetOps = redisTemplate.opsForZSet();
        return zSetOps.range(QUEUE_KEY, 0, -1);
    }

    public Long queueSize() {
        return redisTemplate.opsForZSet().zCard(QUEUE_KEY);
    }
}
