package com.arenacore.matchmaking.controller;

import com.arenacore.grpc.PlayerMmrResponse;
import com.arenacore.matchmaking.client.AuthServiceGrpcClient;
import com.arenacore.matchmaking.model.QueuedPlayer;
import com.arenacore.matchmaking.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;
    private final AuthServiceGrpcClient authServiceGrpcClient;

    @PostMapping("/join")
    public String joinQueue(@RequestHeader("X-Player-Id") Long playerId,
                            @RequestHeader("X-Player-Username") String username) {

        PlayerMmrResponse mmrResponse = authServiceGrpcClient.getPlayerMmr(playerId);

        QueuedPlayer player = new QueuedPlayer(playerId, username, mmrResponse.getMmr());
        queueService.joinQueue(player);

        return "Joined queue: " + username + " (MMR: " + mmrResponse.getMmr() + ")";
    }

    @GetMapping("/view")
    public Set<QueuedPlayer> viewQueue() {
        return queueService.viewQueue();
    }

    @GetMapping("/size")
    public Long queueSize() {
        return queueService.queueSize();
    }
}
