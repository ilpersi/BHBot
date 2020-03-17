import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

/**
 * @author Betalord
 */
public class Misc {

    private static Class<Misc> miscClass = Misc.class;

    static final class Durations {
        static final int SECOND = 1000;
        static final int MINUTE = 60 * SECOND;
        static final int HOUR = 60 * MINUTE;
        static final int DAY = 24 * HOUR;
        static final int WEEK = 7 * DAY;
    }

    /**
     * Return time in milliseconds from the start of the system. Can have a negative value.
     */
    static long getTime() {
        return System.currentTimeMillis();
    }

    static String getStackTrace() {
        StringBuilder r = new StringBuilder();

        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            r.append(ste).append("\n");
        }

        return r.toString();
    }

    static List<String> readTextFile2(String file) throws FileNotFoundException {
        List<String> lines = new ArrayList<>();
        BufferedReader br;

        br = new BufferedReader(new FileReader(file));
        try {
            String line = br.readLine();
            while (line != null) {
                lines.add(line);
                line = br.readLine();
            }
            br.close();

            return lines;
        } catch (IOException e) {
            BHBot.logger.error("Impossible to read file: " + file, e);
            return null;
        }
    }

    /**
     * Returns true on success.
     */
    static boolean saveTextFile(String file, String contents) {
        BufferedWriter bw;

        try {
            File f = new File(file);
            // create parent folder(s) if needed:
            File parent = f.getParentFile();
            if (parent != null && !parent.exists())
                if (!parent.mkdirs()) {
                    BHBot.logger.error("Error with parent.mkdirs() in saveTetFile!");
                    return false;
                }

            bw = new BufferedWriter(new FileWriter(f));
            try {
                bw.write(contents);
            } finally {
                bw.close();
            }
        } catch (IOException e) {
            BHBot.logger.error("saveTextFile could not save contents in file: " + file, e);
            return false;
        }
        return true;
    }

    static String millisToHumanForm(Long millis) {

        // seconds
        long seconds = millis / 1000;
        // minutes
        long minutes = seconds / 60;
        seconds = seconds % 60;
        // hours
        long hours = minutes / 60;
        minutes = minutes % 60;
        // days
        long days = hours / 24;
        hours = hours % 24;

        if (seconds == 0 && minutes == 0 && hours == 0 && days == 0)
            return "0s";

        StringBuilder humanStringBuilder = new StringBuilder();
        if (days > 0) humanStringBuilder.append(String.format("%dd", days));
        if (hours > 0) humanStringBuilder.append(String.format(" %dh", hours));
        if (minutes > 0) humanStringBuilder.append(String.format(" %dm", minutes));
        if (seconds > 0) humanStringBuilder.append(String.format(" %ds", seconds));

        return humanStringBuilder.toString().trim();
    }

    static int max(int... values) {
        int max = Integer.MIN_VALUE;
        for (int value : values)
            if (value > max)
                max = value;
        return max;
    }

    static int min(int... values) {
        int min = Integer.MAX_VALUE;
        for (int value : values)
            if (value < min)
                min = value;
        return min;
    }

    /**
     * Returns index of closest match from the 'values' array.
     */
    static int findClosestMatch(int[] values, int value) {
        int best = Integer.MAX_VALUE;
        int bestIndex = -1;
        for (int i = 0; i < values.length; i++) {
            if (Math.abs(values[i] - value) < best) {
                best = Math.abs(values[i] - value);
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    static String listToString(EnumSet<?> list) {
        StringBuilder r = new StringBuilder();
        for (Object e : list)
            r.append(e).append(", ");
        r = new StringBuilder(r.substring(0, r.length() - 2));
        return r.toString();
    }

    static String encodeFileToBase64Binary(File toEncode) {

        byte[] encoded = new byte[0];
        try {
            encoded = Base64.encodeBase64(FileUtils.readFileToByteArray(toEncode));
        } catch (IOException e) {
            BHBot.logger.error("Error in encodeFileToBase64Binary", e);
        }
        return new String(encoded, StandardCharsets.US_ASCII);
    }

    static int[][] convertTo2D(BufferedImage image) {

        final int w = image.getWidth();
        final int h = image.getHeight();

        int[][] pixels = new int[w][h];

        for (int i = 0; i < w; i++)
            for (int j = 0; j < h; j++)
                pixels[i][j] = image.getRGB(i, j);

        return pixels;
    }

    static long classBuildTimeMillis() throws URISyntaxException, IllegalStateException, IllegalArgumentException {
        URL resource = miscClass.getResource(miscClass.getSimpleName() + ".class");
        if (resource == null) {
            throw new IllegalStateException("Failed to find class file for class: " +
                    miscClass.getName());
        }

        if (resource.getProtocol().equals("file")) {

            return new File(resource.toURI()).lastModified();

        } else if (resource.getProtocol().equals("jar")) {

            String path = resource.getPath();
            return new File(path.substring(5, path.indexOf("!"))).lastModified();

        } else {

            throw new IllegalArgumentException("Unhandled url protocol: " +
                    resource.getProtocol() + " for class: " +
                    miscClass.getName() + " resource: " + resource.toString());
        }
    }

    static Properties getGITInfo() {
        Properties properties = new Properties();
        try {
            InputStream gitResource = miscClass.getClassLoader().getResourceAsStream("git.properties");
            properties.load(gitResource);
        } catch (IOException e) {
            BHBot.logger.error("Impossible to get GIT information", e);
        }
        return properties;
    }

    static void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            BHBot.logger.error("Error while attempting to sleep", e);
        }
    }

}
