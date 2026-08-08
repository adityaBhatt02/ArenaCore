package com.arenacore.lobby.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(nullable = false)
    private String status = "WAITING_FOR_READY";     // WAITING_FOR_READY, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(nullable = false, columnDefinition = "TEXT")
    private String teamAPlayerIds;                   // comma-seperated player id's for now

    @Column(nullable = false, columnDefinition = "TEXT")
    private String teamBPlayerIds;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
