package com.arenacore.auth.model;

public enum Rank {
    BRONZE(0, 999),
    SILVER(1000, 1199),
    GOLD(1200, 1399),
    PLATINUM(1400, 1599),
    DIAMOND(1600, 1799),
    ASCENDANT(1800, 1999),
    IMMORTAL(2000, Integer.MAX_VALUE);

    private final int minMmr;
    private final int maxMmr;

    Rank(int minMmr, int maxMmr) {
        this.minMmr = minMmr;
        this.maxMmr = maxMmr;
    }

    public static Rank fromMmr(int mmr) {
        for(Rank rank: values()) {
            if(mmr >= rank.minMmr && mmr <= rank.maxMmr) return rank;
        }
        return BRONZE;             // fallback, should never hit this
    }
}
