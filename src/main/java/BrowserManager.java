import com.assertthat.selenium_shutterbug.core.Shutterbug;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrowserManager {
    static Map<String, Cue> cues = new HashMap<>();
    private static By byElement;

    private WebDriver driver;
    private JavascriptExecutor jsExecutor;
    private WebElement game;
    public String doNotShareUrl = "";

    private BufferedImage img; // latest screen capture
    private BHBot bot;

    BrowserManager(BHBot bot) {
        this.bot = bot;
    }

    static void addCue(String name, BufferedImage im, Bounds bounds) {
        cues.put(name, new Cue(name, im, bounds));
    }

    static BufferedImage loadImage(String f) {
        BufferedImage img = null;
        ClassLoader classLoader = DungeonThread.class.getClassLoader();
        InputStream resourceURL = classLoader.getResourceAsStream(f);

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

    @SuppressWarnings("SameParameterValue")
    static int loadCueFolder(String cuesPath, String prefix, boolean stripCueStr, Bounds bounds) {
        int totalLoaded = 0;

        // We make sure that the last char of the path is a folder separator
        if (!"/".equals(cuesPath.substring(cuesPath.length() - 1))) cuesPath += "/";

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        URL url = classLoader.getResource(cuesPath);
        if (url != null) { // Run from the IDE
            if ("file".equals(url.getProtocol())) {

                InputStream in = classLoader.getResourceAsStream(cuesPath);
                if (in == null) {
                    BHBot.logger.error("Impossible to create InputStream in loadCueFolder");
                    return totalLoaded;
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String resource;

                while (true) {
                    try {
                        resource = br.readLine();
                        if (resource == null) break;
                    } catch (IOException e) {
                        BHBot.logger.error("Error while reading resources in loadCueFoler", e);
                        continue;
                    }
                    int dotPosition = resource.lastIndexOf('.');
                    String fileExtension = dotPosition > 0 ? resource.substring(dotPosition + 1) : "";
                    if ("png".equals(fileExtension.toLowerCase())) {
                        String cueName = resource.substring(0, dotPosition);

                        if (prefix != null) cueName = prefix + cueName;
                        if (stripCueStr) cueName = cueName.replace("cue", "");

                        addCue(cueName.toLowerCase(), loadImage(cuesPath + resource), bounds);
                        totalLoaded++;
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
                    return totalLoaded;
                }

                JarFile jar;
                try {
                    jar = new JarFile(decodedURL);
                } catch (IOException e) {
                    BHBot.logger.error("Impossible to open JAR file : " + decodedURL, e);
                    return totalLoaded;
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

                            if (prefix != null) cueName = prefix + cueName;
                            if (stripCueStr) cueName = cueName.replace("cue", "");
                            BHBot.logger.trace("cueName: " + cueName);

                            // resourceRelativePath begins with a '/' char and we want to be sure to remove it
                            addCue(cueName.toLowerCase(), loadImage(resourceRelativePath.substring(1)), bounds);
                            totalLoaded++;
                        }
                    }
                }

            }
        }

        return totalLoaded;
    }

    static void buildCues() {
        BrowserManager.addCue("Main", BrowserManager.loadImage("cues/cueMainScreen.png"), new Bounds(90, 5, 100, 10));
        BrowserManager.addCue("Login", BrowserManager.loadImage("cues/cueLogin.png"), new Bounds(270, 260, 330, 300)); // login window (happens seldom)
        BrowserManager.addCue("AreYouThere", BrowserManager.loadImage("cues/cueAreYouThere.png"), new Bounds(240, 245, 265, 260));
        BrowserManager.addCue("Yes", BrowserManager.loadImage("cues/cueYes.png"), null);

        BrowserManager.addCue("Disconnected", BrowserManager.loadImage("cues/cueDisconnected.png"), Bounds.fromWidthHeight(299, 232, 202, 70)); // cue for "You have been disconnected" popup
        BrowserManager.addCue("Reconnect", BrowserManager.loadImage("cues/cueReconnectButton.png"), new Bounds(320, 330, 400, 360)); // used with "You have been disconnected" dialog and also with the "maintenance" dialog
        BrowserManager.addCue("Reload", BrowserManager.loadImage("cues/cueReload.png"), new Bounds(320, 330, 360, 360)); // used in "There is a new update required to play" dialog (happens on Friday night)
        BrowserManager.addCue("Maintenance", BrowserManager.loadImage("cues/cueMaintenance.png"), new Bounds(230, 200, 320, 250)); // cue for "Bit Heroes is currently down for maintenance. Please check back shortly!"
        BrowserManager.addCue("Loading", BrowserManager.loadImage("cues/cueLoading.png"), new Bounds(315, 210, 330, 225)); // cue for "Loading" superimposed screen
        BrowserManager.addCue("RecentlyDisconnected", BrowserManager.loadImage("cues/cueRecentlyDisconnected.png"), new Bounds(250, 195, 535, 320));; // cue for "You were recently disconnected from a dungeon. Do you want to continue the dungeon?" window
        BrowserManager.addCue("UnableToConnect", BrowserManager.loadImage("cues/cueUnableToConnect.png"), new Bounds(245, 235, 270, 250)); // happens when some error occurs for which the flash app is unable to connect to the server. We must simply click on the "Reconnect" button in this case!

        BrowserManager.addCue("DailyRewards", BrowserManager.loadImage("cues/cueDailyRewards.png"), new Bounds(260, 45, 285, 75));
        BrowserManager.addCue("Claim", BrowserManager.loadImage("cues/cueClaim.png"), null); // claim button, when daily rewards popup is open
        BrowserManager.addCue("Items", BrowserManager.loadImage("cues/cueItems.png"), null); // used when we clicked "claim" on daily rewards popup. Used also with main menu ads.
        BrowserManager.addCue("X", BrowserManager.loadImage("cues/cueX.png"), null); // "X" close button used with claimed daily rewards popup
        BrowserManager.addCue("WeeklyRewards", BrowserManager.loadImage("cues/cueWeeklyRewards.png"), new Bounds(185, 95, 250, 185)); // used with reward for GVG/PVP/Gauntlet/Trial on Friday night (when day changes into Saturday)
        BrowserManager.addCue("Bounties", BrowserManager.loadImage("cues/cueBounties.png"), new Bounds(320, 63, 480, 103));
        BrowserManager.addCue("Loot", BrowserManager.loadImage("cues/cueLoot.png"), null);

        BrowserManager.addCue("News", BrowserManager.loadImage("cues/cueNewsPopup.png"), new Bounds(345, 60, 365, 85)); // news popup
        BrowserManager.addCue("Close", BrowserManager.loadImage("cues/cueClose.png"), null); // close button used with "News" popup, also when defeated in dungeon, etc.

        BrowserManager.addCue("EnergyBar", BrowserManager.loadImage("cues/cueEnergyBar.png"), new Bounds(390, 0, 420, 20));
        BrowserManager.addCue("TicketBar", BrowserManager.loadImage("cues/cueTicketBar.png"), new Bounds(540, 0, 770, 20));

        BrowserManager.addCue("RaidButton", BrowserManager.loadImage("cues/cueRaidButton.png"), new Bounds(0, 200, 40, 400));
        BrowserManager.addCue("RaidPopup", BrowserManager.loadImage("cues/cueRaidPopup.png"), Bounds.fromWidthHeight(300, 35, 70, 60));
        BrowserManager.addCue("RaidSummon", BrowserManager.loadImage("cues/cueRaidSummon.png"), new Bounds(480, 360, 540, 380));
        BrowserManager.addCue("RaidLevel", BrowserManager.loadImage("cues/cueRaidLevel.png"), new Bounds(300, 435, 510, 455)); // selected raid type button cue
        BrowserManager.addCue("cueRaidLevelEmpty", BrowserManager.loadImage("cues/cueRaidLevelEmpty.png"), new Bounds(300, 435, 510, 455)); // selected raid type button cue

        // New Raid level detection logic
        BrowserManager.addCue("Raid1Name", BrowserManager.loadImage("cues/raid/r1Name.png"), new Bounds(185, 340, 485, 395));// Raid 1 Name
        BrowserManager.addCue("R1Only", BrowserManager.loadImage("cues/cueR1Only.png"), null); // cue for R1 type selected when R2 (and R3) is not open yet (in that case it won't show raid type selection buttons)
        BrowserManager.addCue("Normal", BrowserManager.loadImage("cues/cueNormal.png"), null);
        BrowserManager.addCue("Hard", BrowserManager.loadImage("cues/cueHard.png"), null);
        BrowserManager.addCue("Heroic", BrowserManager.loadImage("cues/cueHeroic.png"), null);
        BrowserManager.addCue("Accept", BrowserManager.loadImage("cues/cueAccept.png"), null);
        BrowserManager.addCue("D4Accept", BrowserManager.loadImage("cues/cueD4Accept.png"), null);
        BrowserManager.addCue("Cleared", BrowserManager.loadImage("cues/cueCleared.png"), new Bounds(208, 113, 608, 394)); // used for example when raid has been finished
        BrowserManager.addCue("Defeat", BrowserManager.loadImage("cues/cueDefeat.png"), new Bounds(320, 50, 480, 180)); // used for example when you have been defeated in a dungeon. Also used when you have been defeated in a gauntlet.
        BrowserManager.addCue("YesGreen", BrowserManager.loadImage("cues/cueYesGreen.png"), null); // used for example when raid has been finished ("Cleared" popup)
        BrowserManager.addCue("Persuade", BrowserManager.loadImage("cues/cuePersuade.png"), new Bounds(116, 311, 286, 380));
        BrowserManager.addCue("View", BrowserManager.loadImage("cues/cueView.png"), new Bounds(390, 415, 600, 486));
        BrowserManager.addCue("Bribe", BrowserManager.loadImage("cues/cueBribe.png"), new Bounds(505, 305, 684, 375));
        BrowserManager.addCue("SkeletonTreasure", BrowserManager.loadImage("cues/cueSkeletonTreasure.png"), new Bounds(185, 165, 295, 280)); // skeleton treasure found in dungeons (it's a dialog/popup cue)
        BrowserManager.addCue("SkeletonNoKeys", BrowserManager.loadImage("cues/cueSkeletonNoKeys.png"), new Bounds(478, 318, 500, 348)); // red 0

        BrowserManager.addCue("Open", BrowserManager.loadImage("cues/cueOpen.png"), null); // skeleton treasure open button
        BrowserManager.addCue("Decline", BrowserManager.loadImage("cues/cueDecline.png"), null); // decline skeleton treasure button (found in dungeons), also with video ad treasures (found in dungeons)
        BrowserManager.addCue("DeclineRed", BrowserManager.loadImage("cues/cueDeclineRed.png"), null); // decline persuation attempts
        BrowserManager.addCue("Merchant", BrowserManager.loadImage("cues/cueMerchant.png"), null); // cue for merchant dialog/popup
        BrowserManager.addCue("SettingsGear", BrowserManager.loadImage("cues/cueSettingsGear.png"), new Bounds(655, 450, 730, 515)); // settings button
        BrowserManager.addCue("Settings", BrowserManager.loadImage("cues/cueSettings.png"), new Bounds(249, 61, 558, 102)); // settings menu

        BrowserManager.addCue("Team", BrowserManager.loadImage("cues/cueTeam.png"), null); // Team text part of pop-ups about teams
        BrowserManager.addCue("TeamNotFull", BrowserManager.loadImage("cues/cueTeamNotFull.png"), new Bounds(230, 200, 330, 250)); // warning popup when some friend left you and your team is not complete anymore
        BrowserManager.addCue("TeamNotOrdered", BrowserManager.loadImage("cues/cueTeamNotOrdered.png"), new Bounds(230, 190, 350, 250)); // warning popup when some guild member left and your GvG team is not complete anymore
        BrowserManager.addCue("GuildLeaveConfirm", BrowserManager.loadImage("cues/cueGuildLeaveConfirm.png"), new Bounds(195, 105, 605, 395)); // GVG confirm
        BrowserManager.addCue("DisabledBattles", BrowserManager.loadImage("cues/cueDisabledBattles.png"), new Bounds(240, 210, 560, 330)); // Disabled Battles Poppup

        BrowserManager.addCue("No", BrowserManager.loadImage("cues/cueNo.png"), null); // cue for a blue "No" button used for example with "Your team is not full" dialog, or for "Replace consumable" dialog, etc. This is why we can't put concrete borders as position varies a lot.
        BrowserManager.addCue("AutoTeam", BrowserManager.loadImage("cues/cueAutoTeam.png"), null); // "Auto" button that automatically assigns team (in raid, GvG, ...)
        BrowserManager.addCue("Clear", BrowserManager.loadImage("cues/cueClear.png"), null); //clear team button

        BrowserManager.addCue("AutoOn", BrowserManager.loadImage("cues/cueAutoOn.png"), new Bounds(740, 180, 785, 220)); // cue for auto pilot on
        BrowserManager.addCue("AutoOff", BrowserManager.loadImage("cues/cueAutoOff.png"), new Bounds(740, 180, 785, 220)); // cue for auto pilot off
        BrowserManager.addCue("Speed_Full", BrowserManager.loadImage("cues/Speed_Full.png"), new Bounds(7, 488, 65, 504)); // 3/3 speed bar in encounters
        BrowserManager.addCue("Speed", BrowserManager.loadImage("cues/Speed_Text.png"), new Bounds(20, 506, 61, 518)); // speed text label in encounters

        BrowserManager.addCue("Trials", BrowserManager.loadImage("cues/cueTrials.png"), new Bounds(0, 0, 40, 400)); // cue for trials button (note that as of 23.9.2017 they changed the button position to the right side of the screen and modified the glyph)
        BrowserManager.addCue("Trials2", BrowserManager.loadImage("cues/cueTrials2.png"), new Bounds(720, 0, 770, 400)); // an alternative cue for trials (flipped horizontally, located on the right side of the screen). Used since 23.9.2017.
        BrowserManager.addCue("Gauntlet", BrowserManager.loadImage("cues/cueGauntlet.png"), null); // cue for gauntlet button
        BrowserManager.addCue("Gauntlet2", BrowserManager.loadImage("cues/cueGauntlet2.png"), null); // alternative cue for gauntlet button
        BrowserManager.addCue("Play", BrowserManager.loadImage("cues/cuePlay.png"), null); // cue for play button in trials/gauntlet window
        BrowserManager.addCue("TokenBar", BrowserManager.loadImage("cues/cueTokenBar.png"), null);
        BrowserManager.addCue("CloseGreen", BrowserManager.loadImage("cues/cueCloseGreen.png"), null); // close button used with "You have been defeated" popup in gauntlet and also "Victory" window in gauntlet
        BrowserManager.addCue("VictorySmall", BrowserManager.loadImage("cues/Victory_Small.png"), null); // victory window cue found upon completing gauntlet / PvP

        BrowserManager.addCue("UhOh", BrowserManager.loadImage("cues/cueUhoh.png"), new Bounds(319, 122, 526, 184));
        BrowserManager.addCue("ReviveAverage", BrowserManager.loadImage("cues/cueReviveAverage.png"), null);
        BrowserManager.addCue("Purchase", BrowserManager.loadImage("cues/cuePurchase.png"), new Bounds(240, 240, 390, 280));

        BrowserManager.addCue("GuildButton", BrowserManager.loadImage("cues/cueGuildButton.png"), new Bounds(500, 420, 590, 520));
        BrowserManager.addCue("IgnoreShrines", BrowserManager.loadImage("cues/cueIgnoreShrines.png"), new Bounds(120, 250, 675, 475));
        BrowserManager.addCue("IgnoreBoss", BrowserManager.loadImage("cues/cueIgnoreBoss.png"), new Bounds(120, 250, 675, 475));

        BrowserManager.addCue("Quest", BrowserManager.loadImage("cues/cueQuest.png"), new Bounds(0, 0, 40, 40)); // cue for quest (dungeons) button
        BrowserManager.addCue("ZonesButton", BrowserManager.loadImage("cues/cueZonesButton.png"), new Bounds(105, 60, 125, 75));
        BrowserManager.addCue("Zone1", BrowserManager.loadImage("cues/cueZone1.png"), null);
        BrowserManager.addCue("Zone2", BrowserManager.loadImage("cues/cueZone2.png"), null);
        BrowserManager.addCue("Zone3", BrowserManager.loadImage("cues/cueZone3.png"), null);
        BrowserManager.addCue("Zone4", BrowserManager.loadImage("cues/cueZone4.png"), null);
        BrowserManager.addCue("Zone5", BrowserManager.loadImage("cues/cueZone5.png"), null);
        BrowserManager.addCue("Zone6", BrowserManager.loadImage("cues/cueZone6.png"), null);
        BrowserManager.addCue("Zone7", BrowserManager.loadImage("cues/cueZone7.png"), null);
        BrowserManager.addCue("Zone8", BrowserManager.loadImage("cues/cueZone8.png"), null);
        BrowserManager.addCue("Zone9", BrowserManager.loadImage("cues/cueZone9.png"), null);
        BrowserManager.addCue("Zone10", BrowserManager.loadImage("cues/cueZone10.png"), null);
        BrowserManager.addCue("RightArrow", BrowserManager.loadImage("cues/cueRightArrow.png"), null); // arrow used in quest screen to change zone
        BrowserManager.addCue("LeftArrow", BrowserManager.loadImage("cues/cueLeftArrow.png"), null); // arrow used in quest screen to change zone
        BrowserManager.addCue("Enter", BrowserManager.loadImage("cues/cueEnter.png"), null); // "Enter" button found on d4 window
        BrowserManager.addCue("NotEnoughEnergy", BrowserManager.loadImage("cues/cueNotEnoughEnergy.png"), new Bounds(260, 210, 290, 235)); // "Not enough Energy" popup cue

        BrowserManager.addCue("PVP", BrowserManager.loadImage("cues/cuePVP.png"), new Bounds(0, 70, 40, 110)); // PVP icon in main screen
        BrowserManager.addCue("Fight", BrowserManager.loadImage("cues/cueFight.png"), null); // fight button in PVP window
        BrowserManager.addCue("PVPWindow", BrowserManager.loadImage("cues/cuePVPWindow.png"), null); // PVP window cue

        BrowserManager.addCue("DialogRight", BrowserManager.loadImage("cues/cueDialogRight.png"), new Bounds(675, 205, 690, 250)); // cue for the dialog window (when arrow is at the right side of the window)
        BrowserManager.addCue("DialogLeft", BrowserManager.loadImage("cues/cueDialogLeft.png"), new Bounds(100, 205, 125, 250)); // cue for the dialog window (when arrow is at the left side of the window)

        BrowserManager.addCue("SpeedCheck", BrowserManager.loadImage("cues/cueSpeedCheck.png"), new Bounds(0, 450, 100, 520));
        BrowserManager.addCue("Switch", BrowserManager.loadImage("cues/cueSwitch.png"), new Bounds(0, 450, 100, 520)); //unused

        // GVG related:
        BrowserManager.addCue("GVG", BrowserManager.loadImage("cues/cueGVG.png"), null); // main GVG button cue
        BrowserManager.addCue("BadgeBar", BrowserManager.loadImage("cues/cueBadgeBar.png"), null);
        BrowserManager.addCue("GVGWindow", BrowserManager.loadImage("cues/cueGVGWindow.png"), new Bounds(260, 90, 280, 110)); // GVG window cue

        BrowserManager.addCue("InGamePM", BrowserManager.loadImage("cues/cueInGamePM.png"), new Bounds(450, 330, 530, 380)); // note that the guild window uses the same cue! That's why it's important that user doesn't open guild window while bot is working!

        BrowserManager.addCue("TrialsOrGauntletWindow", BrowserManager.loadImage("cues/cueTrialsOrGauntletWindow.png"), new Bounds(300, 30, 510, 105)); // cue for a trials/gauntlet window
        BrowserManager.addCue("NotEnoughTokens", BrowserManager.loadImage("cues/cueNotEnoughTokens.png"), Bounds.fromWidthHeight(274, 228, 253, 79)); // cue to check for the not enough tokens popup

        BrowserManager.addCue("Difficulty", BrowserManager.loadImage("cues/cueDifficulty.png"), new Bounds(450, 330, 640, 450)); // selected difficulty in trials/gauntlet window
        BrowserManager.addCue("DifficultyDisabled", BrowserManager.loadImage("cues/cueDifficultyDisabled.png"), new Bounds(450, 330, 640, 450)); // selected difficulty in trials/gauntlet window (disabled - because game is still fetching data from server)
        BrowserManager.addCue("SelectDifficulty", BrowserManager.loadImage("cues/cueSelectDifficulty.png"), new Bounds(400, 260, 0, 0)/*not exact bounds... the lower-right part of screen!*/); // select difficulty button in trials/gauntlet
        BrowserManager.addCue("DifficultyDropDown", BrowserManager.loadImage("cues/cueDifficultyDropDown.png"), new Bounds(260, 50, 550, 125)); // difficulty drop down menu cue
        BrowserManager.addCue("DifficultyExpedition", BrowserManager.loadImage("cues/cueDifficultyExpedition.png"), null); // selected difficulty in trials/gauntlet window
        BrowserManager.addCue("SelectDifficultyExpedition", BrowserManager.loadImage("cues/cueSelectDifficultyExpedition.png"), null);
        BrowserManager.addCue("DropDownUp", BrowserManager.loadImage("cues/cueDropDownUp.png"), null); // up arrow in difficulty drop down menu (found in trials/gauntlet, for example)
        BrowserManager.addCue("DropDownDown", BrowserManager.loadImage("cues/cueDropDownDown.png"), null); // down arrow in difficulty drop down menu (found in trials/gauntlet, for example)
        BrowserManager.addCue("Cost", BrowserManager.loadImage("cues/cueCost.png"), new Bounds(400, 150, 580, 240)); // used both for PvP and Gauntlet/Trials costs. Note that bounds are very wide, because position of this cue in PvP is different from that in Gauntlet/Trials!
        BrowserManager.addCue("SelectCost", BrowserManager.loadImage("cues/cueSelectCost.png"), new Bounds(555, 170, 595, 205)); // cue for select cost found in both PvP and Gauntlet/Trials windows. Note that bounds are wide, because position of this cue in PvP is different from that in Gauntlet/Trials!
        BrowserManager.addCue("CostDropDown", BrowserManager.loadImage("cues/cueCostDropDown.png"), new Bounds(260, 45, 320, 70)); // cue for cost selection drop down window
        BrowserManager.addCue("0", BrowserManager.loadImage("cues/numbers/cue0.png"), null);
        BrowserManager.addCue("1", BrowserManager.loadImage("cues/numbers/cue1.png"), null);
        BrowserManager.addCue("2", BrowserManager.loadImage("cues/numbers/cue2.png"), null);
        BrowserManager.addCue("3", BrowserManager.loadImage("cues/numbers/cue3.png"), null);
        BrowserManager.addCue("4", BrowserManager.loadImage("cues/numbers/cue4.png"), null);
        BrowserManager.addCue("5", BrowserManager.loadImage("cues/numbers/cue5.png"), null);
        BrowserManager.addCue("6", BrowserManager.loadImage("cues/numbers/cue6.png"), null);
        BrowserManager.addCue("7", BrowserManager.loadImage("cues/numbers/cue7.png"), null);
        BrowserManager.addCue("8", BrowserManager.loadImage("cues/numbers/cue8.png"), null);
        BrowserManager.addCue("9", BrowserManager.loadImage("cues/numbers/cue9.png"), null);
        BrowserManager.addCue("small0", BrowserManager.loadImage("cues/numbers/small0.png"), null);
        BrowserManager.addCue("small1", BrowserManager.loadImage("cues/numbers/small1.png"), null);
        BrowserManager.addCue("small2", BrowserManager.loadImage("cues/numbers/small2.png"), null);
        BrowserManager.addCue("small3", BrowserManager.loadImage("cues/numbers/small3.png"), null);
        BrowserManager.addCue("small4", BrowserManager.loadImage("cues/numbers/small4.png"), null);
        BrowserManager.addCue("small5", BrowserManager.loadImage("cues/numbers/small5.png"), null);
        BrowserManager.addCue("small6", BrowserManager.loadImage("cues/numbers/small6.png"), null);
        BrowserManager.addCue("small7", BrowserManager.loadImage("cues/numbers/small7.png"), null);
        BrowserManager.addCue("small8", BrowserManager.loadImage("cues/numbers/small8.png"), null);
        BrowserManager.addCue("small9", BrowserManager.loadImage("cues/numbers/small9.png"), null);

        // PvP strip related:
        BrowserManager.addCue("StripScrollerTopPos", BrowserManager.loadImage("cues/strip/cueStripScrollerTopPos.png"), new Bounds(525, 140, 540, 370));
        BrowserManager.addCue("StripEquipped", BrowserManager.loadImage("cues/strip/cueStripEquipped.png"), new Bounds(465, 180, 485, 200)); // the little "E" icon upon an equipped item (the top-left item though, we want to detect just that one)
        BrowserManager.addCue("StripItemsTitle", BrowserManager.loadImage("cues/strip/cueStripItemsTitle.png"), new Bounds(335, 70, 360, 80));
        BrowserManager.addCue("StripSelectorButton", BrowserManager.loadImage("cues/strip/cueStripSelectorButton.png"), new Bounds(450, 115, 465, 130));

        // filter titles:
        BrowserManager.addCue("StripTypeBody", BrowserManager.loadImage("cues/strip/cueStripTypeBody.png"), new Bounds(460, 125, 550, 140));
        BrowserManager.addCue("StripTypeHead", BrowserManager.loadImage("cues/strip/cueStripTypeHead.png"), new Bounds(460, 125, 550, 140));
        BrowserManager.addCue("StripTypeMainhand", BrowserManager.loadImage("cues/strip/cueStripTypeMainhand.png"), new Bounds(460, 125, 550, 140));
        BrowserManager.addCue("StripTypeOffhand", BrowserManager.loadImage("cues/strip/cueStripTypeOffhand.png"), new Bounds(460, 125, 550, 140));
        BrowserManager.addCue("StripTypeNeck", BrowserManager.loadImage("cues/strip/cueStripTypeNeck.png"), new Bounds(460, 125, 550, 140));
        BrowserManager.addCue("StripTypeRing", BrowserManager.loadImage("cues/strip/cueStripTypeRing.png"), new Bounds(460, 125, 550, 140));

        // consumables management related:
        BrowserManager.addCue("BonusExp", BrowserManager.loadImage("cues/cueBonusExp.png"), new Bounds(100, 455, 370, 485)); // consumable icon in the main menu (when it's being used)
        BrowserManager.addCue("BonusItem", BrowserManager.loadImage("cues/cueBonusItem.png"), new Bounds(100, 455, 370, 485));
        BrowserManager.addCue("BonusGold", BrowserManager.loadImage("cues/cueBonusGold.png"), new Bounds(100, 455, 370, 485));
        BrowserManager.addCue("BonusSpeed", BrowserManager.loadImage("cues/cueBonusSpeed.png"), new Bounds(100, 455, 370, 485));
        BrowserManager.addCue("ConsumableExpMinor", BrowserManager.loadImage("cues/cueConsumableExpMinor.png"), null); // consumable icon in the inventory
        BrowserManager.addCue("ConsumableExpAverage", BrowserManager.loadImage("cues/cueConsumableExpAverage.png"), null);
        BrowserManager.addCue("ConsumableExpMajor", BrowserManager.loadImage("cues/cueConsumableExpMajor.png"), null);
        BrowserManager.addCue("ConsumableItemMinor", BrowserManager.loadImage("cues/cueConsumableItemMinor.png"), null);
        BrowserManager.addCue("ConsumableItemAverage", BrowserManager.loadImage("cues/cueConsumableItemAverage.png"), null);
        BrowserManager.addCue("ConsumableItemMajor", BrowserManager.loadImage("cues/cueConsumableItemMajor.png"), null);
        BrowserManager.addCue("ConsumableSpeedMinor", BrowserManager.loadImage("cues/cueConsumableSpeedMinor.png"), null);
        BrowserManager.addCue("ConsumableSpeedAverage", BrowserManager.loadImage("cues/cueConsumableSpeedAverage.png"), null);
        BrowserManager.addCue("ConsumableSpeedMajor", BrowserManager.loadImage("cues/cueConsumableSpeedMajor.png"), null);
        BrowserManager.addCue("ConsumableGoldMinor", BrowserManager.loadImage("cues/cueConsumableGoldMinor.png"), null);
        BrowserManager.addCue("ConsumableGoldAverage", BrowserManager.loadImage("cues/cueConsumableGoldAverage.png"), null);
        BrowserManager.addCue("ConsumableGoldMajor", BrowserManager.loadImage("cues/cueConsumableGoldMajor.png"), null);
        BrowserManager.addCue("ConsumablePumkgor", BrowserManager.loadImage("cues/cueConsumablePumkgor.png"), new Bounds(150, 460, 205, 519)); // Special Halloween consumable
        BrowserManager.addCue("ConsumableGingernaut", BrowserManager.loadImage("cues/cueConsumableGingernaut.png"), new Bounds(150, 460, 205, 519)); // Special Chrismast consumable
        BrowserManager.addCue("ConsumableGreatFeast", BrowserManager.loadImage("cues/cueConsumableGreatFeast.png"), new Bounds(150, 460, 205, 519)); // Thanksgiving consumable
        BrowserManager.addCue("ConsumableBroccoli", BrowserManager.loadImage("cues/cueConsumableBroccoli.png"), new Bounds(150, 460, 205, 519)); // Special Halloween consumable
        BrowserManager.addCue("ConsumableCoco", BrowserManager.loadImage("cues/cueConsumableCoco.png"), new Bounds(150, 460, 205, 519)); // Special ?? consumable
        BrowserManager.addCue("ScrollerAtBottom", BrowserManager.loadImage("cues/cueScrollerAtBottom.png"), null); // cue for scroller being at the bottom-most position (we can't scroll down more than this)
        BrowserManager.addCue("ConsumableTitle", BrowserManager.loadImage("cues/cueConsumableTitle.png"), new Bounds(280, 100, 310, 180)); // cue for title of the window that pops up when we want to consume a consumable. Note that vertical range is big here since sometimes is higher due to greater window size and sometimes is lower.
        BrowserManager.addCue("FilterConsumables", BrowserManager.loadImage("cues/cueFilterConsumables.png"), new Bounds(460, 125, 550, 140)); // cue for filter button name
        BrowserManager.addCue("LoadingInventoryIcon", BrowserManager.loadImage("cues/cueLoadingInventoryIcon.png"), null); // cue for loading animation for the icons inside inventory

        // rune management related:
        BrowserManager.addCue("Runes", BrowserManager.loadImage("cues/cueRunes.png"), new Bounds(120, 450, 245, 495)); // runes button in profile
        BrowserManager.addCue("RunesLayout", BrowserManager.loadImage("cues/cueRunesLayout.png"), new Bounds(340, 70, 460, 110)); // runes layout header
        BrowserManager.addCue("RunesPicker", BrowserManager.loadImage("cues/cueRunesPicker.png"), null); // rune picker
        BrowserManager.addCue("RunesSwitch", BrowserManager.loadImage("cues/cueRunesSwitch.png"), new Bounds(320, 260, 480, 295)); // rune picker

        // All minor rune cues
        for (DungeonThread.MinorRune rune : DungeonThread.MinorRune.values()) {
            BrowserManager.addCue(rune.getRuneCueName(), BrowserManager.loadImage(rune.getRuneCueFileName()), null);
            BrowserManager.addCue(rune.getRuneSelectCueName(), BrowserManager.loadImage(rune.getRuneSelectCueFileName()), new Bounds(235, 185, 540, 350));
        }

        // invasion related:
        BrowserManager.addCue("Invasion", BrowserManager.loadImage("cues/cueInvasion.png"), null);
        BrowserManager.addCue("InvasionWindow", BrowserManager.loadImage("cues/cueInvasionWindow.png"), new Bounds(260, 90, 280, 110)); // GVG window cue

        // Expedition related:
        BrowserManager.addCue("ExpeditionButton", BrowserManager.loadImage("cues/cueExpeditionButton.png"), null);
        BrowserManager.addCue("Expedition1", BrowserManager.loadImage("cues/expedition/cueExpedition1Hallowed.png"), new Bounds(168, 34, 628, 108)); // Hallowed Expedtion Title
        BrowserManager.addCue("Expedition2", BrowserManager.loadImage("cues/expedition/cueExpedition2Inferno.png"), new Bounds(200, 40, 600, 100)); //Inferno Expedition
        BrowserManager.addCue("Expedition3", BrowserManager.loadImage("cues/expedition/cueExpedition3Jammie.png"), new Bounds(230, 40, 565, 100)); //Jammie Dimension
        BrowserManager.addCue("Expedition4", BrowserManager.loadImage("cues/expedition/cueExpedition4Idol.png"), new Bounds(230, 40, 565, 100)); //Idol Dimension
        BrowserManager.addCue("Expedition5", BrowserManager.loadImage("cues/expedition/cueExpedition5BattleBards.png"), new Bounds(230, 40, 565, 100)); //Battle Bards!

        //WorldBoss Related
        BrowserManager.addCue("WorldBoss", BrowserManager.loadImage("cues/cueWorldBoss.png"), null);
        BrowserManager.addCue("WorldBossSelector", BrowserManager.loadImage("cues/cueWorldBossSelector.png"), null);
        BrowserManager.addCue("BlueSummon", BrowserManager.loadImage("cues/cueBlueSummon.png"), null);
        BrowserManager.addCue("LargeGreenSummon", BrowserManager.loadImage("cues/cueLargeGreenSummon.png"), null);
        BrowserManager.addCue("SmallGreenSummon", BrowserManager.loadImage("cues/cueSmallGreenSummon.png"), null);
        BrowserManager.addCue("Invite", BrowserManager.loadImage("cues/cueInvite.png"), null);
        BrowserManager.addCue("Start", BrowserManager.loadImage("cues/cueStart.png"), null);
        BrowserManager.addCue("VictoryLarge", BrowserManager.loadImage("cues/Victory_Large.png"), new Bounds(324, 128, 476, 157));
        BrowserManager.addCue("OrlagSelected", BrowserManager.loadImage("cues/cueOrlagSelected.png"), new Bounds(360, 430, 440, 460));
        BrowserManager.addCue("NetherSelected", BrowserManager.loadImage("cues/cueNetherSelected.png"), null);
        BrowserManager.addCue("Private", BrowserManager.loadImage("cues/cuePrivate.png"), new Bounds(310, 320, 370, 380));
        BrowserManager.addCue("Unready", BrowserManager.loadImage("cues/cueWorldBossUnready.png"), new Bounds(170, 210, 215, 420));
        BrowserManager.addCue("WorldBossTier", BrowserManager.loadImage("cues/cueWorldBossTier.png"), Bounds.fromWidthHeight(314, 206, 88, 28));
        BrowserManager.addCue("WorldBossTierDropDown", BrowserManager.loadImage("cues/cueWorldBossTierDropDown.png"), Bounds.fromWidthHeight(304, 199, 194, 42));
        BrowserManager.addCue("WorldBossDifficultyNormal", BrowserManager.loadImage("cues/cueWorldBossDifficultyNormal.png"), new Bounds(300, 275, 500, 325));
        BrowserManager.addCue("WorldBossDifficultyHard", BrowserManager.loadImage("cues/cueWorldBossDifficultyHard.png"), new Bounds(300, 275, 500, 325));
        BrowserManager.addCue("WorldBossDifficultyHeroic", BrowserManager.loadImage("cues/cueWorldBossDifficultyHeroic.png"), new Bounds(300, 275, 500, 325));

        BrowserManager.addCue("cueWBSelectNormal", BrowserManager.loadImage("cues/worldboss/cueWBSelectNormal.png"), new Bounds(260, 140, 510, 320));
        BrowserManager.addCue("cueWBSelectHard", BrowserManager.loadImage("cues/worldboss/cueWBSelectHard.png"), new Bounds(260, 140, 510, 320));
        BrowserManager.addCue("cueWBSelectHeroic", BrowserManager.loadImage("cues/worldboss/cueWBSelectHeroic.png"), new Bounds(260, 140, 510, 320));

        BrowserManager.addCue("OrlagWB", BrowserManager.loadImage("cues/worldboss/orlagclan.png"), new Bounds(190, 355, 400, 390));
        BrowserManager.addCue("NetherWB", BrowserManager.loadImage("cues/worldboss/netherworld.png"), new Bounds(190, 355, 400, 390));
        BrowserManager.addCue("MelvinWB", BrowserManager.loadImage("cues/worldboss/melvinfactory.png"), new Bounds(190, 355, 400, 390));
        BrowserManager.addCue("3xt3rWB", BrowserManager.loadImage("cues/worldboss/3xt3rmin4tion.png"), new Bounds(190, 355, 400, 390));
        BrowserManager.addCue("BrimstoneWB", BrowserManager.loadImage("cues/worldboss/brimstone.png"), new Bounds(190, 355, 400, 390));
        BrowserManager.addCue("WorldBossTitle", BrowserManager.loadImage("cues/worldboss/cueWorldBossTitle.png"), new Bounds(280, 90, 515, 140));
        BrowserManager.addCue("WorldBossSummonTitle", BrowserManager.loadImage("cues/worldboss/cueWorldBossSummonTitle.png"), new Bounds(325, 100, 480, 150));


        //fishing related
        BrowserManager.addCue("FishingButton", BrowserManager.loadImage("cues/cueFishingButton.png"), null);
        BrowserManager.addCue("Exit", BrowserManager.loadImage("cues/cueExit.png"), null);
        BrowserManager.addCue("Fishing", BrowserManager.loadImage("cues/cueFishing.png"), new Bounds(720, 200, 799, 519));
        BrowserManager.addCue("FishingClose", BrowserManager.loadImage("cues/fishingClose.png"), null);
        BrowserManager.addCue("Trade", BrowserManager.loadImage("cues/cueTrade.png"), new Bounds(360, 443, 441, 468));
        BrowserManager.addCue("Hall", BrowserManager.loadImage("cues/cueHall.png"), new Bounds(575, 455, 645, 480));
        BrowserManager.addCue("GuildHallC", BrowserManager.loadImage("cues/cueGuildHallC.png"), new Bounds(750, 55, 792, 13));

        //Familiar bribing cues
        BrowserManager.addCue("NotEnoughGems", BrowserManager.loadImage("cues/cueNotEnoughGems.png"), null); // used when not enough gems are available
        BrowserManager.addCue("CommonFamiliar", BrowserManager.loadImage("cues/familiars/type/cue01CommonFamiliar.png"), new Bounds(525, 240, 674, 365)); // Common Bribe cue
        BrowserManager.addCue("RareFamiliar", BrowserManager.loadImage("cues/familiars/type/cue02RareFamiliar.png"), new Bounds(525, 240, 674, 365)); // Rare Bribe cue
        BrowserManager.addCue("EpicFamiliar", BrowserManager.loadImage("cues/familiars/type/cue03EpicFamiliar.png"), new Bounds(525, 240, 674, 365)); // Epic Bribe cue
        BrowserManager.addCue("LegendaryFamiliar", BrowserManager.loadImage("cues/familiars/type/cue04LegendaryFamiliar.png"), new Bounds(525, 240, 674, 365)); // Epic Bribe cue

        //AutoRevive cues
        BrowserManager.addCue("Potions", BrowserManager.loadImage("cues/autorevive/cuePotions.png"), new Bounds(0, 370, 90, 460)); //Potions button
        BrowserManager.addCue("NoPotions", BrowserManager.loadImage("cues/autorevive/cueNoPotions.png"), new Bounds(210, 190, 590, 350)); // The team does not need revive
        BrowserManager.addCue("Restores", BrowserManager.loadImage("cues/autorevive/cueRestores.png"), new Bounds(145, 320, 655, 395)); // To identify revive and healing potions
        BrowserManager.addCue("Revives", BrowserManager.loadImage("cues/autorevive/cueRevives.png"), new Bounds(145, 320, 655, 395)); // To identify revive potions
        BrowserManager.addCue("MinorAvailable", BrowserManager.loadImage("cues/autorevive/cueMinorAvailable.png"), new Bounds(170, 205, 270, 300));
        BrowserManager.addCue("AverageAvailable", BrowserManager.loadImage("cues/autorevive/cueAverageAvailable.png"), new Bounds(350, 205, 450, 300));
        BrowserManager.addCue("MajorAvailable", BrowserManager.loadImage("cues/autorevive/cueMajorAvailable.png"), new Bounds(535, 205, 635, 300));
        BrowserManager.addCue("SuperAvailable", BrowserManager.loadImage("cues/autorevive/cueSuperAvailable.png"), new Bounds(140, 150, 300, 200));
        BrowserManager.addCue("UnitSelect", BrowserManager.loadImage("cues/autorevive/cueUnitSelect.png"), new Bounds(130, 20, 680, 95));
        BrowserManager.addCue("ScrollerRightDisabled", BrowserManager.loadImage("cues/autorevive/cueScrollerRightDisabled.png"), Bounds.fromWidthHeight(646, 425, 18, 18));
        BrowserManager.addCue("GravestoneHighlighted", BrowserManager.loadImage("cues/autorevive/highlighted_gravestone.png"), new Bounds(50, 230, 340, 400));

        //Items related cues
        BrowserManager.addCue("ItemHer", BrowserManager.loadImage("cues/items/cueItemHer.png"), null); // Heroic Item border
        BrowserManager.addCue("ItemLeg", BrowserManager.loadImage("cues/items/cueItemLeg.png"), null); // Legendary Item border
        BrowserManager.addCue("ItemSet", BrowserManager.loadImage("cues/items/cueItemSet.png"), null); // Set Item border
        BrowserManager.addCue("ItemMyt", BrowserManager.loadImage("cues/items/cueItemMyt.png"), null); // Mythical Item border
        //legendary
        BrowserManager.addCue("Material_R8", BrowserManager.loadImage("cues/items/material_r8.png"), null);
        BrowserManager.addCue("Material_R7", BrowserManager.loadImage("cues/items/material_r7.png"), null);
        BrowserManager.addCue("Material_R6", BrowserManager.loadImage("cues/items/material_r6.png"), null);
        BrowserManager.addCue("Material_R5", BrowserManager.loadImage("cues/items/material_r5.png"), null);
        BrowserManager.addCue("Material_R4", BrowserManager.loadImage("cues/items/material_r4.png"), null);
        BrowserManager.addCue("Material_R3", BrowserManager.loadImage("cues/items/material_r3.png"), null);
        BrowserManager.addCue("Material_R2", BrowserManager.loadImage("cues/items/material_r2.png"), null);
        //heroic
        BrowserManager.addCue("HeroicSchematic", BrowserManager.loadImage("cues/items/heroic_schematic.png"), null);
        BrowserManager.addCue("MicroChip", BrowserManager.loadImage("cues/items/microchip.png"), null);
        BrowserManager.addCue("GoldCoin", BrowserManager.loadImage("cues/items/goldcoin.png"), null);
        BrowserManager.addCue("DemonBlood", BrowserManager.loadImage("cues/items/demon_blood.png"), null);
        BrowserManager.addCue("HobbitsFoot", BrowserManager.loadImage("cues/items/hobbits_foot.png"), null);
        BrowserManager.addCue("MelvinChest", BrowserManager.loadImage("cues/items/melvin_chest.png"), null);
        BrowserManager.addCue("NeuralNetRom", BrowserManager.loadImage("cues/items/neural_net_rom.png"), null);
        BrowserManager.addCue("ScarlargSkin", BrowserManager.loadImage("cues/items/scarlarg_skin.png"), null);

        //weekly reward cues
        //these include the top of the loot window so they aren't triggered by the text in the activity panel
        BrowserManager.addCue("PVP_Rewards", BrowserManager.loadImage("cues/weeklyrewards/pvp.png"), new Bounds(290, 130, 510, 160));
        BrowserManager.addCue("Trials_Rewards", BrowserManager.loadImage("cues/weeklyrewards/trials.png"), new Bounds(290, 130, 510, 160));
        BrowserManager.addCue("Trials_Rewards_Large", BrowserManager.loadImage("cues/weeklyrewards/trials_large.png"), new Bounds(290, 50, 510, 130));
        BrowserManager.addCue("Gauntlet_Rewards", BrowserManager.loadImage("cues/weeklyrewards/gauntlet.png"), new Bounds(290, 130, 510, 160));
        BrowserManager.addCue("Gauntlet_Rewards_Large", BrowserManager.loadImage("cues/weeklyrewards/gauntlet_large.png"), new Bounds(290, 50, 510, 130));
        BrowserManager.addCue("GVG_Rewards", BrowserManager.loadImage("cues/weeklyrewards/gvg.png"), new Bounds(290, 130, 510, 160));
        BrowserManager.addCue("Invasion_Rewards", BrowserManager.loadImage("cues/weeklyrewards/invasion.png"), new Bounds(290, 130, 510, 160));
        BrowserManager.addCue("Expedition_Rewards", BrowserManager.loadImage("cues/weeklyrewards/expedition.png"), new Bounds(290, 130, 510, 160));
        BrowserManager.addCue("Fishing_Rewards", BrowserManager.loadImage("cues/weeklyrewards/fishing.png"), new Bounds(290, 130, 510, 160));


        int newFamCnt = BrowserManager.loadCueFolder("cues/familiars/new_format", null, false, new Bounds(145, 50, 575, 125));
        BHBot.logger.debug("Found " + newFamCnt + " familiar cues.");
    }

    boolean isDoNotShareUrl() {
        return !"".equals(doNotShareUrl);
    }

    private synchronized void connect() throws MalformedURLException {
        ChromeOptions options = new ChromeOptions();

        options.addArguments("user-data-dir=./chrome_profile"); // will create this profile folder where chromedriver.exe is located!
        options.setBinary(bot.chromiumExePath); //set Chromium v69 binary location

        if (bot.settings.autoStartChromeDriver) {
            System.setProperty("webdriver.chrome.driver", bot.chromeDriverExePath);
        } else {
            BHBot.logger.info("chromedriver auto start is off, make sure it is started before running BHBot");
            if (System.getProperty("webdriver.chrome.driver", null) != null) {
                System.clearProperty("webdriver.chrome.driver");
            }
        }

        // disable ephemeral flash permissions flag
        options.addArguments("--disable-features=EnableEphemeralFlashPermission");
        options.addArguments("disable-infobars");

        Map<String, Object> prefs = new HashMap<>();
        // Enable flash for all sites for Chrome 69
        prefs.put("profile.content_settings.exceptions.plugins.*,*.setting", 1);
        options.setExperimentalOption("prefs", prefs);

        DesiredCapabilities capabilities = DesiredCapabilities.chrome();

        /* When we connect the driver, if we don't know the do_not_share_url and if the configs require it,
         * the bot will enable the logging of network events so that when it is fully loaded, it will be possible
         * to analyze them searching for the magic URL
         */
        if (!isDoNotShareUrl() && bot.settings.useDoNotShareURL) {
            LoggingPreferences logPrefs = new LoggingPreferences();
            logPrefs.enable(LogType.PERFORMANCE, Level.ALL);
            options.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);
        }

        capabilities.setCapability("chrome.verbose", false);
        capabilities.setCapability(ChromeOptions.CAPABILITY, options);
        if (bot.settings.autoStartChromeDriver) {
            driver = new ChromeDriver(options);
        } else {
            driver = new RemoteWebDriver(new URL("http://" + bot.chromeDriverAddress), capabilities);
        }
        jsExecutor = (JavascriptExecutor) driver;
    }

    synchronized void restart(boolean useDoNotShareUrl) throws MalformedURLException {
        if (useDoNotShareUrl) {
            Pattern regex = Pattern.compile("\"(https://.+?\\?DO_NOT_SHARE_THIS_LINK[^\"]+?)\"");
            for (LogEntry le : driver.manage().logs().get(LogType.PERFORMANCE)) {
                Matcher regexMatcher = regex.matcher(le.getMessage());
                if (regexMatcher.find()) {
                    BHBot.logger.debug("DO NOT SHARE URL found!");
                    doNotShareUrl = regexMatcher.group(1);
                    break;
                }
            }
        }

        try {
            if (driver != null) {
                driver.close();
                driver.quit();
            }
        } catch (Exception e) {
            BHBot.logger.error("Error while quitting from Chromium", e);
        }

        // disable some annoying INFO messages:
        Logger.getLogger("").setLevel(Level.WARNING);

        connect();
        if (bot.settings.hideWindowOnRestart)
            hideBrowser();
        if ("".equals(doNotShareUrl)) {
            driver.navigate().to("http://www.kongregate.com/games/Juppiomenz/bit-heroes");
            byElement = By.id("game");
        } else {
            driver.navigate().to(doNotShareUrl);
            byElement = By.xpath("//div[1]");
        }

        game = driver.findElement(byElement);

        int vw = Math.toIntExact((Long) jsExecutor.executeScript("return window.outerWidth - window.innerWidth + arguments[0];", game.getSize().width));
        int vh = Math.toIntExact((Long) jsExecutor.executeScript("return window.outerHeight - window.innerHeight + arguments[0];", game.getSize().height));
        if ("".equals(doNotShareUrl)) {
            driver.manage().window().setSize(new Dimension(vw + 50, vh + 30));
        } else {
            driver.manage().window().setSize(new Dimension(vw, vh));
        }
    }

    synchronized void close() {
        driver.close();
        driver.quit();
    }

    synchronized void hideBrowser() {
        driver.manage().window().setPosition(new Point(-10000, 0)); // just to make sure
        BHBot.logger.info("Chrome window has been hidden.");
    }

    synchronized void showBrowser() {
        driver.manage().window().setPosition(new Point(0, 0));
        BHBot.logger.info("Chrome window has been restored.");
    }

    synchronized void scrollGameIntoView() {
        WebElement element = driver.findElement(byElement);

        String scrollElementIntoMiddle = "var viewPortHeight = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);"
                + "var elementTop = arguments[0].getBoundingClientRect().top;"
                + "window.scrollBy(0, elementTop-(viewPortHeight/2));";

        jsExecutor.executeScript(scrollElementIntoMiddle, element);
        Misc.sleep(1000);
    }

    /**
     * This form opens only seldom (haven't figured out what triggers it exactly - perhaps some cookie expired?). We need to handle it!
     */
    synchronized void detectSignInFormAndHandleIt() {
        // close the popup "create new account" form (that hides background):
        WebElement btnClose;
        try {
            btnClose = driver.findElement(By.cssSelector("#kongregate_lightbox_wrapper > div.header_bar > a"));
        } catch (NoSuchElementException e) {
            return;
        }
        btnClose.click();

        // fill in username and password:
        WebElement weUsername;
        try {
            weUsername = driver.findElement(By.xpath("//*[@id='username']"));
        } catch (NoSuchElementException e) {
            return;
        }
        weUsername.clear();
        weUsername.sendKeys(bot.settings.username);

        WebElement wePassword;
        try {
            wePassword = driver.findElement(By.xpath("//*[@id='password']"));
        } catch (NoSuchElementException e) {
            return;
        }
        wePassword.clear();
        wePassword.sendKeys(bot.settings.password);

        // press the "sign-in" button:
        WebElement btnSignIn;
        try {
            btnSignIn = driver.findElement(By.id("sessions_new_form_spinner"));
        } catch (NoSuchElementException e) {
            return;
        }
        btnSignIn.click();

        BHBot.logger.info("Signed-in manually (sign-in prompt was open).");
    }

    /**
     * Handles login screen (it shows seldom though. Perhaps because some cookie expired or something... anyway, we must handle it or else bot can't play the game anymore).
     */
    synchronized void detectLoginFormAndHandleIt(MarvinSegment seg) {
        if (seg == null)
            return;

        // open login popup window:
        jsExecutor.executeScript("active_user.activateInlineLogin(); return false;"); // I found this code within page source itself (it gets triggered upon clicking on some button)

        Misc.sleep(5000); // if we don't sleep enough, login form may still be loading and code bellow will not get executed!

        // fill in username:
        WebElement weUsername;
        try {
            weUsername = driver.findElement(By.cssSelector("body#play > div#lightbox > div#lbContent > div#kongregate_lightbox_wrapper > div#lightbox_form > div#lightboxlogin > div#new_session_shared_form > form > dl > dd > input#username"));
        } catch (NoSuchElementException e) {
            BHBot.logger.warn("Problem: username field not found in the login form (perhaps it was not loaded yet?)!");
            return;
        }
        weUsername.clear();
        weUsername.sendKeys(bot.settings.username);
        BHBot.logger.info("Username entered into the login form.");

        WebElement wePassword;
        try {
            wePassword = driver.findElement(By.cssSelector("body#play > div#lightbox > div#lbContent > div#kongregate_lightbox_wrapper > div#lightbox_form > div#lightboxlogin > div#new_session_shared_form > form > dl > dd > input#password"));
        } catch (NoSuchElementException e) {
            BHBot.logger.warn("Problem: password field not found in the login form (perhaps it was not loaded yet?)!");
            return;
        }
        wePassword.clear();
        wePassword.sendKeys(bot.settings.password);
        BHBot.logger.info("Password entered into the login form.");

        // press the "sign-in" button:
        WebElement btnSignIn;
        try {
            btnSignIn = driver.findElement(By.cssSelector("body#play > div#lightbox > div#lbContent > div#kongregate_lightbox_wrapper > div#lightbox_form > div#lightboxlogin > div#new_session_shared_form > form > dl > dt#signin > input"));
        } catch (NoSuchElementException e) {
            return;
        }
        btnSignIn.click();

        BHBot.logger.info("Signed-in manually (we were signed-out).");

        scrollGameIntoView();
    }

    synchronized BufferedImage takeScreenshot(boolean ofGame) {
        try {
            if (ofGame)
                return Shutterbug.shootElement(driver, game).getImage();
            else
                return Shutterbug.shootPage(driver).getImage();
        } catch (StaleElementReferenceException e) {
            // sometimes the game element is not available, if this happen we just return an empty image
            BHBot.logger.debug("Stale image detected while taking a screenshott", e);

            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        } catch (TimeoutException e) {
            // sometimes Chrome/Chromium crashes and it is impossible to take screenshots from it
            BHBot.logger.warn("Selenium timeout detected while taking a screenshot. A monitor screenshot will be taken", e);

            if (bot.settings.hideWindowOnRestart) showBrowser();

            java.awt.Rectangle screenRect = new java.awt.Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screen;
            try {
                screen = new Robot().createScreenCapture(screenRect);
            } catch (AWTException ex) {
                BHBot.logger.error("Impossible to perform a monitor screenshot", ex);
                screen = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            }

            if (bot.settings.hideWindowOnRestart) showBrowser();
            return screen;
        }
    }

    /**
     * Moves mouse to position (0,0) in the 'game' element (so that it doesn't trigger any highlight popups or similar
     */
    synchronized void moveMouseAway() {
        try {
            Actions act = new Actions(driver);
            act.moveToElement(game, 0, 0);
            act.perform();
        } catch (Exception e) {
            // do nothing
        }
    }

    //moves mouse to XY location (for triggering hover text)

    synchronized void moveMouseToPos(int x, int y) {
        try {
            Actions act = new Actions(driver);
            act.moveToElement(game, x, y);
            act.perform();
        } catch (Exception e) {
            // do nothing
        }
    }

    /**
     * Performs a mouse click on the center of the given segment
     */
    synchronized void clickOnSeg(MarvinSegment seg) {
        Actions act = new Actions(driver);
        act.moveToElement(game, seg.getCenterX(), seg.getCenterY());
        act.click();
        act.moveToElement(game, 0, 0); // so that the mouse doesn't stay on the button, for example. Or else button will be highlighted and cue won't get detected!
        act.perform();
    }

    synchronized void clickInGame(int x, int y) {
        Actions act = new Actions(driver);
        act.moveToElement(game, x, y);
        act.click();
        act.moveToElement(game, 0, 0); // so that the mouse doesn't stay on the button, for example. Or else button will be highlighted and cue won't get detected!
        act.perform();
    }

    synchronized void readScreen() {
        readScreen(true);
    }

    /**
     * @param game if true, then screenshot of a WebElement will be taken that contains the flash game. If false, then simply a screenshot of a browser will be taken.
     */
    @SuppressWarnings("SameParameterValue")
    synchronized void readScreen(boolean game) {
        readScreen(0, game);
    }

    /**
     * First sleeps 'wait' milliseconds and then reads the screen. It's a handy utility method that does two things in one command.
     */
    synchronized void readScreen(int wait) {
        readScreen(wait, true);
    }

    /**
     * @param wait first sleeps 'wait' milliseconds and then reads the screen. It's a handy utility method that does two things in one command.
     * @param game if true, then screenshot of a WebElement will be taken that contains the flash game. If false, then simply a screenshot of a browser will be taken.
     */
    synchronized void readScreen(int wait, boolean game) {
        if (wait != 0)
            Misc.sleep(wait);
        img = takeScreenshot(game);
    }

    /**
     * This method is meant to be used for development purpose. In some situations you want to "fake" the readScreen result
     * with an hand-crafted image. If this is the case, this method is here to help with it.
     *
     * @param screenFilePath the path to the image to be used to load the screen
     */
    @SuppressWarnings("unused")
    void loadScreen(String screenFilePath) {
        File screenImgFile = new File(screenFilePath);

        if (screenImgFile.exists()) {
            BufferedImage screenImg = null;
            try {
                screenImg = ImageIO.read(screenImgFile);
            } catch (IOException e) {
                BHBot.logger.error("Error when loading game screen ", e);
            }

            img = screenImg;
        } else {
            BHBot.logger.error("Impossible to load screen file: " + screenImgFile.getAbsolutePath());
        }
    }

    synchronized public BufferedImage getImg() {
        return img;
    }
}
