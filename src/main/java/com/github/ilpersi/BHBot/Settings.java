package com.github.ilpersi.BHBot;

import com.google.common.collect.Maps;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Settings {
    static String configurationFile = "settings.ini";

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
    boolean dungeonOnTimeout = true;
    LinkedHashSet<String> screenshots;

    //activity settings
    LinkedHashSet<String> activitiesEnabled;
    boolean activitiesRoundRobin = true;

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
    // Max for various expendables for correct calculation if not default
    int maxShards = 4;
    int maxTokens = 10;
    int maxTickets = 10;
    int maxBadges = 10;
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
    RandomCollection<String> dungeons;
    RandomCollection<String> wednesdayDungeons;
    /**
     * List of raids we want to do with a difficulty level and percentage.
     * Examples:
     * '1 3 70;2 1 30' ==> in 70% of cases it will do R1 on heroic, in 30% of cases it will do R2 normal
     * '1 3 100' ==> in 100% of cases it will do R1 on heroic
     */
    RandomCollection<String> raids;
    RandomCollection<String> wednesdayRaids;
    /**
     * World Boss Settings
     **/
    List<String> worldBossSettings;
    int worldBossTimer = 0;
    boolean worldBossSolo = false;
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
    int minSolo = 2;

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
    private final Map<String, String> lastUsedMap = new HashMap<>();

    // If any error happens during the settings loading, this arraylist is populated with the offending lines
    ArrayList<String> wrongSettingLines = new ArrayList<>();

    public Settings() {
        activitiesEnabled = new LinkedHashSet<>();
        setactivitiesEnabledFromString("r d t g p e i v"); // some default values
        screenshots = new LinkedHashSet<>();
        setScreenshotsFromString("w d f b dg wg fe"); // enabled all by default
        worldBossSettings = new ArrayList<>();
        dungeons = new RandomCollection<>();
        setDungeons("z1d4 3 100"); // some default value
        wednesdayDungeons = new RandomCollection<>();
        setWednesdayDungeons(""); // default is empty, else if people delete the line it will load this value
        raids = new RandomCollection<>();
        setRaids("1 3 100"); // some default value
        wednesdayRaids = new RandomCollection<>();
        setWednesdayRaids(""); // default is empty, else if people delete the line it will load this value
        expeditions = new RandomCollection<>();
        setExpeditions("p1 100 100"); // some default value
        pvpstrip = new ArrayList<>();
        gvgstrip = new ArrayList<>();
        consumables = new ArrayList<>();
        familiars = new ArrayList<>();
        autoRevive = new ArrayList<>();
        tankPriority = new ArrayList<>();
        autoShrine = new ArrayList<>();
        autoRuneDefault = new ArrayList<>();
        poNotifyDrop = new ArrayList<>();
        discordNotifyDrop = new ArrayList<>();
        setDifficultyFailsafeFromString("t:0 g:0");
    }

    // a handy shortcut for some debug settings:
    Settings setDebug() {
        /*activitiesEnabled.add("r"); // Raid
        activitiesEnabled.add("d"); // Dungeon
        activitiesEnabled.add("g"); // Gauntlet
        activitiesEnabled.add("t"); // Trials
        activitiesEnabled.add("p"); // PVP
        activitiesEnabled.add("v"); // GVG
        activitiesEnabled.add("i"); // Invasion*/

        difficultyTrials = 60;
        difficultyGauntlet = 60;
        setDungeons("z2d1 3 50", "z2d2 3 50");
        setRaids("1 3 100");

        return this; // for chaining
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

    private void setScreenshots(String... types) {
        this.screenshots.clear();
        for (String t : types) {
            String add = t.trim();
            if ("".equals(add))
                continue;
            this.screenshots.add(add);
        }
    }

    private void setWorldBoss(String... types) {
        this.worldBossSettings.clear();
        for (String t : types) {
            String add = t.trim();
            if (add.equals(""))
                continue;
            this.worldBossSettings.add(add);
        }
    }

    private void setDungeons(String... dungeons) {
        this.dungeons.clear();
        double weight;
        String name;
        String[] config;

        for (String d : dungeons) {
            String add = d.trim();
            if ("".equals(add))
                continue;
            config = add.split(" ");
            weight = Double.parseDouble(config[2]);
            name = config[0] + " " + config[1];
            this.dungeons.add(weight, name);
        }
    }

    private void setWednesdayDungeons(String... wednesdayDungeons) {
        this.wednesdayDungeons.clear();
        double weight;
        String name;
        String[] config;

        for (String d : wednesdayDungeons) {
            String add = d.trim();
            if ("".equals(add))
                continue;
            config = add.split(" ");
            weight = Double.parseDouble(config[2]);
            name = config[0] + " " + config[1];
            this.wednesdayDungeons.add(weight, name);
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

    private void setRaids(String... raids) {
        this.raids.clear();
        double weight;
        String name;
        String[] config;

        for (String d : raids) {
            String add = d.trim();
            if ("".equals(add))
                continue;
            config = add.split(" ");
            weight = Double.parseDouble(config[2]);
            name = config[0] + " " + config[1];
            this.raids.add(weight, name);
        }
    }

    private void setWednesdayRaids(String... wednesdayRaids) {
        this.wednesdayRaids.clear();
        double weight;
        String name;
        String[] config;

        for (String d : wednesdayRaids) {
            String add = d.trim();
            if ("".equals(add))
                continue;
            config = add.split(" ");
            weight = Double.parseDouble(config[2]);
            name = config[0] + " " + config[1];
            this.wednesdayRaids.add(weight, name);
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

    private void setSuccessThreshold (String... tresholds) {
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

    private String getWorldBossAsString() {
        StringBuilder result = new StringBuilder();
        for (String s : worldBossSettings)
            result.append(s).append(" ");
        if (result.length() > 0)
            result = new StringBuilder(result.substring(0, result.length() - 1)); // remove last " " character
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

    private String getDungeonsAsString() {
        return dungeons.toString();
    }

    private String getWednesdayDungeonsAsString() {
        return wednesdayDungeons.toString();
    }

    private String getExpeditionsAsString() {
        return expeditions.toString();
    }

    private String getRaidsAsString() {
        return raids.toString();
    }

    private String getWednesdayRaidsAsString() {
        return wednesdayRaids.toString();
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

    private void setScreenshotsFromString(String s) { setScreenshots(s.split(" ")); }

    private void setWorldBossFromString(String s) {
        setWorldBoss(s.split(" "));
        // clean up (trailing spaces and remove if empty):
        for (int i = worldBossSettings.size() - 1; i >= 0; i--) {
            worldBossSettings.set(i, worldBossSettings.get(i).trim());
            if (worldBossSettings.get(i).equals(""))
                worldBossSettings.remove(i);
        }
    }

    private void setDungeonsFromString(String s) {
        setDungeons(s.split(";"));
    }

    private void setWednesdayDungeonsFromString(String s) {
        setWednesdayDungeons(s.split(";"));
    }

    private void setExpeditionsFromString(String s) {
        setExpeditions(s.split(";"));
    }

    private void setRaidsFromString(String s) {
        setRaids(s.split(";"));
    }

    private void setWednesdayRaidsFromString(String s) {
        setWednesdayRaids(s.split(";"));
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
    void load(List<String> lines) {
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

        minShards = Integer.parseInt(lastUsedMap.getOrDefault("minShards", "" + minShards));
        minTokens = Integer.parseInt(lastUsedMap.getOrDefault("minTokens", "" + minTokens));
        minEnergyPercentage = Integer.parseInt(lastUsedMap.getOrDefault("minEnergyPercentage", "" + minEnergyPercentage));
        minTickets = Integer.parseInt(lastUsedMap.getOrDefault("minTickets", "" + minTickets));
        minBadges = Integer.parseInt(lastUsedMap.getOrDefault("minBadges", "" + minBadges));

        poNotifyAlive = Integer.parseInt(lastUsedMap.getOrDefault("poNotifyAlive", "" + poNotifyAlive));
        discordNotifyAlive = Integer.parseInt(lastUsedMap.getOrDefault("discordNotifyAlive", "" + discordNotifyAlive));

        costPVP = Integer.parseInt(lastUsedMap.getOrDefault("costPVP", "" + costPVP));
        costGVG = Integer.parseInt(lastUsedMap.getOrDefault("costGVG", "" + costGVG));
        costTrials = Integer.parseInt(lastUsedMap.getOrDefault("costTrials", "" + costTrials));
        costGauntlet = Integer.parseInt(lastUsedMap.getOrDefault("costGauntlet", "" + costGauntlet));
        costInvasion = Integer.parseInt(lastUsedMap.getOrDefault("costInvasion", "" + costInvasion));
        costExpedition = Integer.parseInt(lastUsedMap.getOrDefault("costExpedition", "" + costExpedition));

        setWorldBossFromString(lastUsedMap.getOrDefault("worldBoss", getWorldBossAsString()));
        worldBossTimer = Integer.parseInt(lastUsedMap.getOrDefault("worldBossTimer", "" + worldBossTimer));
        dungeonOnTimeout = lastUsedMap.getOrDefault("dungeonOnTimeout", dungeonOnTimeout ? "1" : "0").equals("1");
        worldBossSolo = lastUsedMap.getOrDefault("worldBossSolo", worldBossSolo ? "1" : "0").equals("1");

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
        minSolo = Integer.parseInt(lastUsedMap.getOrDefault("minSolo", "" + minSolo));

        setDungeonsFromString(lastUsedMap.getOrDefault("dungeons", getDungeonsAsString()));
        setWednesdayDungeonsFromString(lastUsedMap.getOrDefault("wednesdayDungeons", getWednesdayDungeonsAsString()));
        setRaidsFromString(lastUsedMap.getOrDefault("raids", getRaidsAsString()));
        setWednesdayRaidsFromString(lastUsedMap.getOrDefault("wednesdayRaids", getWednesdayRaidsAsString()));
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
        List<String> lines = Misc.readTextFile2(file);
        if (lines == null || lines.size() == 0)
            return;

        load(lines);
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
