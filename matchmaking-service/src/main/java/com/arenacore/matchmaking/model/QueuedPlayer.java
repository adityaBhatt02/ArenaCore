package com.arenacore.matchmaking.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueuedPlayer {
    private Long playerId;
    private String username;
    private Integer mmr;
}
