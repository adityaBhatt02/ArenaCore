package com.arenacore.lobby.controller;

import com.arenacore.lobby.dto.LobbyStatusResponse;
import com.arenacore.lobby.service.LobbyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lobby")
@RequiredArgsConstructor
public class LobbyController {

    private final LobbyService lobbyService;

    @PostMapping("/{lobbyId}/ready")
    public LobbyStatusResponse ready(@PathVariable String lobbyId, @RequestHeader("X-Player-Id") Long playerId) {
        return lobbyService.markReady(lobbyId, playerId);
    }

    @GetMapping("/{lobbyId}")
    public LobbyStatusResponse getStatus(@PathVariable String lobbyId) {
        return lobbyService.getLobbyStatus(lobbyId);
    }
}
