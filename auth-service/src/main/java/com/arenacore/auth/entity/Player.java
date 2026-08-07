package com.arenacore.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;        // stores the Bcrypt Hash

    @Column(nullable = false)
    private Integer mmr = 0;       // starting skill rating for matchmaking

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
