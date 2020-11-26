package com.github.ilpersi.BHBot;

import com.google.common.collect.Maps;
import org.openqa.selenium.Point;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Comparator.comparing;

public class DungeonThread implements Runnable {

    private int globalShards;
    private int globalBadges;
    private int globalEnergy;
    private int globalXeals;
    private int globalTickets;
    private int globalTokens;

    // z(?<zone>\d{1,2})d(?<dungeon>[1234])
    // 
    // Options: Case insensitive; Exact spacing; Dot doesn’t match line breaks; ^$ don’t match at line breaks; Default line breaks
    // 
    // Match the character “z” literally (case insensitive) «z»
    // Match the regex below and capture its match into a backreference named “zone” (also backreference number 1) «(?<zone>\d{1,2})»
    //    Match a single character that is a “digit” (ASCII 0–9 only) «\d{1,2}»
    //       Between one and 2 times, as many times as possible, giving back as needed (greedy) «{1,2}»
    // Match the character “d” literally (case insensitive) «d»
    // Match the regex below and capture its match into a backreference named “dungeon” (also backreference number 2) «(?<dungeon>[1234])»
    //    Match a single character from the list “1234” «[1234]»
    private final Pattern dungeonRegex = Pattern.compile("z(?<zone>\\d{1,2})d(?<dungeon>[1234])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
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
    private boolean rerunCurrentActivity = false;

    // Generic counters HashMap
    HashMap<BHBot.State, DungeonCounter> counters = new HashMap<>();

    private long ENERGY_CHECK_INTERVAL = 10 * Misc.Durations.MINUTE;
    private long XEALS_CHECK_INTERVAL = 10 * Misc.Durations.MINUTE;
    private long TICKETS_CHECK_INTERVAL = 10 * Misc.Durations.MINUTE;
    private long TOKENS_CHECK_INTERVAL = 10 * Misc.Durations.MINUTE;
    private long BADGES_CHECK_INTERVAL = 10 * Misc.Durations.MINUTE;
    @SuppressWarnings("FieldCanBeLocal")
    private final long BONUS_CHECK_INTERVAL = 10 * Misc.Durations.MINUTE;

    private long timeLastEnergyCheck = 0; // when did we check for Energy the last time?
    private long timeLastXealsCheck = 0; // when did we check for Xeals the last time?
    private long timeLastShardsCheck = 0; // when did we check for Shards the last time?
    private long timeLastTicketsCheck = 0; // when did we check for Tickets the last time?
    private long timeLastTrialsTokensCheck = 0; // when did we check for trials Tokens the last time?
    private long timeLastGauntletTokensCheck = 0; // when did we check for gauntlet Tokens the last time?
    private long timeLastExpBadgesCheck = 0; // when did we check for badges the last time?
    private long timeLastInvBadgesCheck = 0; // when did we check for badges the last time?
    private long timeLastGVGBadgesCheck = 0; // when did we check for badges the last time?
    private long timeLastBountyCheck = 0; // when did we check for bounties the last time?
    private long timeLastBonusCheck = 0; // when did we check for bonuses (active consumables) the last time?
    long timeLastFishingBaitsCheck = 0; // when did we check for fishing baits the last time?
    private long timeLastFishingCheck = 0; // when did we check for fishing last time?
    private long timeLastDailyGem = 0; // when did we check for daily gem screenshot last time?
    private long timeLastWeeklyGem = Misc.getTime(); // when did we check for weekly gem screenshot last time?

    BHBot bot;
    AutoShrineManager shrineManager;
    AutoReviveManager reviveManager;
    AutoRuneManager runeManager;
    EncounterManager encounterManager;
    DungeonPositionChecker positionChecker;

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
            ArrayList<CueManager.CueDetails> famDetails = CueManager.getCueDetailsFromPath(cuesPath);

            for (CueManager.CueDetails details : famDetails) {
                uniqueFamiliars.add(details.name.toLowerCase());
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
        bot.restart(emergency, false); // assume emergency restart
    }

    public void run() {
        BHBot.logger.info("Bot started successfully!");

        restart(false);

        while (!bot.finished && bot.running) {
            bot.scheduler.backupIdleTime();
            try {
                bot.scheduler.process();
                if (bot.scheduler.isPaused()) {
                    Misc.sleep(500);
                    continue;
                }

                // If the current scheduling is no longer valid, as soon as we get in state Main we break so that the
                // Main Thread can switch to a new valid scheduling without interrupting adventures
                if (bot.currentScheduling != null && !bot.currentScheduling.isActive() && BHBot.State.Main.equals(bot.getState())) {
                    BHBot.logger.debug("Inactive scheduling detected in DungeonThread.");
                    break;
                }

                if (Misc.getTime() - bot.scheduler.getIdleTime() > MAX_IDLE_TIME) {
                    BHBot.logger.warn("Idle time exceeded... perhaps caught in a loop? Restarting... (state=" + bot.getState() + ")");
                    bot.saveGameScreen("idle-timeout-error", "errors");

                    // Safety measure to avoid being stuck forever in dungeons
                    if (bot.getState() != BHBot.State.Main && bot.getState() != BHBot.State.Loading) {
                        if (!bot.settings.autoRuneDefault.isEmpty()) {
                            BHBot.logger.info("Re-validating autoRunes");
                            if (!runeManager.detectEquippedMinorRunes(true, true)) {
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
                        bot.setState(bot.getLastJoinedState()); // we are not sure what type of dungeon we are doing
                        BHBot.logger.warn("Possible dungeon crash, activating failsafe");
                        bot.saveGameScreen("dungeon-crash-failsafe", "errors");
                        shrineManager.updateShrineSettings(false, false); //in case we are stuck in a dungeon lets enable shrines/boss
                        continue;
                    }
                }

                if (BHBot.State.RerunRaid.equals(bot.getState())) {
                    // set up autoRune and autoShrine
                    handleAdventureConfiguration(BHBot.State.Raid, false, null);
                    runeManager.reset();

                    // We change the state only after we performed all the configurations
                    bot.setState(BHBot.State.Raid);
                    bot.setLastJoinedState(BHBot.State.Raid);
                    BHBot.logger.info("Raid rerun initiated!");
                    setAutoOn(Misc.Durations.SECOND);
                }

                // process dungeons of any kind (if we are in any):
                if (bot.getState() == BHBot.State.Raid || bot.getState() == BHBot.State.Trials || bot.getState() == BHBot.State.Gauntlet || bot.getState() == BHBot.State.Dungeon || bot.getState() == BHBot.State.PVP || bot.getState() == BHBot.State.GVG || bot.getState() == BHBot.State.Invasion || bot.getState() == BHBot.State.Expedition || bot.getState() == BHBot.State.WorldBoss) {
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
                        if ((Misc.getTime() - timeLastWeeklyGem) > Misc.Durations.WEEK) {
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
                    runeManager.initialize();


                    String currentActivity = activitySelector(); //else select the activity to attempt
                    if (currentActivity != null) {
                        BHBot.logger.debug("Checking activity: " + currentActivity);

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

                                // set up autoRune and autoShrine
                                handleAdventureConfiguration(BHBot.State.Raid, true, Bounds.fromWidthHeight(600, 80, 80, 80));

                                bot.browser.readScreen(Misc.Durations.SECOND);
                                bot.browser.clickOnSeg(raidBTNSeg);

                                Settings.AdventureSetting raidSetting = decideAdventureRandomly(bot.settings.raids);
                                if (raidSetting == null) {
                                    bot.settings.activitiesEnabled.remove("r");
                                    BHBot.logger.error("It was impossible to choose a raid randomly, raids are disabled!");
                                    bot.notificationManager.sendErrorNotification("Raid Error", "It was impossible to choose a raid randomly, raids are disabled!");

                                    continue;
                                }

                                // We only rerun if round robin is disabled and rerun is enabled for current configuration
                                rerunCurrentActivity = raidSetting.rerun && !bot.settings.activitiesRoundRobin;

                                if (!handleRaidSelection(raidSetting.adventureZone, raidSetting.difficulty)) {
                                    restart();
                                    continue;
                                }

                                seg = MarvinSegment.fromCue(BHBot.cues.get("RaidSummon"), 3 * Misc.Durations.SECOND, bot.browser);
                                if (seg == null) {
                                    BHBot.logger.error("Raid Summon button not found");
                                    restart();
                                    continue;
                                }
                                bot.browser.clickOnSeg(seg);

                                // dismiss character dialog if it pops up:
                                bot.browser.readScreen();
                                detectCharacterDialogAndHandleIt();

                                Cue raidDifficultyCue;
                                switch (raidSetting.difficulty) {
                                    case 1:
                                        raidDifficultyCue = new Cue(BHBot.cues.get("Normal"), null);
                                        break;
                                    case 2:
                                        raidDifficultyCue = new Cue(BHBot.cues.get("Hard"), null);
                                        break;
                                    case 3:
                                    default:
                                        raidDifficultyCue = new Cue(BHBot.cues.get("Heroic"), Bounds.fromWidthHeight(535, 225, 110, 35));
                                        break;
                                }

                                seg = MarvinSegment.fromCue(raidDifficultyCue, Misc.Durations.SECOND * 3, bot.browser);
                                bot.browser.clickOnSeg(seg);

                                //team selection screen
                                /* Solo-for-bounty code */
                                if (raidSetting.solo) { //if the level is soloable then clear the team to complete bounties
                                    bot.browser.readScreen(Misc.Durations.SECOND);
                                    seg = MarvinSegment.fromCue(BHBot.cues.get("Clear"), Misc.Durations.SECOND * 2, Bounds.fromWidthHeight(310, 440, 110, 50), bot.browser);
                                    if (seg != null) {
                                        BHBot.logger.info("Attempting solo as per selected raid setting....");
                                        bot.browser.clickOnSeg(seg);
                                    } else {
                                        BHBot.logger.error("Impossible to find clear button in Dungeon Team!");
                                        restart();
                                        continue;
                                    }
                                }

                                Cue AcceptBounds = new Cue(BHBot.cues.get("Accept"), Bounds.fromWidthHeight(465, 445, 110, 40));
                                bot.browser.closePopupSecurely(AcceptBounds, AcceptBounds);

                                if (raidSetting.solo) {
                                    Cue YesGreenBounds = new Cue(BHBot.cues.get("YesGreen"), Bounds.fromWidthHeight(290, 340, 70, 45));
                                    seg = MarvinSegment.fromCue(YesGreenBounds, 4 * Misc.Durations.SECOND, bot.browser);
                                    if (seg != null) {
                                        bot.browser.clickOnSeg(seg);
                                    } else {
                                        BHBot.logger.error("Impossible to find Yes button in Raid Team!");
                                        restart();
                                    }
                                } else {
                                    if (handleTeamMalformedWarning()) {
                                        BHBot.logger.error("Team incomplete, doing emergency restart..");
                                        restart();
                                        continue;
                                    }
                                }

                                bot.setState(BHBot.State.Raid);
                                bot.setLastJoinedState(BHBot.State.Raid);
                                BHBot.logger.info("Raid initiated!");
                                runeManager.reset();

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
                                seg = MarvinSegment.fromCue(BHBot.cues.get("Gauntlet2"), 0, Bounds.fromWidthHeight(735, 235, 55, 60), bot.browser);
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

                                // set up autoRune and autoShrine
                                handleAdventureConfiguration(trials ? BHBot.State.Trials : BHBot.State.Gauntlet, true, null);

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
                                    int result = selectDifficulty(difficulty, targetDifficulty, BHBot.cues.get("SelectDifficulty"), 1, true);
                                    if (result == 0) { // error!
                                        // see if drop down menu is still open and close it:
                                        bot.browser.readScreen(Misc.Durations.SECOND);
                                        tryClosingWindow(BHBot.cues.get("DifficultyDropDown"));
                                        bot.browser.readScreen(5 * Misc.Durations.SECOND);
                                        BHBot.logger.warn("Unable to change difficulty, usually because desired level is not unlocked. Running " + (trials ? "trials" : "gauntlet") + " at " + difficulty + ".");
                                        bot.notificationManager.sendErrorNotification("T/G Error", "Unable to change " + (trials ? "trials" : "gauntlet") + " difficulty to : " + targetDifficulty + " Running: " + difficulty + " instead.");

                                        // We update the setting file with the old difficulty level
                                        String settingName = trials ? "difficultyTrials" : "difficultyGauntlet";
                                        String original = settingName + " " + targetDifficulty;
                                        String updated = settingName + " " + difficulty;
                                        settingsUpdate(original, updated);

                                    } else if (result != targetDifficulty) {
                                        BHBot.logger.warn(targetDifficulty + " is not available in " + (trials ? "trials" : "gauntlet") + " difficulty selection. Closest match is " + result + ".");

                                        // We update the setting file with the old difficulty level
                                        String settingName = trials ? "difficultyTrials" : "difficultyGauntlet";
                                        String original = settingName + " " + targetDifficulty;
                                        String updated = settingName + " " + result;
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

                                Boolean notEnoughTokens = handleNotEnoughTokensPopup(false);
                                if (notEnoughTokens != null) {
                                    if (notEnoughTokens) {
                                        continue;
                                    }
                                } else {
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
                                notEnoughTokens = handleNotEnoughTokensPopup(true);
                                if (notEnoughTokens != null) {
                                    if (notEnoughTokens) {
                                        continue;
                                    }
                                } else {
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
                                    bot.setLastJoinedState(trials ? BHBot.State.Trials : BHBot.State.Gauntlet);
                                    BHBot.logger.info((trials ? "Trials" : "Gauntlet") + " initiated!");
                                    runeManager.reset();
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

                                // set up autoRune and autoShrine
                                handleAdventureConfiguration(BHBot.State.Dungeon, false, null);

                                seg = MarvinSegment.fromCue(BHBot.cues.get("Quest"), Misc.Durations.SECOND * 3, bot.browser);
                                if (seg == null) {
                                    bot.saveGameScreen("no-quest-btn", "errors", bot.browser.getImg());
                                    BHBot.logger.error("Impposible to find the quest button!");
                                    continue;
                                }

                                bot.browser.clickOnSeg(seg);
                                bot.browser.readScreen(5 * Misc.Durations.SECOND);

                                Settings.AdventureSetting dungeonSetting = decideAdventureRandomly(bot.settings.dungeons);
                                if (dungeonSetting == null) {
                                    bot.settings.activitiesEnabled.remove("d");
                                    BHBot.logger.error("It was impossible to choose a dungeon randomly, dungeons are disabled!");
                                    bot.notificationManager.sendErrorNotification("Dungeon error", "It was impossible to choose a dungeon randomly, dungeons are disabled!");
                                    continue;
                                }

                                Matcher dungeonMatcher = dungeonRegex.matcher(dungeonSetting.adventureZone.toLowerCase());
                                if (!dungeonMatcher.find()) {
                                    BHBot.logger.error("Wrong format in dungeon detected: '" + dungeonSetting.adventureZone + "'! It will be skipped...");
                                    bot.notificationManager.sendErrorNotification("Dungeon error", "Wrong dungeon format detected: " + dungeonSetting.adventureZone);
                                    // TODO close the dungeon window
                                    continue;
                                }

                                int goalZone = Integer.parseInt(dungeonMatcher.group("zone"));
                                int goalDungeon = Integer.parseInt(dungeonMatcher.group("dungeon"));

                                String difficultyName = (dungeonSetting.difficulty == 1 ? "Normal" : dungeonSetting.difficulty == 2 ? "Hard" : "Heroic");

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
                                    seg = MarvinSegment.fromCue(BHBot.cues.get(dungeonSetting.difficulty == 1 ? "Normal" : dungeonSetting.difficulty == 2 ? "Hard" : "Heroic"), 5 * Misc.Durations.SECOND, bot.browser);
                                }
                                bot.browser.clickOnSeg(seg);

                                //team selection screen
                                /* Solo-for-bounty code */
                                if (dungeonSetting.solo) { //if the level is soloable then clear the team to complete bounties
                                    bot.browser.readScreen(Misc.Durations.SECOND);
                                    seg = MarvinSegment.fromCue(BHBot.cues.get("Clear"), Misc.Durations.SECOND * 2, bot.browser);
                                    if (seg != null) {
                                        BHBot.logger.info("Attempting solo as per selected dungeon setting....");
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

                                if (dungeonSetting.solo) {
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

                                if (handleNotEnoughEnergyPopup()) {
                                    continue;
                                }

                                bot.setState(BHBot.State.Dungeon);
                                bot.setLastJoinedState(BHBot.State.Dungeon);
                                runeManager.reset();

                                BHBot.logger.info("Dungeon <z" + goalZone + "d" + goalDungeon + "> " + (dungeonSetting.difficulty == 1 ? "normal" : dungeonSetting.difficulty == 2 ? "hard" : "heroic") + " initiated!");
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
                                handleAdventureConfiguration(BHBot.State.PVP, false, null);

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
                                    bot.setLastJoinedState(BHBot.State.PVP);
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

                                    // set up autoRune and autoShrine
                                    handleAdventureConfiguration(BHBot.State.GVG, true, null);
                                    bot.browser.readScreen(Misc.Durations.SECOND);
                                    bot.browser.clickOnSeg(badgeBtn);

                                    BHBot.logger.info("Attempting GVG...");

                                    if (bot.settings.gvgstrip.size() > 0) {
                                        // If we need to strip down for GVG, we need to close the GVG gump and open it again
                                        bot.browser.closePopupSecurely(BHBot.cues.get("GVGWindow"), BHBot.cues.get("X"));
                                        stripDown(bot.settings.gvgstrip);
                                        bot.browser.clickOnSeg(badgeBtn);
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
                                        bot.setLastJoinedState(BHBot.State.GVG);
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

                                    // set up autoRune and autoShrine
                                    handleAdventureConfiguration(BHBot.State.Invasion, true, null);
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
                                        bot.setLastJoinedState(BHBot.State.Invasion);
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

                                    // set up autoRune and autoShrine
                                    handleAdventureConfiguration(BHBot.State.Expedition, true, null);

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
                                    BHBot.logger.info("Attempting " + targetPortal + " " + portalName + " Portal at difficulty " + targetDifficulty);

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
                                        int result = selectDifficulty(difficulty, targetDifficulty, BHBot.cues.get("SelectDifficultyExpedition"), 5, false);
                                        if (result == 0) { // error!
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

                                        } else if (result != targetDifficulty) {
                                            BHBot.logger.warn(targetDifficulty + " is not available. Running Expedition at the closest match " + result + ".");

                                            // We update the file with the old difficulty level
                                            String original = expeditionFailsafePortal + " " + targetDifficulty;
                                            String updated = expeditionFailsafePortal + " " + result;
                                            settingsUpdate(original, updated);
                                        }
                                    }

                                    //click enter
                                    seg = MarvinSegment.fromCue(BHBot.cues.get("Enter"), 2 * Misc.Durations.SECOND, bot.browser);
                                    bot.browser.clickOnSeg(seg);

                                    //click enter
                                    Cue expeditionAccept = new Cue(BHBot.cues.get("Accept"), Bounds.fromWidthHeight(420, 430, 200, 65));

                                    seg = MarvinSegment.fromCue(expeditionAccept, 3 * Misc.Durations.SECOND, bot.browser);
                                    if (seg != null) {
                                        //bot.browser.clickOnSeg(seg);
                                        bot.browser.closePopupSecurely(expeditionAccept, expeditionAccept);
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
                                        bot.setLastJoinedState(BHBot.State.Expedition);
                                        BHBot.logger.info(portalName + " portal initiated!");
                                        runeManager.reset();
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
                            timeLastXealsCheck = Misc.getTime();

                            bot.browser.readScreen();
                            MarvinSegment wbBTNSeg = MarvinSegment.fromCue(BHBot.cues.get("WorldBoss"), bot.browser);
                            if (wbBTNSeg == null) {
                                bot.scheduler.resetIdleTime();
                                BHBot.logger.error("World Boss button not found");
                                continue;
                            }
                            bot.browser.clickOnSeg(wbBTNSeg);

                            bot.browser.readScreen();
                            detectCharacterDialogAndHandleIt(); //clear dialogue

                            seg = MarvinSegment.fromCue("WorldBossPopup", 5 * Misc.Durations.SECOND, bot.browser); // wait until the raid window opens
                            if (seg == null) {
                                BHBot.logger.warn("Error: attempt at opening world boss window failed. No window cue detected. Ignoring...");
                                bot.scheduler.restoreIdleTime();
                                // we make sure that everything that can be closed is actually closed to avoid idle timeout
                                bot.browser.closePopupSecurely(BHBot.cues.get("X"), BHBot.cues.get("X"));
                                continue;
                            }

                            int xeals = getXeals();
                            globalXeals = xeals;
                            BHBot.logger.readout("Xeals: " + xeals + ", required: >" + bot.settings.minXeals);

                            if (xeals == -1) { // error
                                if (bot.scheduler.doWorldBossImmediately)
                                    bot.scheduler.doWorldBossImmediately = false; // reset it
                                bot.scheduler.restoreIdleTime();
                                continue;
                            }

                            if ((xeals == 0) || (!bot.scheduler.doWorldBossImmediately && (xeals <= bot.settings.minXeals || bot.settings.worldBossSettings.size() == 0))) {
                                if (bot.scheduler.doWorldBossImmediately)
                                    bot.scheduler.doWorldBossImmediately = false; // reset it

                                int xealDifference = bot.settings.minXeals - xeals; //difference between needed and current resource
                                if (xealDifference > 1) {
                                    int increase = (xealDifference - 1) * 45;
                                    XEALS_CHECK_INTERVAL = increase * Misc.Durations.MINUTE; //add 45 minutes to the check interval for each xeal needed above 1
                                } else
                                    XEALS_CHECK_INTERVAL = 10 * Misc.Durations.MINUTE; //if we only need 1 check every 10 minutes
                                bot.browser.readScreen();
                                seg = MarvinSegment.fromCue(BHBot.cues.get("X"), Misc.Durations.SECOND, bot.browser);
                                bot.browser.clickOnSeg(seg);
                                Misc.sleep(Misc.Durations.SECOND);

                                continue;

                            } else {
                                // do the WorldBoss!
                                if (bot.scheduler.doWorldBossImmediately)
                                    bot.scheduler.doWorldBossImmediately = false; // reset it

                                Settings.WorldBossSetting wbSetting = bot.settings.worldBossSettings.next();
                                if (wbSetting == null) {
                                    BHBot.logger.error("No World Boss setting found! Disabling World Boss");
                                    bot.settings.activitiesEnabled.remove("w");
                                    seg = MarvinSegment.fromCue(BHBot.cues.get("X"), Misc.Durations.SECOND, bot.browser);
                                    bot.browser.clickOnSeg(seg);
                                    bot.browser.readScreen(Misc.Durations.SECOND);
                                    continue;
                                }

                                if (!checkWorldBossInput(wbSetting)) {
                                    BHBot.logger.warn("Invalid world boss settings detected, World Boss will be skipped");
                                    seg = MarvinSegment.fromCue(BHBot.cues.get("X"), Misc.Durations.SECOND, bot.browser);
                                    bot.browser.clickOnSeg(seg);
                                    bot.browser.readScreen(Misc.Durations.SECOND);
                                    continue;
                                }

                                // set up autoRune and autoShrine
                                handleAdventureConfiguration(BHBot.State.WorldBoss, true, null);

                                //We re-open the wb window
                                if (bot.settings.autoRune.containsKey("w")) {
                                    bot.browser.readScreen(Misc.Durations.SECOND);
                                    bot.browser.clickOnSeg(wbBTNSeg);
                                }

                                WorldBoss wbType = WorldBoss.fromLetter(String.valueOf(wbSetting.type));
                                if (wbType == null) {
                                    BHBot.logger.error("Unkwon World Boss type: " + wbSetting.type + ". Disabling World Boss");
                                    bot.settings.activitiesEnabled.remove("w");
                                    restart();
                                    continue;
                                }

                                //new settings loading
                                String worldBossDifficultyText = wbSetting.difficulty == 1 ? "Normal" : wbSetting.difficulty == 2 ? "Hard" : "Heroic";

                                if (!wbSetting.solo) {
                                    BHBot.logger.info("Attempting " + worldBossDifficultyText + " T" + wbSetting.tier + " " + wbType.getName() + ". Lobby timeout is " + Misc.millisToHumanForm((long) wbSetting.timer * 1000L) + ".");
                                } else {
                                    BHBot.logger.info("Attempting " + worldBossDifficultyText + " T" + wbSetting.tier + " " + wbType.getName() + " Solo");
                                }

                                bot.browser.readScreen();
                                seg = MarvinSegment.fromCue("DarkBlueSummon", Misc.Durations.SECOND, bot.browser);
                                if (seg != null) {
                                    bot.browser.clickOnSeg(seg);
                                } else {
                                    BHBot.logger.error("Impossible to find dark blue summon in world boss.");

                                    bot.saveGameScreen("wb-no-dark-blue-summon", "errors");
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

                                seg = MarvinSegment.fromCue("LargeDarkBlueSummon", 4 * Misc.Durations.SECOND, bot.browser);
                                bot.browser.clickOnSeg(seg); //selected world boss

                                seg = MarvinSegment.fromCue(BHBot.cues.get("Private"), Misc.Durations.SECOND * 3, bot.browser);
                                if (!wbSetting.solo) {
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
                                if (currentTier != wbSetting.tier) {
                                    BHBot.logger.info("T" + currentTier + " detected, changing to T" + wbSetting.tier);
                                    Misc.sleep(500);
                                    if (!changeWorldBossTier(wbSetting.tier)) {
                                        restart();
                                        continue;
                                    }
                                }

                                //world boss difficulty selection

                                int currentDifficulty = detectWorldBossDifficulty();
                                String currentDifficultyName = (currentDifficulty == 1 ? "Normal" : currentDifficulty == 2 ? "Hard" : "Heroic");
                                String settingsDifficultyName = (wbSetting.difficulty == 1 ? "Normal" : wbSetting.difficulty == 2 ? "Hard" : "Heroic");
                                if (currentDifficulty != wbSetting.difficulty) {
                                    BHBot.logger.info(currentDifficultyName + " detected, changing to " + settingsDifficultyName);
                                    changeWorldBossDifficulty(wbSetting.difficulty);
                                }

                                seg = MarvinSegment.fromCue("SmallDarkBlueSummon", Misc.Durations.SECOND * 3, bot.browser);
                                bot.browser.clickOnSeg(seg); //accept current settings

                                boolean insufficientXeals = handleNotEnoughXealsPopup();
                                if (insufficientXeals) {
                                    continue;
                                }

                                BHBot.logger.info("Starting lobby: " + wbType.getName() + " has " + wbType.getPartySize() + " party members");

                                //wait for lobby to fill with a timer
                                if (!wbSetting.solo) {
                                    // How many invites do we expect for this WB?
                                    int inviteCnt = wbType.getPartySize() - 1;

                                    // Invite and unready buttons bounds are dinamically calculated based on the WB party member
                                    Bounds inviteBounds = Bounds.fromWidthHeight(330, 217, 127, 54 * inviteCnt);
                                    Bounds unreadyBounds = Bounds.fromWidthHeight(177, 217, 24, 54 * inviteCnt);
                                    Bounds totalWBTS = Bounds.fromWidthHeight(604, 70, 81, 25);

                                    // we assume we did not start the WB
                                    boolean lobbyTimeout = true;

                                    // Timings
                                    long startTime = Misc.getTime();
                                    long cutOffTime = startTime + (wbSetting.timer * Misc.Durations.SECOND);
                                    long nextUpdateTime = startTime + (15 * Misc.Durations.SECOND);

                                    // Temporary string used to make sure we don't save 10000000s of screenshots when debugWBTS is enabled
                                    String lastSavedName = "";

                                    // this is long running loop and we want to be sure that it is interrupted when the bot needs to quit
                                    cutOffLoop:
                                    while (Misc.getTime() < cutOffTime && bot.running && !bot.finished) {

                                        // When a puse command is issued, we get out of the WB lobby
                                        if (bot.scheduler.isPaused()) {
                                            BHBot.logger.info("Pause detected, exiting from World Boss loby.");
                                            break;
                                        }

                                        // we make sure to update the screen image as FindSubimage.findSubimage is using a static image
                                        // at the same time we also wait 500ms so to easu CPU consumption
                                        bot.browser.readScreen(500);

                                        // Array used to save party members TS
                                        int[] playersTS = new int[inviteCnt];

                                        // We read the current total TS
                                        int totalTS = 0;
                                        if (wbSetting.minimumTotalTS > 0) {
                                            MarvinImage totalTSImg = new MarvinImage(bot.browser.getImg().getSubimage(totalWBTS.x1, totalWBTS.y1, totalWBTS.width, totalWBTS.height));
                                            totalTSImg.toBlackWhite(new Color(25, 25, 25), new Color(255, 255, 255), 254);
                                            BufferedImage totalTSSubImg = totalTSImg.getBufferedImage();
                                            totalTS = readNumFromImg(totalTSSubImg, "wb_total_ts_", new HashSet<>());

                                            // If readNumFromImg has errors it will return 0, so we make sure this is not the case
                                            if (totalTS > 0 && totalTS >= wbSetting.minimumTotalTS) {

                                                // We need to check that the current party members are ready
                                                List<MarvinSegment> unreadySegs = FindSubimage.findSubimage(bot.browser.getImg(), BHBot.cues.get("Unready").im, 1.0, true, false, unreadyBounds.x1, unreadyBounds.y1, unreadyBounds.x2, unreadyBounds.y2);

                                                if (unreadySegs.isEmpty()) {
                                                    BHBot.logger.info("TS for lobby is " + totalTS + ". " + wbSetting.minimumTotalTS + " requirement reached in " + Misc.millisToHumanForm(Misc.getTime() - startTime));
                                                    lobbyTimeout = false;
                                                    saveDebugWBTSScreen(totalTS, playersTS, lastSavedName);
                                                    break;
                                                } else {
                                                    continue;
                                                }
                                            }

                                        }

                                        List<MarvinSegment> inviteSegs = FindSubimage.findSubimage(bot.browser.getImg(), BHBot.cues.get("Invite").im, 1.0, true, false, inviteBounds.x1, inviteBounds.y1, inviteBounds.x2, inviteBounds.y2);
                                        // At least one person joined the lobby
                                        if (inviteSegs.size() < inviteCnt) {
                                            Bounds TSBound = Bounds.fromWidthHeight(184, 241, 84, 18);

                                            if (wbSetting.minimumPlayerTS > 0) {
                                                for (int partyMemberPos = 0; partyMemberPos < inviteCnt; partyMemberPos++) {
                                                    MarvinImage subImg = new MarvinImage(bot.browser.getImg().getSubimage(TSBound.x1, TSBound.y1 + (54 * partyMemberPos), TSBound.width, TSBound.height));
                                                    subImg.toBlackWhite(new Color(20, 20, 20), new Color(203, 203, 203), 203);
                                                    BufferedImage tsSubImg = subImg.getBufferedImage();

                                                    int playerTS = readNumFromImg(tsSubImg, "wb_player_ts_", new HashSet<>());
                                                    playersTS[partyMemberPos] = playerTS;

                                                    if (playerTS < 1) {
                                                        // Player position one is you, the first party member is position two
                                                        BHBot.logger.trace("It was impossible to read WB player TS for player position " + partyMemberPos + 2);
                                                    } else {
                                                        if (playerTS < wbSetting.minimumPlayerTS) {
                                                            BHBot.logger.info("Player " + (partyMemberPos + 2) + " TS is lower than required minimum: " + playerTS + "/" + wbSetting.minimumPlayerTS);

                                                            // We kick the player if we need to
                                                            Bounds kickBounds = Bounds.fromWidthHeight(411, 220 + (54 * partyMemberPos), 43, 42);
                                                            seg = MarvinSegment.fromCue("WorldBossPlayerKick", 2 * Misc.Durations.SECOND, kickBounds, bot.browser);
                                                            if (seg == null) {
                                                                BHBot.logger.error("Impossible to find kick button for party member " + (partyMemberPos + 2) + ".");
                                                                continue cutOffLoop;
                                                            } else {
                                                                bot.browser.clickOnSeg(seg);
                                                                seg = MarvinSegment.fromCue("WorldBossPopupKick", 5 * Misc.Durations.SECOND, bot.browser);
                                                                if (seg == null) {
                                                                    bot.saveGameScreen("wb-no-player-kick", "wb-ts-error");
                                                                    BHBot.logger.error("Impossible to find player kick confirm popup");
                                                                    restart();
                                                                    break cutOffLoop;
                                                                } else {
                                                                    bot.browser.closePopupSecurely(BHBot.cues.get("WorldBossPopupKick"), new Cue(BHBot.cues.get("YesGreen"), Bounds.fromWidthHeight(260, 340, 130, 40)));
                                                                }
                                                            }
                                                            continue cutOffLoop;
                                                        }
                                                    }

                                                }
                                            }
                                        }

                                        if (inviteSegs.isEmpty()) {
                                            List<MarvinSegment> unreadySegs = FindSubimage.findSubimage(bot.browser.getImg(), BHBot.cues.get("Unready").im, 1.0, true, false, unreadyBounds.x1, unreadyBounds.y1, unreadyBounds.x2, unreadyBounds.y2);

                                            if (unreadySegs.isEmpty()) {
                                                BHBot.logger.info("Lobby filled and ready in " + Misc.millisToHumanForm(Misc.getTime() - startTime));
                                                lobbyTimeout = false;
                                                saveDebugWBTSScreen(totalTS, playersTS, lastSavedName);
                                                break;
                                            }
                                        }

                                        if (Misc.getTime() >= nextUpdateTime) {
                                            if (totalTS > 0) {
                                                BHBot.logger.debug("Total lobby TS is " + totalTS);
                                            }
                                            BHBot.logger.info("Waiting for full ready team. Time out in " + Misc.millisToHumanForm(cutOffTime - Misc.getTime()));
                                            nextUpdateTime = Misc.getTime() + (15 * Misc.Durations.SECOND);
                                            bot.scheduler.resetIdleTime(true);
                                            lastSavedName = saveDebugWBTSScreen(totalTS, playersTS, lastSavedName);
                                        }
                                    }

                                    if (lobbyTimeout) {
                                        BHBot.logger.info("Lobby timed out, returning to main screen.");
                                        // we say we checked (interval - 1) minutes ago, so we check again in a minute
                                        timeLastXealsCheck = Misc.getTime() - ((XEALS_CHECK_INTERVAL) - Misc.Durations.MINUTE);
                                        closeWorldBoss();
                                    } else {
                                        bot.browser.readScreen();
                                        MarvinSegment segStart = MarvinSegment.fromCue(BHBot.cues.get("DarkBlueStart"), 5 * Misc.Durations.SECOND, bot.browser);
                                        if (segStart != null) {
                                            while (segStart != null) {
                                                bot.browser.closePopupSecurely(BHBot.cues.get("DarkBlueStart"), BHBot.cues.get("DarkBlueStart")); //start World Boss
                                                bot.browser.readScreen();
                                                seg = MarvinSegment.fromCue(BHBot.cues.get("TeamNotFull"), 2 * Misc.Durations.SECOND, bot.browser); //check if we have the team not full screen an clear it
                                                if (seg != null) {
                                                    bot.browser.readScreen(2 * Misc.Durations.SECOND); //wait for animation to finish
                                                    bot.browser.clickInGame(330, 360); //yesgreen cue has issues so we use XY to click on Yes
                                                }

                                                segStart = MarvinSegment.fromCue(BHBot.cues.get("DarkBlueStart"), 5 * Misc.Durations.SECOND, bot.browser);
                                            }
                                            BHBot.logger.info(worldBossDifficultyText + " T" + wbSetting.tier + " " + wbType.getName() + " started!");
                                            bot.setState(BHBot.State.WorldBoss);
                                            bot.setLastJoinedState(BHBot.State.WorldBoss);
                                        } else { //generic error / unknown action restart
                                            BHBot.logger.error("Something went wrong while attempting to start the World Boss, restarting");
                                            bot.saveGameScreen("wb-no-start-button", "errors");
                                            restart();
                                        }
                                    }
                                } else {
                                    bot.browser.readScreen();
                                    MarvinSegment segStart = MarvinSegment.fromCue(BHBot.cues.get("DarkBlueStart"), 5 * Misc.Durations.SECOND, bot.browser);
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
                                        BHBot.logger.info(worldBossDifficultyText + " T" + wbSetting.tier + " " + wbType.getName() + " Solo started!");
                                        bot.setState(BHBot.State.WorldBoss);
                                        bot.setLastJoinedState(BHBot.State.WorldBoss);
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

                            // handleFishingBaits() changes the State to FishingBaits
                            bot.setState(BHBot.State.Main);
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

                                // handleFishingBaits() changes the State to FishingBaits
                                bot.setState(BHBot.State.Main);
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

                    } else {
                        // If we don't have any activity to perform, we reset the idle timer check
                        bot.scheduler.resetIdleTime(true);
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
            if (!bot.running && BHBot.State.Main.equals(bot.getState())) {
                break;
            }

            BHBot.logger.trace("Dungeon Thread Sleeping");
            if (BHBot.State.Main.equals(bot.getState()) || BHBot.State.Loading.equals(bot.getState())) {
                Misc.sleep(500);
            } else {
                // While we are in a dungeon we want a faster main loop
                Misc.sleep(50);
            }
        } // main while loop

        BHBot.logger.info("Dungeon thread stopped.");
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
                } else if ("w".equals(activity) && ((Misc.getTime() - timeLastXealsCheck) > XEALS_CHECK_INTERVAL)) {
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
     * Returns number of xeals that we have. Works only if wb popup is open. Returns -1 in case it cannot read number of shards for some reason.
     */
    private int getXeals() {
        MarvinSegment seg;

        seg = MarvinSegment.fromCue("WorldBossPopup", bot.browser);

        if (seg == null) // this should probably not happen
            return -1;

        int left = seg.x2 + 1;
        int top = seg.y1 + 9;

        final Color full = new Color(12, 137, 255);

        int value = 0;
        int maxXeals = bot.settings.maxXeals;

        for (int i = 0; i < 76; i++) {
            value = i;
            Color col = new Color(bot.browser.getImg().getRGB(left + i, top));

            if (!col.equals(full))
                break;
        }

        value = value + 2; //add the last 2 pixels to get an accurate count
        return Math.round(value * (maxXeals / 75.0f)); // round to nearest whole number
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
            positionChecker.resetStartPos();
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
                positionChecker.resetStartPos();
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
            runeManager.handleAutoBossRune(outOfEncounterTimestamp, inEncounterTimestamp);
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
        if ((bot.getState() == BHBot.State.Raid || bot.getState() == BHBot.State.Dungeon) && encounterStatus) {
            seg = MarvinSegment.fromCue(BHBot.cues.get("Persuade"), bot.browser);
            if (seg != null) {
                encounterManager.processFamiliarEncounter();
            }
        }

        /*
         *  Skeleton key code
         *  encounterStatus is set to true as the window obscures the guild icon
         */
        seg = MarvinSegment.fromCue(BHBot.cues.get("SkeletonTreasure"), bot.browser);
        if (seg != null) {
            if (handleSkeletonKey()) {
                restart();
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
        if (bot.getState() == BHBot.State.Raid || bot.getState() == BHBot.State.Dungeon
                || bot.getState() == BHBot.State.Expedition|| bot.getState() == BHBot.State.Trials) {
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

                resetAppropriateTimers();
                reviveManager.reset();

                if (BHBot.State.Raid.equals(bot.getState()) && rerunCurrentActivity ) {
                    setAutoOff(1000);

                    Cue raidRerun = new Cue(BHBot.cues.get("Rerun"), Bounds.fromWidthHeight(425, 345, 95, 35));

                    bot.browser.closePopupSecurely(BHBot.cues.get("Cleared"), raidRerun);

                    // We are out of shards, so we get back to Main
                    seg = MarvinSegment.fromCue("NotEnoughShards", Misc.Durations.SECOND * 3, bot.browser);
                    if (seg != null) {

                        bot.browser.closePopupSecurely(BHBot.cues.get("NotEnoughShards"), BHBot.cues.get("No"));

                        rerunCurrentActivity = false;
                        bot.setState(BHBot.State.Main);
                        return;
                    }

                    bot.setState(BHBot.State.RerunRaid);
                } else {

                    //close 'cleared' popup
                    Bounds yesGreen = null;
                    if (BHBot.State.Raid.equals(bot.getState())) {
                        yesGreen = Bounds.fromWidthHeight(290, 345, 70, 45);
                    }

                    bot.browser.closePopupSecurely(BHBot.cues.get("Cleared"), new Cue(BHBot.cues.get("YesGreen"), yesGreen));

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

                    bot.setState(BHBot.State.Main); // reset state
                }

                return;
            }
        }

        /*
         *  Check for the 'Victory' screen and handle post-activity tasks
         */
        if (bot.getState() == BHBot.State.WorldBoss || bot.getState() == BHBot.State.Gauntlet
                || bot.getState() == BHBot.State.Invasion|| bot.getState() == BHBot.State.PVP
                || bot.getState() == BHBot.State.GVG) {

            if (bot.getState() == BHBot.State.Gauntlet || bot.getState() == BHBot.State.GVG) {
                seg = MarvinSegment.fromCue(BHBot.cues.get("VictorySmall"), bot.browser);
            } else {
                seg = MarvinSegment.fromCue(BHBot.cues.get("VictoryLarge"), bot.browser);
            }
            if (seg != null) {

                Bounds closeBounds;
                switch (bot.getState()) {
                    case Gauntlet:
                        closeBounds = Bounds.fromWidthHeight(320, 420, 160, 65);
                        break;
                    case WorldBoss:
                        closeBounds = Bounds.fromWidthHeight(425, 425, 100, 45);
                        break;
                    default:
                        closeBounds = null;
                        break;
                }

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

                Cue XWithBounds;
                //noinspection SwitchStatementWithTooFewBranches
                switch (bot.getState()) {
                    case WorldBoss:
                        XWithBounds = new Cue(BHBot.cues.get("X"), Bounds.fromWidthHeight(640, 75, 60, 60));
                        break;
                    default:
                        XWithBounds = new Cue(BHBot.cues.get("X"), null);
                        break;
                }

                bot.browser.closePopupSecurely(XWithBounds, BHBot.cues.get("X"));

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
                subm.toBlackWhite(new Color(25, 25, 25), new Color(255, 255, 255), 64);
                BufferedImage subimagetestbw = subm.getBufferedImage();
                int num = readNumFromImg(subimagetestbw, "small", new HashSet<>());
                BHBot.logger.info(bot.getState().getName() + " #" + counters.get(bot.getState()).getTotal() + " completed. Level reached: " + num);
                BHBot.logger.stats("Run time: " + runtime + ". Average: " + runtimeAvg + ".");
            }

            //check for invasion loot drops and send via Pushover/Screenshot
            if (bot.getState() == BHBot.State.Invasion) {
                handleLoot();
            }

            // Difficulty failsafe logic
            if (bot.getState().equals(BHBot.State.Expedition) &&  bot.settings.difficultyFailsafe.containsKey("e")) {
                //Handle difficultyFailsafe for Expedition
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
            } else if (bot.getState().equals(BHBot.State.Trials) && bot.settings.difficultyFailsafe.containsKey("t")) {
                // Difficulty failsafe for trials
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
            } else if (bot.getState().equals(BHBot.State.Gauntlet) && bot.settings.difficultyFailsafe.containsKey("g")) {
                // Difficulty failsafe for Gauntlet
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

            bot.saveGameScreen("defeat-pop-up-" + bot.getState(), "debug", bot.browser.getImg());

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

            if (bot.getState() != BHBot.State.Expedition) {
                //Close the activity window to return us to the main screen
                bot.browser.readScreen(3 * Misc.Durations.SECOND); //wait for slide-in animation to finish
                bot.browser.closePopupSecurely(BHBot.cues.get("X"), BHBot.cues.get("X"));
            } else {
                //For Expedition we need to close 3 windows (Exped/Portal/Team) to return to main screen
                bot.browser.closePopupSecurely(BHBot.cues.get("Enter"), BHBot.cues.get("X"));
                bot.browser.closePopupSecurely(BHBot.cues.get("PortalBorderLeaves"), BHBot.cues.get("X"));
                bot.browser.closePopupSecurely(BHBot.cues.get("BadgeBar"), BHBot.cues.get("X"));
            }

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

                runeManager.reset();
                bot.browser.readScreen(Misc.Durations.SECOND * 2);
            }

            bot.setState(BHBot.State.Main); // reset state
            return;
        }

        // at the end of processDungeon, we revert idle time change (in order for idle detection to function properly):
        bot.scheduler.restoreIdleTime();
    }


    /**
     * This method will take care of handling treasure chests found in raid and dungeons
     *
     * @return true if anny error happens, false on success
     */
    private boolean handleSkeletonKey() {
        MarvinSegment seg;

        // Let's check if we have skeleton keys or not
        seg = MarvinSegment.fromCue(BHBot.cues.get("SkeletonNoKeys"), 2 * Misc.Durations.SECOND, bot.browser);

        final String declineMessage = seg != null ? "No skeleton keys, skipping.." : "Skeleton treasure found, declining.";
        final String acceptMessage = bot.settings.openSkeleton == 1 ? "Skeleton treasure found, attempting to use key" : "Raid Skeleton treasure found, attempting to use key";

        Bounds declineBounds = Bounds.fromWidthHeight(400, 360, 150, 65);
        Bounds greenYesBounds = Bounds.fromWidthHeight(245, 335, 155, 55);
        Bounds openBounds = Bounds.fromWidthHeight(250, 360, 150, 65);

        // we don't have skeleton keys or setting does not allow us to open chests
        if (seg != null || bot.settings.openSkeleton == 0) {
            BHBot.logger.info(declineMessage);
            seg = MarvinSegment.fromCue(BHBot.cues.get("Decline"), 5 * Misc.Durations.SECOND, declineBounds, bot.browser);
            if (seg != null) {
                bot.browser.clickOnSeg(seg);
                seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), 5 * Misc.Durations.SECOND, greenYesBounds, bot.browser);
                if (seg != null) {
                    bot.browser.clickOnSeg(seg);
                } else {
                    bot.saveGameScreen("treasure-decline-no-yes", "errors", bot.browser.getImg());
                    BHBot.logger.error("Impossible to find yes button after decline in handleSkeletonKey");
                    bot.notificationManager.sendErrorNotification("Treasure chest error", "Skeleton Chest gump without YES button");
                    return true;
                }
            } else {
                bot.saveGameScreen("treasure-no-decline", "errors", bot.browser.getImg());
                BHBot.logger.error("Impossible to find Decline button in handleSkeletonKey");
                bot.notificationManager.sendErrorNotification("Treasure chest error", "Skeleton Chest gump without DECLINE button");
                return true;
            }
            return false;
        } else if (bot.settings.openSkeleton == 1 || (bot.settings.openSkeleton == 2 && bot.getState() == BHBot.State.Raid)) {
            // Open all & Raid only
            BHBot.logger.info(acceptMessage);
            seg = MarvinSegment.fromCue(BHBot.cues.get("Open"), 5 * Misc.Durations.SECOND, openBounds, bot.browser);
            if (seg != null) {
                bot.browser.clickOnSeg(seg);
                seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), 5 * Misc.Durations.SECOND, greenYesBounds, bot.browser);
                if (seg != null) {
                    bot.browser.clickOnSeg(seg);
                    if ((bot.settings.screenshots.contains("s"))) {
                        bot.saveGameScreen("skeleton-contents", "rewards");
                    }
                    return false;
                } else {
                    BHBot.logger.error("Impossible to find yes button after open in handleSkeletonKey");
                    bot.saveGameScreen("treasure-open no-yes", "errors");
                    bot.notificationManager.sendErrorNotification("Treasure chest error", "Skeleton Chest gump without YES button");
                    return true;
                }
            } else {
                BHBot.logger.error("Open button not found, restarting");
                bot.saveGameScreen("skeleton-treasure-no-open", "errors");
                bot.notificationManager.sendErrorNotification("Treasure chest error", "Skeleton Chest gump without OPEN button");
                return true;
            }

        } else {
            BHBot.logger.info("Skeleton treasure found, declining.");
            seg = MarvinSegment.fromCue(BHBot.cues.get("Decline"), 5 * Misc.Durations.SECOND, declineBounds, bot.browser);
            if (seg != null) {
                bot.browser.clickOnSeg(seg);
                seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), 5 * Misc.Durations.SECOND, greenYesBounds, bot.browser);
                if (seg != null) {
                    bot.browser.clickOnSeg(seg);
                    return false;
                } else {
                    bot.saveGameScreen("treasure-no-settings-decline-no-yes", "errors", bot.browser.getImg());
                    BHBot.logger.error("Impossible to find yes button after decline with no settings in handleSkeletonKey");
                    bot.notificationManager.sendErrorNotification("Treasure chest error", "Skeleton Chest gump without YES button");
                    return true;
                }
            } else {
                bot.saveGameScreen("treasure-no-settings-no-decline", "errors", bot.browser.getImg());
                BHBot.logger.error("Impossible to find decline with no settings button in handleSkeletonKey");
                bot.notificationManager.sendErrorNotification("Treasure chest error", "Skeleton Chest gump without DECLINE button");
                return true;
            }
        }
    }

    private void handleAutoOff() {
        MarvinSegment seg;

        // Auto Revive is disabled, we re-enable Auto on Dungeon
        if ((bot.settings.autoRevive.size() == 0) || (bot.getState() != BHBot.State.Trials && bot.getState() != BHBot.State.Gauntlet
                && bot.getState() != BHBot.State.Raid && bot.getState() != BHBot.State.Expedition)) {
            BHBot.logger.debug("AutoRevive disabled, reenabling auto.. State = '" + bot.getState() + "'");
            setAutoOn(0);
            bot.scheduler.resetIdleTime(true);
            return;
        }

        // if everyone dies autoRevive attempts to revive people on the defeat screen, this should prevent that
        seg = MarvinSegment.fromCue(BHBot.cues.get("Defeat"), Misc.Durations.SECOND, bot.browser);
        if (seg != null) {
            BHBot.logger.autorevive("Defeat screen, skipping revive check");
            setAutoOn(Misc.Durations.SECOND);
            bot.browser.readScreen(Misc.Durations.SECOND);
            bot.scheduler.resetIdleTime(true);
            return;
        }

        seg = MarvinSegment.fromCue(BHBot.cues.get("VictoryLarge"), 500, bot.browser);
        if (seg != null) {
            BHBot.logger.autorevive("Victory popup, skipping revive check");
            setAutoOn(Misc.Durations.SECOND);

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

        setAutoOn(Misc.Durations.SECOND);
        bot.scheduler.resetIdleTime(true);

        // after reviving we update encounter timestamp as it wasn't updating from processDungeon
        inEncounterTimestamp = Misc.getTime() / 1000;

    }

    private void closeWorldBoss() {
        if (!bot.browser.closePopupSecurely(BHBot.cues.get("DarkBlueStart"), new Cue(BHBot.cues.get("X"), Bounds.fromWidthHeight(700, 50, 55, 60)))) {
            BHBot.logger.error("first x Error returning to main screen from World Boss, restarting");
        }

        Cue yesGreenWB = new Cue(BHBot.cues.get("YesGreen"), Bounds.fromWidthHeight(295, 345, 60, 35));
        if (!bot.browser.closePopupSecurely(yesGreenWB, yesGreenWB)) {
            BHBot.logger.error("yesgreen Error returning to main screen from World Boss, restarting");
        }

        if (!bot.browser.closePopupSecurely(BHBot.cues.get("WorldBossTitle"), new Cue(BHBot.cues.get("X"), Bounds.fromWidthHeight(640, 75, 55, 55)))) {
            BHBot.logger.error("second x Error returning to main screen from World Boss, restarting");
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
        if (z < 1 || z > 13) return null;
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
            case 13:
                switch (d) {
                    case 1:
                        return new Point(610, 346);
                    case 2:
                        return new Point(445, 202);
                    case 3:
                        return new Point(255, 295);
                    case 4:
                        return new Point(160, 145);
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
    private boolean checkWorldBossInput(Settings.WorldBossSetting wbSetting) {
        final long MAX_TIMER = 600;

        WorldBoss wb = WorldBoss.fromLetter(String.valueOf(wbSetting.type));

        //check name
        if (wb == null) {
            BHBot.logger.error("Invalid world boss name, check settings file");
            return false;
        }

        //check tier
        if (wbSetting.tier < wb.minTier || wbSetting.tier > wb.maxTier) {
            BHBot.logger.error("Invalid world boss tier for " + wb.getName() + ", must be between " + wb.getMinTier() + " and " + wb.getMaxTier());
            return false;
        }

        //warn user if timer is over 5 minutes
        if (wbSetting.timer > MAX_TIMER) {
            BHBot.logger.warn("Warning: Timer longer than " + Misc.millisToHumanForm(MAX_TIMER * 1000));
            return false;
        }
        return true;
    }

    /**
     * Returns a random adventure configuration. The logic takes care of giving priority to exact days configuration over the * ones
     *
     * @return a Settings.AdventureSetting element to be used
     */
    private Settings.AdventureSetting decideAdventureRandomly(List<Settings.AdventureSetting> startList) {
        RandomCollection<Settings.AdventureSetting> randomRaid = new RandomCollection<>();

        // We create a random collection that is specific for the current day
        String todayNum = new SimpleDateFormat("u").format(new Date());
        for (Settings.AdventureSetting setting: startList) {
            if (setting.weekDay.contains(todayNum)) randomRaid.add(setting.chanceToRun, setting);
        }

        if (randomRaid.size() > 0) return randomRaid.next();

        // We create a random collection
        for (Settings.AdventureSetting setting: startList) {
            if (setting.weekDay.contains("*")) randomRaid.add(setting.chanceToRun, setting);
        }

        if (randomRaid.size() > 0) return randomRaid.next();

        return null;
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
        else if (MarvinSegment.fromCue("Zone13", bot.browser) != null)
            return 13;
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
            BHBot.logger.info("Changing from WB " + wbSelected.getName() + " to WB " + desiredWorldBoss.getName());
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
    private boolean handleRaidSelection(String desiredRaidZone, int difficulty) {

        int desiredRaid = Integer.parseInt(desiredRaidZone);

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
            BHBot.logger.warn("Raid selected in settings (R" + desiredRaidZone + ") is higher than raid level unlocked, running highest available (R" + raidUnlocked + ")");
            desiredRaid = raidUnlocked;
        }

        BHBot.logger.info("Attempting R" + desiredRaidZone + " " + (difficulty == 1 ? "Normal" : difficulty == 2 ? "Hard" : "Heroic"));

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
            BHBot.logger.info("Changing from R" + selectedRaid + " to R" + desiredRaidZone);
            // we click on the desired cue
            bot.browser.clickOnSeg(raidDotsList.get(desiredRaid - 1));
        }

        return true;
    }

    private void handleAdventureConfiguration(BHBot.State state, boolean closeActivityWindow, Bounds xButtonBounds) {

        if (closeActivityWindow) {
            if (bot.settings.autoShrine.contains(state.getShortcut())
                    || bot.settings.autoRune.containsKey(state.getShortcut())
                    || bot.settings.autoBossRune.containsKey(state.getShortcut())) {

                BHBot.logger.debug("Closing adventure window for " + state.getName());
                tryClosingAdventureWindow(xButtonBounds);
            }
        }

        //autoshrine
        if (bot.settings.autoShrine.contains(state.getShortcut())) {
            BHBot.logger.info("Configuring autoShrine for " + state.getName());
            if (!shrineManager.updateShrineSettings(true, true)) {
                BHBot.logger.error("Impossible to configure autoShrine for " + state.getName());
            }
        }

        //autoBossRune
        if (bot.settings.autoBossRune.containsKey(state.getShortcut()) && !bot.settings.autoShrine.contains(state.getShortcut())) { //if autoshrine disabled but autobossrune enabled
            BHBot.logger.info("Configuring autoBossRune for " + state.getName());
            if (!shrineManager.updateShrineSettings(true, false)) {
                BHBot.logger.error("Impossible to configure autoBossRune for " + state.getName());
            }
        }

        //activity runes
        runeManager.processAutoRune(state.getShortcut());

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
    private boolean handleNotEnoughEnergyPopup() {
        MarvinSegment seg = MarvinSegment.fromCue(BHBot.cues.get("NotEnoughEnergy"), Misc.Durations.SECOND * 3, bot.browser);
        if (seg != null) {
            // we don't have enough energy!
            BHBot.logger.warn("Problem detected: insufficient energy to attempt dungeon. Cancelling...");
            bot.browser.closePopupSecurely(BHBot.cues.get("NotEnoughEnergy"), BHBot.cues.get("No"));

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

            return true;
        } else {
            return false;
        }
    }

    /**
     * Will check if "Not enough xeals" popup is open. If it is, it will automatically close it and close all other windows
     * until it returns to the main screen.
     *
     * @return true in case popup was detected and closed.
     */
    private boolean handleNotEnoughXealsPopup() {
        MarvinSegment seg = MarvinSegment.fromCue("NotEnoughXeals", Misc.Durations.SECOND * 3, bot.browser);
        if (seg != null) {
            // we don't have enough xeals!
            BHBot.logger.warn("Problem detected: insufficient xeals to attempt Wold Boss. Cancelling...");
            bot.browser.closePopupSecurely(BHBot.cues.get("NotEnoughXeals"), BHBot.cues.get("No"));

            bot.browser.closePopupSecurely(BHBot.cues.get("WorldBossSummonTitle"), BHBot.cues.get("X"));

            bot.browser.closePopupSecurely(BHBot.cues.get("WorldBossTitle"), BHBot.cues.get("X"));

            return true;
        } else {
            return false;
        }
    }

    /**
     * Will check if "Not enough tokens" popup is open. If it is, it will automatically close it and close all other windows
     * until it returns to the main screen.
     *
     * @return null if error, true in case popup was detected and closed, false otherwise.
     */
    private Boolean handleNotEnoughTokensPopup(boolean closeTeamWindow) {
        MarvinSegment seg = MarvinSegment.fromCue("NotEnoughTokens", bot.browser);

        if (seg != null) {
            BHBot.logger.warn("Not enough token popup detected! Closing trial window.");

            if (!bot.browser.closePopupSecurely(BHBot.cues.get("NotEnoughTokens"), BHBot.cues.get("No"))) {
                BHBot.logger.error("Impossible to close the 'Not Enough Tokens' pop-up window. Restarting");
                return null;
            }

            if (closeTeamWindow) {
                if (!bot.browser.closePopupSecurely(BHBot.cues.get("Accept"), BHBot.cues.get("X"))) {
                    BHBot.logger.error("Impossible to close the team window when no tokens are available. Restarting");
                    return null;
                }
            }

            if (!bot.browser.closePopupSecurely(BHBot.cues.get("TrialsOrGauntletWindow"), BHBot.cues.get("X"))) {
                BHBot.logger.error("Impossible to close the 'TrialsOrGauntletWindow' window. Restarting");
                return null;
            }

            return true;
        }
        return false;
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

    private int readNumFromImg(BufferedImage im, String numberPrefix, HashSet<Integer> intToSkip) {
        List<ScreenNum> nums = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            if (intToSkip.contains(i)) continue;
            List<MarvinSegment> list = FindSubimage.findSubimage(im, BHBot.cues.get(numberPrefix + "" + i).im, 1.0, true, false, 0, 0, 0, 0);
            //BHBot.logger.info("DEBUG difficulty detection: " + i + " - " + list.size());
            for (MarvinSegment s : list) {
                nums.add(new ScreenNum(Integer.toString(i), s.x1));
            }
        }

        // order list horizontally:
        Collections.sort(nums);

        if (nums.size() == 0)
            return 0; // error

        StringBuilder result = new StringBuilder();
        for (ScreenNum sn : nums) {
            result.append(sn.value);
        }

        return Integer.parseInt(result.toString());
    }

    /**
     * Given a image containing a range of values in this format <value1><separator><value2>, this method will
     * read the image and return the integer representation of <value1> and <value2>.
     *
     * @param im                  a BufferedImage containing the range. The image must be converted in Black & White scale.
     * @param numberPrefix        The prefix used to read number cues. This depends on how cues have been defined
     * @param intToSkip           Should we skip any number from the range read?
     * @param rangeSeparatorName  The name of the separator cue
     * @param rangeSeparatorValue What character will be used to represent the separator internally in the method?
     * @return An integer array of two values containing the minimum and maximum values for the range.
     * In case of error an empty array is returned and you have to check this in your own code.
     */
    @SuppressWarnings("SameParameterValue")
    private int[] readNumRangeFromImg(BufferedImage im, String numberPrefix, HashSet<Integer> intToSkip, String rangeSeparatorName, String rangeSeparatorValue) {
        List<ScreenNum> nums = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            if (intToSkip.contains(i)) continue;
            List<MarvinSegment> list = FindSubimage.findSubimage(im, BHBot.cues.get(numberPrefix + "" + i).im, 1.0, true, false, 0, 0, 0, 0);
            //BHBot.logger.info("DEBUG difficulty detection: " + i + " - " + list.size());
            for (MarvinSegment s : list) {
                nums.add(new ScreenNum(Integer.toString(i), s.x1));
            }
        }

        // No numbers have been found
        if (nums.size() == 0)
            return new int[]{}; // error

        // We take care of the separator
        List<MarvinSegment> list = FindSubimage.findSubimage(im, BHBot.cues.get(numberPrefix + "" + rangeSeparatorName).im, 1.0, true, false, 0, 0, 0, 0);
        //BHBot.logger.info("DEBUG difficulty detection: " + i + " - " + list.size());

        if (list.size() == 0) {
            BHBot.logger.error("No separator character found in readNumRangeFromImg!");
            return new int[]{};
        } else if (list.size() > 1) {
            BHBot.logger.error("More than one separator character found in readNumRangeFromImg!");
            return new int[]{};
        }

        for (MarvinSegment s : list) {
            nums.add(new ScreenNum(rangeSeparatorValue, s.x1));
        }

        // order list horizontally:
        Collections.sort(nums);

        StringBuilder result = new StringBuilder();
        for (ScreenNum sn : nums) {
            result.append(sn.value);
        }

        String[] rangesStr = result.toString().split(rangeSeparatorValue);

        return new int[]{Integer.parseInt(rangesStr[0]), Integer.parseInt(rangesStr[1])};
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
        im.toBlackWhite(new Color(25, 25, 25), new Color(255, 255, 255), 254);

        BufferedImage imb = im.getBufferedImage();

        return readNumFromImg(imb);
    }

    /* World boss reading and changing section */
    private int detectWorldBossTier() {
        int xOffset, yOffset, w, h;
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

        tierDropDown = MarvinSegment.fromCue("WorldBossTierDropDown", Misc.Durations.SECOND * 2, bot.browser); // For tier drop down menu

        if (tierDropDown == null) {
            BHBot.logger.error("Error: unable to detect world boss difficulty selection box in detectWorldBossTier!");
            return 0; // error
        }

        MarvinImage im = new MarvinImage(bot.browser.getImg().getSubimage(xOffset, yOffset, w, h));

        // make it white-gray (to facilitate cue recognition):
        im.toBlackWhite(new Color(25, 25, 25), new Color(255, 255, 255), 254);

        BufferedImage imb = im.getBufferedImage();

        return readNumFromImg(imb);
    }

    /**
     * This method takes care of managing the correct WB tier selection
     * @param targetTier The desired tier for the World Boss
     * @return true for success, false if an error happens
     */
    private boolean changeWorldBossTier(int targetTier) {
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

        // We detect what is the top available tier. This may be different based on player level and unlocked zones
        Bounds topTierBounds = Bounds.fromWidthHeight(403, 156, 27, 26);
        MarvinImage topTierImg = new MarvinImage(bot.browser.getImg().getSubimage(topTierBounds.x1, topTierBounds.y1, topTierBounds.width, topTierBounds.height));
        topTierImg.toBlackWhite(new Color(25, 25, 25), new Color(255, 255, 255), 254);
        int topAvailableTier = readNumFromImg(topTierImg.getBufferedImage(), "", new HashSet<>());

        if (topAvailableTier == 0) {
            BHBot.logger.error("Impossible to detect maximum available tier in World Boss");
            bot.saveGameScreen("wb_max_tier", "errors");
            return false;
        }

        BHBot.logger.debug("Detected top available tier is: " + topAvailableTier);

        // The bounds for the WB tier selection
        Bounds tiersBounds = Bounds.fromWidthHeight(263, 139, 251, 60);

        // Offset between the different tiers buttons
        int tierOffset = 60;

        // position on X axis is independent from tier
        int clickX = tiersBounds.x1 + (tiersBounds.width / 2);

        // Used to understand how many times we should click on bar down/up cue
        int tierDiff = topAvailableTier - targetTier;

        // Used to check if we should scroll
        Function<Integer, Boolean> scrollCheck = tierDiff < 0 ? tierDiffArg -> tierDiffArg < 0 : tierDiffArg -> tierDiffArg > 4;

        // Used to change the tierDiff value during iterations
        Function<Integer, Integer> tierDiffUpdate = tierDiff < 0 ? tierDiffArg -> tierDiffArg + 1 : tierDiffArg -> tierDiffArg - 1;

        // Used to identify the correct cue to scroll
        String cueName = tierDiff < 0 ? "DropDownUp" : "DropDownDown";

        seg = MarvinSegment.fromCue(BHBot.cues.get(cueName), bot.browser);
        if (seg == null) {
            BHBot.logger.error("Error: unable to detect " + cueName + " in World Boss Tier selection");
            bot.saveGameScreen("wb_tier_" + cueName, "errors");
            return false;
        }

        while (scrollCheck.apply(tierDiff)) {
            bot.browser.clickOnSeg(seg);
            tierDiff = tierDiffUpdate.apply(tierDiff);
        }

        int clickY = tiersBounds.y1 + (tierOffset * tierDiff) + (tiersBounds.height / 2);

        bot.browser.clickInGame(clickX, clickY);

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
     * Changes difficulty level in expedition window.
     * Note: for this to work, expedition window must be open!
     *
     * @return 0 in case of error, newDifficulty if everything was ok, another integer if for any reason the desired
     * level could not be set. Caller will have to check this in its own code.
     */
    int selectDifficulty(int oldDifficulty, int newDifficulty, Cue difficulty, int step, boolean useDifficultyRanges) {
        if (oldDifficulty == newDifficulty)
            return newDifficulty; // no change

        MarvinSegment seg = MarvinSegment.fromCue(difficulty, 2 * Misc.Durations.SECOND, bot.browser);
        if (seg == null) {
            BHBot.logger.error("Error: unable to detect 'select difficulty' button while trying to change difficulty level!");
            return 0; // error
        }

        bot.browser.clickOnSeg(seg);

        bot.browser.readScreen(5 * Misc.Durations.SECOND);

        if (useDifficultyRanges) {
            return selectDifficultyFromRange(newDifficulty);
        }

        return selectDifficultyFromDropDown(newDifficulty, 0, step);
    }

    /**
     * Changes difficulty level in trials/gauntlet window using the two step choice: first selecting the range and then
     * choosing the closed matched difficulty.
     * <p>
     * Note: for this to work, trials/gauntlet window needs to be opened!
     *
     * @param newDifficulty The desired target difficulty
     * @return 0 in case of error, newDifficulty if everything was ok, another integer if for any reason the desired
     * level could not be set. Caller will have to check this in its own code.
     */
    private int selectDifficultyFromRange(int newDifficulty) {

        // Bounds for difficulty range
        Bounds difficultyRangeBounds = Bounds.fromWidthHeight(305, 140, 160, 35);
        // offset to read different ranges
        final int yOffset = 60;

        // Color definition for B&W conversion
        Color difficultyBlack = new Color(25, 25, 25);
        Color difficultyWhite = new Color(255, 255, 255);
        final int customMax = 254;

        // Scroller cues
        Cue scrollerAtTop = new Cue(BHBot.cues.get("ScrollerAtTop"), Bounds.fromWidthHeight(520, 115, 35, 90));
//        Cue scrollerAtBottom = new Cue(BHBot.cues.get("ScrollerAtBottom"), Bounds.fromWidthHeight(520, 360, 35, 90) );
        // Scroller max clicks
        final int MAX_CLICKS = 30;

        int cntAttempt = 0;

        // We make sure the scroll is at the top position. This is just failsafe in all the tests this was always the default behavior
        MarvinSegment seg = MarvinSegment.fromCue("ScrollerNone", Misc.Durations.SECOND / 2, bot.browser);
        if (seg == null) {
            seg = MarvinSegment.fromCue(scrollerAtTop, Misc.Durations.SECOND / 2, bot.browser);

            while (seg == null) {
                if (cntAttempt > MAX_CLICKS) {
                    BHBot.logger.error("It was impossible to move the scroller to the top position for the difficulty range.");
                    return 0;
                }
                bot.browser.clickInGame(540, 133);

                seg = MarvinSegment.fromCue(scrollerAtTop, Misc.Durations.SECOND / 2, bot.browser);
                cntAttempt++;
            }
        }

        bot.browser.readScreen(Misc.Durations.SECOND / 2);

        cntAttempt = 0;
        int rangeCnt = 0;
        int minDifficulty;
        do {
            // we could not move the scroller at the bottom position
            if (cntAttempt > MAX_CLICKS) {
                BHBot.logger.error("It was impossible to move the scroller to the bottom position for the difficulty.");
                return 0;
            }

            // We read ranges five at time, so we scroll down when we are done.
            // We also use this to calculate the right difficulty range to read
            int rangePos = rangeCnt % 5;

            // we need to click on the bottom arrow to have new ranges on monitor
            if (rangePos == 0 && rangeCnt > 0) {
                seg = MarvinSegment.fromCue(BHBot.cues.get("DropDownDown"), bot.browser);
                if (seg == null) {
                    BHBot.logger.error("Error: unable to detect down arrow in trials/gauntlet difficulty range drop-down menu!");
                    bot.saveGameScreen("select_difficulty_range_arrow_down", "errors");
                    return 0;
                }

                for (int barPos = 0; barPos < 5; barPos++) {
                    bot.browser.clickOnSeg(seg);
                }
                bot.browser.readScreen(Misc.Durations.SECOND / 2);
            }

            // We use rangePos to read the right difficulty range
            int posOffset = rangePos * yOffset;
            BufferedImage topRangeImg = bot.browser.getImg().getSubimage(difficultyRangeBounds.x1, difficultyRangeBounds.y1 + posOffset, difficultyRangeBounds.width, difficultyRangeBounds.height);
            MarvinImage im = new MarvinImage(topRangeImg);
            im.toBlackWhite(difficultyBlack, difficultyWhite, customMax);

            int[] diffRange = readNumRangeFromImg(im.getBufferedImage(), "", new HashSet<>(), "hyphen", "-");
            BHBot.logger.debug("Detected difficulty range: " + Arrays.toString(diffRange));
            if (diffRange.length != 2) {
                BHBot.logger.error("It was impossible to read the top difficulty range");
                return 0;
            }

            // We save difficulty bounds for readability sake
            int rangeMinDifficulty = diffRange[0], rangeMaxDifficulty = diffRange[1];
            minDifficulty = rangeMinDifficulty;

            // new difficulty out of range, we only check it on the first iteration
            if (rangeCnt == 0) {
                if (newDifficulty > rangeMaxDifficulty) {
                    BHBot.logger.warn("New difficulty " + newDifficulty + " is bigger than maximum available difficulty: " + rangeMaxDifficulty + ". Using maximum difficulty.");
                    newDifficulty = rangeMaxDifficulty;
                }
            }

            // we've found the right range and we click it!
            if (newDifficulty >= rangeMinDifficulty && newDifficulty <= rangeMaxDifficulty) {
                bot.browser.clickInGame((difficultyRangeBounds.x1 + difficultyRangeBounds.width / 2), (difficultyRangeBounds.y1 + posOffset + difficultyRangeBounds.height / 2));

                // We wait for the difficulty selection to come out
                bot.browser.readScreen(Misc.Durations.SECOND);

                // Bounds of the top difficulty value
                Bounds topLvlBounds = Bounds.fromWidthHeight(350, 150, 70, 35);

                /*
                 * In higher tiers difficulty ranges are non continuous and the difference between the values increase.
                 * Low difficulties have steps of 1, then this increase to 5 and finally also to 10. As this appear to be
                 * dynamic, the step between the difficulties is calculated everytime reading from screen the two top
                 * most values in the difficulty popup
                 * */

                // Top most difficulty value
                BufferedImage topLvlBImg = bot.browser.getImg().getSubimage(topLvlBounds.x1, topLvlBounds.y1, topLvlBounds.width, topLvlBounds.height);
                MarvinImage topLvlMImg = new MarvinImage(topLvlBImg);
                topLvlMImg.toBlackWhite(difficultyBlack, difficultyWhite, customMax);
                int topLvl = readNumFromImg(topLvlMImg.getBufferedImage());
                if (topLvl == 0) {
                    BHBot.logger.error("Impossible to read difficulty range top level.");
                    return 0;
                }

                // Second difficulty value
                BufferedImage secondLvlBImg = bot.browser.getImg().getSubimage(topLvlBounds.x1, topLvlBounds.y1 + yOffset, topLvlBounds.width, topLvlBounds.height);
                MarvinImage secondLvlMImg = new MarvinImage(secondLvlBImg);
                secondLvlMImg.toBlackWhite(difficultyBlack, difficultyWhite, customMax);
                int secondLvl = readNumFromImg(secondLvlMImg.getBufferedImage());
                if (secondLvl == 0) {
                    BHBot.logger.error("Impossible to read difficulty range second level.");
                    return 0;
                }

                // Difficulty step value
                int lvlStep = topLvl - secondLvl;
                BHBot.logger.debug("Difficulty step is: " + lvlStep);

                // We calculate all the possible values and back-fill them in an array list so that we can use them later
                List<Integer> possibleDifficulties = new ArrayList<>();
                int startDifficulty = rangeMaxDifficulty;
                while (startDifficulty >= rangeMinDifficulty) {
                    possibleDifficulties.add(startDifficulty);

                    startDifficulty -= lvlStep;
                }

                // It is not always possible to get to the exact desired difficulty, so the best match is chosen
                // The absolute value of the difference is used to check the closest match
                int distance = Math.abs(possibleDifficulties.get(0) - newDifficulty);
                int idx = 0;
                for (int i = 1; i < possibleDifficulties.size(); i++) {
                    int cdistance = Math.abs(possibleDifficulties.get(i) - newDifficulty);
                    if (cdistance <= distance) {
                        idx = i;
                        distance = cdistance;
                    }
                }

                int matchedDifficulty = possibleDifficulties.get(idx);
                if (matchedDifficulty - newDifficulty != 0) {
                    BHBot.logger.info("The closest match to " + newDifficulty + " is " + matchedDifficulty);
                }

                // We have it on screen, so we can click on it!
                if (idx < 5) {
                    bot.browser.clickInGame(topLvlBounds.x1 + topLvlBounds.width / 2, topLvlBounds.y1 + (yOffset * idx) + topLvlBounds.height / 2);
                    return matchedDifficulty;
                } else {
                    // We check that the arrow down on the scroller is there
                    seg = MarvinSegment.fromCue(BHBot.cues.get("DropDownDown"), bot.browser);
                    if (seg == null) {
                        BHBot.logger.error("Error: unable to detect down arrow in trials/gauntlet second step difficulty range drop-down menu!");
                        bot.saveGameScreen("select_difficulty_range_2nd_step_arrow_down", "errors");
                        return 0;
                    }

                    /*
                     * First five difficulty difficulties are on screen, we start to scroll down and we use the possibleDifficulties
                     * ArrayList that we created before to check that the position we currently are at is the one of the
                     * matched difficulty we found earlier
                     * */
                    for (int idxI = 5; idxI < possibleDifficulties.size(); idxI++) {
                        bot.browser.clickOnSeg(seg);

                        if (possibleDifficulties.get(idxI) == matchedDifficulty) {
                            // We can finally click on the difficulty value!!
                            bot.browser.clickInGame(topLvlBounds.x1 + topLvlBounds.width / 2, topLvlBounds.y1 + (yOffset * 4) + topLvlBounds.height / 2);
                            return matchedDifficulty;
                        }
                    }
                }
            }

            cntAttempt++;
            rangeCnt++;
        } while (minDifficulty != 1);

        return 0;
    }

    /**
     * Internal routine. Difficulty drop down must be open for this to work!
     * Note that it closes the drop-down when it is done (except if an error occurred). However there is a close
     * animation and the caller must wait for it to finish.
     *
     * @return false on error (caller must do restart() if he gets false as a result from this method)
     */
    private int selectDifficultyFromDropDown(int newDifficulty, int recursionDepth, int step) {
        // horizontal position of the 5 buttons:
        final int posx = 390;
        // vertical positions of the 5 buttons:
        final int[] posy = new int[]{170, 230, 290, 350, 410};

        if (recursionDepth > 3) {
            BHBot.logger.error("Error: Selecting difficulty level from the drop-down menu ran into an endless loop!");
            bot.saveGameScreen("select_difficulty_recursion", "errors");
            tryClosingWindow(); // clean up after our selves (ignoring any exception while doing it)
            return 0;
        }

        MarvinSegment seg;

        // the first (upper most) of the 5 buttons in the drop-down menu. Note that every while a "tier x" is written bellow it, so text is higher up (hence we need to scan a larger area)
        MarvinImage subm = new MarvinImage(bot.browser.getImg().getSubimage(350, 150, 70, 35));
        subm.toBlackWhite(new Color(25, 25, 25), new Color(255, 255, 255), 254);
        BufferedImage sub = subm.getBufferedImage();
        int num = readNumFromImg(sub);
//		BHBot.logger.info("num = " + Integer.toString(num));
        if (num == 0) {
            BHBot.logger.error("Error: unable to read difficulty level from a drop-down menu!");
            bot.saveGameScreen("select_difficulty_read", "errors");
            tryClosingWindow(); // clean up after our selves (ignoring any exception while doing it)
            return 0;
        }

        int move = (newDifficulty - num) / step; // if negative, we have to move down (in dropdown/numbers), or else up
//		BHBot.logger.info("move = " + Integer.toString(move));

        if (move >= -4 && move <= 0) {
            // we have it on screen. Let's select it!
            bot.browser.clickInGame(posx, posy[Math.abs(move)]); // will auto-close the drop down (but it takes a second or so, since it's animated)
            return newDifficulty;
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
                return 0;
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
                return 0;
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
            im.toBlackWhite(new Color(25, 25, 25), new Color(255, 255, 255), 254);
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

    private void tryClosingAdventureWindow (Bounds xButtonBounds) {
        bot.browser.readScreen();
        MarvinSegment seg = MarvinSegment.fromCue(BHBot.cues.get("X"), Misc.Durations.SECOND * 2, xButtonBounds, bot.browser);

        if (seg != null) bot.browser.clickOnSeg(seg);
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
        bot.setState(BHBot.State.FishingBaits);
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
        timeLastXealsCheck = 0;
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

        if (((globalXeals - 1) >= bot.settings.minXeals) && bot.getState() == BHBot.State.WorldBoss) {
            timeLastXealsCheck = 0;
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
        LEGENDARY("Legendary", 4),
        MYTHIC("Mythic", 5);

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

    /**
     * This Enum is used to group together all the information related to the World Boss
     */
    enum WorldBoss {
        Orlag("o", "Orlag Clan", 1, 3, 12, 5),
        Netherworld("n", "Netherworld", 2, 3, 13, 3),
        Melvin("m", "Melvin", 3, 10, 11, 4),
        Ext3rmin4tion("3", "3xt3rmin4tion", 4, 10, 11, 3),
        BrimstoneSyndicate("b", "Brimstone Syndicate", 5, 11, 12, 3),
        TitansAttack("t", "Titans Attack", 6, 11, 14, 3),
        IgnitedAbyss("i", "The Ignited Abyss", 7, 13, 14, 3),
        Unknown("?", "Unknown", 8, 13, 100, 1);

        private final String letter;
        private final String Name;
        private final int number;
        private final int minTier;
        private final int maxTier;
        private final int partySize;

        /**
         * @param letter             the shortcut letter used in settings.ini
         * @param Name               the real name of the World Boss
         * @param number             the World Boss number counting from left to right starting at 1
         * @param minTier            the minimum tier required to join the World Boss
         * @param maxTier            the maximum tier you are allowed to join for the World Boss
         * @param partySize          the party size of the World Boss
         */
        WorldBoss(String letter, String Name, int number, int minTier, int maxTier, int partySize) {
            this.letter = letter;
            this.Name = Name;
            this.number = number;
            this.minTier = minTier;
            this.maxTier = maxTier;
            this.partySize = partySize;
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

        int getPartySize() {
            return partySize;
        }

        /*int[] getYScrollerPositions() {
            return yScrollerPositions;
        }*/

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
        restrictedCues.put("Sand Clock", BHBot.cues.get("Material_R11"));
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

    String saveDebugWBTSScreen(int totalTS, int[] playersTS, String lastSavedName) {
        if (bot.settings.debugWBTS) {
            // To ease debug we put the TS values in the file name
            StringBuilder fileNameTS = new StringBuilder();
            fileNameTS.append("wb-")
                    .append(counters.get(BHBot.State.WorldBoss).getTotal() + 1)
                    .append("-T").append(totalTS);

            for (int iPartyMember = 0; iPartyMember < playersTS.length; iPartyMember++) {
                fileNameTS.append("-").append(iPartyMember + 1).append("P").append(playersTS[iPartyMember]);
            }

            String finalFileName = fileNameTS.toString();
            if (!lastSavedName.equals(finalFileName)) {
                bot.saveGameScreen(fileNameTS.toString(), "wb-ts-debug", bot.browser.getImg());
            }
            return finalFileName;
        }

        return "";
    }

    void setAutoOff(int timeout) {
        MarvinSegment autoSeg = MarvinSegment.fromCue(BHBot.cues.get("AutoOn"), timeout, bot.browser);

        if (autoSeg != null) {
            bot.browser.clickOnSeg(autoSeg);
        }
    }

    void setAutoOn(int timeout) {
        MarvinSegment autoSeg = MarvinSegment.fromCue(BHBot.cues.get("AutoOff"), timeout, bot.browser);

        if (autoSeg != null) {
            bot.browser.clickOnSeg(autoSeg);
        }
    }

}
