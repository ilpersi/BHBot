package com.github.ilpersi.BHBot;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.LongBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;

class DungeonCounter {

    // Counters and rates
    private IntegerProperty victories;
    private IntegerProperty defeats;
    private IntegerProperty total;
    private IntegerProperty victoriesInARow;
    private IntegerProperty defeatsInARow;

    private LongProperty totalVictoryDuration;
    private LongProperty victoryAverageDuration;
    private LongProperty totalDefeatDuration;
    private LongProperty defeatAverageDuration;

    // String description for success rate
    private StringExpression successRateDesc;

    DungeonCounter(int victoryCnt, int defeatCnt) {

        // We initialize all the properties
        this.victories = new SimpleIntegerProperty(victoryCnt);
        this.defeats = new SimpleIntegerProperty(defeatCnt);
        this.total = new SimpleIntegerProperty(victoryCnt + defeatCnt);
        this.victoriesInARow = new SimpleIntegerProperty(0);
        this.defeatsInARow = new SimpleIntegerProperty(0);
        DoubleProperty succesRate = new SimpleDoubleProperty(0);

        this.totalVictoryDuration = new SimpleLongProperty(0);
        this.victoryAverageDuration = new SimpleLongProperty(0);
        this.totalDefeatDuration = new SimpleLongProperty(0);
        this.defeatAverageDuration = new SimpleLongProperty(0);

        // We create the bindings
        total.bind(this.victories.add(this.defeats));

        // we use two listeners to manage vicotriesInARow
        victories.addListener(this::onVictoriesChange);
        defeats.addListener(this::onDefeatsChange);

        // We want to avoid any division by zero, so we create a lazy binding using an anonymous class
        DoubleBinding succesBinding = new DoubleBinding() {
            {
                this.bind(total);
            }

            @Override
            protected double computeValue() {
                if (total.get() == 0) {
                    return 0.0;
                } else {
                    return (((double) victories.get() / (total.get())) * 100);
                }
            }
        };
        succesRate.bind(succesBinding);

        // Duration management
        LongBinding victoryDurationBinding = new NoZeroDivision(totalVictoryDuration, victories);
        victoryAverageDuration.bind(victoryDurationBinding);
        LongBinding defeatDurationBinding = new NoZeroDivision(totalDefeatDuration, defeats);
        defeatAverageDuration.bind(defeatDurationBinding);

        StringProperty initStr = new SimpleStringProperty("success rate is ");
        successRateDesc = initStr.concat(succesRate.asString("%.02f%%"))
                .concat(" W:").concat(this.victories.asString())
                .concat(" L:").concat(this.defeats.asString())
                .concat(".");
    }

    void increaseVictories() {
        victories.setValue(victories.get() + 1);
    }

    void increaseVictoriesDuration(long milliSeconds) {
        totalVictoryDuration.setValue(totalVictoryDuration.get() + milliSeconds);
    }

    void increaseDefeatsDuration(long milliSeconds) {
        totalDefeatDuration.setValue(totalDefeatDuration.get() + milliSeconds);
    }

    void increaseDefeats() {
        defeats.setValue(defeats.get() + 1);
    }

    int getTotal() {
        return this.total.get();
    }

    int getVictoriesInARow() { return this.victoriesInARow.get();}

    // int getDefeatsInARow() { return this.defeatsInARow.get();}

    String successRateDesc() {
        return successRateDesc.getValue();
    }

    long getVictoryAverageDuration() {return  victoryAverageDuration.get();}

    long getDefeatAverageDuration() {return  defeatAverageDuration.get();}

    private void onVictoriesChange(ObservableValue<? extends Number> prop, Number oldValue, Number newValue) {
        this.defeatsInARow.setValue(0);
        this.victoriesInARow.setValue(victoriesInARow.get() + 1);
    }

    private void onDefeatsChange(ObservableValue<? extends Number> prop, Number oldValue, Number newValue) {
        this.victoriesInARow.setValue(0);
        this.defeatsInARow.setValue(defeatsInARow.get() + 1);
    }

}

class NoZeroDivision extends LongBinding {

    private LongProperty total;
    private IntegerProperty counter;

    NoZeroDivision (LongProperty numerator, IntegerProperty denominator) {
        total = new SimpleLongProperty(0);
        counter = new SimpleIntegerProperty(0);

        this.bind(numerator, denominator);
        total.bind(numerator);
        counter.bind(denominator);
    }

    @Override
    protected long computeValue() {
        if (counter.get() == 0) {
            return 0;
        } else {
            return total.get() / counter.get();
        }
    }
}
