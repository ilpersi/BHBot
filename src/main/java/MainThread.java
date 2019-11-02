import com.assertthat.selenium_shutterbug.core.Shutterbug;
import com.google.common.collect.Maps;
import net.pushover.client.MessagePriority;
import net.pushover.client.PushoverException;
import net.pushover.client.PushoverMessage;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Comparator.comparing;

public class MainThread implements Runnable {
    static final int SECOND = 1000;
    static final int MINUTE = 60 * SECOND;
    private static final int HOUR = 60 * MINUTE;
    private static final int DAY = 24 * HOUR;
    private static int globalShards;
    private static int globalBadges;
    private static int globalEnergy;
    private static int globalTickets;
    private static int globalTokens;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    private static Map<String, Cue> cues = new HashMap<>();
    private static WebDriver driver;
    private static JavascriptExecutor jsExecutor;
    private static WebElement game;
    @SuppressWarnings("FieldCanBeLocal")
    private final int MAX_NUM_FAILED_RESTARTS = 5;
    @SuppressWarnings("FieldCanBeLocal")
    private final boolean QUIT_AFTER_MAX_FAILED_RESTARTS = false;
    @SuppressWarnings("FieldCanBeLocal")
    private final long MAX_IDLE_TIME = 15 * MINUTE;
    @SuppressWarnings("FieldCanBeLocal")
    private final int MAX_CONSECUTIVE_EXCEPTIONS = 10;

    boolean finished = false;
    private boolean[] revived = {false, false, false, false, false};
    private int potionsUsed = 0;
    private boolean startTimeCheck = false;
    private boolean oneTimeshrineCheck = false;
    private boolean autoShrined = false;
    private long activityStartTime;
    private long activityDuration;
    private boolean combatIdleChecker = true;
    private boolean motionChecker = false;
    private int stationaryCounter = 0;
    private boolean stationary = false;
    private BufferedImage movement1;
    private BufferedImage movement2;
    private long outOfEncounterTimestamp = 0;
    private long inEncounterTimestamp = 0;
    private long runMillisAvg = 0;
    private boolean specialDungeon; //d4 check for closing properly when no energy
    private String expeditionFailsafePortal = "";
    private int expeditionFailsafeDifficulty = 0;

    // Generic counters HashMap
    HashMap<State, DungeonCounter> counters = new HashMap<>();

    private int numFailedRestarts = 0; // in a row
    // When we do not have anymore gems to use this is true
    private boolean noGemsToBribe = false;
    private State state; // at which stage of the game/menu are we currently?
    private BufferedImage img; // latest screen capture

    private long ENERGY_CHECK_INTERVAL = 10 * MINUTE;
    private long TICKETS_CHECK_INTERVAL = 10 * MINUTE;
    private long TOKENS_CHECK_INTERVAL = 10 * MINUTE;
    private long BADGES_CHECK_INTERVAL = 10 * MINUTE;

    private long timeLastEnergyCheck = 0; // when did we check for Energy the last time?
    private long timeLastShardsCheck = 0; // when did we check for Shards the last time?
    private long timeLastTicketsCheck = 0; // when did we check for Tickets the last time?
    private long timeLastTrialsTokensCheck = 0; // when did we check for trials Tokens the last time?
    private long timeLastGauntletTokensCheck = 0; // when did we check for gauntlet Tokens the last time?
    private long timeLastExpBadgesCheck = 0; // when did we check for badges the last time?
    private long timeLastInvBadgesCheck = 0; // when did we check for badges the last time?
    private long timeLastGVGBadgesCheck = 0; // when did we check for badges the last time?
    private long timeLastBountyCheck = 0; // when did we check for bounties the last time?
    private long timeLastBonusCheck = 0; // when did we check for bonuses (active consumables) the last time?
    private long timeLastFishingBaitsCheck = 0; // when did we check for fishing baits the last time?
    private long timeLastFishingCheck = 0; // when did we check for fishing last time?
    private long timeLastPOAlive = 0; // when did we send the last PO Notification?
    /**
     * Number of consecutive exceptions. We need to track it in order to detect crash loops that we must break by restarting the Chrome driver. Or else it could get into loop and stale.
     */
    private int numConsecutiveException = 0;

    /**
     * autoshrine settings save
     */
    private boolean ignoreBossSetting = false;
    private boolean ignoreShrinesSetting = false;
    /**
     * global autorune vals
     */
    private boolean autoBossRuned = false;
    private boolean oneTimeRuneCheck = false;
    private MinorRune leftMinorRune;
    private MinorRune rightMinorRune;
    private Iterator<String> activitysIterator = BHBot.settings.activitiesEnabled.iterator();

    /*
          Match the character “z” literally (case sensitive) «z»
          Match the regex below and capture its match into a backreference named “zone” (also backreference number 1) «(?<zone>\d{1,2})»
             Match a single character that is a “digit” (ASCII 0–9 only) «\d{1,2}»
                Between one and 2 times, as many times as possible, giving back as needed (greedy) «{1,2}»
          Match the character “d” literally (case sensitive) «d»
          Match the regex below and capture its match into a backreference named “dungeon” (also backreference number 2) «(?<dungeon>[1234])»
             Match a single character from the list “1234” «[1234]»
          Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s+»
             Between one and unlimited times, as many times as possible, giving back as needed (greedy) «+»
          Match the regex below and capture its match into a backreference named “difficulty” (also backreference number 3) «(?<difficulty>[123])»
             Match a single character from the list “123” «[123]»
         */
    private Pattern dungeonRegex = Pattern.compile("z(?<zone>\\d{1,2})d(?<dungeon>[1234])\\s+(?<difficulty>[123])");

    private static BufferedImage loadImage(String f) {
        BufferedImage img = null;
        ClassLoader classLoader = MainThread.class.getClassLoader();
        InputStream resourceURL = classLoader.getResourceAsStream(f);

        if (resourceURL != null) {
            try {
                img = ImageIO.read(resourceURL);
            } catch (IOException e) {
                BHBot.logger.error("Error while loading Image", e);
            }
        } else {
            BHBot.logger.error("Error with resource: " + f);
//		try {
//			img = ImageIO.read(new File(f));
//		} catch (IOException e) {
//			BHBot.logger.error("Error while loading image");
//			BHBot.logger.error("Error while loading image", e);
        }

        return img;
    }
//	/** Amount of ads that were offered in main screen since last restart. We need it in order to do restart() after 2 ads, since sometimes ads won't get offered anymore after two restarts. */
//	private int numAdOffers = 0;

    private static void addCue(String name, BufferedImage im, Bounds bounds) {
        cues.put(name, new Cue(name, im, bounds));
    }

    @SuppressWarnings("SameParameterValue")
    private static int loadCueFolder(String cuesPath, String prefix, boolean stripCueStr, Bounds bounds) {
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

    static void loadCues() {
        addCue("Main", loadImage("cues/cueMainScreen.png"), new Bounds(90, 5, 100, 10));

        addCue("Login", loadImage("cues/cueLogin.png"), new Bounds(270, 260, 330, 300)); // login window (happens seldom)

        addCue("AreYouThere", loadImage("cues/cueAreYouThere.png"), new Bounds(240, 245, 265, 260));
        addCue("Yes", loadImage("cues/cueYes.png"), null);

        addCue("Disconnected", loadImage("cues/cueDisconnected.png"), Bounds.fromWidthHeight(299, 232, 202, 70)); // cue for "You have been disconnected" popup
        addCue("Reconnect", loadImage("cues/cueReconnectButton.png"), new Bounds(320, 330, 400, 360)); // used with "You have been disconnected" dialog and also with the "maintenance" dialog
        addCue("Reload", loadImage("cues/cueReload.png"), new Bounds(320, 330, 360, 360)); // used in "There is a new update required to play" dialog (happens on Friday night)
        addCue("Maintenance", loadImage("cues/cueMaintenance.png"), new Bounds(230, 200, 320, 250)); // cue for "Bit Heroes is currently down for maintenance. Please check back shortly!"
        addCue("Loading", loadImage("cues/cueLoading.png"), new Bounds(315, 210, 330, 225)); // cue for "Loading" superimposed screen
        addCue("RecentlyDisconnected", loadImage("cues/cueRecentlyDisconnected.png"), new Bounds(220, 195, 255, 230)); // cue for "You were recently disconnected from a dungeon. Do you want to continue the dungeon?" window
        addCue("UnableToConnect", loadImage("cues/cueUnableToConnect.png"), new Bounds(245, 235, 270, 250)); // happens when some error occurs for which the flash app is unable to connect to the server. We must simply click on the "Reconnect" button in this case!

        addCue("DailyRewards", loadImage("cues/cueDailyRewards.png"), new Bounds(260, 45, 285, 75));
        addCue("Claim", loadImage("cues/cueClaim.png"), null); // claim button, when daily rewards popup is open
        addCue("Items", loadImage("cues/cueItems.png"), null); // used when we clicked "claim" on daily rewards popup. Used also with main menu ads.
        addCue("X", loadImage("cues/cueX.png"), null); // "X" close button used with claimed daily rewards popup
        addCue("WeeklyRewards", loadImage("cues/cueWeeklyRewards.png"), new Bounds(185, 95, 250, 185)); // used with reward for GVG/PVP/Gauntlet/Trial on Friday night (when day changes into Saturday)
        addCue("Bounties", loadImage("cues/cueBounties.png"), new Bounds(320, 63, 480, 103));
        addCue("Loot", loadImage("cues/cueLoot.png"), null);

        addCue("News", loadImage("cues/cueNewsPopup.png"), new Bounds(345, 60, 365, 85)); // news popup
        addCue("Close", loadImage("cues/cueClose.png"), null); // close button used with "News" popup, also when defeated in dungeon, etc.

        addCue("Ad", loadImage("cues/cueAd.png"), new Bounds(720, 100, 750, 410)); // main screen ad button cue. Note that sometimes it is higher up on the screen than other times (during GvG week, it will be higher up, above GvG icon)
        addCue("AdPopup", loadImage("cues/cueAdPopup.png"), null);
        addCue("Watch", loadImage("cues/cueWatch.png"), null); // used with ad main screen watch button. Does not work in dungeons (button is a bit different there - used button there!)
        addCue("Watch2", loadImage("cues/cueWatch2.png"), null); // this is an alternative watch button. It is practically the same as the first one, but it has another shade of blue in the first row. Used with watching ads in dungeons (confirmed).
        addCue("AdInProgress", loadImage("cues/cueAdInProgress.png"), null); // we currently don't really need this cue
        addCue("AdFinished", loadImage("cues/cueAdFinished.png"), null); // we currently don't really need this cue
        addCue("Skip", loadImage("cues/cueSkip.png"), null);

        addCue("EnergyBar", loadImage("cues/cueEnergyBar.png"), new Bounds(390, 0, 420, 20));
        addCue("TicketBar", loadImage("cues/cueTicketBar.png"), new Bounds(540, 0, 770, 20));

        addCue("RaidButton", loadImage("cues/cueRaidButton.png"), new Bounds(0, 200, 40, 400));
        addCue("RaidPopup", loadImage("cues/cueRaidPopup.png"), Bounds.fromWidthHeight(300, 35, 70, 60));
        addCue("RaidSummon", loadImage("cues/cueRaidSummon.png"), new Bounds(480, 360, 540, 380));
        addCue("RaidLevel", loadImage("cues/cueRaidLevel.png"), new Bounds(300, 435, 510, 455)); // selected raid type button cue
        addCue("cueRaidLevelEmpty", loadImage("cues/cueRaidLevelEmpty.png"), new Bounds(300, 435, 510, 455)); // selected raid type button cue

        // New Raid level detection logic
        addCue("Raid1Name", loadImage("cues/raid/r1Name.png"), new Bounds(185, 340, 485, 395));// Raid 1 Name

        addCue("R1Only", loadImage("cues/cueR1Only.png"), null); // cue for R1 type selected when R2 (and R3) is not open yet (in that case it won't show raid type selection buttons)

        addCue("Normal", loadImage("cues/cueNormal.png"), null);
        addCue("Hard", loadImage("cues/cueHard.png"), null);
        addCue("Heroic", loadImage("cues/cueHeroic.png"), null);
        addCue("Accept", loadImage("cues/cueAccept.png"), null);
        addCue("D4Accept", loadImage("cues/cueD4Accept.png"), null);
        addCue("Cleared", loadImage("cues/cueCleared.png"), null); // used for example when raid has been finished
        addCue("Defeat", loadImage("cues/cueDefeat.png"), null); // used for example when you have been defeated in a dungeon. Also used when you have been defeated in a gauntlet.
        addCue("YesGreen", loadImage("cues/cueYesGreen.png"), null); // used for example when raid has been finished ("Cleared" popup)
        addCue("Persuade", loadImage("cues/cuePersuade.png"), new Bounds(116, 311, 286, 380));
        addCue("View", loadImage("cues/cueView.png"), new Bounds(390, 415, 600, 486));
        addCue("Bribe", loadImage("cues/cueBribe.png"), new Bounds(505, 305, 684, 375));
        addCue("SkeletonTreasure", loadImage("cues/cueSkeletonTreasure.png"), new Bounds(185, 165, 295, 280)); // skeleton treasure found in dungeons (it's a dialog/popup cue)
        addCue("SkeletonNoKeys", loadImage("cues/cueSkeletonNoKeys.png"), new Bounds(478, 318, 500, 348)); // red 0
        addCue("Open", loadImage("cues/cueOpen.png"), null); // skeleton treasure open button
        addCue("AdTreasure", loadImage("cues/cueAdTreasure.png"), null); // ad treasure found in dungeons (it's a dialog/popup cue)
        addCue("Decline", loadImage("cues/cueDecline.png"), null); // decline skeleton treasure button (found in dungeons), also with video ad treasures (found in dungeons)
        addCue("DeclineRed", loadImage("cues/cueDeclineRed.png"), null); // decline persuation attempts
        addCue("Merchant", loadImage("cues/cueMerchant.png"), null); // cue for merchant dialog/popup
        addCue("SettingsGear", loadImage("cues/cueSettingsGear.png"), new Bounds(655, 450, 730, 515)); // settings button
        addCue("Settings", loadImage("cues/cueSettings.png"), new Bounds(249, 61, 558, 102)); // settings menu

        addCue("Team", loadImage("cues/cueTeam.png"), null); // Team text part of pop-ups about teams
        addCue("TeamNotFull", loadImage("cues/cueTeamNotFull.png"), new Bounds(230, 200, 330, 250)); // warning popup when some friend left you and your team is not complete anymore
        addCue("TeamNotOrdered", loadImage("cues/cueTeamNotOrdered.png"), new Bounds(230, 190, 350, 250)); // warning popup when some guild member left and your GvG team is not complete anymore
        addCue("GuildLeaveConfirm", loadImage("cues/cueGuildLeaveConfirm.png"), new Bounds(195, 105, 605, 395)); // GVG confirm
        addCue("DisabledBattles", loadImage("cues/cueDisabledBattles.png"), new Bounds(240, 210, 560, 330)); // Disabled Battles Poppup

        addCue("No", loadImage("cues/cueNo.png"), null); // cue for a blue "No" button used for example with "Your team is not full" dialog, or for "Replace consumable" dialog, etc. This is why we can't put concrete borders as position varies a lot.
        addCue("AutoTeam", loadImage("cues/cueAutoTeam.png"), null); // "Auto" button that automatically assigns team (in raid, GvG, ...)
        addCue("Clear", loadImage("cues/cueClear.png"), null); //clear team button

        addCue("AutoOn", loadImage("cues/cueAutoOn.png"), new Bounds(740, 180, 785, 220)); // cue for auto pilot on
        addCue("AutoOff", loadImage("cues/cueAutoOff.png"), new Bounds(740, 180, 785, 220)); // cue for auto pilot off

        addCue("Trials", loadImage("cues/cueTrials.png"), new Bounds(0, 0, 40, 400)); // cue for trials button (note that as of 23.9.2017 they changed the button position to the right side of the screen and modified the glyph)
        addCue("Trials2", loadImage("cues/cueTrials2.png"), new Bounds(720, 0, 770, 400)); // an alternative cue for trials (flipped horizontally, located on the right side of the screen). Used since 23.9.2017.
        addCue("Gauntlet", loadImage("cues/cueGauntlet.png"), null); // cue for gauntlet button
        addCue("Gauntlet2", loadImage("cues/cueGauntlet2.png"), null); // alternative cue for gauntlet button
        addCue("Play", loadImage("cues/cuePlay.png"), null); // cue for play button in trials/gauntlet window
        addCue("TokenBar", loadImage("cues/cueTokenBar.png"), null);
        addCue("CloseGreen", loadImage("cues/cueCloseGreen.png"), null); // close button used with "You have been defeated" popup in gauntlet and also "Victory" window in gauntlet
        addCue("Victory", loadImage("cues/cueVictory.png"), null); // victory window cue found upon completing gauntlet / PvP

        addCue("UhOh", loadImage("cues/cueUhoh.png"), new Bounds(319, 122, 526, 184));
        addCue("ReviveAverage", loadImage("cues/cueReviveAverage.png"), null);
        addCue("Purchase", loadImage("cues/cuePurchase.png"), new Bounds(240, 240, 390, 280));

        addCue("GuildButton", loadImage("cues/cueGuildButton.png"), new Bounds(500, 420, 590, 520));
        addCue("IgnoreShrines", loadImage("cues/cueIgnoreShrines.png"), new Bounds(120, 250, 675, 475));
        addCue("IgnoreBoss", loadImage("cues/cueIgnoreBoss.png"), new Bounds(120, 250, 675, 475));


        addCue("Quest", loadImage("cues/cueQuest.png"), new Bounds(0, 0, 40, 40)); // cue for quest (dungeons) button
        addCue("ZonesButton", loadImage("cues/cueZonesButton.png"), new Bounds(105, 60, 125, 75));
        addCue("Zone1", loadImage("cues/cueZone1.png"), null);
        addCue("Zone2", loadImage("cues/cueZone2.png"), null);
        addCue("Zone3", loadImage("cues/cueZone3.png"), null);
        addCue("Zone4", loadImage("cues/cueZone4.png"), null);
        addCue("Zone5", loadImage("cues/cueZone5.png"), null);
        addCue("Zone6", loadImage("cues/cueZone6.png"), null);
        addCue("Zone7", loadImage("cues/cueZone7.png"), null);
        addCue("Zone8", loadImage("cues/cueZone8.png"), null);
        addCue("Zone9", loadImage("cues/cueZone9.png"), null);
        addCue("Zone10", loadImage("cues/cueZone10.png"), null);
        addCue("RightArrow", loadImage("cues/cueRightArrow.png"), null); // arrow used in quest screen to change zone
        addCue("LeftArrow", loadImage("cues/cueLeftArrow.png"), null); // arrow used in quest screen to change zone
        addCue("Enter", loadImage("cues/cueEnter.png"), null); // "Enter" button found on d4 window
//		addCue("EnterExpedition", loadImage("cues/cueEnter.png"),null ); // "Enter" for Expeditions
        addCue("NotEnoughEnergy", loadImage("cues/cueNotEnoughEnergy.png"), new Bounds(260, 210, 290, 235)); // "Not enough Energy" popup cue

        addCue("PVP", loadImage("cues/cuePVP.png"), new Bounds(0, 70, 40, 110)); // PVP icon in main screen
        addCue("Fight", loadImage("cues/cueFight.png"), null); // fight button in PVP window
        addCue("VictoryPopup", loadImage("cues/cueVictoryPopup.png"), null); // victory popup that appears in PVP after you have successfully completed it (needs to be closed). Also found in dungeons after you've completed an encounter (and hence probably also in trials, but not in gauntlet - that one has a different 'Victory' window!)
        addCue("PVPWindow", loadImage("cues/cuePVPWindow.png"), null); // PVP window cue

        addCue("DialogRight", loadImage("cues/cueDialogRight.png"), null); // cue for the dialog window (when arrow is at the right side of the window)
        addCue("DialogLeft", loadImage("cues/cueDialogLeft.png"), null); // cue for the dialog window (when arrow is at the left side of the window)

        addCue("SpeedCheck", loadImage("cues/cueSpeedCheck.png"), new Bounds(0, 450, 100, 520));
        addCue("Switch", loadImage("cues/cueSwitch.png"), new Bounds(0, 450, 100, 520)); //unused

        // GVG related:
        addCue("GVG", loadImage("cues/cueGVG.png"), null); // main GVG button cue
        addCue("BadgeBar", loadImage("cues/cueBadgeBar.png"), null);
        addCue("GVGWindow", loadImage("cues/cueGVGWindow.png"), new Bounds(260, 90, 280, 110)); // GVG window cue

        addCue("InGamePM", loadImage("cues/cueInGamePM.png"), new Bounds(450, 330, 530, 380)); // note that the guild window uses the same cue! That's why it's important that user doesn't open guild window while bot is working!

        addCue("TrialsOrGauntletWindow", loadImage("cues/cueTrialsOrGauntletWindow.png"), new Bounds(300, 30, 510, 105)); // cue for a trials/gauntlet window
        addCue("NotEnoughTokens", loadImage("cues/cueNotEnoughTokens.png"), Bounds.fromWidthHeight(274, 228, 253, 79)); // cue to check for the not enough tokens popup

        addCue("Difficulty", loadImage("cues/cueDifficulty.png"), new Bounds(450, 330, 640, 450)); // selected difficulty in trials/gauntlet window
        addCue("DifficultyDisabled", loadImage("cues/cueDifficultyDisabled.png"), new Bounds(450, 330, 640, 450)); // selected difficulty in trials/gauntlet window (disabled - because game is still fetching data from server)
        addCue("SelectDifficulty", loadImage("cues/cueSelectDifficulty.png"), new Bounds(400, 260, 0, 0)/*not exact bounds... the lower-right part of screen!*/); // select difficulty button in trials/gauntlet
        addCue("DifficultyDropDown", loadImage("cues/cueDifficultyDropDown.png"), new Bounds(260, 50, 550, 125)); // difficulty drop down menu cue
        addCue("DifficultyExpedition", loadImage("cues/cueDifficultyExpedition.png"), null); // selected difficulty in trials/gauntlet window
//		addCue("DifficultyDisabled", loadImage("cues/cueDifficultyDisabled.png"), new Bounds(450, 330, 640, 450)); // selected difficulty in trials/gauntlet window (disabled - because game is still fetching data from server)
        addCue("SelectDifficultyExpedition", loadImage("cues/cueSelectDifficultyExpedition.png"), null);
//		addCue("DifficultyDropDown", loadImage("cues/cueDifficultyDropDown.png"), new Bounds(260, 50, 550, 125)); // difficulty drop down menu cue
        addCue("DropDownUp", loadImage("cues/cueDropDownUp.png"), null); // up arrow in difficulty drop down menu (found in trials/gauntlet, for example)
        addCue("DropDownDown", loadImage("cues/cueDropDownDown.png"), null); // down arrow in difficulty drop down menu (found in trials/gauntlet, for example)
        addCue("Cost", loadImage("cues/cueCost.png"), new Bounds(400, 150, 580, 240)); // used both for PvP and Gauntlet/Trials costs. Note that bounds are very wide, because position of this cue in PvP is different from that in Gauntlet/Trials!
        addCue("SelectCost", loadImage("cues/cueSelectCost.png"), new Bounds(555, 170, 595, 205)); // cue for select cost found in both PvP and Gauntlet/Trials windows. Note that bounds are wide, because position of this cue in PvP is different from that in Gauntlet/Trials!
        addCue("CostDropDown", loadImage("cues/cueCostDropDown.png"), new Bounds(260, 45, 320, 70)); // cue for cost selection drop down window
        addCue("0", loadImage("cues/numbers/cue0.png"), null);
        addCue("1", loadImage("cues/numbers/cue1.png"), null);
        addCue("2", loadImage("cues/numbers/cue2.png"), null);
        addCue("3", loadImage("cues/numbers/cue3.png"), null);
        addCue("4", loadImage("cues/numbers/cue4.png"), null);
        addCue("5", loadImage("cues/numbers/cue5.png"), null);
        addCue("6", loadImage("cues/numbers/cue6.png"), null);
        addCue("7", loadImage("cues/numbers/cue7.png"), null);
        addCue("8", loadImage("cues/numbers/cue8.png"), null);
        addCue("9", loadImage("cues/numbers/cue9.png"), null);
        addCue("small0", loadImage("cues/numbers/small0.png"), null);
        addCue("small1", loadImage("cues/numbers/small1.png"), null);
        addCue("small2", loadImage("cues/numbers/small2.png"), null);
        addCue("small3", loadImage("cues/numbers/small3.png"), null);
        addCue("small4", loadImage("cues/numbers/small4.png"), null);
        addCue("small5", loadImage("cues/numbers/small5.png"), null);
        addCue("small6", loadImage("cues/numbers/small6.png"), null);
        addCue("small7", loadImage("cues/numbers/small7.png"), null);
        addCue("small8", loadImage("cues/numbers/small8.png"), null);
        addCue("small9", loadImage("cues/numbers/small9.png"), null);



        // PvP strip related:
        addCue("StripScrollerTopPos", loadImage("cues/strip/cueStripScrollerTopPos.png"), new Bounds(525, 140, 540, 370));
        addCue("StripEquipped", loadImage("cues/strip/cueStripEquipped.png"), new Bounds(465, 180, 485, 200)); // the little "E" icon upon an equipped item (the top-left item though, we want to detect just that one)
        addCue("StripItemsTitle", loadImage("cues/strip/cueStripItemsTitle.png"), new Bounds(335, 70, 360, 80));
        addCue("StripSelectorButton", loadImage("cues/strip/cueStripSelectorButton.png"), new Bounds(450, 115, 465, 130));
        // filter titles:
        addCue("StripTypeBody", loadImage("cues/strip/cueStripTypeBody.png"), new Bounds(460, 125, 550, 140));
        addCue("StripTypeHead", loadImage("cues/strip/cueStripTypeHead.png"), new Bounds(460, 125, 550, 140));
        addCue("StripTypeMainhand", loadImage("cues/strip/cueStripTypeMainhand.png"), new Bounds(460, 125, 550, 140));
        addCue("StripTypeOffhand", loadImage("cues/strip/cueStripTypeOffhand.png"), new Bounds(460, 125, 550, 140));
        addCue("StripTypeNeck", loadImage("cues/strip/cueStripTypeNeck.png"), new Bounds(460, 125, 550, 140));
        addCue("StripTypeRing", loadImage("cues/strip/cueStripTypeRing.png"), new Bounds(460, 125, 550, 140));

        // consumables management related:
        addCue("BonusExp", loadImage("cues/cueBonusExp.png"), new Bounds(100, 455, 370, 485)); // consumable icon in the main menu (when it's being used)
        addCue("BonusItem", loadImage("cues/cueBonusItem.png"), new Bounds(100, 455, 370, 485));
        addCue("BonusGold", loadImage("cues/cueBonusGold.png"), new Bounds(100, 455, 370, 485));
        addCue("BonusSpeed", loadImage("cues/cueBonusSpeed.png"), new Bounds(100, 455, 370, 485));
        addCue("ConsumableExpMinor", loadImage("cues/cueConsumableExpMinor.png"), null); // consumable icon in the inventory
        addCue("ConsumableExpAverage", loadImage("cues/cueConsumableExpAverage.png"), null);
        addCue("ConsumableExpMajor", loadImage("cues/cueConsumableExpMajor.png"), null);
        addCue("ConsumableItemMinor", loadImage("cues/cueConsumableItemMinor.png"), null);
        addCue("ConsumableItemAverage", loadImage("cues/cueConsumableItemAverage.png"), null);
        addCue("ConsumableItemMajor", loadImage("cues/cueConsumableItemMajor.png"), null);
        addCue("ConsumableSpeedMinor", loadImage("cues/cueConsumableSpeedMinor.png"), null);
        addCue("ConsumableSpeedAverage", loadImage("cues/cueConsumableSpeedAverage.png"), null);
        addCue("ConsumableSpeedMajor", loadImage("cues/cueConsumableSpeedMajor.png"), null);
        addCue("ConsumableGoldMinor", loadImage("cues/cueConsumableGoldMinor.png"), null);
        addCue("ConsumableGoldAverage", loadImage("cues/cueConsumableGoldAverage.png"), null);
        addCue("ConsumableGoldMajor", loadImage("cues/cueConsumableGoldMajor.png"), null);
        addCue("ConsumablePumkgor", loadImage("cues/cueConsumablePumkgor.png"), new Bounds(150, 460, 205, 519)); // Special Halloween consumable
        addCue("ConsumableGingernaut", loadImage("cues/cueConsumableGingernaut.png"), new Bounds(150, 460, 205, 519)); // Special Chrismast consumable
        addCue("ConsumableGreatFeast", loadImage("cues/cueConsumableGreatFeast.png"), new Bounds(150, 460, 205, 519)); // Thanksgiving consumable
        addCue("ConsumableBroccoli", loadImage("cues/cueConsumableBroccoli.png"), new Bounds(150, 460, 205, 519)); // Special Halloween consumable
        addCue("ConsumableCoco", loadImage("cues/cueConsumableCoco.png"), new Bounds(150, 460, 205, 519)); // Special ?? consumable
        addCue("ScrollerAtBottom", loadImage("cues/cueScrollerAtBottom.png"), null); // cue for scroller being at the bottom-most position (we can't scroll down more than this)
        addCue("ConsumableTitle", loadImage("cues/cueConsumableTitle.png"), new Bounds(280, 100, 310, 180)); // cue for title of the window that pops up when we want to consume a consumable. Note that vertical range is big here since sometimes is higher due to greater window size and sometimes is lower.
        addCue("FilterConsumables", loadImage("cues/cueFilterConsumables.png"), new Bounds(460, 125, 550, 140)); // cue for filter button name
        addCue("LoadingInventoryIcon", loadImage("cues/cueLoadingInventoryIcon.png"), null); // cue for loading animation for the icons inside inventory


        // rune management related:
        addCue("Runes", loadImage("cues/cueRunes.png"), new Bounds(120, 450, 245, 495)); // runes button in profile
        addCue("RunesLayout", loadImage("cues/cueRunesLayout.png"), new Bounds(340, 70, 460, 110)); // runes layout header
        addCue("RunesPicker", loadImage("cues/cueRunesPicker.png"), null); // rune picker
        addCue("RunesSwitch", loadImage("cues/cueRunesSwitch.png"), new Bounds(320, 260, 480, 295)); // rune picker

        // All minor rune cues
        for (MinorRune rune : MinorRune.values()) {
            addCue(rune.getRuneCueName(), loadImage(rune.getRuneCueFileName()), null);
            addCue(rune.getRuneSelectCueName(), loadImage(rune.getRuneSelectCueFileName()), new Bounds(235, 185, 540, 350));
        }

        // invasion related:
//		addCue("Invasion", loadImage("cues/cueInvasion.png"), new Bounds(720, 270, 770, 480)); // main Invasion button cue
        addCue("Invasion", loadImage("cues/cueInvasion.png"), null);
        addCue("InvasionWindow", loadImage("cues/cueInvasionWindow.png"), new Bounds(260, 90, 280, 110)); // GVG window cue

        // Expedition related:
        addCue("ExpeditionButton", loadImage("cues/cueExpeditionButton.png"), null);
        addCue("Expedition1", loadImage("cues/expedition/cueExpedition1Hallowed.png"), new Bounds(168, 34, 628, 108)); // Hallowed Expedtion Title
        addCue("Expedition2", loadImage("cues/expedition/cueExpedition2Inferno.png"), new Bounds(200, 40, 600, 100)); //Inferno Expedition
        addCue("Expedition3", loadImage("cues/expedition/cueExpedition3Jammie.png"), new Bounds(230, 40, 565, 100)); //Jammie Dimension
        addCue("Expedition4", loadImage("cues/expedition/cueExpedition4Idol.png"), new Bounds(230, 40, 565, 100)); //Idol Dimension
        addCue("Expedition5", loadImage("cues/expedition/cueExpedition5BattleBards.png"), new Bounds(230, 40, 565, 100)); //Battle Bards!

        //WorldBoss Related
        addCue("WorldBoss", loadImage("cues/cueWorldBoss.png"), null);
        addCue("WorldBossSelector", loadImage("cues/cueWorldBossSelector.png"), null);
        addCue("BlueSummon", loadImage("cues/cueBlueSummon.png"), null);
        addCue("LargeGreenSummon", loadImage("cues/cueLargeGreenSummon.png"), null);
        addCue("SmallGreenSummon", loadImage("cues/cueSmallGreenSummon.png"), null);
        addCue("Invite3rd", loadImage("cues/cueInviteAny.png"), new Bounds(334, 275, 456, 323)); //bounds defined 3rd invite button for Nether
        addCue("Invite4th", loadImage("cues/cueInviteAny.png"), new Bounds(330, 330, 460, 380)); //bounds defined 4th invite button for Melvin
        addCue("Invite", loadImage("cues/cueInvite.png"), new Bounds(336, 387, 452, 422)); //bounds defined 5th invite button for Orlag
        addCue("Start", loadImage("cues/cueStart.png"), null);
        addCue("WorldBossVictory", loadImage("cues/cueWorldBossVictory.png"), null);
        addCue("OrlagSelected", loadImage("cues/cueOrlagSelected.png"), new Bounds(360, 430, 440, 460));
        addCue("NetherSelected", loadImage("cues/cueNetherSelected.png"), null);
        addCue("Private", loadImage("cues/cuePrivate.png"), new Bounds(310, 320, 370, 380));
        addCue("Unready", loadImage("cues/cueWorldBossUnready.png"), new Bounds(170, 210, 215, 420));
        addCue("WorldBossTier", loadImage("cues/cueWorldBossTier.png"), Bounds.fromWidthHeight(314, 206, 88, 28));
        addCue("WorldBossTierDropDown", loadImage("cues/cueWorldBossTierDropDown.png"), Bounds.fromWidthHeight(304, 199, 194, 42));
        addCue("WorldBossDifficultyNormal", loadImage("cues/cueWorldBossDifficultyNormal.png"), new Bounds(300, 275, 500, 325));
        addCue("WorldBossDifficultyHard", loadImage("cues/cueWorldBossDifficultyHard.png"), new Bounds(300, 275, 500, 325));
        addCue("WorldBossDifficultyHeroic", loadImage("cues/cueWorldBossDifficultyHeroic.png"), new Bounds(300, 275, 500, 325));

        addCue("cueWBSelectNormal", loadImage("cues/worldboss/cueWBSelectNormal.png"), new Bounds(260, 140, 510, 320));
        addCue("cueWBSelectHard", loadImage("cues/worldboss/cueWBSelectHard.png"), new Bounds(260, 140, 510, 320));
        addCue("cueWBSelectHeroic", loadImage("cues/worldboss/cueWBSelectHeroic.png"), new Bounds(260, 140, 510, 320));

        addCue("OrlagWB", loadImage("cues/worldboss/orlagclan.png"), new Bounds(190, 355, 400, 390));
        addCue("NetherWB", loadImage("cues/worldboss/netherworld.png"), new Bounds(190, 355, 400, 390));
        addCue("MelvinWB", loadImage("cues/worldboss/melvinfactory.png"), new Bounds(190, 355, 400, 390));
        addCue("3xt3rWB", loadImage("cues/worldboss/3xt3rmin4tion.png"), new Bounds(190, 355, 400, 390));
        addCue("BrimstoneWB", loadImage("cues/worldboss/brimstone.png"), new Bounds(190, 355, 400, 390));
        addCue("WorldBossTitle", loadImage("cues/worldboss/cueWorldBossTitle.png"), new Bounds(280, 90, 515, 140));
        addCue("WorldBossSummonTitle", loadImage("cues/worldboss/cueWorldBossSummonTitle.png"), new Bounds(325, 100, 480, 150));


        //fishing related
        addCue("FishingButton", loadImage("cues/cueFishingButton.png"), null);
        addCue("Exit", loadImage("cues/cueExit.png"), null);
        addCue("Fishing", loadImage("cues/cueFishing.png"), new Bounds(720, 200, 799, 519));
        addCue("FishingClose", loadImage("cues/fishingClose.png"), null);
        addCue("Trade", loadImage("cues/cueTrade.png"), new Bounds(360, 443, 441, 468));
        addCue("Hall", loadImage("cues/cueHall.png"), new Bounds(575, 455, 645, 480));
        addCue("GuildHallC", loadImage("cues/cueGuildHallC.png"), new Bounds(750, 55, 792, 13));

        //Familiar bribing cues
        addCue("NotEnoughGems", loadImage("cues/cueNotEnoughGems.png"), null); // used when not enough gems are available
        addCue("CommonFamiliar", loadImage("cues/familiars/type/cue01CommonFamiliar.png"), new Bounds(525, 240, 674, 365)); // Common Bribe cue
        addCue("RareFamiliar", loadImage("cues/familiars/type/cue02RareFamiliar.png"), new Bounds(525, 240, 674, 365)); // Rare Bribe cue
        addCue("EpicFamiliar", loadImage("cues/familiars/type/cue03EpicFamiliar.png"), new Bounds(525, 240, 674, 365)); // Epic Bribe cue
        addCue("LegendaryFamiliar", loadImage("cues/familiars/type/cue04LegendaryFamiliar.png"), new Bounds(525, 240, 674, 365)); // Epic Bribe cue

        //AutoRevive cues
        addCue("Potions", loadImage("cues/autorevive/cuePotions.png"), new Bounds(0, 370, 90, 460)); //Potions button
        addCue("NoPotions", loadImage("cues/autorevive/cueNoPotions.png"), new Bounds(210, 190, 590, 350)); // The team does not need revive
        addCue("Restores", loadImage("cues/autorevive/cueRestores.png"), new Bounds(145, 320, 655, 395)); // To identify revive and healing potions
        addCue("Revives", loadImage("cues/autorevive/cueRevives.png"), new Bounds(145, 320, 655, 395)); // To identify revive potions
        addCue("MinorAvailable", loadImage("cues/autorevive/cueMinorAvailable.png"), new Bounds(170, 205, 270, 300));
        addCue("AverageAvailable", loadImage("cues/autorevive/cueAverageAvailable.png"), new Bounds(350, 205, 450, 300));
        addCue("MajorAvailable", loadImage("cues/autorevive/cueMajorAvailable.png"), new Bounds(535, 205, 635, 300));
        addCue("SuperAvailable", loadImage("cues/autorevive/cueSuperAvailable.png"), new Bounds(140, 150, 300, 200));
        addCue("UnitSelect", loadImage("cues/autorevive/cueUnitSelect.png"), new Bounds(130, 20, 680, 95));
        addCue("ScrollerRightDisabled", loadImage("cues/autorevive/cueScrollerRightDisabled.png"), Bounds.fromWidthHeight(646, 425, 18, 18));

        //Items related cues
        addCue("ItemLeg", loadImage("cues/items/cueItemLeg.png"), null); // Legendary Item border
        addCue("ItemSet", loadImage("cues/items/cueItemSet.png"), null); // Set Item border
        addCue("ItemMyt", loadImage("cues/items/cueItemMyt.png"), null); // Mythical Item border

        //weekly reward cues
        //these include the top of the loot window so they aren't triggered by the text in the activity panel
        addCue("PVP_Rewards", loadImage("cues/weeklyrewards/pvp.png"), new Bounds(290, 130, 510, 160));
        addCue("Trials_Rewards", loadImage("cues/weeklyrewards/trials.png"), new Bounds(290, 130, 510, 160));
        addCue("Gauntlet_Rewards", loadImage("cues/weeklyrewards/gauntlet.png"), new Bounds(290, 130, 510, 160));
        addCue("GVG_Rewards", loadImage("cues/weeklyrewards/gvg.png"), new Bounds(290, 130, 510, 160));
        addCue("Invasion_Rewards", loadImage("cues/weeklyrewards/invasion.png"), new Bounds(290, 130, 510, 160));
        addCue("Expedition_Rewards", loadImage("cues/weeklyrewards/expedition.png"), new Bounds(290, 130, 510, 160));
        addCue("Fishing_Rewards", loadImage("cues/weeklyrewards/fishing.png"), new Bounds(290, 130, 510, 160));


        int newFamCnt = loadCueFolder("cues/familiars/new_format", null, false, new Bounds(145, 50, 575, 125));

        BHBot.logger.debug("Found " + newFamCnt + " familiar cue(s) with new format.");

    }

    private static void connectDriver() throws MalformedURLException {
        ChromeOptions options = new ChromeOptions();

        options.addArguments("user-data-dir=./chrome_profile"); // will create this profile folder where chromedriver.exe is located!
        options.setBinary(BHBot.chromiumExePath); //set Chromium v69 binary location

        if (BHBot.settings.autoStartChromeDriver) {
            System.setProperty("webdriver.chrome.driver", BHBot.chromeDriverExePath);
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
        capabilities.setCapability("chrome.verbose", false);
        capabilities.setCapability(ChromeOptions.CAPABILITY, options);
//		driver = new RemoteWebDriver(new URL("http://" + BHBot.chromeDriverAddress), capabilities);
        if (BHBot.settings.autoStartChromeDriver) {
            driver = new ChromeDriver(options);
        } else {
            driver = new RemoteWebDriver(new URL("http://" + BHBot.chromeDriverAddress), capabilities);
        }
        jsExecutor = (JavascriptExecutor) driver;
    }

    // http://www.qaautomationsimplified.com/selenium/selenium-webdriver-get-cookies-from-an-existing-session-and-add-those-to-a-newly-instantiated-webdriver-browser-instance/
    // http://www.guru99.com/handling-cookies-selenium-webdriver.html
    static void saveCookies() {
        // create file named Cookies to store Login Information
        File file = new File("Cookies.data");
        try {
            // Delete old file if exists
            Files.deleteIfExists(file.toPath());
            boolean isCookieFile = file.createNewFile();
            if (!isCookieFile) throw new Exception("Impossible to create cookie file!");
            FileWriter fileWrite = new FileWriter(file);
            BufferedWriter Bwrite = new BufferedWriter(fileWrite);
            // loop for getting the cookie information
            for (Cookie ck : driver.manage().getCookies()) {
                Bwrite.write((ck.getName() + ";" + ck.getValue() + ";" + ck.getDomain() + ";" + ck.getPath() + ";" + (ck.getExpiry() == null ? 0 : ck.getExpiry().getTime()) + ";" + ck.isSecure()));
                Bwrite.newLine();
            }
            Bwrite.flush();
            Bwrite.close();
            fileWrite.close();
        } catch (Exception ex) {
            BHBot.logger.error("Impossible to save cookies in saveCookies.", ex);
        }

        BHBot.logger.info("Cookies saved to disk.");
    }

    static void loadCookies() {
        try {
            File file = new File("Cookies.data");
            FileReader fileReader = new FileReader(file);
            BufferedReader Buffreader = new BufferedReader(fileReader);
            String strline;
            while ((strline = Buffreader.readLine()) != null) {
                StringTokenizer token = new StringTokenizer(strline, ";");
                while (token.hasMoreTokens()) {
                    String name = token.nextToken();
                    String value = token.nextToken();
                    String domain = token.nextToken();
                    String path = token.nextToken();

                    String val;
                    if (!(val = token.nextToken()).equals("null")) {
                        new Date(Long.parseLong(val));
                    }
                    boolean isSecure = Boolean.parseBoolean(token.nextToken());
                    Cookie ck = new Cookie(name, value, domain, path, null, isSecure);
                    try {
                        driver.manage().addCookie(ck); // This will add the stored cookie to your current session
                    } catch (Exception e) {
                        BHBot.logger.error("Error while adding cookie", e);
                    }
                }
            }
            Buffreader.close();
            fileReader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        BHBot.logger.info("Cookies loaded.");
    }

    // https://stackoverflow.com/questions/297762/find-known-sub-image-in-larger-image
    private static MarvinSegment findSubimage(BufferedImage src, Cue cue) {
        long timer = Misc.getTime();

        MarvinSegment seg;

        seg = FindSubimage.findImage(
                src,
                cue.im,
                cue.bounds != null ? cue.bounds.x1 : 0,
                cue.bounds != null ? cue.bounds.y1 : 0,
                cue.bounds != null ? cue.bounds.x2 : 0,
                cue.bounds != null ? cue.bounds.y2 : 0
        );

        //source.drawRect(seg.x1, seg.y1, seg.x2-seg.x1, seg.y2-seg.y1, Color.blue);
        //MarvinImageIO.saveImage(source, "window_out.png");
        if (BHBot.settings.debugDetectionTimes)
            BHBot.logger.info("cue detection time: " + (Misc.getTime() - timer) + "ms (" + cue.name + ") [" + (seg != null ? "true" : "false") + "]");
//        if (seg == null) {
//        	return -1;
//        }
        return seg;
    }

    static void printFamiliars() {

        List<String> folders = new ArrayList<>();
        folders.add("cues/familiars/old_format");
        folders.add("cues/familiars/new_format");

        Set<String> uniqueFamiliars = new TreeSet<>();

        for (String cuesPath : folders) {
            // We make sure that the last char of the path is a folder separator
            if (!"/".equals(cuesPath.substring(cuesPath.length() - 1))) cuesPath += "/";

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            URL url = classLoader.getResource(cuesPath);
            if (url != null) { // Run from the IDE
                if ("file".equals(url.getProtocol())) {

                    InputStream in = classLoader.getResourceAsStream(cuesPath);
                    if (in == null) {
                        BHBot.logger.error("Impossible to create InputStream in printFamiliars");
                        return;
                    }

                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    String resource;

                    while (true) {
                        try {
                            resource = br.readLine();
                            if (resource == null) break;
                        } catch (IOException e) {
                            BHBot.logger.error("Error while reading resources in printFamiliars", e);
                            continue;
                        }
                        int dotPosition = resource.lastIndexOf('.');
                        String fileExtension = dotPosition > 0 ? resource.substring(dotPosition + 1) : "";
                        if ("png".equals(fileExtension.toLowerCase())) {
                            String cueName = resource.substring(0, dotPosition);

                            cueName = cueName.replace("cue", "");

                            uniqueFamiliars.add(cueName.toLowerCase());
                        }
                    }
                } else if ("jar".equals(url.getProtocol())) { // Run from JAR
                    String path = url.getPath();
                    String jarPath = path.substring(5, path.indexOf("!"));

                    String decodedURL;
                    try {
                        decodedURL = URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name());
                    } catch (UnsupportedEncodingException e) {
                        BHBot.logger.error("Impossible to decode path for jar in printFamiliars: " + jarPath, e);
                        return;
                    }

                    JarFile jar;
                    try {
                        jar = new JarFile(decodedURL);
                    } catch (IOException e) {
                        BHBot.logger.error("Impossible to open JAR file in printFamiliars: " + decodedURL, e);
                        return;
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
                                BHBot.logger.warn("Unexpected resource filename in load printFamiliars");
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

                                cueName = cueName.replace("cue", "");
                                BHBot.logger.trace("cueName: " + cueName);

                                // resourceRelativePath begins with a '/' char and we want to be sure to remove it
                                uniqueFamiliars.add(cueName.toLowerCase());
                            }
                        }
                    }

                }
            }
        }

        StringBuilder familiarString = new StringBuilder();
        int currentFamiliar = 1;

        for (String familiar : uniqueFamiliars) {
            if (familiarString.length() > 0) familiarString.append(", ");
            if (currentFamiliar % 5 == 0) familiarString.append("\n");
            familiarString.append(familiar);
            currentFamiliar++;
        }

        BHBot.logger.info(familiarString.toString());
    }

