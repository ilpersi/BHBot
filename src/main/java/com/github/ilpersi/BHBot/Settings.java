package com.github.ilpersi.BHBot;

import com.google.common.collect.Maps;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Settings {

    static class WorldBossSetting {
        char type;
        byte difficulty;
        byte tier;
        double chanceToRun;

        // Optional fields can be nullable so we don't use Java primitives
        Short timer;
        Boolean solo;
        Integer minimumTotalTS;
        Integer minimumPlayerTS;

        WorldBossSetting(char type, byte difficulty, byte tier, double chanceToRun, @Nullable Short timer,
                         @Nullable Boolean solo, @Nullable Integer minimumTotalTS, @Nullable Integer minimumPlayerTS) {
            this.type = type;
            this.difficulty = difficulty;
            this.tier = tier;
            this.chanceToRun = chanceToRun;

            // Optional fields
            this.timer = Optional.ofNullable(timer).orElse((short) 300);
            this.solo = Optional.ofNullable(solo).orElse(false);
            this.minimumTotalTS = Optional.ofNullable(minimumTotalTS).orElse(0);
            this.minimumPlayerTS = Optional.ofNullable(minimumPlayerTS).orElse(0);
        }

        @Override
        public String toString() {

            return type +
                    " " + difficulty +
                    " " + tier +
                    " " + (int) chanceToRun +
                    " " + timer +
                    " " + (solo ? "1" : "0") +
                    " " + minimumTotalTS +
                    " " + minimumPlayerTS;
        }
    }

    static class AdventureSetting {
        String weekDay;
        String adventureZone;
        int difficulty;
        double chanceToRun;

        // Optional fields, can be nullable so we use Java primitives
        Boolean rerun;
        Boolean solo;

        AdventureSetting(String weekDay, String zone, int difficulty, double chanceToRun, boolean solo, boolean rerun) {
            this.weekDay = weekDay;
            this.adventureZone = zone;
            this.difficulty = difficulty;
            this.chanceToRun = chanceToRun;
            this.solo = solo;
            this.rerun = rerun;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(this.weekDay).append(' ')
                .append(this.adventureZone).append(" ")
                .append(this.difficulty).append(" ")
                .append((int) this.chanceToRun);


            // we only append what is really needed
            if (solo && rerun) {
                // they are both true, we append both
                sb.append(" ").append(1); // solo
                sb.append(" ").append(1); // rerun
            }  else if (solo) {
                // only rerun is true, we skip solo
                sb.append(" ").append(1); // solo
            } else if (rerun) {
                // rerun is false, solo is true
                sb.append(" ").append(0); // solo
                sb.append(" ").append(1); // rerun
            }

            return sb.toString();
        }

        static AdventureSetting fromString(String configStr){
            String weekDay;
            String adventureZone;
            int difficulty;
            double chanceToRun;

            // \s*(?<weekDay>[*1234567]{1,7})\s(?<adventureZone>[1-9zd]{1,4})\s(?<difficulty>[123])\s(?<chanceToRun>\d+)\s?(?<rerun>[01]?)\s?(?<solo>[01]?)
            // 
            // Options: Case insensitive; Exact spacing; Dot doesn’t match line breaks; ^$ don’t match at line breaks; Default line breaks
            // 
            // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s*»
            //    Between zero and unlimited times, as many times as possible, giving back as needed (greedy) «*»
            // Match the regex below and capture its match into a backreference named “weekDay” (also backreference number 1) «(?<weekDay>[*1234567]{1,7})»
            //    Match a single character from the list “*1234567” «[*1234567]{1,7}»
            //       Between one and 7 times, as many times as possible, giving back as needed (greedy) «{1,7}»
            // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s»
            // Match the regex below and capture its match into a backreference named “adventureZone” (also backreference number 2) «(?<adventureZone>[1-9zd]{1,4})»
            //    Match a single character present in the list below «[1-9zd]{1,4}»
            //       Between one and 4 times, as many times as possible, giving back as needed (greedy) «{1,4}»
            //       A character in the range between “1” and “9” «1-9»
            //       A single character from the list “zd” (case insensitive) «zd»
            // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s»
            // Match the regex below and capture its match into a backreference named “difficulty” (also backreference number 3) «(?<difficulty>[123])»
            //    Match a single character from the list “123” «[123]»
            // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s»
            // Match the regex below and capture its match into a backreference named “chanceToRun” (also backreference number 4) «(?<chanceToRun>\d+)»
            //    Match a single character that is a “digit” (ASCII 0–9 only) «\d+»
            //       Between one and unlimited times, as many times as possible, giving back as needed (greedy) «+»
            // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s?»
            //    Between zero and one times, as many times as possible, giving back as needed (greedy) «?»
            // Match the regex below and capture its match into a backreference named “rerun” (also backreference number 5) «(?<rerun>[01]?)»
            //    Match a single character from the list “01” «[01]?»
            //       Between zero and one times, as many times as possible, giving back as needed (greedy) «?»
            // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s?»
            //    Between zero and one times, as many times as possible, giving back as needed (greedy) «?»
            // Match the regex below and capture its match into a backreference named “solo” (also backreference number 6) «(?<solo>[01]?)»
            //    Match a single character from the list “01” «[01]?»
            //       Between zero and one times, as many times as possible, giving back as needed (greedy) «?»

            Pattern raidRegex = Pattern.compile("\\s*(?<weekDay>[*1234567]{1,7})\\s(?<adventureZone>[1-9zd]{1,4})\\s(?<difficulty>[123])\\s(?<chanceToRun>\\d+)\\s?(?<rerun>[01]?)\\s?(?<solo>[01]?)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

            // Optional fields, by default the value is false
            boolean rerun;
            boolean solo;

            String add = configStr.trim();
            if ("".equals(add))
                return null;

            Matcher raidtMatcher = raidRegex.matcher(add);
            if (raidtMatcher.find()) {
                weekDay = raidtMatcher.group("weekDay");
                adventureZone = raidtMatcher.group("adventureZone");
                difficulty = Integer.parseInt(raidtMatcher.group("difficulty"));
                chanceToRun =  Double.parseDouble(raidtMatcher.group("chanceToRun"));

                // Optional settings
                rerun = !"".equals(raidtMatcher.group("rerun")) && raidtMatcher.group("rerun") != null;
                solo = !"".equals(raidtMatcher.group("solo")) && raidtMatcher.group("solo") != null;

                return new AdventureSetting(weekDay, adventureZone, difficulty, chanceToRun, rerun, solo);
            } else {
                return null;
            }
        }
    }

    /**
     * This class holds the scheduling settings. It is basically composed of two attributes startTime and endTime
     */
    static class ActivitiesScheduleSetting {
        String weekDay;
        LocalTime startTime;
        LocalTime endTime;
        String settingsPlan;
        String chromeProfilePath;

        ActivitiesScheduleSetting(String weekDay, LocalTime startTime, LocalTime endTime, String settingsPlan, String chromeProfilePath) {
            this.weekDay = weekDay;
            this.startTime = startTime;
            this.endTime = endTime;
            this.settingsPlan = settingsPlan;
            this.chromeProfilePath = chromeProfilePath;
        }

        /**
         * @return true if the the current time is >= of the startTime and <= of the endTime and the weekDay is either *
         * or the current day of the week
         */
        boolean isActive() {
            return (this.startTime.compareTo(LocalTime.now()) <= 0) && (this.endTime.compareTo(LocalTime.now()) >= 0
            && (this.weekDay.contains("*") || this.weekDay.contains(new SimpleDateFormat("u").format(new Date()))) );
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append(this.weekDay).append(" ")
                    .append(this.startTime.toString())
                    .append("-")
                    .append(this.endTime.toString());

            if (!"".equals(this.settingsPlan)) result.append(" ").append(this.settingsPlan);
            if (!"".equals(this.chromeProfilePath)) result.append(" \"").append(this.chromeProfilePath).append("\"");

            return result.toString();
        }
    }

    /**
     * This class is a wrapper around List<ActivitiesScheduleSetting> that adds some helper methods to
     * correctly manage the scheduling list
     */
    static class ActivitiesScheduleList implements Iterable<ActivitiesScheduleSetting> {
        private final List<ActivitiesScheduleSetting> scheduleList;

        ActivitiesScheduleList() {
            // We initialize the array list and override the toString method
            this.scheduleList = new ArrayList<>() {
                private static final long serialVersionUID = 1L;

                @Override
                public @NotNull String toString() {
                    StringBuilder result = new StringBuilder();
                    for (ActivitiesScheduleSetting s : this) {
                        if (result.length() > 0) result.append(";");
                        result.append(s.toString());
                    }
                    return result.toString();
                }
            };
        }

        @SuppressWarnings("UnusedReturnValue")
        boolean add(ActivitiesScheduleSetting schedule) {
            return this.scheduleList.add(schedule);
        }

        void clear() {
            this.scheduleList.clear();
        }

        boolean isEmpty() {
            return this.scheduleList.isEmpty();
        }

        int size() {return this.scheduleList.size();}

        @NotNull
        @Override
        public Iterator<ActivitiesScheduleSetting> iterator() {
            return this.scheduleList.iterator();
        }

        @Override
        public String toString() {
            return this.scheduleList.toString();
        }
    }

    static String configurationFile = "settings.ini";
    static String initialConfigurationFile = "settings.ini";

    String username = "";
    String password = "";

    // Pushover info
    String poAppToken = "";
    String poUserToken = "";

    // Discord info
    String discordUserName = "";
    String discordWebHookUrl = "";

    boolean debugDetectionTimes = false; // if true, then each time a cue detection from game screenshot will be attempted, a time taken will be displayed together with a name of the cue
    boolean useDoNotShareURL = false; // if true, then each time a cue detection from game screenshot will be attempted, a time taken will be displayed together with a name of the cue
    boolean hideWindowOnRestart = false; // if true, game window will be hidden upon driver (re)start
    int reconnectTimer = 60;
    boolean idleMode = false;
    // chromedriver autostart
    boolean autoStartChromeDriver = true; // if true, BHBot will automatically run chromedriver at startup
    //Various settings
    int openSkeleton = 0;
    boolean contributeFamiliars = true;
    LinkedHashSet<String> screenshots;

    //activity settings
    LinkedHashSet<String> activitiesEnabled;
    boolean activitiesRoundRobin = true;
    // We extend the ArrayList class with a couple of helper methods
    ActivitiesScheduleList activitiesSchedule = new ActivitiesScheduleList();
    ActivitiesScheduleList defaultActivitiesSchedule = new ActivitiesScheduleList();

    // Pushover settings
    boolean enablePushover = false;
    boolean poNotifyPM = false;
    boolean poNotifyCrash = false;
    boolean poNotifyErrors = false;
    boolean poNotifyBribe = false;
    int poNotifyAlive = 0;

    // discord settings
    boolean enableDiscord = false;
    boolean discordNotifyPM = false;
    boolean discordNotifyCrash = false;
    boolean discordNotifyErrors = false;
    boolean discordNotifyBribe = false;
    int discordNotifyAlive = 0;

    /**
     * This is the minimum amount of shards that the bot must leave for the user. If shards get above this value, bot will play the raids in case raiding is enabled of course.
     */
    int minShards = 2;
    /**
     * This is the minimum amount of tokens that the bot must leave for the user. If tokens get above this value, bot will play the trials/gauntlet in case trials/gauntlet is enabled of course.
     */
    int minTokens = 5;
    /**
     * This is the minimum amount of energy as percentage that the bot must leave for the user. If energy is higher than that, then bot will attempt to play dungeons.
     */
    int minEnergyPercentage = 70;
    /**
     * This is the minimum amount of tickets that the bot must leave for the user. If tickets get above this value, bot will play the pvp in case pvp is enabled of course.
     */
    int minTickets = 5;
    /**
     * This is the minimum amount of badges that the bot must leave for the user. If badges get above this value, bot will play the gvg in case gvg is enabled of course.
     */
    int minBadges = 5;
    /**
     * This is the minimum amount of xeals that the bot must leave for the user. If xeals get above this value, bot will play the wb in case wb is enabled of course.
     */
    int minXeals = 1;

    // Max for various expendables for correct calculation if not default
    int maxShards = 4;
    int maxTokens = 10;
    int maxTickets = 10;
    int maxBadges = 10;
    int maxXeals = 10;

    // costs (1..5) for various events:
    int costPVP = 1;
    int costGVG = 1;
    int costTrials = 1;
    int costGauntlet = 1;
    int costInvasion = 1;
    int costExpedition = 1;
    /**
     * The trials/gauntlet difficulty
     */
    int difficultyTrials = 60;
    int difficultyGauntlet = 60;

    /**
     * Automatically decrease the difficulty when you get defeated
     * The HashMap key is the activity lecter: t for trial, g for gauntlet, e for expedition
     * The Map.Entry is composed by two Integers: the key is the number of levels to decrease, the value is the minimum level
     */
    HashMap<String, Map.Entry<Integer, Integer>> difficultyFailsafe = new HashMap<>();

    /**
     * Similar to difficultyFailsafe, but to increase the difficulty after a certain number of wins
     * The HashMap key is the activity lecter: t for trial, g for gauntlet, e for expedition
     * The Map.Entry is composed by two Integers: the key is the number of levels to increase, the value is the maximum level
     */
    HashMap<String, Map.Entry<Integer, Integer>> successThreshold = new HashMap<>();

    /**
     * PvP/GvG Opponent
     */
    int pvpOpponent = 1;
    int gvgOpponent = 1;
    /**
     * List of expeditions we want to do (there are 4 portals: p1, p2, p3 and p4) with a difficulty level and percentage.
     * The portals are numbered clockwise starting from the top lef and the one in the middle is p4
     * Examples:
     * 'p1 250 70;p2 250 30' ==> in 70% of cases it will do p1 at lvl 250, in 30% of cases it will do p2 at lvl 250
     */
    RandomCollection<String> expeditions;
    /**
     * List of dungeons with percentages that we will attempt to do. Dungeon name must be in standard format, i.e. 'd2z4',
     * followed by a space character and a difficulty level (1-3, where 1 is normal, 2 is hard, 3 is heroic), e.g. '3',
     * and followed by a space character and percentage, e.g. '50'.
     * Example of full string: 'z2d4 3 50'.
     */
    List<AdventureSetting> dungeons;
    /**
     * List of raids we want to do with a difficulty level and percentage.
     * Examples:
     * '1 3 70;2 1 30' ==> in 70% of cases it will do R1 on heroic, in 30% of cases it will do R2 normal
     * '1 3 100' ==> in 100% of cases it will do R1 on heroic
     */
    List<AdventureSetting> raids;
    //RandomCollection<String> wednesdayRaids;
    /**
     * World Boss Settings
     **/
    RandomCollection<WorldBossSetting> worldBossSettings;
    //    List<String> worldBossSettings;
//    int worldBossTimer = 0;
//    boolean worldBossSolo = false;
    boolean debugWBTS = false;
//    boolean dungeonOnTimeout = true;
    /**
     * Autorevive Settings
     **/
    List<String> autoRevive;
    String potionOrder = "123";
    List<String> tankPriority;
    int tankPosition = 1;
    int potionLimit = 5;
    /**
     * Autoshrine settings
     **/
    List<String> autoShrine;
    int battleDelay = 60;
    int shrineDelay = 20;
    int positionDelay = 0;

    /**
     * Autorune settings
     **/
    List<String> autoRuneDefault;
    Map<String, List<String>> autoRune = new HashMap<>();
    Map<String, List<String>> autoBossRune = new HashMap<>();
    /**
     * Drop notificatoin settings
     **/
    List<String> poNotifyDrop;
    List<String> discordNotifyDrop;
    /**
     * List of equipment that should be stripped before attempting PvP (and dressed up again after PvP is done).
     * Allowed tokens:
     * m = mainhand
     * o = offhand
     * h = head
     * b = body
     * n = neck
     * r = ring
     */
    List<String> pvpstrip;
    List<String> gvgstrip;
    /**
     * If true, then bot will try to auto consume consumables as specified by the 'consumables' list.
     */
    boolean autoConsume = false;
    /**
     * if true, the bot will save a screenshot of the victory popup
     */
    boolean victoryScreenshot = false;
    /**
     * List of consumables that we want activate at all times.
     */
    List<String> consumables;
    // List of familiars to bribe
    List<String> familiars;
    /**
     * The level at which we want to try to automatically persuade the familiar
     * 1 is for Common, 2 is for Rare, 3 is for Epic, 4 is for Legendary
     */
    int persuasionLevel = 1;
    int bribeLevel = 0;
    /**
     * Development Settings
     **/
    int familiarScreenshot = 2;

    /**
     * Fishing Settings
     **/
    boolean enterGuildHall = true;
    int baitAmount = 5;
    /**
     * log4j settings
     */
    // Where do we save the logs?
    String logBaseDir = "logs";

    // What is the default level of the logs
    Level logLevel = Level.INFO;
    // How many days of logs do we store?
    int logMaxDays = 30;
    // Do we want to output the status messages? See: http://logging.apache.org/log4j/2.x/manual/configuration.html#StatusMessages
    boolean logPringStatusMessages = false;

    /**
     * Experimental feature. Better use 'false' for now.
     */
    private boolean useHeadlessMode = false; // run Chrome with --headless switch?
    private boolean restartAfterAdOfferTimeout = true; // if true, then bot will automatically restart itself if it hasn't claimed any ad offer in a time longer than defined. This is needed because ads don't appear anymore if Chrome doesn't get restarted.
    private boolean resetTimersOnBattleEnd = true; // if true, readout timers will get reset once dungeon is cleared (or pvp or gvg or any other type of battle)
    private Map<String, String> lastUsedMap = new HashMap<>();

    // If any error happens during the settings loading, this arraylist is populated with the offending lines
    ArrayList<String> wrongSettingLines = new ArrayList<>();

    //If any warning happens during the loading, this arraylist is used to keep track of it
    ArrayList<String> warningSettingLInes = new ArrayList<>();

    public Settings() {
        setDefault();
    }

    /**
     * Takes care of resetting all the settings to their default value.
     */
    private void setDefault() {
        activitiesEnabled = new LinkedHashSet<>();
        activitiesRoundRobin = true;
        activitiesSchedule = defaultActivitiesSchedule;
        autoBossRune = new HashMap<>();
        autoConsume = false;
        autoRevive = new ArrayList<>();
        autoRune = new HashMap<>();
        autoRuneDefault = new ArrayList<>();
        autoShrine = new ArrayList<>();
        autoStartChromeDriver = true;
        baitAmount = 5;
        battleDelay = 60;
        bribeLevel = 0;
        consumables = new ArrayList<>();
        contributeFamiliars = true;
        costExpedition = 1;
        costGauntlet = 1;
        costGVG = 1;
        costInvasion = 1;
        costPVP = 1;
        costTrials = 1;
        debugDetectionTimes = false;
        debugWBTS = false;
        difficultyFailsafe = new HashMap<>();
        difficultyGauntlet = 10;
        difficultyTrials = 10;
        discordNotifyAlive = 0;
        discordNotifyBribe = false;
        discordNotifyCrash = false;
        discordNotifyDrop = new ArrayList<>();
        discordNotifyErrors = false;
        discordNotifyPM = false;
        discordUserName = "";
        discordWebHookUrl = "";
        dungeons = new ArrayList<>();
        enableDiscord = false;
        enablePushover = false;
        enterGuildHall = true;
        expeditions = new RandomCollection<>();
        familiars = new ArrayList<>();
        familiarScreenshot = 2;
        gvgOpponent = 1;
        gvgstrip = new ArrayList<>();
        hideWindowOnRestart = false;
        idleMode = false;
        lastUsedMap = new HashMap<>();
        logBaseDir = "logs";
        logLevel = Level.INFO;
        logMaxDays = 30;
        logPringStatusMessages = false;
        maxBadges = 10;
        maxShards = 4;
        maxTickets = 10;
        maxTokens = 10;
        maxXeals = 10;
        minBadges = 5;
        minEnergyPercentage = 70;
        minShards = 2;
        minTickets = 5;
        minTokens = 5;
        minXeals = 1;
        openSkeleton = 0;
        password = "";
        persuasionLevel = 1;
        poAppToken = "";
        poNotifyAlive = 0;
        poNotifyBribe = false;
        poNotifyCrash = false;
        poNotifyDrop = new ArrayList<>();
        poNotifyErrors = false;
        poNotifyPM = false;
        positionDelay = 0;
        potionLimit = 5;
        potionOrder = "123";
        poUserToken = "";
        pvpOpponent = 1;
        pvpstrip = new ArrayList<>();
        raids = new ArrayList<>();
        reconnectTimer = 60;
        resetTimersOnBattleEnd = true;
        restartAfterAdOfferTimeout = true;
        screenshots = new LinkedHashSet<>();
        shrineDelay = 20;
        successThreshold = new HashMap<>();
        tankPosition = 1;
        tankPriority = new ArrayList<>();
        useDoNotShareURL = false;
        useHeadlessMode = false;
        username = "";
        victoryScreenshot = false;
        warningSettingLInes = new ArrayList<>();
        worldBossSettings = new RandomCollection<>();
        wrongSettingLines = new ArrayList<>();

        setDifficultyFailsafeFromString("t:0 g:0");
        setAdventures(this.dungeons, "dungeon", "* z1d1 1 100");
        setExpeditions("p1 100 100"); // some default value
        setAdventures(this.raids, "raid","* 1 1 100"); // some default value for raids
        setScreenshotsFromString("w d f b dg wg fe"); // enabled all by default
    }

    /**
     * Does nothing except collect ads
     */
    void setIdle() {
        enablePushover = false;
        enableDiscord = false;
        poNotifyPM = false;
        poNotifyCrash = false;
        poNotifyErrors = false;
        poNotifyBribe = false;
        autoConsume = false;
        setAutoRuneDefaultFromString("");
        setactivitiesEnabledFromString("");
        setActivitiesScheduleFromString("");
        setScreenshotsFromString("w d f b dg wg fe"); //so we dont miss any if we are in idle
        idleMode = true;
    }

    /* Cleans the data from the input and saves it at a string */

    private void setactivitiesEnabled(String... types) {
        this.activitiesEnabled.clear();
        for (String t : types) {
            String add = t.trim();
            if ("".equals(add))
                continue;
            this.activitiesEnabled.add(add);
        }
    }

    private void setActivitiesSchedule(String... schedules) {

        // (?<weekDay>[0-9*]+)\s*(?<startH>\d{1,2}):(?<startM>\d{1,2})(?<secStartGrp>:(?<startS>\d{1,2}))?-(?<endH>\d{1,2}):(?<endM>\d{1,2})(?<secEndGrp>:(?<endS>\d{1,2}))?\s*(?<plan>\w+)?\s*(?<chromeProfPath>"(?<profilePath>[^"]+)")?
        // 
        // Options: Case insensitive; Exact spacing; Dot doesn’t match line breaks; ^$ don’t match at line breaks; Default line breaks
        // 
        // Match the regex below and capture its match into a backreference named “weekDay” (also backreference number 1) «(?<weekDay>[0-9*]+)»
        //    Match a single character present in the list below «[0-9*]+»
        //       Between one and unlimited times, as many times as possible, giving back as needed (greedy) «+»
        //       A character in the range between “0” and “9” «0-9»
        //       The literal character “*” «*»
        // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s*»
        //    Between zero and unlimited times, as many times as possible, giving back as needed (greedy) «*»
        // Match the regex below and capture its match into a backreference named “startH” (also backreference number 2) «(?<startH>\d{1,2})»
        //    Match a single character that is a “digit” (ASCII 0–9 only) «\d{1,2}»
        //       Between one and 2 times, as many times as possible, giving back as needed (greedy) «{1,2}»
        // Match the colon character «:»
        // Match the regex below and capture its match into a backreference named “startM” (also backreference number 3) «(?<startM>\d{1,2})»
        //    Match a single character that is a “digit” (ASCII 0–9 only) «\d{1,2}»
        //       Between one and 2 times, as many times as possible, giving back as needed (greedy) «{1,2}»
        // Match the regex below and capture its match into a backreference named “secStartGrp” (also backreference number 4) «(?<secStartGrp>:(?<startS>\d{1,2}))?»
        //    Between zero and one times, as many times as possible, giving back as needed (greedy) «?»
        //    Match the colon character «:»
        //    Match the regex below and capture its match into a backreference named “startS” (also backreference number 5) «(?<startS>\d{1,2})»
        //       Match a single character that is a “digit” (ASCII 0–9 only) «\d{1,2}»
        //          Between one and 2 times, as many times as possible, giving back as needed (greedy) «{1,2}»
        // Match the character “-” literally «-»
        // Match the regex below and capture its match into a backreference named “endH” (also backreference number 6) «(?<endH>\d{1,2})»
        //    Match a single character that is a “digit” (ASCII 0–9 only) «\d{1,2}»
        //       Between one and 2 times, as many times as possible, giving back as needed (greedy) «{1,2}»
        // Match the colon character «:»
        // Match the regex below and capture its match into a backreference named “endM” (also backreference number 7) «(?<endM>\d{1,2})»
        //    Match a single character that is a “digit” (ASCII 0–9 only) «\d{1,2}»
        //       Between one and 2 times, as many times as possible, giving back as needed (greedy) «{1,2}»
        // Match the regex below and capture its match into a backreference named “secEndGrp” (also backreference number 8) «(?<secEndGrp>:(?<endS>\d{1,2}))?»
        //    Between zero and one times, as many times as possible, giving back as needed (greedy) «?»
        //    Match the colon character «:»
        //    Match the regex below and capture its match into a backreference named “endS” (also backreference number 9) «(?<endS>\d{1,2})»
        //       Match a single character that is a “digit” (ASCII 0–9 only) «\d{1,2}»
        //          Between one and 2 times, as many times as possible, giving back as needed (greedy) «{1,2}»
        // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s*»
        //    Between zero and unlimited times, as many times as possible, giving back as needed (greedy) «*»
        // Match the regex below and capture its match into a backreference named “plan” (also backreference number 10) «(?<plan>\w+)?»
        //    Between zero and one times, as many times as possible, giving back as needed (greedy) «?»
        //    Match a single character that is a “word character” (ASCII letter, digit, or underscore only) «\w+»
        //       Between one and unlimited times, as many times as possible, giving back as needed (greedy) «+»
        // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s*»
        //    Between zero and unlimited times, as many times as possible, giving back as needed (greedy) «*»
        // Match the regex below and capture its match into a backreference named “chromeProfPath” (also backreference number 11) «(?<chromeProfPath>"(?<profilePath>[^"]+)")?»
        //    Between zero and one times, as many times as possible, giving back as needed (greedy) «?»
        //    Match the character “"” literally «"»
        //    Match the regex below and capture its match into a backreference named “profilePath” (also backreference number 12) «(?<profilePath>[^"]+)»
        //       Match any character that is NOT a “"” «[^"]+»
        //          Between one and unlimited times, as many times as possible, giving back as needed (greedy) «+»
        //    Match the character “"” literally «"»



        Pattern scheduleRegex = Pattern.compile("(?<weekDay>[0-9*]+)\\s*(?<startH>\\d{1,2}):(?<startM>\\d{1,2})(?<secStartGrp>:(?<startS>\\d{1,2}))?-(?<endH>\\d{1,2}):(?<endM>\\d{1,2})(?<secEndGrp>:(?<endS>\\d{1,2}))?\\s*(?<plan>\\w+)?\\s*(?<chromeProfPath>\"(?<profilePath>[^\"]+)\")?", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

        this.activitiesSchedule.clear();

        for (String s : schedules) {
            String add = s.trim();
            if ("".equals(add)) continue;

            Matcher startMatcher;
            try {
                startMatcher = scheduleRegex.matcher(s);
            } catch (PatternSyntaxException ex) {
                // Syntax error in the regular expression
                warningSettingLInes.add("Error when matching start time format for scheduling: " + add);
                continue;
            }

            if (!startMatcher.find()) {
                warningSettingLInes.add("Impossible to match start time format for scheduling: " + add);
            } else {
                // weekDay
                String weekDay = startMatcher.group("weekDay");

                // Start Time
                int startH = Integer.parseInt(startMatcher.group("startH"));
                int startM = Integer.parseInt(startMatcher.group("startM"));
                int startS = "".equals(startMatcher.group("secStartGrp")) || startMatcher.group("secStartGrp") == null ? 0 : Integer.parseInt(startMatcher.group("startS"));
                LocalTime startTime = LocalTime.of(startH, startM, startS);

                // End Time
                int endH = Integer.parseInt(startMatcher.group("endH"));
                int endM = Integer.parseInt(startMatcher.group("endM"));
                int endS = "".equals(startMatcher.group("secEndGrp")) || startMatcher.group("secEndGrp") == null ? 0 : Integer.parseInt(startMatcher.group("endS"));
                LocalTime endTime = LocalTime.of(endH, endM, endS);

                // Settings Plan
                String plan = "".equals(startMatcher.group("plan")) || startMatcher.group("plan") == null ? "" : startMatcher.group("plan");

                // Chrome Profile Path
                String profilePath = "".equals(startMatcher.group("chromeProfPath")) || startMatcher.group("chromeProfPath") == null ? "" : startMatcher.group("profilePath");

                if (startTime != null && endTime != null) {
                    if (endTime.compareTo(startTime) < 0) {
                        warningSettingLInes.add("End time is before start time for schedule setting: " + add);
                        continue;
                    }
                    activitiesSchedule.add(new ActivitiesScheduleSetting(weekDay, startTime, endTime, plan, profilePath));
                } else {
                    warningSettingLInes.add("Null startTime or endTime for setting: " + add);
                }
            }
        }
    }

    private void setScreenshots(String... types) {
        this.screenshots.clear();
        for (String t : types) {
            String add = t.trim();
            if ("".equals(add))
                continue;
            this.screenshots.add(add);
        }
    }

    private void setWorldBossNew(String... wbSettings) {
        char type;
        byte difficulty;
        byte tier;
        double chanceToRun;

        // Optional fields can be nullable so we don't use Java primitives
        Short timer;
        Boolean solo;
        Integer minimumTotalTS;
        Integer minimumPlayerTS;

        // We dinamically build the letter matching pattern for the regex so that we don't have to modify it when
        // a new world boss is released
        StringBuilder wbTypeString = new StringBuilder();

        wbTypeString.append("[");
        for (DungeonThread.WorldBoss wbType : DungeonThread.WorldBoss.values()) {
            // We skip the unknown boss letter
            if ("?".equals(wbType.getLetter())) continue;

            wbTypeString.append(wbType.getLetter());
        }
        wbTypeString.append("]");

        // \s*(?<type>[onm3bt])\s+(?<difficulty>[123])\s+(?<tier>\d{1,2})\s+(?<chanceToRun>\d*)\s*(?<timer>\d*)\s*(?<dungeonOnTimeout>[01])*\s*(?<solo>[01])*\s*(?<minimumTotalTS>\d+)*\s*(?<minimumPlayerTS>\d+)*
        //
        // Options: Case sensitive; Exact spacing; Dot doesn’t match line breaks; ^$ don’t match at line breaks; Default line breaks
        //
        // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s*»
        //    Between zero and unlimited times, as many times as possible, giving back as needed (greedy) «*»
        // Match the regex below and capture its match into a backreference named “type” (also backreference number 1) «(?<type>[onm3bt])»
        //    Match a single character from the list “onm3bt” (case sensitive) «[onm3bt]»
        // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s+»
        //    Between one and unlimited times, as many times as possible, giving back as needed (greedy) «+»
        // Match the regex below and capture its match into a backreference named “difficulty” (also backreference number 2) «(?<difficulty>[123])»
        //    Match a single character from the list “123” «[123]»
        // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s+»
        //    Between one and unlimited times, as many times as possible, giving back as needed (greedy) «+»
        // Match the regex below and capture its match into a backreference named “tier” (also backreference number 3) «(?<tier>\d{1,2})»
        //    Match a single character that is a “digit” (ASCII 0–9 only) «\d{1,2}»
        //       Between one and 2 times, as many times as possible, giving back as needed (greedy) «{1,2}»
        // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s+»
        //    Between one and unlimited times, as many times as possible, giving back as needed (greedy) «+»
        // Match the regex below and capture its match into a backreference named “chanceToRun” (also backreference number 4) «(?<chanceToRun>\d*)»
        //    Match a single character that is a “digit” (ASCII 0–9 only) «\d*»
        //       Between zero and unlimited times, as many times as possible, giving back as needed (greedy) «*»
        // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s*»
        //    Between zero and unlimited times, as many times as possible, giving back as needed (greedy) «*»
        // Match the regex below and capture its match into a backreference named “timer” (also backreference number 5) «(?<timer>\d*)»
        //    Match a single character that is a “digit” (ASCII 0–9 only) «\d*»
        //       Between zero and unlimited times, as many times as possible, giving back as needed (greedy) «*»
        // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s*»
        //    Between zero and unlimited times, as many times as possible, giving back as needed (greedy) «*»
        // Match the regex below and capture its match into a backreference named “dungeonOnTimeout” (also backreference number 6) «(?<dungeonOnTimeout>[01])*»
        //    Between zero and unlimited times, as many times as possible, giving back as needed (greedy) «*»
        //       You repeated the capturing group itself.  The group will capture only the last iteration.  Put a capturing group around the repeated group to capture all iterations. «*»
        //       Or, if you don’t want to capture anything, replace the capturing group with a non-capturing group to make your regex more efficient.
        //    Match a single character from the list “01” «[01]»
        // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s*»
        //    Between zero and unlimited times, as many times as possible, giving back as needed (greedy) «*»
        // Match the regex below and capture its match into a backreference named “solo” (also backreference number 7) «(?<solo>[01])*»
        //    Between zero and unlimited times, as many times as possible, giving back as needed (greedy) «*»
        //       You repeated the capturing group itself.  The group will capture only the last iteration.  Put a capturing group around the repeated group to capture all iterations. «*»
        //       Or, if you don’t want to capture anything, replace the capturing group with a non-capturing group to make your regex more efficient.
        //    Match a single character from the list “01” «[01]»
        // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s*»
        //    Between zero and unlimited times, as many times as possible, giving back as needed (greedy) «*»
        // Match the regex below and capture its match into a backreference named “minimumTotalTS” (also backreference number 8) «(?<minimumTotalTS>\d+)*»
        //    Between zero and unlimited times, as many times as possible, giving back as needed (greedy) «*»
        //       You repeated the capturing group itself.  The group will capture only the last iteration.  Put a capturing group around the repeated group to capture all iterations. «*»
        //       Or, if you don’t want to capture anything, replace the capturing group with a non-capturing group to make your regex more efficient.
        //    Match a single character that is a “digit” (ASCII 0–9 only) «\d+»
        //       Between one and unlimited times, as many times as possible, giving back as needed (greedy) «+»
        // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s*»
        //    Between zero and unlimited times, as many times as possible, giving back as needed (greedy) «*»
        // Match the regex below and capture its match into a backreference named “minimumPlayerTS” (also backreference number 9) «(?<minimumPlayerTS>\d+)*»
        //    Between zero and unlimited times, as many times as possible, giving back as needed (greedy) «*»
        //       You repeated the capturing group itself.  The group will capture only the last iteration.  Put a capturing group around the repeated group to capture all iterations. «*»
        //       Or, if you don’t want to capture anything, replace the capturing group with a non-capturing group to make your regex more efficient.
        //    Match a single character that is a “digit” (ASCII 0–9 only) «\d+»
        //       Between one and unlimited times, as many times as possible, giving back as needed (greedy) «+»

        Pattern wbRegex = Pattern.compile("\\s*(?<type>" + wbTypeString.toString() + ")\\s+(?<difficulty>[123])\\s+" +
                "(?<tier>\\d{1,2})\\s+(?<chanceToRun>\\d*)\\s*(?<timer>\\d*)\\s*" +
                "(?<solo>[01])*\\s*(?<minimumTotalTS>\\d+)*\\s*(?<minimumPlayerTS>\\d+)*");

        this.worldBossSettings.clear();
        for (String s : wbSettings) {
            String add = s.trim();
            if ("".equals(add))
                continue;

            Matcher wbMatcher;
            try {
                wbMatcher = wbRegex.matcher(add);
            } catch (PatternSyntaxException ex) {
                continue;
            }

            if (wbMatcher.find()) {
                type = wbMatcher.group("type").charAt(0);
                difficulty = Byte.parseByte(wbMatcher.group("difficulty"));
                tier = Byte.parseByte(wbMatcher.group("tier"));
                chanceToRun = Double.parseDouble(wbMatcher.group("chanceToRun"));

                // Nullable fields
                timer = "".equals(wbMatcher.group("timer")) || wbMatcher.group("timer") == null ? null : Short.parseShort(wbMatcher.group("timer"));
                solo = "".equals(wbMatcher.group("solo")) || wbMatcher.group("solo") == null ? null : "1".equals(wbMatcher.group("solo"));
                minimumTotalTS = "".equals(wbMatcher.group("minimumTotalTS")) || wbMatcher.group("minimumTotalTS") == null ? null : Integer.parseInt(wbMatcher.group("minimumTotalTS"));
                minimumPlayerTS = "".equals(wbMatcher.group("minimumPlayerTS")) || wbMatcher.group("minimumPlayerTS") == null ? null : Integer.parseInt(wbMatcher.group("minimumPlayerTS"));

                // Adding to the Random collection
                WorldBossSetting wbSetting = new WorldBossSetting(type, difficulty, tier, chanceToRun, timer, solo, minimumTotalTS, minimumPlayerTS);
                worldBossSettings.add(chanceToRun, wbSetting);
            } else {
                warningSettingLInes.add("Wrong format for worldBoss setting: '" + add + "'. Ignoring it.");
            }
        }
    }

    private void setExpeditions(String... expeditions) {
        this.expeditions.clear();
        double weight;
        String name;
        String[] config;

        for (String d : expeditions) {
            String add = d.trim();
            if ("".equals(add))
                continue;
            config = add.split(" ");
            weight = Double.parseDouble(config[2]);
            name = config[0] + " " + config[1];
            this.expeditions.add(weight, name);
        }
    }

    private void setAdventures(List<AdventureSetting> adventureList, String adventureName, String... adventures) {
        adventureList.clear();

        for (String a: adventures) {
            AdventureSetting adventureSetting = AdventureSetting.fromString(a);

            if (adventureSetting != null) {
                adventureList.add(adventureSetting);
            } else {
                warningSettingLInes.add("Unknown " + adventureName + " setting format: " + a);
            }
        }
    }

    private void setStrips(String... types) {
        this.pvpstrip.clear();
        for (String t : types) {
            String add = t.trim();
            if (add.equals(""))
                continue;
            this.pvpstrip.add(add);
        }
    }

    private void setAutoRevive(String... types) {
        this.autoRevive.clear();
        for (String t : types) {
            String add = t.trim();
            if (add.equals(""))
                continue;
            this.autoRevive.add(add);
        }
    }

    private void setPoNotifyDrop(String... types) {
        this.poNotifyDrop.clear();
        for (String t : types) {
            String add = t.trim();
            if (add.equals(""))
                continue;
            this.poNotifyDrop.add(add);
        }
    }

    private void setDiscordNotifyDrop(String... types) {
        this.discordNotifyDrop.clear();
        for (String t : types) {
            String add = t.trim();
            if (add.equals(""))
                continue;
            this.discordNotifyDrop.add(add);
        }
    }

    private void setTankPriority(String... types) {
        this.tankPriority.clear();
        for (String t : types) {
            String add = t.trim();
            if (add.equals(""))
                continue;
            this.tankPriority.add(add);
        }
    }

    private void setAutoShrine(String... types) {
        this.autoShrine.clear();
        for (String t : types) {
            String add = t.trim();
            if (add.equals(""))
                continue;
            this.autoShrine.add(add);
        }
    }

    private void setAutoRuneDefault(String... runes) {
        this.autoRuneDefault.clear();

        if (!runes[0].equals("")) this.autoRuneDefault.add(runes[0]);

        if (runes.length == 2) {
            if (!runes[1].equals("")) this.autoRuneDefault.add(runes[1]);
        } else {
            if (!runes[0].equals("")) this.autoRuneDefault.add(runes[0]);
        }
    }

    private void setDifficultyFailsafe(String... failSafes) {
        this.difficultyFailsafe.clear();
        // We only support Trial and Gauntlets and Expedition, so we do sanity checks here only settings the right letters t, g, e
        String pattern = "([tge]):([\\d]+)(:([\\d]+))?";
        Pattern r = Pattern.compile(pattern);
        for (String f : failSafes) {

            f = f.trim();

            Matcher m = r.matcher(f);
            if (m.find()) {
                int minimumDifficulty;

                if (m.group(4) != null) {
                    minimumDifficulty = Integer.parseInt(m.group(4));
                } else {
                    if ("e".equals(m.group(1))) {
                        minimumDifficulty = 5;
                    } else {
                        minimumDifficulty = 1;
                    }
                }

                Map.Entry<Integer, Integer> entry = Maps.immutableEntry(Integer.parseInt(m.group(2)), minimumDifficulty);
                difficultyFailsafe.put(m.group(1), entry);

            }
        }
    }

    private void setSuccessThreshold(String... tresholds) {
        this.successThreshold.clear();
        // We only support Trial and Gauntlets, so we do sanity checks here only settings the right letters t and g
        String tresholdPatternStr = "([tg]):([\\d]+):([\\d]+)";
        Pattern tresholdPattern = Pattern.compile(tresholdPatternStr);
        for (String t : tresholds) {
            Matcher m = tresholdPattern.matcher(t.trim());

            if (m.find()) {
                Map.Entry<Integer, Integer> entry = Maps.immutableEntry(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
                successThreshold.put(m.group(1), entry);
            }
        }
    }

    private void setAutoRune(String... runeSets) {
        this.autoRune.clear();
        String activityAutoRune;
        String[] configAutoRune;

        for (String d : runeSets) {
            configAutoRune = d.split(" +");
            if (configAutoRune.length < 2)
                continue;
            activityAutoRune = configAutoRune[0];
            List<String> runes = new ArrayList<>();
            runes.add(configAutoRune[1]);
            if (configAutoRune.length == 3) {
                runes.add(configAutoRune[2]);
            } else {
                runes.add(configAutoRune[1]);
            }
            this.autoRune.put(activityAutoRune, runes);
        }
    }

    private void setAutoBossRune(String... runeSets) {
        this.autoBossRune.clear();
        String activityAutoBossRune;
        String[] configAutoBossRune;

        for (String d : runeSets) {
            configAutoBossRune = d.split(" +");
            if (configAutoBossRune.length < 2)
                continue;
            activityAutoBossRune = configAutoBossRune[0];
            List<String> runes = new ArrayList<>();
            runes.add(configAutoBossRune[1]);
            if (configAutoBossRune.length == 3) {
                runes.add(configAutoBossRune[2]);
            } else {
                runes.add(configAutoBossRune[1]);
            }
            this.autoBossRune.put(activityAutoBossRune, runes);
        }
    }

    private void setGVGStrips(String... types) {
        this.gvgstrip.clear();
        for (String t : types) {
            String add = t.trim();
            if (add.equals(""))
                continue;
            this.gvgstrip.add(add);
        }
    }

    private void setConsumables(String... items) {
        this.consumables.clear();
        for (String i : items) {
            String add = i.trim();
            if (add.equals(""))
                continue;
            this.consumables.add(add);
        }
    }

    private void setFamiliars(String... fams) {
        this.familiars.clear();
        for (String f : fams) {
            String add = f.trim();
            if (add.equals(""))
                continue;
            this.familiars.add(add);
        }
    }

    /* Gets the string from the previous method and creates a list if there are multiple items */

    private String getactivitiesEnabledAsString() {
        StringBuilder result = new StringBuilder();
        for (String s : activitiesEnabled)
            result.append(s).append(" ");
        if (result.length() > 0)
            result = new StringBuilder(result.substring(0, result.length() - 1)); // remove last " " character
        return result.toString();
    }

    private String getWorldBossNewAsString() {
        StringBuilder result = new StringBuilder();

        for (WorldBossSetting s : worldBossSettings) {
            if (result.length() > 0) result.append(";");

            result.append(s.toString());
        }
        return result.toString();
    }

    private String getScreenshotsAsString() {
        StringBuilder result = new StringBuilder();
        for (String s : screenshots)
            result.append(s).append(" ");
        if (result.length() > 0)
            result = new StringBuilder(result.substring(0, result.length() - 1)); // remove last " " character
        return result.toString();
    }

    private String getExpeditionsAsString() {
        return expeditions.toString();
    }

    private String getAdventuresAsString(List<AdventureSetting> adventureSettingList) {
        StringBuilder settingBuilder = new StringBuilder();
        for (AdventureSetting s : adventureSettingList) {
            if (settingBuilder.length() > 0) settingBuilder.append(";");

            settingBuilder.append(s.toString());
        }

        return settingBuilder.toString();
    }

    private String getStripsAsString() {
        StringBuilder result = new StringBuilder();
        for (String s : pvpstrip)
            result.append(s).append(" ");
        if (result.length() > 0)
            result = new StringBuilder(result.substring(0, result.length() - 1)); // remove last " " character
        return result.toString();
    }

    private String getAutoReviveAsString() {
        StringBuilder result = new StringBuilder();
        for (String s : autoRevive)
            result.append(s).append(" ");
        if (result.length() > 0)
            result = new StringBuilder(result.substring(0, result.length() - 1)); // remove last " " character
        return result.toString();
    }

    private String getPoNotifyDropAsString() {
        StringBuilder result = new StringBuilder();
        for (String s : poNotifyDrop)
            result.append(s).append(" ");
        if (result.length() > 0)
            result = new StringBuilder(result.substring(0, result.length() - 1)); // remove last " " character
        return result.toString();
    }

    private String getDiscordNotifyDropAsString() {
        StringBuilder result = new StringBuilder();
        for (String s : discordNotifyDrop)
            result.append(s).append(" ");
        if (result.length() > 0)
            result = new StringBuilder(result.substring(0, result.length() - 1)); // remove last " " character
        return result.toString();
    }

    private String getTankPriorityAsString() {
        StringBuilder result = new StringBuilder();
        for (String s : tankPriority)
            result.append(s).append(" ");
        if (result.length() > 0)
            result = new StringBuilder(result.substring(0, result.length() - 1)); // remove last " " character
        return result.toString();
    }

    private String getAutoShrineAsString() {
        StringBuilder result = new StringBuilder();
        for (String s : autoShrine)
            result.append(s).append(" ");
        if (result.length() > 0)
            result = new StringBuilder(result.substring(0, result.length() - 1)); // remove last " " character
        return result.toString();
    }

    private String getAutoRuneDefaultAsString() {
        StringBuilder result = new StringBuilder();
        for (String s : autoRuneDefault)
            result.append(s).append(" ");
        if (result.length() > 0)
            result = new StringBuilder(result.substring(0, result.length() - 1)); // remove last " " character
        return result.toString();
    }

    private String getAutoRuneAsString() {
        List<String> actionList = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : autoRune.entrySet()) {
            actionList.add(entry.getKey() + " " + String.join(" ", entry.getValue()));
        }
        return String.join("; ", actionList);
    }


    private String getAutoBossRuneAsString() {
        List<String> actionList = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : autoBossRune.entrySet()) {
            actionList.add(entry.getKey() + " " + String.join(" ", entry.getValue()));
        }
        return String.join("; ", actionList);
    }

    private String getDifficultyFailsafeAsString() {
        StringBuilder dfsBuilder = new StringBuilder();
        for (Map.Entry<String, Map.Entry<Integer, Integer>> entry : difficultyFailsafe.entrySet()) {
            if (dfsBuilder.length() > 0) dfsBuilder.append(" ");
            dfsBuilder.append(entry.getKey())
                    .append(":")
                    .append(entry.getValue().getKey())
                    .append(":")
                    .append(entry.getValue().getValue());
        }

        return dfsBuilder.toString();
    }

    private String getSuccessThresholdAsString() {
        StringBuilder sfsBuilder = new StringBuilder();
        for (Map.Entry<String, Map.Entry<Integer, Integer>> entry : successThreshold.entrySet()) {
            if (sfsBuilder.length() > 0) sfsBuilder.append(" ");
            sfsBuilder.append(entry.getKey())
                    .append(":")
                    .append(entry.getValue().getKey())
                    .append(":")
                    .append(entry.getValue().getValue());
        }

        return sfsBuilder.toString();
    }

    private String getGVGStripsAsString() {
        StringBuilder result = new StringBuilder();
        for (String s : gvgstrip)
            result.append(s).append(" ");
        if (result.length() > 0)
            result = new StringBuilder(result.substring(0, result.length() - 1)); // remove last " " character
        return result.toString();
    }

    private String getConsumablesAsString() {
        StringBuilder result = new StringBuilder();
        for (String s : consumables)
            result.append(s).append(" ");
        if (result.length() > 0)
            result = new StringBuilder(result.substring(0, result.length() - 1)); // remove last " " character
        return result.toString();
    }

    private String getFamiliarsAsString() {
        StringBuilder result = new StringBuilder();
        for (String f : familiars)
            result.append(f).append(";");
        if (result.length() > 0)
            result = new StringBuilder(result.substring(0, result.length() - 1)); // remove last ";" character
        return result.toString();
    }

    private String getLogLevelAsString() {
        return logLevel.toString();
    }

    /* Cleans up the data in the list again */

    private void setactivitiesEnabledFromString(String s) {
        setactivitiesEnabled(s.split(" "));
    }

    private void setActivitiesScheduleFromString(String s) {
        setActivitiesSchedule(s.split(";"));
    }

    private void setScreenshotsFromString(String s) {
        setScreenshots(s.split(" "));
    }

    private void setWorldBossNewFromString(String s) {
        setWorldBossNew(s.split(";"));
    }

    private void setDungeonsFromString(String s) {
        setAdventures(this.dungeons, "dungeon", s.split(";"));
    }

    private void setExpeditionsFromString(String s) {
        setExpeditions(s.split(";"));
    }

    private void setRaidsFromString(String s) {
        setAdventures(this.raids, "raid", s.split(";"));
    }

    private void setStripsFromString(String s) {
        setStrips(s.split(" "));
        // clean up (trailing spaces and remove if empty):
        for (int i = pvpstrip.size() - 1; i >= 0; i--) {
            pvpstrip.set(i, pvpstrip.get(i).trim());
            if (pvpstrip.get(i).equals(""))
                pvpstrip.remove(i);
        }
    }

    private void setAutoReviveFromString(String s) {
        setAutoRevive(s.split(" "));
        // clean up (trailing spaces and remove if empty):
        for (int i = autoRevive.size() - 1; i >= 0; i--) {
            autoRevive.set(i, autoRevive.get(i).trim());
            if (autoRevive.get(i).equals(""))
                autoRevive.remove(i);
        }
    }

    private void setPoNotifyDropFromString(String s) {
        setPoNotifyDrop(s.split(" "));
        // clean up (trailing spaces and remove if empty):
        for (int i = poNotifyDrop.size() - 1; i >= 0; i--) {
            poNotifyDrop.set(i, poNotifyDrop.get(i).trim());
            if (poNotifyDrop.get(i).equals(""))
                poNotifyDrop.remove(i);
        }
    }

    private void setDiscordNotifyDropFromString(String s) {
        setDiscordNotifyDrop(s.split(" "));
        // clean up (trailing spaces and remove if empty):
        for (int i = discordNotifyDrop.size() - 1; i >= 0; i--) {
            discordNotifyDrop.set(i, discordNotifyDrop.get(i).trim());
            if (discordNotifyDrop.get(i).equals(""))
                discordNotifyDrop.remove(i);
        }
    }

    private void setTankPriorityFromString(String s) {
        setTankPriority(s.split(" "));
        // clean up (trailing spaces and remove if empty):
        for (int i = tankPriority.size() - 1; i >= 0; i--) {
            tankPriority.set(i, tankPriority.get(i).trim());
            if (tankPriority.get(i).equals(""))
                tankPriority.remove(i);
        }
    }

    private void setAutoShrineFromString(String s) {
        setAutoShrine(s.split(" "));
        // clean up (trailing spaces and remove if empty):
        for (int i = autoShrine.size() - 1; i >= 0; i--) {
            autoShrine.set(i, autoShrine.get(i).trim());
            if (autoShrine.get(i).equals(""))
                autoShrine.remove(i);
        }
    }

    private void setAutoRuneDefaultFromString(String s) {
        setAutoRuneDefault(s.trim().split(" +"));
    }

    private void setAutoRuneFromString(String s) {
        setAutoRune(s.trim().split(" *; *"));
    }

    private void setAutoBossRuneFromString(String s) {
        setAutoBossRune(s.trim().split(" *; *"));
    }

    private void setDifficultyFailsafeFromString(String s) {
        setDifficultyFailsafe(s.trim().split(" "));
    }

    private void setSuccessThresholdFromString(String s) {
        setSuccessThreshold(s.trim().split(" "));
    }

    private void setGVGStripsFromString(String s) {
        setGVGStrips(s.split(" "));
        // clean up (trailing spaces and remove if empty):
        for (int i = gvgstrip.size() - 1; i >= 0; i--) {
            gvgstrip.set(i, gvgstrip.get(i).trim());
            if (gvgstrip.get(i).equals(""))
                gvgstrip.remove(i);
        }
    }

    private void setConsumablesFromString(String s) {
        setConsumables(s.split(" "));
        // clean up (trailing spaces and remove if empty):
        for (int i = consumables.size() - 1; i >= 0; i--) {
            consumables.set(i, consumables.get(i).trim());
            if (consumables.get(i).equals(""))
                consumables.remove(i);
        }
    }

    private void setFamiliarsFromString(String s) {
        setFamiliars(s.split(";"));
        // clean up (trailing spaces and remove if empty):
        for (int i = familiars.size() - 1; i >= 0; i--) {
            familiars.set(i, familiars.get(i).trim());
            if (familiars.get(i).equals(""))
                familiars.remove(i);
        }
    }

    private void setLogLevelFromString(String level) {
        switch (level.toUpperCase()) {
            case "TRACE":
            case "DEBUG":
            case "INFO":
            case "WARN":
            case "ERROR":
            case "FATAL":
            case "OFF":
            case "ALL":
                logLevel = Level.toLevel(level);
                break;
            default:
                logLevel = Level.toLevel("INFO");
                break;
        }
    }

    /**
     * Loads settings from list of string arguments (which are lines of the settings.ini file, for example)
     */
    void load(List<String> lines, boolean reset, String sourceFile) {

        // As different profile may use different configurations, we make sure that everytime lastUsedMap is cleared
        if (reset) {
            setDefault();
            lastUsedMap.clear();
        }

        for (String line : lines) {
            if (line.trim().equals("")) continue;
            if (line.startsWith("#")) continue; // a comment
            try {
                lastUsedMap.put(line.substring(0, line.indexOf(" ")), line.substring(line.indexOf(" ") + 1));
            } catch (StringIndexOutOfBoundsException e) {
                wrongSettingLines.add(line);
            }
        }

        username = lastUsedMap.getOrDefault("username", username);
        password = lastUsedMap.getOrDefault("password", password);
        poAppToken = lastUsedMap.getOrDefault("poAppToken", poAppToken);
        poUserToken = lastUsedMap.getOrDefault("poUserToken", poUserToken);
        discordWebHookUrl = lastUsedMap.getOrDefault("discordWebHookUrl", discordWebHookUrl);
        discordUserName = lastUsedMap.getOrDefault("discordUserName", discordUserName);
        useHeadlessMode = lastUsedMap.getOrDefault("headlessmode", useHeadlessMode ? "1" : "0").equals("1");
        restartAfterAdOfferTimeout = lastUsedMap.getOrDefault("restartAfterAdOfferTimeout", restartAfterAdOfferTimeout ? "1" : "0").equals("1");
        debugDetectionTimes = lastUsedMap.getOrDefault("debugDetectionTimes", debugDetectionTimes ? "1" : "0").equals("1");
        useDoNotShareURL = lastUsedMap.getOrDefault("useDoNotShareURL", useDoNotShareURL ? "1" : "0").equals("1");
        hideWindowOnRestart = lastUsedMap.getOrDefault("hideWindowOnRestart", hideWindowOnRestart ? "1" : "0").equals("1");
        resetTimersOnBattleEnd = lastUsedMap.getOrDefault("resetTimersOnBattleEnd", resetTimersOnBattleEnd ? "1" : "0").equals("1");
        autoStartChromeDriver = lastUsedMap.getOrDefault("autoStartChromeDriver", autoStartChromeDriver ? "1" : "0").equals("1");
        reconnectTimer = Integer.parseInt(lastUsedMap.getOrDefault("reconnectTimer", "" + reconnectTimer));
        setScreenshotsFromString(lastUsedMap.getOrDefault("screenshots", getScreenshotsAsString()));

        setactivitiesEnabledFromString(lastUsedMap.getOrDefault("activitiesEnabled", getactivitiesEnabledAsString()));

        // Scheduling can only be changed by the original configuratin file, not by plans
        if (sourceFile.equals(Settings.initialConfigurationFile)) {
            setActivitiesScheduleFromString(lastUsedMap.getOrDefault("activitiesSchedule", activitiesSchedule.toString()));
            defaultActivitiesSchedule = activitiesSchedule;
        }
        activitiesRoundRobin = lastUsedMap.getOrDefault("activitiesRoundRobin", activitiesRoundRobin ? "1" : "0").equals("1");

        enablePushover = lastUsedMap.getOrDefault("enablePushover", enablePushover ? "1" : "0").equals("1");
        poNotifyPM = lastUsedMap.getOrDefault("poNotifyPM", poNotifyPM ? "1" : "0").equals("1");
        poNotifyCrash = lastUsedMap.getOrDefault("poNotifyCrash", poNotifyCrash ? "1" : "0").equals("1");
        poNotifyErrors = lastUsedMap.getOrDefault("poNotifyErrors", poNotifyErrors ? "1" : "0").equals("1");
        poNotifyBribe = lastUsedMap.getOrDefault("poNotifyBribe", poNotifyBribe ? "1" : "0").equals("1");

        enableDiscord = lastUsedMap.getOrDefault("enableDiscord", enableDiscord ? "1" : "0").equals("1");
        discordNotifyPM = lastUsedMap.getOrDefault("discordNotifyPM", discordNotifyPM ? "1" : "0").equals("1");
        discordNotifyCrash = lastUsedMap.getOrDefault("discordNotifyCrash", discordNotifyCrash ? "1" : "0").equals("1");
        discordNotifyErrors = lastUsedMap.getOrDefault("discordNotifyErrors", discordNotifyErrors ? "1" : "0").equals("1");
        discordNotifyBribe = lastUsedMap.getOrDefault("discordNotifyBribe", discordNotifyBribe ? "1" : "0").equals("1");

        maxShards = Integer.parseInt(lastUsedMap.getOrDefault("maxShards", "" + maxShards));
        maxTokens = Integer.parseInt(lastUsedMap.getOrDefault("maxTokens", "" + maxTokens));
        maxTickets = Integer.parseInt(lastUsedMap.getOrDefault("maxTickets", "" + maxTickets));
        maxBadges = Integer.parseInt(lastUsedMap.getOrDefault("maxBadges", "" + maxBadges));
        maxXeals = Integer.parseInt(lastUsedMap.getOrDefault("maxXeals", "" + maxXeals));

        minShards = Integer.parseInt(lastUsedMap.getOrDefault("minShards", "" + minShards));
        minTokens = Integer.parseInt(lastUsedMap.getOrDefault("minTokens", "" + minTokens));
        minEnergyPercentage = Integer.parseInt(lastUsedMap.getOrDefault("minEnergyPercentage", "" + minEnergyPercentage));
        minTickets = Integer.parseInt(lastUsedMap.getOrDefault("minTickets", "" + minTickets));
        minBadges = Integer.parseInt(lastUsedMap.getOrDefault("minBadges", "" + minBadges));
        minXeals = Integer.parseInt(lastUsedMap.getOrDefault("minXeals", "" + minXeals));

        poNotifyAlive = Integer.parseInt(lastUsedMap.getOrDefault("poNotifyAlive", "" + poNotifyAlive));
        discordNotifyAlive = Integer.parseInt(lastUsedMap.getOrDefault("discordNotifyAlive", "" + discordNotifyAlive));

        costPVP = Integer.parseInt(lastUsedMap.getOrDefault("costPVP", "" + costPVP));
        costGVG = Integer.parseInt(lastUsedMap.getOrDefault("costGVG", "" + costGVG));
        costTrials = Integer.parseInt(lastUsedMap.getOrDefault("costTrials", "" + costTrials));
        costGauntlet = Integer.parseInt(lastUsedMap.getOrDefault("costGauntlet", "" + costGauntlet));
        costInvasion = Integer.parseInt(lastUsedMap.getOrDefault("costInvasion", "" + costInvasion));
        costExpedition = Integer.parseInt(lastUsedMap.getOrDefault("costExpedition", "" + costExpedition));

        setWorldBossNewFromString(lastUsedMap.getOrDefault("worldBoss", getWorldBossNewAsString()));
        debugWBTS = lastUsedMap.getOrDefault("debugWBTS", debugWBTS ? "1" : "0").equals("1");

        setAutoReviveFromString(lastUsedMap.getOrDefault("autoRevive", getAutoReviveAsString()));
        setPoNotifyDropFromString(lastUsedMap.getOrDefault("poNotifyDrop", getPoNotifyDropAsString()));
        setDiscordNotifyDropFromString(lastUsedMap.getOrDefault("discordNotifyDrop", getDiscordNotifyDropAsString()));
        setTankPriorityFromString(lastUsedMap.getOrDefault("tankPriority", getTankPriorityAsString()));
        tankPosition = Integer.parseInt(lastUsedMap.getOrDefault("tankPosition", "" + tankPosition));
        potionOrder = lastUsedMap.getOrDefault("potionOrder", potionOrder);
        potionLimit = Integer.parseInt(lastUsedMap.getOrDefault("potionLimit", "" + potionLimit));

        pvpOpponent = Integer.parseInt(lastUsedMap.getOrDefault("pvpOpponent", "" + pvpOpponent));
        gvgOpponent = Integer.parseInt(lastUsedMap.getOrDefault("gvgOpponent", "" + gvgOpponent));
        difficultyTrials = Integer.parseInt(lastUsedMap.getOrDefault("difficultyTrials", "" + difficultyTrials));
        difficultyGauntlet = Integer.parseInt(lastUsedMap.getOrDefault("difficultyGauntlet", "" + difficultyGauntlet));

        setDungeonsFromString(lastUsedMap.getOrDefault("dungeons", getAdventuresAsString(this.dungeons)));
        setRaidsFromString(lastUsedMap.getOrDefault("raids", getAdventuresAsString(this.raids)));
        setExpeditionsFromString(lastUsedMap.getOrDefault("expeditions", getExpeditionsAsString()));
        setStripsFromString(lastUsedMap.getOrDefault("pvpstrip", getStripsAsString()));
        setGVGStripsFromString(lastUsedMap.getOrDefault("gvgstrip", getGVGStripsAsString()));

        autoConsume = lastUsedMap.getOrDefault("autoconsume", autoConsume ? "1" : "0").equals("1");
        setConsumablesFromString(lastUsedMap.getOrDefault("consumables", getConsumablesAsString()));

        contributeFamiliars = lastUsedMap.getOrDefault("contributeFamiliars", contributeFamiliars ? "1" : "0").equals("1");
        victoryScreenshot = lastUsedMap.getOrDefault("victoryScreenshot", victoryScreenshot ? "1" : "0").equals("1");
        setFamiliarsFromString(lastUsedMap.getOrDefault("familiars", getFamiliarsAsString()));
        familiarScreenshot = Integer.parseInt(lastUsedMap.getOrDefault("familiarScreenshot", "" + familiarScreenshot));

        openSkeleton = Integer.parseInt(lastUsedMap.getOrDefault("openSkeletonChest", "" + openSkeleton));

        setAutoShrineFromString(lastUsedMap.getOrDefault("autoShrine", getAutoShrineAsString()));
        battleDelay = Integer.parseInt(lastUsedMap.getOrDefault("battleDelay", "" + battleDelay));
        shrineDelay = Integer.parseInt(lastUsedMap.getOrDefault("shrineDelay", "" + shrineDelay));
        positionDelay = Integer.parseInt(lastUsedMap.getOrDefault("positionDelay", "" + positionDelay));

        setAutoRuneDefaultFromString(lastUsedMap.getOrDefault("autoRuneDefault", getAutoRuneDefaultAsString()));
        setAutoRuneFromString(lastUsedMap.getOrDefault("autoRune", getAutoRuneAsString()));
        setAutoBossRuneFromString(lastUsedMap.getOrDefault("autoBossRune", getAutoBossRuneAsString()));

        setDifficultyFailsafeFromString(lastUsedMap.getOrDefault("difficultyFailsafe", getDifficultyFailsafeAsString()));
        setSuccessThresholdFromString(lastUsedMap.getOrDefault("successThreshold", getSuccessThresholdAsString()));

        persuasionLevel = Integer.parseInt(lastUsedMap.getOrDefault("persuasionLevel", "" + persuasionLevel));
        bribeLevel = Integer.parseInt(lastUsedMap.getOrDefault("bribeLevel", "" + bribeLevel));

        enterGuildHall = lastUsedMap.getOrDefault("enterGuildHall", enterGuildHall ? "1" : "0").equals("1");
        baitAmount = Integer.parseInt(lastUsedMap.getOrDefault("baitAmount", "" + baitAmount));

        logMaxDays = Integer.parseInt(lastUsedMap.getOrDefault("logMaxDays", "" + logMaxDays));
        logBaseDir = lastUsedMap.getOrDefault("logBaseDir", logBaseDir);
        setLogLevelFromString(lastUsedMap.getOrDefault("logLevel", getLogLevelAsString()));
        logPringStatusMessages = lastUsedMap.getOrDefault("logPringStatusMessages", logPringStatusMessages ? "1" : "0").equals("1");
    }

    /**
     * Loads settings from disk.
     */
    void load() {
        try {
            load(configurationFile);
        } catch (FileNotFoundException e) {
            BHBot.logger.error("It was impossible to load settings from " + configurationFile + ".");
        }
        checkDeprecatedSettings();
        sanitizeSetting();
    }

    /**
     * Loads settings from disk.
     */
    void load(String file) throws FileNotFoundException {

        // If we are reloading a file that is different from the initial one, we always make sure to also reload
        // the original one as some settings can only be changed on the original one e.g.: schedulings
        if (!file.equals(Settings.initialConfigurationFile)) {
            List<String> lines = Misc.fileToList(Settings.initialConfigurationFile);
            if (lines != null && lines.size() > 0) {
                load(lines, true, Settings.initialConfigurationFile);
            }
        }

        List<String> lines = Misc.fileToList(file);
        if (lines == null || lines.size() == 0) return;

        load(lines, true, file);
    }

    boolean checkUnsupportedSettings() {
        boolean result = true;

        if (lastUsedMap.getOrDefault("worldBossTimer", null) != null) {
            BHBot.logger.error("Unsupported 'worldBossTimer " + lastUsedMap.get("worldBossTimer") + "' setting detected: use the new World Boss setting format and restart the bot.");
            result = false;
        }
        if (lastUsedMap.getOrDefault("dungeonOnTimeout", null) != null) {
            BHBot.logger.error("Unsupported 'dungeonOnTimeout " + lastUsedMap.get("dungeonOnTimeout") + "' setting detected: use the new World Boss setting format and restart the bot.");
            result = false;
        }
        if (lastUsedMap.getOrDefault("worldBossSolo", null) != null) {
            BHBot.logger.error("Unsupported 'worldBossSolo " + lastUsedMap.get("worldBossSolo") + "' setting detected: use the new World Boss setting format and restart the bot.");
            result = false;
        }

        // We check if the worldBoss setting has the old format
        if (lastUsedMap.getOrDefault("worldBoss", null) != null && !lastUsedMap.get("worldBoss").contains(";")) {
            try {
                // \s*[onm3bt]\s[123]\s\d{1,2}\s*$
                //
                // Options: Case sensitive; Exact spacing; Dot doesn’t match line breaks; ^$ don’t match at line breaks; Default line breaks
                //
                // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s*»
                //    Between zero and unlimited times, as many times as possible, giving back as needed (greedy) «*»
                // Match a single character from the list “onm3bt” (case sensitive) «[onm3bt]»
                // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s»
                // Match a single character from the list “123” «[123]»
                // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s»
                // Match a single character that is a “digit” (ASCII 0–9 only) «\d{1,2}»
                //    Between one and 2 times, as many times as possible, giving back as needed (greedy) «{1,2}»
                // Match a single character that is a “whitespace character” (ASCII space, tab, line feed, carriage return, vertical tab, form feed) «\s*»
                //    Between zero and unlimited times, as many times as possible, giving back as needed (greedy) «*»
                // Assert position at the end of the string, or before the line break at the end of the string, if any (carriage return and line feed, next line, line separator, paragraph separator) «$»

                boolean isOldFormat = lastUsedMap.get("worldBoss").matches("\\s*[onm3bt]\\s[123]\\s\\d{1,2}\\s*$");
                if (isOldFormat) {
                    BHBot.logger.error("Unsupported format setting 'worldBoss " + lastUsedMap.get("worldBoss") + "' Please review the documentation and update to the new format.");
                    result = false;
                }
            } catch (PatternSyntaxException ex) {
                BHBot.logger.warn("Error while checking the worldBoss setting format");
            }
        }

        return result;
    }

    void checkDeprecatedSettings() {
        if (lastUsedMap.getOrDefault("autoBribe", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: autoBribe. Ignoring it, use a combination of bribeLevel and familiars instead.");
        }

        if (lastUsedMap.getOrDefault("pauseOnDisconnect", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: pauseOnDisconnect. Ignoring it, use reconnectTimer instead.");
        }

        if (lastUsedMap.getOrDefault("expeditionDifficulty", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: expeditionDifficulty. Use the new expedition format instead.");
        }
        if (lastUsedMap.getOrDefault("experimentalAutoRevive", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: experimentalAutoRevive. Old revive system is no longer available.");
        }
        if (lastUsedMap.getOrDefault("worldBossType", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: worldBossType. Use the new World Boss format instead.");
        }
        if (lastUsedMap.getOrDefault("worldBossDifficulty", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: worldBossDifficulty. Use the new World Boss format instead.");
        }
        if (lastUsedMap.getOrDefault("difficulty", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: difficulty. Use the new difficultyTrials and difficultyGauntlet.");
        }
        if (lastUsedMap.getOrDefault("worldBossTier", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: worldBossTier. Use the new World Boss format instead.");
        }
        if (lastUsedMap.getOrDefault("doRaids", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: doRaids. Use the new activitiesEnabled with 'r' letter instead.");
        }
        if (lastUsedMap.getOrDefault("doDungeons", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: doDungeons. Use the new activitiesEnabled with 'd' letter instead.");
        }
        if (lastUsedMap.getOrDefault("doWorldBoss", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: doWorldBoss. Use the new activitiesEnabled with 'w' letter instead.");
        }
        if (lastUsedMap.getOrDefault("doTrials", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: doTrials. Use the new activitiesEnabled with 't' letter instead.");
        }
        if (lastUsedMap.getOrDefault("doGauntlet", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: doGauntlet. Use the new activitiesEnabled with 'g' letter instead.");
        }
        if (lastUsedMap.getOrDefault("doPVP", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: doPVP. Use the new activitiesEnabled with 'p' letter instead.");
        }
        if (lastUsedMap.getOrDefault("doExpedition", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: doExpedition. Use the new activitiesEnabled with 'e' letter instead.");
        }
        if (lastUsedMap.getOrDefault("doInvasion", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: doInvasion. Use the new activitiesEnabled with 'i' letter instead.");
        }
        if (lastUsedMap.getOrDefault("doGVG", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: doGVG. Use the new activitiesEnabled with 'v' letter instead.");
        }
        if (lastUsedMap.getOrDefault("collectBounties", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: collectBounties. Use the new activitiesEnabled with 'b' letter instead.");
        }
        if (lastUsedMap.getOrDefault("difficulty", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: difficulty. Please replace with difficultyTrials and difficultyGauntlet instead.");
        }
        if (lastUsedMap.getOrDefault("collectFishingBaits", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: collectFishingBaits. Use the new activitiesEnabled with 'f' letter instead.");
        }
        if (lastUsedMap.getOrDefault("thursdayRaids", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: thursdayRaids. Use the new wednesdayRaids instead.");
        }
        if (lastUsedMap.getOrDefault("thursdayDungeons", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: thursdayDungeons. Use the new wednesdayDungeons instead.");
        }

        if (lastUsedMap.getOrDefault("wednesdayRaids", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: wednesdayRaids. Use the new raids setting format instead.");
        }

        if (lastUsedMap.getOrDefault("wednesdayDungeons", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: wednesdayDungeons. Use the new dungeons setting format instead.");
        }

        if (lastUsedMap.getOrDefault("minSolo", null) != null) {
            BHBot.logger.warn("Deprecated setting detected: minSolo. Use the new dungeons setting format instead.");
        }
    }

    void sanitizeSetting() {
        // we check and sanitize expeditions values
        String expeditions = lastUsedMap.getOrDefault("expeditions", "");
        if (expeditions.contains("h") || expeditions.contains("i")) {
            BHBot.logger.warn("WARNING: invalid format detected for expeditions settings '" + expeditions + "': " +
                    "a standard value of 'p1 100 100' will be used");
            lastUsedMap.put("expeditions", "p1 100 25;p2 100 25;p3 100 25;p4 100 25");
            setExpeditionsFromString("p1 100 25;p2 100 25;p3 100 25;p4 100 25");
        }

        // sanitize autorevive settings
        String autoRevive = lastUsedMap.getOrDefault("autoRevive", "");
        if (autoRevive.contains("1") || autoRevive.contains("2") || autoRevive.contains("3")) {
            BHBot.logger.warn("WARNING: invalid format detected for autoRevive setting '" + autoRevive + "': " +
                    "this feature will be disabled");
            lastUsedMap.put("autoRevive", "");
            setAutoReviveFromString("");
        }

        // sanitize autoshrine settings
        String autoShrine = lastUsedMap.getOrDefault("autoShrine", "");
        if (autoShrine.contains("1") || autoShrine.contains("2") || autoShrine.contains("3")) {
            BHBot.logger.warn("WARNING: invalid format detected for autoShrine setting '" + autoShrine + "': " +
                    "this feature will be disabled");
            lastUsedMap.put("autoShrine", "");
            setAutoShrineFromString("");
        }

        // sanitize tankPriority settings
        String tankPriority = lastUsedMap.getOrDefault("tankPriority", "");
        if (tankPriority.contains("1") || tankPriority.contains("2") || tankPriority.contains("3")) {
            BHBot.logger.warn("WARNING: invalid format detected for tankPriority setting '" + tankPriority + "': " +
                    "this feature will be disabled");
            lastUsedMap.put("tankPriority", "");
            setTankPriorityFromString("");
        }

        // sanitize pvpOpponent setting
        int pvpOpponentTmp = Integer.parseInt(lastUsedMap.getOrDefault("pvpOpponent", "" + pvpOpponent));
        if (pvpOpponentTmp < 1 || pvpOpponent > 4) {
            BHBot.logger.warn("WARNING: invalid value for pvpOpponent setting '" + pvpOpponentTmp + "': " +
                    "setting it to '1'");
            lastUsedMap.put("pvpOpponent", "1");
            pvpOpponent = 1;
        }

        // sanitize gvgOpponent setting
        int gvgOpponentTmp = Integer.parseInt(lastUsedMap.getOrDefault("gvgOpponent", "" + gvgOpponent));
        if (gvgOpponentTmp < 1 || gvgOpponent > 4) {
            BHBot.logger.warn("WARNING: invalid value for gvgOpponent setting '" + gvgOpponentTmp + "': " +
                    "setting it to '1'");
            lastUsedMap.put("gvgOpponent", "1");
            gvgOpponent = 1;
        }

        // sanitize autorune-related settings
        String runeTypes = "(capture|experience|gold|item_find)";
        String runeActions = "[degiprtwv]";
        // match one or two rune specs
        String runeRegex = runeTypes + "(\\s+" + runeTypes + ")?";
        // match one or more actions, each followed by one or two runes
        String runeActionRegex = runeActions + " +" + runeRegex + "( *; *" + runeActions + " +" + runeRegex + ")*";

        // autoRune defaults
        String autoRuneDefault = lastUsedMap.getOrDefault("autoRuneDefault", "");
        if (!autoRuneDefault.equals("") && !autoRuneDefault.matches(runeRegex)) {
            BHBot.logger.warn("WARNING: invalid format detected for autoRuneDefault setting '" + autoRuneDefault + "': " +
                    "this feature will be disabled");
            lastUsedMap.put("autoRuneDefault", "");
            setAutoRuneDefaultFromString("");
        }

        // autoRunes
        String autoRune = lastUsedMap.getOrDefault("autoRune", "");
        if (!autoRune.equals("") && !autoRune.matches(runeActionRegex)) {
            BHBot.logger.warn("WARNING: invalid format detected for autoRune setting '" + autoRune + "': " +
                    "this feature will be disabled" + ": " + runeActionRegex);
            lastUsedMap.put("autoRune", "");
            setAutoRuneFromString("");
        }

        // autoBossRunes
        String autoBossRune = lastUsedMap.getOrDefault("autoBossRune", "");
        if (!autoBossRune.equals("") && !autoBossRune.matches(runeActionRegex)) {
            BHBot.logger.warn("WARNING: invalid format detected for autoBossRune setting '" + autoBossRune + "': " +
                    "this feature will be disabled");
            lastUsedMap.put("autoBossRune", "");
            setAutoBossRuneFromString("");
        }
    }

    static void resetIniFile() throws IOException {
        ClassLoader classLoader = Settings.class.getClassLoader();
        InputStream resourceURL = classLoader.getResourceAsStream(Settings.configurationFile);

        File iniFile = new File(Settings.configurationFile);

        if (resourceURL != null) {
            Files.copy(resourceURL, iniFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            String fileCreatedMsg = "Standard ini setting file created in '" + iniFile.getPath() + "' please review it and start the bot again.";
            if (BHBot.logger != null) {
                BHBot.logger.info(fileCreatedMsg);
            } else {
                System.out.println(fileCreatedMsg);
            }
            resourceURL.close();
        } else {
            String nullResourceMsg = "Impossible to load standard ini setting file from resources!";

            if (BHBot.logger != null) {
                BHBot.logger.error(nullResourceMsg);
            } else {
                System.out.println(nullResourceMsg);
            }
        }
    }
}
