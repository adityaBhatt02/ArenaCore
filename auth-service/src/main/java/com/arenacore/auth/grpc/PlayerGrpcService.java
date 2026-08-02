package com.arenacore.auth.grpc;

import com.arenacore.auth.repository.PlayerRepository;
import com.arenacore.grpc.PlayerMmrRequest;
import com.arenacore.grpc.PlayerMmrResponse;
import com.arenacore.grpc.PlayerServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class PlayerGrpcService extends PlayerServiceGrpc.PlayerServiceImplBase {

    private final PlayerRepository playerRepository;

    @Override
    public void getPlayerMmr(PlayerMmrRequest request, StreamObserver<PlayerMmrResponse> responseObserver) {
        var player = playerRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new RuntimeException("Player not found"));

        PlayerMmrResponse response = PlayerMmrResponse.newBuilder()
                .setPlayerId(player.getId())
                .setMmr(player.getMmr())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