    void hideBrowser() {
        driver.manage().window().setPosition(new Point(-10000, 0)); // just to make sure
        BHBot.logger.info("Chrome window has been hidden.");
    }

    void showBrowser() {
        driver.manage().window().setPosition(new Point(0, 0));
        BHBot.logger.info("Chrome window has been restored.");
    }

    private void dumpCrashLog() {
        // save screen shot:
        String file = saveGameScreen("crash", "errors");

        if (file == null) {
            BHBot.logger.error("Impossible to create crash screenshot");
            return;
        }

        // save stack trace:
        boolean savedST = Misc.saveTextFile(file.substring(0, file.length() - 4) + ".txt", Misc.getStackTrace());
        if (!savedST) {
            BHBot.logger.info("Impossible to save the stack trace in dumpCrashLog!");
        }

        if (BHBot.settings.enablePushover && BHBot.settings.poNotifyCrash) {
            File poCrashScreen = new File(file);
            sendPushOverMessage("BHbot CRASH!",
                    "BHBot has crashed and a driver emergency restart has been performed!\n\n" + Misc.getStackTrace(), "falling",
                    MessagePriority.HIGH, poCrashScreen.exists() ? poCrashScreen : null);
        }
    }

    private void restart() {
        restart(true); // assume emergency restart
        oneTimeshrineCheck = false; //reset first run shrine check in case its enabled after restarting
    }

