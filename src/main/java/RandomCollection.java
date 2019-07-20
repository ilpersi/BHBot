import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * "Random item selection from a collection". Used to randomly select items from a map using a weight: for example when
 * the settings allow more than tone possibility.
 *
 * @author ilpersi
 */
public class RandomCollection<T> {
    private final NavigableMap<Double, T> map = new TreeMap<>();
    private double total = 0;

    void add(double weight, T result) {
        if (weight <= 0 || map.containsValue(result))
            return;
        total += weight;
        map.put(total, result);
    }

    T next() {
        if (map.size() == 0)
            return null;

        double value = ThreadLocalRandom.current().nextDouble() * total;
        return map.ceilingEntry(value).getValue();
    }

    public int size() {
        return map.size();
    }

    public void clear() {
        map.clear();
        total = 0;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<Double, T> entry : map.entrySet()) {
            String value = entry.getValue().toString();
            int key = entry.getKey().intValue();
            result.append(value);
            result.append(" ");
            result.append(key);
            result.append(";");
        }
        if (result.length() > 0)
            result = new StringBuilder(result.substring(0, result.length() - 1)); // remove last ";" character
        return result.toString();
    }
}
