package com.arenacore.matchmaking.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TeamAssignment {
    private List<QueuedPlayer> teamA;
    private List<QueuedPlayer> teamB;
}
