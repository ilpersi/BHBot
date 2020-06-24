package com.github.ilpersi.BHBot;

import com.google.common.collect.Maps;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.openqa.selenium.Point;

import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Comparator.comparing;

public class DungeonThread implements Runnable {

    private static int globalShards;
    private static int globalBadges;
    private static int globalEnergy;
    private static int globalTickets;
    private static int globalTokens;

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
    private final Pattern dungeonRegex = Pattern.compile("z(?<zone>\\d{1,2})d(?<dungeon>[1234])\\s+(?<difficulty>[123])");
    @SuppressWarnings("FieldCanBeLocal")
    private final long MAX_IDLE_TIME = 15 * Misc.Durations.MINUTE;

    //private final boolean[] revived = {false, false, false, false, false};
    //private int potionsUsed = 0;
    private boolean startTimeCheck = false;
    private long activityStartTime;
    private boolean encounterStatus = true;
    private long outOfEncounterTimestamp = 0;
    private long inEncounterTimestamp = 0;
    private boolean specialDungeon; //d4 check for closing properly when no energy
    private String expeditionFailsafePortal = "";
    private int expeditionFailsafeDifficulty = 0;

    // Generic counters HashMap
    HashMap<BHBot.State, DungeonCounter> counters = new HashMap<>();

    // When we do not have anymore gems to use this is true
    private boolean noGemsToBribe = false;

    private long ENERGY_CHECK_INTERVAL = 10 * Misc.Durations.MINUTE;
    private long TICKETS_CHECK_INTERVAL = 10 * Misc.Durations.MINUTE;
    private long TOKENS_CHECK_INTERVAL = 10 * Misc.Durations.MINUTE;
    private long BADGES_CHECK_INTERVAL = 10 * Misc.Durations.MINUTE;
    @SuppressWarnings("FieldCanBeLocal")
    private final long BONUS_CHECK_INTERVAL = 10 * Misc.Durations.MINUTE;

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
    private long timeLastDailyGem = 0; // when did we check for daily gem screenshot last time?
    private long timeLastWeeklyGem = Misc.getTime(); // when did we check for weekly gem screenshot last time?

    /**
     * global autorune vals
     */
    private boolean autoBossRuned = false;
    private boolean oneTimeRuneCheck = false;
    private MinorRune leftMinorRune;
    private MinorRune rightMinorRune;

    BHBot bot;
    AutoShrineManager shrineManager;
    AutoReviveManager reviveManager;

    private Iterator<String> activitysIterator;

    // Weekly Sunday screenshots cache
    HashMap<String, Boolean> sundayScreenshots = new HashMap<>();

    DungeonThread(BHBot bot) {
        this.bot = bot;

        activitysIterator = bot.settings.activitiesEnabled.iterator();
        reviveManager = new AutoReviveManager(bot);
    }