    /**
     * @param emergency true in case something bad happened (some kind of an error for which we had to do a restart)
     */
    void restart(boolean emergency) {
        // take emergency screenshot (which will have the developer to debug the problem):
        if (emergency) {
            BHBot.logger.warn("Doing driver emergency restart...");
            dumpCrashLog();
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

        try {
            connectDriver();
            if (BHBot.settings.hideWindowOnRestart)
                hideBrowser();
            driver.navigate().to("http://www.kongregate.com/games/Juppiomenz/bit-heroes");
            //sleep(5000);
            //driver.navigate().to("chrome://flags/#run-all-flash-in-allow-mode");
            //driver.navigate().to("chrome://settings/content");
            //BHBot.processCommand("shot");
            game = driver.findElement(By.id("game"));
        } catch (Exception e) {

            if (e instanceof org.openqa.selenium.NoSuchElementException)
                BHBot.logger.warn("Problem: web element with id 'game' not found!");
            if (e instanceof MalformedURLException)
                BHBot.logger.warn("Problem: malformed url detected!");
            if (e instanceof org.openqa.selenium.remote.UnreachableBrowserException) {
                BHBot.logger.error("Impossible to connect to the browser. Make sure chromedirver is started. Will retry in a few minutes... (sleeping)");
                sleep(5 * MINUTE);
                restart();
                return;
            }

            numFailedRestarts++;
            if (QUIT_AFTER_MAX_FAILED_RESTARTS && numFailedRestarts > MAX_NUM_FAILED_RESTARTS) {
                BHBot.logger.fatal("Something went wrong with driver restart. Number of restarts exceeded " + MAX_NUM_FAILED_RESTARTS + ", this is why I'm aborting...");
                finished = true;
                return;
            } else {
                BHBot.logger.error("Something went wrong with driver restart. Will retry in a few minutes... (sleeping)", e);
                sleep(5 * MINUTE);
                restart();
                return;
            }
        }

        detectSignInFormAndHandleIt(); // just in case (happens seldom though)

        int vw = Math.toIntExact((Long) jsExecutor.executeScript("return window.outerWidth - window.innerWidth + arguments[0];", game.getSize().width));
        int vh = Math.toIntExact((Long) jsExecutor.executeScript("return window.outerHeight - window.innerHeight + arguments[0];", game.getSize().height));
        vw += 50; // compensate for scrollbars 70
        vh += 30; // compensate for scrollbars 50
        driver.manage().window().setSize(new Dimension(vw, vh));
        scrollGameIntoView();

        int counter = 0;
        boolean restart = false;
        while (true) {
            try {
                detectLoginFormAndHandleIt();
            } catch (Exception e) {
                counter++;
                if (counter > 20) {
                    BHBot.logger.error("Error: <" + e.getMessage() + "> while trying to detect and handle login form. Restarting...", e);
                    restart = true;
                    break;
                }

                sleep(10 * SECOND);
                continue;
            }
            break;
        }
        if (restart) {
            restart();
            return;
        }

        BHBot.logger.info("Game element found. Starting to run bot..");

        if (BHBot.settings.idleMode) { //skip startup checks if we are in idle mode
            oneTimeshrineCheck = true;
            oneTimeRuneCheck = true;
        }

        if ((BHBot.settings.activitiesEnabled.contains("d")) && (BHBot.settings.activitiesEnabled.contains("w"))) {
            BHBot.logger.info("Both Dungeons and World Boss selected, disabling World Boss.");
            BHBot.logger.info("To run a mixture of both use a low lobby timer and enable dungeonOnTimeout");
            BHBot.settings.activitiesEnabled.remove("w");
        }
        state = State.Loading;
        BHBot.scheduler.resetIdleTime();
        BHBot.scheduler.resume(); // in case it was paused
        numFailedRestarts = 0; // must be last line in this method!
    }

    private void scrollGameIntoView() {
        WebElement element = driver.findElement(By.id("game"));

        String scrollElementIntoMiddle = "var viewPortHeight = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);"
                + "var elementTop = arguments[0].getBoundingClientRect().top;"
                + "window.scrollBy(0, elementTop-(viewPortHeight/2));";

        ((JavascriptExecutor) driver).executeScript(scrollElementIntoMiddle, element);
        sleep(3000);
        /*
         * Bellow code doesn't work well (it does not scroll horizontally):
         *
         * ((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView(true);", game);
         */
//		Actions actions = new Actions(driver);
//		actions.moveToElement(game);
//		sleep(5000);
//		actions.perform();
    }

    public void run() {

        BHBot.logger.info("Driver started succesfully");

        restart(false);

        // We initialize the counter HasMap using the state as key
        for (State state : State.values()) {
            counters.put(state, new DungeonCounter(0, 0));
        }

        while (!finished) {
            BHBot.scheduler.backupIdleTime();
            try {
                BHBot.scheduler.process();
                if (BHBot.scheduler.isPaused()) continue;

                if (Misc.getTime() - BHBot.scheduler.getIdleTime() > MAX_IDLE_TIME) {
                    BHBot.logger.warn("Idle time exceeded... perhaps caught in a loop? Restarting... (state=" + state + ")");
                    saveGameScreen("idle-timeout-error", "errors");

                    // Safety measure to avoid being stuck forever in dungeons
                    if (state != State.Main && state != State.Loading) {
                        BHBot.logger.info("Ensuring that autoShrine settings are disabled");
                        if (!checkShrineSettings(false, false)) {
                            BHBot.logger.error("It was not possible to verify autoShrine settings");
                        }
                        autoShrined = false;

                        if (!BHBot.settings.autoRuneDefault.isEmpty()) {
                            BHBot.logger.info("Re-validating autoRunes");
                            if (!detectEquippedMinorRunes(true, true)) {
                                BHBot.logger.error("It was not possible to verify the equipped runes!");
                            }
                        }
                    }

                    if (BHBot.settings.enablePushover && BHBot.settings.poNotifyErrors) {
                        String idleTimerScreenName = saveGameScreen("idle-timer", img);
                        File idleTimerScreenFile = idleTimerScreenName != null ? new File(idleTimerScreenName) : null;
                        if (BHBot.settings.enablePushover && BHBot.settings.poNotifyErrors) {
                            sendPushOverMessage("Idle timer exceeded", "Idle time exceeded while state = " + state, "siren", MessagePriority.NORMAL, idleTimerScreenFile);

                            if (idleTimerScreenFile != null && !idleTimerScreenFile.delete()) {
                                BHBot.logger.error("Impossible to delete idle timer screenshot.");
                            }
                        }
                    }

                    restart();
                    continue;
                }
                BHBot.scheduler.resetIdleTime();

//                moveMouseAway(); // just in case. Sometimes we weren't able to claim daily reward because mouse was in center and popup window obfuscated the claim button (see screenshot of that error!)
                readScreen();
                MarvinSegment seg;

                seg = detectCue(cues.get("UnableToConnect"));
                if (seg != null) {
                    BHBot.logger.info("'Unable to connect' dialog detected. Reconnecting...");
                    seg = detectCue(cues.get("Reconnect"), 5 * SECOND);
                    clickOnSeg(seg);
                    sleep(5 * SECOND);
                    state = State.Loading;
                    continue;
                }


                // check for "Bit Heroes is currently down for maintenance. Please check back shortly!" window:
                seg = detectCue(cues.get("Maintenance"));
                if (seg != null) {
                    seg = detectCue(cues.get("Reconnect"), 5 * SECOND);
                    clickOnSeg(seg);
                    BHBot.logger.info("Maintenance dialog dismissed.");
                    sleep(5 * SECOND);
                    state = State.Loading;
                    continue;
                }

                // check for "You have been disconnected" dialog:
                seg = detectCue(cues.get("Disconnected"));
                if (seg != null) {
                    if (BHBot.scheduler.isUserInteracting || BHBot.scheduler.dismissReconnectOnNextIteration) {
                        BHBot.scheduler.isUserInteracting = false;
                        BHBot.scheduler.dismissReconnectOnNextIteration = false;
                        seg = detectCue(cues.get("Reconnect"), 5 * SECOND);
                        clickOnSeg(seg);
                        BHBot.logger.info("Disconnected dialog dismissed (reconnecting).");
                        sleep(5 * SECOND);
                        state = State.Loading;
                        continue;
                    } else {
                        BHBot.scheduler.isUserInteracting = true;
                        // probably user has logged in, that's why we got disconnected. Lets leave him alone for some time and then resume!
                        BHBot.logger.info("Disconnect has been detected. Probably due to user interaction. Sleeping for " + Misc.millisToHumanForm(BHBot.settings.reconnectTimer * MINUTE) + "...");
                        BHBot.scheduler.pause(BHBot.settings.reconnectTimer * MINUTE);
                        state = State.Loading;
                        continue;
                    }
                }

                BHBot.scheduler.dismissReconnectOnNextIteration = false; // must be done after checking for "Disconnected" dialog!

                // check for "There is a new update required to play" and click on "Reload" button:
                seg = detectCue(cues.get("Reload"));
                if (seg != null) {
                    clickOnSeg(seg);
                    BHBot.logger.info("Update dialog dismissed.");
                    sleep(5 * SECOND);
                    state = State.Loading;
                    continue;
                }

                // close any PMs:
                handlePM();

                // check for "Are you still there?" popup:
                seg = detectCue(cues.get("AreYouThere"));
                if (seg != null) {
                    BHBot.scheduler.restoreIdleTime();
                    seg = detectCue(cues.get("Yes"), 2 * SECOND);
                    if (seg != null)
                        clickOnSeg(seg);
                    else {
                        BHBot.logger.info("Problem: 'Are you still there?' popup detected, but 'Yes' button not detected. Ignoring...");
                        continue;
                    }
                    sleep(2 * SECOND);
                    continue; // skip other stuff, we must first get rid of this popup!
                }

                // check for "News" popup:
                seg = detectCue(cues.get("News"));
                if (seg != null) {
                    seg = detectCue(cues.get("Close"), 2 * SECOND);
                    clickOnSeg(seg);
                    BHBot.logger.info("News popup dismissed.");
                    readScreen(2 * SECOND);
                    continue;
                }

                // check for daily rewards popup:
                seg = detectCue(cues.get("DailyRewards"));
                if (seg != null) {
                    seg = detectCue(cues.get("Claim"), 5 * SECOND);
                    if (seg != null) {
                        if ((BHBot.settings.screenshots.contains("d"))) {
                            saveGameScreen("daily_reward", "rewards");
                        }
                         clickOnSeg(seg);
                    } else {
                        BHBot.logger.error("Problem: 'Daily reward' popup detected, however could not detect the 'claim' button. Restarting...");
                        restart();
                        continue; // may happen every while, rarely though
                    }

                    readScreen(5 * SECOND);
                    seg = detectCue(cues.get("Items"), SECOND);
                    if (seg == null) {
                        // we must terminate this thread... something happened that should not (unexpected). We must restart the thread!
                        BHBot.logger.error("Error: there is no 'Items' dialog open upon clicking on the 'Claim' button. Restarting...");
                        restart();
                        continue;
                    }
                    seg = detectCue(cues.get("X"));
                    clickOnSeg(seg);
                    BHBot.logger.info("Daily reward claimed successfully.");
                    sleep(2 * SECOND);

                    //We check for news and close so we don't take a gem count every time the bot starts
                    seg = detectCue(cues.get("News"), SECOND);
                    if (seg != null) {
                        seg = detectCue(cues.get("Close"), 2 * SECOND);
                        clickOnSeg(seg);
                        BHBot.logger.info("News popup dismissed.");
                        readScreen(2 * SECOND);

                        if ("7".equals(new SimpleDateFormat("u").format(new Date()))) { //if it's Sunday
                            if ((BHBot.settings.screenshots.contains("wg"))) {
                                saveGameScreen("weekly-gem-count", "gems");
                            }
                        } else {
                            if ((BHBot.settings.screenshots.contains("dg"))) {
                                saveGameScreen("daily-gem-count", "gems"); //else screenshot daily count
                            }
                        }

                        continue;
                    }

                    continue;
                }

                //Handle weekly rewards from events
                handleWeeklyRewards();

                // check for "recently disconnected" popup:
                seg = detectCue(cues.get("RecentlyDisconnected"));
                if (seg != null) {
                    seg = detectCue(cues.get("YesGreen"), 2 * SECOND);
                    if (seg == null) {
                        BHBot.logger.error("Error: detected 'recently disconnected' popup but could not find 'Yes' button. Restarting...");
                        restart();
                        continue;
                    }

                    clickOnSeg(seg);
                    if (state == State.Main || state == State.Loading) {
                        // we set this when we are not sure of what type of dungeon we are doing
                        state = State.UnidentifiedDungeon;
                    } else {
                        BHBot.logger.debug("RecentlyDisconnected status is: " + state);
                    }
                    BHBot.logger.info("'You were recently in a dungeon' dialog detected and confirmed. Resuming dungeon...");
                    sleep(60 * SECOND); //long sleep as if the checkShrine didn't find the potion button we'd enter a restart loop
                    checkShrineSettings(false, false); //in case we are stuck in a dungeon lets enable shrines/boss
                    continue;
                }

                // process dungeons of any kind (if we are in any):
                if (state == State.Raid || state == State.Trials || state == State.Gauntlet || state == State.Dungeon || state == State.PVP || state == State.GVG || state == State.Invasion || state == State.UnidentifiedDungeon || state == State.Expedition || state == State.WorldBoss) {
                    processDungeon();
                    continue;
                }

                // check if we are in the main menu:
                seg = detectCue(cues.get("Main"));
                if (seg != null) {
                    state = State.Main;

                    //Dungeon crash failsafe, this can happen if you crash and reconnect quickly, then get placed back in the dungeon with no reconnect dialogue
                    seg = detectCue(cues.get("AutoOn")); //if we're in Main state, with auto button visible, then we need to change state
                    if (seg != null) {
                        state = State.UnidentifiedDungeon; // we are not sure what type of dungeon we are doing
                        BHBot.logger.warn("Possible dungeon crash, activating failsafe");
                        saveGameScreen("dungeon-crash-failsafe", "errors");
                        checkShrineSettings(false, false); //in case we are stuck in a dungeon lets enable shrines/boss
                        continue;
                    }

                    // check for pushover alive notifications!
                    if (BHBot.settings.enablePushover && BHBot.settings.poNotifyAlive > 0) {

                        // startup notification
                        if (timeLastPOAlive == 0) {
                            timeLastPOAlive = Misc.getTime();

                            timeLastPOAlive = Misc.getTime();
                            String aliveScreenName = saveGameScreen("alive-screen");
                            File aliveScreenFile = aliveScreenName != null ? new File(aliveScreenName) : null;

                            sendPushOverMessage("Startup notification", "BHBot has been succesfully started!", MessagePriority.QUIET, aliveScreenFile);
                            if (aliveScreenFile != null && !aliveScreenFile.delete())
                                BHBot.logger.warn("Impossible to delete tmp img for startup notification.");
                        }

                        // periodic notification
                        if ((Misc.getTime() - timeLastPOAlive) > (BHBot.settings.poNotifyAlive * HOUR)) {
                            timeLastPOAlive = Misc.getTime();
                            String aliveScreenName = saveGameScreen("alive-screen");
                            File aliveScreenFile = aliveScreenName != null ? new File(aliveScreenName) : null;

                            StringBuilder aliveMsg = new StringBuilder();
                            aliveMsg.append("I am alive and doing fine!\n\n");

                            for (State state : State.values()) {
                                if (counters.get(state).getTotal() > 0) {
                                    aliveMsg.append(state.getName()).append(" ")
                                            .append(counters.get(state).successRateDesc())
                                            .append("\n");
                                }
                            }

                            sendPushOverMessage("Alive notification", aliveMsg.toString(), MessagePriority.QUIET, aliveScreenFile);
                            if (aliveScreenFile != null && !aliveScreenFile.delete())
                                BHBot.logger.warn("Impossible to delete tmp img for alive notification.");
                        }
                    }

                    // check for bonuses:
                    long BONUS_CHECK_INTERVAL = 10 * MINUTE;
                    if (BHBot.settings.autoConsume && (Misc.getTime() - timeLastBonusCheck > BONUS_CHECK_INTERVAL)) {
                        timeLastBonusCheck = Misc.getTime();
                        handleConsumables();
                    }

                    //comment for faster testing
//					oneTimeshrineCheck = true;
//					oneTimeRuneCheck = true;

                    // One time check for Autoshrine
                    if (!oneTimeshrineCheck) {

                        BHBot.logger.info("Startup check to make sure autoShrine is initially disabled");
                        if (!checkShrineSettings(false, false)) {
                            BHBot.logger.error("It was not possible to perform the autoShrine start-up check!");
                        }
                        oneTimeshrineCheck = true;
                        readScreen(2 * SECOND); // delay to close the settings window completely before we check for raid button else the settings window is hiding it
                    }

                    // One time check for equipped minor runes
                    if (!BHBot.settings.autoRuneDefault.isEmpty() && !oneTimeRuneCheck) {

                        BHBot.logger.info("Startup check to determined configured minor runes");
                        if (!detectEquippedMinorRunes(true, true)) {
                            BHBot.logger.error("It was not possible to perform the equipped runes start-up check! Disabling autoRune..");
                            BHBot.settings.autoRuneDefault.clear();
                            BHBot.settings.autoRune.clear();
                            BHBot.settings.autoBossRune.clear();
                            continue;

                        }
                        BHBot.logger.info(getRuneName(leftMinorRune.getRuneCueName()) + " equipped in left slot.");
                        BHBot.logger.info(getRuneName(rightMinorRune.getRuneCueName()) + " equipped in right slot.");
                        oneTimeRuneCheck = true;
                        readScreen(2 * SECOND); // delay to close the settings window completely before we check for raid button else the settings window is hiding it
                    }

                    String currentActivity = activitySelector(); //else select the activity to attempt
                    if (currentActivity != null) {
                        BHBot.logger.debug("Checking activity: " + currentActivity);
                    } else {
                        // If we don't have any activity to perform, we reset the idle timer check
                        BHBot.scheduler.resetIdleTime(true);
                        continue;
                    }

                    // check for shards:
                    if ("r".equals(currentActivity)) {
                        timeLastShardsCheck = Misc.getTime();

                        readScreen();
                        MarvinSegment raidBTNSeg = detectCue(cues.get("RaidButton"));

                        if (raidBTNSeg == null) { // if null, then raid button is transparent meaning that raiding is not enabled (we have not achieved it yet, for example)
                            BHBot.scheduler.restoreIdleTime();
                            continue;
                        }
                        clickOnSeg(raidBTNSeg);

                        seg = detectCue("RaidPopup", 5 * SECOND); // wait until the raid window opens
                        if (seg == null) {
                            BHBot.logger.warn("Error: attempt at opening raid window failed. No window cue detected. Ignoring...");
                            BHBot.scheduler.restoreIdleTime();
                            // we make sure that everything that can be closed is actually closed to avoid idle timeout
                            closePopupSecurely(cues.get("X"), cues.get("X"));
                            continue;
                        }


                        int shards = getShards();
                        globalShards = shards;
                        BHBot.logger.readout("Shards: " + shards + ", required: >" + BHBot.settings.minShards);

                        if (shards == -1) { // error
                            BHBot.scheduler.restoreIdleTime();
                            continue;
                        }

                        if ((shards == 0) || (!BHBot.scheduler.doRaidImmediately && (shards <= BHBot.settings.minShards || BHBot.settings.raids.size() == 0))) {
                            if (BHBot.scheduler.doRaidImmediately)
                                BHBot.scheduler.doRaidImmediately = false; // reset it

                            readScreen();
                            seg = detectCue(cues.get("X"), SECOND);
                            clickOnSeg(seg);
                            sleep(SECOND);

                            continue;

                        } else { // do the raiding!

                            if (BHBot.scheduler.doRaidImmediately)
                                BHBot.scheduler.doRaidImmediately = false; // reset it

                            //if we need to configure runes/settings we close the window first
                            if (BHBot.settings.autoShrine.contains("r") || BHBot.settings.autoRune.containsKey("r") || BHBot.settings.autoBossRune.containsKey("r")) {
                                readScreen();
                                seg = detectCue(cues.get("X"), SECOND);
                                clickOnSeg(seg);
                                readScreen(SECOND);
                            }

                            //autoshrine
                            if (BHBot.settings.autoShrine.contains("r")) {
                                BHBot.logger.info("Configuring autoShrine for Raid");
                                if (!checkShrineSettings(true, true)) {
                                    BHBot.logger.error("Impossible to configure autoShrine for Raid!");
                                }
                            }

                            //autoBossRune
                            if (BHBot.settings.autoBossRune.containsKey("r") && !BHBot.settings.autoShrine.contains("r")) { //if autoshrine disabled but autobossrune enabled
                                BHBot.logger.info("Configuring autoBossRune for Raid");
                                if (!checkShrineSettings(true, false)) {
                                    BHBot.logger.error("Impossible to configure autoBossRune for Raid!");
                                }
                            }

                            //activity runes
                            handleMinorRunes("r");

                            readScreen(SECOND);
                            clickOnSeg(raidBTNSeg);

                            String raid = decideRaidRandomly();
                            if (raid == null) {
                                BHBot.settings.activitiesEnabled.remove("r");
                                BHBot.logger.error("It was impossible to choose a raid randomly, raids are disabled!");
                                if (BHBot.settings.enablePushover && BHBot.settings.poNotifyErrors)
                                    sendPushOverMessage("Raid Error", "It was impossible to choose a raid randomly, raids are disabled!", "siren");
                                continue;
                            }

                            int difficulty = Integer.parseInt(raid.split(" ")[1]);
                            int desiredRaid = Integer.parseInt(raid.split(" ")[0]);

                            if (!handleRaidSelection(desiredRaid, difficulty)) {
                                restart();
                                continue;
                            }

                            readScreen(2 * SECOND);
                            seg = detectCue(cues.get("RaidSummon"), 2 * SECOND);
                            if (seg == null) {
                                BHBot.logger.error("Summon button not found");
                            }
                            clickOnSeg(seg);
                            readScreen(2 * SECOND);

                            // dismiss character dialog if it pops up:
                            readScreen();
                            detectCharacterDialogAndHandleIt();

                            seg = detectCue(cues.get(difficulty == 1 ? "Normal" : difficulty == 2 ? "Hard" : "Heroic"));
                            clickOnSeg(seg);
                            readScreen(2 * SECOND);
                            seg = detectCue(cues.get("Accept"), 5 * SECOND);
                            clickOnSeg(seg);
                            readScreen(2 * SECOND);

                            if (handleTeamMalformedWarning()) {
                                BHBot.logger.error("Team incomplete, doing emergency restart..");
                                restart();
                                continue;
                            } else {
                                state = State.Raid;
                                BHBot.logger.info("Raid initiated!");
                                autoShrined = false;
                                autoBossRuned = false;
                            }
                        }
                        continue;
                    } // shards

                    // check for tokens (trials and gauntlet):
                    if (BHBot.scheduler.doTrialsImmediately || BHBot.scheduler.doGauntletImmediately ||
                            ("t".equals(currentActivity)) || ("g".equals(currentActivity))) {
                        if ("t".equals(currentActivity)) timeLastTrialsTokensCheck = Misc.getTime();
                        if ("g".equals(currentActivity)) timeLastGauntletTokensCheck = Misc.getTime();

                        readScreen();

                        boolean trials;
                        seg = detectCue(cues.get("Trials"));
                        if (seg == null) seg = detectCue(cues.get("Trials2"));
                        trials = seg != null; // if false, then we will do gauntlet instead of trials

                        if (seg == null)
                            seg = detectCue(cues.get("Gauntlet"));
                        if (seg == null) {
                            seg = detectCue(cues.get("Gauntlet2"));
                        }
                        if (seg == null) {// trials/gauntlet button not visible (perhaps it is disabled?)
                            BHBot.logger.warn("Gauntlet/Trials button not found");
                            BHBot.scheduler.restoreIdleTime();
                            continue;
                        }

                        if (("g".equals(currentActivity) && trials) || ("t".equals(currentActivity) && !trials))
                            continue;


                        clickOnSeg(seg);
                        MarvinSegment trialBTNSeg = seg;

                        // dismiss character dialog if it pops up:
                        readScreen(2 * SECOND);
                        detectCharacterDialogAndHandleIt();

                        readScreen();
                        int tokens = getTokens();
                        globalTokens = tokens;
                        BHBot.logger.readout("Tokens: " + tokens + ", required: >" + BHBot.settings.minTokens + ", " +
                                (trials ? "Trials" : "Gauntlet") + " cost: " + (trials ? BHBot.settings.costTrials : BHBot.settings.costGauntlet));

                        if (tokens == -1) { // error
                            BHBot.scheduler.restoreIdleTime();
                            continue;
                        }

                        if (((!BHBot.scheduler.doTrialsImmediately && !BHBot.scheduler.doGauntletImmediately) && (tokens <= BHBot.settings.minTokens)) || (tokens < (trials ? BHBot.settings.costTrials : BHBot.settings.costGauntlet))) {
                            readScreen();
                            seg = detectCue(cues.get("X"), SECOND);
                            clickOnSeg(seg);
                            readScreen(SECOND);

                            //if we have 1 token and need 5 we don't need to check every 10 minutes, this increases the timer so we start checking again when we are one token short
                            int tokenDifference = (trials ? BHBot.settings.costTrials : BHBot.settings.costGauntlet) - tokens; //difference between needed and current resource
                            if (tokenDifference > 1) {
                                int increase = (tokenDifference - 1) * 45;
                                TOKENS_CHECK_INTERVAL = increase * MINUTE; //add 45 minutes to TOKENS_CHECK_INTERVAL for each token needed above 1
                            } else TOKENS_CHECK_INTERVAL = 10 * MINUTE; //if we only need 1 token check every 10 minutes

                            if (BHBot.scheduler.doTrialsImmediately) {
                                BHBot.scheduler.doTrialsImmediately = false; // if we don't have resources to run we need to disable force it
                            } else if (BHBot.scheduler.doGauntletImmediately) {
                                BHBot.scheduler.doGauntletImmediately = false;
                            }

                            continue;
                        } else {
                            // do the trials/gauntlet!

                            if (BHBot.scheduler.doTrialsImmediately) {
                                BHBot.scheduler.doTrialsImmediately = false; // reset it
                            } else if (BHBot.scheduler.doGauntletImmediately) {
                                BHBot.scheduler.doGauntletImmediately = false;
                            }

                            // One time check for Autoshrine
                            if (trials) {

                                //if we need to configure runes/settings we close the window first
                                if (BHBot.settings.autoShrine.contains("t") || BHBot.settings.autoRune.containsKey("t") || BHBot.settings.autoBossRune.containsKey("t")) {
                                    readScreen();
                                    seg = detectCue(cues.get("X"), SECOND);
                                    clickOnSeg(seg);
                                    readScreen(SECOND);
                                }

                                //autoshrine
                                if (BHBot.settings.autoShrine.contains("t")) {
                                    BHBot.logger.info("Configuring autoShrine for Trials");
                                    if (!checkShrineSettings(true, true)) {
                                        BHBot.logger.error("Impossible to configure autoShrine for Trials!");
                                    }
                                }

                                //autoBossRune
                                if (BHBot.settings.autoBossRune.containsKey("t") && !BHBot.settings.autoShrine.contains("t")) { //if autoshrine disabled but autobossrune enabled
                                    BHBot.logger.info("Configuring autoBossRune for Trials");
                                    if (!checkShrineSettings(true, false)) {
                                        BHBot.logger.error("Impossible to configure autoBossRune for Trials!");
                                    }
                                }

                                //activity runes
                                handleMinorRunes("t");

                                readScreen(SECOND);
                                clickOnSeg(trialBTNSeg);
                                readScreen(SECOND); //wait for window animation

                            } else {

                                if (BHBot.settings.autoRune.containsKey("g")) {
                                    handleMinorRunes("g");
                                    readScreen(SECOND);
                                }

                                readScreen(SECOND);
                                clickOnSeg(trialBTNSeg);
                                readScreen(SECOND); //wait for window animation
                            }

                            // apply the correct difficulty
                            int targetDifficulty = trials ? BHBot.settings.difficultyTrials : BHBot.settings.difficultyGauntlet;

                            BHBot.logger.info("Attempting " + (trials ? "trials" : "gauntlet") + " at level " + targetDifficulty + "...");

                            int difficulty = detectDifficulty();
                            if (difficulty == 0) { // error!
                                BHBot.logger.error("Due to an error#1 in difficulty detection, " + (trials ? "trials" : "gauntlet") + " will be skipped.");
                                closePopupSecurely(cues.get("TrialsOrGauntletWindow"), cues.get("X"));
                                continue;
                            }
                            if (difficulty != targetDifficulty) {
                                BHBot.logger.info("Detected " + (trials ? "trials" : "gauntlet") + " difficulty level: " + difficulty + ", settings level: " + targetDifficulty + ". Changing..");
                                boolean result = selectDifficulty(difficulty, targetDifficulty);
                                if (!result) { // error!
                                    // see if drop down menu is still open and close it:
                                    readScreen(SECOND);
                                    tryClosingWindow(cues.get("DifficultyDropDown"));
                                    readScreen(5 * SECOND);
                                    BHBot.logger.warn("Unable to change difficulty, usually because desired level is not unlocked. Running " + (trials ? "trials" : "gauntlet") + " at " + difficulty + ".");
                                    sendPushOverMessage("T/G Error", "Unable to change difficulty to : " + targetDifficulty + " Running: " + difficulty + " instead.", "siren");
                                }
                            }

                            // select cost if needed:
                            readScreen(2 * SECOND); // wait for the popup to stabilize a bit
                            int cost = detectCost();
                            if (cost == 0) { // error!
                                BHBot.logger.error("Due to an error#1 in cost detection, " + (trials ? "trials" : "gauntlet") + " will be skipped.");
                                closePopupSecurely(cues.get("TrialsOrGauntletWindow"), cues.get("X"));
                                continue;
                            }
                            if (cost != (trials ? BHBot.settings.costTrials : BHBot.settings.costGauntlet)) {
                                BHBot.logger.info("Detected " + (trials ? "trials" : "gauntlet") + " cost: " + cost + ", settings cost is " + (trials ? BHBot.settings.costTrials : BHBot.settings.costGauntlet) + ". Changing it...");
                                boolean result = selectCost(cost, (trials ? BHBot.settings.costTrials : BHBot.settings.costGauntlet));
                                if (!result) { // error!
                                    // see if drop down menu is still open and close it:
                                    readScreen(SECOND);
                                    tryClosingWindow(cues.get("CostDropDown"));
                                    readScreen(5 * SECOND);
                                    tryClosingWindow(cues.get("TrialsOrGauntletWindow"));
                                    BHBot.logger.error("Due to an error#2 in cost selection, " + (trials ? "trials" : "gauntlet") + " will be skipped.");
                                    continue;
                                }

                                // We wait for the cost selector window to close
                                detectCue("TrialsOrGauntletWindow", SECOND * 2);
                                readScreen();
                            }

                            seg = detectCue(cues.get("Play"), 2 * SECOND);
                            if (seg == null) {
                                BHBot.logger.error("Error: Play button not found while trying to do " + (trials ? "trials" : "gauntlet") + ". Ignoring...");
                                tryClosingWindow(cues.get("TrialsOrGauntletWindow"));
                                continue;
                            }
                            clickOnSeg(seg);
                            readScreen(2 * SECOND);

                            if (!handleNotEnoughTokensPopup(false)) {
                                restart();
                                continue;
                            }

                            // dismiss character dialog if it pops up:
                            detectCharacterDialogAndHandleIt();

                            seg = detectCue(cues.get("Accept"), 5 * SECOND);
                            clickOnSeg(seg);
                            readScreen(2 * SECOND);

                            // This is a Bit Heroes bug!
                            // On t/g main screen the token bar is wrongly full so it goes trough the "Play" button and
                            // then it fails on the team "Accept" button
                            if (!handleNotEnoughTokensPopup(true)) {
                                restart();
                                continue;
                            }

                            sleep(3 * SECOND);

                            if (handleTeamMalformedWarning()) {
                                BHBot.logger.error("Team incomplete, doing emergency restart..");
                                restart();
                                continue;
                            } else {
                                state = trials ? State.Trials : State.Gauntlet;
                                BHBot.logger.info((trials ? "Trials" : "Gauntlet") + " initiated!");
                                autoShrined = false;
                                autoBossRuned = false;
                            }
                        }
                        continue;
                    } // tokens (trials and gauntlet)

                    // check for energy:
                    if ("d".equals(currentActivity)) {
                        timeLastEnergyCheck = Misc.getTime();

                        readScreen();

                        int energy = getEnergy();
                        globalEnergy = energy;
                        BHBot.logger.readout("Energy: " + energy + "%, required: >" + BHBot.settings.minEnergyPercentage + "%");

                        if (energy == -1) { // error
                            BHBot.scheduler.restoreIdleTime();
                            if (BHBot.scheduler.doDungeonImmediately)
                                BHBot.scheduler.doDungeonImmediately = false; // reset it
                            continue;
                        }

                        if (BHBot.settings.countActivities) {
                            int dungeonCounter = Integer.parseInt(BHBot.settings.dungeonsRun.split(" ")[1]);
                            if (dungeonCounter >= 10) {
                                if (BHBot.scheduler.doDungeonImmediately)
                                    BHBot.scheduler.doDungeonImmediately = false; // reset it
                                BHBot.scheduler.restoreIdleTime();
                                continue;
                            }
                        }

                        if (!BHBot.scheduler.doDungeonImmediately && (energy <= BHBot.settings.minEnergyPercentage || BHBot.settings.dungeons.size() == 0)) {
                            sleep(SECOND);

                            //if we have 1 resource and need 5 we don't need to check every 10 minutes, this increases the timer so we start checking again when we are one under the check limit
                            int energyDifference = BHBot.settings.minEnergyPercentage - energy; //difference between needed and current resource
                            if (energyDifference > 1) {
                                int increase = (energyDifference - 1) * 8;
                                ENERGY_CHECK_INTERVAL = increase * MINUTE; //add 8 minutes to the check interval for each energy % needed above 1
                            } else ENERGY_CHECK_INTERVAL = 10 * MINUTE; //if we only need 1 check every 10 minutes

                            continue;
                        } else {
                            // do the dungeon!

                            if (BHBot.scheduler.doDungeonImmediately)
                                BHBot.scheduler.doDungeonImmediately = false; // reset it

                            //configure activity runes
                            handleMinorRunes("d");

                            if (BHBot.settings.autoBossRune.containsKey("d") && !BHBot.settings.autoShrine.contains("d")) { //if autoshrine disabled but autorune enabled

                                BHBot.logger.info("Configuring autoBossRune for Dungeons");
                                if (!checkShrineSettings(true, false)) {
                                    BHBot.logger.error("Impossible to configure autoBossRune for Dungeons!");
                                }

                                readScreen(SECOND);
                                sleep(2 * SECOND);
                            }

                            seg = detectCue(cues.get("Quest"));
                            clickOnSeg(seg);
                            readScreen(5 * SECOND);

                            String dungeon = decideDungeonRandomly();
                            if (dungeon == null) {
                                BHBot.settings.activitiesEnabled.remove("d");
                                BHBot.logger.error("It was impossible to choose a dungeon randomly, dungeons are disabled!");
                                if (BHBot.settings.enablePushover && BHBot.settings.poNotifyErrors)
                                    sendPushOverMessage("Dungeon error", "It was impossible to choose a dungeon randomly, dungeons are disabled!", "siren");
                                continue;
                            }

                            Matcher dungeonMatcher = dungeonRegex.matcher(dungeon.toLowerCase());
                            if (!dungeonMatcher.find()) {
                                BHBot.logger.error("Wrong format in dungeon detected: " + dungeon + "! It will be skipped...");
                                if (BHBot.settings.enablePushover && BHBot.settings.poNotifyErrors)
                                    sendPushOverMessage("Dungeon error", "Wrong dungeon format detected: " + dungeon, "siren");
                                continue;
                            }

                            int goalZone = Integer.parseInt(dungeonMatcher.group("zone"));
                            int goalDungeon = Integer.parseInt(dungeonMatcher.group("dungeon"));
                            int difficulty = Integer.parseInt(dungeonMatcher.group("difficulty"));

                            String difficultyName = (difficulty == 1 ? "Normal" : difficulty == 2 ? "Hard" : "Heroic");

                            BHBot.logger.info("Attempting " + difficultyName + " z" + goalZone + "d" + goalDungeon);

                            int currentZone = readCurrentZone();
                            int vec = goalZone - currentZone; // movement vector
//							BHBot.logger.info("Current zone: " + Integer.toString(currentZone) + " Target Zone: " + Integer.toString(goalZone));
                            while (vec != 0) { // move to the correct zone
                                if (vec > 0) {
                                    // note that moving to the right will fail in case player has not unlocked the zone yet!
                                    readScreen(SECOND); // wait for screen to stabilise
                                    seg = detectCue(cues.get("RightArrow"));
                                    if (seg == null) {
                                        BHBot.logger.error("Right button not found, zone unlocked?");
                                        break; // happens for example when player hasn't unlock the zone yet
                                    }
                                    //coords are used as moving multiple screens would crash the bot when using the arrow cues
                                    clickInGame(740, 275);
                                    vec--;
                                } else {
                                    sleep(500);
                                    //coords are used as moving multiple screens would crash the bot when using the arrow cues
                                    clickInGame(55, 275);
                                    vec++;
                                }
                            }

                            readScreen(2 * SECOND);

                            // click on the dungeon:
                            Point p = getDungeonIconPos(goalZone, goalDungeon);
                            if (p == null) {
                                BHBot.settings.activitiesEnabled.remove("d");
                                BHBot.logger.error("It was impossible to get icon position of dungeon z" + goalZone + "d" + goalDungeon + ". Dungeons are now disabled!");
                                if (BHBot.settings.enablePushover && BHBot.settings.poNotifyErrors)
                                    sendPushOverMessage("Dungeon error", "It was impossible to get icon position of dungeon z" + goalZone + "d" + goalDungeon + ". Dungeons are now disabled!", "siren");
                                continue;
                            }

                            clickInGame(p.x, p.y);

                            readScreen(3 * SECOND);
                            // select difficulty (If D4 just hit enter):
                            if ((goalDungeon == 4) || (goalZone == 7 && goalDungeon == 3) || (goalZone == 8 && goalDungeon == 3)) { // D4, or Z7D3/Z8D3
                                specialDungeon = true;
                                seg = detectCue(cues.get("Enter"), 5 * SECOND);
                                clickOnSeg(seg);
                            } else { //else select appropriate difficulty
                                seg = detectCue(cues.get(difficulty == 1 ? "Normal" : difficulty == 2 ? "Hard" : "Heroic"), 5 * SECOND);
                                clickOnSeg(seg);
                            }

                            //team selection screen
                            /* Solo-for-bounty code */
                            if (goalZone <= BHBot.settings.minSolo) { //if the level is soloable then clear the team to complete bounties
                                readScreen(SECOND);
                                seg = detectCue(cues.get("Clear"), SECOND * 2);
                                if (seg != null) {
                                    BHBot.logger.info("Selected zone under dungeon solo threshold, attempting solo");
                                    clickOnSeg(seg);
                                } else {
                                    BHBot.logger.error("Impossible to find clear button in Dungeon Team!");
                                    restart();
                                    continue;
                                }
                            }

                            readScreen();
                            seg = detectCue(cues.get("Accept"), SECOND * 2);
                            clickOnSeg(seg);

                            if (goalZone <= BHBot.settings.minSolo) {
                                readScreen(3 * SECOND); //wait for dropdown animation to finish
                                seg = detectCue(cues.get("YesGreen"), 2 * SECOND);
                                if (seg != null) {
                                    clickOnSeg(seg);
                                } else {
                                    BHBot.logger.error("Impossible to find Yes button in Dungeon Team!");
                                    restart();
                                }
                            } else {
                                if (handleTeamMalformedWarning()) {
                                    restart();
                                    continue;
                                }
                            }

                            if (handleNotEnoughEnergyPopup(3 * SECOND, State.Dungeon)) {
                                continue;
                            }

                            state = State.Dungeon;
                            autoShrined = false;
                            autoBossRuned = false;

                            BHBot.logger.info("Dungeon <z" + goalZone + "d" + goalDungeon + "> " + (difficulty == 1 ? "normal" : difficulty == 2 ? "hard" : "heroic") + " initiated!");
                        }
                        continue;
                    } // energy

                    // check for Tickets (PvP):
                    if ("p".equals(currentActivity)) {
                        timeLastTicketsCheck = Misc.getTime();

                        readScreen();

                        int tickets = getTickets();
                        globalTickets = tickets;
                        BHBot.logger.readout("Tickets: " + tickets + ", required: >" + BHBot.settings.minTickets + ", PVP cost: " + BHBot.settings.costPVP);

                        if (tickets == -1) { // error
                            BHBot.scheduler.restoreIdleTime();
                            continue;
                        }

                        if ((!BHBot.scheduler.doPVPImmediately && (tickets <= BHBot.settings.minTickets)) || (tickets < BHBot.settings.costPVP)) {
                            sleep(SECOND);

                            //if we have 1 resource and need 5 we don't need to check every 10 minutes, this increases the timer so we start checking again when we are one under the check limit
                            int ticketDifference = BHBot.settings.costPVP - tickets; //difference between needed and current resource
                            if (ticketDifference > 1) {
                                int increase = (ticketDifference - 1) * 45;
                                TICKETS_CHECK_INTERVAL = increase * MINUTE; //add 45 minutes to the check interval for each ticket needed above 1
                            } else TICKETS_CHECK_INTERVAL = 10 * MINUTE; //if we only need 1 check every 10 minutes

                            continue;
                        } else {
                            // do the pvp!

                            if (BHBot.scheduler.doPVPImmediately)
                                BHBot.scheduler.doPVPImmediately = false; // reset it

                            //configure activity runes
                            handleMinorRunes("p");

                            BHBot.logger.info("Attempting PVP...");
                            stripDown(BHBot.settings.pvpstrip);

                            seg = detectCue(cues.get("PVP"));
                            if (seg == null) {
                                BHBot.logger.warn("PVP button not found. Skipping PVP...");
                                dressUp(BHBot.settings.pvpstrip);
                                continue; // should not happen though
                            }
                            clickOnSeg(seg);

                            // select cost if needed:
                            readScreen(2 * SECOND); // wait for the popup to stabilize a bit
                            int cost = detectCost();
                            if (cost == 0) { // error!
                                BHBot.logger.error("Due to an error#1 in cost detection, PVP will be skipped.");
                                closePopupSecurely(cues.get("PVPWindow"), cues.get("X"));
                                dressUp(BHBot.settings.pvpstrip);
                                continue;
                            }
                            if (cost != BHBot.settings.costPVP) {
                                BHBot.logger.info("Detected PVP cost: " + cost + ", settings cost is " + BHBot.settings.costPVP + ". Changing..");
                                boolean result = selectCost(cost, BHBot.settings.costPVP);
                                if (!result) { // error!
                                    // see if drop down menu is still open and close it:
                                    readScreen(SECOND);
                                    tryClosingWindow(cues.get("CostDropDown"));
                                    readScreen(5 * SECOND);
                                    seg = detectCue(cues.get("PVPWindow"), 15 * SECOND);
                                    if (seg != null)
                                        closePopupSecurely(cues.get("PVPWindow"), cues.get("X"));
                                    BHBot.logger.error("Due to an error#2 in cost selection, PVP will be skipped.");
                                    dressUp(BHBot.settings.pvpstrip);
                                    continue;
                                }
                            }

                            seg = detectCue(cues.get("Play"), 5 * SECOND);
                            clickOnSeg(seg);
                            readScreen(2 * SECOND);

                            // dismiss character dialog if it pops up:
                            detectCharacterDialogAndHandleIt();

                            Bounds pvpOpponentBounds = opponentSelector(BHBot.settings.pvpOpponent);
                            String opponentName = (BHBot.settings.pvpOpponent == 1 ? "1st" : BHBot.settings.pvpOpponent == 2 ? "2nd" : BHBot.settings.pvpOpponent == 3 ? "3rd" : "4th");
                            BHBot.logger.info("Selecting " + opponentName + " opponent");
                            seg = detectCue("Fight", 5 * SECOND, pvpOpponentBounds);
                            if (seg == null) {
                                BHBot.logger.error("Imppossible to find the Fight button in the PVP screen, restarting!");
                                restart();
                                continue;
                            }
                            clickOnSeg(seg);

                            readScreen();
                            seg = detectCue("Accept", 5 * SECOND, new Bounds(430, 430, 630, 500));
                            if (seg == null) {
                                BHBot.logger.error("Impossible to find the Accept button in the PVP screen, restarting");
                                restart();
                                continue;
                            }
                            clickOnSeg(seg);

                            if (handleTeamMalformedWarning()) {
                                BHBot.logger.error("Team incomplete, doing emergency restart..");
                                restart();
                                continue;
                            } else {
                                state = State.PVP;
                                BHBot.logger.info("PVP initiated!");
                            }
                        }
                        continue;
                    } // PvP

                    // check for badges (for GVG/Invasion/Expedition):
                    if (("v".equals(currentActivity)) || ("i".equals(currentActivity)) || ("e".equals(currentActivity))) {

                        String checkedActivity = currentActivity;

                        if ("v".equals(currentActivity)) timeLastGVGBadgesCheck = Misc.getTime();
                        if ("i".equals(currentActivity)) timeLastInvBadgesCheck = Misc.getTime();
                        if ("e".equals(currentActivity)) timeLastExpBadgesCheck = Misc.getTime();

                        readScreen();

                        BadgeEvent badgeEvent = BadgeEvent.None;
                        MarvinSegment badgeBtn = null;

                        HashMap<Cue, BadgeEvent> badgeEvents = new HashMap<>();
                        badgeEvents.put(cues.get("ExpeditionButton"), BadgeEvent.Expedition);
                        badgeEvents.put(cues.get("GVG"), BadgeEvent.GVG);
                        badgeEvents.put(cues.get("Invasion"), BadgeEvent.Invasion);

                        for (Map.Entry<Cue, BadgeEvent> event : badgeEvents.entrySet()) {
                            badgeBtn = detectCue(event.getKey());
                            if (badgeBtn != null) {
                                badgeEvent = event.getValue();
                                seg = badgeBtn;
                                break;
                            }
                        }


                        if (badgeEvent == BadgeEvent.None) { // GvG/invasion button not visible (perhaps this week there is no GvG/Invasion/Expedition event?)
                            BHBot.scheduler.restoreIdleTime();
                            BHBot.logger.debug("No badge event found, skipping");
                            continue;
                        }

                        if (badgeEvent == BadgeEvent.Expedition) currentActivity = "e";
                        if (badgeEvent == BadgeEvent.Invasion) currentActivity = "i";
                        if (badgeEvent == BadgeEvent.GVG) currentActivity = "v";

                        if (!currentActivity.equals(checkedActivity)) { //if checked activity and chosen activity don't match we skip
                            continue;
                        }

                        clickOnSeg(seg);
                        sleep(2 * SECOND);

                        detectCharacterDialogAndHandleIt(); // needed for invasion

                        readScreen();
                        int badges = getBadges();
                        globalBadges = badges;
                        BHBot.logger.readout("Badges: " + badges + ", required: >" + BHBot.settings.minBadges + ", " + badgeEvent.toString() + " cost: " +
                                (badgeEvent == BadgeEvent.GVG ? BHBot.settings.costGVG : badgeEvent == BadgeEvent.Invasion ? BHBot.settings.costInvasion : BHBot.settings.costExpedition));

                        if (badges == -1) { // error
                            BHBot.scheduler.restoreIdleTime();
                            continue;
                        }

                        // check GVG:
                        if (badgeEvent == BadgeEvent.GVG) {
                            if ((!BHBot.scheduler.doGVGImmediately && (badges <= BHBot.settings.minBadges)) || (badges < BHBot.settings.costGVG)) {

                                //if we have 1 resource and need 5 we don't need to check every 10 minutes, this increases the timer so we start checking again when we are one under the check limit
                                int badgeDifference = BHBot.settings.costGVG - badges; //difference between needed and current resource
                                if (badgeDifference > 1) {
                                    int increase = (badgeDifference - 1) * 45;
                                    BADGES_CHECK_INTERVAL = increase * MINUTE; //add 45 minutes to the check interval for each ticket needed above 1
                                } else BADGES_CHECK_INTERVAL = 10 * MINUTE; //if we only need 1 check every 10 minutes

                                readScreen();
                                seg = detectCue(cues.get("X"), SECOND);
                                clickOnSeg(seg);
                                sleep(SECOND);
                                continue;
                            } else {
                                // do the GVG!

                                if (BHBot.scheduler.doGVGImmediately)
                                    BHBot.scheduler.doGVGImmediately = false; // reset it


                                //configure activity runes
                                handleMinorRunes("v");
                                readScreen(SECOND);
                                clickOnSeg(badgeBtn);

                                BHBot.logger.info("Attempting GVG...");

                                if (BHBot.settings.gvgstrip.size() > 0) {
                                    // If we need to strip down for GVG, we need to close the GVG gump and open it again
                                    seg = detectCue(cues.get("X"), SECOND * 2);
                                    clickOnSeg(seg);
                                    readScreen(2 * SECOND);
                                    stripDown(BHBot.settings.gvgstrip);
                                    seg = detectCue(cues.get("GVG"), SECOND * 3);
                                    clickOnSeg(seg);
                                }

                                // select cost if needed:
                                readScreen(2 * SECOND); // wait for the popup to stabilize a bit
                                int cost = detectCost();
                                if (cost == 0) { // error!
                                    BHBot.logger.error("Due to an error#1 in cost detection, GVG will be skipped.");
                                    closePopupSecurely(cues.get("GVGWindow"), cues.get("X"));
                                    continue;
                                }
                                if (cost != BHBot.settings.costGVG) {
                                    BHBot.logger.info("Detected GVG cost: " + cost + ", settings cost is " + BHBot.settings.costGVG + ". Changing..");
                                    boolean result = selectCost(cost, BHBot.settings.costGVG);
                                    if (!result) { // error!
                                        // see if drop down menu is still open and close it:
                                        readScreen(SECOND);
                                        tryClosingWindow(cues.get("CostDropDown"));
                                        readScreen(5 * SECOND);
                                        seg = detectCue(cues.get("GVGWindow"), 15 * SECOND);
                                        if (seg != null)
                                            closePopupSecurely(cues.get("GVGWindow"), cues.get("X"));
                                        BHBot.logger.error("Due to an error#2 in cost selection, GVG will be skipped.");
                                        dressUp(BHBot.settings.gvgstrip);
                                        continue;
                                    }
                                }


                                seg = detectCue(cues.get("Play"), 5 * SECOND);
                                clickOnSeg(seg);
                                readScreen(2 * SECOND);

                                // Sometimes, before the reset, battles are disabled
                                Boolean disabledBattles = handleDisabledBattles();
                                if (disabledBattles == null) {
                                    restart();
                                    continue;
                                } else if (disabledBattles) {
                                    readScreen();
                                    closePopupSecurely(cues.get("GVGWindow"), cues.get("X"));
                                    continue;
                                }

                                //On initial GvG run you'll get a warning about not being able to leave guild, this will close that
                                if (handleGuildLeaveConfirm()) {
                                    restart();
                                    continue;
                                }

                                Bounds gvgOpponentBounds = opponentSelector(BHBot.settings.gvgOpponent);
                                String opponentName = (BHBot.settings.gvgOpponent == 1 ? "1st" : BHBot.settings.gvgOpponent == 2 ? "2nd" : BHBot.settings.gvgOpponent == 3 ? "3rd" : "4th");
                                BHBot.logger.info("Selecting " + opponentName + " opponent");
                                seg = detectCue(cues.get("Fight"), 5 * SECOND, gvgOpponentBounds);
                                if (seg == null) {
                                    BHBot.logger.error("Imppossible to find the Fight button in the GvG screen, restarting!");
                                    restart();
                                    continue;
                                }
                                clickOnSeg(seg);
                                readScreen();
                                sleep(SECOND);

                                seg = detectCue(cues.get("Accept"), 2 * SECOND);
                                if (seg == null) {
                                    BHBot.logger.error("Imppossible to find the Accept button in the GvG screen, restarting!");
                                    restart();
                                    continue;
                                }
                                clickOnSeg(seg);
                                sleep(SECOND);

                                if (handleTeamMalformedWarning()) {
                                    BHBot.logger.error("Team incomplete, doing emergency restart..");
                                    restart();
                                    continue;
                                } else {
                                    state = State.GVG;
                                    BHBot.logger.info("GVG initiated!");
                                }
                            }
                            continue;
                        } // GvG
                        // check invasion:
                        else if (badgeEvent == BadgeEvent.Invasion) {
                            if ((!BHBot.scheduler.doInvasionImmediately && (badges <= BHBot.settings.minBadges)) || (badges < BHBot.settings.costInvasion)) {

                                //if we have 1 resource and need 5 we don't need to check every 10 minutes, this increases the timer so we start checking again when we are one under the check limit
                                int badgeDifference = BHBot.settings.costGVG - badges; //difference between needed and current resource
                                if (badgeDifference > 1) {
                                    int increase = (badgeDifference - 1) * 45;
                                    BADGES_CHECK_INTERVAL = increase * MINUTE; //add 45 minutes to the check interval for each ticket needed above 1
                                } else BADGES_CHECK_INTERVAL = 10 * MINUTE; //if we only need 1 check every 10 minutes

                                readScreen();
                                seg = detectCue(cues.get("X"), SECOND);
                                clickOnSeg(seg);
                                sleep(SECOND);
                                continue;
                            } else {
                                // do the invasion!

                                if (BHBot.scheduler.doInvasionImmediately)
                                    BHBot.scheduler.doInvasionImmediately = false; // reset it

                                //configure activity runes
                                handleMinorRunes("i");
                                readScreen(SECOND);
                                clickOnSeg(badgeBtn);

                                BHBot.logger.info("Attempting invasion...");

                                // select cost if needed:
                                readScreen(2 * SECOND); // wait for the popup to stabilize a bit
                                int cost = detectCost();
                                if (cost == 0) { // error!
                                    BHBot.logger.error("Due to an error#1 in cost detection, invasion will be skipped.");
                                    closePopupSecurely(cues.get("InvasionWindow"), cues.get("X"));
                                    continue;
                                }
                                if (cost != BHBot.settings.costInvasion) {
                                    BHBot.logger.info("Detected invasion cost: " + cost + ", settings cost is " + BHBot.settings.costInvasion + ". Changing..");
                                    boolean result = selectCost(cost, BHBot.settings.costInvasion);
                                    if (!result) { // error!
                                        // see if drop down menu is still open and close it:
                                        readScreen(SECOND);
                                        tryClosingWindow(cues.get("CostDropDown"));
                                        readScreen(5 * SECOND);
                                        seg = detectCue(cues.get("InvasionWindow"), 15 * SECOND);
                                        if (seg != null)
                                            closePopupSecurely(cues.get("InvasionWindow"), cues.get("X"));
                                        BHBot.logger.error("Due to an error#2 in cost selection, invasion will be skipped.");
                                        continue;
                                    }
                                }

                                seg = detectCue(cues.get("Play"), 5 * SECOND);
                                clickOnSeg(seg);

                                readScreen(3000);
                                seg = detectCue(cues.get("Accept"));
                                if (seg == null) {
                                    BHBot.logger.error("Unable to find the Accept button in the Invasion screen, restarting!");
                                    restart();
                                    continue;
                                }
                                clickOnSeg(seg);
                                sleep(2 * SECOND);

                                if (handleTeamMalformedWarning()) {
                                    BHBot.logger.error("Team incomplete, doing emergency restart..");
                                    restart();
                                    continue;
                                } else {
                                    state = State.Invasion;
                                    BHBot.logger.info("Invasion initiated!");
                                    autoShrined = false;
                                }
                            }
                            continue;
                        } // invasion

                        // check Expedition
                        else if (badgeEvent == BadgeEvent.Expedition) {

                            if ((!BHBot.scheduler.doExpeditionImmediately && (badges <= BHBot.settings.minBadges)) || (badges < BHBot.settings.costExpedition)) {

                                //if we have 1 resource and need 5 we don't need to check every 10 minutes, this increases the timer so we start checking again when we are one under the check limit
                                int badgeDifference = BHBot.settings.costGVG - badges; //difference between needed and current resource
                                if (badgeDifference > 1) {
                                    int increase = (badgeDifference - 1) * 45;
                                    BADGES_CHECK_INTERVAL = increase * MINUTE; //add 45 minutes to the check interval for each ticket needed above 1
                                } else BADGES_CHECK_INTERVAL = 10 * MINUTE; //if we only need 1 check every 10 minutes

                                seg = detectCue(cues.get("X"));
                                clickOnSeg(seg);
                                sleep(2 * SECOND);
                                continue;
                            } else {
                                // do the expedition!

                                if (BHBot.scheduler.doExpeditionImmediately)
                                    BHBot.scheduler.doExpeditionImmediately = false; // reset it

                                if (BHBot.settings.costExpedition > badges) {
                                    BHBot.logger.info("Target cost " + BHBot.settings.costExpedition + " is higher than available badges " + badges + ". Expedition will be skipped.");
                                    seg = detectCue(cues.get("X"));
                                    clickOnSeg(seg);
                                    sleep(2 * SECOND);
                                    continue;
                                }

                                //if we need to configure runes/settings we close the window first
                                if (BHBot.settings.autoShrine.contains("e") || BHBot.settings.autoRune.containsKey("e") || BHBot.settings.autoBossRune.containsKey("e")) {
                                    readScreen();
                                    seg = detectCue(cues.get("X"), SECOND);
                                    clickOnSeg(seg);
                                    readScreen(SECOND);
                                }

                                //autoshrine
                                if (BHBot.settings.autoShrine.contains("e")) {
                                    BHBot.logger.info("Configuring autoShrine for Expedition");
                                    if (!checkShrineSettings(true, true)) {
                                        BHBot.logger.error("Impossible to configure autoShrine for Expedition!");
                                    }
                                }

                                //autoBossRune
                                if (BHBot.settings.autoBossRune.containsKey("e") && !BHBot.settings.autoShrine.contains("e")) { //if autoshrine disabled but autobossrune enabled
                                    BHBot.logger.info("Configuring autoBossRune for Expedition");
                                    if (!checkShrineSettings(true, false)) {
                                        BHBot.logger.error("Impossible to configure autoBossRune for Expedition!");
                                    }
                                }

                                //activity runes
                                handleMinorRunes("e");

                                readScreen(SECOND);
                                clickOnSeg(badgeBtn);
                                readScreen(SECOND * 2);

                                BHBot.logger.info("Attempting expedition...");

                                readScreen(SECOND * 2);
                                int cost = detectCost();
                                if (cost == 0) { // error!
                                    BHBot.logger.error("Due to an error#1 in cost detection, Expedition cost will be skipped.");
                                    closePopupSecurely(cues.get("ExpeditionWindow"), cues.get("X"));
                                    continue;
                                }

                                if (cost != BHBot.settings.costExpedition) {
                                    BHBot.logger.info("Detected Expedition cost: " + cost + ", settings cost is " + BHBot.settings.costExpedition + ". Changing..");
                                    boolean result = selectCost(cost, BHBot.settings.costExpedition);
                                    if (!result) { // error!
                                        // see if drop down menu is still open and close it:
                                        readScreen(SECOND);
                                        tryClosingWindow(cues.get("CostDropDown"));
                                        readScreen(5 * SECOND);
                                        seg = detectCue(cues.get("X"));
                                        clickOnSeg(seg);
                                        sleep(2 * SECOND);
                                        BHBot.logger.error("Due to an error in cost selection, Expedition will be skipped.");
                                        continue;
                                    }
                                    readScreen(SECOND * 2);
                                }

                                seg = detectCue(cues.get("Play"), 2 * SECOND);
                                clickOnSeg(seg);
                                readScreen(2 * SECOND);

                                //Select Expedition and write portal to a variable
                                String randomExpedition = BHBot.settings.expeditions.next();
                                if (randomExpedition == null) {
                                    BHBot.settings.activitiesEnabled.remove("e");
                                    BHBot.logger.error("It was impossible to randomly choose an expedition. Expeditions are disabled.");
                                    if (BHBot.settings.enablePushover && BHBot.settings.poNotifyErrors)
                                        sendPushOverMessage("Expedition error", "It was impossible to randomly choose an expedition. Expeditions are disabled.", "siren");
                                    continue;
                                }

                                String[] expedition = randomExpedition.split(" ");
                                String targetPortal = expedition[0];
                                int targetDifficulty = Integer.parseInt(expedition[1]);

                                // if exped difficulty isn't a multiple of 5 we reduce it
                                int difficultyModule = targetDifficulty % 5;
                                if (difficultyModule != 0) {
                                    BHBot.logger.warn(targetDifficulty + " is not a multiplier of 5! Rounding it to " + (targetDifficulty - difficultyModule) + "..." );
                                    targetDifficulty -= difficultyModule;
                                }
                                // If difficulty is lesser that 5, we round it
                                if (targetDifficulty < 5) {
                                    BHBot.logger.warn("Expedition difficulty can not be smaller than 5, rounding it to 5.");
                                    targetDifficulty = 5;
                                }

                                readScreen();
                                int currentExpedition;
                                if (detectCue(cues.get("Expedition1")) != null) {
                                    currentExpedition = 1;
                                } else if (detectCue(cues.get("Expedition2")) != null) {
                                    currentExpedition = 2;
                                } else if (detectCue(cues.get("Expedition3")) != null) {
                                    currentExpedition = 3;
                                } else if (detectCue(cues.get("Expedition4")) != null) {
                                    currentExpedition = 4;
                                } else if (detectCue("Expedition5") != null) {
                                    currentExpedition = 5;
                                } else {
                                    BHBot.settings.activitiesEnabled.remove("e");
                                    BHBot.logger.error("It was impossible to get the current expedition type!");
                                    if (BHBot.settings.enablePushover && BHBot.settings.poNotifyErrors)
                                        sendPushOverMessage("Expedition error", "It was impossible to get the current expedition type. Expeditions are now disabled!", "siren");

                                    readScreen();
                                    seg = detectCue(cues.get("X"), SECOND);
                                    if (seg != null) clickOnSeg(seg);
                                    readScreen(2 * SECOND);
                                    continue;
                                }

                                String portalName = getExpeditionPortalName(currentExpedition, targetPortal);
                                BHBot.logger.info("Attempting " + portalName + " Portal at difficulty " + targetDifficulty);

                                //write current portal and difficulty to global values for difficultyFailsafe
                                expeditionFailsafePortal = targetPortal;
                                expeditionFailsafeDifficulty = targetDifficulty;

                                // click on the chosen portal:
                                Point p = getExpeditionIconPos(currentExpedition, targetPortal);
                                if (p == null) {
                                    BHBot.settings.activitiesEnabled.remove("e");
                                    BHBot.logger.error("It was impossible to get portal position for " + portalName + ". Expeditions are now disabled!");
                                    if (BHBot.settings.enablePushover && BHBot.settings.poNotifyErrors)
                                        sendPushOverMessage("Expedition error", "It was impossible to get portal position for " + portalName + ". Expeditions are now disabled!", "siren");

                                    readScreen();
                                    seg = detectCue(cues.get("X"), SECOND);
                                    if (seg != null) clickOnSeg(seg);
                                    readScreen(2 * SECOND);
                                    continue;
                                }

                                clickInGame(p.x, p.y);

                                // select difficulty if needed:
                                int difficulty = detectDifficulty(cues.get("DifficultyExpedition"));
                                if (difficulty == 0) { // error!
                                    BHBot.logger.warn("Due to an error in difficulty detection, Expedition will be skipped.");
                                    seg = detectCue(cues.get("X"));
                                    while (seg != null) {
                                        clickOnSeg(seg);
                                        readScreen(2 * SECOND);
                                        seg = detectCue(cues.get("X"));
                                    }
                                    continue;
                                }

                                if (difficulty != targetDifficulty) {
                                    BHBot.logger.info("Detected Expedition difficulty level: " + difficulty + ", settings level is " + targetDifficulty + ". Changing..");
                                    boolean result = selectDifficulty(difficulty, targetDifficulty, cues.get("SelectDifficultyExpedition"), 5);
                                    if (!result) { // error!
                                        // see if drop down menu is still open and close it:
                                        readScreen();
                                        seg = detectCue(cues.get("X"));
                                        while (seg != null) {
                                            clickOnSeg(seg);
                                            readScreen(2 * SECOND);
                                            seg = detectCue(cues.get("X"));
                                        }
                                        BHBot.logger.error("Due to an error in difficulty selection, Expedition will be skipped.");
                                        continue;
                                    }
                                }

                                //click enter
                                seg = detectCue(cues.get("Enter"), 2 * SECOND);
                                clickOnSeg(seg);

                                //click enter
                                seg = detectCue(cues.get("Accept"), 3 * SECOND);
                                if (seg != null) {
                                    clickOnSeg(seg);
                                } else {
                                    BHBot.logger.error("No accept button for expedition team!");
                                    saveGameScreen("expedtion-no-accept", "errors");
                                    restart();
                                }

                                if (handleTeamMalformedWarning()) {
                                    BHBot.logger.error("Team incomplete, doing emergency restart..");
                                    restart();
                                    continue;
                                } else {
                                    state = State.Expedition;
                                    BHBot.logger.info(portalName + " portal initiated!");
                                    autoShrined = false;
                                    autoBossRuned = false;
                                }

                                if (handleGuildLeaveConfirm()) {
                                    restart();
                                    continue;
                                }
                            }
                            continue;
                        } else {
                            // do neither gvg nor invasion
                            seg = detectCue(cues.get("X"));
                            clickOnSeg(seg);
                            sleep(2 * SECOND);
                            continue;
                        }
                    } // badges

                    // Check worldBoss:
                    if ("w".equals(currentActivity)) {
                        timeLastEnergyCheck = Misc.getTime();
                        int energy = getEnergy();
                        globalEnergy = energy;
                        BHBot.logger.readout("Energy: " + energy + "%, required: >" + BHBot.settings.minEnergyPercentage + "%");

                        if (energy == -1) { // error
                            if (BHBot.scheduler.doWorldBossImmediately)
                                BHBot.scheduler.doWorldBossImmediately = false; // reset it
                            BHBot.scheduler.restoreIdleTime();


                            continue;
                        }

                        if (!BHBot.scheduler.doWorldBossImmediately && (energy <= BHBot.settings.minEnergyPercentage)) {

                            //if we have 1 resource and need 5 we don't need to check every 10 minutes, this increases the timer so we start checking again when we are one under the check limit
                            int energyDifference = BHBot.settings.minEnergyPercentage - energy; //difference between needed and current resource
                            if (energyDifference > 1) {
                                int increase = (energyDifference - 1) * 4;
                                ENERGY_CHECK_INTERVAL = increase * MINUTE; //add 4 minutes to the check interval for each energy % needed above 1
                            } else ENERGY_CHECK_INTERVAL = 10 * MINUTE; //if we only need 1 check every 10 minutes

                            sleep(SECOND);
                            continue;
                        } else {
                            // do the WorldBoss!
                            if (BHBot.scheduler.doWorldBossImmediately)
                                BHBot.scheduler.doWorldBossImmediately = false; // reset it

                            if (!checkWorldBossInput()) {
                                BHBot.logger.warn("Invalid world boss settings detected, World Boss will be skipped");
                                continue;
                            }

                            //configure activity runes
                            handleMinorRunes("w");

                            seg = detectCue(cues.get("WorldBoss"));
                            if (seg != null) {
                                clickOnSeg(seg);
                            } else {
                                BHBot.logger.error("World Boss button not found");
                                continue;
                            }

                            readScreen();
                            detectCharacterDialogAndHandleIt(); //clear dialogue

                            WorldBoss wbType = WorldBoss.fromLetter(BHBot.settings.worldBossSettings.get(0));
                            if (wbType == null) {
                                BHBot.logger.error("Unkwon World Boss type: " + BHBot.settings.worldBossSettings.get(0) + ". Disabling World Boss");
                                BHBot.settings.activitiesEnabled.remove("w");
                                restart();
                                continue;
                            }

                            int worldBossDifficulty = Integer.parseInt(BHBot.settings.worldBossSettings.get(1));
                            int worldBossTier = Integer.parseInt(BHBot.settings.worldBossSettings.get(2));
                            int worldBossTimer = BHBot.settings.worldBossTimer;

                            //new settings loading
                            String worldBossDifficultyText = worldBossDifficulty == 1 ? "Normal" : worldBossDifficulty == 2 ? "Hard" : "Heroic";

                            if (!BHBot.settings.worldBossSolo) {
                                BHBot.logger.info("Attempting " + worldBossDifficultyText + " T" + worldBossTier + " " + wbType.getName() + ". Lobby timeout is " + worldBossTimer + "s.");
                            } else {
                                BHBot.logger.info("Attempting " + worldBossDifficultyText + " T" + worldBossTier + " " + wbType.getName() + " Solo");
                            }

                            readScreen();
                            seg = detectCue(cues.get("BlueSummon"), SECOND);
                            if (seg != null) {
                                clickOnSeg(seg);
                            } else {
                                BHBot.logger.error("Impossible to find blue summon in world boss.");

                                String WBErrorScreen = saveGameScreen("wb-no-blue-summon", "errors");
                                if (BHBot.settings.enablePushover && BHBot.settings.poNotifyErrors) {
                                    if (WBErrorScreen != null) {
                                        sendPushOverMessage("World Boss error", "Impossible to find blue summon.", "siren", MessagePriority.NORMAL, new File(WBErrorScreen));
                                    } else {
                                        sendPushOverMessage("World Boss error", "Impossible to find blue summon.", "siren", MessagePriority.NORMAL, null);
                                    }
                                }

                                closePopupSecurely(cues.get("WorldBossTitle"), cues.get("X"));
                                continue;
                            }
                            readScreen(2 * SECOND); //wait for screen to stablise

                            //world boss type selection
                            if (!handleWorldBossSelection(wbType)){
                                BHBot.logger.error("Impossible to change select the desired World Boss. Restarting...");
                                restart();
                                continue;
                            }

//							sleep(SECOND); //more stabilising if we changed world boss type
                            readScreen(SECOND);
                            seg = detectCue(cues.get("LargeGreenSummon"), 2 * SECOND);
                            clickOnSeg(seg); //selected world boss

                            readScreen(SECOND);
                            seg = detectCue(cues.get("Private"), SECOND);
                            if (!BHBot.settings.worldBossSolo) {
                                if (seg != null) {
                                    BHBot.logger.info("Unchecking private lobby");
                                    clickOnSeg(seg);
                                }
                            } else {
                                if (seg == null) {
                                    BHBot.logger.info("Enabling private lobby for solo World Boss");
                                    sleep(500);
                                    clickInGame(340, 350);
                                    readScreen(500);
                                }
                            }

                            //world boss tier selection

                            int currentTier = detectWorldBossTier();
                            sleep(500);
                            if (currentTier != worldBossTier) {
                                BHBot.logger.info("T" + currentTier + " detected, changing to T" + worldBossTier);
                                sleep(500);
                                changeWorldBossTier(worldBossTier);
                            }

                            //world boss difficulty selection

                            int currentDifficulty = detectWorldBossDifficulty();
                            String currentDifficultyName = (currentDifficulty == 1 ? "Normal" : currentDifficulty == 2 ? "Hard" : "Heroic");
                            String settingsDifficultyName = (worldBossDifficulty == 1 ? "Normal" : worldBossDifficulty == 2 ? "Hard" : "Heroic");
                            if (currentDifficulty != worldBossDifficulty) {
                                BHBot.logger.info(currentDifficultyName + " detected, changing to " + settingsDifficultyName);
                                changeWorldBossDifficulty(worldBossDifficulty);
                            }

                            readScreen(SECOND);
                            seg = detectCue(cues.get("SmallGreenSummon"), SECOND * 2);
                            clickOnSeg(seg); //accept current settings

                            boolean insufficientEnergy = handleNotEnoughEnergyPopup(SECOND * 3, State.WorldBoss);
                            if (insufficientEnergy) {
                                continue;
                            }

                            BHBot.logger.info("Starting lobby");

                            /*
                             *
                             * this part gets messy as WB is much more dynamic and harder to automate with human players
                             * I've tried to introduce as many error catchers with restarts(); as possible to keep things running smoothly
                             *
                             */

                            //wait for lobby to fill with a timer
                            if (!BHBot.settings.worldBossSolo) {
                                for (int i = 0; i < worldBossTimer; i++) {
                                    sleep(SECOND);
                                    readScreen();

                                    switch (wbType.getLetter()) {
                                        case "o":  //shouldn't have this inside the loop but it doesn't work if its outside
                                            seg = detectCue(cues.get("Invite")); // 5th Invite button for Orlag
                                            break;
                                        case "n":
                                        case "3":
                                        case "b":
                                            seg = detectCue(cues.get("Invite3rd")); // 3rd Invite button for Nether, 3xt3rmin4tion & Brimstone
                                            break;
                                        case "m":
                                            seg = detectCue(cues.get("Invite4th")); // 4th Invite button for Melvin
                                            break;
                                    }

                                    if (seg != null) { //while the relevant invite button exists
                                        if (i != 0 && (i % 15) == 0) { //every 15 seconds
                                            int timeLeft = worldBossTimer - i;
                                            BHBot.logger.info("Waiting for full team. Time out in " + timeLeft + " seconds.");
                                        }
                                        if (i == (worldBossTimer - 1)) { //out of time
                                            if (BHBot.settings.dungeonOnTimeout) { //setting to run a dungeon if we cant fill a lobby
                                                BHBot.logger.info("Lobby timed out, running dungeon instead");
                                                closeWorldBoss();
                                                sleep(4 * SECOND); //make sure we're stable on the main screen
                                                BHBot.scheduler.doDungeonImmediately = true;
                                            } else {
                                                BHBot.logger.info("Lobby timed out, returning to main screen.");
                                                timeLastEnergyCheck -= 9 * MINUTE; // remove 9 minutes from the check time so we check again in a minute
                                                closeWorldBoss();
                                            }
                                        }
                                    } else {
                                        BHBot.logger.info("Lobby filled in " + i + " seconds!");
                                        i = worldBossTimer; // end the for loop

                                        //check that all players are ready
                                        BHBot.logger.info("Making sure everyones ready..");
                                        int j = 1;
                                        while (j != 20) { //ready check for 10 seconds
                                            seg = detectCue(cues.get("Unready"), 2 * SECOND); //this checks all 4 ready statuses
                                            readScreen();
                                            if (seg == null) {// no red X's found
                                                break;
                                            } else { //red X's found
                                                //BHBot.logger.info(Integer.toString(j));
                                                j++;
                                                sleep(500); //check every 500ms
                                            }
                                        }

                                        if (j >= 20) {
                                            BHBot.logger.error("Ready check not passed after 10 seconds, restarting");
                                            restart();
                                        }

                                        sleep(500);
                                        readScreen();
                                        MarvinSegment segStart = detectCue(cues.get("Start"), 5 * SECOND);
                                        if (segStart != null) {
                                            clickOnSeg(segStart); //start World Boss
                                            readScreen();
                                            seg = detectCue(cues.get("TeamNotFull"), 2 * SECOND); //check if we have the team not full screen an clear it
                                            if (seg != null) {
                                                sleep(2 * SECOND); //wait for animation to finish
                                                clickInGame(330, 360); //yesgreen cue has issues so we use XY to click on Yes
                                            }
                                            BHBot.logger.info(worldBossDifficultyText + " T" + worldBossTier + " " + wbType.getName() + " started!");
                                            state = State.WorldBoss;
                                            sleep(6 * SECOND); //long wait to make sure we are in the world boss dungeon

                                            readScreen();
                                            MarvinSegment segAutoOn = detectCue(cues.get("AutoOn"));
                                            if (segAutoOn == null) { // if state = worldboss but there's no auto button something went wrong, so restart
                                                BHBot.logger.info("World Boss started but no encounter detected, restarting");
                                                restart();
                                            }
                                        } else { //generic error / unknown action restart
                                            BHBot.logger.error("Something went wrong while attempting to start the World Boss, restarting");
                                            saveGameScreen("wb-no-start-button", "errors");
                                            restart();
                                        }

                                    }
                                }
                            } else {
                                readScreen();
                                MarvinSegment segStart = detectCue(cues.get("Start"), 2 * SECOND);
                                if (segStart != null) {
                                    clickOnSeg(segStart); //start World Boss
                                    sleep(2 * SECOND); //wait for dropdown animation to finish
                                    seg = detectCue(cues.get("YesGreen"), 2 * SECOND); //clear empty team prompt
                                    if (seg == null) {
                                        sleep(500);
                                        clickInGame(330, 360); //yesgreen cue has issues so we use pos to click on Yes as a backup
                                    } else {
                                        clickOnSeg(seg);
                                        clickInGame(330, 360); //click anyway this cue has issues
                                    }
                                    BHBot.logger.info(worldBossDifficultyText + " T" + worldBossTier + " " + wbType.getName() + " Solo started!");
                                    state = State.WorldBoss;
                                    continue;
                                }
                                continue;
                            }
                        }
                        continue;
                    } // World Boss

                    //bounties activity
                    if ("b".equals(currentActivity)) {
                        timeLastBountyCheck = Misc.getTime();

                        if (BHBot.scheduler.collectBountiesImmediately) {
                            BHBot.scheduler.collectBountiesImmediately = false; //disable collectImmediately again if its been activated
                        }
                        BHBot.logger.info("Checking for completed bounties");

                        clickInGame(130, 440);

                        seg = detectCue(cues.get("Bounties"), SECOND * 5);
                        if (seg != null) {
                            readScreen();
                            seg = detectCue(cues.get("Loot"), SECOND * 5, new Bounds(505, 245, 585, 275));
                            while (seg != null) {
                                clickOnSeg(seg);
                                seg = detectCue(cues.get("WeeklyRewards"), SECOND * 5, new Bounds(190, 100, 615, 400));
                                if (seg != null) {
                                    seg = detectCue(cues.get("X"), 5 * SECOND);
                                    if (seg != null) {
                                        if ((BHBot.settings.screenshots.contains("b"))) {
                                            saveGameScreen("bounty-loot", "rewards");
                                        }
                                        clickOnSeg(seg);
                                        BHBot.logger.info("Collected bounties");
                                        sleep(SECOND * 2);
                                    } else {
                                        BHBot.logger.error("Error when collecting bounty items, restarting...");
                                        saveGameScreen("bounties-error-collect", "errors");
                                        restart();
                                    }
                                } else {
                                    BHBot.logger.error("Error finding bounty item dialog, restarting...");
                                    saveGameScreen("bounties-error-item");
                                    restart();
                                }

                                seg = detectCue(cues.get("Loot"), SECOND * 5, new Bounds(505, 245, 585, 275));
                            }

                            seg = detectCue(cues.get("X"), 5 * SECOND);
                            if (seg != null) {
                                clickOnSeg(seg);
                                readScreen();
                            } else {
                                BHBot.logger.error("Impossible to close the bounties dialog, restarting...");
                                saveGameScreen("bounties-error-closing");
                                restart();
                            }
                        } else {
                            BHBot.logger.error("Impossible to detect the Bounties dialog, restarting...");
                            saveGameScreen("bounties-error-dialog");
                            restart();
                        }
                        readScreen(SECOND * 2);
                        continue;
                    }

                    //fishing baits
                    if ("a".equals(currentActivity)) {
                        timeLastFishingBaitsCheck = Misc.getTime();

                        if (BHBot.scheduler.doFishingBaitsImmediately) {
                            BHBot.scheduler.doFishingBaitsImmediately = false; //disable collectImmediately again if its been activated
                        }

                        handleFishingBaits();
                        continue;
                    }

                    //fishing
                    if ("f".equals(currentActivity)) {
                        timeLastFishingCheck = Misc.getTime();

                        if (BHBot.scheduler.doFishingImmediately) {
                            BHBot.scheduler.doFishingImmediately = false; //disable collectImmediately again if its been activated
                        }

                        if ((Misc.getTime() - timeLastFishingBaitsCheck) > DAY) { //if we haven't collected bait today we need to do that first
                            handleFishingBaits();
                        }

                        boolean botPresent = new File("bh-fisher.jar").exists();
                        if (!botPresent) {
                            BHBot.logger.warn("bh-fisher.jar not found in root directory, fishing disabled.");
                            BHBot.logger.warn("For information on configuring fishing check the wiki page on github");
                            BHBot.settings.activitiesEnabled.remove("f");
                            return;
                        }

                        handleFishing();
                        continue;
                    }

                } // main screen processing
            } catch (Exception e) {
                if (e instanceof org.openqa.selenium.WebDriverException && e.getMessage().startsWith("chrome not reachable")) {
                    // this happens when user manually closes the Chrome window, for example
                    BHBot.logger.error("Error: chrome is not reachable! Restarting...", e);
                    restart();
                    continue;
                } else if (e instanceof java.awt.image.RasterFormatException) {
                    // not sure in what cases this happen, but it happens
                    BHBot.logger.error("Error: RasterFormatException. Attempting to re-align the window...", e);
                    sleep(500);
                    scrollGameIntoView();
                    sleep(500);
                    try {
                        readScreen();
                    } catch (Exception e2) {
                        BHBot.logger.error("Error: re-alignment failed(" + e2.getMessage() + "). Restarting...");
                        restart();
                        continue;
                    }
                    BHBot.logger.info("Realignment seems to have worked.");
                    continue;
                } else if (e instanceof org.openqa.selenium.StaleElementReferenceException) {
                    // this is a rare error, however it happens. See this for more info:
                    // http://www.seleniumhq.org/exceptions/stale_element_reference.jsp
                    BHBot.logger.error("Error: StaleElementReferenceException. Restarting...", e);
                    restart();
                    continue;
                } else if (e instanceof com.assertthat.selenium_shutterbug.utils.web.ElementOutsideViewportException) {
                    BHBot.logger.info("Error: ElementOutsideViewportException. Ignoring...");
                    //added this 1 second delay as attempting ads often triggers this
                    //will trigger the restart in the if statement below after 30 seconds
                    sleep(SECOND);
                    // we must not call 'continue' here, because this error could be a loop error, this is why we need to increase numConsecutiveException bellow in the code!
                } else {
                    // unknown error!
                    BHBot.logger.error("Unmanaged exception in main run loop", e);
                }

                numConsecutiveException++;
                if (numConsecutiveException > MAX_CONSECUTIVE_EXCEPTIONS) {
                    numConsecutiveException = 0; // reset it
                    BHBot.logger.warn("Problem detected: number of consecutive exceptions is higher than " + MAX_CONSECUTIVE_EXCEPTIONS + ". This probably means we're caught in a loop. Restarting...");
                    restart();
                    continue;
                }

                BHBot.scheduler.restoreIdleTime();

                continue;
            }

            // well, we got through all the checks. Means that nothing much has happened. So lets sleep for a few seconds in order to not make processing too heavy...
            numConsecutiveException = 0; // reset exception counter
            BHBot.scheduler.restoreIdleTime(); // revert changes to idle time
            if (finished) break; // skip sleeping if finished flag has been set!
            sleep(SECOND);
        } // main while loop

        BHBot.logger.info("Stopping main thread...");
        driver.close();
        driver.quit();
        BHBot.logger.info("Main thread stopped.");
    }

    private String activitySelector() {

        if (BHBot.scheduler.doRaidImmediately) {
            return "r";
        } else if (BHBot.scheduler.doDungeonImmediately) {
            return "d";
        } else if (BHBot.scheduler.doWorldBossImmediately) {
            return "w";
        } else if (BHBot.scheduler.doTrialsImmediately) {
            return "t";
        } else if (BHBot.scheduler.doGauntletImmediately) {
            return "g";
        } else if (BHBot.scheduler.doPVPImmediately) {
            return "p";
        } else if (BHBot.scheduler.doInvasionImmediately) {
            return "i";
        } else if (BHBot.scheduler.doGVGImmediately) {
            return "v";
        } else if (BHBot.scheduler.doExpeditionImmediately) {
            return "e";
        } else if (BHBot.scheduler.collectBountiesImmediately) {
            return "b";
        } else if (BHBot.scheduler.doFishingBaitsImmediately) {
            return "a";
        } else if (BHBot.scheduler.doFishingImmediately) {
            return "f";
        }

        if (BHBot.settings.activitiesEnabled.isEmpty()) {
            return null;
        } else {

            String activity;

            if (!BHBot.settings.activitiesRoundRobin) {
                activitysIterator = BHBot.settings.activitiesEnabled.iterator(); //reset the iterator
            }

            //loop through in defined order, if we match activity and timer we select the activity
            while (activitysIterator.hasNext()) {

                try {
                    activity = activitysIterator.next(); //set iterator to string for .equals()
                } catch (java.util.ConcurrentModificationException e) {
                    activitysIterator = BHBot.settings.activitiesEnabled.iterator();
                    activity = activitysIterator.next();
                }

                if (activity.equals("r") && ((Misc.getTime() - timeLastShardsCheck) > (long) (15 * MINUTE))) {
                    return "r";
                } else if ("d".equals(activity) && ((Misc.getTime() - timeLastEnergyCheck) > ENERGY_CHECK_INTERVAL)) {
                    return "d";
                } else if ("w".equals(activity) && ((Misc.getTime() - timeLastEnergyCheck) > ENERGY_CHECK_INTERVAL)) {
                    return "w";
                } else if ("t".equals(activity) && ((Misc.getTime() - timeLastTrialsTokensCheck) > TOKENS_CHECK_INTERVAL)) {
                    return "t";
                } else if ("g".equals(activity) && ((Misc.getTime() - timeLastGauntletTokensCheck) > TOKENS_CHECK_INTERVAL)) {
                    return "g";
                } else if ("p".equals(activity) && ((Misc.getTime() - timeLastTicketsCheck) > TICKETS_CHECK_INTERVAL)) {
                    return "p";
                } else if ("i".equals(activity) && ((Misc.getTime() - timeLastInvBadgesCheck) > BADGES_CHECK_INTERVAL)) {
                    return "i";
                } else if ("v".equals(activity) && ((Misc.getTime() - timeLastGVGBadgesCheck) > BADGES_CHECK_INTERVAL)) {
                    return "v";
                } else if ("e".equals(activity) && ((Misc.getTime() - timeLastExpBadgesCheck) > BADGES_CHECK_INTERVAL)) {
                    return "e";
                } else if ("b".equals(activity) && ((Misc.getTime() - timeLastBountyCheck) > (long) HOUR)) {
                    return "b";
                } else if ("a".equals(activity) && ((Misc.getTime() - timeLastFishingBaitsCheck) > (long) DAY)) {
                    return "a";
                } else if ("f".equals(activity) && ((Misc.getTime() - timeLastFishingCheck) > (long) DAY)) {
                    return "f";
                }
            }

            // If we reach this point activityIterator.hasNext() is false
            if (BHBot.settings.activitiesRoundRobin) {
                activitysIterator = BHBot.settings.activitiesEnabled.iterator();
            }

            return null; //return null if no matches
        }
    }

    private boolean openSettings(@SuppressWarnings("SameParameterValue") int delay) {
        readScreen();

        MarvinSegment seg = detectCue(cues.get("SettingsGear"));
        if (seg != null) {
            clickOnSeg(seg);
            readScreen(delay);
            seg = detectCue(cues.get("Settings"), SECOND * 3);
            return seg != null;
        } else {
            BHBot.logger.error("Impossible to find the settings button!");
            saveGameScreen("open-settings-no-btn", "errors");
            return false;
        }
    }

    boolean checkShrineSettings(boolean ignoreBoss, boolean ignoreShrines) {
        //open settings
        int ignoreBossCnt = 0;
        int ignoreShrineCnt = 0;

        if (openSettings(SECOND)) {
            if (ignoreBoss) {
                while (detectCue(cues.get("IgnoreBoss"), SECOND) != null) {
                    BHBot.logger.debug("Enabling Ignore Boss");
                    clickInGame(194, 366);
                    readScreen(500);

                    if (ignoreBossCnt++ > 10) {
                        BHBot.logger.error("Impossible to enable Ignore Boss");
                        return false;
                    }
                }
                ignoreBossSetting = true;
                BHBot.logger.debug("Ignore Boss Enabled");
            } else {
                while (detectCue(cues.get("IgnoreBoss"), SECOND) == null) {
                    BHBot.logger.debug("Disabling Ignore Boss");
                    clickInGame(194, 366);
                    readScreen(500);

                    if (ignoreBossCnt++ > 10) {
                        BHBot.logger.error("Impossible to Disable Ignore Boss");
                        return false;
                    }
                }
                ignoreBossSetting = false;
                BHBot.logger.debug("Ignore Boss Disabled");
            }

            if (ignoreShrines) {
                while (detectCue(cues.get("IgnoreShrines"), SECOND) != null) {
                    BHBot.logger.debug("Enabling Ignore Shrine");
                    clickInGame(194, 402);
                    readScreen(500);

                    if (ignoreShrineCnt++ > 10) {
                        BHBot.logger.error("Impossible to enable Ignore Shrines");
                        return false;
                    }
                }
                ignoreShrinesSetting = true;
                BHBot.logger.debug("Ignore Shrine Enabled");
            } else {
                while (detectCue(cues.get("IgnoreShrines"), SECOND) == null) {
                    BHBot.logger.debug("Disabling Ignore Shrine");
                    clickInGame(194, 402);
                    readScreen(500);

                    if (ignoreShrineCnt++ > 10) {
                        BHBot.logger.error("Impossible to disable Ignore Shrines");
                        return false;
                    }
                }
                ignoreShrinesSetting = false;
                BHBot.logger.debug("Ignore Shrine Disabled");
            }

            readScreen(SECOND);

            closePopupSecurely(cues.get("Settings"), new Cue(cues.get("X"), new Bounds(608, 39, 711, 131)));

            return true;
        } else {
            BHBot.logger.warn("Impossible to open settings menu!");
            return false;
        }
    }

    private boolean openRunesMenu() {
        // Open character menu
        clickInGame(55, 465);

        MarvinSegment seg = detectCue(cues.get("Runes"), 15 * SECOND);
        if (seg == null) {
            BHBot.logger.warn("Error: unable to detect runes button! Skipping...");
            return true;
        }

        clickOnSeg(seg);
        sleep(500); //sleep for window animation (15s below was crashing the bot, not sure why

        seg = detectCue(cues.get("RunesLayout"), 15 * SECOND);
        if (seg == null) {
            BHBot.logger.warn("Error: unable to detect rune layout! Skipping...");
            seg = detectCue(cues.get("X"), 5 * SECOND);
            if (seg != null) {
                clickOnSeg(seg);
            }
            return true;
        }

        return false;
    }

    boolean detectEquippedMinorRunes(boolean enterRunesMenu, boolean exitRunesMenu) {

        if (enterRunesMenu && openRunesMenu())
            return false;

        // determine equipped runes
        leftMinorRune = null;
        rightMinorRune = null;
        readScreen();
        for (MinorRune rune : MinorRune.values()) {
            Cue runeCue = rune.getRuneCue();

            // left rune
            MarvinSegment seg = detectCue(runeCue, 0, new Bounds(230, 245, 320, 330));
            if (seg != null)
                leftMinorRune = rune;

            // right rune
            seg = detectCue(runeCue, 0, new Bounds(480, 245, 565, 330));
            if (seg != null)
                rightMinorRune = rune;

        }

        if (exitRunesMenu) {
            sleep(500);
            closePopupSecurely(cues.get("RunesLayout"), cues.get("X"));
            sleep(500);
            closePopupSecurely(cues.get("StripSelectorButton"), cues.get("X"));
        }

        boolean success = true;
        if (leftMinorRune == null) {
            BHBot.logger.warn("Error: Unable to detect left minor rune!");
            success = false;
        } else {
            BHBot.logger.debug(leftMinorRune + " equipped in left slot.");
        }
        if (rightMinorRune == null) {
            BHBot.logger.warn("Error: Unable to detect right minor rune!");
            success = false;
        } else {
            BHBot.logger.debug(rightMinorRune + " equipped in right slot.");
        }

        return success;
    }

    /**
     * This form opens only seldom (haven't figured out what triggers it exactly - perhaps some cookie expired?). We need to handle it!
     */
    private void detectSignInFormAndHandleIt() {
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
        weUsername.sendKeys(BHBot.settings.username);

        WebElement wePassword;
        try {
            wePassword = driver.findElement(By.xpath("//*[@id='password']"));
        } catch (NoSuchElementException e) {
            return;
        }
        wePassword.clear();
        wePassword.sendKeys(BHBot.settings.password);

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
    private void detectLoginFormAndHandleIt() {
        readScreen();

        MarvinSegment seg = detectCue(cues.get("Login"));

        if (seg == null)
            return;

        // open login popup window:
        jsExecutor.executeScript("active_user.activateInlineLogin(); return false;"); // I found this code within page source itself (it gets triggered upon clicking on some button)

        sleep(5000); // if we don't sleep enough, login form may still be loading and code bellow will not get executed!

        // fill in username:
        WebElement weUsername;
        try {
            weUsername = driver.findElement(By.cssSelector("body#play > div#lightbox > div#lbContent > div#kongregate_lightbox_wrapper > div#lightbox_form > div#lightboxlogin > div#new_session_shared_form > form > dl > dd > input#username"));
        } catch (NoSuchElementException e) {
            BHBot.logger.warn("Problem: username field not found in the login form (perhaps it was not loaded yet?)!");
            return;
        }
        weUsername.clear();
        weUsername.sendKeys(BHBot.settings.username);
        BHBot.logger.info("Username entered into the login form.");

        WebElement wePassword;
        try {
            wePassword = driver.findElement(By.cssSelector("body#play > div#lightbox > div#lbContent > div#kongregate_lightbox_wrapper > div#lightbox_form > div#lightboxlogin > div#new_session_shared_form > form > dl > dd > input#password"));
        } catch (NoSuchElementException e) {
            BHBot.logger.warn("Problem: password field not found in the login form (perhaps it was not loaded yet?)!");
            return;
        }
        wePassword.clear();
        wePassword.sendKeys(BHBot.settings.password);
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

    /**
     * This will handle dialog that open up when you encounter a boss for the first time, for example, or open a raid window or trials window for the first time, etc.
     */
    private void detectCharacterDialogAndHandleIt() {
        final Color cuec1 = new Color(238, 241, 249); // white
        final Color cuec2 = new Color(82, 90, 98); // gray

        MarvinSegment right;
        MarvinSegment left;
        int steps = 0;

        while (true) {
            readScreen();

            right = detectCue(cues.get("DialogRight"));
            left = detectCue(cues.get("DialogLeft"));

            // double check right-side dialog cue:
            if (right != null) {
                if (
                        !(new Color(img.getRGB(right.x2 + 1, right.y1 + 24))).equals(cuec1) ||
                                !(new Color(img.getRGB(right.x2 + 4, right.y1 + 24))).equals(cuec2)
                )
                    right = null;
            }

            // double check left-side dialog cue:
            if (left != null) {
                if (
                        !(new Color(img.getRGB(left.x1 - 1, left.y1 + 24))).equals(cuec1) ||
                                !(new Color(img.getRGB(left.x1 - 4, left.y1 + 24))).equals(cuec2)
                )
                    left = null;
            }

            if (left == null && right == null)
                break; // dialog not detected

            // click to dismiss/progress it:
            if (left != null)
                clickOnSeg(left);
            else
                clickOnSeg(right);

            sleep(2 * SECOND);
            steps++;
        }

        if (steps > 0)
            BHBot.logger.info("Character dialog dismissed.");
    }

    private BufferedImage takeScreenshot(boolean ofGame) {
        try {
            if (ofGame)
                return Shutterbug.shootElement(driver, game).getImage();
            else
                return Shutterbug.shootPage(driver).getImage();
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            // sometimes the game element is not available, if this happen we just return an empty image
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        }
    }

    void readScreen() {
        readScreen(true);
    }

    /**
     * @param game if true, then screenshot of a WebElement will be taken that contains the flash game. If false, then simply a screenshot of a browser will be taken.
     */
    private void readScreen(boolean game) {
        readScreen(0, game);
    }

    /**
     * First sleeps 'wait' milliseconds and then reads the screen. It's a handy utility method that does two things in one command.
     */
    private void readScreen(int wait) {
        readScreen(wait, true);
    }

    /**
     * @param wait first sleeps 'wait' milliseconds and then reads the screen. It's a handy utility method that does two things in one command.
     * @param game if true, then screenshot of a WebElement will be taken that contains the flash game. If false, then simply a screenshot of a browser will be taken.
     */
    private void readScreen(int wait, boolean game) {
        if (wait != 0)
            sleep(wait);
        img = takeScreenshot(game);
    }

    /**
     * This method is ment to be used for development purpose. In some situations you want to "fake" the readScreen result
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

    private MarvinSegment detectCue(Cue cue) {
        return detectCue(cue, 0, true);
    }

    private MarvinSegment detectCue(Cue cue, int timeout, Bounds bounds) {
        return detectCue(new Cue(cue, bounds), timeout, true);
    }

    private MarvinSegment detectCue(Cue cue, int timeout) {
        return detectCue(cue, timeout, true);
    }

    // Cue detection based on String
    private MarvinSegment detectCue(String cueName) {
        return detectCue(cues.get(cueName), 0, true);
    }

    private MarvinSegment detectCue(String cueName, int timeout, Bounds bounds) {
        return detectCue(new Cue(cues.get(cueName), bounds), timeout, true);
    }

    private MarvinSegment detectCue(String cueName, int timeout) {
        return detectCue(cues.get(cueName), timeout, true);
    }

    /**
     * Will try (and retry) to detect cue from image until timeout is reached. May return null if cue has not been detected within given 'timeout' time. If 'timeout' is 0,
     * then it will attempt at cue detection only once and return the result immediately.
     */
    private MarvinSegment detectCue(Cue cue, int timeout, @SuppressWarnings("SameParameterValue") boolean game) {
        long timer = Misc.getTime();
        MarvinSegment seg = findSubimage(img, cue);

        while (seg == null) {
            if ((Misc.getTime() - timer) >= timeout)
                break;
            readScreen(500, game);
            seg = findSubimage(img, cue);
        }

        if (seg == null && timeout > 0) { // segment not detected when expected (timeout>0 tells us that we probably expect to find certain cue, since we are waiting for it to appear)
            if (handlePM()) { // perhaps PM window has opened and that is why we couldn't detect the cue?
                sleep(3 * SECOND);
                readScreen(game);
                seg = findSubimage(img, cue); // re-read the original segment
            }
        }

        return seg;
    }

    private int getSegCenterX(MarvinSegment seg) {
        return (seg.x1 + seg.x2) / 2;
    }

    private int getSegCenterY(MarvinSegment seg) {
        return (seg.y1 + seg.y2) / 2;
    }

    /**
     * Moves mouse to position (0,0) in the 'game' element (so that it doesn't trigger any highlight popups or similar
     */
    private void moveMouseAway() {
        try {
            Actions act = new Actions(driver);
            act.moveToElement(game, 0, 0);
            act.perform();
        } catch (Exception e) {
            // do nothing
        }
    }

    /**
     * Performs a mouse click on the center of the given segment
     */
    private void clickOnSeg(MarvinSegment seg) {
        Actions act = new Actions(driver);
        act.moveToElement(game, getSegCenterX(seg), getSegCenterY(seg));
        act.click();
        act.moveToElement(game, 0, 0); // so that the mouse doesn't stay on the button, for example. Or else button will be highlighted and cue won't get detected!
        act.perform();
    }

    private void clickInGame(int x, int y) {
        Actions act = new Actions(driver);
        act.moveToElement(game, x, y);
        act.click();
        act.moveToElement(game, 0, 0); // so that the mouse doesn't stay on the button, for example. Or else button will be highlighted and cue won't get detected!
        act.perform();
    }

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            BHBot.logger.error("Error while attempting to sleep", e);
        }
    }

    /**
     * Returns amount of energy in percent (0-100). Returns -1 in case it cannot read energy for some reason.
     */
    private int getEnergy() {
        MarvinSegment seg;

        seg = detectCue(cues.get("EnergyBar"));

        if (seg == null) // this should probably not happen
            return -1;

        int left = seg.x2 + 1;
        int top = seg.y1 + 6;

        final Color full = new Color(136, 197, 44);
        //final Color limit = new Color(87, 133, 21);
        //final Color empty = new Color(49, 50, 51);

        int value = 0;

        // energy bar is 80 pixels long (however last two pixels will have "medium" color and not full color (it's so due to shading))
        for (int i = 0; i < 78; i++) {
            value = i;
            Color col = new Color(img.getRGB(left + i, top));

            if (!col.equals(full))
                break;
        }

        return Math.round(value * (100 / 77.0f)); // scale it to interval [0..100]
    }

	/*public MarvinSegment detectCue(Cue cue, Bounds bounds) {
		return detectCue(new Cue(cue, bounds), 0, true);
	}*/

    /**
     * Returns number of tickets left (for PvP) in interval [0..10]. Returns -1 in case it cannot read number of tickets for some reason.
     */
    private int getTickets() {
        MarvinSegment seg;

        seg = detectCue(cues.get("TicketBar"));

        if (seg == null) // this should probably not happen
            return -1;

        int left = seg.x2 + 1;
        int top = seg.y1 + 6;

        final Color full = new Color(226, 42, 81);

        int value = 0;
        int maxTickets = BHBot.settings.maxTickets;

        // ticket bar is 80 pixels long (however last two pixels will have "medium" color and not full color (it's so due to shading))
        for (int i = 0; i < 78; i++) {
            value = i;
            Color col = new Color(img.getRGB(left + i, top));

            if (!col.equals(full))
                break;
        }

        value = value + 2; //add the last 2 pixels to get an accurate count
//		BHBot.logger.info("Pre-rounded stat = " + Float.toString(value * (maxTickets / 77.0f)));
        return Math.round(value * (maxTickets / 77.0f)); // scale it to interval [0..10]
    }

    /**
     * Returns number of shards that we have. Works only if raid popup is open. Returns -1 in case it cannot read number of shards for some reason.
     */
    private int getShards() {
        MarvinSegment seg;

        seg = detectCue(cues.get("RaidPopup"));

        if (seg == null) // this should probably not happen
            return -1;

        int left = seg.x2 + 1;
        int top = seg.y1 + 9;

        final Color full = new Color(199, 79, 175);

        int value = 0;
        int maxShards = BHBot.settings.maxShards;

        for (int i = 0; i < 76; i++) {
            value = i;
            Color col = new Color(img.getRGB(left + i, top));

            if (!col.equals(full))
                break;
        }

        value = value + 2; //add the last 2 pixels to get an accurate count
//		BHBot.logger.info("Pre-rounded stat = " + Float.toString(value * (maxShards / 77.0f)));
        return Math.round(value * (maxShards / 75.0f)); // round to nearest whole number
    }

    /**
     * Returns number of tokens we have. Works only if trials/gauntlet window is open. Returns -1 in case it cannot read number of tokens for some reason.
     */
    private int getTokens() {
        MarvinSegment seg;

        seg = detectCue(cues.get("TokenBar"));

        if (seg == null) // this should probably not happen
            return -1;

        int left = seg.x2;
        int top = seg.y1 + 6;

        final Color full = new Color(17, 208, 226);

        int value = 0;
        int maxTokens = BHBot.settings.maxTokens;

        // tokens bar is 78 pixels wide (however last two pixels will have "medium" color and not full color (it's so due to shading))
        for (int i = 0; i < 76; i++) {
            value = i + 1;
            Color col = new Color(img.getRGB(left + i, top));

            if (!col.equals(full))
                break;
        }

//		BHBot.logger.info("Pre-rounded stat = " + Float.toString(value * (maxTokens / 77.0f)));
        return Math.round(value * (maxTokens / 76.0f)); // scale it to interval [0..10]
    }

    /**
     * Returns number of badges we have. Works only if GVG window is open. Returns -1 in case it cannot read number of badges for some reason.
     */
    private int getBadges() {
        MarvinSegment seg;

        seg = detectCue(cues.get("BadgeBar"));

        if (seg == null) // this should probably not happen
            return -1;

        int left = seg.x2 + 1;
        int top = seg.y1 + 6;

        final Color full = new Color(17, 208, 226);

        int value = 0;
        int maxBadges = BHBot.settings.maxBadges;

        // badges bar is 78 pixels wide (however last two pixels will have "medium" color and not full color (it's so due to shading))
        for (int i = 0; i < 76; i++) {
            value = i;
            Color col = new Color(img.getRGB(left + i, top));

            if (!col.equals(full))
                break;
        }

        value = value + 2; //add the last 2 pixels to get an accurate count
//		BHBot.logger.info("Pre-rounded stat = " + Float.toString(value * (maxBadges / 77.0f)));
        return Math.round(value * (maxBadges / 75.0f)); // scale it to interval [0..10]
    }

    /**
     * Processes any kind of dungeon: <br>
     * - normal dungeon <br>
     * - raid <br>
     * - trial <br>
     * - gauntlet <br>
     */
    private void processDungeon() {
        MarvinSegment seg;
        readScreen();

        if (!startTimeCheck) {
            activityStartTime = TimeUnit.MILLISECONDS.toSeconds(Misc.getTime());
            BHBot.logger.debug("Start time: " + activityStartTime);
            outOfEncounterTimestamp = TimeUnit.MILLISECONDS.toSeconds(Misc.getTime());
            inEncounterTimestamp = TimeUnit.MILLISECONDS.toSeconds(Misc.getTime());
            startTimeCheck = true;
            combatIdleChecker = true;
            motionChecker = true;
            stationaryCounter = 0;
            stationary = false;
            movement2 = img; //so we don't return error on our first movement check
        }

        activityDuration = (TimeUnit.MILLISECONDS.toSeconds(Misc.getTime()) - activityStartTime);

        if (state == State.Invasion) {
            //we check the invasion level bounds, if it is static for 30s we stop resetting idle timer
            motionChecker(1.0, 356, 445, 21, 40, 30);
            if (!stationary) {
                BHBot.scheduler.resetIdleTime(true);
            }
        }

        //We use guild button visibility to determine whether we are in an encounter or not
        MarvinSegment guildButtonSeg = detectCue(cues.get("GuildButton"));
        if (guildButtonSeg != null) {
            outOfEncounterTimestamp = TimeUnit.MILLISECONDS.toSeconds(Misc.getTime());
            if (combatIdleChecker) {
                BHBot.logger.trace("Updating idle time (Out of combat)");
                BHBot.scheduler.resetIdleTime(true);
                combatIdleChecker = false;
            }
        } else {
            inEncounterTimestamp = TimeUnit.MILLISECONDS.toSeconds(Misc.getTime());
            if (!combatIdleChecker) {
                BHBot.logger.trace("Updating idle time (In combat)");
                BHBot.scheduler.resetIdleTime(true);
                combatIdleChecker = true;
            }
        }

        if ((outOfEncounterTimestamp - inEncounterTimestamp) > 0) {
            BHBot.logger.debug("Seconds since last encounter: " + (outOfEncounterTimestamp - inEncounterTimestamp));
        }

        /*
         * autoRune Code
         */

        handleAutoBossRune();

        /*
         * autoShrine Code
         */

        handleAutoShrine();

        /*
         * autoRevive code
         */

        //trials/raid revive code + auto-off check
        seg = detectCue(cues.get("AutoOff"));
        if (seg != null) {
            handleAutoRevive();
        }

        /*
         * autoBribe code
         */

        // check for persuasions:
        seg = detectCue(cues.get("Persuade"));
        if (seg != null) {
            handleFamiliarEncounter();
        }

        /*
         * Skeleton key code
         */

        seg = detectCue(cues.get("SkeletonTreasure"));
        if (seg != null) {
            if (handleSkeletonKey()) {
                restart();
            }
        }

        // check for merchant's offer (and decline it):
        seg = detectCue(cues.get("Merchant"));
        if (seg != null) {
            seg = detectCue(cues.get("Decline"), 5 * SECOND);
            clickOnSeg(seg);

            readScreen(SECOND);
            seg = detectCue(cues.get("YesGreen"), 5 * SECOND);
            clickOnSeg(seg);
            return;
        }

        /*
         * Check if we're done (victory in PVP mode) - this may also close local fights in dungeons, this is why we check if state is State.PVP and react only to that one.
         * There were some crashing and clicking on SHOP problems with this one in dungeons and raids (and possibly elsewhere).
         * Hence I fixed it by checking if state==State.PVP.
         */
        if (state == State.PVP) {
            readScreen();
            seg = detectCue(cues.get("VictoryPopup"));
            if (seg != null) {

                handleVictory();
                counters.get(state).increaseVictories();
                BHBot.logger.info(state.getName() + " #" + counters.get(state).getTotal() + " completed. Result: Victory");
                BHBot.logger.stats(state.getName() + " " + counters.get(state).successRateDesc());

                closePopupSecurely(cues.get("VictoryPopup"), cues.get("CloseGreen")); // ignore failure

                // close the PVP window, in case it is open:
                readScreen(2 * SECOND);

                seg = detectCue(cues.get("PVPWindow"), 5 * SECOND);
                if (seg != null)
                    closePopupSecurely(cues.get("PVPWindow"), cues.get("X")); // ignore failure
                sleep(SECOND);
                resetAppropriateTimers();
                if (state == State.PVP) dressUp(BHBot.settings.pvpstrip);
                if (state == State.GVG) dressUp(BHBot.settings.gvgstrip);
                state = State.Main; // reset state
                return;
            }
        }

        // check for any character dialog:
        /* This is nearly half of the processing time of proccessDungeon(); so trying to minimize its usage */
        if (state == State.Dungeon || state == State.Raid) {
            detectCharacterDialogAndHandleIt();
        }

        // check if we're done (cleared):
        seg = detectCue(cues.get("Cleared"));
        if (seg != null) {
            counters.get(state).increaseVictories();
            handleSuccessTreshold(state);

            BHBot.logger.info(state.getName() + " #" + counters.get(state).getTotal() + " completed. Result: Victory");
            BHBot.logger.stats(state.getName() + " " + counters.get(state).successRateDesc());

            closePopupSecurely(cues.get("Cleared"), cues.get("YesGreen"));

            // close the raid/dungeon/trials/gauntlet window:
            readScreen(2 * SECOND); //we need a delay so we don't detect the button before the window has finished sliding in
            seg = detectCue(cues.get("X"), 5 * SECOND);
            if (seg != null)
                clickOnSeg(seg);
            else BHBot.logger.warn("Error #1: unable to find 'X' button to close raid/dungeon/trials/gauntlet window. Ignoring...");

            if (state == State.Expedition) {

                readScreen(2 * SECOND);

                // Close Portal Map after expedition
                readScreen(2 * SECOND);
                seg = detectCue(cues.get("X"));
                clickOnSeg(seg);
                sleep(SECOND);

                // close Expedition window after Expedition
                readScreen(2 * SECOND);
                seg = detectCue(cues.get("X"));
                clickOnSeg(seg);
                sleep(SECOND);
            }
            if (state == State.Raid) {
                long runMillis = Misc.getTime() - activityStartTime * 1000; //get elapsed time in milliseconds
                String runtime = String.format("%01dm%02ds", //format to mss
                        TimeUnit.MILLISECONDS.toMinutes(runMillis),
                        TimeUnit.MILLISECONDS.toSeconds(runMillis) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(runMillis)));
                runMillisAvg += runMillis; //on success add runtime to runMillisAvg
                String runtimeAvg = String.format("%01dm%02ds", //format to mss
                        TimeUnit.MILLISECONDS.toMinutes(runMillisAvg / counters.get(State.Raid).getVictories()), //then we divide runMillisavg by completed raids to get average time
                        TimeUnit.MILLISECONDS.toSeconds(runMillisAvg / counters.get(State.Raid).getVictories()) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(runMillisAvg / counters.get(State.Raid).getVictories())));
                BHBot.logger.stats("Run time: " + runtime + ". Average: " + runtimeAvg + ".");
            }

            resetAppropriateTimers();
            resetRevives();
            if (state == State.PVP) dressUp(BHBot.settings.pvpstrip);
            if (state == State.GVG) dressUp(BHBot.settings.gvgstrip);
            state = State.Main; // reset state
            return;
        }

