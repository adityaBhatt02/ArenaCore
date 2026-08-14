package com.arenacore.lobby.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchStartedEvent {
    private String lobbyId;
    private List<Long> teamAPlayerIds;
    private List<Long> teamBPlayerIds;
    private Instant startedAt;
}
