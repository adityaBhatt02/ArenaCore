package com.arenacore.matchmaking.config;

import com.arenacore.grpc.PlayerServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class AuthGrpcClientConfig {

    @Bean
    PlayerServiceGrpc.PlayerServiceBlockingStub playerServiceStub(GrpcChannelFactory channels) {
        return PlayerServiceGrpc.newBlockingStub(channels.createChannel("auth-service"));
    }
}