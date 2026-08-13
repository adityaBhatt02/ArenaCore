package com.arenacore.lobby.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

    @Entity
    @Table(name = "lobby_players")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class LobbyPlayer {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "lobby_id", nullable = false)
        private Lobby lobby;

        @Column(nullable = false)
        private Long playerId;      // references Auth Service's player, no FK across services.

        @Column(nullable = false)
        private String username;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private Team team;         // TEAM_A or TEAM_B

        @Column(nullable = false)
        private Boolean ready = false;
    }
