package com.github.ilpersi.BHBot;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * This is a stand-alone class to be used as an utility. The goal of this class is to provide a tool to work with
 * black and white images in a fast way.
 * As output of the tool, in the path specified in args[0], the tool will create a copy of the original files converted
 * in black and white scale. This is useful if you need to create new b&w images and you don't want to create them one
 * by one.
 *
 * This tool can be used for other meaning also.
 */
public class BlackWhiteTools {
    public static void main(String[] args) {

        if (args.length > 0) {
            folderToBlackWhite(args[0]);
            //testWBPlayersTS(args[0]);
            //testInvasion(args[0]);
        }
    }

    @SuppressWarnings("unused")
    static void folderToBlackWhite(String folderPath) {
        File imgFolder = new File(folderPath);

        if (imgFolder.exists() && imgFolder.isDirectory()) {
            File[] folderFiles = imgFolder.listFiles();
            if (folderFiles != null) {
                for (final File fileEntry : folderFiles) {
                    if (fileEntry.isDirectory()) {
                        folderToBlackWhite(fileEntry.getAbsolutePath());
                    } else if (!"BlackWhite_".equals(fileEntry.getName().substring(0, 11))) {
                        BufferedImage screenImg;
                        try {
                            screenImg = ImageIO.read(fileEntry);

                            MarvinImage origImg = new MarvinImage(screenImg);
                            /*
                             * ATTENTION IF YOU WANT THIS TO WORK CORRECTLY, REMEMBER TO CHECK THE PARAMETERS
                             * OF THE TOBLACKWHITE METHOD
                             */
                            origImg.toBlackWhite(new Color(25, 25, 25), new Color(255, 255, 255), 254);
                            BufferedImage bwImage = origImg.getBufferedImage();

                            String fileName = "BlackWhite_" + fileEntry.getName();
                            String newFilePath = fileEntry.getAbsolutePath().replace(fileEntry.getName(), fileName);

                            File outputFile = new File(newFilePath);
                            if (outputFile.exists()) {
                                if (!outputFile.delete()) {
                                    System.out.println("Impossible to delete " + newFilePath);
                                } else {
                                    outputFile = new File(newFilePath);
                                }
                            }
                            ImageIO.write(bwImage, "png", outputFile);
                        } catch (IOException e) {
                            System.out.println("Error when loading game screen ");
                        }
                    }
                }
            }
        }
    }

    static void testWBPlayersTS(String screenPath) {
        File imgFile = new File(screenPath);
        int invitesCnt = 4;

        if (imgFile.exists()) {
            BufferedImage screenImg;
            try {
                screenImg = ImageIO.read(imgFile);
                Bounds TSBound = Bounds.fromWidthHeight(184, 241, 84, 18);

                for (int partyMemberPos = 0; partyMemberPos < invitesCnt - 1; partyMemberPos++) {
                    MarvinImage subImg = new MarvinImage(screenImg.getSubimage(TSBound.x1, TSBound.y1 + (54 * partyMemberPos), TSBound.width, TSBound.height));
                    subImg.toBlackWhite(new Color(20, 20, 20), new Color(203, 203, 203), 203);
                    BufferedImage subimagetestbw = subImg.getBufferedImage();

                }

            } catch (IOException e) {
                System.out.println("Error when loading game screen ");
            }


        }
    }

    /**
     * This method is used to test the B&W tools with the invasion final screen to read the level you reached
     * @param screenPath Path to the screen to be used to check the invasion reading logic
     */
    @SuppressWarnings("unused")
    static void testInvasion(String screenPath) {
        File imgFile = new File(screenPath);

        if (imgFile.exists()) {
            BufferedImage screenImg;
            try {
                screenImg = ImageIO.read(imgFile);

                MarvinImage subImg = new MarvinImage(screenImg.getSubimage(375, 20, 55, 20));
                subImg.toBlackWhite(new Color(10, 11, 13), new Color(64, 64, 64), 0);
                BufferedImage subimagetestbw = subImg.getBufferedImage();

            } catch (IOException e) {
                System.out.println("Error when loading game screen ");
            }


        }
    }
}
