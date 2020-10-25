package com.github.ilpersi.BHBot;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * This is a stand-alone class to be used as an utility. The goal of this class is to provide a tool to compare cues.
 * Using the img1Path and img2Path, it is possible to specify the two cues you want to compare
 * As output of the tool, in the path specified in imgOutPath, the tool will create a new cue that is the result of
 * the comparison: in this resulting cue only the pixels that are equal between the two input cues will have a color,
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
                    //we check each pixel, if it matches we increase the counter
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

    static private void createDiffImage(File img1File, File img2File, String imgOutPath) {
        if (img1File != null && img2File != null) {
            BufferedImage img1;
            try {
                img1 = ImageIO.read(img1File);
            } catch (IOException e) {
                System.out.println("Error while reading image 1: " + img1File.getName());
                e.printStackTrace();
                return;
            }


            BufferedImage img2 = null;
            try {
                img2 = ImageIO.read(img2File);
            } catch (IOException e) {
                System.out.println("Error while reading image 2: " + img2File.getName());
                e.printStackTrace();
            }

            if (img1 != null && img2 != null) {
                // To allow the comparison, image cues need to be of the same size
                if (img1.getWidth() == img2.getWidth() && img1.getHeight() == img2.getHeight()) {

                    // Buffered image to handle the output
                    BufferedImage img3 = new BufferedImage(img1.getWidth(), img1.getHeight(), BufferedImage.TYPE_INT_ARGB);

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
                    System.out.printf("Img 1 %s W:%d H:%d -- Img 2 %s W:%d H:%d\n",
                            img1File.getName(), img1.getWidth(), img1.getHeight(), img2File.getName(), img2.getWidth(), img2.getHeight());
                }
            }
        }
    }

    public static void main(String[] args) {
        String flUsage = "USAGE with file list: CueCompare -fl|--file-list <inputImg1FilePath> <inputImg2FilePath> [... <inputImgNFilePath>] <outputImgFilePath>";
        String fpUsage = "USAGE with file path: CueCompare -fp|--file-path <inputFolderPath> <outputImgPath>";
        String MD5FLfpUsage = "USAGE with file list: CueCompare -md5fl|--md5-file-list <inputImg1FilePath> <inputImg2FilePath> [... <inputImgNFilePath>]";

        if ("-fl".equals(args[0]) || "--file-list".equals(args[0])) {
            if (args.length < 4) {
                System.out.println(flUsage);
                return;
            }

            int secondImgIdx = 2;

            for (int imgCnt = secondImgIdx; imgCnt <= (args.length - 2); imgCnt++) {

                // After the first for iteration the source image is the output of the previous comparison
                String img1Path;
                if (imgCnt == secondImgIdx) {
                    img1Path = args[imgCnt - 1];
                } else {
                    img1Path = args[args.length - 1];
                }

                // current image
                String img2Path = args[imgCnt];
                String imgOutPath = args[args.length - 1];

                File img1File = new File(img1Path);
                File img2File = new File(img2Path);

                createDiffImage(img1File, img2File, imgOutPath);


            }
        } else if ("-md5fl".equals(args[0]) || "--md5-file-list".equals(args[0])) {
            if (args.length < 2) {
                System.out.println(MD5FLfpUsage);
                return;
            }

            for (int imgCnt = 1; imgCnt < (args.length); imgCnt++) {

                // After the first for iteration the source image is the output of the previous comparison
                String imgPath = args[imgCnt];

                File imgFile = new File(imgPath);
                BufferedImage img;
                try {
                    img = ImageIO.read(imgFile);
                } catch (IOException e) {
                    System.out.println("Error while reading image " + imgFile.getAbsolutePath());
                    e.printStackTrace();
                    continue;
                }

                System.out.println(imgFile.getAbsolutePath() + " MD5 is : " + Misc.imgToMD5(img));


            }
        } else if ("-fp".equals(args[0]) || "--file-path".equals(args[0])) {
            if (args.length != 3) {
                System.out.println(fpUsage);
                return;
            }

            String folderPath = args[1];
            File folderFile = new File(folderPath);

            if (!folderFile.exists()) {
                System.out.println("Path does not exist: " + folderPath);
                return;
            }

            if (!folderFile.isDirectory()) {
                System.out.println("Path is not a directory: " + folderPath);
                return;
            }

            String outPath = args[2];
            File outputFile = new File(outPath);

            if (outputFile.isDirectory()) {
                System.out.println("outputImgPat parameter must not be a directory.");
                return;
            }

            int imgCnt = 1;
            File img1File = null;

            for (final File imgFile : folderFile.listFiles()) {
                if (imgFile.isDirectory()) {
                    System.out.println("Skipping directory: " + imgFile.getAbsolutePath());
                    //noinspection UnnecessaryContinue
                    continue;
                } else if (imgFile.getName().equals(outputFile.getName())) {
                    //noinspection UnnecessaryContinue
                    continue;
                } else {
                    String fileName = imgFile.getName();
                    String fileExtension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();

                    if (!".jpg".equals(fileExtension) && !".png".equals(fileExtension)) {
                        System.out.println("Skipping non image file: " + fileName);
                        continue;
                    }

                    if (imgCnt == 1) {
                        img1File = imgFile;
                        imgCnt++;
                        continue;
                    }

                    createDiffImage(img1File, imgFile, outPath);

                    img1File = new File(outPath);

                    imgCnt++;
                }
            }

            System.out.println("Merged " + imgCnt + " images.");


        } else {
            System.out.println(flUsage);
            System.out.println(fpUsage);
            System.out.println(MD5FLfpUsage);
        }
    }
}