        // check if we're done (defeat):
        seg = detectCue(cues.get("Defeat"));
        if (seg != null) {
            if (state == State.Invasion) {
                readScreen();

                //we read and store the Invasion level to return
                MarvinImage subm = new MarvinImage(img.getSubimage(375, 20, 55, 20));
                makeImageBlackWhite(subm, new Color(25, 25, 25), new Color(255, 255, 255), 64);
                BufferedImage subimagetestbw = subm.getBufferedImage();
                int num = readNumFromImg(subimagetestbw, "small", new HashSet<>());

                counters.get(state).increaseDefeats();
                BHBot.logger.info(state.getName() + " #" + counters.get(state).getTotal() + " completed. Level reached: " + num);
            } else {
                counters.get(state).increaseDefeats();
                BHBot.logger.warn(state.getName() + " #" + counters.get(state).getTotal() + " completed. Result: Defeat.");
                BHBot.logger.stats(state.getName() + " " + counters.get(state).successRateDesc());
            }

            boolean closed = false;
            // close button in dungeons is blue, in gauntlet it is green:
            seg = detectCue(cues.get("Close"), 2 * SECOND);
            if (seg != null) {
                clickOnSeg(seg);
                closed = true;
            }

            // close button in gauntlet:
            seg = detectCue(cues.get("CloseGreen"), 2 * SECOND);
            if (seg != null) {
                clickOnSeg(seg);
                closed = true;
            }

            if (!closed) {
                BHBot.logger.warn("Problem: 'Defeat' popup detected but no 'Close' button detected. Ignoring...");
                if (state == State.PVP) dressUp(BHBot.settings.pvpstrip);
                if (state == State.GVG) dressUp(BHBot.settings.gvgstrip);
                return;
            }

            // close the raid/dungeon/trials/gauntlet window:
            readScreen(2 * SECOND);
            detectCue(cues.get("X"), 5 * SECOND);
            sleep(SECOND); // because the popup window may still be sliding down so the "X" button is changing position and we must wait for it to stabilize
            seg = detectCue(cues.get("X"), SECOND);
            if (seg != null)
                clickOnSeg(seg);
            else
                BHBot.logger.warn("Error #2: unable to find 'X' button to close raid/dungeon/trials/gauntlet window. Ignoring...");
            sleep(SECOND);
            if (state == State.Expedition) {
                sleep(SECOND);

                // Close Portal Map after expedition
                readScreen(2 * SECOND);
                seg = detectCue(cues.get("X"));
                clickOnSeg(seg);
                sleep(SECOND);

                // close Expedition window after Expedition
                readScreen(2 * SECOND);
                seg = detectCue(cues.get("X"));
                clickOnSeg(seg);
                sleep(SECOND);

                if (BHBot.settings.difficultyFailsafe.containsKey("e")) {
                    // The key is the difficulty decrease, the value is the minimum level
                    Map.Entry<Integer, Integer> expedDifficultyFailsafe = BHBot.settings.difficultyFailsafe.get("e");
                    int levelOffset = expedDifficultyFailsafe.getKey();
                    int minimumLevel = expedDifficultyFailsafe.getValue();

                    // We check that the level offset for expedition is a multiplier of 5
                    int levelOffsetModule = levelOffset % 5;
                    if (levelOffsetModule != 0) {

                        int newLevelOffset = levelOffset + (5 - levelOffsetModule);
                        BHBot.logger.warn("Level offset " + levelOffset + " is not multiplier of 5, rounding it to " + newLevelOffset);

                        BHBot.settings.difficultyFailsafe.put("e", Maps.immutableEntry(newLevelOffset, minimumLevel));
                    }

                    // We calculate the new difficulty
                    int newExpedDifficulty = expeditionFailsafeDifficulty - levelOffset;

                    // We check that the new difficulty is not lower than the minimum
                    if (newExpedDifficulty < minimumLevel) newExpedDifficulty = minimumLevel;
                    if (newExpedDifficulty < 5) newExpedDifficulty = 5;

                    // If the new difficulty is different from the current one, we update the ini setting
                    if (newExpedDifficulty != expeditionFailsafeDifficulty) {
                        String original = expeditionFailsafePortal + " " + expeditionFailsafeDifficulty;
                        String updated = expeditionFailsafePortal + " " + newExpedDifficulty;
                        settingsUpdate(original, updated);
                    }
                }
            }
            if (state.equals(State.Raid)) {

                long runMillis = Misc.getTime() - (activityStartTime * 1000);
                String runtime = String.format("%01dm%02ds",
                        TimeUnit.MILLISECONDS.toMinutes(runMillis),
                        TimeUnit.MILLISECONDS.toSeconds(runMillis) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(runMillis)));
                BHBot.logger.stats("Run time: " + runtime);
            } else if (state.equals(State.Trials)) {
                if (BHBot.settings.difficultyFailsafe.containsKey("t")) {
                    // The key is the difficulty decrease, the value is the minimum level
                    Map.Entry<Integer, Integer> trialDifficultyFailsafe = BHBot.settings.difficultyFailsafe.get("t");
                    int levelOffset = trialDifficultyFailsafe.getKey();
                    int minimumLevel = trialDifficultyFailsafe.getValue();

                    // We calculate the new difficulty
                    int newTrialDifficulty = BHBot.settings.difficultyTrials - levelOffset;

                    // We check that the new difficulty is not lower than the minimum
                    if (newTrialDifficulty < minimumLevel) newTrialDifficulty = minimumLevel;

                    // If the new difficulty is different from the current one, we update the ini setting
                    if (newTrialDifficulty != BHBot.settings.difficultyTrials) {
                        String original = "difficultyTrials " + BHBot.settings.difficultyTrials;
                        String updated = "difficultyTrials " + newTrialDifficulty;
                        settingsUpdate(original, updated);
                    }
                }
            } else if (state.equals(State.Gauntlet)) {
                if (BHBot.settings.difficultyFailsafe.containsKey("g")) {
                    // The key is the difficulty decrease, the value is the minimum level
                    Map.Entry<Integer, Integer> gauntletDifficultyFailsafe = BHBot.settings.difficultyFailsafe.get("g");
                    int levelOffset = gauntletDifficultyFailsafe.getKey();
                    int minimumLevel = gauntletDifficultyFailsafe.getValue();

                    // We calculate the new difficulty
                    int newGauntletDifficulty = BHBot.settings.difficultyGauntlet - levelOffset;

                    // We check that the new difficulty is not lower than the minimum
                    if (newGauntletDifficulty < minimumLevel) newGauntletDifficulty = minimumLevel;

                    // If the new difficulty is different from the current one, we update the ini setting
                    if (newGauntletDifficulty != BHBot.settings.difficultyGauntlet) {
                        String original = "difficultyGauntlet " + BHBot.settings.difficultyGauntlet;
                        String updated = "difficultyGauntlet " + newGauntletDifficulty;
                        settingsUpdate(original, updated);
                    }
                }
            }
            resetAppropriateTimers();
            resetRevives();

