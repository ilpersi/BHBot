package com.github.ilpersi.BHBot;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.*;

public class EncounterManager {
    private final BHBot bot;
    static HashMap<String, FamiliarDetails> famMD5Table = new HashMap<>();

    public enum FamiliarType {
        ERROR("Error", 0),
        COMMON("Common", 1),
        RARE("Rare", 2),
        EPIC("Epic", 3),
        LEGENDARY("Legendary", 4);

        private final String name;
        private final int type;

        FamiliarType(String name, int type) {
            this.name = name;
            this.type = type;
        }

        public int getValue() {
            return this.type;
        }

        public String toString() {
            return this.name;
        }

    }

    static class BribeSettings {
        String familiarName;
        int toBribeCnt;

        BribeSettings() {
            this.familiarName = "";
            this.toBribeCnt = 0;
        }
    }

    static class FamiliarDetails {
        String name;
        FamiliarType type;

        FamiliarDetails(String familiarName, FamiliarType familiarType) {
            this.name = familiarName;
            this.type = familiarType;
        }
    }

    EncounterManager(BHBot bot) {
        this.bot = bot;
    }

    void processFamiliarEncounter() {
        MarvinSegment seg;

        BHBot.logger.autobribe("Familiar encountered");
        bot.browser.readScreen(2 * Misc.Durations.SECOND);

        FamiliarType familiarLevel;
        if (MarvinSegment.fromCue(BHBot.cues.get("CommonFamiliar"), bot.browser) != null) {
            familiarLevel = FamiliarType.COMMON;
        } else if (MarvinSegment.fromCue(BHBot.cues.get("RareFamiliar"), bot.browser) != null) {
            familiarLevel = FamiliarType.RARE;
        } else if (MarvinSegment.fromCue(BHBot.cues.get("EpicFamiliar"), bot.browser) != null) {
            familiarLevel = FamiliarType.EPIC;
        } else if (MarvinSegment.fromCue(BHBot.cues.get("LegendaryFamiliar"), bot.browser) != null) {
            familiarLevel = FamiliarType.LEGENDARY;
        } else {
            familiarLevel = FamiliarType.ERROR; // error
        }

        DungeonThread.PersuationType persuasion;
        BribeSettings bribeInfo = new BribeSettings();

        // Checking familiars setting takes time and a lot of cues verifications. We try to minimize the number of times
        // this is done
        boolean skipBribeNames = false;
        if ((bot.settings.bribeLevel > 0 && familiarLevel.getValue() >= bot.settings.bribeLevel) ||
                (bot.settings.familiars.size() == 0)) {
            skipBribeNames = true;
        }

        if (!skipBribeNames) {
            bribeInfo = verifyBribeNames();
        }

        if ((bot.settings.bribeLevel > 0 && familiarLevel.getValue() >= bot.settings.bribeLevel) ||
                bribeInfo.toBribeCnt > 0) {
            persuasion = DungeonThread.PersuationType.BRIBE;
        } else if ((bot.settings.persuasionLevel > 0 && familiarLevel.getValue() >= bot.settings.persuasionLevel)) {
            persuasion = DungeonThread.PersuationType.PERSUADE;
        } else {
            persuasion = DungeonThread.PersuationType.DECLINE;
        }

        // If we're set to bribe and we don't have gems, we default to PERSUASION
        if (persuasion == DungeonThread.PersuationType.BRIBE && bot.noGemsToBribe) {
            persuasion = DungeonThread.PersuationType.PERSUADE;
        }

        StringBuilder persuasionLog = new StringBuilder("familiar-");
        persuasionLog.append(familiarLevel.toString().toUpperCase()).append("-");
        persuasionLog.append(persuasion.toString().toUpperCase()).append("-");
        persuasionLog.append("attempt");

        // We save all the errors and persuasions based on settings
        if ((familiarLevel.getValue() >= bot.settings.familiarScreenshot) || familiarLevel == FamiliarType.ERROR) {
            bot.saveGameScreen(persuasionLog.toString(), "familiars");

            if (bot.settings.contributeFamiliars) {
                contributeFamiliarShoot(persuasionLog.toString(), familiarLevel);
            }
        }

        // We attempt persuasion or bribe based on settings
        if (persuasion == DungeonThread.PersuationType.BRIBE) {
            if (!bribeFamiliar()) {
                BHBot.logger.autobribe("Bribe attempt failed! Trying with persuasion...");
                if (persuadeFamiliar()) {
                    BHBot.logger.autobribe(familiarLevel.toString().toUpperCase() + " persuasion attempted.");
                } else {
                    BHBot.logger.error("Impossible to persuade familiar, restarting...");
                    bot.restart(true, false);
                }
            } else {
                updateFamiliarCounter(bribeInfo.familiarName.toUpperCase());
            }
        } else if (persuasion == DungeonThread.PersuationType.PERSUADE) {
            if (persuadeFamiliar()) {
                BHBot.logger.autobribe(familiarLevel.toString().toUpperCase() + " persuasion attempted.");
            } else {
                BHBot.logger.error("Impossible to attempt persuasion, restarting.");
                bot.restart(true, false);
            }
        } else {
            seg = MarvinSegment.fromCue(BHBot.cues.get("DeclineRed"), bot.browser);
            if (seg != null) {
                bot.browser.clickOnSeg(seg); // seg = detectCue(cues.get("Persuade"))
                bot.browser.readScreen(Misc.Durations.SECOND * 2);
                seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), Misc.Durations.SECOND, bot.browser);
                bot.browser.clickOnSeg(seg);
                BHBot.logger.autobribe(familiarLevel.toString().toUpperCase() + " persuasion declined.");
            } else {
                BHBot.logger.error("Impossible to find the decline button, restarting...");
                bot.restart(true, false);
            }
        }
    }

    /**
     * Will verify if in the current persuasion screen one of the bribeNames is present
     */
    private BribeSettings verifyBribeNames() {

        List<String> wrongNames = new ArrayList<>();
        BribeSettings result = new BribeSettings();
        String familiarName;
        int toBribeCnt;

        bot.browser.readScreen(Misc.Durations.SECOND);
        for (String familiarDetails : bot.settings.familiars) {
            // familiar details from settings
            String[] details = familiarDetails.toLowerCase().split(" ");
            familiarName = details[0];
            toBribeCnt = Integer.parseInt(details[1]);

            // cue related stuff
            //boolean isOldFormat = false;

            Cue familiarCue = BHBot.cues.getOrNull(familiarName);

            if (familiarCue != null) {
                if (toBribeCnt > 0) {
                    if (MarvinSegment.fromCue(familiarCue, Misc.Durations.SECOND * 3, bot.browser) != null) {
                        BHBot.logger.autobribe("Detected familiar " + familiarDetails + " as valid in familiars");
                        result.toBribeCnt = toBribeCnt;
                        result.familiarName = familiarName;
                        break;

                    }

                } else {
                    BHBot.logger.warn("Count for familiar " + familiarName + " is 0! Temporary removing it form the settings...");
                    wrongNames.add(familiarDetails);
                }
            } else {
                BHBot.logger.warn("Impossible to find a cue for familiar " + familiarName + ", it will be temporary removed from settings.");
                wrongNames.add(familiarDetails);
            }
        }

        // If there is any error we update the settings
        for (String wrongName : wrongNames) {
            bot.settings.familiars.remove(wrongName);
        }

        return result;
    }

    private boolean bribeFamiliar() {
        bot.browser.readScreen();
        MarvinSegment seg = MarvinSegment.fromCue(BHBot.cues.get("Bribe"), Misc.Durations.SECOND * 3, bot.browser);
        BufferedImage tmpScreen = bot.browser.getImg();

        if (seg != null) {
            bot.browser.clickOnSeg(seg);
            Misc.sleep(2 * Misc.Durations.SECOND);

            seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), Misc.Durations.SECOND * 5, bot.browser);
            if (seg != null) {
                bot.browser.clickOnSeg(seg);
                Misc.sleep(2 * Misc.Durations.SECOND);
            }

            if (MarvinSegment.fromCue(BHBot.cues.get("NotEnoughGems"), Misc.Durations.SECOND * 5, bot.browser) != null) {
                BHBot.logger.warn("Not enough gems to attempt a bribe!");
                bot.noGemsToBribe = true;
                if (!bot.browser.closePopupSecurely(BHBot.cues.get("NotEnoughGems"), BHBot.cues.get("No"))) {
                    BHBot.logger.error("Impossible to close the Not Enough gems pop-up. Restarting...");
                    bot.restart(true, false);
                }
                return false;
            }
            bot.notificationManager.sendBribeNotification(tmpScreen);
            return true;
        }

        return false;
    }

    private boolean persuadeFamiliar() {

        MarvinSegment seg;
        seg = MarvinSegment.fromCue(BHBot.cues.get("Persuade"), bot.browser);
        if (seg != null) {

            bot.browser.clickOnSeg(seg); // seg = detectCue(cues.get("Persuade"))
            Misc.sleep(2 * Misc.Durations.SECOND);

            bot.browser.readScreen();
            seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), bot.browser);
            bot.browser.clickOnSeg(seg);
            Misc.sleep(2 * Misc.Durations.SECOND);

            return true;
        }

        return false;
    }

    /**
     * This method is contributing familiar cues to the project. Before contributing the familiar image,
     * the method is taking care of stripping all the unnecessary pixels
     *
     * @param shootName    Name of the image containing the familiar screenshot
     * @param familiarType The familiar type: COMMON|RARE|EPIC|LEGENDARY
     */
    private void contributeFamiliarShoot(String shootName, FamiliarType familiarType) {

        bot.browser.readScreen(Misc.Durations.SECOND);
        BufferedImage famNameImg = EncounterManager.getFamiliarNameImg(bot.browser.getImg(), familiarType, new Bounds(105, 60, 640, 105));

        Misc.contributeImage(famNameImg, shootName, null);

    }

    private void updateFamiliarCounter(String fam) {
        String familiarToUpdate = "";
        String updatedFamiliar = "";

        for (String fa : bot.settings.familiars) { //cycle through array
            String fString = fa.toUpperCase().split(" ")[0]; //case sensitive for a match so convert to upper case
            int currentCounter = Integer.parseInt(fa.split(" ")[1]); //set the bribe counter to an int
            if (fam.equals(fString)) { //when match is found from the function
                familiarToUpdate = fa; //write current status to String
                currentCounter--; // decrease the counter
                updatedFamiliar = (fString.toLowerCase() + " " + currentCounter); //update new string with familiar name and decrease counter
            }
        }

        try {
            // input the file content to the StringBuffer "input"
            BufferedReader file = new BufferedReader(new FileReader("settings.ini"));
            String line;
            StringBuilder inputBuffer = new StringBuilder();

            //print lines to string with linebreaks
            while ((line = file.readLine()) != null) {
                inputBuffer.append(line);
                inputBuffer.append(System.getProperty("line.separator"));
            }
            String inputStr = inputBuffer.toString(); //load lines to string
            file.close();

            //find containing string and update with the output string from the function above
            if (inputStr.contains(familiarToUpdate)) {
                inputStr = inputStr.replace(familiarToUpdate, updatedFamiliar);
            }

            // write the string from memory over the existing file
            FileOutputStream fileOut = new FileOutputStream("settings.ini");
            fileOut.write(inputStr.getBytes());
            fileOut.close();

            bot.settings.load();  //reload the new settings file so the counter will be updated for the next bribe

        } catch (Exception e) {
            System.out.println("Problem writing to settings file");
        }
    }

    /**
     * This methods extract an image only containing the familiar name. The logic is based on the type of the familiar.
     * Once that the type is known, the name will be extracted using a specific value for the color
     *
     * @param screenImg    A Buffered Image containing the image
     * @param familiarType What is the type of the familiar we are looking to find the name
     * @return A Buffered Image containing just the familiar name
     */
    static BufferedImage getFamiliarNameImg(BufferedImage screenImg, FamiliarType familiarType, Bounds nameBounds) {
        int familiarTxtColor;
        switch (familiarType) {
            case COMMON:
                familiarTxtColor = -6881668;
                break;
            case RARE:
                familiarTxtColor = -7168525;
                break;
            case EPIC:
                familiarTxtColor = -98436;
                break;
            case LEGENDARY:
                familiarTxtColor = -66048;
                break;
            case ERROR:
            default:
                familiarTxtColor = 0;
                break;
        }

        if (familiarTxtColor == 0) return null;


        BufferedImage nameImgRect;
        if (nameBounds != null)
            nameImgRect = screenImg.getSubimage(nameBounds.x1, nameBounds.y1, nameBounds.width, nameBounds.height);
        else
            nameImgRect = screenImg;

		/*File zoneImgTmp = new File("tmp-NAME-ZONE.png");
		try {
			ImageIO.write(nameImgRect, "png", zoneImgTmp);
		} catch (IOException e) {
			BHBot.logger.error("", e);
		}*/

        int minX = nameImgRect.getWidth();
        int minY = nameImgRect.getHeight();
        int maxY = 0;
        int maxX = 0;

        int[][] pixelMatrix = Misc.convertTo2D(nameImgRect);
        for (int y = 0; y < nameImgRect.getHeight(); y++) {
            for (int x = 0; x < nameImgRect.getWidth(); x++) {
                if (pixelMatrix[x][y] == familiarTxtColor) {
                    if (y < minY) minY = y;
                    if (x < minX) minX = x;
                    if (y > maxY) maxY = y;
                    if (x > maxX) maxX = x;
                } else {
                    nameImgRect.setRGB(x, y, 0);
                }
            }

        }

        // pixel comparison is 0 based while image size i 1 based
        int width = maxX > 0 ? maxX - minX + 1 : 0;
        int height = maxY > 0 ? maxY - minY + 1 : 0;

        return nameImgRect.getSubimage(minX, minY, width, height);
    }

    /**
     * This will build the full list of MD5 for all the known familiars. This list will be used to manage bribing and
     * persuasions during encounters.
     */
    static void buildMD5() {
        final ClassLoader classLoader = EncounterManager.class.getClassLoader();
        int totalMD5Cnt = 0;

        HashMap<EncounterManager.FamiliarType, String> folders = new HashMap<>();
        folders.put(FamiliarType.COMMON, "cues/familiars/01 Common");
        folders.put(FamiliarType.RARE, "cues/familiars/02 Rare");
        folders.put(FamiliarType.EPIC, "cues/familiars/03 Epic");
        folders.put(FamiliarType.LEGENDARY, "cues/familiars/04 Legendary");

        for (Map.Entry<EncounterManager.FamiliarType, String> cuesPath : folders.entrySet()) {
            ArrayList<CueManager.CueDetails> famDetails = CueManager.getCueDetailsFromPath(cuesPath.getValue());
            totalMD5Cnt += famDetails.size();

            for (CueManager.CueDetails details : famDetails) {

                BufferedImage famImg = CueManager.loadImage(classLoader, details.path);
                BufferedImage famNameImg = EncounterManager.getFamiliarNameImg(famImg, cuesPath.getKey(), null);

                String MD5Str = Misc.imgToMD5(famNameImg);

                EncounterManager.FamiliarDetails familiar = new FamiliarDetails(details.name, cuesPath.getKey());
                EncounterManager.famMD5Table.put(MD5Str, familiar);

            }
        }
        BHBot.logger.debug("Loaded " + totalMD5Cnt + " familiars MD5.");
    }
}
