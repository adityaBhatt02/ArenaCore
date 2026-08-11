package com.arenacore.lobby.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LobbyStatusResponse {
    private String lobbyId;
    private String lobbyStatus;
    private List<PlayerReadyInfo> players;

    @Data
    @AllArgsConstructor
    public static class PlayerReadyInfo {
        private Long playerId;
        private String username;
        private String team;
        private Boolean ready;
    }
}
