package com.arenacore.auth.dto;

import com.arenacore.auth.model.Rank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerRankResponse {
    private Long playerId;
    private String username;
    private String rank;

    public static PlayerRankResponse from(Long playerId, String username, Integer mmr) {
        Rank rank = Rank.fromMmr(mmr);
        return new PlayerRankResponse(playerId, username, rank.name());
    }
}
