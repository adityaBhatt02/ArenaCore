package com.arenacore.lobby.service;

import com.arenacore.lobby.dto.LobbyStatusResponse;
import com.arenacore.lobby.entity.Lobby;
import com.arenacore.lobby.entity.LobbyPlayer;
import com.arenacore.lobby.entity.LobbyStatus;
import com.arenacore.lobby.repository.LobbyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LobbyService {

    private final LobbyRepository lobbyRepository;

    public LobbyStatusResponse markReady(String lobbyId, Long playerId) {
        Lobby lobby = lobbyRepository.findById(UUID.fromString(lobbyId))
                .orElseThrow(() -> new IllegalArgumentException("Lobby not found"));

        LobbyPlayer player = lobby.getPlayers().stream()
                .filter(p -> p.getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not in this lobby"));

        player.setReady(true);

        boolean allReady = lobby.getPlayers().stream().allMatch(LobbyPlayer::getReady);
        if (allReady) {
            lobby.setStatus(LobbyStatus.IN_PROGRESS);
            // TODO: PUBLISH MatchStarted event to Kafka here, once Kafka is wired up.
        }

        Lobby saved = lobbyRepository.save(lobby);
        return toResponse(saved);
    }

    public LobbyStatusResponse getLobbyStatus(String lobbyId) {
        Lobby lobby = lobbyRepository.findById(UUID.fromString(lobbyId))
                .orElseThrow(() -> new IllegalArgumentException("Lobby not found"));

        return toResponse(lobby);
    }

    private LobbyStatusResponse toResponse(Lobby lobby) {
        List<LobbyStatusResponse.PlayerReadyInfo> players = lobby.getPlayers().stream()
                .map(p -> new LobbyStatusResponse.PlayerReadyInfo(
                        p.getPlayerId(), p.getUsername(), p.getTeam().name(), p.getReady()))
                .toList();

        return new LobbyStatusResponse(lobby.getId().toString(), lobby.getStatus().name(), players);
    }
}
