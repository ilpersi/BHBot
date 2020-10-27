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

    public enum PersuationType {
        DECLINE("Decline"),
        PERSUADE("Persuasion"),
        BRIBE("Bribe");

        private final String name;

        PersuationType(String name) {
            this.name = name;
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

        PersuationType persuasion;
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
            persuasion = PersuationType.BRIBE;
        } else if ((bot.settings.persuasionLevel > 0 && familiarLevel.getValue() >= bot.settings.persuasionLevel)) {
            persuasion = PersuationType.PERSUADE;
        } else {
            persuasion = PersuationType.DECLINE;
        }

        // If we're set to bribe and we don't have gems, we default to PERSUASION
        if (persuasion == PersuationType.BRIBE && bot.noGemsToBribe) {
            persuasion = PersuationType.PERSUADE;
        }

        StringBuilder persuasionLog = new StringBuilder("familiar-");
        persuasionLog.append(familiarLevel.toString().toUpperCase()).append("-");
        persuasionLog.append(persuasion.toString().toUpperCase()).append("-");
        persuasionLog.append("attempt");

        // We save all the errors and persuasions based on settings
        if ((familiarLevel.getValue() >= bot.settings.familiarScreenshot) || familiarLevel == FamiliarType.ERROR) {
            bot.saveGameScreen(persuasionLog.toString(), "familiars");
        }

        // if (bot.settings.contributeFamiliars) {

        // We build the MD5 string for the current encounter
        BufferedImage famNameImg = EncounterManager.getFamiliarNameImg(bot.browser.getImg(), familiarLevel, new Bounds(105, 60, 640, 105));
        String famNameMD5 = Misc.imgToMD5(famNameImg);

        // We check if the familiar is known
        FamiliarDetails encounterDetails = EncounterManager.famMD5Table.getOrDefault(famNameMD5, null);
        if (encounterDetails == null) {
            // String unkMD5 = bot.saveGameScreen(familiarLevel.toString() + "-unknown-familiar", "unknown-familiars", famNameImg);
            // BHBot.logger.debug("MD5 familiar unknown: '" + famNameMD5 + "' saved as " + unkMD5);
            // we contribute unknown familiars
            Misc.contributeImage(famNameImg, persuasionLog.toString(), null);
        } else {
            BHBot.logger.debug("MD5 familiar detected: " + encounterDetails.name);
        }
        // }

        // We attempt persuasion or bribe based on settings
        if (persuasion == PersuationType.BRIBE) {
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
        } else if (persuasion == PersuationType.PERSUADE) {
            if (persuadeFamiliar()) {
                BHBot.logger.autobribe(familiarLevel.toString().toUpperCase() + " persuasion attempted.");
            } else {
                BHBot.logger.error("Impossible to attempt persuasion, restarting.");
                bot.restart(true, false);
            }
        } else {
            seg = MarvinSegment.fromCue(BHBot.cues.get("DeclineRed"), 0, Bounds.fromWidthHeight(205, 420, 200, 65), bot.browser);
            if (seg != null) {
                bot.browser.clickOnSeg(seg); // seg = detectCue(cues.get("Persuade"))

                seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), Misc.Durations.SECOND * 5, Bounds.fromWidthHeight(245, 330, 165, 65), bot.browser);
                if (seg != null) {
                    bot.browser.clickOnSeg(seg);
                    BHBot.logger.autobribe(familiarLevel.toString().toUpperCase() + " persuasion declined.");
                } else {
                    BHBot.logger.error("Impossible to find the yes-green button after decline, restarting...");
                    bot.restart(true, false);
                }
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

        for (String familiarDetails : bot.settings.familiars) {
            // familiar details from settings
            String[] details = familiarDetails.toLowerCase().split(" ");
            familiarName = details[0];
            toBribeCnt = Integer.parseInt(details[1]);

            Cue familiarCue = BHBot.cues.getOrNull(familiarName);

            if (familiarCue != null) {
                if (toBribeCnt > 0) {
                    // The familiar screen is opened, we we search for cues without timeout
                    if (MarvinSegment.fromCue(familiarCue, bot.browser) != null) {
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

    /**
     * Bot will attempt to bribe the current encountered familiar.
     * Encounter window must be opened for this to work correctly
     *
     * @return true if bribe attempt is correctly performed, false otherwise
     */
    private boolean bribeFamiliar() {
        MarvinSegment seg = MarvinSegment.fromCue(BHBot.cues.get("Bribe"), bot.browser);
        BufferedImage tmpScreen = bot.browser.getImg();

        if (seg != null) {
            bot.browser.clickOnSeg(seg);

            // TODO Add Bounds for YesGreen
            seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), Misc.Durations.SECOND * 7, bot.browser);
            if (seg != null) {
                bot.browser.clickOnSeg(seg);
            } else {
                BHBot.logger.error("Impossible to find YesGreen in bribeFamiliar");
                return false;
            }

            // TODO Add Bounds for NotEnoughGems
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

    /**
     * Bot will attempt to persuade the current encountered familiar.
     * For this to work, the familiar window must be opened!
     *
     * @return true if persuasion is successfully performed, false otherwise
     */
    private boolean persuadeFamiliar() {

        MarvinSegment seg;
        seg = MarvinSegment.fromCue(BHBot.cues.get("Persuade"), bot.browser);
        if (seg != null) {

            bot.browser.clickOnSeg(seg); // seg = detectCue(cues.get("Persuade"))

            seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), Misc.Durations.SECOND * 5, Bounds.fromWidthHeight(245, 330, 165, 65), bot.browser);
            if (seg != null) {
                bot.browser.clickOnSeg(seg);
                return true;
            } else {
                BHBot.logger.error("Impossible to find the YesGreen button in persuadeFamiliar");
            }
        }
        return false;
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

    /**
     * Print the full list of MD5 hashes
     */
    static void printMD5() {
        for (Map.Entry<String, EncounterManager.FamiliarDetails> famDetails: EncounterManager.famMD5Table.entrySet()) {
            BHBot.logger.debug("MD5 '" + famDetails.getKey() + "' - > " + famDetails.getValue().name);
        }
    }

    /**
     * This method will search for famName in the MD5 hashmap and print the MD5 hash if found
     *
     * @param famName The name of the desired familiar
     */
    static void printMD5(String famName) {
        for (Map.Entry<String, EncounterManager.FamiliarDetails> famDetails: EncounterManager.famMD5Table.entrySet()) {
            if (famName.toLowerCase().equals(famDetails.getValue().name.toLowerCase())) {
                BHBot.logger.debug("MD5 '" + famDetails.getKey() + "' - > " + famDetails.getValue().name);
                return;
            }
        }
        BHBot.logger.warn("Familiar name not found: " + famName);
    }
}
