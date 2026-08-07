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

        boolean assignToA = true;
        for(QueuedPlayer player : sorted) {
            if(assignToA) {
                teamA.add(player);
            }else {
                teamB.add(player);
            }
            assignToA = !assignToA;
        }
        return new TeamAssignment(teamA, teamB);
    }
}
