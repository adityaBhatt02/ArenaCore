package com.arenacore.lobby.service;

import com.arenacore.lobby.dto.LobbyStatusResponse;
import com.arenacore.lobby.entity.Lobby;
import com.arenacore.lobby.entity.LobbyPlayer;
import com.arenacore.lobby.entity.LobbyStatus;
import com.arenacore.lobby.entity.Team;
import com.arenacore.lobby.event.MatchStartedEvent;
import com.arenacore.lobby.repository.LobbyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LobbyService {

    private static final String MATCH_STARTED_TOPIC = "match-started";

    private final LobbyRepository lobbyRepository;
    private final KafkaTemplate<String, MatchStartedEvent> kafkaTemplate;

    public LobbyStatusResponse markReady(String lobbyId, Long playerId) {
        Lobby lobby = lobbyRepository.findById(UUID.fromString(lobbyId))
                .orElseThrow(() -> new IllegalArgumentException("Lobby not found"));

        LobbyPlayer player = lobby.getPlayers().stream()
                .filter(p -> p.getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not in this lobby"));

        player.setReady(true);

        boolean allReady = lobby.getPlayers().stream()
                .allMatch(LobbyPlayer::getReady);

        if (allReady) {
            lobby.setStatus(LobbyStatus.IN_PROGRESS);
            publishMatchStarted(lobby);                // PUBLISH MatchStarted event to Kafka.
        }

        Lobby saved = lobbyRepository.save(lobby);
        return toResponse(saved);
    }

    public LobbyStatusResponse getLobbyStatus(String lobbyId) {
        Lobby lobby = lobbyRepository.findById(UUID.fromString(lobbyId))
                .orElseThrow(() -> new IllegalArgumentException("Lobby not found"));

        return toResponse(lobby);
    }

    private void publishMatchStarted(Lobby lobby) {
        List<Long> teamAIds = lobby.getPlayers().stream()
                .filter(p -> p.getTeam() == Team.TEAM_A)
                .map(LobbyPlayer::getPlayerId)
                .toList();

        List<Long> teamBIds = lobby.getPlayers().stream()
                .filter(p -> p.getTeam() == Team.TEAM_B)
                .map(LobbyPlayer::getPlayerId)
                .toList();

        MatchStartedEvent event = new MatchStartedEvent(lobby.getId().toString(), teamAIds, teamBIds, Instant.now());

        kafkaTemplate.send(MATCH_STARTED_TOPIC, lobby.getId().toString(), event);
    }

    private LobbyStatusResponse toResponse(Lobby lobby) {
        List<LobbyStatusResponse.PlayerReadyInfo> players = lobby.getPlayers().stream()
                .map(p -> new LobbyStatusResponse.PlayerReadyInfo(
                        p.getPlayerId(),
                        p.getUsername(),
                        p.getTeam().name(),
                        p.getReady()))
                .toList();

        return new LobbyStatusResponse(lobby.getId().toString(), lobby.getStatus().name(), players);
    }
}