    static void printFamiliars() {

        List<String> folders = new ArrayList<>();
        folders.add("cues/familiars/01 Common");
        folders.add("cues/familiars/02 Rare");
        folders.add("cues/familiars/03 Epic");
        folders.add("cues/familiars/04 Legendary");

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

    void restart() {
        restart(true); // assume emergency restart
    }

    void restart(boolean emergency) {

        if (bot.settings.idleMode) {
            oneTimeRuneCheck = true;
        }
        bot.restart(emergency, false); // assume emergency restart
    }

    public void run() {
        BHBot.logger.info("Bot started successfully!");

        restart(false);

        // We initialize the counter HasMap using the state as key
        for (BHBot.State state : BHBot.State.values()) {
            counters.put(state, new DungeonCounter(0, 0));
        }

        while (!bot.finished) {
            bot.scheduler.backupIdleTime();
            try {
                bot.scheduler.process();
                if (bot.scheduler.isPaused()) continue;

                if (Misc.getTime() - bot.scheduler.getIdleTime() > MAX_IDLE_TIME) {
                    BHBot.logger.warn("Idle time exceeded... perhaps caught in a loop? Restarting... (state=" + bot.getState() + ")");
                    bot.saveGameScreen("idle-timeout-error", "errors");

                    // Safety measure to avoid being stuck forever in dungeons
                    if (bot.getState() != BHBot.State.Main && bot.getState() != BHBot.State.Loading) {
                        if (!bot.settings.autoRuneDefault.isEmpty()) {
                            BHBot.logger.info("Re-validating autoRunes");
                            if (!detectEquippedMinorRunes(true, true)) {
                                BHBot.logger.error("It was not possible to verify the equipped runes!");
                            }
                        }
                    }

                    bot.notificationManager.sendErrorNotification("Idle timer exceeded", "Idle time exceeded while state = " + bot.getState());

                    restart();
                    continue;
                }
                bot.scheduler.resetIdleTime();

                bot.browser.moveMouseAway(); // just in case. Sometimes we weren't able to claim daily reward because mouse was in center and popup window obfuscated the claim button (see screenshot of that error!)
                MarvinSegment seg;
                bot.browser.readScreen();

                //Dungeon crash failsafe, this can happen if you crash and reconnect quickly, then get placed back in the dungeon with no reconnect dialogue
                if (bot.getState() == BHBot.State.Loading) {
                    MarvinSegment autoOn = MarvinSegment.fromCue(BHBot.cues.get("AutoOn"), bot.browser);
                    MarvinSegment autoOff = MarvinSegment.fromCue(BHBot.cues.get("AutoOff"), bot.browser);
                    if (autoOn != null || autoOff != null) { //if we're in Loading state, with auto button visible, then we need to change state
                        bot.setState(BHBot.State.UnidentifiedDungeon); // we are not sure what type of dungeon we are doing
                        BHBot.logger.warn("Possible dungeon crash, activating failsafe");
                        bot.saveGameScreen("dungeon-crash-failsafe", "errors");
                        shrineManager.updateShrineSettings(false, false); //in case we are stuck in a dungeon lets enable shrines/boss
                        continue;
                    }
                }

                // process dungeons of any kind (if we are in any):
                if (bot.getState() == BHBot.State.Raid || bot.getState() == BHBot.State.Trials || bot.getState() == BHBot.State.Gauntlet || bot.getState() == BHBot.State.Dungeon || bot.getState() == BHBot.State.PVP || bot.getState() == BHBot.State.GVG || bot.getState() == BHBot.State.Invasion || bot.getState() == BHBot.State.UnidentifiedDungeon || bot.getState() == BHBot.State.Expedition || bot.getState() == BHBot.State.WorldBoss) {
                    processDungeon();
                    continue;
                }

                // check if we are in the main menu:
                seg = MarvinSegment.fromCue(BHBot.cues.get("Main"), bot.browser);

                if (seg != null) {

                    /* The bot is now fully started, so based on the options we search the logs looking for the
                     * do_not_share url and if we find it, we save it for later usage
                     */
                    if (!bot.browser.isDoNotShareUrl() && bot.settings.useDoNotShareURL) {
                        bot.restart(false, true);
                        continue;
                    }

                    bot.setState(BHBot.State.Main);

                    bot.notificationManager.sendStartUpnotification();

                    // weekly gem screenshot every Sunday and after or after a week the bot is running.
                    if (bot.settings.screenshots.contains("wg")) {
                        // The option is enabled and more than a week has passed
                        if (  (Misc.getTime() - timeLastWeeklyGem) > Misc.Durations.WEEK) {
                            timeLastWeeklyGem = Misc.getTime();

                            BufferedImage gems = bot.browser.getImg().getSubimage(133, 16, 80, 14);
                            bot.saveGameScreen("weekly-gems", "gems", gems);
                        } else {
                            // Less than a week has passed, we check if it is Sunday
                            if ("7".equals(new SimpleDateFormat("u").format(new Date()))) {
                                SimpleDateFormat sundayKeyFormat = new SimpleDateFormat("yyyyMMdd");
                                String sundayKey = sundayKeyFormat.format(new Date());

                                // if the date key is not there, it means that this Sunday we got no screenshot
                                if (!sundayScreenshots.getOrDefault(sundayKey, false)) {
                                    timeLastWeeklyGem = Misc.getTime();
                                    sundayScreenshots.put(sundayKey, true);

                                    BufferedImage gems = bot.browser.getImg().getSubimage(133, 16, 80, 14);
                                    bot.saveGameScreen("weekly-gems", "gems", gems);
                                }
                            }
                        }
                    }

                    // daily gem screenshot
                    if ((bot.settings.screenshots.contains("dg")) && (Misc.getTime() - timeLastDailyGem) > Misc.Durations.DAY) {
                        timeLastDailyGem = Misc.getTime();

                        BufferedImage gems = bot.browser.getImg().getSubimage(133, 16, 80, 14);
                        bot.saveGameScreen("daily-gems", "gems", gems); //else screenshot daily count
                    }

                    // check for bonuses:
                    if (bot.settings.autoConsume && (Misc.getTime() - timeLastBonusCheck > BONUS_CHECK_INTERVAL)) {
                        timeLastBonusCheck = Misc.getTime();
                        handleConsumables();
                    }

                    shrineManager.initialize();

                    // One time check for equipped minor runes
                    if (!bot.settings.autoRuneDefault.isEmpty() && !oneTimeRuneCheck) {

                        BHBot.logger.info("Startup check to determined configured minor runes");
                        if (!detectEquippedMinorRunes(true, true)) {
                            BHBot.logger.error("It was not possible to perform the equipped runes start-up check! Disabling autoRune..");
                            bot.settings.autoRuneDefault.clear();
                            bot.settings.autoRune.clear();
                            bot.settings.autoBossRune.clear();
                            continue;

                        }
                        BHBot.logger.info(getRuneName(leftMinorRune.getRuneCueName()) + " equipped in left slot.");
                        BHBot.logger.info(getRuneName(rightMinorRune.getRuneCueName()) + " equipped in right slot.");
                        oneTimeRuneCheck = true;
                        bot.browser.readScreen(2 * Misc.Durations.SECOND); // delay to close the settings window completely before we check for raid button else the settings window is hiding it
                    }

                    String currentActivity = activitySelector(); //else select the activity to attempt
                    if (currentActivity != null) {
                        BHBot.logger.debug("Checking activity: " + currentActivity);
                    } else {
                        // If we don't have any activity to perform, we reset the idle timer check
                        bot.scheduler.resetIdleTime(true);
                        continue;
                    }

                    // check for shards:
                    if ("r".equals(currentActivity)) {
                        timeLastShardsCheck = Misc.getTime();

                        bot.browser.readScreen();
                        MarvinSegment raidBTNSeg = MarvinSegment.fromCue(BHBot.cues.get("RaidButton"), bot.browser);

                        if (raidBTNSeg == null) { // if null, then raid button is transparent meaning that raiding is not enabled (we have not achieved it yet, for example)
                            bot.scheduler.restoreIdleTime();
                            continue;
                        }
                        bot.browser.clickOnSeg(raidBTNSeg);

                        seg = MarvinSegment.fromCue("RaidPopup", 5 * Misc.Durations.SECOND, bot.browser); // wait until the raid window opens
                        if (seg == null) {
                            BHBot.logger.warn("Error: attempt at opening raid window failed. No window cue detected. Ignoring...");
                            bot.scheduler.restoreIdleTime();
                            // we make sure that everything that can be closed is actually closed to avoid idle timeout
                            bot.browser.closePopupSecurely(BHBot.cues.get("X"), BHBot.cues.get("X"));
                            continue;
                        }


                        int shards = getShards();
                        globalShards = shards;
                        BHBot.logger.readout("Shards: " + shards + ", required: >" + bot.settings.minShards);

                        if (shards == -1) { // error
                            bot.scheduler.restoreIdleTime();
                            continue;
                        }

                        if ((shards == 0) || (!bot.scheduler.doRaidImmediately && (shards <= bot.settings.minShards || bot.settings.raids.size() == 0))) {
                            if (bot.scheduler.doRaidImmediately)
                                bot.scheduler.doRaidImmediately = false; // reset it

                            bot.browser.readScreen();
                            seg = MarvinSegment.fromCue(BHBot.cues.get("X"), Misc.Durations.SECOND, bot.browser);
                            bot.browser.clickOnSeg(seg);
                            Misc.sleep(Misc.Durations.SECOND);

                            continue;

                        } else { // do the raiding!

                            if (bot.scheduler.doRaidImmediately)
                                bot.scheduler.doRaidImmediately = false; // reset it

                            //if we need to configure runes/settings we close the window first
                            if (bot.settings.autoShrine.contains("r") || bot.settings.autoRune.containsKey("r") || bot.settings.autoBossRune.containsKey("r")) {
                                bot.browser.readScreen();
                                seg = MarvinSegment.fromCue(BHBot.cues.get("X"), Misc.Durations.SECOND, bot.browser);
                                bot.browser.clickOnSeg(seg);
                                bot.browser.readScreen(Misc.Durations.SECOND);
                            }

                            //autoshrine
                            if (bot.settings.autoShrine.contains("r")) {
                                BHBot.logger.info("Configuring autoShrine for Raid");
                                if (!shrineManager.updateShrineSettings(true, true)) {
                                    BHBot.logger.error("Impossible to configure autoShrine for Raid!");
                                }
                            }

                            //autoBossRune
                            if (bot.settings.autoBossRune.containsKey("r") && !bot.settings.autoShrine.contains("r")) { //if autoshrine disabled but autobossrune enabled
                                BHBot.logger.info("Configuring autoBossRune for Raid");
                                if (!shrineManager.updateShrineSettings(true, false)) {
                                    BHBot.logger.error("Impossible to configure autoBossRune for Raid!");
                                }
                            }

                            //activity runes
                            handleMinorRunes("r");

                            bot.browser.readScreen(Misc.Durations.SECOND);
                            bot.browser.clickOnSeg(raidBTNSeg);

                            String raid = decideRaidRandomly();
                            if (raid == null) {
                                bot.settings.activitiesEnabled.remove("r");
                                BHBot.logger.error("It was impossible to choose a raid randomly, raids are disabled!");
                                bot.notificationManager.sendErrorNotification("Raid Error", "It was impossible to choose a raid randomly, raids are disabled!");

                                continue;
                            }

                            int difficulty = Integer.parseInt(raid.split(" ")[1]);
                            int desiredRaid = Integer.parseInt(raid.split(" ")[0]);

                            if (!handleRaidSelection(desiredRaid, difficulty)) {
                                restart();
                                continue;
                            }

                            bot.browser.readScreen(2 * Misc.Durations.SECOND);
                            seg = MarvinSegment.fromCue(BHBot.cues.get("RaidSummon"), 2 * Misc.Durations.SECOND, bot.browser);
                            if (seg == null) {
                                BHBot.logger.error("Raid Summon button not found");
                                restart();
                                continue;
                            }
                            bot.browser.clickOnSeg(seg);
                            bot.browser.readScreen(2 * Misc.Durations.SECOND);

                            // dismiss character dialog if it pops up:
                            bot.browser.readScreen();
                            detectCharacterDialogAndHandleIt();

                            seg = MarvinSegment.fromCue(BHBot.cues.get(difficulty == 1 ? "Normal" : difficulty == 2 ? "Hard" : "Heroic"), bot.browser);
                            bot.browser.clickOnSeg(seg);
                            bot.browser.readScreen(2 * Misc.Durations.SECOND);

                            //seg = MarvinSegment.fromCue(BHBot.cues.get("Accept"), 5 * Misc.Durations.SECOND, bot.browser);
                            //bot.browser.clickOnSeg(seg);
                            bot.browser.closePopupSecurely(BHBot.cues.get("Accept"), BHBot.cues.get("Accept"));
                            bot.browser.readScreen(2 * Misc.Durations.SECOND);

                            if (handleTeamMalformedWarning()) {
                                BHBot.logger.error("Team incomplete, doing emergency restart..");
                                restart();
                                continue;
                            } else {
                                bot.setState(BHBot.State.Raid);
                                BHBot.logger.info("Raid initiated!");
                                autoBossRuned = false;
                            }
                        }
                        continue;
                    } // shards

                    // check for tokens (trials and gauntlet):
                    if (bot.scheduler.doTrialsImmediately || bot.scheduler.doGauntletImmediately ||
                            ("t".equals(currentActivity)) || ("g".equals(currentActivity))) {
                        if ("t".equals(currentActivity)) timeLastTrialsTokensCheck = Misc.getTime();
                        if ("g".equals(currentActivity)) timeLastGauntletTokensCheck = Misc.getTime();

                        bot.browser.readScreen();

                        boolean trials;
                        seg = MarvinSegment.fromCue(BHBot.cues.get("Trials"), bot.browser);
                        if (seg == null) seg = MarvinSegment.fromCue(BHBot.cues.get("Trials2"), bot.browser);
                        trials = seg != null; // if false, then we will do gauntlet instead of trials

                        if (seg == null)
                            seg = MarvinSegment.fromCue(BHBot.cues.get("Gauntlet"), bot.browser);
                        if (seg == null) {
                            seg = MarvinSegment.fromCue(BHBot.cues.get("Gauntlet2"), bot.browser);
                        }
                        if (seg == null) {// trials/gauntlet button not visible (perhaps it is disabled?)
                            BHBot.logger.warn("Gauntlet/Trials button not found");
                            bot.scheduler.restoreIdleTime();
                            continue;
                        }

                        if (("g".equals(currentActivity) && trials) || ("t".equals(currentActivity) && !trials))
                            continue;


                        bot.browser.clickOnSeg(seg);
                        MarvinSegment trialBTNSeg = seg;

                        // dismiss character dialog if it pops up:
                        bot.browser.readScreen(2 * Misc.Durations.SECOND);
                        detectCharacterDialogAndHandleIt();

                        bot.browser.readScreen();
                        int tokens = getTokens();
                        globalTokens = tokens;
                        BHBot.logger.readout("Tokens: " + tokens + ", required: >" + bot.settings.minTokens + ", " +
                                (trials ? "Trials" : "Gauntlet") + " cost: " + (trials ? bot.settings.costTrials : bot.settings.costGauntlet));

                        if (tokens == -1) { // error
                            bot.scheduler.restoreIdleTime();
                            continue;
                        }

                        if (((!bot.scheduler.doTrialsImmediately && !bot.scheduler.doGauntletImmediately) && (tokens <= bot.settings.minTokens)) || (tokens < (trials ? bot.settings.costTrials : bot.settings.costGauntlet))) {
                            bot.browser.readScreen();
                            seg = MarvinSegment.fromCue(BHBot.cues.get("X"), Misc.Durations.SECOND, bot.browser);
                            bot.browser.clickOnSeg(seg);
                            bot.browser.readScreen(Misc.Durations.SECOND);

                            //if we have 1 token and need 5 we don't need to check every 10 minutes, this increases the timer so we start checking again when we are one token short
                            int tokenDifference = (trials ? bot.settings.costTrials : bot.settings.costGauntlet) - tokens; //difference between needed and current resource
                            if (tokenDifference > 1) {
                                int increase = (tokenDifference - 1) * 45;
                                TOKENS_CHECK_INTERVAL = increase * Misc.Durations.MINUTE; //add 45 minutes to TOKENS_CHECK_INTERVAL for each token needed above 1
                            } else
                                TOKENS_CHECK_INTERVAL = 10 * Misc.Durations.MINUTE; //if we only need 1 token check every 10 minutes

                            if (bot.scheduler.doTrialsImmediately) {
                                bot.scheduler.doTrialsImmediately = false; // if we don't have resources to run we need to disable force it
                            } else if (bot.scheduler.doGauntletImmediately) {
                                bot.scheduler.doGauntletImmediately = false;
                            }

                            continue;
                        } else {
                            // do the trials/gauntlet!

                            if (bot.scheduler.doTrialsImmediately) {
                                bot.scheduler.doTrialsImmediately = false; // reset it
                            } else if (bot.scheduler.doGauntletImmediately) {
                                bot.scheduler.doGauntletImmediately = false;
                            }

                            // One time check for Autoshrine
                            //wait for window animation
                            if (trials) {
                                //if we need to configure runes/settings we close the window first
                                if (bot.settings.autoShrine.contains("t") || bot.settings.autoRune.containsKey("t") || bot.settings.autoBossRune.containsKey("t")) {
                                    bot.browser.readScreen();
                                    seg = MarvinSegment.fromCue(BHBot.cues.get("X"), Misc.Durations.SECOND, bot.browser);
                                    bot.browser.clickOnSeg(seg);
                                    bot.browser.readScreen(Misc.Durations.SECOND);
                                }

                                //autoshrine
                                if (bot.settings.autoShrine.contains("t")) {
                                    BHBot.logger.info("Configuring autoShrine for Trials");
                                    if (!shrineManager.updateShrineSettings(true, true)) {
                                        BHBot.logger.error("Impossible to configure autoShrine for Trials!");
                                    }
                                }

                                //autoBossRune
                                if (bot.settings.autoBossRune.containsKey("t") && !bot.settings.autoShrine.contains("t")) { //if autoshrine disabled but autobossrune enabled
                                    BHBot.logger.info("Configuring autoBossRune for Trials");
                                    if (!shrineManager.updateShrineSettings(true, false)) {
                                        BHBot.logger.error("Impossible to configure autoBossRune for Trials!");
                                    }
                                }

                                //activity runes
                                handleMinorRunes("t");

                            } else {

                                if (bot.settings.autoRune.containsKey("g")) {
                                    handleMinorRunes("g");
                                    bot.browser.readScreen(Misc.Durations.SECOND);
                                }

                            }
                            bot.browser.readScreen(Misc.Durations.SECOND);
                            bot.browser.clickOnSeg(trialBTNSeg);
                            bot.browser.readScreen(Misc.Durations.SECOND); //wait for window animation

                            // apply the correct difficulty
                            int targetDifficulty = trials ? bot.settings.difficultyTrials : bot.settings.difficultyGauntlet;

                            BHBot.logger.info("Attempting " + (trials ? "trials" : "gauntlet") + " at level " + targetDifficulty + "...");

                            int difficulty = detectDifficulty();
                            if (difficulty == 0) { // error!
                                BHBot.logger.error("Due to an error#1 in difficulty detection, " + (trials ? "trials" : "gauntlet") + " will be skipped.");
                                bot.browser.closePopupSecurely(BHBot.cues.get("TrialsOrGauntletWindow"), BHBot.cues.get("X"));
                                continue;
                            }
                            if (difficulty != targetDifficulty) {
                                BHBot.logger.info("Detected " + (trials ? "trials" : "gauntlet") + " difficulty level: " + difficulty + ", settings level: " + targetDifficulty + ". Changing..");
                                boolean result = selectDifficulty(difficulty, targetDifficulty, BHBot.cues.get("SelectDifficulty"), 1);
                                if (!result) { // error!
                                    // see if drop down menu is still open and close it:
                                    bot.browser.readScreen(Misc.Durations.SECOND);
                                    tryClosingWindow(BHBot.cues.get("DifficultyDropDown"));
                                    bot.browser.readScreen(5 * Misc.Durations.SECOND);
                                    BHBot.logger.warn("Unable to change difficulty, usually because desired level is not unlocked. Running " + (trials ? "trials" : "gauntlet") + " at " + difficulty + ".");
                                    bot.notificationManager.sendErrorNotification("T/G Error", "Unable to change " + (trials ? "trials" : "gauntlet") + " difficulty to : " + targetDifficulty + " Running: " + difficulty + " instead.");

                                    // We update the setting file with the old difficulty level
                                    String settingName = trials ? "difficultyTrials": "difficultyGauntlet";
                                    String original = settingName + " " + targetDifficulty;
                                    String updated = settingName + " " + difficulty;
                                    settingsUpdate(original, updated);

                                }
                            }

                            // select cost if needed:
                            bot.browser.readScreen(2 * Misc.Durations.SECOND); // wait for the popup to stabilize a bit
                            int cost = detectCost();
                            if (cost == 0) { // error!
                                BHBot.logger.error("Due to an error#1 in cost detection, " + (trials ? "trials" : "gauntlet") + " will be skipped.");
                                bot.browser.closePopupSecurely(BHBot.cues.get("TrialsOrGauntletWindow"), BHBot.cues.get("X"));
                                continue;
                            }
                            if (cost != (trials ? bot.settings.costTrials : bot.settings.costGauntlet)) {
                                BHBot.logger.info("Detected " + (trials ? "trials" : "gauntlet") + " cost: " + cost + ", settings cost is " + (trials ? bot.settings.costTrials : bot.settings.costGauntlet) + ". Changing it...");
                                boolean result = selectCost(cost, (trials ? bot.settings.costTrials : bot.settings.costGauntlet));
                                if (!result) { // error!
                                    // see if drop down menu is still open and close it:
                                    bot.browser.readScreen(Misc.Durations.SECOND);
                                    tryClosingWindow(BHBot.cues.get("CostDropDown"));
                                    bot.browser.readScreen(5 * Misc.Durations.SECOND);
                                    tryClosingWindow(BHBot.cues.get("TrialsOrGauntletWindow"));
                                    BHBot.logger.error("Due to an error#2 in cost selection, " + (trials ? "trials" : "gauntlet") + " will be skipped.");
                                    continue;
                                }

                                // We wait for the cost selector window to close
                                MarvinSegment.fromCue("TrialsOrGauntletWindow", Misc.Durations.SECOND * 2, bot.browser);
                                bot.browser.readScreen();
                            }

                            seg = MarvinSegment.fromCue(BHBot.cues.get("Play"), 2 * Misc.Durations.SECOND, bot.browser);
                            if (seg == null) {
                                BHBot.logger.error("Error: Play button not found while trying to do " + (trials ? "trials" : "gauntlet") + ". Ignoring...");
                                tryClosingWindow(BHBot.cues.get("TrialsOrGauntletWindow"));
                                continue;
                            }
                            bot.browser.clickOnSeg(seg);
                            bot.browser.readScreen(2 * Misc.Durations.SECOND);

                            if (!handleNotEnoughTokensPopup(false)) {
                                restart();
                                continue;
                            }

                            // dismiss character dialog if it pops up:
                            detectCharacterDialogAndHandleIt();

                            //seg = MarvinSegment.fromCue(BHBot.cues.get("Accept"), 5 * Misc.Durations.SECOND, bot.browser);
                            //bot.browser.clickOnSeg(seg);
                            bot.browser.closePopupSecurely(BHBot.cues.get("Accept"), BHBot.cues.get("Accept"));
                            bot.browser.readScreen(2 * Misc.Durations.SECOND);

                            // This is a Bit Heroes bug!
                            // On t/g main screen the token bar is wrongly full so it goes trough the "Play" button and
                            // then it fails on the team "Accept" button
                            if (!handleNotEnoughTokensPopup(true)) {
                                restart();
                                continue;
                            }

                            Misc.sleep(3 * Misc.Durations.SECOND);

                            if (handleTeamMalformedWarning()) {
                                BHBot.logger.error("Team incomplete, doing emergency restart..");
                                restart();
                                continue;
                            } else {
                                bot.setState(trials ? BHBot.State.Trials : BHBot.State.Gauntlet);
                                BHBot.logger.info((trials ? "Trials" : "Gauntlet") + " initiated!");
                                autoBossRuned = false;
                            }
                        }
                        continue;
                    } // tokens (trials and gauntlet)

                    // check for energy:
                    if ("d".equals(currentActivity)) {
                        timeLastEnergyCheck = Misc.getTime();

                        bot.browser.readScreen();

                        int energy = getEnergy();
                        globalEnergy = energy;
                        BHBot.logger.readout("Energy: " + energy + "%, required: >" + bot.settings.minEnergyPercentage + "%");

                        if (energy == -1) { // error
                            bot.scheduler.restoreIdleTime();
                            if (bot.scheduler.doDungeonImmediately)
                                bot.scheduler.doDungeonImmediately = false; // reset it
                            continue;
                        }

                        if (!bot.scheduler.doDungeonImmediately && (energy <= bot.settings.minEnergyPercentage || bot.settings.dungeons.size() == 0)) {
                            Misc.sleep(Misc.Durations.SECOND);

                            //if we have 1 resource and need 5 we don't need to check every 10 minutes, this increases the timer so we start checking again when we are one under the check limit
                            int energyDifference = bot.settings.minEnergyPercentage - energy; //difference between needed and current resource
                            if (energyDifference > 1) {
                                int increase = (energyDifference - 1) * 8;
                                ENERGY_CHECK_INTERVAL = increase * Misc.Durations.MINUTE; //add 8 minutes to the check interval for each energy % needed above 1
                            } else
                                ENERGY_CHECK_INTERVAL = 10 * Misc.Durations.MINUTE; //if we only need 1 check every 10 minutes

                            continue;
                        } else {
                            // do the dungeon!

                            if (bot.scheduler.doDungeonImmediately)
                                bot.scheduler.doDungeonImmediately = false; // reset it

                            //configure activity runes
                            handleMinorRunes("d");

                            if (bot.settings.autoBossRune.containsKey("d") && !bot.settings.autoShrine.contains("d")) { //if autoshrine disabled but autorune enabled

                                BHBot.logger.info("Configuring autoBossRune for Dungeons");
                                if (!shrineManager.updateShrineSettings(true, false)) {
                                    BHBot.logger.error("Impossible to configure autoBossRune for Dungeons!");
                                }

                                bot.browser.readScreen(Misc.Durations.SECOND);
                                Misc.sleep(2 * Misc.Durations.SECOND);
                            }

                            seg = MarvinSegment.fromCue(BHBot.cues.get("Quest"), bot.browser);
                            bot.browser.clickOnSeg(seg);
                            bot.browser.readScreen(5 * Misc.Durations.SECOND);

                            String dungeon = decideDungeonRandomly();
                            if (dungeon == null) {
                                bot.settings.activitiesEnabled.remove("d");
                                BHBot.logger.error("It was impossible to choose a dungeon randomly, dungeons are disabled!");
                                bot.notificationManager.sendErrorNotification("Dungeon error", "It was impossible to choose a dungeon randomly, dungeons are disabled!");
                                continue;
                            }

                            Matcher dungeonMatcher = dungeonRegex.matcher(dungeon.toLowerCase());
                            if (!dungeonMatcher.find()) {
                                BHBot.logger.error("Wrong format in dungeon detected: " + dungeon + "! It will be skipped...");
                                bot.notificationManager.sendErrorNotification("Dungeon error", "Wrong dungeon format detected: " + dungeon);
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
                                    bot.browser.readScreen(Misc.Durations.SECOND); // wait for screen to stabilise
                                    seg = MarvinSegment.fromCue(BHBot.cues.get("RightArrow"), bot.browser);
                                    if (seg == null) {
                                        BHBot.logger.error("Right button not found, zone unlocked?");
                                        break; // happens for example when player hasn't unlock the zone yet
                                    }
                                    //coords are used as moving multiple screens would crash the bot when using the arrow cues
                                    bot.browser.clickInGame(740, 275);
                                    vec--;
                                } else {
                                    Misc.sleep(500);
                                    //coords are used as moving multiple screens would crash the bot when using the arrow cues
                                    bot.browser.clickInGame(55, 275);
                                    vec++;
                                }
                            }

                            bot.browser.readScreen(2 * Misc.Durations.SECOND);

                            // click on the dungeon:
                            Point p = getDungeonIconPos(goalZone, goalDungeon);
                            if (p == null) {
                                bot.settings.activitiesEnabled.remove("d");
                                BHBot.logger.error("It was impossible to get icon position of dungeon z" + goalZone + "d" + goalDungeon + ". Dungeons are now disabled!");
                                bot.notificationManager.sendErrorNotification("Dungeon error", "It was impossible to get icon position of dungeon z" + goalZone + "d" + goalDungeon + ". Dungeons are now disabled!");
                                continue;
                            }

                            bot.browser.clickInGame(p.x, p.y);

                            bot.browser.readScreen(3 * Misc.Durations.SECOND);
                            // select difficulty (If D4 just hit enter):
                            if ((goalDungeon == 4) || (goalZone == 7 && goalDungeon == 3) || (goalZone == 8 && goalDungeon == 3)) { // D4, or Z7D3/Z8D3
                                specialDungeon = true;
                                seg = MarvinSegment.fromCue(BHBot.cues.get("Enter"), 5 * Misc.Durations.SECOND, bot.browser);
                            } else { //else select appropriate difficulty
                                seg = MarvinSegment.fromCue(BHBot.cues.get(difficulty == 1 ? "Normal" : difficulty == 2 ? "Hard" : "Heroic"), 5 * Misc.Durations.SECOND, bot.browser);
                            }
                            bot.browser.clickOnSeg(seg);

                            //team selection screen
                            /* Solo-for-bounty code */
                            if (goalZone <= bot.settings.minSolo) { //if the level is soloable then clear the team to complete bounties
                                bot.browser.readScreen(Misc.Durations.SECOND);
                                seg = MarvinSegment.fromCue(BHBot.cues.get("Clear"), Misc.Durations.SECOND * 2, bot.browser);
                                if (seg != null) {
                                    BHBot.logger.info("Selected zone under dungeon solo threshold, attempting solo");
                                    bot.browser.clickOnSeg(seg);
                                } else {
                                    BHBot.logger.error("Impossible to find clear button in Dungeon Team!");
                                    restart();
                                    continue;
                                }
                            }

                            bot.browser.readScreen();
                            //seg = MarvinSegment.fromCue(BHBot.cues.get("Accept"), Misc.Durations.SECOND * 2, bot.browser);
                            //bot.browser.clickOnSeg(seg);
                            bot.browser.closePopupSecurely(BHBot.cues.get("Accept"), BHBot.cues.get("Accept"));

                            if (goalZone <= bot.settings.minSolo) {
                                bot.browser.readScreen(3 * Misc.Durations.SECOND); //wait for dropdown animation to finish
                                seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), 2 * Misc.Durations.SECOND, bot.browser);
                                if (seg != null) {
                                    bot.browser.clickOnSeg(seg);
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

                            if (handleNotEnoughEnergyPopup(3 * Misc.Durations.SECOND, BHBot.State.Dungeon)) {
                                continue;
                            }

                            bot.setState(BHBot.State.Dungeon);
                            autoBossRuned = false;

                            BHBot.logger.info("Dungeon <z" + goalZone + "d" + goalDungeon + "> " + (difficulty == 1 ? "normal" : difficulty == 2 ? "hard" : "heroic") + " initiated!");
                        }
                        continue;
                    } // energy

                    // check for Tickets (PvP):
                    if ("p".equals(currentActivity)) {
                        timeLastTicketsCheck = Misc.getTime();

                        bot.browser.readScreen();

                        int tickets = getTickets();
                        globalTickets = tickets;
                        BHBot.logger.readout("Tickets: " + tickets + ", required: >" + bot.settings.minTickets + ", PVP cost: " + bot.settings.costPVP);

                        if (tickets == -1) { // error
                            bot.scheduler.restoreIdleTime();
                            continue;
                        }

                        if ((!bot.scheduler.doPVPImmediately && (tickets <= bot.settings.minTickets)) || (tickets < bot.settings.costPVP)) {
                            Misc.sleep(Misc.Durations.SECOND);

                            //if we have 1 resource and need 5 we don't need to check every 10 minutes, this increases the timer so we start checking again when we are one under the check limit
                            int ticketDifference = bot.settings.costPVP - tickets; //difference between needed and current resource
                            if (ticketDifference > 1) {
                                int increase = (ticketDifference - 1) * 45;
                                TICKETS_CHECK_INTERVAL = increase * Misc.Durations.MINUTE; //add 45 minutes to the check interval for each ticket needed above 1
                            } else
                                TICKETS_CHECK_INTERVAL = 10 * Misc.Durations.MINUTE; //if we only need 1 check every 10 minutes

                            continue;
                        } else {
                            // do the pvp!

                            if (bot.scheduler.doPVPImmediately)
                                bot.scheduler.doPVPImmediately = false; // reset it

                            //configure activity runes
                            handleMinorRunes("p");

                            BHBot.logger.info("Attempting PVP...");
                            stripDown(bot.settings.pvpstrip);

                            seg = MarvinSegment.fromCue(BHBot.cues.get("PVP"), bot.browser);
                            if (seg == null) {
                                BHBot.logger.warn("PVP button not found. Skipping PVP...");
                                dressUp(bot.settings.pvpstrip);
                                continue; // should not happen though
                            }
                            bot.browser.clickOnSeg(seg);

                            // select cost if needed:
                            bot.browser.readScreen(2 * Misc.Durations.SECOND); // wait for the popup to stabilize a bit
                            int cost = detectCost();
                            if (cost == 0) { // error!
                                BHBot.logger.error("Due to an error#1 in cost detection, PVP will be skipped.");
                                bot.browser.closePopupSecurely(BHBot.cues.get("PVPWindow"), BHBot.cues.get("X"));
                                dressUp(bot.settings.pvpstrip);
                                continue;
                            }
                            if (cost != bot.settings.costPVP) {
                                BHBot.logger.info("Detected PVP cost: " + cost + ", settings cost is " + bot.settings.costPVP + ". Changing..");
                                boolean result = selectCost(cost, bot.settings.costPVP);
                                if (!result) { // error!
                                    // see if drop down menu is still open and close it:
                                    bot.browser.readScreen(Misc.Durations.SECOND);
                                    tryClosingWindow(BHBot.cues.get("CostDropDown"));
                                    bot.browser.readScreen(5 * Misc.Durations.SECOND);
                                    seg = MarvinSegment.fromCue(BHBot.cues.get("PVPWindow"), 15 * Misc.Durations.SECOND, bot.browser);
                                    if (seg != null)
                                        bot.browser.closePopupSecurely(BHBot.cues.get("PVPWindow"), BHBot.cues.get("X"));
                                    BHBot.logger.error("Due to an error#2 in cost selection, PVP will be skipped.");
                                    dressUp(bot.settings.pvpstrip);
                                    continue;
                                }
                            }

                            seg = MarvinSegment.fromCue(BHBot.cues.get("Play"), 5 * Misc.Durations.SECOND, bot.browser);
                            bot.browser.clickOnSeg(seg);
                            bot.browser.readScreen(2 * Misc.Durations.SECOND);

                            // dismiss character dialog if it pops up:
                            detectCharacterDialogAndHandleIt();

                            Bounds pvpOpponentBounds = opponentSelector(bot.settings.pvpOpponent);
                            String opponentName = (bot.settings.pvpOpponent == 1 ? "1st" : bot.settings.pvpOpponent == 2 ? "2nd" : bot.settings.pvpOpponent == 3 ? "3rd" : "4th");
                            BHBot.logger.info("Selecting " + opponentName + " opponent");
                            seg = MarvinSegment.fromCue("Fight", 5 * Misc.Durations.SECOND, pvpOpponentBounds, bot.browser);
                            if (seg == null) {
                                BHBot.logger.error("Imppossible to find the Fight button in the PVP screen, restarting!");
                                restart();
                                continue;
                            }
                            bot.browser.clickOnSeg(seg);

                            bot.browser.readScreen();
                            seg = MarvinSegment.fromCue("Accept", 5 * Misc.Durations.SECOND, new Bounds(430, 430, 630, 500), bot.browser);
                            if (seg == null) {
                                BHBot.logger.error("Impossible to find the Accept button in the PVP screen, restarting");
                                restart();
                                continue;
                            }
                            bot.browser.closePopupSecurely(BHBot.cues.get("Accept"), BHBot.cues.get("Accept"));
                            //bot.browser.clickOnSeg(seg);

                            if (handleTeamMalformedWarning()) {
                                BHBot.logger.error("Team incomplete, doing emergency restart..");
                                restart();
                                continue;
                            } else {
                                bot.setState(BHBot.State.PVP);
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

                        bot.browser.readScreen();

                        BadgeEvent badgeEvent = BadgeEvent.None;
                        MarvinSegment badgeBtn = null;

                        HashMap<Cue, BadgeEvent> badgeEvents = new HashMap<>();
                        badgeEvents.put(BHBot.cues.get("ExpeditionButton"), BadgeEvent.Expedition);
                        badgeEvents.put(BHBot.cues.get("GVG"), BadgeEvent.GVG);
                        badgeEvents.put(BHBot.cues.get("Invasion"), BadgeEvent.Invasion);

                        for (Map.Entry<Cue, BadgeEvent> event : badgeEvents.entrySet()) {
                            badgeBtn = MarvinSegment.fromCue(event.getKey(), bot.browser);
                            if (badgeBtn != null) {
                                badgeEvent = event.getValue();
                                seg = badgeBtn;
                                break;
                            }
                        }


                        if (badgeEvent == BadgeEvent.None) { // GvG/invasion button not visible (perhaps this week there is no GvG/Invasion/Expedition event?)
                            bot.scheduler.restoreIdleTime();
                            BHBot.logger.debug("No badge event found, skipping");
                            continue;
                        }

                        if (badgeEvent == BadgeEvent.Expedition) currentActivity = "e";
                        if (badgeEvent == BadgeEvent.Invasion) currentActivity = "i";
                        if (badgeEvent == BadgeEvent.GVG) currentActivity = "v";

                        if (!currentActivity.equals(checkedActivity)) { //if checked activity and chosen activity don't match we skip
                            continue;
                        }

                        bot.browser.clickOnSeg(seg);
                        Misc.sleep(2 * Misc.Durations.SECOND);

                        detectCharacterDialogAndHandleIt(); // needed for invasion

                        bot.browser.readScreen();
                        int badges = getBadges();
                        globalBadges = badges;
                        BHBot.logger.readout("Badges: " + badges + ", required: >" + bot.settings.minBadges + ", " + badgeEvent.toString() + " cost: " +
                                (badgeEvent == BadgeEvent.GVG ? bot.settings.costGVG : badgeEvent == BadgeEvent.Invasion ? bot.settings.costInvasion : bot.settings.costExpedition));

                        if (badges == -1) { // error
                            bot.scheduler.restoreIdleTime();
                            continue;
                        }

                        // check GVG:
                        if (badgeEvent == BadgeEvent.GVG) {
                            if ((!bot.scheduler.doGVGImmediately && (badges <= bot.settings.minBadges)) || (badges < bot.settings.costGVG)) {

                                //if we have 1 resource and need 5 we don't need to check every 10 minutes, this increases the timer so we start checking again when we are one under the check limit
                                int badgeDifference = bot.settings.costGVG - badges; //difference between needed and current resource
                                if (badgeDifference > 1) {
                                    int increase = (badgeDifference - 1) * 45;
                                    BADGES_CHECK_INTERVAL = increase * Misc.Durations.MINUTE; //add 45 minutes to the check interval for each ticket needed above 1
                                } else
                                    BADGES_CHECK_INTERVAL = 10 * Misc.Durations.MINUTE; //if we only need 1 check every 10 minutes

                                bot.browser.readScreen();
                                seg = MarvinSegment.fromCue(BHBot.cues.get("X"), Misc.Durations.SECOND, bot.browser);
                                bot.browser.clickOnSeg(seg);
                                Misc.sleep(Misc.Durations.SECOND);
                                continue;
                            } else {
                                // do the GVG!

                                if (bot.scheduler.doGVGImmediately)
                                    bot.scheduler.doGVGImmediately = false; // reset it


                                //configure activity runes
                                handleMinorRunes("v");
                                bot.browser.readScreen(Misc.Durations.SECOND);
                                bot.browser.clickOnSeg(badgeBtn);

                                BHBot.logger.info("Attempting GVG...");

                                if (bot.settings.gvgstrip.size() > 0) {
                                    // If we need to strip down for GVG, we need to close the GVG gump and open it again
                                    seg = MarvinSegment.fromCue(BHBot.cues.get("X"), Misc.Durations.SECOND * 2, bot.browser);
                                    bot.browser.clickOnSeg(seg);
                                    bot.browser.readScreen(2 * Misc.Durations.SECOND);
                                    stripDown(bot.settings.gvgstrip);
                                    seg = MarvinSegment.fromCue(BHBot.cues.get("GVG"), Misc.Durations.SECOND * 3, bot.browser);
                                    bot.browser.clickOnSeg(seg);
                                }

                                // select cost if needed:
                                bot.browser.readScreen(2 * Misc.Durations.SECOND); // wait for the popup to stabilize a bit
                                int cost = detectCost();
                                if (cost == 0) { // error!
                                    BHBot.logger.error("Due to an error#1 in cost detection, GVG will be skipped.");
                                    bot.browser.closePopupSecurely(BHBot.cues.get("GVGWindow"), BHBot.cues.get("X"));
                                    continue;
                                }
                                if (cost != bot.settings.costGVG) {
                                    BHBot.logger.info("Detected GVG cost: " + cost + ", settings cost is " + bot.settings.costGVG + ". Changing..");
                                    boolean result = selectCost(cost, bot.settings.costGVG);
                                    if (!result) { // error!
                                        // see if drop down menu is still open and close it:
                                        bot.browser.readScreen(Misc.Durations.SECOND);
                                        tryClosingWindow(BHBot.cues.get("CostDropDown"));
                                        bot.browser.readScreen(5 * Misc.Durations.SECOND);
                                        seg = MarvinSegment.fromCue(BHBot.cues.get("GVGWindow"), 15 * Misc.Durations.SECOND, bot.browser);
                                        if (seg != null)
                                            bot.browser.closePopupSecurely(BHBot.cues.get("GVGWindow"), BHBot.cues.get("X"));
                                        BHBot.logger.error("Due to an error#2 in cost selection, GVG will be skipped.");
                                        dressUp(bot.settings.gvgstrip);
                                        continue;
                                    }
                                }


                                seg = MarvinSegment.fromCue(BHBot.cues.get("Play"), 5 * Misc.Durations.SECOND, bot.browser);
                                bot.browser.clickOnSeg(seg);
                                bot.browser.readScreen(2 * Misc.Durations.SECOND);

                                // Sometimes, before the reset, battles are disabled
                                Boolean disabledBattles = handleDisabledBattles();
                                if (disabledBattles == null) {
                                    restart();
                                    continue;
                                } else if (disabledBattles) {
                                    bot.browser.readScreen();
                                    bot.browser.closePopupSecurely(BHBot.cues.get("GVGWindow"), BHBot.cues.get("X"));
                                    continue;
                                }

                                //On initial GvG run you'll get a warning about not being able to leave guild, this will close that
                                if (handleGuildLeaveConfirm()) {
                                    restart();
                                    continue;
                                }

                                Bounds gvgOpponentBounds = opponentSelector(bot.settings.gvgOpponent);
                                String opponentName = (bot.settings.gvgOpponent == 1 ? "1st" : bot.settings.gvgOpponent == 2 ? "2nd" : bot.settings.gvgOpponent == 3 ? "3rd" : "4th");
                                BHBot.logger.info("Selecting " + opponentName + " opponent");
                                seg = MarvinSegment.fromCue(BHBot.cues.get("Fight"), 5 * Misc.Durations.SECOND, gvgOpponentBounds, bot.browser);
                                if (seg == null) {
                                    BHBot.logger.error("Imppossible to find the Fight button in the GvG screen, restarting!");
                                    restart();
                                    continue;
                                }
                                bot.browser.clickOnSeg(seg);
                                bot.browser.readScreen();
                                Misc.sleep(Misc.Durations.SECOND);

                                seg = MarvinSegment.fromCue(BHBot.cues.get("Accept"), 5 * Misc.Durations.SECOND, Bounds.fromWidthHeight(470, 445, 100, 40), bot.browser);
                                if (seg == null) {
                                    BHBot.logger.error("Imppossible to find the Accept button in the GvG screen, restarting!");
                                    restart();
                                    continue;
                                }
                                //bot.browser.clickOnSeg(seg);
                                bot.browser.closePopupSecurely(BHBot.cues.get("Accept"), BHBot.cues.get("Accept"));
                                Misc.sleep(Misc.Durations.SECOND);

                                if (handleTeamMalformedWarning()) {
                                    BHBot.logger.error("Team incomplete, doing emergency restart..");
                                    restart();
                                    continue;
                                } else {
                                    bot.setState(BHBot.State.GVG);
                                    BHBot.logger.info("GVG initiated!");
                                }
                            }
                            continue;
                        } // GvG
                        // check invasion:
                        else if (badgeEvent == BadgeEvent.Invasion) {
                            if ((!bot.scheduler.doInvasionImmediately && (badges <= bot.settings.minBadges)) || (badges < bot.settings.costInvasion)) {

                                //if we have 1 resource and need 5 we don't need to check every 10 minutes, this increases the timer so we start checking again when we are one under the check limit
                                int badgeDifference = bot.settings.costGVG - badges; //difference between needed and current resource
                                if (badgeDifference > 1) {
                                    int increase = (badgeDifference - 1) * 45;
                                    BADGES_CHECK_INTERVAL = increase * Misc.Durations.MINUTE; //add 45 minutes to the check interval for each ticket needed above 1
                                } else
                                    BADGES_CHECK_INTERVAL = 10 * Misc.Durations.MINUTE; //if we only need 1 check every 10 minutes

                                bot.browser.readScreen();
                                seg = MarvinSegment.fromCue(BHBot.cues.get("X"), Misc.Durations.SECOND, bot.browser);
                                bot.browser.clickOnSeg(seg);
                                Misc.sleep(Misc.Durations.SECOND);
                                continue;
                            } else {
                                // do the invasion!

                                if (bot.scheduler.doInvasionImmediately)
                                    bot.scheduler.doInvasionImmediately = false; // reset it

                                //configure activity runes
                                handleMinorRunes("i");
                                bot.browser.readScreen(Misc.Durations.SECOND);
                                bot.browser.clickOnSeg(badgeBtn);

                                BHBot.logger.info("Attempting invasion...");

                                // select cost if needed:
                                bot.browser.readScreen(2 * Misc.Durations.SECOND); // wait for the popup to stabilize a bit
                                int cost = detectCost();
                                if (cost == 0) { // error!
                                    BHBot.logger.error("Due to an error#1 in cost detection, invasion will be skipped.");
                                    bot.browser.closePopupSecurely(BHBot.cues.get("InvasionWindow"), BHBot.cues.get("X"));
                                    continue;
                                }
                                if (cost != bot.settings.costInvasion) {
                                    BHBot.logger.info("Detected invasion cost: " + cost + ", settings cost is " + bot.settings.costInvasion + ". Changing..");
                                    boolean result = selectCost(cost, bot.settings.costInvasion);
                                    if (!result) { // error!
                                        // see if drop down menu is still open and close it:
                                        bot.browser.readScreen(Misc.Durations.SECOND);
                                        tryClosingWindow(BHBot.cues.get("CostDropDown"));
                                        bot.browser.readScreen(5 * Misc.Durations.SECOND);
                                        seg = MarvinSegment.fromCue(BHBot.cues.get("InvasionWindow"), 15 * Misc.Durations.SECOND, bot.browser);
                                        if (seg != null)
                                            bot.browser.closePopupSecurely(BHBot.cues.get("InvasionWindow"), BHBot.cues.get("X"));
                                        BHBot.logger.error("Due to an error#2 in cost selection, invasion will be skipped.");
                                        continue;
                                    }
                                }

                                seg = MarvinSegment.fromCue(BHBot.cues.get("Play"), 5 * Misc.Durations.SECOND, Bounds.fromWidthHeight(505, 255, 90, 45), bot.browser);
                                if (seg == null) {
                                    BHBot.logger.error("Unable to find the Play button in the Invasion screen, restarting!");
                                    restart();
                                    continue;
                                }
                                bot.browser.clickOnSeg(seg);

                                seg = MarvinSegment.fromCue(BHBot.cues.get("Accept"), 10 * Misc.Durations.SECOND, Bounds.fromWidthHeight(470, 450, 100, 30), bot.browser);
                                if (seg == null) {
                                    BHBot.logger.error("Unable to find the Accept button in the Invasion screen, restarting!");
                                    restart();
                                    continue;
                                }
                                //bot.browser.clickOnSeg(seg);
                                bot.browser.closePopupSecurely(BHBot.cues.get("Accept"), BHBot.cues.get("Accept"));
                                Misc.sleep(2 * Misc.Durations.SECOND);

                                if (handleTeamMalformedWarning()) {
                                    BHBot.logger.error("Team incomplete, doing emergency restart..");
                                    restart();
                                    continue;
                                } else {
                                    bot.setState(BHBot.State.Invasion);
                                    BHBot.logger.info("Invasion initiated!");
                                }
                            }
                            continue;
                        } // invasion

                        // check Expedition
                        else if (badgeEvent == BadgeEvent.Expedition) {

                            if ((!bot.scheduler.doExpeditionImmediately && (badges <= bot.settings.minBadges)) || (badges < bot.settings.costExpedition)) {

                                //if we have 1 resource and need 5 we don't need to check every 10 minutes, this increases the timer so we start checking again when we are one under the check limit
                                int badgeDifference = bot.settings.costGVG - badges; //difference between needed and current resource
                                if (badgeDifference > 1) {
                                    int increase = (badgeDifference - 1) * 45;
                                    BADGES_CHECK_INTERVAL = increase * Misc.Durations.MINUTE; //add 45 minutes to the check interval for each ticket needed above 1
                                } else
                                    BADGES_CHECK_INTERVAL = 10 * Misc.Durations.MINUTE; //if we only need 1 check every 10 minutes

                                seg = MarvinSegment.fromCue(BHBot.cues.get("X"), bot.browser);
                                bot.browser.clickOnSeg(seg);
                                Misc.sleep(2 * Misc.Durations.SECOND);
                                continue;
                            } else {
                                // do the expedition!

                                if (bot.scheduler.doExpeditionImmediately)
                                    bot.scheduler.doExpeditionImmediately = false; // reset it

                                if (bot.settings.costExpedition > badges) {
                                    BHBot.logger.info("Target cost " + bot.settings.costExpedition + " is higher than available badges " + badges + ". Expedition will be skipped.");
                                    seg = MarvinSegment.fromCue(BHBot.cues.get("X"), bot.browser);
                                    bot.browser.clickOnSeg(seg);
                                    Misc.sleep(2 * Misc.Durations.SECOND);
                                    continue;
                                }

                                //if we need to configure runes/settings we close the window first
                                if (bot.settings.autoShrine.contains("e") || bot.settings.autoRune.containsKey("e") || bot.settings.autoBossRune.containsKey("e")) {
                                    bot.browser.readScreen();
                                    seg = MarvinSegment.fromCue(BHBot.cues.get("X"), Misc.Durations.SECOND, bot.browser);
                                    bot.browser.clickOnSeg(seg);
                                    bot.browser.readScreen(Misc.Durations.SECOND);
                                }

                                //autoshrine
                                if (bot.settings.autoShrine.contains("e")) {
                                    BHBot.logger.info("Configuring autoShrine for Expedition");
                                    if (!shrineManager.updateShrineSettings(true, true)) {
                                        BHBot.logger.error("Impossible to configure autoShrine for Expedition!");
                                    }
                                }

                                //autoBossRune
                                if (bot.settings.autoBossRune.containsKey("e") && !bot.settings.autoShrine.contains("e")) { //if autoshrine disabled but autobossrune enabled
                                    BHBot.logger.info("Configuring autoBossRune for Expedition");
                                    if (!shrineManager.updateShrineSettings(true, false)) {
                                        BHBot.logger.error("Impossible to configure autoBossRune for Expedition!");
                                    }
                                }

                                //activity runes
                                handleMinorRunes("e");

                                bot.browser.readScreen(Misc.Durations.SECOND);
                                bot.browser.clickOnSeg(badgeBtn);
                                bot.browser.readScreen(Misc.Durations.SECOND * 2);

                                BHBot.logger.info("Attempting expedition...");

                                bot.browser.readScreen(Misc.Durations.SECOND * 2);
                                int cost = detectCost();
                                if (cost == 0) { // error!
                                    BHBot.logger.error("Due to an error#1 in cost detection, Expedition cost will be skipped.");
                                    bot.browser.closePopupSecurely(BHBot.cues.get("ExpeditionWindow"), BHBot.cues.get("X"));
                                    continue;
                                }

                                if (cost != bot.settings.costExpedition) {
                                    BHBot.logger.info("Detected Expedition cost: " + cost + ", settings cost is " + bot.settings.costExpedition + ". Changing..");
                                    boolean result = selectCost(cost, bot.settings.costExpedition);
                                    if (!result) { // error!
                                        // see if drop down menu is still open and close it:
                                        bot.browser.readScreen(Misc.Durations.SECOND);
                                        tryClosingWindow(BHBot.cues.get("CostDropDown"));
                                        bot.browser.readScreen(5 * Misc.Durations.SECOND);
                                        seg = MarvinSegment.fromCue(BHBot.cues.get("X"), bot.browser);
                                        bot.browser.clickOnSeg(seg);
                                        Misc.sleep(2 * Misc.Durations.SECOND);
                                        BHBot.logger.error("Due to an error in cost selection, Expedition will be skipped.");
                                        continue;
                                    }
                                    bot.browser.readScreen(Misc.Durations.SECOND * 2);
                                }

                                seg = MarvinSegment.fromCue(BHBot.cues.get("Play"), 2 * Misc.Durations.SECOND, bot.browser);
                                bot.browser.clickOnSeg(seg);
                                bot.browser.readScreen(2 * Misc.Durations.SECOND);

                                //Select Expedition and write portal to a variable
                                String randomExpedition = bot.settings.expeditions.next();
                                if (randomExpedition == null) {
                                    bot.settings.activitiesEnabled.remove("e");
                                    BHBot.logger.error("It was impossible to randomly choose an expedition. Expeditions are disabled.");
                                    bot.notificationManager.sendErrorNotification("Expedition error", "It was impossible to randomly choose an expedition. Expeditions are disabled.");
                                    continue;
                                }

                                String[] expedition = randomExpedition.split(" ");
                                String targetPortal = expedition[0];
                                int targetDifficulty = Integer.parseInt(expedition[1]);

                                // if exped difficulty isn't a multiple of 5 we reduce it
                                int difficultyModule = targetDifficulty % 5;
                                if (difficultyModule != 0) {
                                    BHBot.logger.warn(targetDifficulty + " is not a multiplier of 5! Rounding it to " + (targetDifficulty - difficultyModule) + "...");
                                    targetDifficulty -= difficultyModule;
                                }
                                // If difficulty is lesser that 5, we round it
                                if (targetDifficulty < 5) {
                                    BHBot.logger.warn("Expedition difficulty can not be smaller than 5, rounding it to 5.");
                                    targetDifficulty = 5;
                                }

                                bot.browser.readScreen();
                                int currentExpedition;
                                if (MarvinSegment.fromCue(BHBot.cues.get("Expedition1"), bot.browser) != null) {
                                    currentExpedition = 1;
                                } else if (MarvinSegment.fromCue(BHBot.cues.get("Expedition2"), bot.browser) != null) {
                                    currentExpedition = 2;
                                } else if (MarvinSegment.fromCue(BHBot.cues.get("Expedition3"), bot.browser) != null) {
                                    currentExpedition = 3;
                                } else if (MarvinSegment.fromCue(BHBot.cues.get("Expedition4"), bot.browser) != null) {
                                    currentExpedition = 4;
                                } else if (MarvinSegment.fromCue("Expedition5", bot.browser) != null) {
                                    currentExpedition = 5;
                                } else {
                                    bot.settings.activitiesEnabled.remove("e");
                                    BHBot.logger.error("It was impossible to get the current expedition type!");
                                    bot.notificationManager.sendErrorNotification("Expedition error", "It was impossible to get the current expedition type. Expeditions are now disabled!");

                                    bot.browser.readScreen();
                                    seg = MarvinSegment.fromCue(BHBot.cues.get("X"), Misc.Durations.SECOND, bot.browser);
                                    if (seg != null) bot.browser.clickOnSeg(seg);
                                    bot.browser.readScreen(2 * Misc.Durations.SECOND);
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
                                    bot.settings.activitiesEnabled.remove("e");
                                    BHBot.logger.error("It was impossible to get portal position for " + portalName + ". Expeditions are now disabled!");
                                    bot.notificationManager.sendErrorNotification("Expedition error", "It was impossible to get portal position for " + portalName + ". Expeditions are now disabled!");

                                    bot.browser.readScreen();
                                    seg = MarvinSegment.fromCue(BHBot.cues.get("X"), Misc.Durations.SECOND, bot.browser);
                                    if (seg != null) bot.browser.clickOnSeg(seg);
                                    bot.browser.readScreen(2 * Misc.Durations.SECOND);
                                    continue;
                                }

                                bot.browser.clickInGame(p.x, p.y);

                                // select difficulty if needed:
                                int difficulty = detectDifficulty(BHBot.cues.get("DifficultyExpedition"));
                                if (difficulty == 0) { // error!
                                    BHBot.logger.warn("Due to an error in difficulty detection, Expedition will be skipped.");
                                    seg = MarvinSegment.fromCue(BHBot.cues.get("X"), bot.browser);
                                    while (seg != null) {
                                        bot.browser.clickOnSeg(seg);
                                        bot.browser.readScreen(2 * Misc.Durations.SECOND);
                                        seg = MarvinSegment.fromCue(BHBot.cues.get("X"), bot.browser);
                                    }
                                    continue;
                                }

                                if (difficulty != targetDifficulty) {
                                    BHBot.logger.info("Detected Expedition difficulty level: " + difficulty + ", settings level is " + targetDifficulty + ". Changing..");
                                    boolean result = selectDifficulty(difficulty, targetDifficulty, BHBot.cues.get("SelectDifficultyExpedition"), 5);
                                    if (!result) { // error!
                                        // see if drop down menu is still open and close it:
                                        bot.browser.readScreen(Misc.Durations.SECOND);
                                        tryClosingWindow(BHBot.cues.get("DifficultyDropDown"));
                                        bot.browser.readScreen(5 * Misc.Durations.SECOND);
                                        BHBot.logger.warn("Unable to change difficulty, usually because desired level is not unlocked. Running Expedition at " + difficulty + ".");
                                        bot.notificationManager.sendErrorNotification("Expedition Error", "Unable to change expedtion difficulty to : " + targetDifficulty + " Running: " + difficulty + " instead.");

                                        // We update the file with the old difficulty level
                                        String original = expeditionFailsafePortal + " " + targetDifficulty;
                                        String updated = expeditionFailsafePortal + " " + difficulty;
                                        settingsUpdate(original, updated);

                                    }
                                }

                                //click enter
                                seg = MarvinSegment.fromCue(BHBot.cues.get("Enter"), 2 * Misc.Durations.SECOND, bot.browser);
                                bot.browser.clickOnSeg(seg);

                                //click enter
                                seg = MarvinSegment.fromCue(BHBot.cues.get("Accept"), 3 * Misc.Durations.SECOND, bot.browser);
                                if (seg != null) {
                                    //bot.browser.clickOnSeg(seg);
                                    bot.browser.closePopupSecurely(BHBot.cues.get("Accept"), BHBot.cues.get("Accept"));
                                } else {
                                    BHBot.logger.error("No accept button for expedition team!");
                                    bot.saveGameScreen("expedtion-no-accept", "errors");
                                    restart();
                                }

                                if (handleTeamMalformedWarning()) {
                                    BHBot.logger.error("Team incomplete, doing emergency restart..");
                                    restart();
                                    continue;
                                } else {
                                    bot.setState(BHBot.State.Expedition);
                                    BHBot.logger.info(portalName + " portal initiated!");
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
                            seg = MarvinSegment.fromCue(BHBot.cues.get("X"), bot.browser);
                            bot.browser.clickOnSeg(seg);
                            Misc.sleep(2 * Misc.Durations.SECOND);
                            continue;
                        }
                    } // badges

                    // Check worldBoss:
                    if ("w".equals(currentActivity)) {
                        timeLastEnergyCheck = Misc.getTime();
                        int energy = getEnergy();
                        globalEnergy = energy;
                        BHBot.logger.readout("Energy: " + energy + "%, required: >" + bot.settings.minEnergyPercentage + "%");

                        if (energy == -1) { // error
                            if (bot.scheduler.doWorldBossImmediately)
                                bot.scheduler.doWorldBossImmediately = false; // reset it
                            bot.scheduler.restoreIdleTime();


                            continue;
                        }

                        if (!bot.scheduler.doWorldBossImmediately && (energy <= bot.settings.minEnergyPercentage)) {

                            //if we have 1 resource and need 5 we don't need to check every 10 minutes, this increases the timer so we start checking again when we are one under the check limit
                            int energyDifference = bot.settings.minEnergyPercentage - energy; //difference between needed and current resource
                            if (energyDifference > 1) {
                                int increase = (energyDifference - 1) * 4;
                                ENERGY_CHECK_INTERVAL = increase * Misc.Durations.MINUTE; //add 4 minutes to the check interval for each energy % needed above 1
                            } else
                                ENERGY_CHECK_INTERVAL = 10 * Misc.Durations.MINUTE; //if we only need 1 check every 10 minutes

                            Misc.sleep(Misc.Durations.SECOND);
                            continue;
                        } else {
                            // do the WorldBoss!
                            if (bot.scheduler.doWorldBossImmediately)
                                bot.scheduler.doWorldBossImmediately = false; // reset it

                            if (!checkWorldBossInput()) {
                                BHBot.logger.warn("Invalid world boss settings detected, World Boss will be skipped");
                                continue;
                            }

                            //configure activity runes
                            handleMinorRunes("w");

                            seg = MarvinSegment.fromCue(BHBot.cues.get("WorldBoss"), bot.browser);
                            if (seg != null) {
                                bot.browser.clickOnSeg(seg);
                            } else {
                                BHBot.logger.error("World Boss button not found");
                                continue;
                            }

                            bot.browser.readScreen();
                            detectCharacterDialogAndHandleIt(); //clear dialogue

                            WorldBoss wbType = WorldBoss.fromLetter(bot.settings.worldBossSettings.get(0));
                            if (wbType == null) {
                                BHBot.logger.error("Unkwon World Boss type: " + bot.settings.worldBossSettings.get(0) + ". Disabling World Boss");
                                bot.settings.activitiesEnabled.remove("w");
                                restart();
                                continue;
                            }

                            int worldBossDifficulty = Integer.parseInt(bot.settings.worldBossSettings.get(1));
                            int worldBossTier = Integer.parseInt(bot.settings.worldBossSettings.get(2));
                            int worldBossTimer = bot.settings.worldBossTimer;

                            //new settings loading
                            String worldBossDifficultyText = worldBossDifficulty == 1 ? "Normal" : worldBossDifficulty == 2 ? "Hard" : "Heroic";

                            if (!bot.settings.worldBossSolo) {
                                BHBot.logger.info("Attempting " + worldBossDifficultyText + " T" + worldBossTier + " " + wbType.getName() + ". Lobby timeout is " + worldBossTimer + "s.");
                            } else {
                                BHBot.logger.info("Attempting " + worldBossDifficultyText + " T" + worldBossTier + " " + wbType.getName() + " Solo");
                            }

                            bot.browser.readScreen();
                            seg = MarvinSegment.fromCue(BHBot.cues.get("BlueSummon"), Misc.Durations.SECOND, bot.browser);
                            if (seg != null) {
                                bot.browser.clickOnSeg(seg);
                            } else {
                                BHBot.logger.error("Impossible to find blue summon in world boss.");

                                bot.saveGameScreen("wb-no-blue-summon", "errors");
                                bot.notificationManager.sendErrorNotification("World Boss error", "Impossible to find blue summon.");

                                bot.browser.closePopupSecurely(BHBot.cues.get("WorldBossTitle"), BHBot.cues.get("X"));
                                continue;
                            }
                            bot.browser.readScreen(2 * Misc.Durations.SECOND); //wait for screen to stablise

                            //world boss type selection
                            if (!handleWorldBossSelection(wbType)) {
                                BHBot.logger.error("Impossible to change select the desired World Boss. Restarting...");
                                restart();
                                continue;
                            }

//							Misc.sleep(SECOND); //more stabilising if we changed world boss type
                            bot.browser.readScreen(Misc.Durations.SECOND);
                            seg = MarvinSegment.fromCue(BHBot.cues.get("LargeGreenSummon"), 2 * Misc.Durations.SECOND, bot.browser);
                            bot.browser.clickOnSeg(seg); //selected world boss

                            bot.browser.readScreen(Misc.Durations.SECOND);
                            seg = MarvinSegment.fromCue(BHBot.cues.get("Private"), Misc.Durations.SECOND, bot.browser);
                            if (!bot.settings.worldBossSolo) {
                                if (seg != null) {
                                    BHBot.logger.info("Unchecking private lobby");
                                    bot.browser.clickOnSeg(seg);
                                }
                            } else {
                                if (seg == null) {
                                    BHBot.logger.info("Enabling private lobby for solo World Boss");
                                    Misc.sleep(500);
                                    bot.browser.clickInGame(340, 350);
                                    bot.browser.readScreen(500);
                                }
                            }

                            //world boss tier selection

                            int currentTier = detectWorldBossTier();
                            Misc.sleep(500);
                            if (currentTier != worldBossTier) {
                                BHBot.logger.info("T" + currentTier + " detected, changing to T" + worldBossTier);
                                Misc.sleep(500);
                                if (!changeWorldBossTier(worldBossTier, wbType)) {
                                    restart();
                                    continue;
                                }
                            }

                            //world boss difficulty selection

                            int currentDifficulty = detectWorldBossDifficulty();
                            String currentDifficultyName = (currentDifficulty == 1 ? "Normal" : currentDifficulty == 2 ? "Hard" : "Heroic");
                            String settingsDifficultyName = (worldBossDifficulty == 1 ? "Normal" : worldBossDifficulty == 2 ? "Hard" : "Heroic");
                            if (currentDifficulty != worldBossDifficulty) {
                                BHBot.logger.info(currentDifficultyName + " detected, changing to " + settingsDifficultyName);
                                changeWorldBossDifficulty(worldBossDifficulty);
                            }

                            bot.browser.readScreen(Misc.Durations.SECOND);
                            seg = MarvinSegment.fromCue(BHBot.cues.get("SmallGreenSummon"), Misc.Durations.SECOND * 2, bot.browser);
                            bot.browser.clickOnSeg(seg); //accept current settings

                            boolean insufficientEnergy = handleNotEnoughEnergyPopup(Misc.Durations.SECOND * 3, BHBot.State.WorldBoss);
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
                            if (!bot.settings.worldBossSolo) {
                                Bounds inviteButton = inviteBounds(wbType.getLetter());
                                for (int i = 0; i < worldBossTimer; i++) {
                                    bot.browser.readScreen(Misc.Durations.SECOND);
                                    seg = MarvinSegment.fromCue(BHBot.cues.get("Invite"), 0, inviteButton, bot.browser);
                                    if (seg != null) { //while the relevant invite button exists
                                        if (i != 0 && (i % 15) == 0) { //every 15 seconds
                                            int timeLeft = worldBossTimer - i;
                                            BHBot.logger.info("Waiting for full team. Time out in " + timeLeft + " seconds.");
                                        }
                                        if (i == (worldBossTimer - 1)) { //out of time
                                            if (bot.settings.dungeonOnTimeout) { //setting to run a dungeon if we cant fill a lobby
                                                BHBot.logger.info("Lobby timed out, running dungeon instead");
                                                closeWorldBoss();
                                                Misc.sleep(4 * Misc.Durations.SECOND); //make sure we're stable on the main screen
                                                bot.scheduler.doDungeonImmediately = true;
                                            } else {
                                                BHBot.logger.info("Lobby timed out, returning to main screen.");
                                                // we say we checked (interval - 1) minutes ago, so we check again in a minute
                                                timeLastEnergyCheck = Misc.getTime() - ((ENERGY_CHECK_INTERVAL) - Misc.Durations.MINUTE);
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
                                            seg = MarvinSegment.fromCue(BHBot.cues.get("Unready"), 2 * Misc.Durations.SECOND, bot.browser); //this checks all 4 ready statuses
                                            bot.browser.readScreen();
                                            if (seg == null) {// no red X's found
                                                break;
                                            } else { //red X's found
                                                //BHBot.logger.info(Integer.toString(j));
                                                j++;
                                                Misc.sleep(500); //check every 500ms
                                            }
                                        }

                                        if (j >= 20) {
                                            BHBot.logger.error("Ready check not passed after 10 seconds, restarting");
                                            restart();
                                        }

                                        Misc.sleep(500);
                                        bot.browser.readScreen();
                                        MarvinSegment segStart = MarvinSegment.fromCue(BHBot.cues.get("Start"), 5 * Misc.Durations.SECOND, bot.browser);
                                        if (segStart != null) {
                                            bot.browser.clickOnSeg(segStart); //start World Boss
                                            bot.browser.readScreen();
                                            seg = MarvinSegment.fromCue(BHBot.cues.get("TeamNotFull"), 2 * Misc.Durations.SECOND, bot.browser); //check if we have the team not full screen an clear it
                                            if (seg != null) {
                                                Misc.sleep(2 * Misc.Durations.SECOND); //wait for animation to finish
                                                bot.browser.clickInGame(330, 360); //yesgreen cue has issues so we use XY to click on Yes
                                            }
                                            BHBot.logger.info(worldBossDifficultyText + " T" + worldBossTier + " " + wbType.getName() + " started!");
                                            bot.setState(BHBot.State.WorldBoss);
                                        } else { //generic error / unknown action restart
                                            BHBot.logger.error("Something went wrong while attempting to start the World Boss, restarting");
                                            bot.saveGameScreen("wb-no-start-button", "errors");
                                            restart();
                                        }

                                    }
                                }
                            } else {
                                bot.browser.readScreen();
                                MarvinSegment segStart = MarvinSegment.fromCue(BHBot.cues.get("Start"), 2 * Misc.Durations.SECOND, bot.browser);
                                if (segStart != null) {
                                    bot.browser.clickOnSeg(segStart); //start World Boss
                                    Misc.sleep(2 * Misc.Durations.SECOND); //wait for dropdown animation to finish
                                    seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), 2 * Misc.Durations.SECOND, bot.browser); //clear empty team prompt
                                    //click anyway this cue has issues
                                    if (seg == null) {
                                        Misc.sleep(500);
                                    } else {
                                        bot.browser.clickOnSeg(seg);
                                    }
                                    bot.browser.clickInGame(330, 360); //yesgreen cue has issues so we use pos to click on Yes as a backup
                                    BHBot.logger.info(worldBossDifficultyText + " T" + worldBossTier + " " + wbType.getName() + " Solo started!");
                                    bot.setState(BHBot.State.WorldBoss);
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

                        if (bot.scheduler.collectBountiesImmediately) {
                            bot.scheduler.collectBountiesImmediately = false; //disable collectImmediately again if its been activated
                        }
                        BHBot.logger.info("Checking for completed bounties");

                        bot.browser.clickInGame(130, 440);

                        seg = MarvinSegment.fromCue(BHBot.cues.get("Bounties"), Misc.Durations.SECOND * 5, bot.browser);
                        if (seg != null) {
                            bot.browser.readScreen();
                            seg = MarvinSegment.fromCue(BHBot.cues.get("Loot"), Misc.Durations.SECOND * 5, new Bounds(505, 245, 585, 275), bot.browser);
                            while (seg != null) {
                                bot.browser.clickOnSeg(seg);
                                seg = MarvinSegment.fromCue(BHBot.cues.get("WeeklyRewards"), Misc.Durations.SECOND * 5, new Bounds(190, 100, 615, 400), bot.browser);
                                if (seg != null) {
                                    seg = MarvinSegment.fromCue(BHBot.cues.get("X"), 5 * Misc.Durations.SECOND, bot.browser);
                                    if (seg != null) {
                                        if ((bot.settings.screenshots.contains("b"))) {
                                            bot.saveGameScreen("bounty-loot", "rewards");
                                        }
                                        bot.browser.clickOnSeg(seg);
                                        BHBot.logger.info("Collected bounties");
                                        Misc.sleep(Misc.Durations.SECOND * 2);
                                    } else {
                                        BHBot.logger.error("Error when collecting bounty items, restarting...");
                                        bot.saveGameScreen("bounties-error-collect", "errors");
                                        restart();
                                    }
                                } else {
                                    BHBot.logger.error("Error finding bounty item dialog, restarting...");
                                    bot.saveGameScreen("bounties-error-item", "errors");
                                    restart();
                                }

                                seg = MarvinSegment.fromCue(BHBot.cues.get("Loot"), Misc.Durations.SECOND * 5, new Bounds(505, 245, 585, 275), bot.browser);
                            }

                            seg = MarvinSegment.fromCue(BHBot.cues.get("X"), 5 * Misc.Durations.SECOND, bot.browser);
                            if (seg != null) {
                                bot.browser.clickOnSeg(seg);
                            } else {
                                BHBot.logger.error("Impossible to close the bounties dialog, restarting...");
                                bot.saveGameScreen("bounties-error-closing", "errors");
                                restart();
                            }
                        } else {
                            BHBot.logger.error("Impossible to detect the Bounties dialog, restarting...");
                            bot.saveGameScreen("bounties-error-dialog", "errors");
                            restart();
                        }
                        bot.browser.readScreen(Misc.Durations.SECOND * 2);
                        continue;
                    }

                    //fishing baits
                    if ("a".equals(currentActivity)) {
                        timeLastFishingBaitsCheck = Misc.getTime();

                        if (bot.scheduler.doFishingBaitsImmediately) {
                            bot.scheduler.doFishingBaitsImmediately = false; //disable collectImmediately again if its been activated
                        }

                        handleFishingBaits();
                        continue;
                    }

                    //fishing
                    if ("f".equals(currentActivity)) {
                        timeLastFishingCheck = Misc.getTime();

                        if (bot.scheduler.doFishingImmediately) {
                            bot.scheduler.doFishingImmediately = false; //disable collectImmediately again if its been activated
                        }

                        if ((Misc.getTime() - timeLastFishingBaitsCheck) > Misc.Durations.DAY) { //if we haven't collected bait today we need to do that first
                            handleFishingBaits();
                        }

                        boolean botPresent = new File("bh-fisher.jar").exists();
                        if (!botPresent) {
                            BHBot.logger.warn("bh-fisher.jar not found in root directory, fishing disabled.");
                            BHBot.logger.warn("For information on configuring fishing check the wiki page on github");
                            bot.settings.activitiesEnabled.remove("f");
                            return;
                        } else {
                            handleFishing();
                        }
                        continue;
                    }

                } // main screen processing
            } catch (Exception e) {
                if (bot.excManager.manageException(e)) continue;
                bot.scheduler.restoreIdleTime();
                continue;
            }

            // well, we got through all the checks. Means that nothing much has happened. So lets sleep for a second in order to not make processing too heavy...
            bot.excManager.numConsecutiveException = 0; // reset exception counter
            bot.scheduler.restoreIdleTime(); // revert changes to idle time
            if (bot.finished) break; // skip sleeping if finished flag has been set!
            Misc.sleep(Misc.Durations.SECOND);
        } // main while loop

        BHBot.logger.info("Dungeon thread stopped.");
    }

    private Bounds inviteBounds(String wb) {
        Bounds inviteButton;
        switch (wb) {
            // 3rd Invite button for Nether, 3xt3rmin4tion & Brimstone
            case "m":
                inviteButton = new Bounds(330, 330, 460, 380); // 4th Invite button for Melvin
                break;
            case "o":
                inviteButton = new Bounds(336, 387, 452, 422); // 5th Invite button for Orlag
                break;
            case "n":
            case "3":
            case "b":
            default:
                inviteButton = new Bounds(334, 275, 456, 323); // on error return 3rd invite
                break;
        }
        return inviteButton;
    }

    private String activitySelector() {

        if (bot.scheduler.doRaidImmediately) {
            return "r";
        } else if (bot.scheduler.doDungeonImmediately) {
            return "d";
        } else if (bot.scheduler.doWorldBossImmediately) {
            return "w";
        } else if (bot.scheduler.doTrialsImmediately) {
            return "t";
        } else if (bot.scheduler.doGauntletImmediately) {
            return "g";
        } else if (bot.scheduler.doPVPImmediately) {
            return "p";
        } else if (bot.scheduler.doInvasionImmediately) {
            return "i";
        } else if (bot.scheduler.doGVGImmediately) {
            return "v";
        } else if (bot.scheduler.doExpeditionImmediately) {
            return "e";
        } else if (bot.scheduler.collectBountiesImmediately) {
            return "b";
        } else if (bot.scheduler.doFishingBaitsImmediately) {
            return "a";
        } else if (bot.scheduler.doFishingImmediately) {
            return "f";
        }

        //return null if no matches
        if (!bot.settings.activitiesEnabled.isEmpty()) {

            String activity;

            if (!bot.settings.activitiesRoundRobin) {
                activitysIterator = null;
                activitysIterator = bot.settings.activitiesEnabled.iterator(); //reset the iterator
            }

            //loop through in defined order, if we match activity and timer we select the activity
            while (activitysIterator.hasNext()) {

                try {
                    activity = activitysIterator.next(); //set iterator to string for .equals()
                } catch (ConcurrentModificationException e) {
                    activitysIterator = bot.settings.activitiesEnabled.iterator();
                    activity = activitysIterator.next();
                }

                if (activity.equals("r") && ((Misc.getTime() - timeLastShardsCheck) > (long) (15 * Misc.Durations.MINUTE))) {
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
                } else if ("b".equals(activity) && ((Misc.getTime() - timeLastBountyCheck) > (long) Misc.Durations.HOUR)) {
                    return "b";
                } else if ("a".equals(activity) && ((Misc.getTime() - timeLastFishingBaitsCheck) > (long) Misc.Durations.DAY)) {
                    return "a";
                } else if ("f".equals(activity) && ((Misc.getTime() - timeLastFishingCheck) > (long) Misc.Durations.DAY)) {
                    return "f";
                }
            }

            // If we reach this point activityIterator.hasNext() is false
            if (bot.settings.activitiesRoundRobin) {
                activitysIterator = bot.settings.activitiesEnabled.iterator();
            }

        }
        return null;
    }

    private boolean openRunesMenu() {
        // Open character menu
        bot.browser.clickInGame(55, 465);

        MarvinSegment seg = MarvinSegment.fromCue(BHBot.cues.get("Runes"), 15 * Misc.Durations.SECOND, bot.browser);
        if (seg == null) {
            BHBot.logger.warn("Error: unable to detect runes button! Skipping...");
            return true;
        }

        bot.browser.clickOnSeg(seg);
        Misc.sleep(500); //sleep for window animation (15s below was crashing the bot, not sure why

        seg = MarvinSegment.fromCue(BHBot.cues.get("RunesLayout"), 15 * Misc.Durations.SECOND, bot.browser);
        if (seg == null) {
            BHBot.logger.warn("Error: unable to detect rune layout! Skipping...");
            seg = MarvinSegment.fromCue(BHBot.cues.get("X"), 5 * Misc.Durations.SECOND, bot.browser);
            if (seg != null) {
                bot.browser.clickOnSeg(seg);
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
        bot.browser.readScreen();
        for (MinorRune rune : MinorRune.values()) {
            Cue runeCue = rune.getRuneCue();

            // left rune
            MarvinSegment seg = MarvinSegment.fromCue(runeCue, 0, new Bounds(230, 245, 320, 330), bot.browser);
            if (seg != null)
                leftMinorRune = rune;

            // right rune
            seg = MarvinSegment.fromCue(runeCue, 0, new Bounds(480, 245, 565, 330), bot.browser);
            if (seg != null)
                rightMinorRune = rune;

        }

        if (exitRunesMenu) {
            Misc.sleep(500);
            bot.browser.closePopupSecurely(BHBot.cues.get("RunesLayout"), BHBot.cues.get("X"));
            Misc.sleep(500);
            bot.browser.closePopupSecurely(BHBot.cues.get("StripSelectorButton"), BHBot.cues.get("X"));
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
     * This will handle dialog that open up when you encounter a boss for the first time, for example, or open a raid window or trials window for the first time, etc.
     */
    private void detectCharacterDialogAndHandleIt() {
        MarvinSegment right;
        MarvinSegment left;
        int steps = 0;

        while (true) {
            bot.browser.readScreen();

            right = MarvinSegment.fromCue(BHBot.cues.get("DialogRight"), bot.browser);
            left = MarvinSegment.fromCue(BHBot.cues.get("DialogLeft"), bot.browser);

            //if we don't find either exit
            if (left == null && right == null) break;

            // if we find left or right click them
            if (left != null) bot.browser.clickOnSeg(left);
            if (right != null) bot.browser.clickOnSeg(right);

            steps++;
            Misc.sleep(Misc.Durations.SECOND);
        }

        if (steps > 0)
            BHBot.logger.info("Character dialog dismissed.");
    }

    /**
     * Returns amount of energy in percent (0-100). Returns -1 in case it cannot read energy for some reason.
     */
    private int getEnergy() {
        MarvinSegment seg;

        seg = MarvinSegment.fromCue(BHBot.cues.get("EnergyBar"), bot.browser);

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
            Color col = new Color(bot.browser.getImg().getRGB(left + i, top));

            if (!col.equals(full))
                break;
        }

        return Math.round(value * (100 / 77.0f)); // scale it to interval [0..100]
    }

    /**
     * Returns number of tickets left (for PvP) in interval [0..10]. Returns -1 in case it cannot read number of tickets for some reason.
     */
    private int getTickets() {
        MarvinSegment seg;

        seg = MarvinSegment.fromCue(BHBot.cues.get("TicketBar"), bot.browser);

        if (seg == null) // this should probably not happen
            return -1;

        int left = seg.x2 + 1;
        int top = seg.y1 + 6;

        final Color full = new Color(226, 42, 81);

        int value = 0;
        int maxTickets = bot.settings.maxTickets;

        // ticket bar is 80 pixels long (however last two pixels will have "medium" color and not full color (it's so due to shading))
        for (int i = 0; i < 78; i++) {
            value = i;
            Color col = new Color(bot.browser.getImg().getRGB(left + i, top));

            if (!col.equals(full))
                break;
        }

        value = value + 2; //add the last 2 pixels to get an accurate count
        return Math.round(value * (maxTickets / 77.0f)); // scale it to interval [0..10]
    }

    /**
     * Returns number of shards that we have. Works only if raid popup is open. Returns -1 in case it cannot read number of shards for some reason.
     */
    private int getShards() {
        MarvinSegment seg;

        seg = MarvinSegment.fromCue(BHBot.cues.get("RaidPopup"), bot.browser);

        if (seg == null) // this should probably not happen
            return -1;

        int left = seg.x2 + 1;
        int top = seg.y1 + 9;

        final Color full = new Color(199, 79, 175);

        int value = 0;
        int maxShards = bot.settings.maxShards;

        for (int i = 0; i < 76; i++) {
            value = i;
            Color col = new Color(bot.browser.getImg().getRGB(left + i, top));

            if (!col.equals(full))
                break;
        }

        value = value + 2; //add the last 2 pixels to get an accurate count
        return Math.round(value * (maxShards / 75.0f)); // round to nearest whole number
    }

    /**
     * Returns number of tokens we have. Works only if trials/gauntlet window is open. Returns -1 in case it cannot read number of tokens for some reason.
     */
    private int getTokens() {
        MarvinSegment seg;

        seg = MarvinSegment.fromCue(BHBot.cues.get("TokenBar"), bot.browser);

        if (seg == null) // this should probably not happen
            return -1;

        int left = seg.x2;
        int top = seg.y1 + 6;

        final Color full = new Color(17, 208, 226);

        int value = 0;
        int maxTokens = bot.settings.maxTokens;

        // tokens bar is 78 pixels wide (however last two pixels will have "medium" color and not full color (it's so due to shading))
        for (int i = 0; i < 76; i++) {
            value = i + 1;
            Color col = new Color(bot.browser.getImg().getRGB(left + i, top));

            if (!col.equals(full))
                break;
        }

        return Math.round(value * (maxTokens / 76.0f)); // scale it to interval [0..10]
    }

    /**
     * Returns number of badges we have. Works only if GVG window is open. Returns -1 in case it cannot read number of badges for some reason.
     */
    private int getBadges() {
        MarvinSegment seg;

        seg = MarvinSegment.fromCue(BHBot.cues.get("BadgeBar"), bot.browser);

        if (seg == null) // this should probably not happen
            return -1;

        int left = seg.x2 + 1;
        int top = seg.y1 + 6;

        final Color full = new Color(17, 208, 226);

        int value = 0;
        int maxBadges = bot.settings.maxBadges;

        // badges bar is 78 pixels wide (however last two pixels will have "medium" color and not full color (it's so due to shading))
        for (int i = 0; i < 76; i++) {
            value = i;
            Color col = new Color(bot.browser.getImg().getRGB(left + i, top));

            if (!col.equals(full))
                break;
        }

        value = value + 2; //add the last 2 pixels to get an accurate count
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
        bot.browser.readScreen();

        if (!startTimeCheck) {
            activityStartTime = TimeUnit.MILLISECONDS.toSeconds(Misc.getTime());
            BHBot.logger.debug("Start time: " + activityStartTime);
            outOfEncounterTimestamp = TimeUnit.MILLISECONDS.toSeconds(Misc.getTime());
            inEncounterTimestamp = TimeUnit.MILLISECONDS.toSeconds(Misc.getTime());
            startTimeCheck = true;
            encounterStatus = false; //true is in encounter, false is out of encounter
        }

        long activityDuration = (TimeUnit.MILLISECONDS.toSeconds(Misc.getTime()) - activityStartTime);

        /*
         * Encounter detection code
         * We use guild button visibility to detect whether we are in combat
         */
        MarvinSegment guildButtonSeg = MarvinSegment.fromCue(BHBot.cues.get("GuildButton"), bot.browser);
        if (guildButtonSeg != null) {
            outOfEncounterTimestamp = TimeUnit.MILLISECONDS.toSeconds(Misc.getTime());
            if (encounterStatus) {
                BHBot.logger.trace("Updating idle time (Out of combat)");
                bot.scheduler.resetIdleTime(true);
                encounterStatus = false;
            }
        } else {
            inEncounterTimestamp = TimeUnit.MILLISECONDS.toSeconds(Misc.getTime());
            if (!encounterStatus) {
                BHBot.logger.trace("Updating idle time (In combat)");
                bot.scheduler.resetIdleTime(true);
                encounterStatus = true;
            }
        }

        /*
         *  handleLoot code
         *  It's enabled in these activities to try and catch real-time loot drops, as the loot window automatically closes
         */
        if (bot.getState() == BHBot.State.Raid || bot.getState() == BHBot.State.Dungeon || bot.getState() == BHBot.State.Expedition || bot.getState() == BHBot.State.Trials) {
            handleLoot();
        }

        /*
         * autoRune Code
         */
        if (bot.settings.autoBossRune.containsKey(bot.getState().getShortcut()) && !encounterStatus) {
            handleAutoBossRune();
        }

        /*
         * autoShrine Code
         */
        if (bot.settings.autoShrine.contains(bot.getState().getShortcut()) && !encounterStatus) {
            shrineManager.processAutoShrine((outOfEncounterTimestamp - inEncounterTimestamp));
        }

        /*
         * autoRevive code
         * This also handles re-enabling auto
         */
        seg = MarvinSegment.fromCue(BHBot.cues.get("AutoOff"), bot.browser);
        if (seg != null) {
            handleAutoOff();
        }

        /*
         * autoBribe/Persuasion code
         */
        if ((bot.getState() == BHBot.State.Raid || bot.getState() == BHBot.State.Dungeon || bot.getState() == BHBot.State.UnidentifiedDungeon) && (activityDuration % 5 == 0) && encounterStatus) {
            seg = MarvinSegment.fromCue(BHBot.cues.get("Persuade"), bot.browser);
            if (seg != null) {
                handleFamiliarEncounter();
            }
        }

        /*
         *  Skeleton key code
         *  encounterStatus is set to true as the window obscures the guild icon
         */
        if (activityDuration % 5 == 0 && encounterStatus) {
            seg = MarvinSegment.fromCue(BHBot.cues.get("SkeletonTreasure"), bot.browser);
            if (seg != null) {
                if (handleSkeletonKey()) {
                    restart();
                }
            }
        }

        /*
         *  1x Speed check
         *  We check once per activity, when we're in combat
         */
        if (activityDuration % 5 == 0 && encounterStatus) { //we check once per activity when we are in encounter
            MarvinSegment speedFull = MarvinSegment.fromCue("Speed_Full", bot.browser);
            MarvinSegment speedLabel = MarvinSegment.fromCue("Speed", bot.browser);
            if (speedLabel != null && speedFull == null) { //if we see speed label but not 3/3 speed
                BHBot.logger.warn("1x speed detected, fixing..");
                seg = MarvinSegment.fromCue("Speed", bot.browser);
                if (seg != null) {
                    bot.browser.clickOnSeg(seg);
                    return;
                }
            } else {
                BHBot.logger.debug("Speed settings checked.");
            }
        }

        /*
         *   Merchant offer check
         *   Not super common so we check every 5 seconds
         */
        if (activityDuration % 5 == 0 && encounterStatus) {
            seg = MarvinSegment.fromCue(BHBot.cues.get("Merchant"), bot.browser);
            if (seg != null) {
                seg = MarvinSegment.fromCue(BHBot.cues.get("Decline"), 5 * Misc.Durations.SECOND, bot.browser);
                if (seg != null) {
                    bot.browser.clickOnSeg(seg);
                } else BHBot.logger.error("Merchant 'decline' cue not found");

                bot.browser.readScreen(Misc.Durations.SECOND);
                seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), 5 * Misc.Durations.SECOND, bot.browser);
                if (seg != null) {
                    bot.browser.clickOnSeg(seg);
                } else BHBot.logger.error("Merchant 'yes' cue not found");
            }
        }

        /*
         *   Character dialogue check
         *   This is a one time event per account instance, so we don't need to check it very often
         *   encounterStatus is set to true as the dialogue obscures the guild icon
         */
        if (activityDuration % 10 == 0 && encounterStatus && (bot.getState() == BHBot.State.Dungeon || bot.getState() == BHBot.State.Raid)) {
            detectCharacterDialogAndHandleIt();
        }


        /*
         *  Check for the 'Cleared' dialogue and handle post-activity tasks
         */
        if (bot.getState() == BHBot.State.Raid || bot.getState() == BHBot.State.Dungeon || bot.getState() == BHBot.State.Expedition || bot.getState() == BHBot.State.Trials || bot.getState() == BHBot.State.UnidentifiedDungeon) {
            seg = MarvinSegment.fromCue(BHBot.cues.get("Cleared"), bot.browser);
            if (seg != null) {

                //Calculate activity stats
                counters.get(bot.getState()).increaseVictories();
                long activityRuntime = Misc.getTime() - activityStartTime * 1000; //get elapsed time in milliseconds
                String runtime = Misc.millisToHumanForm(activityRuntime);
                counters.get(bot.getState()).increaseVictoriesDuration(activityRuntime);
                String runtimeAvg = Misc.millisToHumanForm(counters.get(bot.getState()).getVictoryAverageDuration());
                //return stats
                BHBot.logger.info(bot.getState().getName() + " #" + counters.get(bot.getState()).getTotal() + " completed. Result: Victory");
                BHBot.logger.stats(bot.getState().getName() + " " + counters.get(bot.getState()).successRateDesc());
                BHBot.logger.stats("Victory run time: " + runtime + ". Average: " + runtimeAvg + ".");

                //handle SuccessThreshold
                handleSuccessThreshold(bot.getState());

                //close 'cleared' popup
                bot.browser.closePopupSecurely(BHBot.cues.get("Cleared"), BHBot.cues.get("YesGreen"));

                // close the activity window to return us to the main screen
                if (bot.getState() != BHBot.State.Expedition) {
                    bot.browser.readScreen(3 * Misc.Durations.SECOND); //wait for slide-in animation to finish
                    bot.browser.closePopupSecurely(BHBot.cues.get("X"), BHBot.cues.get("X"));
                }

                //For Expedition we need to close 3 windows (Exped/Portal/Team) to return to main screen
                if (bot.getState() == BHBot.State.Expedition) {
                    bot.browser.closePopupSecurely(BHBot.cues.get("Enter"), BHBot.cues.get("X"));
                    bot.browser.closePopupSecurely(BHBot.cues.get("PortalBorderLeaves"), BHBot.cues.get("X"));
                    bot.browser.closePopupSecurely(BHBot.cues.get("BadgeBar"), BHBot.cues.get("X"));
                }

                resetAppropriateTimers();
                reviveManager.reset();

                bot.setState(BHBot.State.Main); // reset state
                return;
            }
        }

        /*
         *  Check for the 'Victory' screen and handle post-activity tasks
         */
        if (bot.getState() == BHBot.State.WorldBoss || bot.getState() == BHBot.State.Gauntlet || bot.getState() == BHBot.State.Invasion || bot.getState() == BHBot.State.PVP || bot.getState() == BHBot.State.GVG) {
            if (bot.getState() == BHBot.State.Gauntlet || bot.getState() == BHBot.State.GVG) {
                seg = MarvinSegment.fromCue(BHBot.cues.get("VictorySmall"), bot.browser);
            } else {
                seg = MarvinSegment.fromCue(BHBot.cues.get("VictoryLarge"), bot.browser);
            }
            if (seg != null) {

                Bounds closeBounds = null;
                if (BHBot.State.Gauntlet.equals(bot.getState())) closeBounds = Bounds.fromWidthHeight(320, 420, 160, 65);
                // Sometime the victory pop-up is show without the close button, this check is there to ignore it
                seg = MarvinSegment.fromCue(BHBot.cues.get("CloseGreen"), 2 * Misc.Durations.SECOND, closeBounds, bot.browser);
                if (seg == null) {
                    BHBot.logger.debug("Unable to find close button for " + bot.getState().getName() + " victory screen. Ignoring it.");
                    handleLoot();
                    return;
                }

                //Calculate activity stats
                counters.get(bot.getState()).increaseVictories();
                long activityRuntime = Misc.getTime() - activityStartTime * 1000; //get elapsed time in milliseconds
                String runtime = Misc.millisToHumanForm(activityRuntime);
                counters.get(bot.getState()).increaseVictoriesDuration(activityRuntime);
                String runtimeAvg = Misc.millisToHumanForm(counters.get(bot.getState()).getVictoryAverageDuration());
                //return stats
                BHBot.logger.info(bot.getState().getName() + " #" + counters.get(bot.getState()).getTotal() + " completed. Result: Victory");
                BHBot.logger.stats(bot.getState().getName() + " " + counters.get(bot.getState()).successRateDesc());
                BHBot.logger.stats("Victory run time: " + runtime + ". Average: " + runtimeAvg + ".");

                //handle SuccessThreshold
                handleSuccessThreshold(bot.getState());

                //check for loot drops and send via Pushover/Screenshot
                handleLoot();

                // If we are here the close button should be there
                seg = MarvinSegment.fromCue(BHBot.cues.get("CloseGreen"), 2 * Misc.Durations.SECOND, closeBounds, bot.browser);
                if (seg != null) {
                    bot.browser.clickOnSeg(seg);
                } else {
                    BHBot.logger.error("Victory pop-up error while performing " + bot.getState().getName() + "! Restarting the bot.");
                    restart();
                    return;
                }

                // close the activity window to return us to the main screen
                bot.browser.readScreen(3 * Misc.Durations.SECOND); //wait for slide-in animation to finish
                bot.browser.closePopupSecurely(BHBot.cues.get("X"), BHBot.cues.get("X"));

                //last few post activity tasks
                resetAppropriateTimers();
                reviveManager.reset();
                if (bot.getState() == BHBot.State.GVG) dressUp(bot.settings.gvgstrip);
                if (bot.getState() == BHBot.State.PVP) dressUp(bot.settings.pvpstrip);

                //return to main state
                bot.setState(BHBot.State.Main); // reset state
                return;
            }
        }

        /*
         *  Check for the 'Defeat' dialogue and handle post-activity tasks
         *  Most activities have custom tasks on defeat
         */
        seg = MarvinSegment.fromCue(BHBot.cues.get("Defeat"), bot.browser);
        if (seg != null) {

            //Calculate activity stats
            counters.get(bot.getState()).increaseDefeats();
            long activityRuntime = Misc.getTime() - activityStartTime * 1000; //get elapsed time in milliseconds
            String runtime = Misc.millisToHumanForm(activityRuntime);
            counters.get(bot.getState()).increaseDefeatsDuration(activityRuntime);
            String runtimeAvg = Misc.millisToHumanForm(counters.get(bot.getState()).getDefeatAverageDuration());

            //return stats for non-invasion
            if (bot.getState() != BHBot.State.Invasion) {
                BHBot.logger.warn(bot.getState().getName() + " #" + counters.get(bot.getState()).getTotal() + " completed. Result: Defeat.");
                BHBot.logger.stats(bot.getState().getName() + " " + counters.get(bot.getState()).successRateDesc());
                BHBot.logger.stats("Defeat run time: " + runtime + ". Average: " + runtimeAvg + ".");
            } else {
                //return the stats for invasion (no victory possible so we skip the warning)
                bot.browser.readScreen();
                MarvinImage subm = new MarvinImage(bot.browser.getImg().getSubimage(375, 20, 55, 20));
                makeImageBlackWhite(subm, new Color(25, 25, 25), new Color(255, 255, 255), 64);
                BufferedImage subimagetestbw = subm.getBufferedImage();
                int num = readNumFromImg(subimagetestbw, "small", new HashSet<>());
                BHBot.logger.info(bot.getState().getName() + " #" + counters.get(bot.getState()).getTotal() + " completed. Level reached: " + num);
                BHBot.logger.stats("Run time: " + runtime + ". Average: " + runtimeAvg + ".");
            }

            //check for invasion loot drops and send via Pushover/Screenshot
            if (bot.getState() == BHBot.State.Invasion) {
                handleLoot();
            }

            //in Gauntlet/Invasion/GVG the close button is green, everywhere else its blue
            if (bot.getState() == BHBot.State.Gauntlet || bot.getState() == BHBot.State.Invasion || bot.getState() == BHBot.State.GVG) {
                seg = MarvinSegment.fromCue(BHBot.cues.get("CloseGreen"), 2 * Misc.Durations.SECOND, bot.browser);
            } else {
                if (bot.getState() == BHBot.State.PVP) {
                    seg = MarvinSegment.fromCue("Close", 3 * Misc.Durations.SECOND, Bounds.fromWidthHeight(355, 345, 85, 35), bot.browser);
                } else {
                    seg = MarvinSegment.fromCue(BHBot.cues.get("Close"), 2 * Misc.Durations.SECOND, bot.browser);
                }
            }

            if (seg != null) {
                bot.browser.clickOnSeg(seg);
            } else {
                BHBot.logger.warn("Problem: 'Defeat' popup detected but no 'Close' button detected in " + bot.getState().getName() + ".");
                if (bot.getState() == BHBot.State.PVP) dressUp(bot.settings.pvpstrip);
                if (bot.getState() == BHBot.State.GVG) dressUp(bot.settings.gvgstrip);
                return;
            }

            //Close the activity window to return us to the main screen
            if (bot.getState() != BHBot.State.Expedition) {
                bot.browser.readScreen(3 * Misc.Durations.SECOND); //wait for slide-in animation to finish
                bot.browser.closePopupSecurely(BHBot.cues.get("X"), BHBot.cues.get("X"));
            }

            //For Expedition we need to close 3 windows (Exped/Portal/Team) to return to main screen
            if (bot.getState() == BHBot.State.Expedition) {
                bot.browser.closePopupSecurely(BHBot.cues.get("Enter"), BHBot.cues.get("X"));
                bot.browser.closePopupSecurely(BHBot.cues.get("PortalBorderLeaves"), BHBot.cues.get("X"));
                bot.browser.closePopupSecurely(BHBot.cues.get("BadgeBar"), BHBot.cues.get("X"));

                //Handle difficultyFailsafe for Exped
                if (bot.settings.difficultyFailsafe.containsKey("e")) {
                    // The key is the difficulty decrease, the value is the minimum level
                    Map.Entry<Integer, Integer> expedDifficultyFailsafe = bot.settings.difficultyFailsafe.get("e");
                    int levelOffset = expedDifficultyFailsafe.getKey();
                    int minimumLevel = expedDifficultyFailsafe.getValue();

                    // We check that the level offset for expedition is a multiplier of 5
                    int levelOffsetModule = levelOffset % 5;
                    if (levelOffsetModule != 0) {
                        int newLevelOffset = levelOffset + (5 - levelOffsetModule);
                        BHBot.logger.warn("Level offset " + levelOffset + " is not multiplier of 5, rounding it to " + newLevelOffset);
                        bot.settings.difficultyFailsafe.put("e", Maps.immutableEntry(newLevelOffset, minimumLevel));
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

            if (bot.getState().equals(BHBot.State.Trials) && bot.settings.difficultyFailsafe.containsKey("t")) {
                // The key is the difficulty decrease, the value is the minimum level
                Map.Entry<Integer, Integer> trialDifficultyFailsafe = bot.settings.difficultyFailsafe.get("t");
                int levelOffset = trialDifficultyFailsafe.getKey();
                int minimumLevel = trialDifficultyFailsafe.getValue();

                // We calculate the new difficulty
                int newTrialDifficulty = bot.settings.difficultyTrials - levelOffset;

                // We check that the new difficulty is not lower than the minimum
                if (newTrialDifficulty < minimumLevel) newTrialDifficulty = minimumLevel;

                // If the new difficulty is different from the current one, we update the ini setting
                if (newTrialDifficulty != bot.settings.difficultyTrials) {
                    String original = "difficultyTrials " + bot.settings.difficultyTrials;
                    String updated = "difficultyTrials " + newTrialDifficulty;
                    settingsUpdate(original, updated);
                }
            }

            if (bot.getState().equals(BHBot.State.Gauntlet) && bot.settings.difficultyFailsafe.containsKey("g")) {
                // The key is the difficulty decrease, the value is the minimum level
                Map.Entry<Integer, Integer> gauntletDifficultyFailsafe = bot.settings.difficultyFailsafe.get("g");
                int levelOffset = gauntletDifficultyFailsafe.getKey();
                int minimumLevel = gauntletDifficultyFailsafe.getValue();

                // We calculate the new difficulty
                int newGauntletDifficulty = bot.settings.difficultyGauntlet - levelOffset;

                // We check that the new difficulty is not lower than the minimum
                if (newGauntletDifficulty < minimumLevel) newGauntletDifficulty = minimumLevel;

                // If the new difficulty is different from the current one, we update the ini setting
                if (newGauntletDifficulty != bot.settings.difficultyGauntlet) {
                    String original = "difficultyGauntlet " + bot.settings.difficultyGauntlet;
                    String updated = "difficultyGauntlet " + newGauntletDifficulty;
                    settingsUpdate(original, updated);
                }
            }

            resetAppropriateTimers();
            reviveManager.reset();

            // We make sure to dress up
            if (bot.getState() == BHBot.State.PVP && bot.settings.pvpstrip.size() > 0) dressUp(bot.settings.pvpstrip);
            if (bot.getState() == BHBot.State.GVG && bot.settings.gvgstrip.size() > 0) dressUp(bot.settings.gvgstrip);

            // We make sure to disable autoShrine when defeated
            if (bot.getState() == BHBot.State.Trials || bot.getState() == BHBot.State.Raid || bot.getState() == BHBot.State.Expedition) {

                bot.browser.readScreen(Misc.Durations.SECOND);
                if (!shrineManager.updateShrineSettings(false, false)) {
                    BHBot.logger.error("Impossible to disable autoShrine after defeat! Restarting..");
                    restart();
                }

                autoBossRuned = false;
                bot.browser.readScreen(Misc.Durations.SECOND * 2);
            }

            bot.setState(BHBot.State.Main); // reset state
            return;
        }

        // at the end of processDungeon, we revert idle time change (in order for idle detection to function properly):
        bot.scheduler.restoreIdleTime();
    }

    private void handleAutoBossRune() { //seperate function so we can run autoRune without autoShrine
        MarvinSegment guildButtonSeg;
        guildButtonSeg = MarvinSegment.fromCue(BHBot.cues.get("GuildButton"), bot.browser);

        if ((bot.getState() == BHBot.State.Raid && !bot.settings.autoShrine.contains("r") && bot.settings.autoBossRune.containsKey("r")) ||
                (bot.getState() == BHBot.State.Trials && !bot.settings.autoShrine.contains("t") && bot.settings.autoBossRune.containsKey("t")) ||
                (bot.getState() == BHBot.State.Expedition && !bot.settings.autoShrine.contains("e") && bot.settings.autoBossRune.containsKey("e")) ||
                (bot.getState() == BHBot.State.Dungeon && bot.settings.autoBossRune.containsKey("d")) ||
                bot.getState() == BHBot.State.UnidentifiedDungeon) {
            if (!autoBossRuned) {
                if ((((outOfEncounterTimestamp - inEncounterTimestamp) >= bot.settings.battleDelay) && guildButtonSeg != null)) {
                    BHBot.logger.autorune(bot.settings.battleDelay + "s since last encounter, changing runes for boss encounter");

                    handleMinorBossRunes();

                    if (!shrineManager.updateShrineSettings(false, false)) {
                        BHBot.logger.error("Impossible to disable Ignore Boss in handleAutoBossRune!");
                        BHBot.logger.warn("Resetting encounter timer to try again in 30 seconds.");
                        inEncounterTimestamp = Misc.getTime() / 1000;
                        return;
                    }

                    // We disable and re-enable the auto feature
                    while (MarvinSegment.fromCue(BHBot.cues.get("AutoOn"), 500, bot.browser) != null) {
                        bot.browser.clickInGame(780, 270); //auto off
                        bot.browser.readScreen(500);
                    }
                    while (MarvinSegment.fromCue(BHBot.cues.get("AutoOff"), 500, bot.browser) != null) {
                        bot.browser.clickInGame(780, 270); //auto on again
                        bot.browser.readScreen(500);
                    }
                    autoBossRuned = true;
                }
            }
        }
    }

    void handleMinorRunes(String activity) {
        List<String> desiredRunesAsStrs;
        String activityName = bot.getState().getNameFromShortcut(activity);
        if (bot.settings.autoRuneDefault.isEmpty()) {
            BHBot.logger.debug("autoRunesDefault not defined; aborting autoRunes");
            return;
        }

        if (!bot.settings.autoRune.containsKey(activity)) {
            BHBot.logger.debug("No autoRunes assigned for " + activityName + ", using defaults.");
            desiredRunesAsStrs = bot.settings.autoRuneDefault;
        } else {
            BHBot.logger.info("Configuring autoRunes for " + activityName);
            desiredRunesAsStrs = bot.settings.autoRune.get(activity);
        }

        List<MinorRuneEffect> desiredRunes = resolveDesiredRunes(desiredRunesAsStrs);
        if (noRunesNeedSwitching(desiredRunes)) {
            return;
        }

        // Back out of any raid/gauntlet/trial/GvG/etc pre-menu
        MarvinSegment seg = MarvinSegment.fromCue(BHBot.cues.get("X"), 2 * Misc.Durations.SECOND, bot.browser);
        if (seg != null) {
            bot.browser.clickOnSeg(seg);
            bot.browser.readScreen(Misc.Durations.SECOND);
        }

        if (!switchMinorRunes(desiredRunes))
            BHBot.logger.info("AutoRune failed!");

    }

    void handleMinorBossRunes() {
        if (bot.settings.autoRuneDefault.isEmpty()) {
            BHBot.logger.debug("autoRunesDefault not defined; aborting autoBossRunes");
            return;
        }

        String activity = bot.getState().getShortcut();
        // Hack to work around unknown dungeons
        if (activity.equals("ud"))
            activity = "d";
        if (!bot.settings.autoBossRune.containsKey(activity)) {
            BHBot.logger.info("No autoBossRunes assigned for " + bot.getState().getName() + ", skipping.");
            return;
        }

        List<String> desiredRunesAsStrs = bot.settings.autoBossRune.get(activity);
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
            desiredRunesAsStrs = bot.settings.autoRuneDefault;
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
            Misc.sleep(500); //sleep for window animation to finish
            bot.browser.clickInGame(280, 290); // Click on left rune
            if (!switchSingleMinorRune(desiredLeftRune)) {
                BHBot.logger.error("Failed to switch left minor rune.");
                return false;
            }
        }

        if (desiredRightRune != rightMinorRune.getRuneEffect()) {
            BHBot.logger.debug("Switching right minor rune.");
            Misc.sleep(500); //sleep for window animation to finish
            bot.browser.clickInGame(520, 290); // Click on right rune
            if (!switchSingleMinorRune(desiredRightRune)) {
                BHBot.logger.error("Failed to switch right minor rune.");
                return false;
            }
        }

        Misc.sleep(Misc.Durations.SECOND); //sleep while we wait for window animation

        if (!detectEquippedMinorRunes(false, true)) {
            BHBot.logger.error("Unable to detect runes, post-equip.");
            return false;
        }

        Misc.sleep(2 * Misc.Durations.SECOND);
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
        bot.browser.readScreen(500); //sleep for window animation to finish

        MarvinSegment seg = MarvinSegment.fromCue(BHBot.cues.get("RunesSwitch"), 5 * Misc.Durations.SECOND, bot.browser);
        if (seg == null) {
            BHBot.logger.error("Failed to find rune switch button.");
            return false;
        }
        bot.browser.clickOnSeg(seg);

        bot.browser.readScreen(500); //sleep for window animation to finish

        seg = MarvinSegment.fromCue(BHBot.cues.get("RunesPicker"), 5 * Misc.Durations.SECOND, bot.browser);
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
            seg = MarvinSegment.fromCue(runeCue, bot.browser);
            if (seg == null) {
                BHBot.logger.debug("Unable to find " + getRuneName(thisRune.getRuneCueName()) + " in rune picker.");
                continue;
            }
            BHBot.logger.autorune("Switched to " + getRuneName(thisRune.getRuneCueName()));
            bot.browser.clickOnSeg(seg);
            Misc.sleep(Misc.Durations.SECOND);
            return true;
        }

        BHBot.logger.error("Unable to find rune of type " + desiredRune);
        bot.browser.closePopupSecurely(BHBot.cues.get("RunesPicker"), BHBot.cues.get("X"));
        Misc.sleep(Misc.Durations.SECOND);
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

        seg = MarvinSegment.fromCue(BHBot.cues.get("SkeletonNoKeys"), 2 * Misc.Durations.SECOND, bot.browser);
        if (seg != null) {
            BHBot.logger.warn("No skeleton keys, skipping..");
            seg = MarvinSegment.fromCue(BHBot.cues.get("Decline"), 5 * Misc.Durations.SECOND, bot.browser);
            bot.browser.clickOnSeg(seg);
            bot.browser.readScreen(Misc.Durations.SECOND);
            seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), 5 * Misc.Durations.SECOND, bot.browser);
            bot.browser.clickOnSeg(seg);
            return false;
        }

        if (bot.settings.openSkeleton == 0) {
            BHBot.logger.info("Skeleton treasure found, declining.");
            seg = MarvinSegment.fromCue(BHBot.cues.get("Decline"), 5 * Misc.Durations.SECOND, bot.browser);
            bot.browser.clickOnSeg(seg);
            bot.browser.readScreen(Misc.Durations.SECOND);
            seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), 5 * Misc.Durations.SECOND, bot.browser);
            bot.browser.clickOnSeg(seg);
            return false;

        } else if (bot.settings.openSkeleton == 1) {
            BHBot.logger.info("Skeleton treasure found, attemping to use key");
            seg = MarvinSegment.fromCue(BHBot.cues.get("Open"), 5 * Misc.Durations.SECOND, bot.browser);
            if (seg == null) {
                BHBot.logger.error("Open button not found, restarting");
                bot.saveGameScreen("skeleton-treasure-no-open");
                bot.notificationManager.sendErrorNotification("Treasure chest error", "Skeleton Chest gump without OPEN button");
                return true;
            }
            bot.browser.clickOnSeg(seg);
            bot.browser.readScreen(Misc.Durations.SECOND);
            seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), 5 * Misc.Durations.SECOND, bot.browser);
            if (seg == null) {
                BHBot.logger.error("Yes button not found, restarting");
                bot.saveGameScreen("skeleton-treasure-no-yes");
                bot.notificationManager.sendErrorNotification("Treasure chest error", "Skeleton Chest gump without YES button");
                return true;
            }
            bot.browser.clickOnSeg(seg);
            return false;

        } else if (bot.settings.openSkeleton == 2 && bot.getState() == BHBot.State.Raid) {
            BHBot.logger.info("Raid Skeleton treasure found, attemping to use key");
            seg = MarvinSegment.fromCue(BHBot.cues.get("Open"), 5 * Misc.Durations.SECOND, bot.browser);
            if (seg == null) {
                BHBot.logger.error("Open button not found, restarting");
                bot.saveGameScreen("skeleton-treasure-no-open");
                bot.notificationManager.sendErrorNotification("Treasure chest error", "Skeleton Chest gump without OPEN button");
                return true;
            }
            bot.browser.clickOnSeg(seg);
            bot.browser.readScreen(Misc.Durations.SECOND);
            seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), 5 * Misc.Durations.SECOND, bot.browser);
            if (seg == null) {
                BHBot.logger.error("Yes button not found, restarting");
                bot.saveGameScreen("skeleton-treasure-no-yes");
                bot.notificationManager.sendErrorNotification("Treasure chest error", "Skeleton Chest gump without YES button");
                return true;
            }
            bot.browser.clickOnSeg(seg);
            Misc.sleep(500);
            if ((bot.settings.screenshots.contains("s"))) {
                bot.saveGameScreen("skeleton-contents", "rewards");
            }
            return false;

        } else
            BHBot.logger.info("Skeleton treasure found, declining.");
        seg = MarvinSegment.fromCue(BHBot.cues.get("Decline"), 5 * Misc.Durations.SECOND, bot.browser);
        bot.browser.clickOnSeg(seg);
        bot.browser.readScreen(Misc.Durations.SECOND);
        seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), 5 * Misc.Durations.SECOND, bot.browser);
        bot.browser.clickOnSeg(seg);
        return false;
    }

    private void handleFamiliarEncounter() {
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

        PersuationType persuasion;
        BribeDetails bribeInfo = new BribeDetails();

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
        if (persuasion == PersuationType.BRIBE && noGemsToBribe) {
            persuasion = PersuationType.PERSUADE;
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
            seg = MarvinSegment.fromCue(BHBot.cues.get("DeclineRed"), bot.browser);
            if (seg != null) {
                bot.browser.clickOnSeg(seg); // seg = detectCue(cues.get("Persuade"))
                bot.browser.readScreen(Misc.Durations.SECOND * 2);
                seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), Misc.Durations.SECOND, bot.browser);
                bot.browser.clickOnSeg(seg);
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
            seg = MarvinSegment.fromCue(BHBot.cues.get("View"), Misc.Durations.SECOND * 3, bot.browser);
            if (seg != null) {
                bot.browser.clickOnSeg(seg);
                bot.browser.readScreen(Misc.Durations.SECOND * 2);
                return true;
            } else {
                return false;
            }
        };

        BooleanSupplier closeView = () -> {
            MarvinSegment seg;
            seg = MarvinSegment.fromCue(BHBot.cues.get("X"), 2 * Misc.Durations.SECOND, bot.browser);
            if (seg != null) {
                bot.browser.clickOnSeg(seg);
                bot.browser.readScreen(Misc.Durations.SECOND);
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

        bot.browser.readScreen(Misc.Durations.SECOND);
        for (String familiarDetails : bot.settings.familiars) {
            // familiar details from settings
            String[] details = familiarDetails.toLowerCase().split(" ");
            familiarName = details[0];
            toBribeCnt = Integer.parseInt(details[1]);

            // cue related stuff
            boolean isOldFormat = false;

            Cue familiarCue = BHBot.cues.getOrNull(familiarName);

            if (familiarCue == null) {
                familiarCue = BHBot.cues.getOrNull("old" + familiarName);
                if (familiarCue != null) isOldFormat = true;
            }

            if (familiarCue != null) {
                if (toBribeCnt > 0) {
                    if (isOldFormat) { // Old style familiar
                        if (!viewIsOpened) { // we try to open the view menu if closed
                            if (openView.getAsBoolean()) {
                                bot.browser.readScreen(Misc.Durations.SECOND * 2);
                                viewIsOpened = true;
                            } else {
                                BHBot.logger.error("Old format familiar with no view button");
                                restart();
                            }
                        }
                    } else { // New style familiar
                        if (viewIsOpened) { // we try to close the view menu if opened
                            if (closeView.getAsBoolean()) {
                                bot.browser.readScreen(Misc.Durations.SECOND);
                                viewIsOpened = false;
                            } else {
                                BHBot.logger.error("Old style familiar detected with no X button to close the view menu.");
                                restart();
                            }
                        }
                    }

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

        if (viewIsOpened) {
            if (!closeView.getAsBoolean()) {
                BHBot.logger.error("Impossible to close view menu at the end of familiar setting loop!");
                restart();
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
                noGemsToBribe = true;
                if (!bot.browser.closePopupSecurely(BHBot.cues.get("NotEnoughGems"), BHBot.cues.get("No"))) {
                    BHBot.logger.error("Impossible to close the Not Enough gems pop-up. Restarting...");
                    restart();
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

    private void handleAutoOff() {
        MarvinSegment seg;

        // Auto Revive is disabled, we re-enable Auto on Dungeon
        if ((bot.settings.autoRevive.size() == 0) || (bot.getState() != BHBot.State.Trials && bot.getState() != BHBot.State.Gauntlet
                && bot.getState() != BHBot.State.Raid && bot.getState() != BHBot.State.Expedition)) {
            BHBot.logger.debug("AutoRevive disabled, reenabling auto.. State = '" + bot.getState() + "'");
            seg = MarvinSegment.fromCue(BHBot.cues.get("AutoOff"), bot.browser);
            if (seg != null) bot.browser.clickOnSeg(seg);
            bot.scheduler.resetIdleTime(true);
            return;
        }

        // if everyone dies autoRevive attempts to revive people on the defeat screen, this should prevent that
        seg = MarvinSegment.fromCue(BHBot.cues.get("Defeat"), Misc.Durations.SECOND, bot.browser);
        if (seg != null) {
            BHBot.logger.autorevive("Defeat screen, skipping revive check");
            seg = MarvinSegment.fromCue(BHBot.cues.get("AutoOff"), Misc.Durations.SECOND, bot.browser);
            if (seg != null) bot.browser.clickOnSeg(seg);
            bot.browser.readScreen(Misc.Durations.SECOND);
            bot.scheduler.resetIdleTime(true);
            return;
        }

        seg = MarvinSegment.fromCue(BHBot.cues.get("VictoryLarge"), 500, bot.browser);
        if (seg != null) {
            BHBot.logger.autorevive("Victory popup, skipping revive check");
            seg = MarvinSegment.fromCue(BHBot.cues.get("AutoOff"), Misc.Durations.SECOND, bot.browser);
            if (seg != null) bot.browser.clickOnSeg(seg);

            seg = MarvinSegment.fromCue(BHBot.cues.get("CloseGreen"), 2 * Misc.Durations.SECOND, bot.browser); // after enabling auto again the bot would get stuck at the victory screen, this should close it
            if (seg != null)
                bot.browser.clickOnSeg(seg);
            else {
                BHBot.logger.warn("Problem: 'Victory' window has been detected, but no 'Close' button. Ignoring...");
                return;
            }
            bot.scheduler.resetIdleTime(true);
            return;
        }

        reviveManager.processAutoRevive();

        seg = MarvinSegment.fromCue(BHBot.cues.get("AutoOff"), Misc.Durations.SECOND, bot.browser);
        if (seg != null) bot.browser.clickOnSeg(seg);
        bot.scheduler.resetIdleTime(true);

        // after reviving we update encounter timestamp as it wasn't updating from processDungeon
        inEncounterTimestamp = Misc.getTime() / 1000;

    }

    private void closeWorldBoss() {
        MarvinSegment seg;

        seg = MarvinSegment.fromCue(BHBot.cues.get("X"), 3 * Misc.Durations.SECOND, Bounds.fromWidthHeight(700, 50, 55, 60), bot.browser);
        if (seg != null) {
            bot.browser.clickOnSeg(seg);
        } else {
            BHBot.logger.error("first x Error returning to main screen from World Boss, restarting");
        }

        seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), 3 * Misc.Durations.SECOND, Bounds.fromWidthHeight(295, 345, 60, 35), bot.browser);
        if (seg != null) {
            bot.browser.clickOnSeg(seg);
        } else {
            BHBot.logger.error("yesgreen Error returning to main screen from World Boss, restarting");
        }

        seg = MarvinSegment.fromCue(BHBot.cues.get("X"), 3 * Misc.Durations.SECOND, Bounds.fromWidthHeight(640, 75, 55, 55), bot.browser);
        if (seg != null) {
            bot.browser.clickOnSeg(seg);
        } else {
            BHBot.logger.error("second x Error returning to main screen from World Boss, restarting");
        }

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

            //find containing string and update with the output string from the function above
            if (inputStr.contains(string)) {
                inputStr = inputStr.replace(string, updatedString);
                BHBot.logger.info("Replaced '" + string + "' with '" + updatedString + "' in " + Settings.configurationFile);
            } else BHBot.logger.error("Error finding string: " + string);

            // write the string from memory over the existing file
            FileOutputStream fileOut = new FileOutputStream(Settings.configurationFile);
            fileOut.write(inputStr.getBytes());
            fileOut.close();

            bot.settings.load();  //reload the new settings file so the counter will be updated for the next bribe

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
        if (z < 1 || z > 12) return null;
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
                break;
            case 11:
                switch (d) {
                    case 1:
                        return new Point(345, 408);
                    case 2:
                        return new Point(205, 160);
                    case 3:
                        return new Point(670, 205);
                    case 4:
                        BHBot.logger.warn("Zone 11 only has 3 dungeons, falling back to z11d2");
                        return new Point(205, 160);
                }
            case 12:
                switch (d) {
                    case 1:
                        return new Point(567, 413);
                    case 2:
                        return new Point(460, 150);
                    case 3:
                        return new Point(560, 400);
                    case 4:
                        return new Point(405, 290);
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
            if (!bot.browser.isDoNotShareUrl()) {
                Color col = new Color(bot.browser.getImg().getRGB(portalCheck[i].x, portalCheck[i].y));
                portalEnabled[i] = col.equals(colorCheck[i]);
            } else {
                Color col = new Color(bot.browser.getImg().getRGB(portalCheck[i].x - 1, portalCheck[i].y - 3));
                portalEnabled[i] = col.equals(colorCheck[i]);
            }

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
        String worldBossType = bot.settings.worldBossSettings.get(0);
        int worldBossTier = Integer.parseInt(bot.settings.worldBossSettings.get(2));
        WorldBoss wb = WorldBoss.fromLetter(worldBossType);

        //check name
        if (wb == null) {
            BHBot.logger.error("Invalid world boss name, check settings file");
            return false;
        }

        //check tier
        if (worldBossTier < wb.minTier || worldBossTier > wb.maxTier) {
            BHBot.logger.error("Invalid world boss tier for " + wb.getName() + ", must be between " + wb.getMinTier() + " and " + wb.getMaxTier());
            return false;
        }

        //warn user if timer is over 5 minutes
        if (bot.settings.worldBossTimer > 300) {
            BHBot.logger.warn("Warning: Timer longer than 5 minutes");
            return false;
        }
        return true;
    }

    /**
     * Returns dungeon and difficulty level, e.g. 'z2d4 2'.
     */
    private String decideDungeonRandomly() {

        if ("3".equals(new SimpleDateFormat("u").format(new Date())) &&
                bot.settings.wednesdayDungeons.size() > 0) { // if its wednesday and wednesdayRaids is not empty
            return bot.settings.wednesdayDungeons.next();
        } else {
            return bot.settings.dungeons.next();
        }
    }

    /**
     * Returns raid type (1, 2 or 3) and difficulty level (1, 2 or 3, which correspond to normal, hard and heroic), e.g. '1 3'.
     */
    private String decideRaidRandomly() {
        if ("3".equals(new SimpleDateFormat("u").format(new Date())) &&
                bot.settings.wednesdayRaids.size() > 0) { // if its wednesday and wednesdayRaids is not empty
            return bot.settings.wednesdayRaids.next();
        } else {
            return bot.settings.raids.next();
        }
    }

    /**
     * Returns number of zone that is currently selected in the quest window (we need to be in the quest window for this to work).
     * Returns 0 in case zone could not be read (in case we are not in the quest window, for example).
     */
    private int readCurrentZone() {
        if (MarvinSegment.fromCue("Zone1", bot.browser) != null)
            return 1;
        else if (MarvinSegment.fromCue("Zone2", bot.browser) != null)
            return 2;
        else if (MarvinSegment.fromCue("Zone3", bot.browser) != null)
            return 3;
        else if (MarvinSegment.fromCue("Zone4", bot.browser) != null)
            return 4;
        else if (MarvinSegment.fromCue("Zone5", bot.browser) != null)
            return 5;
        else if (MarvinSegment.fromCue("Zone6", bot.browser) != null)
            return 6;
        else if (MarvinSegment.fromCue("Zone7", bot.browser) != null)
            return 7;
        else if (MarvinSegment.fromCue("Zone8", bot.browser) != null)
            return 8;
        else if (MarvinSegment.fromCue("Zone9", bot.browser) != null)
            return 9;
        else if (MarvinSegment.fromCue("Zone10", bot.browser) != null)
            return 10;
        else if (MarvinSegment.fromCue("Zone11", bot.browser) != null)
            return 11;
        else if (MarvinSegment.fromCue("Zone12", bot.browser) != null)
            return 12;
        else
            return 0;
    }

    void expeditionReadTest() {
        String expedition = bot.settings.expeditions.next();
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
        bot.browser.readScreen(Misc.Durations.SECOND);

        int wbUnlocked = 0;
        int desiredWB = desiredWorldBoss.getNumber();

        // we get the grey dots on the raid selection popup
        List<MarvinSegment> wbDotsList = FindSubimage.findSubimage(bot.browser.getImg(), BHBot.cues.get("cueRaidLevelEmpty").im, 1.0, true, false, 0, 0, 0, 0);
        // we update the number of unlocked raids
        wbUnlocked += wbDotsList.size();

        // A  temporary variable to save the position of the current selected raid
        int selectedWBX1;

        seg = MarvinSegment.fromCue(BHBot.cues.get("RaidLevel"), bot.browser);
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
            bot.browser.clickOnSeg(wbDotsList.get(desiredWB - 1));
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
        bot.browser.readScreen(Misc.Durations.SECOND);

        int raidUnlocked = 0;
        // we get the grey dots on the raid selection popup
        List<MarvinSegment> raidDotsList = FindSubimage.findSubimage(bot.browser.getImg(), BHBot.cues.get("cueRaidLevelEmpty").im, 1.0, true, false, 0, 0, 0, 0);
        // we update the number of unlocked raids
        raidUnlocked += raidDotsList.size();

        // Is only R1 unlocked?
        boolean onlyR1 = false;
        if (raidUnlocked == 0 && MarvinSegment.fromCue(BHBot.cues.get("Raid1Name"), bot.browser) != null) {
            raidUnlocked += 1;
            onlyR1 = true;
        }

        // A  temporary variable to save the position of the current selected raid
        int selectedRaidX1 = 0;

        // we look for the the currently selected raid, the green dot
        if (!onlyR1) {
            seg = MarvinSegment.fromCue(BHBot.cues.get("RaidLevel"), bot.browser);
            if (seg != null) {
                raidUnlocked += 1;
                selectedRaidX1 = seg.getX1();
                raidDotsList.add(seg);
            } else {
                BHBot.logger.error("Impossible to detect the currently selected green cue!");
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
            bot.browser.clickOnSeg(raidDotsList.get(desiredRaid - 1));
        }

        return true;
    }

    private void contributeFamiliarShoot(String shootName, FamiliarType familiarType) {

        HttpClient httpClient = HttpClients.custom().useSystemProperties().build();

        final HttpPost post = new HttpPost("https://script.google.com/macros/s/AKfycby-tCXZ6MHt_ZSUixCcNbYFjDuri6WvljomLgGy_m5lLZw1y5fZ/exec");

        // we generate a sub image with just the name of the familiar
        bot.browser.readScreen(Misc.Durations.SECOND);
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

        BufferedImage zoneImg = bot.browser.getImg().getSubimage(105, 60, 640, 105);

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
     * Handles popup that tells you that your team is not complete. Happens when some friend left you.
     * This method will attempt to click on "Auto" button to refill your team.
     * Note that this can happen in raid and GvG only, since in other games (PvP, Gauntlet/Trials) you can use only familiars.
     * In GvG, on the other hand, there is additional dialog possible (which is not possible in raid): "team not ordered" dialog.
     *
     * @return true in case emergency restart is needed.
     */
    private boolean handleTeamMalformedWarning() {

        // We look for the team text on top of the text pop-up
        MarvinSegment seg = MarvinSegment.fromCue(BHBot.cues.get("Team"), Misc.Durations.SECOND * 3, new Bounds(330, 135, 480, 180), bot.browser);
        if (seg == null) {
            return false;
        }

        if (MarvinSegment.fromCue(BHBot.cues.get("TeamNotFull"), Misc.Durations.SECOND, bot.browser) != null || MarvinSegment.fromCue(BHBot.cues.get("TeamNotOrdered"), Misc.Durations.SECOND, bot.browser) != null) {
            bot.browser.readScreen(Misc.Durations.SECOND);
            seg = MarvinSegment.fromCue(BHBot.cues.get("No"), 2 * Misc.Durations.SECOND, bot.browser);
            if (seg == null) {
                BHBot.logger.error("Error: 'Team not full/ordered' window detected, but no 'No' button found. Restarting...");
                return true;
            }
            bot.browser.clickOnSeg(seg);
            bot.browser.readScreen();

            seg = MarvinSegment.fromCue(BHBot.cues.get("AutoTeam"), 2 * Misc.Durations.SECOND, bot.browser);
            if (seg == null) {
                BHBot.logger.error("Error: 'Team not full/ordered' window detected, but no 'Auto' button found. Restarting...");
                return true;
            }
            bot.browser.clickOnSeg(seg);

            bot.browser.readScreen();
            seg = MarvinSegment.fromCue(BHBot.cues.get("Accept"), 2 * Misc.Durations.SECOND, bot.browser);
            if (seg == null) {
                BHBot.logger.error("Error: 'Team not full/ordered' window detected, but no 'Accept' button found. Restarting...");
                return true;
            }

            String message = "'Team not full/ordered' dialog detected and handled - team has been auto assigned!";

            bot.notificationManager.sendErrorNotification("Team auto assigned", message);

            //bot.browser.clickOnSeg(seg);
            bot.browser.closePopupSecurely(BHBot.cues.get("Accept"), BHBot.cues.get("Accept"));

            BHBot.logger.info(message);
        }

        return false; // all OK
    }

    private boolean handleGuildLeaveConfirm() {
        bot.browser.readScreen();
        if (MarvinSegment.fromCue(BHBot.cues.get("GuildLeaveConfirm"), Misc.Durations.SECOND * 3, bot.browser) != null) {
            Misc.sleep(500); // in case popup is still sliding downward
            bot.browser.readScreen();
            MarvinSegment seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), 10 * Misc.Durations.SECOND, bot.browser);
            if (seg == null) {
                BHBot.logger.error("Error: 'Guild Leave Confirm' window detected, but no 'Yes' green button found. Restarting...");
                return true;
            }
            bot.browser.clickOnSeg(seg);
            Misc.sleep(2 * Misc.Durations.SECOND);

            BHBot.logger.info("'Guild Leave' dialog detected and handled!");
        }

        return false; // all ok
    }

    private Boolean handleDisabledBattles() {
        bot.browser.readScreen();
        if (MarvinSegment.fromCue(BHBot.cues.get("DisabledBattles"), Misc.Durations.SECOND * 3, bot.browser) != null) {
            Misc.sleep(500); // in case popup is still sliding downward
            bot.browser.readScreen();
            MarvinSegment seg = MarvinSegment.fromCue(BHBot.cues.get("Close"), 10 * Misc.Durations.SECOND, bot.browser);
            if (seg == null) {
                BHBot.logger.error("Error: 'Disabled battles' popup detected, but no 'Close' blue button found. Restarting...");
                return null;
            }
            bot.browser.clickOnSeg(seg);
            Misc.sleep(2 * Misc.Durations.SECOND);

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
    private boolean handleNotEnoughEnergyPopup(@SuppressWarnings("SameParameterValue") int delay, BHBot.State state) {
        MarvinSegment seg = MarvinSegment.fromCue(BHBot.cues.get("NotEnoughEnergy"), delay, bot.browser);
        if (seg != null) {
            // we don't have enough energy!
            BHBot.logger.warn("Problem detected: insufficient energy to attempt " + state + ". Cancelling...");
            bot.browser.closePopupSecurely(BHBot.cues.get("NotEnoughEnergy"), BHBot.cues.get("No"));


            if (state.equals(BHBot.State.WorldBoss)) {
                bot.browser.closePopupSecurely(BHBot.cues.get("WorldBossSummonTitle"), BHBot.cues.get("X"));

                bot.browser.closePopupSecurely(BHBot.cues.get("WorldBossTitle"), BHBot.cues.get("X"));
            } else {
                bot.browser.closePopupSecurely(BHBot.cues.get("AutoTeam"), BHBot.cues.get("X"));

                // if D4 close the dungeon info window, else close the char selection screen
                if (specialDungeon) {
                    seg = MarvinSegment.fromCue(BHBot.cues.get("X"), 5 * Misc.Durations.SECOND, bot.browser);
                    if (seg != null)
                        bot.browser.clickOnSeg(seg);
                    specialDungeon = false;
                } else {
                    // close difficulty selection screen:
                    bot.browser.closePopupSecurely(BHBot.cues.get("Normal"), BHBot.cues.get("X"));
                }

                // close zone view window:
                bot.browser.closePopupSecurely(BHBot.cues.get("ZonesButton"), BHBot.cues.get("X"));
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
        MarvinSegment seg = MarvinSegment.fromCue("NotEnoughTokens", bot.browser);

        if (seg != null) {
            BHBot.logger.warn("Not enough token popup detected! Closing trial window.");

            if (!bot.browser.closePopupSecurely(BHBot.cues.get("NotEnoughTokens"), BHBot.cues.get("No"))) {
                BHBot.logger.error("Impossible to close the 'Not Enough Tokens' pop-up window. Restarting");
                return false;
            }

            if (closeTeamWindow) {
                if (!bot.browser.closePopupSecurely(BHBot.cues.get("Accept"), BHBot.cues.get("X"))) {
                    BHBot.logger.error("Impossible to close the team window when no tokens are available. Restarting");
                    return false;
                }
            }

            if (!bot.browser.closePopupSecurely(BHBot.cues.get("TrialsOrGauntletWindow"), BHBot.cues.get("X"))) {
                BHBot.logger.error("Impossible to close the 'TrialsOrGauntletWindow' window. Restarting");
                return false;
            }
        }
        return true;
    }

    /**
     * This method will handle the success threshold based on the state
     *
     * @param state the State used to check the success threshold
     */
    private void handleSuccessThreshold(BHBot.State state) {

        // We only handle Trials and Gautlets
        if (bot.getState() != BHBot.State.Gauntlet && bot.getState() != BHBot.State.Trials) return;

        BHBot.logger.debug("Victories in a row for " + state + " is " + counters.get(bot.getState()).getVictoriesInARow());

        // We make sure that we have a setting for the current state
        if (bot.settings.successThreshold.containsKey(bot.getState().getShortcut())) {
            Map.Entry<Integer, Integer> successThreshold = bot.settings.successThreshold.get(bot.getState().getShortcut());
            int minimumVictories = successThreshold.getKey();
            int lvlIncrease = successThreshold.getValue();

            if (counters.get(bot.getState()).getVictoriesInARow() >= minimumVictories) {
                if ("t".equals(bot.getState().getShortcut()) || "g".equals(bot.getState().getShortcut())) {
                    int newDifficulty;
                    String original, updated;

                    if ("t".equals(bot.getState().getShortcut())) {
                        newDifficulty = bot.settings.difficultyTrials + lvlIncrease;
                        original = "difficultyTrials " + bot.settings.difficultyTrials;
                        updated = "difficultyTrials " + newDifficulty;
                    } else { // Gauntlets
                        newDifficulty = bot.settings.difficultyGauntlet + lvlIncrease;
                        original = "difficultyGauntlet " + bot.settings.difficultyGauntlet;
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
            List<MarvinSegment> list = FindSubimage.findSubimage(im, BHBot.cues.get(numberPrefix + "" + i).im, 1.0, true, false, 0, 0, 0, 0);
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
     * @param input  The input image that will be converted in black and white scale
     * @param black  White color treshold
     * @param white  Black color treshold
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
        return detectDifficulty(BHBot.cues.get("Difficulty"));
    }

    /**
     * Detects selected difficulty in trials/gauntlet window. <br>
     * NOTE: Trials/gauntlet window must be open for this to work! <br>
     *
     * @return 0 in case of an error, or the selected difficulty level instead.
     */
    private int detectDifficulty(Cue difficulty) {
        bot.browser.readScreen(2 * Misc.Durations.SECOND); // note that sometimes the cue will be gray (disabled) since the game is fetching data from the server - in that case we'll have to wait a bit

        MarvinSegment seg = MarvinSegment.fromCue(difficulty, bot.browser);
        if (seg == null) {
            seg = MarvinSegment.fromCue(BHBot.cues.get("DifficultyDisabled"), bot.browser);
            if (seg != null) { // game is still fetching data from the server... we must wait a bit!
                Misc.sleep(5 * Misc.Durations.SECOND);
                seg = MarvinSegment.fromCue(difficulty, 20 * Misc.Durations.SECOND, bot.browser);
            }
        }
        if (seg == null) {
            BHBot.logger.error("Error: unable to detect difficulty selection box!");
            bot.saveGameScreen("difficulty_error", "errors");
            return 0; // error
        }

        MarvinImage im = new MarvinImage(bot.browser.getImg().getSubimage(seg.x1 + 35, seg.y1 + 30, 55, 19));

        // make it white-gray (to facilitate cue recognition):
        makeImageBlackWhite(im, new Color(25, 25, 25), new Color(255, 255, 255));

        BufferedImage imb = im.getBufferedImage();

        return readNumFromImg(imb);
    }

    /* World boss reading and changing section */
    private int detectWorldBossTier() {
        int xOffset, yOffset, w, h;
        bot.browser.readScreen(Misc.Durations.SECOND);
        MarvinSegment tierDropDown;

        if (!bot.browser.isDoNotShareUrl()) {
            xOffset = 401;
            yOffset = 210;
        } else {
            xOffset = 400;
            yOffset = 207;
        }
        w = 21;
        h = 19;

        tierDropDown = MarvinSegment.fromCue("WorldBossTierDropDown", Misc.Durations.SECOND, bot.browser); // For tier drop down menu

        if (tierDropDown == null) {
            BHBot.logger.error("Error: unable to detect world boss difficulty selection box in detectWorldBossTier!");
            return 0; // error
        }

        MarvinImage im = new MarvinImage(bot.browser.getImg().getSubimage(xOffset, yOffset, w, h));

        // make it white-gray (to facilitate cue recognition):
        makeImageBlackWhite(im, new Color(25, 25, 25), new Color(255, 255, 255));

        BufferedImage imb = im.getBufferedImage();

        return readNumFromImg(imb);
    }

    private boolean changeWorldBossTier(int targetTier, WorldBoss wbType) {
        MarvinSegment seg;
        bot.browser.readScreen(Misc.Durations.SECOND); //wait for screen to stabilize
        seg = MarvinSegment.fromCue(BHBot.cues.get("WorldBossTierDropDown"), 2 * Misc.Durations.SECOND, bot.browser);

        if (seg == null) {
            BHBot.logger.error("Error: unable to detect world boss difficulty selection box in changeWorldBossTier!");
            bot.saveGameScreen("change_wb_error", "errors");
            return false;
        }

        bot.browser.clickOnSeg(seg);
        bot.browser.readScreen(2 * Misc.Durations.SECOND); //wait for screen to stabilize

        // Temporary variables initialialized
        Point tierButton = new Point(388, 167); // Position of the top tier selection button
        int yOffset; // If the bot needs to click on a tier that is not the top most, what is the offset of it?

        // If the world boss has scrollbar positions, it means that we must check if we need to scroll the bar
        if ( wbType.getYScrollerPositions().length > 0) {

            seg = MarvinSegment.fromCue(BHBot.cues.get("StripScrollerTopPos"), 2 * Misc.Durations.SECOND, bot.browser);

            if (seg == null) {
                BHBot.logger.error("Error: unable to detect world boss scrollbar postion in changeWorldBossTier!");
                bot.saveGameScreen("scroll_wb_error", "errors");
                return false;
            }

            int scrollBarPos = Misc.findClosestMatch(wbType.getYScrollerPositions(), seg.y1);

            // if scrollBarPos is zero, the bar is at top and the max tier available is equal to maxTierAvailable
            int maxTierAvailable = wbType.getMaxTier() - scrollBarPos;
            int minTierAvailable = maxTierAvailable - 4; // there are five buttons

            // Do we need to scroll down?
            if (targetTier < minTierAvailable) {
                while (targetTier < minTierAvailable) {
                    seg = MarvinSegment.fromCue("DropDownDown", 5 * Misc.Durations.SECOND, Bounds.fromWidthHeight(515, 415, 50, 50),  bot.browser);
                    if (seg == null) {
                        BHBot.logger.error("Error: unable to scroll dowon in changeWorldBossTier!");
                        bot.saveGameScreen("scroll_down_wb_error", "errors");
                        return false;
                    }

                    bot.browser.clickOnSeg(seg);
                    maxTierAvailable -= 1;
                    minTierAvailable -= 1;
                }
            }

            // Do we need to scroll up?
            if (targetTier > maxTierAvailable) {
                while (targetTier > maxTierAvailable) {
                    seg = MarvinSegment.fromCue("DropDownUp", 5 * Misc.Durations.SECOND, Bounds.fromWidthHeight(515, 115, 50, 50),  bot.browser);
                    if (seg == null) {
                        BHBot.logger.error("Error: unable to scroll up in changeWorldBossTier!");
                        bot.saveGameScreen("scroll_up_wb_error", "errors");
                        return false;
                    }

                    bot.browser.clickOnSeg(seg);
                    maxTierAvailable += 1;
                    minTierAvailable += 1;
                }
            }

            yOffset = maxTierAvailable - targetTier;
        } else {
            yOffset = wbType.getMaxTier() - targetTier;
        }

        tierButton.y += yOffset * 60;
        bot.browser.clickInGame(tierButton);

        return true;
    }

    private int detectWorldBossDifficulty() {
        bot.browser.readScreen();

        if (MarvinSegment.fromCue(BHBot.cues.get("WorldBossDifficultyNormal"), Misc.Durations.SECOND, bot.browser) != null) {
            return 1;
        } else if (MarvinSegment.fromCue(BHBot.cues.get("WorldBossDifficultyHard"), Misc.Durations.SECOND, bot.browser) != null) {
            return 2;
        } else if (MarvinSegment.fromCue(BHBot.cues.get("WorldBossDifficultyHeroic"), Misc.Durations.SECOND, bot.browser) != null) {
            return 3;
        } else return 0;
    }

    private void changeWorldBossDifficulty(int target) {

        bot.browser.readScreen(Misc.Durations.SECOND); //screen stabilising
        bot.browser.clickInGame(480, 300); //difficulty button
        bot.browser.readScreen(Misc.Durations.SECOND); //screen stabilising

        Cue difficultySelection;

        if (target == 1) {
            difficultySelection = BHBot.cues.get("cueWBSelectNormal");
        } else if (target == 2) {
            difficultySelection = BHBot.cues.get("cueWBSelectHard");
        } else if (target == 3) {
            difficultySelection = BHBot.cues.get("cueWBSelectHeroic");
        } else {
            BHBot.logger.error("Wrong target value in changeWorldBossDifficulty, defult to normal!");
            difficultySelection = BHBot.cues.get("cueWBSelectNormal");
        }

        MarvinSegment seg = MarvinSegment.fromCue(difficultySelection, Misc.Durations.SECOND * 2, bot.browser);
        if (seg != null) {
            bot.browser.clickOnSeg(seg);
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
        return selectDifficulty(oldDifficulty, newDifficulty, BHBot.cues.get("SelectDifficulty"), 1);
    }

    private boolean selectDifficulty(int oldDifficulty, int newDifficulty, Cue difficulty, int step) {
        if (oldDifficulty == newDifficulty)
            return true; // no change

        MarvinSegment seg = MarvinSegment.fromCue(difficulty, 2 * Misc.Durations.SECOND, bot.browser);
        if (seg == null) {
            BHBot.logger.error("Error: unable to detect 'select difficulty' button while trying to change difficulty level!");
            return false; // error
        }

        bot.browser.clickOnSeg(seg);

        bot.browser.readScreen(5 * Misc.Durations.SECOND);

        return selectDifficultyFromDropDown(newDifficulty, 0, step);
    }

    /**
     * Internal routine. Difficulty drop down must be open for this to work!
     * Note that it closes the drop-down when it is done (except if an error occurred). However there is a close
     * animation and the caller must wait for it to finish.
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
            bot.saveGameScreen("select_difficulty_recursion", "errors");
            tryClosingWindow(); // clean up after our selves (ignoring any exception while doing it)
            return false;
        }

        MarvinSegment seg;

        MarvinImage subm = new MarvinImage(bot.browser.getImg().getSubimage(350, 150, 70, 35)); // the first (upper most) of the 5 buttons in the drop-down menu. Note that every while a "tier x" is written bellow it, so text is higher up (hence we need to scan a larger area)
        makeImageBlackWhite(subm, new Color(25, 25, 25), new Color(255, 255, 255));
        BufferedImage sub = subm.getBufferedImage();
        int num = readNumFromImg(sub);
//		BHBot.logger.info("num = " + Integer.toString(num));
        if (num == 0) {
            BHBot.logger.error("Error: unable to read difficulty level from a drop-down menu!");
            bot.saveGameScreen("select_difficulty_read", "errors");
            tryClosingWindow(); // clean up after our selves (ignoring any exception while doing it)
            return false;
        }

        int move = (newDifficulty - num) / step; // if negative, we have to move down (in dropdown/numbers), or else up
//		BHBot.logger.info("move = " + Integer.toString(move));

        if (move >= -4 && move <= 0) {
            // we have it on screen. Let's select it!
            bot.browser.clickInGame(posx, posy[Math.abs(move)]); // will auto-close the drop down (but it takes a second or so, since it's animated)
            return true;
        }

        // scroll the drop-down until we reach our position:
        // recursively select new difficulty
        //*** should we increase this time?
        if (move > 0) {
            // move up
            seg = MarvinSegment.fromCue(BHBot.cues.get("DropDownUp"), bot.browser);
            if (seg == null) {
                BHBot.logger.error("Error: unable to detect up arrow in trials/gauntlet/expedition difficulty drop-down menu!");
                bot.saveGameScreen("select_difficulty_arrow_up", "errors");
                bot.browser.clickInGame(posx, posy[0]); // regardless of the error, click on the first selection in the drop-down, so that we don't need to re-scroll entire list next time we try!
                return false;
            }
            for (int i = 0; i < move; i++) {
                bot.browser.clickOnSeg(seg);
            }
            // OK, we should have a target value on screen now, in the first spot. Let's click it!
        } else {
            // move down
            seg = MarvinSegment.fromCue(BHBot.cues.get("DropDownDown"), bot.browser);
            if (seg == null) {
                BHBot.logger.error("Error: unable to detect down arrow in trials/gauntlet/expedition difficulty drop-down menu!");
                bot.saveGameScreen("select_difficulty_arrow_down", "errors");
                bot.browser.clickInGame(posx, posy[0]); // regardless of the error, click on the first selection in the drop-down, so that we don't need to re-scroll entire list next time we try!
                return false;
            }
            int moves = Math.abs(move) - 4;
//			BHBot.logger.info("Scrolls to 60 = " + Integer.toString(moves));
            for (int i = 0; i < moves; i++) {
                bot.browser.clickOnSeg(seg);
            }
            // OK, we should have a target value on screen now, in the first spot. Let's click it!
        }
        bot.browser.readScreen(5 * Misc.Durations.SECOND); //*** should we increase this time?
        return selectDifficultyFromDropDown(newDifficulty, recursionDepth + 1, step); // recursively select new difficulty
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
        MarvinSegment seg = MarvinSegment.fromCue(BHBot.cues.get("Cost"), 15 * Misc.Durations.SECOND, bot.browser);
        if (seg == null) {
            BHBot.logger.error("Error: unable to detect cost selection box!");
            bot.saveGameScreen("cost_selection", "errors");
            return 0; // error
        }

        // because the popup may still be sliding down and hence cue could be changing position, we try to read cost in a loop (until a certain timeout):
        int d;
        int counter = 0;
        boolean success = true;
        while (true) {
            MarvinImage im = new MarvinImage(bot.browser.getImg().getSubimage(seg.x1 + 2, seg.y1 + 20, 35, 24));
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
            Misc.sleep(Misc.Durations.SECOND); // sleep a bit in order for the popup to slide down
            bot.browser.readScreen();
            seg = MarvinSegment.fromCue(BHBot.cues.get("Cost"), bot.browser);
        }

        if (!success) {
            BHBot.logger.error("Error: unable to detect cost selection box value!");
            bot.saveGameScreen("cost_value", "errors");
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

        MarvinSegment seg = MarvinSegment.fromCue(BHBot.cues.get("SelectCost"), 5 * Misc.Durations.SECOND, bot.browser);
        if (seg == null) {
            BHBot.logger.error("Error: unable to detect 'select cost' button while trying to change cost!");
            return false; // error
        }

        bot.browser.clickOnSeg(seg);

        MarvinSegment.fromCue("CostDropDown", 5 * Misc.Durations.SECOND, bot.browser); // wait for the cost selection popup window to open

        // horizontal position of the 5 buttons:
        final int posx = 390;
        // vertical positions of the 5 buttons:
        final int[] posy = new int[]{170, 230, 290, 350, 410};

        bot.browser.clickInGame(posx, posy[newCost - 1]); // will auto-close the drop down (but it takes a second or so, since it's animated)
        Misc.sleep(2 * Misc.Durations.SECOND);

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
                seg = MarvinSegment.fromCue(windowCue, bot.browser);
                if (seg == null)
                    return;
            }
            seg = MarvinSegment.fromCue(BHBot.cues.get("X"), bot.browser);
            if (seg != null)
                bot.browser.clickOnSeg(seg);
        } catch (Exception e) {
            BHBot.logger.error("Error in tryClosingWindow", e);
        }
    }

    /**
     * @return -1 on error
     */
    private int detectEquipmentFilterScrollerPos() {
        final int[] yScrollerPositions = {146, 164, 181, 199, 217, 235, 252, 270, 288, 306, 323, 341}; // top scroller positions

        MarvinSegment seg = MarvinSegment.fromCue(BHBot.cues.get("StripScrollerTopPos"), 2 * Misc.Durations.SECOND, bot.browser);
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
        bot.browser.clickInGame(55, 465);

        seg = MarvinSegment.fromCue(BHBot.cues.get("StripSelectorButton"), 10 * Misc.Durations.SECOND, bot.browser);
        if (seg == null) {
            BHBot.logger.error("Error: unable to detect equipment filter button! Skipping...");
            return;
        }

        // now lets see if the right category is already selected:
        seg = MarvinSegment.fromCue(type.getCue(), 500, bot.browser);
        if (seg == null) {
            // OK we need to manually select the correct category!
            seg = MarvinSegment.fromCue(BHBot.cues.get("StripSelectorButton"), bot.browser);
            bot.browser.clickOnSeg(seg);

            MarvinSegment.fromCue(BHBot.cues.get("StripItemsTitle"), 10 * Misc.Durations.SECOND, bot.browser); // waits until "Items" popup is detected
            bot.browser.readScreen(500); // to stabilize sliding popup a bit

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
                seg = MarvinSegment.fromCue(BHBot.cues.get("DropDownDown"), 5 * Misc.Durations.SECOND, bot.browser);
                for (int i = 0; i < move; i++) {
                    bot.browser.clickOnSeg(seg);
                    scrollerPos++;
                }
            } else { // bestIndex > type.maxPos
                // we must scroll up!
                int move = scrollerPos - type.minPos();
                seg = MarvinSegment.fromCue(BHBot.cues.get("DropDownUp"), 5 * Misc.Durations.SECOND, bot.browser);
                for (int i = 0; i < move; i++) {
                    bot.browser.clickOnSeg(seg);
                    scrollerPos--;
                }
            }

            // make sure scroller is in correct position now:
            bot.browser.readScreen(500); // so that the scroller stabilizes a bit
            int newScrollerPos = detectEquipmentFilterScrollerPos();
            int counter = 0;
            while (newScrollerPos != scrollerPos) {
                if (counter > 3) {
                    BHBot.logger.warn("Problem detected: unable to adjust scroller position in the character window (scroller position: " + newScrollerPos + ", should be: " + scrollerPos + ")! Skipping strip down/up...");
                    return;
                }
                bot.browser.readScreen(Misc.Durations.SECOND);
                newScrollerPos = detectEquipmentFilterScrollerPos();
                counter++;
            }
            bot.browser.clickInGame(xButtonPosition, yButtonPositions[type.getButtonPos() - scrollerPos]);
            // clicking on the button will close the window automatically... we just need to wait a bit for it to close
            MarvinSegment.fromCue(BHBot.cues.get("StripSelectorButton"), 5 * Misc.Durations.SECOND, bot.browser); // we do this just in order to wait for the previous menu to reappear
        }

        waitForInventoryIconsToLoad(); // first of all, lets make sure that all icons are loaded

        // now deselect/select the strongest equipment in the menu:

        seg = MarvinSegment.fromCue(BHBot.cues.get("StripEquipped"), 500, bot.browser); // if "E" icon is not found, that means that some other item is equipped or that no item is equipped
        boolean equipped = seg != null; // is strongest item equipped already?

        // position of top-left item (which is the strongest) is (490, 210)
        if (dir == StripDirection.StripDown) {
            bot.browser.clickInGame(490, 210);
        }
        if (!equipped) // in case item was not equipped, we must click on it twice, first time to equip it, second to unequip it. This could happen for example when we had some weaker item equipped (or no item equipped).
            bot.browser.clickInGame(490, 210);

        // OK, we're done, lets close the character menu window:
        bot.browser.closePopupSecurely(BHBot.cues.get("StripSelectorButton"), BHBot.cues.get("X"));
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

    /**
     * Daily collection of fishing baits!
     */
    private void handleFishingBaits() {
        MarvinSegment seg;

        seg = MarvinSegment.fromCue(BHBot.cues.get("Fishing"), Misc.Durations.SECOND * 5, bot.browser);
        if (seg != null) {
            bot.browser.clickOnSeg(seg);
            Misc.sleep(Misc.Durations.SECOND); // we allow some seconds as maybe the reward popup is sliding down

            detectCharacterDialogAndHandleIt();

            seg = MarvinSegment.fromCue(BHBot.cues.get("WeeklyRewards"), Misc.Durations.SECOND * 5, bot.browser);
            if (seg != null) {
                seg = MarvinSegment.fromCue(BHBot.cues.get("X"), 5 * Misc.Durations.SECOND, bot.browser);
                if (seg != null) {
                    if ((bot.settings.screenshots.contains("a"))) {
                        bot.saveGameScreen("fishing-baits", "fishing");
                    }
                    bot.browser.clickOnSeg(seg);
                    BHBot.logger.info("Correctly collected fishing baits");
                    bot.browser.readScreen(Misc.Durations.SECOND * 2);
                } else {
                    BHBot.logger.error("Something weng wrong while collecting fishing baits, restarting...");
                    bot.saveGameScreen("fishing-error-baits", "errors");
                    restart();
                }
            }

            seg = MarvinSegment.fromCue(BHBot.cues.get("X"), 5 * Misc.Durations.SECOND, bot.browser);
            if (seg != null) {
                bot.browser.clickOnSeg(seg);
                Misc.sleep(Misc.Durations.SECOND * 2);
                bot.browser.readScreen();
            } else {
                BHBot.logger.error("Something went wrong while closing the fishing dialog, restarting...");
                bot.saveGameScreen("fishing-error-closing", "errors");
                restart();
            }

        } else {
            BHBot.logger.warn("Impossible to find the fishing button");
        }
        bot.browser.readScreen(Misc.Durations.SECOND * 2);
    }

    private boolean consumableReplaceCheck() {
        int coloursFound = 0;

        boolean foundGreen = false;
        boolean foundBlue = false;
        boolean foundRedFaded = false;
        boolean foundYellow = false;
        boolean foundRed = false;

        bot.browser.readScreen();
        BufferedImage consumableTest = bot.browser.getImg().getSubimage(258, 218, 311, 107);

        Color green = new Color(150, 254, 124);
        Color blue = new Color(146, 157, 243);
        Color redFaded = new Color(254, 127, 124); //faded red on 75% boosts
        Color yellow = new Color(254, 254, 0);
        Color red = new Color(254, 0, 71);

        for (int y = 0; y < consumableTest.getHeight(); y++) {
            for (int x = 0; x < consumableTest.getWidth(); x++) {
                if (!foundGreen && new Color(consumableTest.getRGB(x, y)).equals(green)) {
                    foundGreen = true;
                    coloursFound++;
                } else if (!foundBlue && new Color(consumableTest.getRGB(x, y)).equals(blue)) {
                    foundBlue = true;
                    coloursFound++;
                } else if (!foundRedFaded && new Color(consumableTest.getRGB(x, y)).equals(redFaded)) {
                    foundRedFaded = true;
                    coloursFound++;
                } else if (!foundYellow && new Color(consumableTest.getRGB(x, y)).equals(yellow)) {
                    foundYellow = true;
                    coloursFound++;
                } else if (!foundRed && new Color(consumableTest.getRGB(x, y)).equals(red)) {
                    foundRed = true;
                    coloursFound++;
                }

                if (coloursFound > 1) {
                    BHBot.logger.info("Replace Consumables text found, skipping");
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * We must be in main menu for this to work!
     */
    private void handleConsumables() {
        if (!bot.settings.autoConsume || bot.settings.consumables.size() == 0) // consumables management is turned off!
            return;

        MarvinSegment seg;

        boolean exp = MarvinSegment.fromCue(BHBot.cues.get("BonusExp"), bot.browser) != null;
        boolean item = MarvinSegment.fromCue(BHBot.cues.get("BonusItem"), bot.browser) != null;
        boolean speed = MarvinSegment.fromCue(BHBot.cues.get("BonusSpeed"), bot.browser) != null;
        boolean gold = MarvinSegment.fromCue(BHBot.cues.get("BonusGold"), bot.browser) != null;

        // Special consumables
        if (MarvinSegment.fromCue(BHBot.cues.get("ConsumablePumkgor"), bot.browser) != null || MarvinSegment.fromCue(BHBot.cues.get("ConsumableBroccoli"), bot.browser) != null
                || MarvinSegment.fromCue(BHBot.cues.get("ConsumableGreatFeast"), bot.browser) != null || MarvinSegment.fromCue(BHBot.cues.get("ConsumableGingernaut"), bot.browser) != null
                || MarvinSegment.fromCue(BHBot.cues.get("ConsumableCoco"), bot.browser) != null) {
            exp = true;
            item = true;
            speed = true;
            gold = true;
            // BHBot.logger.info("Special consumable detected, skipping all the rest...");
        }

        EnumSet<ConsumableType> duplicateConsumables = EnumSet.noneOf(ConsumableType.class); // here we store consumables that we wanted to consume now but we have detected they are already active, so we skipped them (used for error reporting)
        EnumSet<ConsumableType> consumables = EnumSet.noneOf(ConsumableType.class); // here we store consumables that we want to consume now
        for (String s : bot.settings.consumables)
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
        bot.browser.clickInGame(55, 465);

        seg = MarvinSegment.fromCue(BHBot.cues.get("StripSelectorButton"), 15 * Misc.Durations.SECOND, bot.browser);
        if (seg == null) {
            BHBot.logger.warn("Error: unable to detect equipment filter button! Skipping...");
            return;
        }

        // now lets select the <Consumables> category (if it is not already selected):
        seg = MarvinSegment.fromCue(BHBot.cues.get("FilterConsumables"), 500, bot.browser);
        if (seg == null) { // if not, right category (<Consumables>) is already selected!
            // OK we need to manually select the <Consumables> category!
            seg = MarvinSegment.fromCue(BHBot.cues.get("StripSelectorButton"), bot.browser);
            bot.browser.clickOnSeg(seg);

            MarvinSegment.fromCue(BHBot.cues.get("StripItemsTitle"), 10 * Misc.Durations.SECOND, bot.browser); // waits until "Items" popup is detected
            bot.browser.readScreen(500); // to stabilize sliding popup a bit

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
                seg = MarvinSegment.fromCue(BHBot.cues.get("DropDownUp"), 5 * Misc.Durations.SECOND, bot.browser);
                for (int i = 0; i < move; i++) {
                    bot.browser.clickOnSeg(seg);
                    scrollerPos--;
                }
            }

            // make sure scroller is in correct position now:
            bot.browser.readScreen(2000); // so that the scroller stabilizes a bit //Quick Fix slow down
            int newScrollerPos = detectEquipmentFilterScrollerPos();
            int counter = 0;
            while (newScrollerPos != scrollerPos) {
                if (counter > 3) {
                    BHBot.logger.warn("Problem detected: unable to adjust scroller position in the character window (scroller position: " + newScrollerPos + ", should be: " + scrollerPos + ")! Skipping consumption of consumables...");
                    return;
                }
                bot.browser.readScreen(Misc.Durations.SECOND);
                newScrollerPos = detectEquipmentFilterScrollerPos();
                counter++;
            }
            bot.browser.clickInGame(xButtonPosition, yButtonPositions[1]);
            // clicking on the button will close the window automatically... we just need to wait a bit for it to close
            MarvinSegment.fromCue(BHBot.cues.get("StripSelectorButton"), 5 * Misc.Durations.SECOND, bot.browser); // we do this just in order to wait for the previous menu to reappear
        }

        // now consume the consumable(s):

        bot.browser.readScreen(500); // to stabilize window a bit
        Bounds bounds = new Bounds(450, 165, 670, 460); // detection area (where consumables icons are visible)

        while (!consumables.isEmpty()) {
            waitForInventoryIconsToLoad(); // first of all, lets make sure that all icons are loaded
            for (Iterator<ConsumableType> i = consumables.iterator(); i.hasNext(); ) {
                ConsumableType c = i.next();
                seg = MarvinSegment.fromCue(new Cue(c.getInventoryCue(), bounds), bot.browser);
                if (seg != null) {
                    // OK we found the consumable icon! Lets click it...
                    bot.browser.clickOnSeg(seg);
                    MarvinSegment.fromCue(BHBot.cues.get("ConsumableTitle"), 5 * Misc.Durations.SECOND, bot.browser); // wait for the consumable popup window to appear
                    bot.browser.readScreen(500); // wait for sliding popup to stabilize a bit

                    /*
                     *  Measure distance between "Consumable" (popup title) and "Yes" (green yes button).
                     *  This seems to be the safest way to distinguish the two window types. Because text
                     *  inside windows change and sometimes letters are wider apart and sometimes no, so it
                     *  is not possible to detect cue like "replace" wording, or any other (I've tried that
                     *  and failed).
                     */

                    if (!consumableReplaceCheck()) {
                        // don't consume the consumable... it's already in use!
                        BHBot.logger.warn("\"Replace consumable\" dialog detected for (" + c.getName() + "). Skipping...");
                        duplicateConsumables.add(c);
                        bot.browser.closePopupSecurely(BHBot.cues.get("ConsumableTitle"), BHBot.cues.get("No"));
                    } else {
                        // consume the consumable:
                        bot.browser.closePopupSecurely(BHBot.cues.get("ConsumableTitle"), BHBot.cues.get("YesGreen"));
                    }
                    MarvinSegment.fromCue(BHBot.cues.get("StripSelectorButton"), 5 * Misc.Durations.SECOND, bot.browser); // we do this just in order to wait for the previous menu to reappear
                    i.remove();
                }
            }

            if (!consumables.isEmpty()) {
                seg = MarvinSegment.fromCue(BHBot.cues.get("ScrollerAtBottom"), 500, bot.browser);
                if (seg != null)
                    break; // there is nothing we can do anymore... we've scrolled to the bottom and haven't found the icon(s). We obviously don't have the required consumable(s)!

                // lets scroll down:
                seg = MarvinSegment.fromCue(BHBot.cues.get("DropDownDown"), 5 * Misc.Durations.SECOND, bot.browser);
                for (int i = 0; i < 4; i++) { //the menu has 4 rows so we move to the next four rows and check again
                    bot.browser.clickOnSeg(seg);
                }

                bot.browser.readScreen(Misc.Durations.SECOND); // so that the scroller stabilizes a bit
            }
        }

        // OK, we're done, lets close the character menu window:
        boolean result = bot.browser.closePopupSecurely(BHBot.cues.get("StripSelectorButton"), BHBot.cues.get("X"));
        if (!result) {
            BHBot.logger.warn("Done. Error detected while trying to close character window. Ignoring...");
            return;
        }

        if (!consumables.isEmpty()) {
            BHBot.logger.warn("Some consumables were not found (out of stock?) so were not consumed. These are: " + Misc.listToString(consumables) + ".");

            for (ConsumableType c : consumables) {
                bot.settings.consumables.remove(c.getName());
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
        Cue cue = new Cue(BHBot.cues.get("LoadingInventoryIcon"), bounds);

        int counter = 0;
        seg = MarvinSegment.fromCue(cue, bot.browser);
        while (seg != null) {
            bot.browser.readScreen(Misc.Durations.SECOND);

            seg = MarvinSegment.fromCue(BHBot.cues.get("StripSelectorButton"), bot.browser);
            if (seg == null) {
                BHBot.logger.error("Error: while detecting possible loading of inventory icons, inventory cue has not been detected! Ignoring...");
                return;
            }

            seg = MarvinSegment.fromCue(cue, bot.browser);
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

        /*
            In this section we check if we are able to run the activity again and if so reset the timer to 0
            else we wait for the standard timer until we check again
         */

        if (((globalShards - 1) >= bot.settings.minShards) && bot.getState() == BHBot.State.Raid) {
            timeLastShardsCheck = 0;
        }

        if (((globalBadges - bot.settings.costExpedition) >= bot.settings.costExpedition) && bot.getState() == BHBot.State.Expedition) {
            timeLastExpBadgesCheck = 0;
        }

        if (((globalBadges - bot.settings.costInvasion) >= bot.settings.costInvasion) && bot.getState() == BHBot.State.Invasion) {
            timeLastInvBadgesCheck = 0;
        }

        if (((globalBadges - bot.settings.costGVG) >= bot.settings.costGVG && bot.getState() == BHBot.State.GVG)) {
            timeLastGVGBadgesCheck = 0;
        }

        if (((globalEnergy - 10) >= bot.settings.minEnergyPercentage) && bot.getState() == BHBot.State.Dungeon) {
            timeLastEnergyCheck = 0;
        }

        if (((globalEnergy - 10) >= bot.settings.minEnergyPercentage) && bot.getState() == BHBot.State.WorldBoss) {
            timeLastEnergyCheck = 0;
        }

        if (((globalTickets - bot.settings.costPVP) >= bot.settings.costPVP) && bot.getState() == BHBot.State.PVP) {
            timeLastTicketsCheck = 0;
        }

        if (((globalTokens - bot.settings.costTrials) >= bot.settings.costTrials) && bot.getState() == BHBot.State.Trials) {
            timeLastTrialsTokensCheck = 0;
        }

        if (((globalTokens - bot.settings.costGauntlet) >= bot.settings.costGauntlet && bot.getState() == BHBot.State.Gauntlet)) {
            timeLastGauntletTokensCheck = 0;
        }
    }

    private Bounds opponentSelector(int opponent) {

        if (bot.settings.pvpOpponent < 1 || bot.settings.pvpOpponent > 4) {
            //if setting outside 1-4th opponents we default to 1st
            BHBot.logger.warn("pvpOpponent must be between 1 and 4, defaulting to first opponent");
            bot.settings.pvpOpponent = 1;
            return new Bounds(544, 188, 661, 225); //1st opponent
        }

        if (bot.settings.gvgOpponent < 1 || bot.settings.gvgOpponent > 4) {
            //if setting outside 1-4th opponents we default to 1st
            BHBot.logger.warn("gvgOpponent must be between 1 and 4, defaulting to first opponent");
            bot.settings.gvgOpponent = 1;
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
        bot.setState(BHBot.State.Main);
        resetTimers();
    }

    private void handleFishing() {
        MarvinSegment seg;

        seg = MarvinSegment.fromCue(BHBot.cues.get("Fishing"), Misc.Durations.SECOND * 5, bot.browser);
        if (seg != null) {

            //we make sure that the window is visible
            bot.browser.showBrowser();

            bot.browser.clickOnSeg(seg);
            Misc.sleep(Misc.Durations.SECOND); // we allow some seconds as maybe the reward popup is sliding down

            detectCharacterDialogAndHandleIt();

            int fishingTime = 10 + (bot.settings.baitAmount * 15); //pause for around 15 seconds per bait used, plus 10 seconds buffer

            bot.browser.readScreen();

            seg = MarvinSegment.fromCue(BHBot.cues.get("Play"), Misc.Durations.SECOND * 5, bot.browser);
            if (seg != null) {
                bot.browser.clickOnSeg(seg);
            }

            seg = MarvinSegment.fromCue(BHBot.cues.get("Start"), Misc.Durations.SECOND * 20, bot.browser);
            if (seg != null) {
                try {
                    BHBot.logger.info("Pausing for " + fishingTime + " seconds to fish");
                    bot.scheduler.pause();

                    Process fisher = Runtime.getRuntime().exec("cmd /k \"cd DIRECTORY & java -jar bh-fisher.jar\" " + bot.settings.baitAmount);
                    if (!fisher.waitFor(fishingTime, TimeUnit.SECONDS)) { //run and wait for fishingTime seconds
                        bot.scheduler.resume();
                    }

                } catch (IOException | InterruptedException ex) {
                    BHBot.logger.error("Can't start bh-fisher.jar", ex);
                }

            } else BHBot.logger.info("start not found");

            if (!closeFishingSafely()) {
                BHBot.logger.error("Error closing fishing, restarting..");
                restart();
            }

            bot.browser.readScreen(Misc.Durations.SECOND);
            if (bot.settings.enterGuildHall) enterGuildHall();

            if (bot.settings.hideWindowOnRestart) bot.browser.hideBrowser();
        }

    }

    private boolean closeFishingSafely() {
        MarvinSegment seg;
        bot.browser.readScreen();

        seg = MarvinSegment.fromCue(BHBot.cues.get("Trade"), Misc.Durations.SECOND * 3, bot.browser);
        if (seg != null) {
            bot.browser.clickOnSeg(seg);
        }

        seg = MarvinSegment.fromCue(BHBot.cues.get("X"), Misc.Durations.SECOND * 3, bot.browser);
        if (seg != null) {
            bot.browser.clickOnSeg(seg);
        }

        seg = MarvinSegment.fromCue(BHBot.cues.get("FishingClose"), 3 * Misc.Durations.SECOND, bot.browser);
        if (seg != null) {
            bot.browser.clickOnSeg(seg);
        }

        seg = MarvinSegment.fromCue(BHBot.cues.get("GuildButton"), Misc.Durations.SECOND * 5, bot.browser);
        //else not
        return seg != null; //if we can see the guild button we are successful

    }

    private void enterGuildHall() {
        MarvinSegment seg;

        seg = MarvinSegment.fromCue(BHBot.cues.get("GuildButton"), Misc.Durations.SECOND * 5, bot.browser);
        if (seg != null) {
            bot.browser.clickOnSeg(seg);
        }

        seg = MarvinSegment.fromCue(BHBot.cues.get("Hall"), Misc.Durations.SECOND * 5, bot.browser);
        if (seg != null) {
            bot.browser.clickOnSeg(seg);
        }
    }

    private void handleLoot() {
        MarvinSegment seg;
        BufferedImage victoryPopUpImg = bot.browser.getImg();

        if (bot.notificationManager.shouldNotify()) {
            bot.browser.readScreen();
            String droppedItemMessage;
            Bounds victoryDropArea = new Bounds(100, 160, 630, 420);

            //linkedHashMap so we iterate from mythic > heroic
            LinkedHashMap<String, Cue> itemTier = new LinkedHashMap<>();
            itemTier.put("m", BHBot.cues.get("ItemMyt"));
            itemTier.put("s", BHBot.cues.get("ItemSet"));
            itemTier.put("l", BHBot.cues.get("ItemLeg"));
            itemTier.put("h", BHBot.cues.get("ItemHer"));

            for (Map.Entry<String, Cue> item : itemTier.entrySet()) {
                if (bot.settings.poNotifyDrop.contains(item.getKey()) || bot.settings.discordNotifyDrop.contains(item.getKey())) {
                    seg = FindSubimage.findImage(victoryPopUpImg, item.getValue().im, victoryDropArea.x1, victoryDropArea.y1, victoryDropArea.x2, victoryDropArea.y2);
                    if (seg != null) {
                        // so we don't get legendary crafting materials in raids triggering handleLoot
                        if ((item.getKey().equals("l")) && (restrictedCues(victoryPopUpImg, seg.getBounds()))) return;

                        // this is so we only get Coins, Crafting Materials and Schematics for heroic items
                        if (item.getKey().equals("h") && (!allowedCues(victoryPopUpImg, seg.getBounds()))) return;

                        // we get a mouse over screen of the item if possible
                        if (bot.getState() != BHBot.State.Raid && bot.getState() != BHBot.State.Dungeon && bot.getState() != BHBot.State.Expedition && bot.getState() != BHBot.State.Trials) {
                            //the window moves too fast in these events to mouseOver
                            bot.browser.moveMouseToPos(seg.getCenterX(), seg.getCenterY());
                            bot.browser.readScreen();
                            victoryPopUpImg = bot.browser.getImg();
                            bot.browser.moveMouseAway();
                        }

                        String tierName = getItemTier(item.getKey());
                        droppedItemMessage = tierName + " item dropped!";
                        BHBot.logger.debug(droppedItemMessage);
                        if (bot.settings.victoryScreenshot) {
                            bot.saveGameScreen(bot.getState() + "-" + tierName.toLowerCase(), "loot", victoryPopUpImg);
                        }

                        bot.notificationManager.sendDropNotification(tierName + " item drop in " + bot.getState(), droppedItemMessage, victoryPopUpImg);
                        break;
                    }
                }
            }
        }

    }

    // TODO Merge with ItemGrade Enum
    private String getItemTier(String tier) {
        switch (tier) {
            case "m":
                return "Mythic";
            case "s":
                return "Set";
            case "l":
                return "Legendary";
            case "h":
                return "Heroic";
            default:
                return "unknown_tier";
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

        private final String cueName;

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
            return BHBot.cues.get(cueName);
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

        private final String name;
        private final String inventoryCue;

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
            return BHBot.cues.get(inventoryCue);
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
    enum MinorRune {
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
        private final MinorRuneEffect effect;
        private final ItemGrade grade;

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
            return BHBot.cues.get(getRuneCueName());
        }


        public String getRuneSelectCueName() {
            return "MinorRune" + effect + grade + "Select";
        }

        public String getRuneSelectCueFileName() {
            return "cues/runes/minor" + effect + grade + "Select.png";
        }

        public Cue getRuneSelectCue() {
            return BHBot.cues.get(getRuneSelectCueName());
        }

        @Override
        public String toString() {
            return grade.toString().toLowerCase() + "_" + effect.toString().toLowerCase();
        }
    }

    /**
     * This Enum is used to group together all the information related to the World Boss
     */
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private enum WorldBoss {
        Orlag("o", "Orlag Clan", 1, 3, 12, 5, new int[] {147, 175, 204, 232, 261, 289}),
        Netherworld("n", "Netherworld", 2, 3, 9, 3, new int[] {146, 187, 227}),
        Melvin("m", "Melvin", 3, 10, 11, 4, new int[] {}),
        Ext3rmin4tion("3", "3xt3rmin4tion", 4, 10, 11, 3, new int[] {}),
        BrimstoneSyndicate("b", "Brimstone Syndicate", 5, 11, 12, 3, new int[] {}),
        TitansAttack("t", "Titans Attack", 6, 11, 13, 3, new int[] {}),
        Unknown("?", "Unknown", 7, 11, 100, 1, new int[] {});

        private final String letter;
        private final String Name;
        private final int number;
        private final int minTier;
        private final int maxTier;
        private final int partySize;
        private final int[] yScrollerPositions;

        /**
         * @param letter the shortcut letter used in settings.ini
         * @param Name the real name of the World Boss
         * @param number the World Boss number counting from left to right starting at 1
         * @param minTier the minimum tier required to join the World Boss
         * @param maxTier the maximum tier you are allowed to join for the World Boss
         * @param partySize the party size of the World Boss
         * @param yScrollerPositions the positions of the scroller bar in the tier selection window
         */
        WorldBoss(String letter, String Name, int number, int minTier, int maxTier, int partySize, int[] yScrollerPositions) {
            this.letter = letter;
            this.Name = Name;
            this.number = number;
            this.minTier = minTier;
            this.maxTier = maxTier;
            this.partySize = partySize;
            this.yScrollerPositions = yScrollerPositions;
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

        int getMinTier() {
            return minTier;
        }

        int getMaxTier() {
            return maxTier;
        }

        int[] getYScrollerPositions(){return yScrollerPositions;}

        static WorldBoss fromLetter(String Letter) {
            for (WorldBoss wb : WorldBoss.values()) {
                if (wb.getLetter().equals(Letter)) return wb;
            }
            return null;
        }

        static WorldBoss fromNumber(int number) {
            for (WorldBoss wb : WorldBoss.values()) {
                if (wb.getNumber() == number) return wb;
            }
            return null;
        }
    }

    void cueDifference() { //return similarity % between two screenshots taken 3 seconds apart
        bot.browser.readScreen();
        BufferedImage img1 = bot.browser.getImg();
        Misc.sleep(2500);
        bot.browser.readScreen();
        BufferedImage img2 = bot.browser.getImg();
        CueCompare.imageDifference(img1, img2, 0.8, 0, 800, 0, 520);
    }

    /*
     * Returns true if it finds a defined Cue
     * You can input with getSegBounds to only search the found item area
     */

    private boolean restrictedCues(BufferedImage victoryPopUpImg, Bounds foundArea) {
        MarvinSegment seg;
        HashMap<String, Cue> restrictedCues = new HashMap<>();
        restrictedCues.put("Monster Cell", BHBot.cues.get("Material_R10"));
        restrictedCues.put("Power Stone", BHBot.cues.get("Material_R9"));
        restrictedCues.put("Fire Blossom", BHBot.cues.get("Material_R8"));
        restrictedCues.put("Crubble", BHBot.cues.get("Material_R7"));
        restrictedCues.put("Beanstalk", BHBot.cues.get("Material_R6"));
        restrictedCues.put("Luminous Stone", BHBot.cues.get("Material_R5"));
        restrictedCues.put("Rombit", BHBot.cues.get("Material_R4"));
        restrictedCues.put("Dubloon", BHBot.cues.get("Material_R3"));
        restrictedCues.put("Hyper Shard", BHBot.cues.get("Material_R2"));

        for (Map.Entry<String, Cue> cue : restrictedCues.entrySet()) {
            seg = FindSubimage.findImage(victoryPopUpImg, cue.getValue().im, foundArea.x1, foundArea.y1, foundArea.x2, foundArea.y2);
            if (seg != null) {
                BHBot.logger.debug("Legendary: " + cue.getKey() + " found, skipping handleLoot");
                return true;
            }
        }
        return false;
    }

    /*
     * Returns true if it finds a defined Cue
     * You can input with getSegBounds to only search the found item area
     */

    private boolean allowedCues(BufferedImage victoryPopUpImg, Bounds foundArea) {
        MarvinSegment seg;

        //so we aren't triggered by Skeleton Key heroic cue
        MarvinSegment treasure = MarvinSegment.fromCue(BHBot.cues.get("SkeletonTreasure"), bot.browser);
        if (treasure != null) {
            return false;
        }

        HashMap<String, Cue> allowedCues = new HashMap<>();
        allowedCues.put("Gold Coin", BHBot.cues.get("GoldCoin"));
        allowedCues.put("Heroic Schematic", BHBot.cues.get("HeroicSchematic"));
        allowedCues.put("Microprocessing Chip", BHBot.cues.get("MicroChip"));
        allowedCues.put("Demon Blood", BHBot.cues.get("DemonBlood"));
        allowedCues.put("Hobbit's Foot", BHBot.cues.get("HobbitsFoot"));
        allowedCues.put("Melvin Chest", BHBot.cues.get("MelvinChest"));
        allowedCues.put("Neural Net Rom", BHBot.cues.get("NeuralNetRom"));
        allowedCues.put("Scarlarg Skin", BHBot.cues.get("ScarlargSkin"));

        for (Map.Entry<String, Cue> cue : allowedCues.entrySet()) {
            seg = FindSubimage.findImage(victoryPopUpImg, cue.getValue().im, foundArea.x1, foundArea.y1, foundArea.x2, foundArea.y2);
            if (seg != null) {
                BHBot.logger.debug(cue.getKey() + " found!");
                return true;
            }
        }
        return false;
    }

}
