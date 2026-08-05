package com.arenacore.matchmaking.client;

import com.arenacore.grpc.PlayerMmrRequest;
import com.arenacore.grpc.PlayerMmrResponse;
import com.arenacore.grpc.PlayerServiceGrpc;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;



@Component
@RequiredArgsConstructor
public class AuthServiceGrpcClient {

    private final PlayerServiceGrpc.PlayerServiceBlockingStub playerServiceStub;

    public PlayerMmrResponse getPlayerMmr(Long playerId) {
        PlayerMmrRequest request = PlayerMmrRequest.newBuilder()
                .setPlayerId(playerId)
                .build();
        return playerServiceStub.getPlayerMmr(request);
    }



}
