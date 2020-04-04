package com.github.ilpersi.BHBot;

import java.awt.image.BufferedImage;

/**
 * @author Betalord
 */
public class Cue {
    public String name;
    BufferedImage im;
    Bounds bounds;

    @SuppressWarnings("unused")
    public Cue(String name, BufferedImage im) {
        this.name = name;
        this.im = im;
        bounds = null;
    }

    Cue(String name, BufferedImage im, Bounds bounds) {
        this.name = name;
        this.im = im;
        this.bounds = bounds;
    }

    Cue(Cue cue, Bounds bounds) {
        this.name = cue.name;
        this.im = cue.im;
        this.bounds = bounds;
    }
}
