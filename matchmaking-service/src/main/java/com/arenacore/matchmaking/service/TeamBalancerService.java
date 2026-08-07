package com.arenacore.matchmaking.service;

import com.arenacore.matchmaking.model.QueuedPlayer;
import com.arenacore.matchmaking.model.TeamAssignment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TeamBalancerService {

    public TeamAssignment balanceTeam(List<QueuedPlayer> claimedPlayers) {
        List<QueuedPlayer> sorted = new ArrayList<>(claimedPlayers);
        sorted.sort(Comparator.comparingInt(QueuedPlayer::getMmr).reversed());

        List<QueuedPlayer> teamA = new ArrayList<>();
        List<QueuedPlayer> teamB = new ArrayList<>();

        int totalA = 0 , totalB = 0;

        // Always give the next player to whichever team currently has the lower total MMR
        for(QueuedPlayer player : sorted) {
            if(totalA <= totalB) {
                teamA.add(player);
                totalA += player.getMmr();
            }else {
                teamB.add(player);
                totalB += player.getMmr();
            }
        }
        return new TeamAssignment(teamA, teamB);
    }
}
