package com.arenacore.matchmaking.service;

import com.arenacore.matchmaking.model.QueuedPlayer;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QueueService {

    private static final String QUEUE_KEY = "matchmaking:queue";

    private final RedisTemplate<String, QueuedPlayer> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @SuppressWarnings("unchecked")
    public List<QueuedPlayer> claimPlayers(int count) {
        ResourceScriptSource scriptSource = new ResourceScriptSource(new ClassPathResource("scripts/claim_players.lua"));
        RedisScript<List> script = RedisScript.of(scriptSource.getResource(), List.class);

        List<String> rawResults = redisTemplate.execute(
                script,
                Collections.singletonList(QUEUE_KEY),
                String.valueOf(count)
        );

        if(rawResults == null || rawResults.isEmpty()) return Collections.emptyList();

        return rawResults.stream()
                .map(json -> objectMapper.readValue(json, QueuedPlayer.class))
                .collect(Collectors.toList());
    }
}
