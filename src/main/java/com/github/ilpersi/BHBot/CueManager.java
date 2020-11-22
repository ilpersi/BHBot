package com.github.ilpersi.BHBot;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class CueManager {
    private static class CueData {
        private final String cuePath;
        private final Bounds cueBounds;

        CueData(String cuePath, Bounds cueBounds) {
            this.cuePath = cuePath;
            this.cueBounds = cueBounds;
        }
    }

    static class CueDetails {
        final String name;
        final String path;

        CueDetails(String cueName, String cuePath) {
            this.name = cueName;
            this.path = cuePath;
        }
    }

    private final Map<String, CueData> addedCues = new HashMap<>();
    private final Map<String, Cue> loadedCues = new HashMap<>();
    private final ClassLoader classLoader = CueManager.class.getClassLoader();

    CueManager() {
        buildCues();
    }

    private void addCue(String cueKey, String cuePath, Bounds cueBounds) {
        addedCues.put(cueKey, new CueData(cuePath, cueBounds));
    }

    Cue get(String cueKey) {
        if (!loadedCues.containsKey(cueKey)) {
            CueData cueData = addedCues.get(cueKey);
            loadedCues.put(cueKey, new Cue(cueKey, loadImage(classLoader, cueData.cuePath), cueData.cueBounds));

            // once we loaded the cue, we don't need the data anymore
            addedCues.remove(cueKey);
        }

        return loadedCues.get(cueKey);
    }

    Cue getOrNull(String cueKey) {
        try {
            return get(cueKey);
        } catch (NullPointerException ex) {
            return null;
        }
    }

    int size () {
        return loadedCues.size();
    }

    static BufferedImage loadImage(ClassLoader classLoader, String f) {
        BufferedImage img = null;
        InputStream resourceURL = classLoader.getResourceAsStream(f);

        if (resourceURL == null) {
            String decodedURL = null;
            try {
                decodedURL = URLDecoder.decode(f, StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                BHBot.logger.error("Error while decoding URL: ", e);
            }

            if (decodedURL != null) {
                resourceURL = classLoader.getResourceAsStream(decodedURL);
            }
            BHBot.logger.trace("Encoded IMG URI is: " + decodedURL);
        }

        if (resourceURL != null) {
            try {
                img = ImageIO.read(resourceURL);
            } catch (IOException e) {
                BHBot.logger.error("Error while loading Image", e);
            }
        } else {
            BHBot.logger.error("Error with resource: " + f);
        }

        return img;
    }

    /**
     * Given an origin folder, this nethod will return cueDetails for all the cues that are part of that folder
     *
     * @param cuesPath The path where to search for PNG cues
     * @return  An ArrayList of CueDetails with name and path for each of the found Cue
     */
    static ArrayList<CueDetails> getCueDetailsFromPath(String cuesPath) {
        ArrayList<CueDetails> cueDetails = new ArrayList<>();

        // We make sure that the last char of the path is a folder separator
        if (!"/".equals(cuesPath.substring(cuesPath.length() - 1))) cuesPath += "/";

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        URL url = classLoader.getResource(cuesPath);
        if (url != null) { // Run from the IDE
            if ("file".equals(url.getProtocol())) {

                InputStream in = classLoader.getResourceAsStream(cuesPath);
                if (in == null) {
                    BHBot.logger.error("Impossible to create InputStream in getCueDetails");
                    return cueDetails;
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String resource;

                while (true) {
                    try {
                        resource = br.readLine();
                        if (resource == null) break;
                    } catch (IOException e) {
                        BHBot.logger.error("Error while reading resources in getCueDetails", e);
                        continue;
                    }
                    int dotPosition = resource.lastIndexOf('.');
                    String fileExtension = dotPosition > 0 ? resource.substring(dotPosition + 1) : "";
                    if ("png".equals(fileExtension.toLowerCase())) {
                        String cueName = resource.substring(0, dotPosition);

                        CueDetails details = new CueDetails(cueName.toLowerCase(), cuesPath + resource);
                        cueDetails.add(details);
                    }
                }
            } else if ("jar".equals(url.getProtocol())) { // Run from JAR
                BHBot.logger.debug("Reading JAR File for cues in path " + cuesPath);
                String path = url.getPath();
                String jarPath = path.substring(5, path.indexOf("!"));

                String decodedURL;
                try {
                    decodedURL = URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name());
                } catch (UnsupportedEncodingException e) {
                    BHBot.logger.error("Impossible to decode pat for jar: " + jarPath, e);
                    return cueDetails;
                }

                JarFile jar;
                try {
                    jar = new JarFile(decodedURL);
                } catch (IOException e) {
                    BHBot.logger.error("Impossible to open JAR file : " + decodedURL, e);
                    return cueDetails;
                }

                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(cuesPath) && !cuesPath.equals(name)) {
                        URL resource = classLoader.getResource(name);

                        if (resource == null) continue;

                        String resourcePath = resource.toString();
                        BHBot.logger.trace("resourcePath: " + resourcePath);
                        if (!resourcePath.contains("!")) {
                            BHBot.logger.warn("Unexpected resource filename in load Cue Folder");
                            continue;
                        }

                        String[] fileDetails = resourcePath.split("!");
                        String resourceRelativePath = fileDetails[1];
                        BHBot.logger.trace("resourceRelativePath : " + resourceRelativePath);
                        int lastSlashPosition = resourceRelativePath.lastIndexOf('/');
                        String fileName = resourceRelativePath.substring(lastSlashPosition + 1);

                        int dotPosition = fileName.lastIndexOf('.');
                        String fileExtension = dotPosition > 0 ? fileName.substring(dotPosition + 1) : "";
                        if ("png".equals(fileExtension.toLowerCase())) {
                            String cueName = fileName.substring(0, dotPosition);

                            BHBot.logger.trace("cueName: " + cueName.toLowerCase());

                            // resourceRelativePath begins with a '/' char and we want to be sure to remove it
                            CueDetails details = new CueDetails(cueName.toLowerCase(), resourceRelativePath.substring(1));
                            cueDetails.add(details);
                        }
                    }
                }

            }
        }
        
        return cueDetails;
    }

    @SuppressWarnings("SameParameterValue")
    int loadCueFolder(String cuesPath, String prefix, boolean stripCueStr, Bounds bounds) {
        int totalLoaded = 0;
        
        ArrayList<CueDetails> cueDetails = CueManager.getCueDetailsFromPath(cuesPath);
        if (cueDetails.size() > 0) {
            totalLoaded += cueDetails.size();

            for (CueDetails details : cueDetails) {
                String cueName = details.name;
                if (prefix != null) cueName = prefix + cueName;
                if (stripCueStr) cueName = cueName.replace("cue", "");

                addCue(cueName, details.path, bounds);
            }
        }

        return totalLoaded;
    }

    private void buildCues() {
        addCue("Main", "cues/cueMainScreen.png", new Bounds(90, 5, 100, 20));
        addCue("Login", "cues/cueLogin.png", new Bounds(270, 260, 330, 300)); // login window (happens seldom)
        addCue("AreYouThere", "cues/cueAreYouThere.png", Bounds.fromWidthHeight(250, 240, 300, 45));
        addCue("Yes", "cues/cueYes.png", null);

        addCue("Disconnected", "cues/cueDisconnected.png", Bounds.fromWidthHeight(299, 232, 202, 70)); // cue for "You have been disconnected" popup
        addCue("Reconnect", "cues/cueReconnectButton.png", new Bounds(320, 330, 400, 360)); // used with "You have been disconnected" dialog and also with the "maintenance" dialog
        addCue("Reload", "cues/cueReload.png", new Bounds(320, 330, 360, 360)); // used in "There is a new update required to play" dialog (happens on Friday night)
        addCue("Maintenance", "cues/cueMaintenance.png", new Bounds(230, 200, 320, 250)); // cue for "Bit Heroes is currently down for maintenance. Please check back shortly!"
        addCue("Loading", "cues/cueLoading.png", new Bounds(315, 210, 330, 225)); // cue for "Loading" superimposed screen
        addCue("RecentlyDisconnected", "cues/cueRecentlyDisconnected.png", new Bounds(250, 195, 535, 320)); // cue for "You were recently disconnected from a dungeon. Do you want to continue the dungeon?" window
        addCue("UnableToConnect", "cues/cueUnableToConnect.png", new Bounds(245, 235, 270, 250)); // happens when some error occurs for which the flash app is unable to connect to the server. We must simply click on the "Reconnect" button in this case!
        addCue("GearCheck", "cues/cueGearCheck.png", Bounds.fromWidthHeight(244, 208, 314, 120));

        addCue("DailyRewards", "cues/cueDailyRewards.png", new Bounds(260, 45, 285, 75));
        addCue("Claim", "cues/cueClaim.png", null); // claim button, when daily rewards popup is open
        addCue("Items", "cues/cueItems.png", null); // used when we clicked "claim" on daily rewards popup. Used also with main menu ads.
        addCue("X", "cues/cueX.png", null); // "X" close button used with claimed daily rewards popup
        addCue("WeeklyRewards", "cues/cueWeeklyRewards.png", new Bounds(185, 95, 250, 185)); // used with reward for GVG/PVP/Gauntlet/Trial on Friday night (when day changes into Saturday)
        addCue("Bounties", "cues/cueBounties.png", new Bounds(320, 63, 480, 103));
        addCue("Loot", "cues/cueLoot.png", null);

        addCue("News", "cues/cueNewsPopup.png", new Bounds(345, 60, 365, 85)); // news popup
        addCue("Close", "cues/cueClose.png", null); // close button used with "News" popup, also when defeated in dungeon, etc.

        addCue("EnergyBar", "cues/cueEnergyBar.png", new Bounds(390, 0, 420, 20));
        addCue("TicketBar", "cues/cueTicketBar.png", new Bounds(540, 0, 770, 20));

        addCue("RaidButton", "cues/cueRaidButton.png", new Bounds(0, 200, 40, 400));
        addCue("RaidPopup", "cues/cueRaidPopup.png", Bounds.fromWidthHeight(300, 35, 70, 60));
        addCue("RaidSummon", "cues/cueRaidSummon.png", new Bounds(480, 360, 540, 380));
        addCue("RaidLevel", "cues/cueRaidLevel.png", Bounds.fromWidthHeight(230, 435, 330, 20)); // selected raid type button cue
        addCue("cueRaidLevelEmpty", "cues/cueRaidLevelEmpty.png", Bounds.fromWidthHeight(230, 435, 330, 20)); // unselected raid type button cue
        addCue("NotEnoughShards", "cues/cueNotEnoughShards.png", Bounds.fromWidthHeight(265, 215, 270, 70));

        // New Raid level detection logic
        addCue("Raid1Name", "cues/raid/r1Name.png", new Bounds(185, 340, 485, 395));// Raid 1 Name
        addCue("R1Only", "cues/cueR1Only.png", null); // cue for R1 type selected when R2 (and R3) is not open yet (in that case it won't show raid type selection buttons)
        addCue("Normal", "cues/cueNormal.png", null);
        addCue("Hard", "cues/cueHard.png", null);
        addCue("Heroic", "cues/cueHeroic.png", null);
        addCue("Accept", "cues/cueAccept.png", null);
        addCue("D4Accept", "cues/cueD4Accept.png", null);
        addCue("Cleared", "cues/cueCleared.png", new Bounds(208, 113, 608, 394)); // used for example when raid has been finished
        addCue("Defeat", "cues/cueDefeat.png", new Bounds(320, 50, 480, 180)); // used for example when you have been defeated in a dungeon. Also used when you have been defeated in a gauntlet.
        addCue("YesGreen", "cues/cueYesGreen.png", null); // used for example when raid has been finished ("Cleared" popup)
        addCue("Rerun", "cues/cueRerun.png", null); // used for example when raid has been finished ("Cleared" popup)
        addCue("Persuade", "cues/cuePersuade.png", new Bounds(116, 311, 286, 380));
        addCue("View", "cues/cueView.png", new Bounds(390, 415, 600, 486));
        addCue("Bribe", "cues/cueBribe.png", new Bounds(505, 305, 684, 375));
        addCue("SkeletonTreasure", "cues/cueSkeletonTreasure.png", new Bounds(185, 165, 295, 280)); // skeleton treasure found in dungeons (it's a dialog/popup cue)
        addCue("SkeletonNoKeys", "cues/cueSkeletonNoKeys.png", new Bounds(478, 318, 500, 348)); // red 0

        addCue("Open", "cues/cueOpen.png", null); // skeleton treasure open button
        addCue("Decline", "cues/cueDecline.png", null); // decline skeleton treasure button (found in dungeons), also with video ad treasures (found in dungeons)
        addCue("DeclineRed", "cues/cueDeclineRed.png", null); // decline persuation attempts
        addCue("Merchant", "cues/cueMerchant.png", null); // cue for merchant dialog/popup
        addCue("SettingsGear", "cues/cueSettingsGear.png", new Bounds(655, 450, 730, 515)); // settings button
        addCue("Settings", "cues/cueSettings.png", new Bounds(249, 61, 558, 102)); // settings menu

        addCue("Team", "cues/cueTeam.png", null); // Team text part of pop-ups about teams
        addCue("TeamNotFull", "cues/cueTeamNotFull.png", new Bounds(230, 200, 330, 250)); // warning popup when some friend left you and your team is not complete anymore
        addCue("TeamNotOrdered", "cues/cueTeamNotOrdered.png", new Bounds(230, 190, 350, 250)); // warning popup when some guild member left and your GvG team is not complete anymore
        addCue("GuildLeaveConfirm", "cues/cueGuildLeaveConfirm.png", new Bounds(195, 105, 605, 395)); // GVG confirm
        addCue("DisabledBattles", "cues/cueDisabledBattles.png", new Bounds(240, 210, 560, 330)); // Disabled Battles Poppup

        addCue("No", "cues/cueNo.png", null); // cue for a blue "No" button used for example with "Your team is not full" dialog, or for "Replace consumable" dialog, etc. This is why we can't put concrete borders as position varies a lot.
        addCue("AutoTeam", "cues/cueAutoTeam.png", null); // "Auto" button that automatically assigns team (in raid, GvG, ...)
        addCue("Clear", "cues/cueClear.png", null); //clear team button

        addCue("AutoOn", "cues/cueAutoOn.png", new Bounds(740, 180, 785, 220)); // cue for auto pilot on
        addCue("AutoOff", "cues/cueAutoOff.png", new Bounds(740, 180, 785, 220)); // cue for auto pilot off
        addCue("Speed_Full", "cues/Speed_Full.png", new Bounds(7, 488, 65, 504)); // 3/3 speed bar in encounters
        addCue("Speed", "cues/Speed_Text.png", new Bounds(20, 506, 61, 518)); // speed text label in encounters

        addCue("Trials", "cues/cueTrials.png", new Bounds(0, 0, 40, 400)); // cue for trials button (note that as of 23.9.2017 they changed the button position to the right side of the screen and modified the glyph)
        addCue("Trials2", "cues/cueTrials2.png", new Bounds(720, 0, 770, 400)); // an alternative cue for trials (flipped horizontally, located on the right side of the screen). Used since 23.9.2017.
        addCue("Gauntlet", "cues/cueGauntlet.png", null); // cue for gauntlet button
        addCue("Gauntlet2", "cues/cueGauntlet2.png", null); // alternative cue for gauntlet button
        addCue("Play", "cues/cuePlay.png", null); // cue for play button in trials/gauntlet window
        addCue("TokenBar", "cues/cueTokenBar.png", null);
        addCue("CloseGreen", "cues/cueCloseGreen.png", null); // close button used with "You have been defeated" popup in gauntlet and also "Victory" window in gauntlet
        addCue("VictorySmall", "cues/Victory_Small.png", null); // victory window cue found upon completing gauntlet / PvP

        addCue("UhOh", "cues/cueUhoh.png", new Bounds(319, 122, 526, 184));
        addCue("ReviveAverage", "cues/cueReviveAverage.png", null);
        addCue("Purchase", "cues/cuePurchase.png", new Bounds(240, 240, 390, 280));

        addCue("GuildButton", "cues/cueGuildButton.png", new Bounds(500, 420, 590, 520));
        addCue("IgnoreCheck", "cues/cueIgnoreCheck.png", null);

        addCue("Quest", "cues/cueQuest.png", new Bounds(0, 0, 40, 40)); // cue for quest (dungeons) button
        addCue("ZonesButton", "cues/cueZonesButton.png", new Bounds(105, 60, 125, 75));
        addCue("Zone1", "cues/cueZone1.png", null);
        addCue("Zone2", "cues/cueZone2.png", null);
        addCue("Zone3", "cues/cueZone3.png", null);
        addCue("Zone4", "cues/cueZone4.png", null);
        addCue("Zone5", "cues/cueZone5.png", null);
        addCue("Zone6", "cues/cueZone6.png", null);
        addCue("Zone7", "cues/cueZone7.png", null);
        addCue("Zone8", "cues/cueZone8.png", null);
        addCue("Zone9", "cues/cueZone9.png", null);
        addCue("Zone10", "cues/cueZone10.png", null);
        addCue("Zone11", "cues/cueZone11.png", null);
        addCue("Zone12", "cues/cueZone12.png", null);
        addCue("Zone13", "cues/cueZone13.png", Bounds.fromWidthHeight(250, 50, 300, 40));
        addCue("RightArrow", "cues/cueRightArrow.png", null); // arrow used in quest screen to change zone
        addCue("LeftArrow", "cues/cueLeftArrow.png", null); // arrow used in quest screen to change zone
        addCue("Enter", "cues/cueEnter.png", null); // "Enter" button found on d4 window
        addCue("NotEnoughEnergy", "cues/cueNotEnoughEnergy.png", new Bounds(260, 210, 290, 235)); // "Not enough Energy" popup cue

        addCue("PVP", "cues/cuePVP.png", new Bounds(0, 70, 40, 110)); // PVP icon in main screen
        addCue("Fight", "cues/cueFight.png", null); // fight button in PVP window
        addCue("PVPWindow", "cues/cuePVPWindow.png", null); // PVP window cue

        addCue("DialogRight", "cues/cueDialogRight.png", new Bounds(675, 205, 690, 250)); // cue for the dialog window (when arrow is at the right side of the window)
        addCue("DialogLeft", "cues/cueDialogLeft.png", new Bounds(100, 205, 125, 250)); // cue for the dialog window (when arrow is at the left side of the window)

        addCue("SpeedCheck", "cues/cueSpeedCheck.png", new Bounds(0, 450, 100, 520));
        addCue("Switch", "cues/cueSwitch.png", new Bounds(0, 450, 100, 520)); //unused

        // GVG related:
        addCue("GVG", "cues/cueGVG.png", null); // main GVG button cue
        addCue("BadgeBar", "cues/cueBadgeBar.png", null);
        addCue("GVGWindow", "cues/cueGVGWindow.png", new Bounds(260, 90, 280, 110)); // GVG window cue

        addCue("InGamePM", "cues/cueInGamePM.png", new Bounds(450, 330, 530, 380)); // note that the guild window uses the same cue! That's why it's important that user doesn't open guild window while bot is working!

        addCue("TrialsOrGauntletWindow", "cues/cueTrialsOrGauntletWindow.png", new Bounds(300, 30, 510, 105)); // cue for a trials/gauntlet window
        addCue("NotEnoughTokens", "cues/cueNotEnoughTokens.png", Bounds.fromWidthHeight(274, 228, 253, 79)); // cue to check for the not enough tokens popup

        addCue("Difficulty", "cues/cueDifficulty.png", new Bounds(450, 330, 640, 450)); // selected difficulty in trials/gauntlet window
        addCue("DifficultyDisabled", "cues/cueDifficultyDisabled.png", new Bounds(450, 330, 640, 450)); // selected difficulty in trials/gauntlet window (disabled - because game is still fetching data from server)
        addCue("SelectDifficulty", "cues/cueSelectDifficulty.png", new Bounds(400, 260, 0, 0)/*not exact bounds... the lower-right part of screen!*/); // select difficulty button in trials/gauntlet
        addCue("DifficultyDropDown", "cues/cueDifficultyDropDown.png", new Bounds(260, 50, 550, 125)); // difficulty drop down menu cue
        addCue("DifficultyExpedition", "cues/cueDifficultyExpedition.png", null); // selected difficulty in trials/gauntlet window
        addCue("SelectDifficultyExpedition", "cues/cueSelectDifficultyExpedition.png", null);
        addCue("DropDownUp", "cues/cueDropDownUp.png", null); // up arrow in difficulty drop down menu (found in trials/gauntlet, for example)
        addCue("DropDownDown", "cues/cueDropDownDown.png", null); // down arrow in difficulty drop down menu (found in trials/gauntlet, for example)
        addCue("Cost", "cues/cueCost.png", new Bounds(400, 150, 580, 240)); // used both for PvP and Gauntlet/Trials costs. Note that bounds are very wide, because position of this cue in PvP is different from that in Gauntlet/Trials!
        addCue("SelectCost", "cues/cueSelectCost.png", new Bounds(555, 170, 595, 205)); // cue for select cost found in both PvP and Gauntlet/Trials windows. Note that bounds are wide, because position of this cue in PvP is different from that in Gauntlet/Trials!
        addCue("CostDropDown", "cues/cueCostDropDown.png", new Bounds(260, 45, 320, 70)); // cue for cost selection drop down window
        addCue("0", "cues/numbers/cue0.png", null);
        addCue("1", "cues/numbers/cue1.png", null);
        addCue("2", "cues/numbers/cue2.png", null);
        addCue("3", "cues/numbers/cue3.png", null);
        addCue("4", "cues/numbers/cue4.png", null);
        addCue("5", "cues/numbers/cue5.png", null);
        addCue("6", "cues/numbers/cue6.png", null);
        addCue("7", "cues/numbers/cue7.png", null);
        addCue("8", "cues/numbers/cue8.png", null);
        addCue("9", "cues/numbers/cue9.png", null);

        // Difficulty Tier
        addCue("hyphen", "cues/numbers/hyphen.png", null);

        // WB player TS numbers
        addCue("wb_player_ts_0", "cues/worldboss/ts/wb_player_ts_0.png", null);
        addCue("wb_player_ts_1", "cues/worldboss/ts/wb_player_ts_1.png", null);
        addCue("wb_player_ts_2", "cues/worldboss/ts/wb_player_ts_2.png", null);
        addCue("wb_player_ts_3", "cues/worldboss/ts/wb_player_ts_3.png", null);
        addCue("wb_player_ts_4", "cues/worldboss/ts/wb_player_ts_4.png", null);
        addCue("wb_player_ts_5", "cues/worldboss/ts/wb_player_ts_5.png", null);
        addCue("wb_player_ts_6", "cues/worldboss/ts/wb_player_ts_6.png", null);
        addCue("wb_player_ts_7", "cues/worldboss/ts/wb_player_ts_7.png", null);
        addCue("wb_player_ts_8", "cues/worldboss/ts/wb_player_ts_8.png", null);
        addCue("wb_player_ts_9", "cues/worldboss/ts/wb_player_ts_9.png", null);

        // WB player TS numbers
        addCue("wb_total_ts_0", "cues/worldboss/ts/wb_total_ts_0.png", null);
        addCue("wb_total_ts_1", "cues/worldboss/ts/wb_total_ts_1.png", null);
        addCue("wb_total_ts_2", "cues/worldboss/ts/wb_total_ts_2.png", null);
        addCue("wb_total_ts_3", "cues/worldboss/ts/wb_total_ts_3.png", null);
        addCue("wb_total_ts_4", "cues/worldboss/ts/wb_total_ts_4.png", null);
        addCue("wb_total_ts_5", "cues/worldboss/ts/wb_total_ts_5.png", null);
        addCue("wb_total_ts_6", "cues/worldboss/ts/wb_total_ts_6.png", null);
        addCue("wb_total_ts_7", "cues/worldboss/ts/wb_total_ts_7.png", null);
        addCue("wb_total_ts_8", "cues/worldboss/ts/wb_total_ts_8.png", null);
        addCue("wb_total_ts_9", "cues/worldboss/ts/wb_total_ts_9.png", null);

        // Invasion Level Numbers
        addCue("small0", "cues/numbers/small0.png", null);
        addCue("small1", "cues/numbers/small1.png", null);
        addCue("small2", "cues/numbers/small2.png", null);
        addCue("small3", "cues/numbers/small3.png", null);
        addCue("small4", "cues/numbers/small4.png", null);
        addCue("small5", "cues/numbers/small5.png", null);
        addCue("small6", "cues/numbers/small6.png", null);
        addCue("small7", "cues/numbers/small7.png", null);
        addCue("small8", "cues/numbers/small8.png", null);
        addCue("small9", "cues/numbers/small9.png", null);

        // T/G Gauntlet difficulty related
        addCue("ScrollerAtTop", "cues/cueScrollerAtTop.png", null);
        addCue("ScrollerNone", "cues/cueScrollerNone.png", Bounds.fromWidthHeight(525, 120, 30, 330));


        // PvP strip related:
        addCue("StripScrollerTopPos", "cues/strip/cueStripScrollerTopPos.png", new Bounds(525, 140, 540, 370));
        addCue("StripEquipped", "cues/strip/cueStripEquipped.png", new Bounds(465, 180, 485, 200)); // the little "E" icon upon an equipped item (the top-left item though, we want to detect just that one)
        addCue("StripItemsTitle", "cues/strip/cueStripItemsTitle.png", new Bounds(335, 70, 360, 80));
        addCue("StripSelectorButton", "cues/strip/cueStripSelectorButton.png", new Bounds(450, 115, 465, 130));

        // filter titles:
        addCue("StripTypeBody", "cues/strip/cueStripTypeBody.png", new Bounds(460, 125, 550, 140));
        addCue("StripTypeHead", "cues/strip/cueStripTypeHead.png", new Bounds(460, 125, 550, 140));
        addCue("StripTypeMainhand", "cues/strip/cueStripTypeMainhand.png", new Bounds(460, 125, 550, 140));
        addCue("StripTypeOffhand", "cues/strip/cueStripTypeOffhand.png", new Bounds(460, 125, 550, 140));
        addCue("StripTypeNeck", "cues/strip/cueStripTypeNeck.png", new Bounds(460, 125, 550, 140));
        addCue("StripTypeRing", "cues/strip/cueStripTypeRing.png", new Bounds(460, 125, 550, 140));

        // consumables management related:
        addCue("BonusExp", "cues/cueBonusExp.png", new Bounds(100, 455, 370, 485)); // consumable icon in the main menu (when it's being used)
        addCue("BonusItem", "cues/cueBonusItem.png", new Bounds(100, 455, 370, 485));
        addCue("BonusGold", "cues/cueBonusGold.png", new Bounds(100, 455, 370, 485));
        addCue("BonusSpeed", "cues/cueBonusSpeed.png", new Bounds(100, 455, 370, 485));
        addCue("ConsumableExpMinor", "cues/cueConsumableExpMinor.png", null); // consumable icon in the inventory
        addCue("ConsumableExpAverage", "cues/cueConsumableExpAverage.png", null);
        addCue("ConsumableExpMajor", "cues/cueConsumableExpMajor.png", null);
        addCue("ConsumableItemMinor", "cues/cueConsumableItemMinor.png", null);
        addCue("ConsumableItemAverage", "cues/cueConsumableItemAverage.png", null);
        addCue("ConsumableItemMajor", "cues/cueConsumableItemMajor.png", null);
        addCue("ConsumableSpeedMinor", "cues/cueConsumableSpeedMinor.png", null);
        addCue("ConsumableSpeedAverage", "cues/cueConsumableSpeedAverage.png", null);
        addCue("ConsumableSpeedMajor", "cues/cueConsumableSpeedMajor.png", null);
        addCue("ConsumableGoldMinor", "cues/cueConsumableGoldMinor.png", null);
        addCue("ConsumableGoldAverage", "cues/cueConsumableGoldAverage.png", null);
        addCue("ConsumableGoldMajor", "cues/cueConsumableGoldMajor.png", null);
        addCue("ConsumablePumkgor", "cues/cueConsumablePumkgor.png", new Bounds(150, 460, 205, 519)); // Special Halloween consumable
        addCue("ConsumableGingernaut", "cues/cueConsumableGingernaut.png", new Bounds(150, 460, 205, 519)); // Special Chrismast consumable
        addCue("ConsumableGreatFeast", "cues/cueConsumableGreatFeast.png", new Bounds(150, 460, 205, 519)); // Thanksgiving consumable
        addCue("ConsumableBroccoli", "cues/cueConsumableBroccoli.png", new Bounds(150, 460, 205, 519)); // Special Halloween consumable
        addCue("ConsumableCoco", "cues/cueConsumableCoco.png", new Bounds(150, 460, 205, 519)); // Special ?? consumable
        addCue("ScrollerAtBottom", "cues/cueScrollerAtBottom.png", null); // cue for scroller being at the bottom-most position (we can't scroll down more than this)
        addCue("ConsumableTitle", "cues/cueConsumableTitle.png", new Bounds(280, 100, 310, 180)); // cue for title of the window that pops up when we want to consume a consumable. Note that vertical range is big here since sometimes is higher due to greater window size and sometimes is lower.
        addCue("FilterConsumables", "cues/cueFilterConsumables.png", new Bounds(460, 125, 550, 140)); // cue for filter button name
        addCue("LoadingInventoryIcon", "cues/cueLoadingInventoryIcon.png", null); // cue for loading animation for the icons inside inventory

        // rune management related:
        addCue("Runes", "cues/cueRunes.png", new Bounds(120, 450, 245, 495)); // runes button in profile
        addCue("RunesLayout", "cues/cueRunesLayout.png", new Bounds(340, 70, 460, 110)); // runes layout header
        addCue("RunesPicker", "cues/cueRunesPicker.png", null); // rune picker
        addCue("RunesSwitch", "cues/cueRunesSwitch.png", new Bounds(320, 260, 480, 295)); // rune picker

        // All minor rune cues
        for (AutoRuneManager.MinorRune rune : AutoRuneManager.MinorRune.values()) {
            addCue(rune.getRuneCueName(), rune.getRuneCueFileName(), null);
            addCue(rune.getRuneSelectCueName(), rune.getRuneSelectCueFileName(), new Bounds(235, 185, 540, 350));
        }

        // invasion related:
        addCue("Invasion", "cues/cueInvasion.png", null);
        addCue("InvasionWindow", "cues/cueInvasionWindow.png", new Bounds(260, 90, 280, 110)); // GVG window cue

        // Expedition related:
        addCue("ExpeditionButton", "cues/cueExpeditionButton.png", null);
        addCue("Expedition1", "cues/expedition/cueExpedition1Hallowed.png", new Bounds(168, 34, 628, 108)); // Hallowed Expedtion Title
        addCue("Expedition2", "cues/expedition/cueExpedition2Inferno.png", new Bounds(200, 40, 600, 100)); //Inferno Expedition
        addCue("Expedition3", "cues/expedition/cueExpedition3Jammie.png", new Bounds(230, 40, 565, 100)); //Jammie Dimension
        addCue("Expedition4", "cues/expedition/cueExpedition4Idol.png", new Bounds(230, 40, 565, 100)); //Idol Dimension
        addCue("Expedition5", "cues/expedition/cueExpedition5BattleBards.png", new Bounds(230, 40, 565, 100)); //Battle Bards!
        addCue("PortalBorderLeaves", "cues/expedition/portalBorderLeaves.png", new Bounds(48, 447, 107, 503));

        //WorldBoss Related
        addCue("WorldBoss", "cues/worldboss/cueWorldBoss.png", null);
        addCue("WorldBossSelector", "cues/worldboss/cueWorldBossSelector.png", null);
        addCue("DarkBlueSummon", "cues/worldboss/cueDarkBlueSummon.png", Bounds.fromWidthHeight(453, 449, 99, 28));
        addCue("LargeDarkBlueSummon", "cues/worldboss/cueLargeDarkBlueSummon.png", Bounds.fromWidthHeight(492, 363, 99, 28));
        addCue("SmallDarkBlueSummon", "cues/worldboss/cueSmallDarkBlueSummon.png", Bounds.fromWidthHeight(430, 388, 95, 28));
        addCue("Invite", "cues/worldboss/cueDarkBlueInvite.png", null);
        addCue("Start", "cues/cueStart.png", null);
        addCue("DarkBlueStart", "cues/worldboss/cueDarkBlueStart.png", Bounds.fromWidthHeight(285, 435, 190, 65));
        addCue("VictoryLarge", "cues/Victory_Large.png", new Bounds(324, 128, 476, 157));
        addCue("OrlagSelected", "cues/cueOrlagSelected.png", new Bounds(360, 430, 440, 460));
        addCue("NetherSelected", "cues/cueNetherSelected.png", null);
        addCue("Private", "cues/cuePrivate.png", new Bounds(310, 320, 370, 380));
        addCue("Unready", "cues/worldboss/cueWorldBossUnready.png", new Bounds(170, 210, 215, 420));
        addCue("WorldBossTier", "cues/worldboss/cueWorldBossTier.png", Bounds.fromWidthHeight(314, 206, 88, 28));
        addCue("WorldBossTierDropDown", "cues/worldboss/cueWorldBossTierDropDown.png", Bounds.fromWidthHeight(304, 199, 194, 42));
        addCue("WorldBossDifficultyNormal", "cues/worldboss/cueWorldBossDifficultyNormal.png", new Bounds(300, 275, 500, 325));
        addCue("WorldBossDifficultyHard", "cues/worldboss/cueWorldBossDifficultyHard.png", new Bounds(300, 275, 500, 325));
        addCue("WorldBossDifficultyHeroic", "cues/worldboss/cueWorldBossDifficultyHeroic.png", new Bounds(300, 275, 500, 325));
        addCue("WorldBossPopup", "cues/worldboss/cueWorldBossPopup.png", Bounds.fromWidthHeight(305, 29, 67, 53));
        addCue("NotEnoughXeals", "cues/worldboss/cueNotEnoughXeals.png", Bounds.fromWidthHeight(270, 220, 260, 90));

        addCue("cueWBSelectNormal", "cues/worldboss/cueWBSelectNormal.png", new Bounds(260, 140, 510, 320));
        addCue("cueWBSelectHard", "cues/worldboss/cueWBSelectHard.png", new Bounds(260, 140, 510, 320));
        addCue("cueWBSelectHeroic", "cues/worldboss/cueWBSelectHeroic.png", new Bounds(260, 140, 510, 320));


        addCue("WorldBossTitle", "cues/worldboss/cueWorldBossTitle.png", new Bounds(280, 90, 515, 140));
        addCue("WorldBossSummonTitle", "cues/worldboss/cueWorldBossSummonTitle.png", new Bounds(325, 100, 480, 150));
        addCue("WorldBossPlayerKick", "cues/worldboss/ts/wb_player_kick.png", null);
        addCue("WorldBossPopupKick", "cues/worldboss/ts/wb_popup_kick.png", Bounds.fromWidthHeight(355, 135, 85, 40));


        //fishing related
        addCue("FishingButton", "cues/cueFishingButton.png", null);
        addCue("Exit", "cues/cueExit.png", null);
        addCue("Fishing", "cues/cueFishing.png", new Bounds(720, 200, 799, 519));
        addCue("FishingClose", "cues/fishingClose.png", null);
        addCue("Trade", "cues/cueTrade.png", new Bounds(360, 443, 441, 468));
        addCue("Hall", "cues/cueHall.png", new Bounds(575, 455, 645, 480));
        addCue("GuildHallC", "cues/cueGuildHallC.png", new Bounds(750, 55, 792, 13));

        //Familiar bribing cues
        addCue("NotEnoughGems", "cues/cueNotEnoughGems.png", null); // used when not enough gems are available
        addCue("CommonFamiliar", "cues/familiars/type/cue01CommonFamiliar.png", new Bounds(525, 240, 674, 365)); // Common Bribe cue
        addCue("RareFamiliar", "cues/familiars/type/cue02RareFamiliar.png", new Bounds(525, 240, 674, 365)); // Rare Bribe cue
        addCue("EpicFamiliar", "cues/familiars/type/cue03EpicFamiliar.png", new Bounds(525, 240, 674, 365)); // Epic Bribe cue
        addCue("LegendaryFamiliar", "cues/familiars/type/cue04LegendaryFamiliar.png", new Bounds(525, 240, 674, 365)); // Epic Bribe cue

        //AutoRevive cues
        addCue("Potions", "cues/autorevive/cuePotions.png", new Bounds(0, 370, 90, 460)); //Potions button
        addCue("NoPotions", "cues/autorevive/cueNoPotions.png", new Bounds(210, 190, 590, 350)); // The team does not need revive
        addCue("Restores", "cues/autorevive/cueRestores.png", new Bounds(145, 320, 655, 395)); // To identify revive and healing potions
        addCue("Revives", "cues/autorevive/cueRevives.png", new Bounds(145, 320, 655, 395)); // To identify revive potions
        addCue("MinorAvailable", "cues/autorevive/cueMinorAvailable.png", new Bounds(170, 205, 270, 300));
        addCue("AverageAvailable", "cues/autorevive/cueAverageAvailable.png", new Bounds(350, 205, 450, 300));
        addCue("MajorAvailable", "cues/autorevive/cueMajorAvailable.png", new Bounds(535, 205, 635, 300));
        addCue("SuperAvailable", "cues/autorevive/cueSuperAvailable.png", new Bounds(140, 150, 300, 200));
        addCue("UnitSelect", "cues/autorevive/cueUnitSelect.png", new Bounds(130, 20, 680, 95));
        addCue("ScrollerRightDisabled", "cues/autorevive/cueScrollerRightDisabled.png", Bounds.fromWidthHeight(646, 425, 18, 18));
        addCue("GravestoneHighlighted", "cues/autorevive/highlighted_gravestone.png", new Bounds(50, 230, 340, 400));

        //Items related cues
        addCue("ItemHer", "cues/items/cueItemHer.png", null); // Heroic Item border
        addCue("ItemLeg", "cues/items/cueItemLeg.png", null); // Legendary Item border
        addCue("ItemSet", "cues/items/cueItemSet.png", null); // Set Item border
        addCue("ItemMyt", "cues/items/cueItemMyt.png", null); // Mythical Item border
        //legendary
        addCue("Material_R11", "cues/items/material_r11.png", null);
        addCue("Material_R10", "cues/items/material_r10.png", null);
        addCue("Material_R9", "cues/items/material_r9.png", null);
        addCue("Material_R8", "cues/items/material_r8.png", null);
        addCue("Material_R7", "cues/items/material_r7.png", null);
        addCue("Material_R6", "cues/items/material_r6.png", null);
        addCue("Material_R5", "cues/items/material_r5.png", null);
        addCue("Material_R4", "cues/items/material_r4.png", null);
        addCue("Material_R3", "cues/items/material_r3.png", null);
        addCue("Material_R2", "cues/items/material_r2.png", null);
        //heroic
        addCue("HeroicSchematic", "cues/items/heroic_schematic.png", null);
        addCue("MicroChip", "cues/items/microchip.png", null);
        addCue("GoldCoin", "cues/items/goldcoin.png", null);
        addCue("DemonBlood", "cues/items/demon_blood.png", null);
        addCue("HobbitsFoot", "cues/items/hobbits_foot.png", null);
        addCue("MelvinChest", "cues/items/melvin_chest.png", null);
        addCue("NeuralNetRom", "cues/items/neural_net_rom.png", null);
        addCue("ScarlargSkin", "cues/items/scarlarg_skin.png", null);

        //weekly reward cues
        //these include the top of the loot window so they aren't triggered by the text in the activity panel
        addCue("PVP_Rewards", "cues/weeklyrewards/pvp.png", new Bounds(290, 130, 510, 160));
        addCue("Trials_Rewards", "cues/weeklyrewards/trials.png", new Bounds(290, 130, 510, 160));
        addCue("Trials_Rewards_Large", "cues/weeklyrewards/trials_large.png", new Bounds(290, 50, 510, 130));
        addCue("Gauntlet_Rewards", "cues/weeklyrewards/gauntlet.png", new Bounds(290, 130, 510, 160));
        addCue("Gauntlet_Rewards_Large", "cues/weeklyrewards/gauntlet_large.png", new Bounds(290, 50, 510, 130));
        addCue("GVG_Rewards", "cues/weeklyrewards/gvg.png", new Bounds(290, 130, 510, 160));
        addCue("Invasion_Rewards", "cues/weeklyrewards/invasion.png", new Bounds(290, 130, 510, 160));
        addCue("Expedition_Rewards", "cues/weeklyrewards/expedition.png", new Bounds(290, 130, 510, 160));
        addCue("Fishing_Rewards", "cues/weeklyrewards/fishing.png", new Bounds(290, 130, 510, 160));
        addCue("Fishing_Bait", "cues/weeklyrewards/fishing_bait.png", Bounds.fromWidthHeight(391, 199, 70, 69));


        int newFamCnt = loadCueFolder("cues/familiars/01 Common", null, false, new Bounds(145, 50, 575, 125));
        newFamCnt += loadCueFolder("cues/familiars/02 Rare", null, false, new Bounds(145, 50, 575, 125));
        newFamCnt += loadCueFolder("cues/familiars/03 Epic", null, false, new Bounds(145, 50, 575, 125));
        newFamCnt += loadCueFolder("cues/familiars/04 Legendary", null, false, new Bounds(145, 50, 575, 125));
        BHBot.logger.debug("Found " + newFamCnt + " familiar cues.");
    }
}
