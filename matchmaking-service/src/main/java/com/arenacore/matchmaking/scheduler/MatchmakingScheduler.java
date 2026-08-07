package com.arenacore.matchmaking.scheduler;

import com.arenacore.matchmaking.model.QueuedPlayer;
import com.arenacore.matchmaking.model.TeamAssignment;
import com.arenacore.matchmaking.service.QueueService;
import com.arenacore.matchmaking.service.TeamBalancerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchmakingScheduler {

    private static final int PLAYERS_PER_MATCH = 10;
    private static final int MAX_MMR_RANGE = 150;

    private final QueueService queueService;
    private final TeamBalancerService teamBalancerService;

    @Scheduled(fixedRate = 5000)
    public void tryFormMatch() {
        List<QueuedPlayer> claimed = queueService.claimPlayers(PLAYERS_PER_MATCH, MAX_MMR_RANGE);

        if(claimed.isEmpty()) {
            log.info("Not enough players in queue to form a match.");
            return;
        }

        TeamAssignment teams = teamBalancerService.balanceTeam(claimed);

        log.info("Match formed! Team A: {}", teams.getTeamA());
        log.info("Match formed! Team B: {}", teams.getTeamB());

        // TODO: call Lobby Service via gRPC here once it exists
    }
}
