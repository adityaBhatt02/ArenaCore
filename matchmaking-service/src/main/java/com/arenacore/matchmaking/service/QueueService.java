package com.arenacore.matchmaking.service;

import com.arenacore.matchmaking.model.QueuedPlayer;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<QueuedPlayer> claimPlayers(int count, int maxMmrRange) {
        ResourceScriptSource scriptSource = new ResourceScriptSource(new ClassPathResource("scripts/claim_players.lua"));
        RedisScript script = RedisScript.of(scriptSource.getResource(), List.class);

        RedisSerializer stringSerializer = redisTemplate.getStringSerializer();

        Object result = redisTemplate.execute(
                script,
                stringSerializer,
                stringSerializer,
                Collections.singletonList(QUEUE_KEY),
                String.valueOf(count),
                String.valueOf(maxMmrRange)
        );

        List<String> rawResults = (List<String>) result;

        if (rawResults == null || rawResults.isEmpty()) {
            return Collections.emptyList();
        }

        return rawResults.stream()
                .map(json -> objectMapper.readValue(json, QueuedPlayer.class))
                .collect(Collectors.toList());
    }
}
