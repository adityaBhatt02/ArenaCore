package com.arenacore.auth.controller;

import com.arenacore.auth.dto.AuthResponse;
import com.arenacore.auth.dto.PlayerRankResponse;
import com.arenacore.auth.dto.RegisterRequest;
import com.arenacore.auth.entity.Player;
import com.arenacore.auth.repository.PlayerRepository;
import com.arenacore.auth.security.JwtService;
import com.arenacore.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final PlayerRepository playerRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        Player player = authService.register(request.getUsername(), request.getPassword());

        AuthResponse response = new AuthResponse(player.getId(), player.getUsername(), jwtService.generateToken(player.getId(),player.getUsername()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody RegisterRequest request) {
        Player player = authService.validateLogin(request.getUsername(), request.getPassword());

        AuthResponse response = new AuthResponse(player.getId(), player.getUsername(), jwtService.generateToken(player.getId(),player.getUsername()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/rank")
    public ResponseEntity<PlayerRankResponse> getPlayerRank(@PathVariable Long id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        return ResponseEntity.ok(PlayerRankResponse.from(player.getId(), player.getUsername(), player.getMmr()));
    }
}
