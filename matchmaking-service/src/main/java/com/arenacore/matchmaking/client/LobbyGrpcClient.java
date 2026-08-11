package com.arenacore.matchmaking.client;

import com.arenacore.grpc.CreateLobbyRequest;
import com.arenacore.grpc.CreateLobbyResponse;
import com.arenacore.grpc.LobbyServiceGrpc;
import com.arenacore.grpc.PlayerInfo;
import com.arenacore.matchmaking.model.QueuedPlayer;
import com.arenacore.matchmaking.model.TeamAssignment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LobbyGrpcClient {

    private final LobbyServiceGrpc.LobbyServiceBlockingStub lobbyServiceStub;

    public CreateLobbyResponse createLobby(TeamAssignment teams) {
        CreateLobbyRequest request = CreateLobbyRequest.newBuilder()
                .addAllTeamA(toPlayerInfoList(teams.getTeamA()))
                .addAllTeamB(toPlayerInfoList(teams.getTeamB()))
                .build();

        return lobbyServiceStub.createLobby(request);
    }

    private List<PlayerInfo> toPlayerInfoList(List<QueuedPlayer> players) {
        return players.stream()
                .map(p -> PlayerInfo.newBuilder()
                        .setPlayerId(p.getPlayerId())
                        .setUsername(p.getUsername())
                        .setMmr(p.getMmr())
                        .build())
                .toList();
    }
}
