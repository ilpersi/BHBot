package com.github.ilpersi.BHBot;

/**
 * @author Betalord
 */
class Bounds {
    int x1, y1, x2, y2;

    Bounds(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    /**
     * This is an helper method to create Bounds using width and height. This should be useful to create new Bounds
     * starting from the information that you can get from GIMP tool
     * @param x1 x position on the image
     * @param y1 y position on the image
     * @param width width of the Bounds
     * @param height height of the Bounds
     * @return a new Bounds object created using width and height instead of 4 points (x1, y1, x2, y2)
     */
    static Bounds fromWidthHeight(int x1, int y1, int width, int height) {
        return new Bounds(x1, y1, x1+width, y1+height);
    }
}