            // We make sure to dress up
            if (state == State.PVP && BHBot.settings.pvpstrip.size() > 0) {
                dressUp(BHBot.settings.pvpstrip);
                readScreen(SECOND * 2);
            }
            if (state == State.GVG && BHBot.settings.gvgstrip.size() > 0) {
                dressUp(BHBot.settings.gvgstrip);
                readScreen(SECOND * 2);
            }

            // We make sure to disable autoshrine when defeated
            if (state == State.Trials || state == State.Raid || state == State.Expedition) {
                if (ignoreBossSetting && ignoreShrinesSetting) {
                    if (!checkShrineSettings(false, false)) {
                        BHBot.logger.error("Impossible to disable autoShrine after defeat! Restarting..");
                        restart();
                    }
                }
                autoShrined = false;
                autoBossRuned = false;
                readScreen(SECOND * 2);
            }

            state = State.Main; // reset state
            return;
        }

        // check if we're done (victory - found in gauntlet, for example):
        //Victory screen on world boss is slightly different, so we check both
        MarvinSegment seg1 = detectCue(cues.get("Victory"));
        MarvinSegment seg2 = detectCue(cues.get("WorldBossVictory"));
        if (state != State.Raid && seg1 != null || (state == State.WorldBoss && seg2 != null)) { //seg2 needs state defined as otherwise the battle victory screen in dungeon-type encounters triggers it
            counters.get(state).increaseVictories();
            handleSuccessTreshold(state);

            BHBot.logger.info(state.getName() + " #" + counters.get(state).getTotal() + " completed. Result: Victory");
            BHBot.logger.stats(state.getName() + " " + counters.get(state).successRateDesc());

            handleVictory();

            seg = detectCue(cues.get("CloseGreen"), 2 * SECOND); // we need to wait a little bit because the popup scrolls down and the close button is semi-transparent (it stabilizes after popup scrolls down and bounces a bit)
            if (seg != null)
                clickOnSeg(seg);
            else {
                BHBot.logger.debug("Problem: 'Victory' window (as found in e.g. gauntlets) has been detected, but no 'Close' button. Ignoring...");
                return;
            }

            // close the raid/dungeon/trial/gauntlet window:
            readScreen(2 * SECOND);
            detectCue(cues.get("X"), 5 * SECOND);
            sleep(SECOND); // because the popup window may still be sliding down so the "X" button is changing position and we must wait for it to stabilize
            seg = detectCue(cues.get("X"), SECOND);
            if (seg != null)
                clickOnSeg(seg);
            else
                BHBot.logger.warn("Error #3: unable to find 'X' button to close raid/dungeon/trials/gauntlet window. Ignoring...");

            sleep(SECOND);
            if (state == State.WorldBoss && (BHBot.settings.countActivities)) {
                updateActivityCounter(state.getName());
            }
            resetAppropriateTimers();
            resetRevives();
            if (state == State.GVG) dressUp(BHBot.settings.gvgstrip);

            /* Sometime Bit Heroes ignores the Ignore Boss / Ignore Shrines setting and so, when we finish a dungeon,
             *  We make sure to disable them to avoid the idle time exceeded warning. */
            if (state == State.Trials || state == State.Raid || state == State.Expedition) {
                if (ignoreShrinesSetting || ignoreBossSetting) {
                    readScreen(SECOND);
                    checkShrineSettings(false, false);
                }
            }

            state = State.Main; // reset state
            return;
        }

        //small sleep so this function isn't too taxing on performance
