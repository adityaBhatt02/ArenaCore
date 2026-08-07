package com.arenacore.auth.model;

public class RankCalculator {

    public static Division getDivision(int mmr) {
        Rank rank = Rank.fromMmr(mmr);

        if(rank == Rank.IMMORTAL) return null;                     // No division at top tier

        int range = rank.getMaxMmr() - rank.getMinMmr() + 1;       // GOLD: 899 - 600 + 1 = 300 total MMR values in this rank
        int section = range/3;

        int offset = mmr - rank.getMinMmr();

        if(offset < section) return Division.III;
        if(offset < section * 2) return Division.II;

        return Division.I;
    }

    public static String getDisplayName(int mmr) {
        Rank rank = Rank.fromMmr(mmr);
        Division division = getDivision(mmr);

        if(division == null) return rank.name();                // IMMORTAL
        return rank.name() + " " + division.name();
    }
}
