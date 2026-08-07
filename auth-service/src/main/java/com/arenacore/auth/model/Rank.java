package com.arenacore.auth.model;

import lombok.Getter;

@Getter
public enum Rank {
    BRONZE(0, 299),
    SILVER(300, 599),
    GOLD(600, 899),
    PLATINUM(900, 1199),
    DIAMOND(1200, 1499),
    ASCENDANT(1500, 1799),
    IMMORTAL(1800, Integer.MAX_VALUE);

    private final int minMmr;
    private final int maxMmr;

    Rank(int minMmr, int maxMmr) {
        this.minMmr = minMmr;
        this.maxMmr = maxMmr;
    }

    public static Rank fromMmr(int mmr) {
        for (Rank rank : values()) {
            if (mmr >= rank.minMmr && mmr <= rank.maxMmr) {
                return rank;
            }
        }
        throw new IllegalArgumentException("Invalid MMR: " + mmr);
    }
}