//        sleep(2000);

        // at the end of this method, revert idle time change (in order for idle detection to function properly):
        BHBot.scheduler.restoreIdleTime();
    }

    private void handleAutoBossRune() { //seperate function so we can run autoRune without autoShrine
        MarvinSegment guildButtonSeg;
        guildButtonSeg = detectCue(cues.get("GuildButton"));

        if ((state == State.Raid && !BHBot.settings.autoShrine.contains("r") && BHBot.settings.autoBossRune.containsKey("r")) ||
                (state == State.Trials && !BHBot.settings.autoShrine.contains("t") && BHBot.settings.autoBossRune.containsKey("t")) ||
                (state == State.Expedition && !BHBot.settings.autoShrine.contains("e") && BHBot.settings.autoBossRune.containsKey("e")) ||
                (state == State.Dungeon && BHBot.settings.autoBossRune.containsKey("d")) ||
                state == State.UnidentifiedDungeon) {
            if (activityDuration > 60) { //if we're past 60 seconds into the activity
                if (!autoBossRuned) {
                    if ((outOfEncounterTimestamp - inEncounterTimestamp) > BHBot.settings.battleDelay && guildButtonSeg != null) { //and it's been the battleDelay setting since last encounter
                        BHBot.logger.autorune("No activity for " + BHBot.settings.battleDelay + "s, changing runes for boss encounter");

                        handleMinorBossRunes();

                        if (!checkShrineSettings(false, false)) {
                            BHBot.logger.error("Impossible to disable Ignore Shrines in handleAutoBossRune!");
                            BHBot.logger.warn("Resetting encounter timer to try again in " + BHBot.settings.battleDelay + " seconds.");
                            inEncounterTimestamp = Misc.getTime() / 1000;
                            return;
                        }

                        // We disable and re-enable the auto feature
                        while (detectCue(cues.get("AutoOn"), 500) != null) {
                            clickInGame(780, 270); //auto off
                            readScreen(500);
                        }
                        while (detectCue(cues.get("AutoOff"), 500) != null) {
                            clickInGame(780, 270); //auto on again
                            readScreen(500);
                        }

                        autoBossRuned = true;
                    }
                }
            }
        }
    }

    private void handleAutoShrine() {
        MarvinSegment guildButtonSeg;
        guildButtonSeg = detectCue(cues.get("GuildButton"));

        if ((state == State.Raid && BHBot.settings.autoShrine.contains("r")) ||
                (state == State.Trials && BHBot.settings.autoShrine.contains("t")) ||
                (state == State.Expedition && BHBot.settings.autoShrine.contains("e")) ||
                (state == State.UnidentifiedDungeon)) {
            if (activityDuration > 60) { //if we're past 60 seconds into the activity
                if (!autoShrined) {
                    if ((outOfEncounterTimestamp - inEncounterTimestamp) > BHBot.settings.battleDelay && guildButtonSeg != null) { //and it's been the battleDelay setting since last encounter
                        BHBot.logger.autoshrine("No activity for " + BHBot.settings.battleDelay + "s, disabling ignore shrines");

                        if (!checkShrineSettings(true, false)) {
                            BHBot.logger.error("Impossible to disable Ignore Shrines in handleAutoShrine!");
                            return;
                        }
                        readScreen(100);

                        // We disable and re-enable the auto feature
                        while (detectCue(cues.get("AutoOn"), 500) != null) {
                            clickInGame(780, 270); //auto off
                            readScreen(500);
                        }
                        while (detectCue(cues.get("AutoOff"), 500) != null) {
                            clickInGame(780, 270); //auto on again
                            readScreen(500);
                        }

                        if ((state == State.Raid && BHBot.settings.autoBossRune.containsKey("r")) || (state == State.Trials && BHBot.settings.autoBossRune.containsKey("t")) ||
                                (state == State.Expedition && BHBot.settings.autoBossRune.containsKey("e")) || (state == State.Dungeon && BHBot.settings.autoBossRune.containsKey("d"))) {
                                     handleMinorBossRunes();
                        } else {
                            BHBot.logger.autoshrine("Waiting " + BHBot.settings.shrineDelay + "s to use shrines");
                            sleep(BHBot.settings.shrineDelay * SECOND); //if we're not changing runes we sleep while we activate shrines
                        }

                        if (!checkShrineSettings(false, false)) {
                            BHBot.logger.error("Impossible to disable Ignore Boss in handleAutoShrine!");
                            return;
                        }
                        readScreen(100);

                        // We disable and re-enable the auto feature
                        while (detectCue(cues.get("AutoOn"), 500) != null) {
                            clickInGame(780, 270); //auto off
                            readScreen(500);
                        }
                        while (detectCue(cues.get("AutoOff"), 500) != null) {
                            clickInGame(780, 270); //auto on again
                            readScreen(500);
                        }

                        autoShrined = true;
                        BHBot.scheduler.resetIdleTime(true);
                    }
                }
            }
        }
    }

    private void motionChecker(double sensitivity, int x1, int x2, int y1, int y2, int counter) {
        if (motionChecker) {
            readScreen();
            movement1 = img;
            if (CueCompare.imageDifference(movement1, movement2, sensitivity, x1 ,x2 ,y1 ,y2)) {
                stationaryCounter++;
            } else stationaryCounter = 0;
            motionChecker = false;
        } else {
            readScreen();
            movement2 = img;
            if (CueCompare.imageDifference(movement1, movement2, sensitivity, x1 ,x2 ,y1 ,y2)) {
                stationaryCounter++;
            } else stationaryCounter = 0;
            motionChecker = true;
        }

        if (stationaryCounter >= counter) { //if we have 2 stationary reports in a row
            BHBot.logger.debug("Looks like we are stationary");
            stationary = true;
            stationaryCounter = 0;
        }
    }

    private void handleMinorRunes(String activity) {
        List<String> desiredRunesAsStrs;
        String activityName = state.getNameFromShortcut(activity);
        if (BHBot.settings.autoRuneDefault.isEmpty()) {
            BHBot.logger.debug("autoRunesDefault not defined; aborting autoRunes");
            return;
        }

        if (!BHBot.settings.autoRune.containsKey(activity)) {
            BHBot.logger.debug("No autoRunes assigned for " + activityName + ", using defaults.");
            desiredRunesAsStrs = BHBot.settings.autoRuneDefault;
        } else {
            BHBot.logger.info("Configuring autoRunes for " + activityName);
            desiredRunesAsStrs = BHBot.settings.autoRune.get(activity);
        }

        List<MinorRuneEffect> desiredRunes = resolveDesiredRunes(desiredRunesAsStrs);
        if (noRunesNeedSwitching(desiredRunes)) {
            return;
        }

        // Back out of any raid/gauntlet/trial/GvG/etc pre-menu
        MarvinSegment seg = detectCue(cues.get("X"), 2 * SECOND);
        if (seg != null) {
            clickOnSeg(seg);
            readScreen(SECOND);
        }

        if (!switchMinorRunes(desiredRunes))
            BHBot.logger.info("AutoRune failed!");

    }

    private void handleMinorBossRunes() {
        if (BHBot.settings.autoRuneDefault.isEmpty()) {
            BHBot.logger.debug("autoRunesDefault not defined; aborting autoBossRunes");
            return;
        }

        String activity = state.getShortcut();
        // Hack to work around unknown dungeons
        if (activity.equals("ud"))
            activity = "d";
        if (!BHBot.settings.autoBossRune.containsKey(activity)) {
            BHBot.logger.info("No autoBossRunes assigned for " + state.getName() + ", skipping.");
            return;
        }

        List<String> desiredRunesAsStrs = BHBot.settings.autoBossRune.get(activity);
        List<MinorRuneEffect> desiredRunes = resolveDesiredRunes(desiredRunesAsStrs);
        if (noRunesNeedSwitching(desiredRunes))
            return;

        if (!switchMinorRunes(desiredRunes))
            BHBot.logger.autorune("AutoBossRune failed!");

    }

    private List<MinorRuneEffect> resolveDesiredRunes(List<String> desiredRunesAsStrs) {
        List<MinorRuneEffect> desiredRunes = new ArrayList<>();

        if (desiredRunesAsStrs.size() != 2) {
            BHBot.logger.error("Got malformed autoRunes, using defaults: " + String.join(" ", desiredRunesAsStrs));
            desiredRunesAsStrs = BHBot.settings.autoRuneDefault;
        }

        String strLeftRune = desiredRunesAsStrs.get(0);
        MinorRuneEffect desiredLeftRune = MinorRuneEffect.getEffectFromName(strLeftRune);
        if (desiredLeftRune == null) {
            BHBot.logger.error("No rune type found for left rune name " + strLeftRune);
            desiredLeftRune = leftMinorRune.getRuneEffect();
        }
        desiredRunes.add(desiredLeftRune);

        String strRightRune = desiredRunesAsStrs.get(1);
        MinorRuneEffect desiredRightRune = MinorRuneEffect.getEffectFromName(strRightRune);
        if (desiredRightRune == null) {
            BHBot.logger.error("No rune type found for right rune name " + strRightRune);
            desiredRightRune = rightMinorRune.getRuneEffect();
        }

        desiredRunes.add(desiredRightRune);

        return desiredRunes;
    }

    private boolean noRunesNeedSwitching(List<MinorRuneEffect> desiredRunes) {
        MinorRuneEffect desiredLeftRune = desiredRunes.get(0);
        MinorRuneEffect desiredRightRune = desiredRunes.get(1);
        MinorRuneEffect currentLeftRune = leftMinorRune.getRuneEffect();
        MinorRuneEffect currentRightRune = rightMinorRune.getRuneEffect();

        if (desiredLeftRune == currentLeftRune && desiredRightRune == currentRightRune) {
            BHBot.logger.debug("No runes found that need switching.");
            return true; // Nothing to do
        }

        if (desiredLeftRune != currentLeftRune) {
            BHBot.logger.debug("Left minor rune needs to be switched.");
        }
        if (desiredRightRune != currentRightRune) {
            BHBot.logger.debug("Right minor rune needs to be switched.");
        }

        return false;

    }

    private Boolean switchMinorRunes(List<MinorRuneEffect> desiredRunes) {
        MinorRuneEffect desiredLeftRune = desiredRunes.get(0);
        MinorRuneEffect desiredRightRune = desiredRunes.get(1);

        if (!detectEquippedMinorRunes(true, false)) {
            BHBot.logger.error("Unable to detect runes, pre-equip.");
            return false;
        }

        if (desiredLeftRune != leftMinorRune.getRuneEffect()) {
            BHBot.logger.debug("Switching left minor rune.");
            sleep(500); //sleep for window animation to finish
            clickInGame(280, 290); // Click on left rune
            if (!switchSingleMinorRune(desiredLeftRune)) {
                BHBot.logger.error("Failed to switch left minor rune.");
                return false;
            }
        }


        if (desiredRightRune != rightMinorRune.getRuneEffect()) {
            BHBot.logger.debug("Switching right minor rune.");
            sleep(500); //sleep for window animation to finish
            clickInGame(520, 290); // Click on right rune
            if (!switchSingleMinorRune(desiredRightRune)) {
                BHBot.logger.error("Failed to switch right minor rune.");
                return false;
            }
        }

        sleep(SECOND); //sleep while we wait for window animation

        if (!detectEquippedMinorRunes(false, true)) {
            BHBot.logger.error("Unable to detect runes, post-equip.");
            return false;
        }

        sleep(2 * SECOND);
        boolean success = true;
        if (desiredLeftRune != leftMinorRune.getRuneEffect()) {
            BHBot.logger.error("Left minor rune failed to switch for unknown reason.");
            success = false;
        }
        if (desiredRightRune != rightMinorRune.getRuneEffect()) {
            BHBot.logger.error("Right minor rune failed to switch for unknown reason.");
            success = false;
        }

        return success;
    }

    private Boolean switchSingleMinorRune(MinorRuneEffect desiredRune) {

        MarvinSegment seg = detectCue(cues.get("RunesSwitch"), 5 * SECOND);
        if (seg == null) {
            BHBot.logger.error("Failed to find rune switch button.");
            return false;
        }
        clickOnSeg(seg);

        seg = detectCue(cues.get("RunesPicker"), 5 * SECOND);
        if (seg == null) {
            BHBot.logger.error("Failed to find rune picker.");
            return false;
        }

        ItemGrade maxRuneGrade = MinorRune.maxGrade;
        for (int runeGradeVal = maxRuneGrade.getValue(); runeGradeVal > 0; runeGradeVal--) {
            ItemGrade runeGrade = ItemGrade.getGradeFromValue(runeGradeVal);
            MinorRune thisRune = MinorRune.getRune(desiredRune, runeGrade);

            if (thisRune == null) {
                BHBot.logger.error("Unable to getRune in switchSingleMinorRune");
                continue;
            }

            Cue runeCue = thisRune.getRuneSelectCue();
            if (runeCue == null) {
                BHBot.logger.error("Unable to find cue for rune " + getRuneName(thisRune.getRuneCueName()));
                continue;
            }
            seg = detectCue(runeCue);
            if (seg == null) {
                BHBot.logger.debug("Unable to find " + getRuneName(thisRune.getRuneCueName()) + " in rune picker.");
                continue;
            }
            BHBot.logger.autorune("Switched to " + getRuneName(thisRune.getRuneCueName()));
            clickOnSeg(seg);
            sleep(SECOND);
            return true;
        }

        BHBot.logger.error("Unable to find rune of type " + desiredRune);
        closePopupSecurely(cues.get("RunesPicker"), cues.get("X"));
        sleep(SECOND);
        return false;
    }

    /**
     * Function to return the name of the runes for console output
     */
    private String getRuneName(String runeName) {

        switch (runeName) {
            case "MinorRuneExperienceCommon":
                return "Common Experience";
            case "MinorRuneExperienceRare":
                return "Rare Experience";
            case "MinorRuneExperienceEpic":
                return "Epic Experience";
            case "MinorRuneExperienceLegendary":
                return "Legendary Experience";
            case "MinorRuneItem_FindCommon":
                return "Common Item Find";
            case "MinorRuneItem_FindRare":
                return "Rare Item Find";
            case "MinorRuneItem_FindEpic":
                return "Epic Item Find";
            case "MinorRuneItem_FindLegendary":
                return "Legendary Item Find";
            case "MinorRuneGoldCommon":
                return "Common Gold";
            case "MinorRuneGoldRare":
                return "Rare Gold";
            case "MinorRuneGoldEpic":
                return "Epic Gold";
            case "MinorRuneGoldLegendary":
                return "Legendary Gold";
            case "MinorRuneCaptureCommon":
                return "Common Capture";
            case "MinorRuneCaptureRare":
                return "Rare Capture";
            case "MinorRuneCaptureEpic":
                return "Epic Capture";
            case "MinorRuneCaptureLegendary":
                return "Legendary Capture";
            default:
                return null;
        }
    }

    private boolean handleSkeletonKey() {
        MarvinSegment seg;

        seg = detectCue(cues.get("SkeletonNoKeys"), 2 * SECOND);
        if (seg != null) {
            BHBot.logger.warn("No skeleton keys, skipping..");
            seg = detectCue(cues.get("Decline"), 5 * SECOND);
            clickOnSeg(seg);
            readScreen(SECOND);
            seg = detectCue(cues.get("YesGreen"), 5 * SECOND);
            clickOnSeg(seg);
            return false;
        }

        if (BHBot.settings.openSkeleton == 0) {
            BHBot.logger.info("Skeleton treasure found, declining.");
            seg = detectCue(cues.get("Decline"), 5 * SECOND);
            clickOnSeg(seg);
            readScreen(SECOND);
            seg = detectCue(cues.get("YesGreen"), 5 * SECOND);
            clickOnSeg(seg);
            return false;

        } else if (BHBot.settings.openSkeleton == 1) {
            BHBot.logger.info("Skeleton treasure found, attemping to use key");
            seg = detectCue(cues.get("Open"), 5 * SECOND);
            if (seg == null) {
                BHBot.logger.error("Open button not found, restarting");
                String STScreen = saveGameScreen("skeleton-treasure-no-open");
                if (BHBot.settings.enablePushover && BHBot.settings.poNotifyErrors) {
                    if (STScreen != null) {
                        sendPushOverMessage("Treasure chest error", "Skeleton Chest gump without OPEN button", "siren", MessagePriority.NORMAL, new File(STScreen));
                    } else {
                        sendPushOverMessage("Treasure chest error", "Skeleton Chest gump without OPEN button", "siren", MessagePriority.NORMAL, null);
                    }
                }
                return true;
            }
            clickOnSeg(seg);
            readScreen(SECOND);
            seg = detectCue(cues.get("YesGreen"), 5 * SECOND);
            if (seg == null) {
                BHBot.logger.error("Yes button not found, restarting");
                if (BHBot.settings.enablePushover && BHBot.settings.poNotifyErrors) {
                    String STScreen = saveGameScreen("skeleton-treasure-no-yes");
                    if (STScreen != null) {
                        sendPushOverMessage("Treasure chest error", "Skeleton Chest gump without YES button", "siren", MessagePriority.NORMAL, new File(STScreen));
                    } else {
                        sendPushOverMessage("Treasure chest error", "Skeleton Chest gump without YES button", "siren", MessagePriority.NORMAL, null);
                    }
                }
                return true;
            }
            clickOnSeg(seg);
            return false;

        } else if (BHBot.settings.openSkeleton == 2 && state == State.Raid) {
            BHBot.logger.info("Raid Skeleton treasure found, attemping to use key");
            seg = detectCue(cues.get("Open"), 5 * SECOND);
            if (seg == null) {
                BHBot.logger.error("Open button not found, restarting");
                String STScreen = saveGameScreen("skeleton-treasure-no-open");
                if (BHBot.settings.enablePushover && BHBot.settings.poNotifyErrors) {
                    if (STScreen != null) {
                        sendPushOverMessage("Treasure chest error", "Skeleton Chest gump without OPEN button", "siren", MessagePriority.NORMAL, new File(STScreen));
                    } else {
                        sendPushOverMessage("Treasure chest error", "Skeleton Chest gump without OPEN button", "siren", MessagePriority.NORMAL, null);
                    }
                }
                return true;
            }
            clickOnSeg(seg);
            readScreen(SECOND);
            seg = detectCue(cues.get("YesGreen"), 5 * SECOND);
            if (seg == null) {
                BHBot.logger.error("Yes button not found, restarting");
                if (BHBot.settings.enablePushover && BHBot.settings.poNotifyErrors) {
                    String STScreen = saveGameScreen("skeleton-treasure-no-yes");
                    if (STScreen != null) {
                        sendPushOverMessage("Treasure chest error", "Skeleton Chest gump without YES button", "siren", MessagePriority.NORMAL, new File(STScreen));
                    } else {
                        sendPushOverMessage("Treasure chest error", "Skeleton Chest gump without YES button", "siren", MessagePriority.NORMAL, null);
                    }
                }
                return true;
            }
            clickOnSeg(seg);
            sleep(500);
            if ((BHBot.settings.screenshots.contains("s"))) {
                saveGameScreen("skeleton-contents", "rewards");
            }
            return false;

        } else
            BHBot.logger.info("Skeleton treasure found, declining.");
        seg = detectCue(cues.get("Decline"), 5 * SECOND);
        clickOnSeg(seg);
        readScreen(SECOND);
        seg = detectCue(cues.get("YesGreen"), 5 * SECOND);
        clickOnSeg(seg);
        return false;
    }

    private void handleFamiliarEncounter() {
        MarvinSegment seg;

        BHBot.logger.autobribe("Familiar encountered");
        readScreen(2 * SECOND);

        FamiliarType familiarLevel;
        if (detectCue(cues.get("CommonFamiliar")) != null) {
            familiarLevel = FamiliarType.COMMON;
        } else if (detectCue(cues.get("RareFamiliar")) != null) {
            familiarLevel = FamiliarType.RARE;
        } else if (detectCue(cues.get("EpicFamiliar")) != null) {
            familiarLevel = FamiliarType.EPIC;
        } else if (detectCue(cues.get("LegendaryFamiliar")) != null) {
            familiarLevel = FamiliarType.LEGENDARY;
        } else {
            familiarLevel = FamiliarType.ERROR; // error
        }

        PersuationType persuasion;
        BribeDetails bribeInfo = new BribeDetails();

        // Checking familiars setting takes time and a lot of cues verifications. We try to minimize the number of times
        // this is done
        boolean skipBribeNames = false;
        if ((BHBot.settings.bribeLevel > 0 && familiarLevel.getValue() >= BHBot.settings.bribeLevel) ||
                (BHBot.settings.familiars.size() == 0)) {
            skipBribeNames = true;
        }

        if (!skipBribeNames) {
            bribeInfo = verifyBribeNames();
        }

        if ((BHBot.settings.bribeLevel > 0 && familiarLevel.getValue() >= BHBot.settings.bribeLevel) ||
                bribeInfo.toBribeCnt > 0) {
            persuasion = PersuationType.BRIBE;
        } else if ((BHBot.settings.persuasionLevel > 0 && familiarLevel.getValue() >= BHBot.settings.persuasionLevel)) {
            persuasion = PersuationType.PERSUADE;
        } else {
            persuasion = PersuationType.DECLINE;
        }

        // If we're set to bribe and we don't have gems, we default to PERSUASION
        if (persuasion == PersuationType.BRIBE && noGemsToBribe) {
            persuasion = PersuationType.PERSUADE;
        }

        StringBuilder persuasionLog = new StringBuilder("familiar-");
        persuasionLog.append(familiarLevel.toString().toUpperCase()).append("-");
        persuasionLog.append(persuasion.toString().toUpperCase()).append("-");
        persuasionLog.append("attempt");

        // We save all the errors and persuasions based on settings
        if ((familiarLevel.getValue() >= BHBot.settings.familiarScreenshot) || familiarLevel == FamiliarType.ERROR) {
            saveGameScreen(persuasionLog.toString(), "familiars");

            if (BHBot.settings.contributeFamiliars) {
                contributeFamiliarShoot(persuasionLog.toString(), familiarLevel);
            }
        }

        // We attempt persuasion or bribe based on settings
        if (persuasion == PersuationType.BRIBE) {
            if (!bribeFamiliar()) {
                BHBot.logger.autobribe("Bribe attempt failed! Trying with persuasion...");
                if (persuadeFamiliar()) {
                    BHBot.logger.autobribe(familiarLevel.toString().toUpperCase() + " persuasion attempted.");
                } else {
                    BHBot.logger.error("Impossible to persuade familiar, restarting...");
                    restart();
                }
            } else {
                updateFamiliarCounter(bribeInfo.familiarName.toUpperCase());
            }
        } else if (persuasion == PersuationType.PERSUADE) {
            if (persuadeFamiliar()) {
                BHBot.logger.autobribe(familiarLevel.toString().toUpperCase() + " persuasion attempted.");
            } else {
                BHBot.logger.error("Impossible to attempt persuasion, restarting.");
                restart();
            }
        } else {
            seg = detectCue(cues.get("DeclineRed"));
            if (seg != null) {
                clickOnSeg(seg); // seg = detectCue(cues.get("Persuade"))
                readScreen(SECOND * 2);
                seg = detectCue(cues.get("YesGreen"), SECOND);
                clickOnSeg(seg);
                BHBot.logger.autobribe(familiarLevel.toString().toUpperCase() + " persuasion declined.");
            } else {
                BHBot.logger.error("Impossible to find the decline button, restarting...");
                restart();
            }
        }
    }

    /**
     * Will verify if in the current persuasion screen one of the bribeNames is present
     */
    private BribeDetails verifyBribeNames() {

        BooleanSupplier openView = () -> {
            MarvinSegment seg;
            seg = detectCue(cues.get("View"), SECOND * 3);
            if (seg != null) {
                clickOnSeg(seg);
                readScreen(SECOND * 2);
                return true;
            } else {
                return false;
            }
        };

        BooleanSupplier closeView = () -> {
            MarvinSegment seg;
            seg = detectCue(cues.get("X"), 2 * SECOND);
            if (seg != null) {
                clickOnSeg(seg);
                readScreen(SECOND);
                return true;
            } else {
                return false;
            }
        };

        List<String> wrongNames = new ArrayList<>();
        BribeDetails result = new BribeDetails();
        String familiarName;
        int toBribeCnt;

        boolean viewIsOpened = false;

        readScreen(SECOND);
        for (String familiarDetails : BHBot.settings.familiars) {
            // familiar details from settings
            String[] details = familiarDetails.toLowerCase().split(" ");
            familiarName = details[0];
            toBribeCnt = Integer.parseInt(details[1]);

            // cue related stuff
            boolean isOldFormat = false;

            Cue familiarCue = cues.getOrDefault(familiarName, null);

            if (familiarCue == null) {
                familiarCue = cues.getOrDefault("old" + familiarName, null);
                if (familiarCue != null) isOldFormat = true;
            }

            if (familiarCue != null) {
                if (toBribeCnt > 0) {
                    if (isOldFormat) { // Old style familiar
                        if (!viewIsOpened) { // we try to open the view menu if closed
                            if (openView.getAsBoolean()) {
                                readScreen(SECOND * 2);
                                viewIsOpened = true;
                            } else {
                                BHBot.logger.error("Old format familiar with no view button");
                                restart();
                            }
                        }
                    } else { // New style familiar
                        if (viewIsOpened) { // we try to close the view menu if opened
                            if (closeView.getAsBoolean()) {
                                readScreen(SECOND);
                                viewIsOpened = false;
                            } else {
                                BHBot.logger.error("Old style familiar detected with no X button to close the view menu.");
                                restart();
                            }
                        }
                    }

                    if (detectCue(familiarCue, SECOND * 3) != null) {
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

        if (viewIsOpened) {
            if (!closeView.getAsBoolean()) {
                BHBot.logger.error("Impossible to close view menu at the end of familiar setting loop!");
                restart();
            }
        }

        // If there is any error we update the settings
        for (String wrongName : wrongNames) {
            BHBot.settings.familiars.remove(wrongName);
        }

        return result;
    }

    private boolean bribeFamiliar() {
        readScreen();
        MarvinSegment seg = detectCue(cues.get("Bribe"), SECOND * 3);
        BufferedImage tmpScreen = img;

        if (seg != null) {
            clickOnSeg(seg);
            sleep(2 * SECOND);

            seg = detectCue(cues.get("YesGreen"), SECOND * 5);
            if (seg != null) {
                clickOnSeg(seg);
                sleep(2 * SECOND);
            }

            if (detectCue(cues.get("NotEnoughGems"), SECOND * 5) != null) {
                BHBot.logger.warn("Not enough gems to attempt a bribe!");
                noGemsToBribe = true;
                if (!closePopupSecurely(cues.get("NotEnoughGems"), cues.get("No"))) {
                    BHBot.logger.error("Impossible to close the Not Enough gems pop-up. Restarting...");
                    restart();
                }
                return false;
            }
            if (BHBot.settings.enablePushover && BHBot.settings.poNotifyBribe) {
                String bribeScreenName = saveGameScreen("bribe-screen", tmpScreen);
                File bribeScreenFile = bribeScreenName != null ? new File(bribeScreenName) : null;
                sendPushOverMessage("Creature Bribe", "A familiar has been bribed!", "bugle", MessagePriority.NORMAL, bribeScreenFile);
                if (bribeScreenFile != null && !bribeScreenFile.delete()) BHBot.logger.warn("Impossible to delete tmp img file for bribe.");
            }
            return true;
        }

        return false;
    }

    private boolean persuadeFamiliar() {

        MarvinSegment seg;
        seg = detectCue(cues.get("Persuade"));
        if (seg != null) {

            clickOnSeg(seg); // seg = detectCue(cues.get("Persuade"))
            sleep(2 * SECOND);

            readScreen();
            seg = detectCue(cues.get("YesGreen"));
            clickOnSeg(seg);
            sleep(2 * SECOND);

            return true;
        }

        return false;
    }

    private void handleAutoRevive() {

        // We only use auto-revive for Trials, Gauntlet and Raids

        MarvinSegment seg;

        // Auto Revive is disabled, we re-enable it
        if ((BHBot.settings.autoRevive.size() == 0) || (state != State.Trials && state != State.Gauntlet
                && state != State.Raid && state != State.Expedition)) {
            BHBot.logger.debug("AutoRevive disabled, reenabling auto.. State = '" + state + "'");
            seg = detectCue(cues.get("AutoOff"));
            if (seg != null) clickOnSeg(seg);
            BHBot.scheduler.resetIdleTime(true);
            return;
        }

        // if everyone dies autoRevive attempts to revive people on the defeat screen, this should prevent that
        seg = detectCue(cues.get("Defeat"), SECOND);
        if (seg != null) {
            BHBot.logger.autorevive("Defeat screen, skipping revive check");
            seg = detectCue(cues.get("AutoOff"), SECOND);
            if (seg != null) clickOnSeg(seg);
            readScreen(SECOND);
            BHBot.scheduler.resetIdleTime(true);
            return;
        }

        seg = detectCue(cues.get("VictoryPopup"), 500);
        if (seg != null) {
            BHBot.logger.autorevive("Victory popup, skipping revive check");
            seg = detectCue(cues.get("AutoOff"), SECOND);
            if (seg != null) clickOnSeg(seg);

            seg = detectCue(cues.get("CloseGreen"), 2 * SECOND); // after enabling auto again the bot would get stuck at the victory screen, this should close it
            if (seg != null)
                clickOnSeg(seg);
            else {
                BHBot.logger.warn("Problem: 'Victory' window has been detected, but no 'Close' button. Ignoring...");
                return;
            }
            BHBot.scheduler.resetIdleTime(true);
            return;
        }

        // we make sure that we stick with the limits
        if (potionsUsed >= BHBot.settings.potionLimit) {
            BHBot.logger.autorevive("Potion limit reached, skipping revive check");
            seg = detectCue(cues.get("AutoOff"), SECOND);
            if (seg != null) clickOnSeg(seg);
            BHBot.scheduler.resetIdleTime(true);
            return;
        }

        seg = detectCue(cues.get("Potions"), SECOND * 2);
        if (seg != null) {
            clickOnSeg(seg);
            readScreen(SECOND);

            // If no potions are needed, we re-enable the Auto function
            seg = detectCue(cues.get("NoPotions"), SECOND); // Everyone is Full HP
            if (seg != null) {
                seg = detectCue("Close", SECOND, new Bounds(300, 330, 500, 400));
                if (seg != null) {
                    BHBot.logger.autorevive("None of the team members need a consumable, exiting from autoRevive");
                    clickOnSeg(seg);
                    seg = detectCue(cues.get("AutoOff"), SECOND);
                    clickOnSeg(seg);
                } else {
                    BHBot.logger.error("No potions cue detected, without close button, restarting!");
                    saveGameScreen("autorevive-no-potions-no-close", img);
                    restart();
                }
                BHBot.scheduler.resetIdleTime(true);
                return;
            }

            // Based on the state we get the team size
            HashMap<Integer, Point> revivePositions = new HashMap<>();
            switch (state) {
                case Trials:
                case Gauntlet:
                case Expedition:
                    revivePositions.put(1, new Point(290, 315));
                    revivePositions.put(2, new Point(200, 340));
                    revivePositions.put(3, new Point(115, 285));
                    break;
                case Raid:
                    revivePositions.put(1, new Point(305, 320));
                    revivePositions.put(2, new Point(250, 345));
                    revivePositions.put(3, new Point(200, 267));
                    revivePositions.put(4, new Point(150, 325));
                    revivePositions.put(5, new Point(90, 295));
                    break;
                default:
                    break;
            }

            if ((state == State.Trials && BHBot.settings.autoRevive.contains("t")) ||
                    (state == State.Gauntlet && BHBot.settings.autoRevive.contains("g")) ||
                    (state == State.Raid && BHBot.settings.autoRevive.contains("r")) ||
                    (state == State.Expedition && BHBot.settings.autoRevive.contains("e"))) {

                // from char to potion name
                HashMap<Character, String> potionTranslate = new HashMap<>();
                potionTranslate.put('1', "Minor");
                potionTranslate.put('2', "Average");
                potionTranslate.put('3', "Major");

                for (Map.Entry<Integer, Point> item : revivePositions.entrySet()) {
                    Integer slotNum = item.getKey();
                    Point slotPos = item.getValue();

                    if (revived[slotNum - 1]) continue;

                    if (potionsUsed == BHBot.settings.potionLimit) {
                        BHBot.logger.autorevive("Potion limit reached, exiting from Auto Revive");
                        readScreen(SECOND);
                        break;
                    }

                    // If we revive a team member we need to reopen the potion menu again
                    seg = detectCue(cues.get("UnitSelect"), SECOND);
                    if (seg == null) {
                        seg = detectCue(cues.get("Potions"), SECOND * 2);
                        if (seg != null) {
                            clickOnSeg(seg);
                            readScreen(SECOND);

                            // If no potions are needed, we re-enable the Auto function
                            seg = detectCue(cues.get("NoPotions"), SECOND); // Everyone is Full HP
                            if (seg != null) {
                                seg = detectCue(cues.get("Close"), SECOND, new Bounds(300, 330, 500, 400));
                                if (seg != null) {
                                    BHBot.logger.autorevive("None of the team members need a consumable, exiting from autoRevive");
                                    clickOnSeg(seg);
                                    seg = detectCue(cues.get("AutoOff"), SECOND);
                                    clickOnSeg(seg);
                                } else {
                                    BHBot.logger.error("Error while reopening the potions menu: no close button found!");
                                    saveGameScreen("autorevive-no-potions-for-error", img);
                                    restart();
                                }
                                return;
                            }
                        }
                    }

                    readScreen(SECOND);
                    clickInGame(slotPos.x, slotPos.y);
                    readScreen(SECOND);

                    MarvinSegment superHealSeg = detectCue(cues.get("SuperAvailable"));

                    if (superHealSeg != null) {
                        if (detectCue(cues.get("ScrollerRightDisabled")) != null) {
                            BHBot.logger.debug("Slot " + slotNum + " is up for super potion, closing revive window.");
                            seg = detectCue(cues.get("X"));
                            clickOnSeg(seg);
                            readScreen(SECOND);
                            continue;
                        }

                        // If super potion is available, we skip it
                        int superPotionMaxChecks = 10, superPotionCurrentCheck = 0;
                        while (superPotionCurrentCheck < superPotionMaxChecks && detectCue(cues.get("SuperAvailable")) != null) {
                            clickInGame(656, 434);
                            readScreen(500);
                            superPotionCurrentCheck++;
                        }
                    }

                    MarvinSegment reviveSeg = detectCue(cues.get("Revives"), SECOND);
                    MarvinSegment restoreSeg = detectCue(cues.get("Restores"));

                    if (restoreSeg != null) { // we can use one potion, we don't know which one: revive or healing
                        if (reviveSeg == null) { // we can use a revive potion
                            BHBot.logger.debug("Slot " + slotNum + " is up for healing potions, so it does not need revive");
                            seg = detectCue(cues.get("X"));
                            clickOnSeg(seg);
                            readScreen(SECOND);
                            continue;
                        }
                    } else {
                        continue;
                    }

                    // We check what revives are available, and we save the seg for future reuse
                    HashMap<Character, MarvinSegment> availablePotions = new HashMap<>();
                    availablePotions.put('1', detectCue(cues.get("MinorAvailable")));
                    availablePotions.put('2', detectCue(cues.get("AverageAvailable")));
                    availablePotions.put('3', detectCue(cues.get("MajorAvailable")));

                    // No more potions are available
                    if (availablePotions.get('1') == null && availablePotions.get('2') == null && availablePotions.get('3') == null) {
                        BHBot.logger.warn("No potions are avilable, autoRevive well be temporary disabled!");
                        BHBot.settings.autoRevive = new ArrayList<>();
                        BHBot.scheduler.resetIdleTime(true);
                        return;
                    }

                    // We manage tank priority using the best potion we have
                    if (slotNum == (BHBot.settings.tankPosition) &&
                            ((state == State.Trials && BHBot.settings.tankPriority.contains("t")) ||
                                    (state == State.Gauntlet && BHBot.settings.tankPriority.contains("g")) ||
                                    (state == State.Raid && BHBot.settings.tankPriority.contains("r")) ||
                                    (state == State.Expedition && BHBot.settings.tankPriority.contains("e")))) {
                        for (char potion : "321".toCharArray()) {
                            seg = availablePotions.get(potion);
                            if (seg != null) {
                                BHBot.logger.autorevive("Handling tank priority (position: " + BHBot.settings.tankPosition + ") with " + potionTranslate.get(potion) + " revive.");
                                clickOnSeg(seg);
                                readScreen(SECOND);
                                seg = detectCue(cues.get("YesGreen"), SECOND, new Bounds(230, 320, 550, 410));
                                clickOnSeg(seg);
                                revived[BHBot.settings.tankPosition - 1] = true;
                                potionsUsed++;
                                readScreen(SECOND);
                                BHBot.scheduler.resetIdleTime(true);
                                break;
                            }
                        }
                    }

                    if (!revived[slotNum - 1]) { // This is only false when tank priory kicks in
                        for (char potion : BHBot.settings.potionOrder.toCharArray()) {
                            // BHBot.logger.info("Checking potion " + potion);
                            seg = availablePotions.get(potion);
                            if (seg != null) {
                                BHBot.logger.autorevive("Using " + potionTranslate.get(potion) + " revive on slot " + slotNum + ".");
                                clickOnSeg(seg);
                                readScreen(SECOND);
                                seg = detectCue(cues.get("YesGreen"), SECOND, new Bounds(230, 320, 550, 410));
                                clickOnSeg(seg);
                                revived[slotNum - 1] = true;
                                potionsUsed++;
                                readScreen(SECOND);
                                BHBot.scheduler.resetIdleTime(true);
                                break;
                            }
                        }
                    }
                }
            }
        } else { // Impossible to find the potions button
            saveGameScreen("auto-revive-no-potions");
            BHBot.logger.autorevive("Impossible to find the potions button!");
        }

        // If the unit selection screen is still open, we need to close it
        seg = detectCue(cues.get("UnitSelect"), SECOND);
        if (seg != null) {
            seg = detectCue(cues.get("X"), SECOND);
            if (seg != null) {
                clickOnSeg(seg);
                readScreen(SECOND);
            }
        }

        inEncounterTimestamp = Misc.getTime() / 1000; //after reviving we update encounter timestamp as it wasn't updating from processDungeon

        seg = detectCue(cues.get("AutoOff"), SECOND);
        if (seg != null) clickOnSeg(seg);
        BHBot.scheduler.resetIdleTime(true);
    }

    private void closeWorldBoss() {
        MarvinSegment seg;

        sleep(SECOND);
        seg = detectCue(cues.get("X"), 2 * SECOND);
        if (seg != null) {
            clickOnSeg(seg);
        } else {
            BHBot.logger.error("first x Error returning to main screen from World Boss, restarting");
//			return false;
        }

        sleep(SECOND);
        seg = detectCue(cues.get("YesGreen"), 2 * SECOND);
        if (seg != null) {
            clickOnSeg(seg);
        } else {
            BHBot.logger.error("yesgreen Error returning to main screen from World Boss, restarting");
//			return false;
        }

        sleep(SECOND);
        seg = detectCue(cues.get("X"), 2 * SECOND);
        if (seg != null) {
            clickOnSeg(seg);
        } else {
            BHBot.logger.error("second x Error returning to main screen from World Boss, restarting");
//			return false;
        }

//		return true;
    }

    private void updateFamiliarCounter(String fam) {
        String familiarToUpdate = "";
        String updatedFamiliar = "";

        for (String fa : BHBot.settings.familiars) { //cycle through array
            String fString = fa.toUpperCase().split(" ")[0]; //case sensitive for a match so convert to upper case
            int currentCounter = Integer.parseInt(fa.split(" ")[1]); //set the bribe counter to an int
            if (fam.equals(fString)) { //when match is found from the function
                familiarToUpdate = fa; //write current status to String
                currentCounter--; // decrease the counter
                updatedFamiliar = (fString.toLowerCase() + " " + currentCounter); //update new string with familiar name and decrease counter
//				BHBot.logger.info("Before: " + familiarToUpdate);
//				BHBot.logger.info("Updated: " + updatedFamiliar);
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

//	        BHBot.logger.info(inputStr); // check that it's inputted right

            //find containing string and update with the output string from the function above
            if (inputStr.contains(familiarToUpdate)) {
                inputStr = inputStr.replace(familiarToUpdate, updatedFamiliar);
            }

            // write the string from memory over the existing file
            // a bit risky for crashes
            FileOutputStream fileOut = new FileOutputStream("settings.ini");
            fileOut.write(inputStr.getBytes());
            fileOut.close();

            BHBot.settings.load();  //reload the new settings file so the counter will be updated for the next bribe

        } catch (Exception e) {
            System.out.println("Problem writing to settings file");
        }
    }

    private void settingsUpdate(String string, String updatedString) {

        try {
            // input the file content to the StringBuffer "input"
            BufferedReader file = new BufferedReader(new FileReader(Settings.configurationFile));
            String line;
            StringBuilder inputBuffer = new StringBuilder();

            //print lines to string with linebreaks
            while ((line = file.readLine()) != null) {
                inputBuffer.append(line);
                inputBuffer.append(System.getProperty("line.separator"));
            }
            String inputStr = inputBuffer.toString(); //load lines to string
            file.close();

//	        BHBot.logger.info(inputStr); // check that it's inputted right

            //find containing string and update with the output string from the function above
            if (inputStr.contains(string)) {
                inputStr = inputStr.replace(string, updatedString);
                BHBot.logger.info("Replaced '" + string + "' with '" + updatedString + "' in " + Settings.configurationFile);
            } else BHBot.logger.error("Error finding string: " + string);

            // write the string from memory over the existing file
            // a bit risky for crashes
            FileOutputStream fileOut = new FileOutputStream(Settings.configurationFile);
            fileOut.write(inputStr.getBytes());
            fileOut.close();

            BHBot.settings.load();  //reload the new settings file so the counter will be updated for the next bribe

        } catch (Exception e) {
            System.out.println("Problem writing to settings file");
        }
    }

    private void updateActivityCounter(String activity) {
        String typeToUpdate = "";
        String updatedType = "";

        if ("Dungeon".equals(activity)) {
            String numberRun = BHBot.settings.dungeonsRun.split(" ")[0]; //case sensitive for a match so convert to upper case
            int currentCounter = Integer.parseInt(BHBot.settings.dungeonsRun.split(" ")[1]); //set the bribe counter to an int
            typeToUpdate = numberRun + " " + currentCounter; //write current status to String
            currentCounter++; // decrease the counter
            updatedType = (numberRun + " " + currentCounter); //update new string with familiar name and decrease counter
            BHBot.logger.info("Before: " + typeToUpdate);
            BHBot.logger.info("Updated: " + updatedType);
        }

        if ("World Boss".equals(activity)) {
            String numberRun = BHBot.settings.worldBossRun.split(" ")[0]; //case sensitive for a match so convert to upper case
            int currentCounter = Integer.parseInt(BHBot.settings.worldBossRun.split(" ")[1]); //set the bribe counter to an int
            typeToUpdate = numberRun + " " + currentCounter; //write current status to String
            currentCounter++; // decrease the counter
            updatedType = (numberRun + " " + currentCounter); //update new string with familiar name and decrease counter
            BHBot.logger.info("Before: " + typeToUpdate);
            BHBot.logger.info("Updated: " + updatedType);
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

//	        BHBot.logger.info(inputStr); // check that it's inputted right

            //find containing string and update with the output from the function above
            if (inputStr.contains(typeToUpdate)) {
                inputStr = inputStr.replace(typeToUpdate, updatedType);
            }

            // write the string from memory over the existing file
            // a bit risky for crashes
            FileOutputStream fileOut = new FileOutputStream("settings.ini");
            fileOut.write(inputStr.getBytes());
            fileOut.close();

            BHBot.settings.load();  //reload the new settings file so the counter will be updated for the next bribe

        } catch (Exception e) {
            System.out.println("Problem writing to settings file");
        }
    }

    /**
     * @param z and integer with the desired zone.
     * @param d and integer with the desired dungeon.
     * @return null in case dungeon parameter is malformed (can even throw an exception)
     */
    private Point getDungeonIconPos(int z, int d) {
        if (z < 1 || z > 10) return null;
        if (d < 1 || d > 4) return null;

        switch (z) {
            case 1: // zone 1
                switch (d) {
                    case 1:
                        return new Point(240, 350);
                    case 2:
                        return new Point(580, 190);
                    case 3:
                        return new Point(660, 330);
                    case 4:
                        return new Point(410, 230);
                }
                break;
            case 2: // zone 2
                switch (d) {
                    case 1:
                        return new Point(215, 270);
                    case 2:
                        return new Point(550, 150);
                    case 3:
                        return new Point(515, 380);
                    case 4:
                        return new Point(400, 270);
                }
                break;
            case 3: // zone 3
                switch (d) {
                    case 1:
                        return new Point(145, 200);
                    case 2:
                        return new Point(430, 300);
                    case 3:
                        return new Point(565, 375);
                    case 4:
                        return new Point(570, 170);
                }
                break;
            case 4: // zone 4
                switch (d) {
                    case 1:
                        return new Point(300, 400);
                    case 2:
                        return new Point(260, 200);
                    case 3:
                        return new Point(650, 200);
                    case 4:
                        return new Point(400, 270);
                }
                break;
            case 5: // zone 5
                switch (d) {
                    case 1:
                        return new Point(150, 200);
                    case 2:
                        return new Point(410, 380);
                    case 3:
                        return new Point(630, 240);
                    case 4:
                        return new Point(550, 150);
                }
                break;
            case 6: // zone 6
                switch (d) {
                    case 1:
                        return new Point(150, 220);
                    case 2:
                        return new Point(500, 400);
                    case 3:
                        return new Point(550, 120);
                    case 4:
                        return new Point(400, 270);
                }
                break;
            case 7: // zone 7
                switch (d) {
                    case 1:
                        return new Point(215, 315);
                    case 2:
                        return new Point(570, 165);
                    case 3:
                        return new Point(400, 290);
                    case 4:
                        BHBot.logger.warn("Zone 7 only has 3 dungeons, falling back to z7d2");
                        return new Point(650, 400);
                }
                break;
            case 8: // zone 8
                switch (d) {
                    case 1:
                        return new Point(570, 170);
                    case 2:
                        return new Point(650, 390);
                    case 3:
                        return new Point(250, 370);
                    case 4:
                        BHBot.logger.warn("Zone 8 only has 3 dungeons, falling back to z8d2");
                        return new Point(570, 340);
                }
                break;
            case 9:
                switch (d) {
                    case 1:
                        return new Point(310, 165);
                    case 2:
                        return new Point(610, 190);
                    case 3:
                        return new Point(375, 415);
                    case 4:
                        BHBot.logger.warn("Zone 9 only has 3 dungeons, falling back to z9d2");
                        return new Point(610, 190);
                }
                break;
            case 10:
                switch (d) {
                    case 1:
                        return new Point(468, 389);
                    case 2:
                        return new Point(428, 261);
                    case 3:
                        return new Point(145, 200);
                    case 4:
                        return new Point(585, 167);
                }
        }


        return null;
    }

    /**
     * Function to return the name of the portal for console output
     */
    private String getExpeditionPortalName(int currentExpedition, String targetPortal) {
        if (currentExpedition > 5) {
            BHBot.logger.error("Unexpected expedition int in getExpeditionPortalName: " + currentExpedition);
            return null;
        }

        if (!"p1".equals(targetPortal) && !"p2".equals(targetPortal)
                && !"p3".equals(targetPortal) && !"p4".equals(targetPortal)) {
            BHBot.logger.error("Unexpected target portal in getExpeditionPortalName: " + targetPortal);
            return null;
        }

        switch (currentExpedition) {
            case 1: // Hallowed Dimension
                switch (targetPortal) {
                    case "p1":
                        return "Googarum's";
                    case "p2":
                        return "Svord's";
                    case "p3":
                        return "Twimbos";
                    case "p4":
                        return "X5-T34M's";
                    default:
                        return null;
                }
            case 2: // Inferno dimension
                switch (targetPortal) {
                    case "p1":
                        return "Raleib's";
                    case "p2":
                        return "Blemo's";
                    case "p3":
                        return "Gummy's";
                    case "p4":
                        return "Zarlocks";
                    default:
                        return null;
                }
            case 3:
                switch (targetPortal) {
                    case "p1":
                        return "Zorgo Crossing";
                    case "p2":
                        return "Yackerz Tundra";
                    case "p3":
                        return "Vionot Sewer";
                    case "p4":
                        return "Grampa Hef's Heart";
                    default:
                        return null;
                }
            case 4: // Idol dimension
                switch (targetPortal) {
                    case "p1":
                        return "Blublix";
                    case "p2":
                        return "Mowhi";
                    case "p3":
                        return "Wizbot";
                    case "p4":
                        return "Astamus";
                    default:
                        return null;
                }
            case 5: // Battle Bards!
                switch (targetPortal) {
                    case "p1":
                        return "Hero Fest";
                    case "p2":
                        return "Burning Fam";
                    case "p3":
                        return "Melvapaloozo";
                    case "p4":
                        return "Bitstock";
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    /**
     * @param targetPortal in standard format, e.g. "h4/i4".
     * @return null in case dungeon parameter is malformed (can even throw an exception)
     */
    private Point getExpeditionIconPos(int currentExpedition, String targetPortal) {
        if (targetPortal.length() != 2) {
            BHBot.logger.error("targetPortal length Mismatch in getExpeditionIconPos");
            return null;
        }

        String portalName = getExpeditionPortalName(currentExpedition, targetPortal);
//        if (currentExpedition == 1) {
//            portalName = "Hallowed";
//        } else if (currentExpedition == 2) {
//            portalName = "Inferno";
//        } else if (currentExpedition == 3) {
//            portalName = "Jammie";
//        } else if (currentExpedition == 4) {
//            portalName = "Idol";
//        } else if (currentExpedition == 5) {
//            portalName = "Battle Bards";
//        } else {
//            BHBot.logger.error("Unknown Expedition in getExpeditionIconPos " + currentExpedition);
//            saveGameScreen("unknown-expedition");
//            return null;
//        }

        if (!"p1".equals(targetPortal) && !"p2".equals(targetPortal)
                && !"p3".equals(targetPortal) && !"p4".equals(targetPortal)) {
            BHBot.logger.error("Unexpected target portal in getExpeditionIconPos: " + targetPortal);
            return null;
        }

        int portalInt;
        switch (targetPortal) {
            case "p1":
                portalInt = 1;
                break;
            case "p2":
                portalInt = 2;
                break;
            case "p3":
                portalInt = 3;
                break;
            case "p4":
                portalInt = 4;
                break;
            default:
                portalInt = 0;
                break;
        }

        // we check for white border to understand if the portal is enabled
        Point[] portalCheck = new Point[4];
        Point[] portalPosition = new Point[4];
        Color[] colorCheck = new Color[4];
        boolean[] portalEnabled = new boolean[4];

        if (currentExpedition == 1) { // Hallowed

            portalCheck[0] = new Point(190, 146); //Googarum
            portalCheck[1] = new Point(484, 205); //Svord
            portalCheck[2] = new Point(328, 339); //Twimbo
            portalCheck[3] = new Point(641, 345); //X5-T34M

            portalPosition[0] = new Point(200, 200); //Googarum
            portalPosition[1] = new Point(520, 220); //Svord
            portalPosition[2] = new Point(360, 360); //Twimbo
            portalPosition[3] = new Point(650, 380); //X5-T34M

            colorCheck[0] = Color.WHITE;
            colorCheck[1] = Color.WHITE;
            colorCheck[2] = Color.WHITE;
            colorCheck[3] = Color.WHITE;
        } else if (currentExpedition == 2) { // Inferno
            portalCheck[0] = new Point(185, 206); // Raleib
            portalCheck[1] = new Point(570, 209); // Blemo
            portalCheck[2] = new Point(383, 395); // Gummy
            portalCheck[3] = new Point(381, 265); // Zarlock

            portalPosition[0] = new Point(200, 195); // Raleib
            portalPosition[1] = new Point(600, 195); // Blemo
            portalPosition[2] = new Point(420, 405); // Gummy
            portalPosition[3] = new Point(420, 270); // Zarlock

            colorCheck[0] = Color.WHITE;
            colorCheck[1] = Color.WHITE;
            colorCheck[2] = Color.WHITE;
            colorCheck[3] = Color.WHITE;
        } else if (currentExpedition == 3) { // Jammie
            portalCheck[0] = new Point(145, 187); // Zorgo
            portalCheck[1] = new Point(309, 289); // Yackerz
            portalCheck[2] = new Point(474, 343); // Vionot
            portalCheck[3] = new Point(621, 370); // Grampa

            portalPosition[0] = new Point(170, 200); // Zorgo
            portalPosition[1] = new Point(315, 260); // Yackerz
            portalPosition[2] = new Point(480, 360); // Vinot
            portalPosition[3] = new Point(635, 385); // Grampa

            colorCheck[0] = Color.WHITE;
            colorCheck[1] = Color.WHITE;
            colorCheck[2] = Color.WHITE;
            colorCheck[3] = Color.WHITE;
        } else if (currentExpedition == 4) { // Idol
            portalCheck[0] = new Point(370, 140); // Blublix
            portalCheck[1] = new Point(226, 369); // Mowhi
            portalCheck[2] = new Point(534, 350); // Wizbot
            portalCheck[3] = new Point(370, 324); // Astamus

            portalPosition[0] = new Point(400, 165); // Blublix
            portalPosition[1] = new Point(243, 385); // Mowhi
            portalPosition[2] = new Point(562, 375); // Wizbot
            portalPosition[3] = new Point(400, 318); // Astamus

            colorCheck[0] = Color.WHITE;
            colorCheck[1] = Color.WHITE;
            colorCheck[2] = Color.WHITE;
            colorCheck[3] = new Color(251, 201, 126);
        } else { // Battle Bards!
            portalCheck[0] = new Point(387, 152); // Hero Fest
            portalCheck[1] = new Point(253, 412); // Burning Fam
            portalCheck[2] = new Point(568, 418); // Melvapaloozo
            portalCheck[3] = new Point(435, 306); // Bitstock

            portalPosition[0] = new Point(402, 172); // Hero Fest
            portalPosition[1] = new Point(240, 371); // Burning Fam
            portalPosition[2] = new Point(565, 383); // Melvapaloozo
            portalPosition[3] = new Point(396, 315); // Bitstock

            colorCheck[0] = Color.WHITE;
            colorCheck[1] = Color.WHITE;
            colorCheck[2] = new Color(255, 254, 255); //Melvapaloozo is one bit off pure white for some reason
            colorCheck[3] = Color.WHITE;
        }

        // We check which of the portals are enabled
        for (int i = 0; i <= 3; i++) {
            Color col = new Color(img.getRGB(portalCheck[i].x, portalCheck[i].y));
            portalEnabled[i] = col.equals(colorCheck[i]);
        }

        if (portalEnabled[portalInt - 1]) {
            return portalPosition[portalInt - 1];
        }

        // If the desired portal is not enabled, we try to find the highest enabled one
        int i = 3;
        while (i >= 0) {
            if (portalEnabled[i]) {
                BHBot.logger.warn(portalName + " is not available! Falling back on p" + (i + 1) + "...");
                return portalPosition[i];
            }
            i--; //cycle down through 4 - 1 until we return an activated portal
        }

        return null;
    }

    /**
     * Check world boss inputs are valid
     **/
    private boolean checkWorldBossInput() {
        boolean failed = false;
        int passed = 0;

        String worldBossType = BHBot.settings.worldBossSettings.get(0);
        int worldBossTier = Integer.parseInt(BHBot.settings.worldBossSettings.get(2));

        //check name
        if ("o".equals(worldBossType) || "n".equals(worldBossType) || "m".equals(worldBossType)
                || "3".equals(worldBossType) || "b".equals(worldBossType)) {
            passed++;
        } else {
            BHBot.logger.error("Invalid world boss name, check settings file");
            failed = true;
        }

        //check tier
        if ("o".equals(worldBossType) || "n".equals(worldBossType)) {
            if (worldBossTier >= 3 && worldBossTier <= 9) {
                passed++;
            } else {
                BHBot.logger.error("Invalid world boss tier for Orlang or Nether, must be between 3 and 9");
                failed = true;
            }
        } else if ("m".equals(worldBossType) || "3".equals(worldBossType)) {
            if (worldBossTier >= 10 && worldBossTier <= 11) {
                passed++;
            } else {
                BHBot.logger.error("Invalid world boss tier for Melvin, 3xt3rmin4tion or Brimstone, must be T10 or higher.");
                failed = true;
            }
        } else if ("b".equals(worldBossType)) {
            if (worldBossTier == 11) {
                passed++;
            } else {
                BHBot.logger.error("Invalid world boss tier for Brimstone, must be T11.");
                failed = true;
            }
        }

        //warn user if timer is over 5 minutes
        if (BHBot.settings.worldBossTimer <= 300) {
            passed++;
        } else {
            BHBot.logger.warn("Warning: Timer longer than 5 minutes");
        }

        return !failed && passed == 3;


    }

    /**
     * Returns dungeon and difficulty level, e.g. 'z2d4 2'.
     */
    private String decideDungeonRandomly() {

        if ("3".equals(new SimpleDateFormat("u").format(new Date())) &&
                BHBot.settings.wednesdayDungeons.size() > 0) { // if its wednesday and wednesdayRaids is not empty
            return BHBot.settings.wednesdayDungeons.next();
        } else {
            return BHBot.settings.dungeons.next();
        }
    }

    /**
     * Returns raid type (1, 2 or 3) and difficulty level (1, 2 or 3, which correspond to normal, hard and heroic), e.g. '1 3'.
     */
    private String decideRaidRandomly() {
        if ("3".equals(new SimpleDateFormat("u").format(new Date())) &&
                BHBot.settings.wednesdayRaids.size() > 0) { // if its wednesday and wednesdayRaids is not empty
            return BHBot.settings.wednesdayRaids.next();
        } else {
            return BHBot.settings.raids.next();
        }
    }

    /**
     * Returns number of zone that is currently selected in the quest window (we need to be in the quest window for this to work).
     * Returns 0 in case zone could not be read (in case we are not in the quest window, for example).
     */
    private int readCurrentZone() {
        if (detectCue("Zone1") != null)
            return 1;
        else if (detectCue("Zone2") != null)
            return 2;
        else if (detectCue("Zone3") != null)
            return 3;
        else if (detectCue("Zone4") != null)
            return 4;
        else if (detectCue("Zone5") != null)
            return 5;
        else if (detectCue("Zone6") != null)
            return 6;
        else if (detectCue("Zone7") != null)
            return 7;
        else if (detectCue("Zone8") != null)
            return 8;
        else if (detectCue("Zone9") != null)
            return 9;
        else if (detectCue("Zone10") != null)
            return 10;
        else
            return 0;
    }

	/*private int checkFamiliarCounter(String fam) { //returns current catch count for given familiar from the settings file
		int catchCount = 0;
		for (String f : BHBot.settings.familiars) { //cycle familiars defined in settings
				String fString = f.toUpperCase().split(" ")[0]; //stringify the familiar name
				if (fam.equals(fString)) { // on match return
					catchCount = Integer.parseInt(f.split(" ")[1]);
				}
			}
		return catchCount;
		}*/

    void expeditionReadTest() {
        String expedition = BHBot.settings.expeditions.next();
        if (expedition != null) {
            expedition = expedition.split(" ")[0];
            BHBot.logger.info("Expedition chosen: " + expedition);
        }
    }

    /**
     * Note: world boss window must be open for this to work!
     * <p>
     * Returns false in case it failed.
     */
    private boolean handleWorldBossSelection(WorldBoss desiredWorldBoss) {

        MarvinSegment seg;

        // we refresh the screen
        readScreen(SECOND);

        int wbUnlocked = 0;
        int desiredWB = desiredWorldBoss.getNumber();

        // we get the grey dots on the raid selection popup
        List<MarvinSegment> wbDotsList = FindSubimage.findSubimage(img, cues.get("cueRaidLevelEmpty").im, 1.0, true, false, 0, 0, 0, 0);
        // we update the number of unlocked raids
        wbUnlocked += wbDotsList.size();

        // A  temporary variable to save the position of the current selected raid
        int selectedWBX1;

        seg = detectCue(cues.get("RaidLevel"));
        if (seg != null) {
            wbUnlocked += 1;
            selectedWBX1 = seg.getX1();
            wbDotsList.add(seg);
        } else {
            BHBot.logger.error("Impossible to detect the currently selected green cue!");
            return false;
        }

        WorldBoss unlockedWB = WorldBoss.fromNumber(wbUnlocked);
        if (unlockedWB == null) {
            BHBot.logger.error("Unknown unlocked World Boss integer: " + wbUnlocked);
            return false;
        }

        BHBot.logger.debug("Detected: WB " + unlockedWB.getName() + " unlocked");

        if (wbUnlocked < desiredWB) {
            BHBot.logger.warn("World Boss selected in settings (" + desiredWorldBoss.getName() + ") is higher than world boss unlocked, running highest available (" + unlockedWB.getName() + ")");
            desiredWB = wbUnlocked;
        }

        // we sort the list of dots, using the x1 coordinate
        wbDotsList.sort(comparing(MarvinSegment::getX1));

        int selectedWB = 0;
        for (MarvinSegment raidDotSeg : wbDotsList) {
            selectedWB++;
            if (raidDotSeg.getX1() == selectedWBX1) break;
        }

        WorldBoss wbSelected = WorldBoss.fromNumber(selectedWB);
        if (wbSelected == null) {
            BHBot.logger.error("Unknown selected World Boss integer: " + wbUnlocked);
            return false;
        }

        BHBot.logger.debug("WB selected is " + wbSelected.getName());

        if (selectedWB == 0) { // an error!
            BHBot.logger.error("It was impossible to determine the currently selected raid!");
            return false;
        }

        if (selectedWB != desiredWB) {
            // we need to change the raid type!
            BHBot.logger.info("Changing from WB" + wbSelected.getName() + " to WB" + desiredWorldBoss.getName());
            // we click on the desired cue
            clickOnSeg(wbDotsList.get(desiredWB - 1));
        }

        return true;
    }

    /**
     * Note: raid window must be open for this to work!
     * <p>
     * Returns false in case it failed.
     */
    private boolean handleRaidSelection(int desiredRaid, int difficulty) {

        MarvinSegment seg;

        // we refresh the screen
        readScreen(SECOND);

        int raidUnlocked = 0;
        // we get the grey dots on the raid selection popup
        List<MarvinSegment> raidDotsList = FindSubimage.findSubimage(img, cues.get("cueRaidLevelEmpty").im, 1.0, true, false, 0, 0, 0, 0);
        // we update the number of unlocked raids
        raidUnlocked += raidDotsList.size();

        // Is only R1 unlocked?
        boolean onlyR1 = false;
        if (raidUnlocked == 0 && detectCue(cues.get("Raid1Name")) != null) {
            raidUnlocked += 1;
            onlyR1 = true;
        }

        // A  temporary variable to save the position of the current selected raid
        int selectedRaidX1 = 0;

        // we look for the the currently selected raid, the green dot
        if (!onlyR1) {
            seg = detectCue(cues.get("RaidLevel"));
            if (seg != null) {
                raidUnlocked += 1;
                selectedRaidX1 = seg.getX1();
                raidDotsList.add(seg);
            } else {
                BHBot.logger.error("Impossible to detect the currently selected grey cue!");
                return false;
            }
        }

        BHBot.logger.debug("Detected: R" + raidUnlocked + " unlocked");

        if (raidUnlocked < desiredRaid) {
            BHBot.logger.warn("Raid selected in settings (R" + desiredRaid + ") is higher than raid level unlocked, running highest available (R" + raidUnlocked + ")");
            desiredRaid = raidUnlocked;
        }

        BHBot.logger.info("Attempting R" + desiredRaid + " " + (difficulty == 1 ? "Normal" : difficulty == 2 ? "Hard" : "Heroic"));

        // we sort the list of dots, using the x1 coordinate
        raidDotsList.sort(comparing(MarvinSegment::getX1));

        int selectedRaid = 0;
        if (!onlyR1) {
            for (MarvinSegment raidDotSeg : raidDotsList) {
                selectedRaid++;
                if (raidDotSeg.getX1() == selectedRaidX1) break;
            }
        } else {
            selectedRaid = 1;
        }

        BHBot.logger.debug("Raid selected is R" + selectedRaid);

        if (selectedRaid == 0) { // an error!
            BHBot.logger.error("It was impossible to determine the currently selected raid!");
            return false;
        }

        if (!onlyR1 && (selectedRaid != desiredRaid)) {
            // we need to change the raid type!
            BHBot.logger.info("Changing from R" + selectedRaid + " to R" + desiredRaid);
            // we click on the desired cue
            clickOnSeg(raidDotsList.get(desiredRaid - 1));
        }

        return true;
    }

    /**
     * Takes screenshot of current game and saves it to disk to a file with a given prefix (date will be added, and optionally a number at the end of file name).
     * In case of failure, it will just ignore the error.
     *
     * @return name of the path in which the screenshot has been saved (successfully or not)
     */
    String saveGameScreen(String prefix) {
        return saveGameScreen(prefix, null, takeScreenshot(true));
    }

    private String saveGameScreen(String prefix, BufferedImage img) {
        return saveGameScreen(prefix, null, img);
    }

    private String saveGameScreen(String prefix, String subFolder) {
        return saveGameScreen(prefix, subFolder, takeScreenshot(true));
    }

    private String saveGameScreen(String prefix, String subFolder, BufferedImage img) {

        // sub-folder logic management
        String screenshotPath = BHBot.screenshotPath;
        if (subFolder != null) {
            File subFolderPath = new File(BHBot.screenshotPath + subFolder + "/");
            if (!subFolderPath.exists()) {
                if (!subFolderPath.mkdir()) {
                    BHBot.logger.error("Impossible to create screenshot sub folder in " + subFolder);
                    return null;
                } else {
                    try {
                        BHBot.logger.info("Created screenshot sub-folder " + subFolderPath.getCanonicalPath());
                    } catch (IOException e) {
                        BHBot.logger.error("Error while getting Canonical Path for newly created screenshots sub-folder", e);
                    }
                }
            }
            screenshotPath += subFolder + "/";
        }

        Date date = new Date();
        String name = prefix + "_" + dateFormat.format(date) + ".png";
        int num = 0;
        File f = new File(screenshotPath + name);
        while (f.exists()) {
            num++;
            name = prefix + "_" + dateFormat.format(date) + "_" + num + ".png";
            f = new File(screenshotPath + name);
        }

        // save screen shot:
        try {
            ImageIO.write(img, "png", f);
        } catch (Exception e) {
            BHBot.logger.error("Impossible to take a screenshot!");
        }

        return f.getPath();
    }

    private void contributeFamiliarShoot(String shootName, FamiliarType familiarType) {

        HttpClient httpClient = HttpClients.custom().useSystemProperties().build();

        final HttpPost post = new HttpPost("https://script.google.com/macros/s/AKfycby-tCXZ6MHt_ZSUixCcNbYFjDuri6WvljomLgGy_m5lLZw1y5fZ/exec");

        // we generate a sub image with just the name of the familiar
        readScreen(SECOND);
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

        if (familiarTxtColor == 0) return;

        BufferedImage zoneImg = img.getSubimage(105, 60, 640, 105);

		/*File zoneImgTmp = new File("tmp-NAME-ZONE.png");
		try {
			ImageIO.write(zoneImg, "png", zoneImgTmp);
		} catch (IOException e) {
			BHBot.logger.error("", e);
		}*/

        int minX = zoneImg.getWidth();
        int minY = zoneImg.getHeight();
        int maxY = 0;
        int maxX = 0;

        int[][] pixelMatrix = Misc.convertTo2D(zoneImg);
        for (int y = 0; y < zoneImg.getHeight(); y++) {
            for (int x = 0; x < zoneImg.getWidth(); x++) {
                if (pixelMatrix[x][y] == familiarTxtColor) {
                    if (y < minY) minY = y;
                    if (x < minX) minX = x;
                    if (y > maxY) maxY = y;
                    if (x > maxX) maxX = x;
                } else {
                    zoneImg.setRGB(x, y, 0);
                }
            }

        }

        BufferedImage nameImg = zoneImg.getSubimage(minX, minY, maxX - minX, maxY - minY);
//		zoneImgTmp.delete();

        File nameImgFile = new File(shootName + "-ctb.png");
        try {
            ImageIO.write(nameImg, "png", nameImgFile);
        } catch (IOException e) {
            BHBot.logger.error("Error while creating contribution file", e);
        }

        MimetypesFileTypeMap ftm = new MimetypesFileTypeMap();
        ContentType ct = ContentType.create(ftm.getContentType(nameImgFile));

        List<NameValuePair> params = new ArrayList<>(3);
        params.add(new BasicNameValuePair("mimeType", ct.toString()));
        params.add(new BasicNameValuePair("name", nameImgFile.getName()));
        params.add(new BasicNameValuePair("data", Misc.encodeFileToBase64Binary(nameImgFile)));

        try {
            post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            BHBot.logger.error("Error while encoding POST request in contribution", e);
        }

        try {
            httpClient.execute(post);
        } catch (IOException e) {
            BHBot.logger.error("Error while executing HTTP request in contribution", e);
        }

        if (!nameImgFile.delete()) {
            BHBot.logger.warn("Impossible to delete " + nameImgFile.getAbsolutePath());
        }

    }

    /**
     * Will detect and handle (close) in-game private message (from the current screen capture). Returns true in case PM has been handled.
     */
    private boolean handlePM() {
        if (detectCue(cues.get("InGamePM")) != null) {
            MarvinSegment seg = detectCue(cues.get("X"), 5 * SECOND);
            if (seg == null) {
                BHBot.logger.error("Error: in-game PM window detected, but no close button found. Restarting...");
                restart(); //*** problem: after a call to this, it will return to the main loop. It should call "continue" inside the main loop or else there could be other exceptions!
                return true;
            }

            try {
                String pmFileName = saveGameScreen("pm");
                if (BHBot.settings.enablePushover && BHBot.settings.poNotifyPM) {
                    if (pmFileName != null) {
                        sendPushOverMessage("New PM", "You've just received a new PM, check it out!", MessagePriority.NORMAL, new File(pmFileName));
                    } else {
                        sendPushOverMessage("New PM", "You've just received a new PM, check it out!", MessagePriority.NORMAL, null);
                    }
                }
                clickOnSeg(seg);
            } catch (Exception e) {
                // ignore it
            }
            return true;
        } else {
            return false; // no PM detected
        }
    }

    /**
     * Handles popup that tells you that your team is not complete. Happens when some friend left you.
     * This method will attempt to click on "Auto" button to refill your team.
     * Note that this can happen in raid and GvG only, since in other games (PvP, Gauntlet/Trials) you can use only familiars.
     * In GvG, on the other hand, there is additional dialog possible (which is not possible in raid): "team not ordered" dialog.
     *
     * @return true in case emergency restart is needed.
     */
    private boolean handleTeamMalformedWarning() {

        // We look for the team text on top of the text pop-up
        MarvinSegment seg = detectCue(cues.get("Team"), SECOND * 3, new Bounds(330, 135, 480, 180));
        if (seg == null) {
            return false;
        }

        if (detectCue(cues.get("TeamNotFull"), SECOND) != null || detectCue(cues.get("TeamNotOrdered"), SECOND) != null) {
            readScreen(SECOND);
            seg = detectCue(cues.get("No"), 2 * SECOND);
            if (seg == null) {
                BHBot.logger.error("Error: 'Team not full/ordered' window detected, but no 'No' button found. Restarting...");
                return true;
            }
            clickOnSeg(seg);
            readScreen();

            seg = detectCue(cues.get("AutoTeam"), 2 * SECOND);
            if (seg == null) {
                BHBot.logger.error("Error: 'Team not full/ordered' window detected, but no 'Auto' button found. Restarting...");
                return true;
            }
            clickOnSeg(seg);

            readScreen();
            seg = detectCue(cues.get("Accept"), 2 * SECOND);
            if (seg == null) {
                BHBot.logger.error("Error: 'Team not full/ordered' window detected, but no 'Accept' button found. Restarting...");
                return true;
            }

            String message = "'Team not full/ordered' dialog detected and handled - team has been auto assigned!";

            if (BHBot.settings.enablePushover && BHBot.settings.poNotifyErrors) {
                String teamScreen = saveGameScreen("auto-team");
                File teamFile = teamScreen != null ? new File(teamScreen) : null;
                sendPushOverMessage("Team auto assigned", message, "siren", MessagePriority.NORMAL, teamFile);
                if (teamFile != null && !teamFile.delete())
                    BHBot.logger.warn("Impossible to delete tmp error img file for team auto assign");
            }

            clickOnSeg(seg);

            BHBot.logger.info(message);
        }

        return false; // all OK
    }

    private boolean handleGuildLeaveConfirm() {
        readScreen();
        if (detectCue(cues.get("GuildLeaveConfirm"), SECOND * 3) != null) {
            sleep(500); // in case popup is still sliding downward
            readScreen();
            MarvinSegment seg = detectCue(cues.get("YesGreen"), 10 * SECOND);
            if (seg == null) {
                BHBot.logger.error("Error: 'Guild Leave Confirm' window detected, but no 'Yes' green button found. Restarting...");
                return true;
            }
            clickOnSeg(seg);
            sleep(2 * SECOND);

            BHBot.logger.info("'Guild Leave' dialog detected and handled!");
        }

        return false; // all ok
    }

    private Boolean handleDisabledBattles() {
        readScreen();
        if (detectCue(cues.get("DisabledBattles"), SECOND * 3) != null) {
            sleep(500); // in case popup is still sliding downward
            readScreen();
            MarvinSegment seg = detectCue(cues.get("Close"), 10 * SECOND);
            if (seg == null) {
                BHBot.logger.error("Error: 'Disabled battles' popup detected, but no 'Close' blue button found. Restarting...");
                return null;
            }
            clickOnSeg(seg);
            sleep(2 * SECOND);

            BHBot.logger.info("'Disabled battles' popup detected and handled!");
            return true;
        }

        return false; // all ok, battles are enabled
    }

    /**
     * Will check if "Not enough energy" popup is open. If it is, it will automatically close it and close all other windows
     * until it returns to the main screen.
     *
     * @return true in case popup was detected and closed.
     */
    private boolean handleNotEnoughEnergyPopup(@SuppressWarnings("SameParameterValue") int delay, State state) {
        MarvinSegment seg = detectCue(cues.get("NotEnoughEnergy"), delay);
        if (seg != null) {
            // we don't have enough energy!
            BHBot.logger.warn("Problem detected: insufficient energy to attempt " + state + ". Cancelling...");
            closePopupSecurely(cues.get("NotEnoughEnergy"), cues.get("No"));


            if (state.equals(State.WorldBoss)) {
                closePopupSecurely(cues.get("WorldBossSummonTitle"), cues.get("X"));

                closePopupSecurely(cues.get("WorldBossTitle"), cues.get("X"));
            } else {
                closePopupSecurely(cues.get("AutoTeam"), cues.get("X"));

                // if D4 close the dungeon info window, else close the char selection screen
                if (specialDungeon) {
                    seg = detectCue(cues.get("X"), 5 * SECOND);
                    if (seg != null)
                        clickOnSeg(seg);
                    specialDungeon = false;
                } else {
                    // close difficulty selection screen:
                    closePopupSecurely(cues.get("Normal"), cues.get("X"));
                }

                // close zone view window:
                closePopupSecurely(cues.get("ZonesButton"), cues.get("X"));
            }

            return true;
        } else {
            return false;
        }
    }

    /**
     * Will check if "Not enough tokens" popup is open. If it is, it will automatically close it and close all other windows
     * until it returns to the main screen.
     *
     * @return true in case popup was detected and closed.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean handleNotEnoughTokensPopup(boolean closeTeamWindow) {
        MarvinSegment seg = detectCue("NotEnoughTokens");

        if (seg != null) {
            BHBot.logger.warn("Not enough token popup detected! Closing trial window.");

            if (!closePopupSecurely(cues.get("NotEnoughTokens"), cues.get("No"))) {
                BHBot.logger.error("Impossible to close the 'Not Enough Tokens' pop-up window. Restarting");
                return false;
            }

            if (closeTeamWindow) {
                if (!closePopupSecurely(cues.get("Accept"), cues.get("X"))) {
                    BHBot.logger.error("Impossible to close the team window when no tokens are available. Restarting");
                    return false;
                }
            }

            if (!closePopupSecurely(cues.get("TrialsOrGauntletWindow"), cues.get("X"))) {
                BHBot.logger.error("Impossible to close the 'TrialsOrGauntletWindow' window. Restarting");
                return false;
            }
        }
        return true;
    }

    /**
     * This method will handle the success threshold based on the state
     * @param state the State used to check the success threshold
     */
    private void handleSuccessTreshold(State state) {

        // We only handle Trials and Gautlets
        if (state != State.Gauntlet && state != State.Trials) return;

        BHBot.logger.debug("Victories in a row for " + state + " is " + counters.get(state).getVictoriesInARow());

        // We make sure that we have a setting for the current state
        if (BHBot.settings.successTreshold.containsKey(state.getShortcut())) {
            Map.Entry<Integer, Integer> successTreshold = BHBot.settings.successTreshold.get(state.getShortcut());
            int minimumVictories = successTreshold.getKey();
            int lvlIncrease = successTreshold.getValue();

            if (counters.get(state).getVictoriesInARow() >= minimumVictories) {
                if ("t".equals(state.getShortcut()) || "g".equals(state.getShortcut())) {
                    int newDifficulty;
                    String original, updated;

                    if ("t".equals(state.getShortcut())) {
                        newDifficulty = BHBot.settings.difficultyTrials + lvlIncrease;
                        original = "difficultyTrials " + BHBot.settings.difficultyTrials;
                        updated = "difficultyTrials " + newDifficulty;
                    } else { // Gauntlets
                        newDifficulty = BHBot.settings.difficultyGauntlet + lvlIncrease;
                        original = "difficultyGauntlet " + BHBot.settings.difficultyGauntlet;
                        updated = "difficultyGauntlet " + newDifficulty;
                    }

                    settingsUpdate(original, updated);
                }
            }
        }
    }

    /**
     * Reads number from given image.
     *
     * @return 0 in case of error.
     */
    private int readNumFromImg(BufferedImage im) {
        return readNumFromImg(im, "", new HashSet<>());
    }

    private int readNumFromImg(BufferedImage im, @SuppressWarnings("SameParameterValue") String numberPrefix, HashSet<Integer> intToSkip) {
        List<ScreenNum> nums = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            if (intToSkip.contains(i)) continue;
            List<MarvinSegment> list = FindSubimage.findSubimage(im, cues.get(numberPrefix + "" + i).im, 1.0, true, false, 0, 0, 0, 0);
            //BHBot.logger.info("DEBUG difficulty detection: " + i + " - " + list.size());
            for (MarvinSegment s : list) {
                nums.add(new ScreenNum(i, s.x1));
            }
        }

        // order list horizontally:
        Collections.sort(nums);

        if (nums.size() == 0)
            return 0; // error

        int d = 0; // difficulty
        int f = 1; // factor
        for (int i = nums.size() - 1; i >= 0; i--) {
            d += nums.get(i).value * f;
            f *= 10;
        }

        return d;
    }

    private void makeImageBlackWhite(MarvinImage input, Color black, Color white) {
        makeImageBlackWhite(input, black, white, 254);
    }

    /**
     * @param input The input image that will be converted in black and white scale
     * @param black White color treshold
     * @param white Black color treshold
     * @param custom Use the custom value to search for a specific RGB value if the numbers are not white
     *               E.G for invasion defeat screen the number colour is 64,64,64 in the background
     */
    private void makeImageBlackWhite(MarvinImage input, Color black, Color white, int custom) {
        int[] map = input.getIntColorArray();
        int white_rgb = white.getRGB();
        int black_rgb = black.getRGB();
        for (int i = 0; i < map.length; i++) {
            Color c = new Color(map[i], true);
            int r = c.getRed();
            int g = c.getGreen();
            int b = c.getBlue();
            int max = Misc.max(r, g, b);
            int min = Misc.min(r, g, b);
            //int diff = (max-r) + (max-g) + (max-b);
            int diff = max - min;
            if (diff >= 80 || (diff == 0 && max == custom)) { // it's a number color
                map[i] = white_rgb;
            } else { // it's a blackish background
                map[i] = black_rgb;
            }
        }
        input.setIntColorArray(map);
        input.update(); // must be called! Or else things won't work...
    }

    int detectDifficulty() {
        return detectDifficulty(cues.get("Difficulty"));
    }

    /**
     * Detects selected difficulty in trials/gauntlet window. <br>
     * NOTE: Trials/gauntlet window must be open for this to work! <br>
     *
     * @return 0 in case of an error, or the selected difficulty level instead.
     */
    private int detectDifficulty(Cue difficulty) {
        readScreen(2 * SECOND); // note that sometimes the cue will be gray (disabled) since the game is fetching data from the server - in that case we'll have to wait a bit

        MarvinSegment seg = detectCue(difficulty);
        if (seg == null) {
            seg = detectCue(cues.get("DifficultyDisabled"));
            if (seg != null) { // game is still fetching data from the server... we must wait a bit!
                sleep(5 * SECOND);
                seg = detectCue(difficulty, 20 * SECOND);
            }
        }
        if (seg == null) {
            BHBot.logger.error("Error: unable to detect difficulty selection box!");
            saveGameScreen("early_error");
            return 0; // error
        }

        MarvinImage im = new MarvinImage(img.getSubimage(seg.x1 + 35, seg.y1 + 30, 55, 19));

        // make it white-gray (to facilitate cue recognition):
        makeImageBlackWhite(im, new Color(25, 25, 25), new Color(255, 255, 255));

        BufferedImage imb = im.getBufferedImage();

        return readNumFromImg(imb);
    }

    /* World boss reading and changing section */
    private int detectWorldBossTier() {

        readScreen(SECOND);
        MarvinSegment tierDropDown;
        int xOffset = 401, yOffset = 210, w = 21, h = 19;

        tierDropDown = detectCue("WorldBossTierDropDown", SECOND); // For tier drop down menu

        if (tierDropDown == null) {
            BHBot.logger.error("Error: unable to detect world boss difficulty selection box in detectWorldBossTier!");
            return 0; // error
        }

        MarvinImage im = new MarvinImage(img.getSubimage(xOffset, yOffset, w, h));

        // make it white-gray (to facilitate cue recognition):
        makeImageBlackWhite(im, new Color(25, 25, 25), new Color(255, 255, 255));

        BufferedImage imb = im.getBufferedImage();

        return readNumFromImg(imb);
    }

    private void changeWorldBossTier(int target) {
        MarvinSegment seg;
        readScreen(SECOND); //wait for screen to stabilize
        seg = detectCue(cues.get("WorldBossTierDropDown"), 2 * SECOND);

        if (seg == null) {
            BHBot.logger.error("Error: unable to detect world boss difficulty selection box in changeWorldBossTier!");
            saveGameScreen("early_error");
            restart();
        }

        clickOnSeg(seg);
        readScreen(2 * SECOND); //wait for screen to stabilize

        //get known screen position for difficulty screen selection
        if (target >= 5) { //top most
            readScreen();
            MarvinSegment up = detectCue(cues.get("DropDownUp"), SECOND);
            if (up != null) {
                clickOnSeg(up);
                clickOnSeg(up);
            }
        } else { //bottom most
            readScreen();
            MarvinSegment down = detectCue(cues.get("DropDownDown"), SECOND);
            if (down != null) {
                clickOnSeg(down);
                clickOnSeg(down);
            }
        }
        readScreen(SECOND); //wait for screen to stabilize
        Point diff = getDifficultyButtonXY(target);
        if (diff != null) {
            //noinspection SuspiciousNameCombination
            clickInGame(diff.y, diff.x);
        }
    }

    private Point getDifficultyButtonXY(int target) {
        switch (target) {
            case 3:
            case 5: // top 5 buttons after we scroll to the top
                return new Point(410, 390);
            case 4: // bottom 2 buttons after we scroll to the bottom
            case 6:
                return new Point(350, 390);
            case 7:
                return new Point(290, 390);
            case 8:
            case 10:
                return new Point(230, 390);
            case 9:
            case 11:
                return new Point(170, 390);
        }
        return null;
    }

    private int detectWorldBossDifficulty() {
        readScreen();

        if (detectCue(cues.get("WorldBossDifficultyNormal"), SECOND) != null) {
            return 1;
        } else if (detectCue(cues.get("WorldBossDifficultyHard"), SECOND) != null) {
            return 2;
        } else if (detectCue(cues.get("WorldBossDifficultyHeroic"), SECOND) != null) {
            return 3;
        } else return 0;
    }

    private void changeWorldBossDifficulty(int target) {

        readScreen(SECOND); //screen stabilising
        clickInGame(480, 300); //difficulty button
        readScreen(SECOND); //screen stabilising

        Cue difficultySelection;

        if (target == 1) {
            difficultySelection = cues.get("cueWBSelectNormal");
        } else if (target == 2) {
            difficultySelection = cues.get("cueWBSelectHard");
        } else if (target == 3) {
            difficultySelection = cues.get("cueWBSelectHeroic");
        } else {
            BHBot.logger.error("Wrong target value in changeWorldBossDifficulty, defult to normal!");
            difficultySelection = cues.get("cueWBSelectNormal");
        }

        MarvinSegment seg = detectCue(difficultySelection, SECOND * 2);
        if (seg != null) {
            clickOnSeg(seg);
        } else {
            BHBot.logger.error("Impossible to detect desired difficulty in changeWorldBossDifficulty!");
            restart();
        }
    }

    /**
     * Changes difficulty level in trials/gauntlet window. <br>
     * Note: for this to work, trials/gauntlet window must be open!
     *
     * @return false in case of an error (unable to change difficulty).
     */
    boolean selectDifficulty(int oldDifficulty, int newDifficulty) {
        return selectDifficulty(oldDifficulty, newDifficulty, cues.get("SelectDifficulty"), 1);
    }

    private boolean selectDifficulty(int oldDifficulty, int newDifficulty, Cue difficulty, int step) {
        if (oldDifficulty == newDifficulty)
            return true; // no change

        MarvinSegment seg = detectCue(difficulty, 2 * SECOND);
        if (seg == null) {
            BHBot.logger.error("Error: unable to detect 'select difficulty' button while trying to change difficulty level!");
            return false; // error
        }

        clickOnSeg(seg);

        readScreen(5 * SECOND);

        return selectDifficultyFromDropDown(newDifficulty, step);
    }

    /**
     * Internal routine. Difficulty drop down must be open for this to work!
     * Note that it closes the drop-down when it is done (except if an error occurred). However there is a close
     * animation and the caller must wait for it to finish.
     *
     * @return false in case of an error.
     */
    private boolean selectDifficultyFromDropDown(int newDifficulty, int step) {
        return selectDifficultyFromDropDown(newDifficulty, 0, step);
    }

    /**
     * Internal routine - do not use it manually! <br>
     *
     * @return false on error (caller must do restart() if he gets false as a result from this method)
     */
    private boolean selectDifficultyFromDropDown(int newDifficulty, int recursionDepth, int step) {
        // horizontal position of the 5 buttons:
        final int posx = 390;
        // vertical positions of the 5 buttons:
        final int[] posy = new int[]{170, 230, 290, 350, 410};

        if (recursionDepth > 3) {
            BHBot.logger.error("Error: Selecting difficulty level from the drop-down menu ran into an endless loop!");
            saveGameScreen("early_error");
            tryClosingWindow(); // clean up after our selves (ignoring any exception while doing it)
            return false;
        }

        MarvinSegment seg;

        MarvinImage subm = new MarvinImage(img.getSubimage(350, 150, 70, 35)); // the first (upper most) of the 5 buttons in the drop-down menu. Note that every while a "tier x" is written bellow it, so text is higher up (hence we need to scan a larger area)
        makeImageBlackWhite(subm, new Color(25, 25, 25), new Color(255, 255, 255));
        BufferedImage sub = subm.getBufferedImage();
        int num = readNumFromImg(sub);
//		BHBot.logger.info("num = " + Integer.toString(num));
        if (num == 0) {
            BHBot.logger.error("Error: unable to read difficulty level from a drop-down menu!");
            saveGameScreen("early_error");
            tryClosingWindow(); // clean up after our selves (ignoring any exception while doing it)
            return false;
        }

        int move = (newDifficulty - num) / step; // if negative, we have to move down (in dropdown/numbers), or else up
//		BHBot.logger.info("move = " + Integer.toString(move));

        if (move >= -4 && move <= 0) {
            // we have it on screen. Let's select it!
            clickInGame(posx, posy[Math.abs(move)]); // will auto-close the drop down (but it takes a second or so, since it's animated)
            return true;
        }

        // scroll the drop-down until we reach our position:
        if (move > 0) {
            // move up
            seg = detectCue(cues.get("DropDownUp"));
            if (seg == null) {
                BHBot.logger.error("Error: unable to detect up arrow in trials/gauntlet difficulty drop-down menu!");
                saveGameScreen("early_error");
                clickInGame(posx, posy[0]); // regardless of the error, click on the first selection in the drop-down, so that we don't need to re-scroll entire list next time we try!
                return false;
            }
            for (int i = 0; i < move; i++) {
                clickOnSeg(seg);
            }
            // OK, we should have a target value on screen now, in the first spot. Let's click it!
            readScreen(5 * SECOND); //*** should we increase this time?
            return selectDifficultyFromDropDown(newDifficulty, recursionDepth + 1, step); // recursively select new difficulty
        } else {
            // move down
            seg = detectCue(cues.get("DropDownDown"));
            if (seg == null) {
                BHBot.logger.error("Error: unable to detect down arrow in trials/gauntlet difficulty drop-down menu!");
                saveGameScreen("early_error");
                clickInGame(posx, posy[0]); // regardless of the error, click on the first selection in the drop-down, so that we don't need to re-scroll entire list next time we try!
                return false;
            }
            int moves = Math.abs(move) - 4;
//			BHBot.logger.info("Scrolls to 60 = " + Integer.toString(moves));
            for (int i = 0; i < moves; i++) {
                clickOnSeg(seg);
            }
            // OK, we should have a target value on screen now, in the first spot. Let's click it!
            readScreen(5 * SECOND); //*** should we increase this time?
            return selectDifficultyFromDropDown(newDifficulty, recursionDepth + 1, step); // recursively select new difficulty
        }
    }

    /**
     * This method detects the select cost in PvP/GvG/Trials/Gauntlet window. <p>
     * <p>
     * Note: PvP cost has different position from GvG/Gauntlet/Trials. <br>
     * Note: PvP/GvG/Trials/Gauntlet window must be open in order for this to work!
     *
     * @return 0 in case of an error, or cost value in interval [1..5]
     */
    int detectCost() {
        MarvinSegment seg = detectCue(cues.get("Cost"), 15 * SECOND);
        if (seg == null) {
            BHBot.logger.error("Error: unable to detect cost selection box!");
            saveGameScreen("early_error");
            return 0; // error
        }

        // because the popup may still be sliding down and hence cue could be changing position, we try to read cost in a loop (until a certain timeout):
        int d;
        int counter = 0;
        boolean success = true;
        while (true) {
            MarvinImage im = new MarvinImage(img.getSubimage(seg.x1 + 2, seg.y1 + 20, 35, 24));
            makeImageBlackWhite(im, new Color(25, 25, 25), new Color(255, 255, 255));
            BufferedImage imb = im.getBufferedImage();
            d = readNumFromImg(imb);
            if (d != 0)
                break; // success

            counter++;
            if (counter > 10) {
                success = false;
                break;
            }
            sleep(SECOND); // sleep a bit in order for the popup to slide down
            readScreen();
            seg = detectCue(cues.get("Cost"));
        }

        if (!success) {
            BHBot.logger.error("Error: unable to detect cost selection box value!");
            saveGameScreen("early_error");
            return 0;
        }

        return d;
    }

    /**
     * Changes cost in PvP, GvG, or Trials/Gauntlet window. <br>
     * Note: for this to work, PvP/GvG/Trials/Gauntlet window must be open!
     *
     * @return false in case of an error (unable to change cost).
     */
    boolean selectCost(int oldCost, int newCost) {
        if (oldCost == newCost)
            return true; // no change

        MarvinSegment seg = detectCue(cues.get("SelectCost"), 5 * SECOND);
        if (seg == null) {
            BHBot.logger.error("Error: unable to detect 'select cost' button while trying to change cost!");
            return false; // error
        }

        clickOnSeg(seg);

        detectCue("CostDropDown", 5 * SECOND); // wait for the cost selection popup window to open

        // horizontal position of the 5 buttons:
        final int posx = 390;
        // vertical positions of the 5 buttons:
        final int[] posy = new int[]{170, 230, 290, 350, 410};

        clickInGame(posx, posy[newCost - 1]); // will auto-close the drop down (but it takes a second or so, since it's animated)
        sleep(2 * SECOND);

        return true;
    }

    /**
     * Will try to click on "X" button of the currently open popup window. On error, it will ignore it. <br>
     * NOTE: This method does not re-read screen before (or after) cue detection!
     */
    private void tryClosingWindow() {
        tryClosingWindow(null);
    }

    /**
     * Will try to click on "X" button of the currently open popup window that is identified by the 'windowCue'. It will ignore any errors. <br>
     * NOTE: This method does not re-read screen before (or after) cue detection!
     */
    private void tryClosingWindow(Cue windowCue) {
        try {
            MarvinSegment seg;
            if (windowCue != null) {
                seg = detectCue(windowCue);
                if (seg == null)
                    return;
            }
            seg = detectCue(cues.get("X"));
            if (seg != null)
                clickOnSeg(seg);
        } catch (Exception e) {
            BHBot.logger.error("Error in tryClosingWindow", e);
        }
    }

    /**
     * Will close the popup by clicking on the 'close' cue and checking that 'popup' cue is gone. It will repeat this operation
     * until either 'popup' cue is gone or timeout is reached. This method ensures that the popup is closed. Sometimes just clicking once
     * on the close button ('close' cue) doesn't work, since popup is still sliding down and we miss the button, this is why we need to
     * check if it is actually closed. This is what this method does.
     * <p>
     * Note that before entering into this method, caller had probably already detected the 'popup' cue (but not necessarily). <br>
     * Note: in case of failure, it will print it out.
     *
     * @return false in case it failed to close it (timed out).
     */
    private boolean closePopupSecurely(Cue popup, Cue close) {
        MarvinSegment seg1, seg2;
        int counter;
        seg1 = detectCue(close);
        seg2 = detectCue(popup);

        // make sure popup window is on the screen (or else wait until it appears):
        counter = 0;
        while (seg2 == null) {
            counter++;
            if (counter > 10) {
                BHBot.logger.error("Error: unable to close popup <" + popup.name + "> securely: popup cue not detected!");
                return false;
            }
            readScreen(SECOND);
            seg2 = detectCue(popup);
        }

        counter = 0;
        // there is no more popup window, so we're finished!
        while (seg2 != null) {
            if (seg1 != null)
                clickOnSeg(seg1);

            counter++;
            if (counter > 10) {
                BHBot.logger.error("Error: unable to close popup <" + popup.name + "> securely: either close button has not been detected or popup would not close!");
                return false;
            }

            readScreen(SECOND);
            seg1 = detectCue(close);
            seg2 = detectCue(popup);
        }

        return true;
    }

    /**
     * @return -1 on error
     */
    private int detectEquipmentFilterScrollerPos() {
        final int[] yScrollerPositions = {146, 164, 181, 199, 217, 235, 252, 270, 288, 306, 323, 341}; // top scroller positions

        MarvinSegment seg = detectCue(cues.get("StripScrollerTopPos"), 2 * SECOND);
        if (seg == null) {
            return -1;
        }
        int pos = seg.y1;

        return Misc.findClosestMatch(yScrollerPositions, pos);
    }

    /**
     * Will strip character down (as a preparation for the PvP battle) of items passed as parameters to this method.
     * Note that before calling this method, game must be in the main method!
     *
     * @param type which item type should we equip/unequip
     * @param dir  direction - either strip down or dress up
     */
    private void strip(EquipmentType type, StripDirection dir) {
        MarvinSegment seg;

        // click on the character menu button (it's a bottom-left button with your character image on it):
        clickInGame(55, 465);

        seg = detectCue(cues.get("StripSelectorButton"), 10 * SECOND);
        if (seg == null) {
            BHBot.logger.error("Error: unable to detect equipment filter button! Skipping...");
            return;
        }

        // now lets see if the right category is already selected:
        seg = detectCue(type.getCue(), 500);
        if (seg == null) {
            // OK we need to manually select the correct category!
            seg = detectCue(cues.get("StripSelectorButton"));
            clickOnSeg(seg);

            detectCue(cues.get("StripItemsTitle"), 10 * SECOND); // waits until "Items" popup is detected
            readScreen(500); // to stabilize sliding popup a bit

            int scrollerPos = detectEquipmentFilterScrollerPos();
//			BHBot.logger.info("Scroller Pos = " + Integer.toString(scrollerPos));
            if (scrollerPos == -1) {
                BHBot.logger.warn("Problem detected: unable to detect scroller position in the character window (location #1)! Skipping strip down/up...");
                return;
            }

            int[] yButtonPositions = {170, 230, 290, 350, 410}; // center y positions of the 5 buttons
            int xButtonPosition = 390;

            if (scrollerPos < type.minPos()) {
                // we must scroll down!
                int move = type.minPos() - scrollerPos;
                seg = detectCue(cues.get("DropDownDown"), 5 * SECOND);
                for (int i = 0; i < move; i++) {
                    clickOnSeg(seg);
                    scrollerPos++;
                }
            } else { // bestIndex > type.maxPos
                // we must scroll up!
                int move = scrollerPos - type.minPos();
                seg = detectCue(cues.get("DropDownUp"), 5 * SECOND);
                for (int i = 0; i < move; i++) {
                    clickOnSeg(seg);
                    scrollerPos--;
                }
            }

            // make sure scroller is in correct position now:
            readScreen(500); // so that the scroller stabilizes a bit
            int newScrollerPos = detectEquipmentFilterScrollerPos();
            int counter = 0;
            while (newScrollerPos != scrollerPos) {
                if (counter > 3) {
                    BHBot.logger.warn("Problem detected: unable to adjust scroller position in the character window (scroller position: " + newScrollerPos + ", should be: " + scrollerPos + ")! Skipping strip down/up...");
                    return;
                }
                readScreen(SECOND);
                newScrollerPos = detectEquipmentFilterScrollerPos();
                counter++;
            }
            clickInGame(xButtonPosition, yButtonPositions[type.getButtonPos() - scrollerPos]);
            // clicking on the button will close the window automatically... we just need to wait a bit for it to close
            detectCue(cues.get("StripSelectorButton"), 5 * SECOND); // we do this just in order to wait for the previous menu to reappear
        }

        waitForInventoryIconsToLoad(); // first of all, lets make sure that all icons are loaded

        // now deselect/select the strongest equipment in the menu:

        seg = detectCue(cues.get("StripEquipped"), 500); // if "E" icon is not found, that means that some other item is equipped or that no item is equipped
        boolean equipped = seg != null; // is strongest item equipped already?

        // position of top-left item (which is the strongest) is (490, 210)
        if (dir == StripDirection.StripDown) {
            clickInGame(490, 210);
            if (!equipped) // in case item was not equipped, we must click on it twice, first time to equip it, second to unequip it. This could happen for example when we had some weaker item equipped (or no item equipped).
                clickInGame(490, 210);
        } else {
            if (!equipped)
                clickInGame(490, 210);
        }

        // OK, we're done, lets close the character menu window:
        closePopupSecurely(cues.get("StripSelectorButton"), cues.get("X"));
    }

    private void stripDown(List<String> striplist) {
        if (striplist.size() == 0)
            return;

        StringBuilder list = new StringBuilder();
        for (String type : striplist) {
            list.append(EquipmentType.letterToName(type)).append(", ");
        }
        list = new StringBuilder(list.substring(0, list.length() - 2));
        BHBot.logger.info("Stripping down for PvP/GVG (" + list + ")...");

        for (String type : striplist) {
            strip(EquipmentType.letterToType(type), StripDirection.StripDown);
        }
    }

    private void dressUp(List<String> striplist) {
        if (striplist.size() == 0)
            return;

        StringBuilder list = new StringBuilder();
        for (String type : striplist) {
            list.append(EquipmentType.letterToName(type)).append(", ");
        }
        list = new StringBuilder(list.substring(0, list.length() - 2));
        BHBot.logger.info("Dressing back up (" + list + ")...");

        // we reverse the order so that we have to make less clicks to dress up equipment
        Collections.reverse(striplist);
        for (String type : striplist) {
            strip(EquipmentType.letterToType(type), StripDirection.DressUp);
        }
        Collections.reverse(striplist);
    }

    //*** for DEBUG only!
	/*public void numTest() {
		MarvinSegment seg;

		while (true) {
			readScreen(500);

			MarvinImage subm = new MarvinImage(img.getSubimage(350, 150, 70, 35)); // the first (upper most) of the 5 buttons in the drop-down menu
			makeImageBlackWhite(subm, new Color(25, 25, 25), new Color(255, 255, 255), 254);
			BufferedImage sub = subm.getBufferedImage();
			int num = readNumFromImg(sub, "");
			if (num == 0) {
				BHBot.logger.info("Error: unable to read difficulty level from a drop-down menu!");
				return;
			}
			BHBot.logger.info("Difficulty: " + num);

			// move up
			seg = detectCue(cues.get("DropDownUp"));
			if (seg == null) {
				BHBot.logger.info("Error: unable to detect up arrow in trials/gauntlet difficulty drop-down menu!");
				return;
			}
			clickOnSeg(seg);
		}
	}*/

    /**
     * Daily collection of fishing baits!
     */
    private void handleFishingBaits() {
        MarvinSegment seg;

        seg = detectCue(cues.get("Fishing"), SECOND * 5);
        if (seg != null) {
            clickOnSeg(seg);
            sleep(SECOND); // we allow some seconds as maybe the reward popup is sliding down

            detectCharacterDialogAndHandleIt();

            seg = detectCue(cues.get("WeeklyRewards"), SECOND * 5);
            if (seg != null) {
                seg = detectCue(cues.get("X"), 5 * SECOND);
                if (seg != null) {
                    if ((BHBot.settings.screenshots.contains("a"))) {
                        saveGameScreen("fishing-baits");
                    }
                    clickOnSeg(seg);
                    BHBot.logger.info("Correctly collected fishing baits");
                    readScreen(SECOND * 2);
                } else {
                    BHBot.logger.error("Something weng wrong while collecting fishing baits, restarting...");
                    saveGameScreen("fishing-error-baits");
                    restart();
                }
            }

            seg = detectCue(cues.get("X"), 5 * SECOND);
            if (seg != null) {
                clickOnSeg(seg);
                sleep(SECOND * 2);
                readScreen();
            } else {
                BHBot.logger.error("Something went wrong while closing the fishing dialog, restarting...");
                saveGameScreen("fishing-error-closing");
                restart();
            }

        } else {
            BHBot.logger.warn("Impossible to find the fishing button");
        }
        readScreen(SECOND * 2);
    }

    /**
     * We must be in main menu for this to work!
     */
    private void handleConsumables() {
        if (!BHBot.settings.autoConsume || BHBot.settings.consumables.size() == 0) // consumables management is turned off!
            return;

        MarvinSegment seg;

        boolean exp = detectCue(cues.get("BonusExp")) != null;
        boolean item = detectCue(cues.get("BonusItem")) != null;
        boolean speed = detectCue(cues.get("BonusSpeed")) != null;
        boolean gold = detectCue(cues.get("BonusGold")) != null;

        // Special consumables
        if (detectCue(cues.get("ConsumablePumkgor")) != null || detectCue(cues.get("ConsumableBroccoli")) != null
                || detectCue(cues.get("ConsumableGreatFeast")) != null || detectCue(cues.get("ConsumableGingernaut")) != null
                || detectCue(cues.get("ConsumableCoco")) != null) {
            exp = true;
            item = true;
            speed = true;
            gold = true;
            // BHBot.logger.info("Special consumable detected, skipping all the rest...");
        }

        EnumSet<ConsumableType> duplicateConsumables = EnumSet.noneOf(ConsumableType.class); // here we store consumables that we wanted to consume now but we have detected they are already active, so we skipped them (used for error reporting)
        EnumSet<ConsumableType> consumables = EnumSet.noneOf(ConsumableType.class); // here we store consumables that we want to consume now
        for (String s : BHBot.settings.consumables)
            consumables.add(ConsumableType.getTypeFromName(s));
        //BHBot.logger.info("Testing for following consumables: " + Misc.listToString(consumables));

        if (exp) {
            consumables.remove(ConsumableType.EXP_MINOR);
            consumables.remove(ConsumableType.EXP_AVERAGE);
            consumables.remove(ConsumableType.EXP_MAJOR);
        }

        if (item) {
            consumables.remove(ConsumableType.ITEM_MINOR);
            consumables.remove(ConsumableType.ITEM_AVERAGE);
            consumables.remove(ConsumableType.ITEM_MAJOR);
        }

        if (speed) {
            consumables.remove(ConsumableType.SPEED_MINOR);
            consumables.remove(ConsumableType.SPEED_AVERAGE);
            consumables.remove(ConsumableType.SPEED_MAJOR);
        }

        if (gold) {
            consumables.remove(ConsumableType.GOLD_MINOR);
            consumables.remove(ConsumableType.GOLD_AVERAGE);
            consumables.remove(ConsumableType.GOLD_MAJOR);
        }

        // so now we have only those consumables in the 'consumables' list that we actually need to consume right now!

        if (consumables.isEmpty()) // we don't need to do anything!
            return;

        // OK, try to consume some consumables!
        BHBot.logger.info("Trying to consume some consumables (" + Misc.listToString(consumables) + ")...");

        // click on the character menu button (it's a bottom-left button with your character image on it):
        clickInGame(55, 465);

        seg = detectCue(cues.get("StripSelectorButton"), 15 * SECOND);
        if (seg == null) {
            BHBot.logger.warn("Error: unable to detect equipment filter button! Skipping...");
            return;
        }

        // now lets select the <Consumables> category (if it is not already selected):
        seg = detectCue(cues.get("FilterConsumables"), 500);
        if (seg == null) { // if not, right category (<Consumables>) is already selected!
            // OK we need to manually select the <Consumables> category!
            seg = detectCue(cues.get("StripSelectorButton"));
            clickOnSeg(seg);

            detectCue(cues.get("StripItemsTitle"), 10 * SECOND); // waits until "Items" popup is detected
            readScreen(500); // to stabilize sliding popup a bit

            int scrollerPos = detectEquipmentFilterScrollerPos();
            if (scrollerPos == -1) {
                BHBot.logger.warn("Problem detected: unable to detect scroller position in the character window (location #1)! Skipping consumption of consumables...");
                return;
            }

            int[] yButtonPositions = {170, 230, 290, 350, 410}; // center y positions of the 5 buttons
            int xButtonPosition = 390;

            if (scrollerPos != 0) {
                // we must scroll up!
                int move = scrollerPos;
                seg = detectCue(cues.get("DropDownUp"), 5 * SECOND);
                for (int i = 0; i < move; i++) {
                    clickOnSeg(seg);
                    scrollerPos--;
                }
            }

            // make sure scroller is in correct position now:
            readScreen(2000); // so that the scroller stabilizes a bit //Quick Fix slow down
            int newScrollerPos = detectEquipmentFilterScrollerPos();
            int counter = 0;
            while (newScrollerPos != scrollerPos) {
                if (counter > 3) {
                    BHBot.logger.warn("Problem detected: unable to adjust scroller position in the character window (scroller position: " + newScrollerPos + ", should be: " + scrollerPos + ")! Skipping consumption of consumables...");
                    return;
                }
                readScreen(SECOND);
                newScrollerPos = detectEquipmentFilterScrollerPos();
                counter++;
            }
            clickInGame(xButtonPosition, yButtonPositions[1]);
            // clicking on the button will close the window automatically... we just need to wait a bit for it to close
            detectCue(cues.get("StripSelectorButton"), 5 * SECOND); // we do this just in order to wait for the previous menu to reappear
        }

        // now consume the consumable(s):

        readScreen(500); // to stabilize window a bit
        Bounds bounds = new Bounds(450, 165, 670, 460); // detection area (where consumables icons are visible)

        while (!consumables.isEmpty()) {
            waitForInventoryIconsToLoad(); // first of all, lets make sure that all icons are loaded
            for (Iterator<ConsumableType> i = consumables.iterator(); i.hasNext(); ) {
                ConsumableType c = i.next();
                seg = detectCue(new Cue(c.getInventoryCue(), bounds));
                if (seg != null) {
                    // OK we found the consumable icon! Lets click it...
                    clickOnSeg(seg);
                    detectCue(cues.get("ConsumableTitle"), 5 * SECOND); // wait for the consumable popup window to appear
                    readScreen(500); // wait for sliding popup to stabilize a bit

                    /*
                     *  Measure distance between "Consumable" (popup title) and "Yes" (green yes button).
                     *  This seems to be the safest way to distinguish the two window types. Because text
                     *  inside windows change and sometimes letters are wider apart and sometimes no, so it
                     *  is not possible to detect cue like "replace" wording, or any other (I've tried that
                     *  and failed).
                     */
                    int dist;
                    seg = detectCue(cues.get("ConsumableTitle"));
                    dist = seg.y1;
                    seg = detectCue(cues.get("YesGreen"));
                    dist = seg.y1 - dist;
                    // distance for the big window should be 262 pixels, for the small one it should be 212.

                    if (dist > 250) {
                        // don't consume the consumable... it's already in use!
                        BHBot.logger.warn("Error: \"Replace consumable\" dialog detected, meaning consumable is already in use (" + c.getName() + "). Skipping...");
                        duplicateConsumables.add(c);
                        closePopupSecurely(cues.get("ConsumableTitle"), cues.get("No"));
                    } else {
                        // consume the consumable:
                        closePopupSecurely(cues.get("ConsumableTitle"), cues.get("YesGreen"));
                    }
                    detectCue(cues.get("StripSelectorButton"), 5 * SECOND); // we do this just in order to wait for the previous menu to reappear
                    i.remove();
                }
            }

            if (!consumables.isEmpty()) {
                seg = detectCue(cues.get("ScrollerAtBottom"), 500);
                if (seg != null)
                    break; // there is nothing we can do anymore... we've scrolled to the bottom and haven't found the icon(s). We obviously don't have the required consumable(s)!

                // lets scroll down:
                seg = detectCue(cues.get("DropDownDown"), 5 * SECOND);
                clickOnSeg(seg);

                readScreen(SECOND); // so that the scroller stabilizes a bit
            }
        }

        // OK, we're done, lets close the character menu window:
        boolean result = closePopupSecurely(cues.get("StripSelectorButton"), cues.get("X"));
        if (!result) {
            BHBot.logger.warn("Done. Error detected while trying to close character window. Ignoring...");
            return;
        }

        if (!consumables.isEmpty()) {
            BHBot.logger.warn("Some consumables were not found (out of stock?) so were not consumed. These are: " + Misc.listToString(consumables) + ".");

            for (ConsumableType c : consumables) {
                BHBot.settings.consumables.remove(c.getName());
            }

            BHBot.logger.warn("The following consumables have been removed from auto-consume list: " + Misc.listToString(consumables) + ". In order to reactivate them, reload your settings.ini file using 'reload' command.");
        } else {
            if (!duplicateConsumables.isEmpty())
                BHBot.logger.info("Done. Some of the consumables have been skipped since they are already in use: " + Misc.listToString(duplicateConsumables));
            else
                BHBot.logger.info("Done. Desired consumables have been successfully consumed.");
        }
    }

    /**
     * Will make sure all the icons in the inventory have been loaded.
     */
    private void waitForInventoryIconsToLoad() {
        Bounds bounds = new Bounds(450, 165, 670, 460); // detection area (where inventory icons are visible)
        MarvinSegment seg;
        Cue cue = new Cue(cues.get("LoadingInventoryIcon"), bounds);

        int counter = 0;
        seg = detectCue(cue);
        while (seg != null) {
            readScreen(SECOND);

            seg = detectCue(cues.get("StripSelectorButton"));
            if (seg == null) {
                BHBot.logger.error("Error: while detecting possible loading of inventory icons, inventory cue has not been detected! Ignoring...");
                return;
            }

            seg = detectCue(cue);
            counter++;
            if (counter > 100) {
                BHBot.logger.error("Error: loading of icons has been detected in the inventory screen, but it didn't finish in time. Ignoring...");
                return;
            }
        }
    }

    /**
     * Will reset readout timers.
     */
    void resetTimers() {
        timeLastExpBadgesCheck = 0;
        timeLastInvBadgesCheck = 0;
        timeLastGVGBadgesCheck = 0;
        timeLastEnergyCheck = 0;
        timeLastShardsCheck = 0;
        timeLastTicketsCheck = 0;
        timeLastTrialsTokensCheck = 0;
        timeLastGauntletTokensCheck = 0;
        timeLastBonusCheck = 0;
        timeLastFishingCheck = 0;
        timeLastFishingBaitsCheck = 0;
    }

    /* This will only reset timers for activities we still have resources to run */
    /* This saves cycling through the list of all activities to run every time we finish one */
    /* It's also useful for other related settings to be reset on activity finish */
    private void resetAppropriateTimers() {
        startTimeCheck = false;
        specialDungeon = false;
        potionsUsed = 0;

        /*
            In this section we check if we are able to run the activity again and if so reset the timer to 0
            else we wait for the standard timer until we check again
         */

        if (((globalShards - 1) >= BHBot.settings.minShards) && state == State.Raid) {
            timeLastShardsCheck = 0;
        }

        if (((globalBadges - BHBot.settings.costExpedition) >= BHBot.settings.costExpedition) && state == State.Expedition) {
            timeLastExpBadgesCheck = 0;
        }

        if (((globalBadges - BHBot.settings.costInvasion) >= BHBot.settings.costInvasion) && state == State.Invasion) {
            timeLastInvBadgesCheck = 0;
        }

        if (((globalBadges - BHBot.settings.costGVG) >= BHBot.settings.costGVG && state == State.GVG)) {
            timeLastGVGBadgesCheck = 0;
        }

        if (((globalEnergy - 10) >= BHBot.settings.minEnergyPercentage) && state == State.Dungeon) {
            timeLastEnergyCheck = 0;
        }

        if (((globalEnergy - 10) >= BHBot.settings.minEnergyPercentage) && state == State.WorldBoss) {
            timeLastEnergyCheck = 0;
        }

        if (((globalTickets - BHBot.settings.costPVP) >= BHBot.settings.costPVP) && state == State.PVP) {
            timeLastTicketsCheck = 0;
        }

        if (((globalTokens - BHBot.settings.costTrials) >= BHBot.settings.costTrials) && state == State.Trials) {
            timeLastTrialsTokensCheck = 0;
        }

        if (((globalTokens - BHBot.settings.costGauntlet) >= BHBot.settings.costGauntlet && state == State.Gauntlet)) {
            timeLastGauntletTokensCheck = 0;
        }
    }

    private void resetRevives() {
        revived[0] = false;
        revived[1] = false;
        revived[2] = false;
        revived[3] = false;
        revived[4] = false;
    }

    void sendPushOverMessage(String title, String msg, MessagePriority priority, File attachment) {
        sendPushOverMessage(title, msg, "pushover", priority, attachment);
    }

    private void sendPushOverMessage(String title, String msg, @SuppressWarnings("SameParameterValue") String sound) {
        sendPushOverMessage(title, msg, sound, MessagePriority.NORMAL, null);
    }

    private void sendPushOverMessage(String title, String msg, String sound, MessagePriority priority, File attachment) {
        if (BHBot.settings.enablePushover) {
            try {
                BHBot.poClient.pushMessage(
                        PushoverMessage.builderWithApiToken(BHBot.settings.poAppToken)
                                .setUserId(BHBot.settings.poUserToken)
                                .setTitle(title)
                                .setMessage(msg)
                                .setPriority(priority)
                                .setSound(sound)
                                .setAttachment(attachment)
                                .build());
            } catch (PushoverException e) {
                BHBot.logger.error("Error while sending Pushover message", e);
            }
        }
    }

    private Bounds opponentSelector(int opponent) {

        if (BHBot.settings.pvpOpponent < 1 || BHBot.settings.pvpOpponent > 4) {
            //if setting outside 1-4th opponents we default to 1st
            BHBot.logger.warn("pvpOpponent must be between 1 and 4, defaulting to first opponent");
            BHBot.settings.pvpOpponent = 1;
            return new Bounds(544, 188, 661, 225); //1st opponent
        }

        if (BHBot.settings.gvgOpponent < 1 || BHBot.settings.gvgOpponent > 4) {
            //if setting outside 1-4th opponents we default to 1st
            BHBot.logger.warn("gvgOpponent must be between 1 and 4, defaulting to first opponent");
            BHBot.settings.gvgOpponent = 1;
            return new Bounds(544, 188, 661, 225); //1st opponent
        }

        switch (opponent) {
            case 1:
                return new Bounds(545, 188, 660, 225); //1st opponent
            case 2:
                return new Bounds(545, 243, 660, 279); //2nd opponent
            case 3:
                return new Bounds(544, 296, 660, 335); //1st opponent
            case 4:
                return new Bounds(544, 351, 660, 388); //1st opponent
        }
        return null;
    }

    void softReset() {
        state = State.Main;
        resetTimers();
    }

    private void handleFishing() {
        MarvinSegment seg;

        seg = detectCue(cues.get("Fishing"), SECOND * 5);
        if (seg != null) {

            //we make sure that the window is visible
            showBrowser();

            clickOnSeg(seg);
            sleep(SECOND); // we allow some seconds as maybe the reward popup is sliding down

            detectCharacterDialogAndHandleIt();

            int fishingTime = 10 + (BHBot.settings.baitAmount * 15); //pause for around 15 seconds per bait used, plus 10 seconds buffer

            readScreen();

            seg = detectCue(cues.get("Play"), SECOND * 5);
            if (seg != null) {
                clickOnSeg(seg);
            }

            seg = detectCue(cues.get("Start"), SECOND * 20);
            if (seg != null) {
                try {
                    BHBot.logger.info("Pausing for " + fishingTime + " seconds to fish");
                    BHBot.scheduler.pause();

                    Process fisher = Runtime.getRuntime().exec("cmd /k \"cd DIRECTORY & java -jar bh-fisher.jar\" " + BHBot.settings.baitAmount);
                    if (!fisher.waitFor(fishingTime, TimeUnit.SECONDS)) { //run and wait for fishingTime seconds
                        BHBot.scheduler.resume();
                    }

                } catch (IOException | InterruptedException ex) {
                    BHBot.logger.error("Can't start bh-fisher.jar", ex);
                }

            } else BHBot.logger.info("start not found");

            if (!closeFishingSafely()) {
                BHBot.logger.error("Error closing fishing, restarting..");
                restart();
            }

            readScreen(SECOND);
            enterGuildHall();

            if (BHBot.settings.hideWindowOnRestart) hideBrowser();
        }

    }

    private boolean closeFishingSafely() {
        MarvinSegment seg;
        readScreen();

        seg = detectCue(cues.get("Trade"), SECOND * 3);
        if (seg != null) {
            clickOnSeg(seg);
        }

        seg = detectCue(cues.get("X"), SECOND * 3);
        if (seg != null) {
            clickOnSeg(seg);
        }

        seg = detectCue(cues.get("FishingClose"), 3 * SECOND);
        if (seg != null) {
            clickOnSeg(seg);
        }

        seg = detectCue(cues.get("GuildButton"), SECOND * 5);
        //else not
        return seg != null; //if we can see the guild button we are successful

    }

    private void enterGuildHall() {
        MarvinSegment seg;

        seg = detectCue(cues.get("GuildButton"), SECOND * 5);
        if (seg != null) {
            clickOnSeg(seg);
        }

        seg = detectCue(cues.get("Hall"), SECOND * 5);
        if (seg != null) {
            clickOnSeg(seg);
        }
    }

    private void handleVictory() {

        BufferedImage victoryPopUpImg = img;

        if (BHBot.settings.victoryScreenshot) {
            saveGameScreen("victory-" + state, img);
        }

        if (BHBot.settings.enablePushover) {
            readScreen();

            String DroppedItem = "";
            Bounds victoryDropArea = new Bounds(175, 340, 625, 425);

            if (BHBot.settings.poNotifyDrop.contains("l") &&
                    detectCue(cues.get("ItemLeg"), 0, victoryDropArea) != null) {
                DroppedItem += "Legendary Item dropped";
            }
            if (BHBot.settings.poNotifyDrop.contains("s") &&
                    detectCue(cues.get("ItemSet"), 0, victoryDropArea) != null) {
                if (DroppedItem.length() > 0) DroppedItem += "\n";
                DroppedItem += "Set Item dropped";
            }
            if (BHBot.settings.poNotifyDrop.contains("m") &&
                    detectCue(cues.get("ItemMyt"), 0, victoryDropArea) != null) {
                if (DroppedItem.length() > 0) DroppedItem += "\n";
                DroppedItem += "Mythical Item dropped";
            }

            if (DroppedItem.length() > 0) {
                String victoryScreenName = saveGameScreen("victory-screen", victoryPopUpImg);
                File victoryScreenFile = victoryScreenName != null ? new File(victoryScreenName): null;
                sendPushOverMessage(state + " item Drop", DroppedItem, "magic", MessagePriority.HIGH, victoryScreenFile);
                if (victoryScreenFile != null && !victoryScreenFile.delete())
                    BHBot.logger.warn("Impossible to delete tmp img file for victory drop.");
            }
        }

    }

    public enum State {
        Dungeon("Dungeon", "d"),
        Expedition("Expedition", "e"),
        Gauntlet("Gauntlet", "g"),
        GVG("GVG", "v"),
        Invasion("Invasion", "i"),
        Loading("Loading..."),
        Main("Main screen"),
        PVP("PVP", "p"),
        Raid("Raid", "r"),
        Trials("Trials", "t"),
        UnidentifiedDungeon("Unidentified dungeon", "ud"), // this one is used when we log in and we get a "You were recently disconnected from a dungeon. Do you want to continue the dungeon?" window
        WorldBoss("World Boss", "w");

        private String name;
        private String shortcut;

        State(String name) {
            this.name = name;
            this.shortcut = null;
        }

        State(String name, String shortcut) {
            this.name = name;
            this.shortcut = shortcut;
        }

        public String getName() {
            return name;
        }

        public String getShortcut() {
            return shortcut;
        }

        public String getNameFromShortcut(String shortcut) {
            for (State state : State.values())
                if (state.shortcut != null && state.shortcut.equals(shortcut))
                    return state.name;
            return null;
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

    /**
     * Events that use badges as "fuel".
     */
    public enum BadgeEvent {
        None,
        GVG,
        Expedition,
        Invasion
    }

    public enum EquipmentType {
        Mainhand("StripTypeMainhand"),
        Offhand("StripTypeOffhand"),
        Head("StripTypeHead"),
        Body("StripTypeBody"),
        Neck("StripTypeNeck"),
        Ring("StripTypeRing");

        private String cueName;

        EquipmentType(String cueName) {
            this.cueName = cueName;
        }

        public static String letterToName(String s) {
            switch (s) {
                case "m":
                    return "mainhand";
                case "o":
                    return "offhand";
                case "h":
                    return "head";
                case "b":
                    return "body";
                case "n":
                    return "neck";
                case "r":
                    return "ring";
                default:
                    return "unknown_item";
            }
        }

        public static EquipmentType letterToType(String s) {
            switch (s) {
                case "m":
                    return Mainhand;
                case "o":
                    return Offhand;
                case "h":
                    return Head;
                case "b":
                    return Body;
                case "n":
                    return Neck;
                case "r":
                    return Ring;
                default:
                    return null; // should not happen!
            }
        }

//		public int maxPos() {
////			return Math.min(6 + ordinal(), 10);
////		}

        /**
         * Returns equipment filter button cue (it's title cue actually)
         */
        public Cue getCue() {
            return cues.get(cueName);
        }

        public int minPos() {
            return 4 + ordinal();
        }

        public int getButtonPos() {
            return 8 + ordinal();
        }
    }

    public enum StripDirection {
        StripDown,
        DressUp
    }

    private enum ConsumableType {
        EXP_MINOR("exp_minor", "ConsumableExpMinor"), // experience tome
        EXP_AVERAGE("exp_average", "ConsumableExpAverage"),
        EXP_MAJOR("exp_major", "ConsumableExpMajor"),

        ITEM_MINOR("item_minor", "ConsumableItemMinor"), // item find scroll
        ITEM_AVERAGE("item_average", "ConsumableItemAverage"),
        ITEM_MAJOR("item_major", "ConsumableItemMajor"),

        GOLD_MINOR("gold_minor", "ConsumableGoldMinor"), // item find scroll
        GOLD_AVERAGE("gold_average", "ConsumableGoldAverage"),
        GOLD_MAJOR("gold_major", "ConsumableGoldMajor"),

        SPEED_MINOR("speed_minor", "ConsumableSpeedMinor"), // speed kicks
        SPEED_AVERAGE("speed_average", "ConsumableSpeedAverage"),
        SPEED_MAJOR("speed_major", "ConsumableSpeedMajor");

        private String name;
        private String inventoryCue;

        ConsumableType(String name, String inventoryCue) {
            this.name = name;
            this.inventoryCue = inventoryCue;
        }

        public static ConsumableType getTypeFromName(String name) {
            for (ConsumableType type : ConsumableType.values())
                if (type.name.equals(name))
                    return type;
            return null;
        }

        /**
         * Returns name as it appears in e.g. settings.ini.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns image cue from inventory window
         */
        public Cue getInventoryCue() {
            return cues.get(inventoryCue);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum ItemGrade {
        COMMON("Common", 1),
        RARE("Rare", 2),
        EPIC("Epic", 3),
        LEGENDARY("Legendary", 4);
		/*SET("Set", 5),
		MYTHIC("Mythic", 6),
		ANCIENT("Ancient", 6);*/

        private final String name;
        private final int value;

        ItemGrade(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public static ItemGrade getGradeFromValue(int value) {
            for (ItemGrade grade : ItemGrade.values())
                if (grade.value == value)
                    return grade;
            return null;
        }

        public int getValue() {
            return value;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private enum MinorRuneEffect {
        CAPTURE("Capture"),
        EXPERIENCE("Experience"),
        GOLD("Gold"),
        ITEM_FIND("Item_Find");

        private final String name;

        MinorRuneEffect(String name) {
            this.name = name;
        }

        public static MinorRuneEffect getEffectFromName(String name) {
            for (MinorRuneEffect effect : MinorRuneEffect.values())
                if (effect.name.toLowerCase().equals(name.toLowerCase()))
                    return effect;
            return null;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @SuppressWarnings("unused")
    private enum MinorRune {
        EXP_COMMON(MinorRuneEffect.EXPERIENCE, ItemGrade.COMMON),
        EXP_RARE(MinorRuneEffect.EXPERIENCE, ItemGrade.RARE),
        EXP_EPIC(MinorRuneEffect.EXPERIENCE, ItemGrade.EPIC),
        EXP_LEGENDARY(MinorRuneEffect.EXPERIENCE, ItemGrade.LEGENDARY),

        ITEM_COMMON(MinorRuneEffect.ITEM_FIND, ItemGrade.COMMON),
        ITEM_RARE(MinorRuneEffect.ITEM_FIND, ItemGrade.RARE),
        ITEM_EPIC(MinorRuneEffect.ITEM_FIND, ItemGrade.EPIC),
        ITEM_LEGENDARY(MinorRuneEffect.ITEM_FIND, ItemGrade.LEGENDARY),

        GOLD_COMMON(MinorRuneEffect.GOLD, ItemGrade.COMMON),
        //		GOLD_RARE(MinorRuneEffect.GOLD, ItemGrade.RARE),
//		GOLD_EPIC(MinorRuneEffect.GOLD, ItemGrade.EPIC),
        GOLD_LEGENDARY(MinorRuneEffect.GOLD, ItemGrade.LEGENDARY),

        CAPTURE_COMMON(MinorRuneEffect.CAPTURE, ItemGrade.COMMON),
        CAPTURE_RARE(MinorRuneEffect.CAPTURE, ItemGrade.RARE),
        CAPTURE_EPIC(MinorRuneEffect.CAPTURE, ItemGrade.EPIC),
        CAPTURE_LEGENDARY(MinorRuneEffect.CAPTURE, ItemGrade.LEGENDARY);

        public static ItemGrade maxGrade = ItemGrade.LEGENDARY;
        private MinorRuneEffect effect;
        private ItemGrade grade;

        MinorRune(MinorRuneEffect effect, ItemGrade grade) {
            this.effect = effect;
            this.grade = grade;
        }

        public static MinorRune getRune(MinorRuneEffect effect, ItemGrade grade) {
            for (MinorRune rune : MinorRune.values()) {
                if (rune.effect == effect && rune.grade == grade)
                    return rune;
            }
            return null;
        }

        public MinorRuneEffect getRuneEffect() {
            return effect;
        }

        public String getRuneCueName() {
            return "MinorRune" + effect + grade;
        }

        public String getRuneCueFileName() {
            return "cues/runes/minor" + effect + grade + ".png";
        }

        public Cue getRuneCue() {
            return cues.get(getRuneCueName());
        }


        public String getRuneSelectCueName() {
            return "MinorRune" + effect + grade + "Select";
        }

        public String getRuneSelectCueFileName() {
            return "cues/runes/minor" + effect + grade + "Select.png";
        }

        public Cue getRuneSelectCue() {
            return cues.get(getRuneSelectCueName());
        }

        @Override
        public String toString() {
            return grade.toString().toLowerCase() + "_" + effect.toString().toLowerCase();
        }
    }

    @SuppressWarnings("unused")
    private enum WorldBoss {

        Orlag("o", "Orlag Clan", 1),
        Netherworld("n", "Netherworld", 2),
        Melvin("m", "Melvin", 3),
        Ext3rmin4tion("3", "3xt3rmin4tion", 4),
        BrimstoneSyndicate("b", "Brimstone Syndicate", 5),
        Unknown("?", "Unknown", 6);

        private String letter;
        private String Name;
        private int number;

        WorldBoss(String letter, String Name, int number) {
            this.letter = letter;
            this.Name = Name;
            this.number = number;
        }

        String getLetter() {
            return letter;
        }

        String getName() {
            return Name;
        }

        int getNumber() {
            return number;
        }

        static WorldBoss fromLetter(String Letter) {
            for (WorldBoss wb: WorldBoss.values()) {
                if (wb.getLetter().equals(Letter)) return wb;
            }
            return null;
        }

        static WorldBoss fromNumber(int number) {
            for (WorldBoss wb: WorldBoss.values()) {
                if (wb.getNumber() == number) return wb;
            }
            return null;
        }
    }

    void cueDifference(){ //return similarity % between two screenshots taken 3 seconds apart
        readScreen();
        BufferedImage img1 = img;
        sleep(2500);
        readScreen();
        BufferedImage img2 = img;
        CueCompare.imageDifference(img1, img2, 0.8, 0, 800, 0 ,520);
    }

    void handleWeeklyRewards() {
        // check for weekly rewards popup
        // (note that several, 2 or even 3 such popups may open one after another)
        MarvinSegment seg;
        if (state == State.Loading || state == State.Main) {
            readScreen();

            seg = detectCue(cues.get("PVP_Rewards"));
            if (seg != null) {
                BufferedImage reward = img;
                seg = detectCue(cues.get("X"), 5 * SECOND);
                if (seg != null) clickOnSeg(seg);
                else {
                    BHBot.logger.error("PvP reward popup detected, however could not detect the X button. Restarting...");
                    restart();
                }

                BHBot.logger.info("PVP reward claimed successfully.");
                if ((BHBot.settings.screenshots.contains("w"))) {
                    saveGameScreen("pvp_reward","rewards", reward);
                }
            }

            seg = detectCue(cues.get("Trials_Rewards"));
            if (seg != null) {
                BufferedImage reward = img;
                seg = detectCue(cues.get("X"), 5 * SECOND);
                if (seg != null) clickOnSeg(seg);
                else {
                    BHBot.logger.error("Trials reward popup detected, however could not detect the X button. Restarting...");
                    restart();
                }

                BHBot.logger.info("Trials reward claimed successfully.");
                if ((BHBot.settings.screenshots.contains("w"))) {
                    saveGameScreen("trials_reward","rewards", reward);
                }
            }

            seg = detectCue(cues.get("Gauntlet_Rewards"));
            if (seg != null) {
                BufferedImage reward = img;
                seg = detectCue(cues.get("X"), 5 * SECOND);
                if (seg != null) clickOnSeg(seg);
                else {
                    BHBot.logger.error("Gauntlet reward popup detected, however could not detect the X button. Restarting...");
                    restart();
                }

                BHBot.logger.info("Gauntlet reward claimed successfully.");
                if ((BHBot.settings.screenshots.contains("w"))) {
                    saveGameScreen("gauntlet_reward","rewards", reward);
                }
            }

            seg = detectCue(cues.get("Fishing_Rewards"));
            if (seg != null) {
                BufferedImage reward = img;
                seg = detectCue(cues.get("X"), 5 * SECOND);
                if (seg != null) clickOnSeg(seg);
                else {
                    BHBot.logger.error("Fishing reward popup detected, however could not detect the X button. Restarting...");
                    restart();
                }

                BHBot.logger.info("Fishing reward claimed successfully.");
                if ((BHBot.settings.screenshots.contains("w"))) {
                    saveGameScreen("fishing_reward","rewards", reward);
                }
            }

            seg = detectCue(cues.get("Invasion_Rewards"));
            if (seg != null) {
                BufferedImage reward = img;
                seg = detectCue(cues.get("X"), 5 * SECOND);
                if (seg != null) clickOnSeg(seg);
                else {
                    BHBot.logger.error("Invasion reward popup detected, however could not detect the X button. Restarting...");
                    restart();
                }

                BHBot.logger.info("Invasion reward claimed successfully.");
                if ((BHBot.settings.screenshots.contains("w"))) {
                    saveGameScreen("invasion_reward","rewards", reward);
                }
            }

            seg = detectCue(cues.get("Expedition_Rewards"));
            if (seg != null) {
                BufferedImage reward = img;
                seg = detectCue(cues.get("X"), 5 * SECOND);
                if (seg != null) clickOnSeg(seg);
                else {
                    BHBot.logger.error("Expedition popup detected, however could not detect the X button. Restarting...");
                    restart();
                }

                BHBot.logger.info("Expedition reward claimed successfully.");
                if ((BHBot.settings.screenshots.contains("w"))) {
                    saveGameScreen("expedition_reward","rewards", reward);
                }
            }

            seg = detectCue(cues.get("GVG_Rewards"));
            if (seg != null) {
                BufferedImage reward = img;
                seg = detectCue(cues.get("X"), 5 * SECOND);
                if (seg != null) clickOnSeg(seg);
                else {
                    BHBot.logger.error("GVG popup detected, however could not detect the X button. Restarting...");
                    restart();
                }

                BHBot.logger.info("GVG reward claimed successfully.");
                if ((BHBot.settings.screenshots.contains("w"))) {
                    saveGameScreen("gvg_reward","rewards", reward);
                }
            }
        }
    }

}
