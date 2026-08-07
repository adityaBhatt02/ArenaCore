package com.arenacore.auth.service;

import com.arenacore.auth.entity.Player;
import com.arenacore.auth.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;

    public Player register(String username, String rawPassword) {
        if(playerRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken!");
        }

        String hashedPassword = passwordEncoder.encode(rawPassword);

        Player player = new Player();
        player.setUsername(username);
        player.setPassword(hashedPassword);
        // mmr defaults to 0 automatically

        return playerRepository.save(player);
    }

    public Player validateLogin(String username, String rawPassword) {
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if(!passwordEncoder.matches(rawPassword, player.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password!");
        }

        return player;
    }
}
