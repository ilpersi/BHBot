package com.github.ilpersi.BHBot;

/**
 * "Number on screen". Used with number detection algorithm (when detecting selected difficulty level, for example).
 *
 * @author Betalord
 */
public class ScreenNum implements Comparable<ScreenNum> {
    String value; // [0..9]
    private int xpos; // horizontal position on screen (relative to the detected image, for example)

    ScreenNum(String value, int xpos) {
        this.value = value;
        this.xpos = xpos;
    }

    public int compareTo(ScreenNum other) {
        return Integer.compare(this.xpos, other.xpos);
    }
}
