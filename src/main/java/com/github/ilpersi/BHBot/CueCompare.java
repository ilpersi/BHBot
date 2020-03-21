package com.github.ilpersi.BHBot;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * This is a stand-alone class to be used as an utility. The goal of this class is to provide a tool to compare cues.
 * Using the img1Path and img2Path, it is possible to specify the two cues you want to compare
 * As output of the tool, in the path specified in imgOutPath, the tool will create a new cue that is the result of
 * the comparison: in this resulting cue only the pixels that are equal between the two imput cues will have a color,
 * the remaining pixels will be transparent.
 *
 * This tool is useful when you want to easily remove changing background from a cue or you want to spot differences
 * between two cues that apparently seems the same.
 *
 * This tool has been created to ease the porting of BHBot to Steam and can be used for other meaning also.
 */
public class CueCompare {
    /*
        cueCompare compares the pixels in the bounds of two images, and divides matching pixels by the area pixel count
        to return a percent similarity match.

        If the result is more than the sensitivity (decimal percentage) it will return true
     */

    public static boolean imageDifference(BufferedImage img1, BufferedImage img2, double sensitivity, int x1, int x2, int y1, int y2) {
        int totalPixels = ((x2 - x1) * (y2 - y1));
        double matchingPixels = 0;
        double result;

        if (img1 != null && img2 != null) {

        // Buffered image to handle the output
        int[][] pixelMatrix1 = Misc.convertTo2D(img1);
        int[][] pixelMatrix2 = Misc.convertTo2D(img2);

            for (int y = y1; y < y2; y++) {
                for (int x = x1; x < x2; x++) {
                    //we check each pixel, if it matchs we increase the counter
                    if (pixelMatrix1[x][y] == pixelMatrix2[x][y]) {
                        matchingPixels++;
                    }
                }
            }
            result = matchingPixels / totalPixels;
            BHBot.logger.trace(matchingPixels);
            BHBot.logger.trace(totalPixels);
            BHBot.logger.trace(String.format("%.2f%%",result * 100) + " similarity between last two screenshots.");
            return result >= sensitivity;
        } else {
            BHBot.logger.error("Compare failed.");
            return false;
        }
    }

    public static void main(String[] args) {
        // Images paths
        String img1Path = "steam_restores_1.png";
        String img2Path = "steam_restores_2.png";
        String imgOutPath = "cue_comparison.png";

        // Buffered images
        BufferedImage img1 = null;
        BufferedImage img2 = null;
        BufferedImage img3;

        try {
            img1 = ImageIO.read(new File(img1Path));
            img2 = ImageIO.read(new File(img2Path));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // If both the input cues are ok, we go on with the processing
        if (img1 != null && img2 != null) {
            // To allow the comparison, image cues need to be of the same size
            if (img1.getWidth() == img2.getWidth() && img1.getHeight() == img2.getHeight()) {

                // Buffered image to handle the output
                img3 = new BufferedImage(img1.getWidth(), img1.getHeight(), BufferedImage.TYPE_INT_ARGB);

                int[][] pixelMatrix1 = Misc.convertTo2D(img1);
                int[][] pixelMatrix2 = Misc.convertTo2D(img2);

                for (int y = 0; y < img1.getHeight(); y++) {
                    for (int x = 0; x < img1.getWidth(); x++) {
                        if (pixelMatrix1[x][y] == pixelMatrix2[x][y]) {
                            // If both images have the same color in the same pixel, this is copied to the output image
                            img3.setRGB(x, y, pixelMatrix1[x][y]);
                        } else {
                            // If color of the same pixel is different, a transparent pixel is created in the destination image
                            img3.setRGB(x, y, 0);
                        }
                    }

                }

                File nameImgFile = new File(imgOutPath);
                try {
                    ImageIO.write(img3, "png", nameImgFile);
                } catch (IOException e) {
                    BHBot.logger.error("Error while creating comparison file", e);
                }

            } else {
                System.out.printf("Img 1 W:%d H:%d -- Img 2 W:%d H:%d", img1.getWidth(), img1.getHeight(), img2.getWidth(), img2.getHeight());
            }
        }
    }
}
