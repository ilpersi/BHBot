import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.*;

class DungeonCounter {

    // Counters and rates
    private IntegerProperty victories;
    private IntegerProperty defeats;
    private IntegerProperty total;

    // String description for success rate
    private StringExpression successRateDesc;

    DungeonCounter(int victoryCnt, int defeatCnt) {

        // We initialize all the properties
        this.victories = new SimpleIntegerProperty(victoryCnt);
        this.defeats = new SimpleIntegerProperty(defeatCnt);
        this.total = new SimpleIntegerProperty(victoryCnt + defeatCnt);
        DoubleProperty succesRate = new SimpleDoubleProperty(0);

        // We create the bindings
        total.bind(this.victories.add(this.defeats));

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
                    return (((double) DungeonCounter.this.victories.get() / (total.get())) * 100);
                }
            }
        };
        succesRate.bind(succesBinding);

        StringProperty initStr = new SimpleStringProperty("success rate is ");
        successRateDesc = initStr.concat(succesRate.asString("%.02f%%"))
                .concat(" W:").concat(this.victories.asString())
                .concat(" L:").concat(this.defeats.asString())
                .concat(".");
    }

    void increaseVictories(int inc) {
        victories.setValue(victories.get() + inc);
    }

    void increaseDefeats(int inc) {
        defeats.setValue(defeats.get() + inc);
    }

    int getTotal() {
        return this.total.get();
    }

    int getVictories() {
        return this.victories.get();
    }

    String successRateDesc() {
        return successRateDesc.getValue();
    }

}
