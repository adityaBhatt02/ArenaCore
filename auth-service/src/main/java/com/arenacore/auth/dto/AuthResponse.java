package com.arenacore.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private Long playerId;
    private String username;
    private String token;
}
