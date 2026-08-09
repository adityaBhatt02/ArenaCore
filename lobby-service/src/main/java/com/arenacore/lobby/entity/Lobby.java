package com.arenacore.lobby.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lobbies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lobby {

    @Id
    private String id = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LobbyStatus status = LobbyStatus.WAITING_FOR_READY;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String teamAPlayerIds;                   // comma-seperated player id's for now

    @Column(nullable = false, columnDefinition = "TEXT")
    private String teamBPlayerIds;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
