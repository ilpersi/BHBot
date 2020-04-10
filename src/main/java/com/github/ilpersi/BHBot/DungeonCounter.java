package com.github.ilpersi.BHBot;

class DungeonCounter {

    // Counters and rates
    private int victories;
    private int defeats;
    private int total;
    private int victoriesInARow;
    private int defeatsInARow;

    private Long totalVictoryDuration;
    private Long victoryAverageDuration;
    private Long totalDefeatDuration;
    private Long defeatAverageDuration;

    // String description for success rate
    private String successRateDesc;

    DungeonCounter(int victoryCnt, int defeatCnt) {

        // We initialize all the properties
        this.victories = victoryCnt;
        this.defeats = defeatCnt;
        this.total = victoryCnt + defeatCnt;
        this.victoriesInARow = 0;
        this.defeatsInARow = 0;

        this.totalVictoryDuration = 0L;
        this.victoryAverageDuration = 0L;
        this.totalDefeatDuration = 0L;
        this.defeatAverageDuration = 0L;
    }

    private void  updateTotal() {
        total = victories + defeats;

        updateSuccesRate();
    }

    private void updateSuccesRate() {
        double succesRate = total > 0 ? ((double) victories / (total)) * 100 : 0.0;

        successRateDesc = "success rate is "
                .concat(String.format("%.02f%%", succesRate))
                .concat(" W:").concat("" + this.victories)
                .concat(" L:").concat("" + this.defeats)
                .concat(".");
    }

    void increaseVictories() {
        victories += 1;

        victoriesInARow += 1;
        defeatsInARow = 0;

        updateTotal();
    }

    void increaseVictoriesDuration(long milliSeconds) {
        totalVictoryDuration += milliSeconds;

        if (victories > 0) victoryAverageDuration = totalVictoryDuration / victories;
    }

    void increaseDefeatsDuration(long milliSeconds) {
        totalDefeatDuration += milliSeconds;

        if (defeats > 0) defeatAverageDuration = totalDefeatDuration / defeats;
    }

    void increaseDefeats() {
        defeats += 1;

        defeatsInARow += 1;
        victoriesInARow = 0;

        updateTotal();
    }

    int getTotal() {
        return this.total;
    }

    int getVictoriesInARow() { return this.victoriesInARow;}

    @SuppressWarnings("unused")
    int getDefeatsInARow() { return this.defeatsInARow;}

    String successRateDesc() {
        return successRateDesc;
    }

    long getVictoryAverageDuration() {return  victoryAverageDuration;}

    long getDefeatAverageDuration() {return  defeatAverageDuration;}

}
