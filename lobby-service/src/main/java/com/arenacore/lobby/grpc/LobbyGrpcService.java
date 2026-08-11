package com.arenacore.lobby.grpc;

import com.arenacore.grpc.CreateLobbyRequest;
import com.arenacore.grpc.CreateLobbyResponse;
import com.arenacore.grpc.LobbyServiceGrpc;
import com.arenacore.grpc.PlayerInfo;
import com.arenacore.lobby.entity.Lobby;
import com.arenacore.lobby.entity.LobbyPlayer;
import com.arenacore.lobby.entity.Team;
import com.arenacore.lobby.repository.LobbyRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class LobbyGrpcService extends LobbyServiceGrpc.LobbyServiceImplBase {

    private final LobbyRepository lobbyRepository;

    @Override
    public void createLobby(CreateLobbyRequest request, StreamObserver<CreateLobbyResponse> responseObserver) {

        try {
            Lobby lobby = new Lobby();
            request.getTeamAList().forEach(p -> lobby.getPlayers().add(toLobbyPlayer(p, lobby, Team.TEAM_A)));
            request.getTeamBList().forEach(p -> lobby.getPlayers().add(toLobbyPlayer(p, lobby, Team.TEAM_B)));

            Lobby saved = lobbyRepository.save(lobby);
            CreateLobbyResponse response = CreateLobbyResponse.newBuilder()
                    .setLobbyId(saved.getId().toString())
                    .setStatus(saved.getStatus().name())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e){
            log.error("createLobby failed", e);
            responseObserver.onError(
                    Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException()
            );
        }
    }

    public LobbyPlayer toLobbyPlayer(PlayerInfo info, Lobby lobby, Team team) {
        LobbyPlayer lp = new LobbyPlayer();
        lp.setLobby(lobby);
        lp.setPlayerId(info.getPlayerId());
        lp.setUsername(info.getUsername());
        lp.setTeam(team);
        return lp;
    }
}
