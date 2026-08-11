package com.arenacore.matchmaking.config;

import com.arenacore.grpc.LobbyServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class LobbyGrpcClientConfig {

    @Bean
    public LobbyServiceGrpc.LobbyServiceBlockingStub lobbyServiceStub(GrpcChannelFactory channels) {
        return LobbyServiceGrpc.newBlockingStub(channels.createChannel("localhost:9091"));
    }
}
