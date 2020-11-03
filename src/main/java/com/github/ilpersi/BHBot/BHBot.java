package com.github.ilpersi.BHBot;

import com.google.gson.Gson;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.remote.UnreachableBrowserException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;


public class BHBot {

    private static final String PROGRAM_NAME = "BHBot";
    static CueManager cues;
    private static String BHBotVersion;
    private static Properties gitProperties;
    static String screenshotPath = "./screenshots/";
    static BHBotLogger logger;

    // static settings
    static boolean debugDetectionTimes = false;
    // TODO understand if it is possible to differentiate log settings without making them static
    static String logBaseDir;
    static long logMaxDays;
    static Level logLevel;

    DungeonThread dungeon;
    private int numFailedRestarts = 0; // in a row

    Settings settings = new Settings();
    Scheduler scheduler = new Scheduler();
    NotificationManager notificationManager;
    ExceptionManager excManager;
    String chromeDriverAddress = "127.0.0.1:9515";
    String chromiumExePath = "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\Chromium\\Application\\chrome.exe";
    String chromeDriverExePath = "./chromedriver.exe";
    private Thread dungeonThread;
    private Thread blockerThread;

    private BHBot.State state; // at which stage of the game/menu are we currently?
    private BHBot.State lastJoinedState;
    /**
     * Set it to true to end main loop and end program gracefully
     */
    boolean finished = false;
    boolean running = false;
    BrowserManager browser;

    long botStartTime;

    // When we do not have anymore gems to use this is true
    boolean noGemsToBribe = false;

    // currently used scheduling
    Settings.ActivitiesScheduleSetting currentScheduling = null;

    public static void main(String[] args) {
        BHBot bot = new BHBot();
        bot.notificationManager = new NotificationManager(bot);
        bot.excManager = new ExceptionManager(bot);

        String userDataDir = "./chrome_profile";

        // We make sure that our configurationFactory is added to the list of configuration factories.
        System.setProperty("log4j.configurationFactory", "com.github.ilpersi.BHBot.BHBotConfigurationFactory");
        // We enable the log4j2 debug output if we need to
        if (bot.settings.logPringStatusMessages) System.setProperty("log4j2.debug", "true");

        // manage command line options
        for (int i = 0; i < args.length; i++) { //select settings file to load
            switch (args[i]) {
                case "chromedriver":
                case "chromedriverpath":
                case "chromeDriverExePath":
                    bot.chromeDriverExePath = args[i + 1];
                    i++;
                    continue;
                case "chromedriveraddress":  //change chrome driver port
                case "chromeDriverAddress":
                    bot.chromeDriverAddress = args[i + 1];
                    i++;
                    continue;
                case "chromium":
                case "chromiumpath":
                case "chromiumExePath":
                    bot.chromiumExePath = args[i + 1];
                    i++;
                    continue;
                case "init":  //start bot in idle mode
                case "idle":  //start bot in idle mode
                    Settings.configurationFile = "LOAD_IDLE_SETTINGS";
                    i++;
                    continue;
                case "settings":
                case "configurationFile":
                    Settings.configurationFile = args[i + 1];
                    i++;
                    continue;
                case "userdatadir":
                case "userDataDir":
                    userDataDir = args[i + 1];
                    i++;
            }
        }

        Settings.initialConfigurationFile = Settings.configurationFile;

        if ("LOAD_IDLE_SETTINGS".equals(Settings.configurationFile)) {
            bot.settings.setIdle();
        } else {
            /* if the specified setting file is "settings.ini", we check if the file exists
            if the file does not exist, we assume this is the first time the user is running the bot
             */
            try {
                bot.settings.load(Settings.configurationFile);
            } catch (FileNotFoundException e) {
                // We handle the default configuration file and we generate an empty one
                if ("settings.ini".equals(Settings.configurationFile)) {
                    try {
                        Settings.resetIniFile();
                    } catch (IOException ex) {
                        System.out.println("Error while creating settings.ini in main folder");
                        ex.printStackTrace();
                        return;
                    }

                    try {
                        bot.settings.load(Settings.configurationFile);
                    } catch (FileNotFoundException ex) {
                        System.out.println("It was impossible to find settings.ini, even after it has been created!");
                        return;
                    }
                    bot.settings.setIdle();

                } else {
                    System.out.println("It was impossible to find file " + Settings.configurationFile + ".");
                    return;
                }
            }
        }

        // We make sure to save the default schedulings, so they are never erased at reload
        bot.settings.defaultActivitiesSchedule = bot.settings.activitiesSchedule;

        // settings are now loaded
        debugDetectionTimes = bot.settings.debugDetectionTimes;
        logBaseDir = bot.settings.logBaseDir;
        logMaxDays = bot.settings.logMaxDays;
        logLevel = bot.settings.logLevel;

        logger = BHBotLogger.create();

        // we need to initialize the CueManager after that we started log4j, so that the cue manager can use it to log
        cues = new CueManager();

        // As we have initialized the cues successfully we also build familiar MD5 details
        EncounterManager.buildMD5();

        // If any error is present after parsing the config file, we stop the bot
        if (bot.settings.wrongSettingLines.size() > 0) {
            for (String wrongLine : bot.settings.wrongSettingLines) {
                logger.fatal("It was impossible to parse the following setting line and it has been skipped: '" + wrongLine + "!" +
                        "Please review your settings.ini file'");
            }
            return;
        }

        // If any warning is present during the setting parsing, we raise it
        if (!bot.settings.warningSettingLInes.isEmpty()) {
            for (String warningLine : bot.settings.warningSettingLInes) {
                logger.warn(warningLine);
            }
        }

        Properties properties = new Properties();
        try {
            properties.load(BHBot.class.getResourceAsStream("/pom.properties"));
            BHBotVersion = properties.getProperty("version");
        } catch (IOException e) {
            logger.error("Impossible to get pom.properties from jar", e);
            BHBotVersion = "UNKNOWN";
        }

        try {
            logger.info(PROGRAM_NAME + " v" + BHBotVersion + " build on " + new Date(Misc.classBuildTimeMillis()) + " started.");
        } catch (URISyntaxException e) {
            logger.info(PROGRAM_NAME + " v" + BHBotVersion + " started. Unknown build date.");
        }

        gitProperties = Misc.getGITInfo();
        logger.info("GIT commit id: " + gitProperties.get("git.commit.id") + "  time: " + gitProperties.get("git.commit.time"));

        if (!"UNKNOWN".equals(BHBotVersion)) {
            checkNewRelease();
        } else {
            logger.warn("Unknown BHBotVersion, impossible to check for updates.");
        }

        logger.info("Settings loaded from file");

        // We check for no longer supported settings
        if (!bot.settings.checkUnsupportedSettings()) return;

        bot.settings.checkDeprecatedSettings();
        bot.settings.sanitizeSetting();

        if (!bot.settings.username.equals("") && !bot.settings.username.equals("yourusername")) {
            logger.info("Character: " + bot.settings.username);
        }

        if (!bot.checkPaths()) return;

        bot.botStartTime = Misc.getTime();

        // Scanner scanner = new Scanner(System.in);
        logger.debug("Opening InputThread on stdin ...");
        InputThread reader = new InputThread(System.in, logger);
        while (!bot.finished) {
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    bot.processCommand((line));
                    Misc.sleep(500);
                }
            } catch (java.io.IOException e) {
                logger.error("Impossible to read user input", e);
            }

            // When the current schedule is no longer valid, we exit from it
            if (bot.running && bot.currentScheduling != null && State.Main.equals(bot.getState())) {
                if (!bot.currentScheduling.isActive()) {
                    bot.running = false;
                    bot.stop();
                    bot.currentScheduling = null;
                    continue;
                }
            }

            // When the bot is not running, we check if an active schedule is available
            if (!bot.running) {
                if (bot.settings.activitiesSchedule.isEmpty()) {
                    BHBot.logger.debug("Scheduling is empty, using default configuration.");
                    bot.browser = new BrowserManager(bot, userDataDir);
                    bot.running = true;
                    bot.scheduler.resetIdleTime(true);
                    bot.processCommand("start");
                    continue;
                } else {

                    BHBot.logger.trace("Checking for available schedulings");
                    for (Settings.ActivitiesScheduleSetting s : bot.settings.activitiesSchedule) {
                        if (s.isActive()) {

                            // We check what Chrome Profile path to use
                            String chromeProfilePath = "".equals(s.chromeProfilePath) ? userDataDir : s.chromeProfilePath;

                            // We check what setting plan to use
                            if (!"".equals(s.settingsPlan)) {
                                Settings.configurationFile = "plans/" + s.settingsPlan + ".ini";
                            } else {
                                Settings.configurationFile = Settings.initialConfigurationFile;
                            }

                            // We load the settings
                            try {
                                bot.settings.load(Settings.configurationFile);
                            } catch (FileNotFoundException e) {
                                BHBot.logger.error("It was impossible to load setting file for scheduling : " + s.toString());
                                continue;
                            }

                            // We save the current scheduling
                            bot.currentScheduling = s;

                            bot.settings.checkDeprecatedSettings();
                            bot.settings.sanitizeSetting();
                            bot.reloadLogger();

                            bot.browser = new BrowserManager(bot, chromeProfilePath);
                            bot.running = true;
                            bot.scheduler.resetIdleTime(true);
                            bot.processCommand("start");
                            BHBot.logger.info("Current scheduler is: " + s.toString());
                            break;
                        }
                    }
                }
            }

            BHBot.logger.trace("Main Thread Sleeping");
            Misc.sleep(500);

        }

        bot.stop();
        reader.close();
        logger.info(PROGRAM_NAME + " has finished.");
    }

    private void stop() {
        if (dungeonThread != null && dungeonThread.isAlive()) {
            try {
                // wait for 10 seconds for the main thread to terminate
                logger.info("Waiting for dungeon thread to finish... (timeout=10s)");
                dungeonThread.join(10 * Misc.Durations.SECOND);
            } catch (InterruptedException e) {
                logger.error("Error when joining Main Thread", e);
            }

            if (dungeonThread.isAlive()) {
                logger.warn("Dungeon thread is still alive. Force stopping it now...");
                dungeonThread.interrupt();
                try {
                    dungeonThread.join(); // until thread stops
                } catch (InterruptedException e) {
                    logger.error("Error while force stopping", e);
                }
            }
        }

        if (blockerThread != null && blockerThread.isAlive()) {
            try {
                // wait for 10 seconds for the main thread to terminate
                logger.info("Waiting for blocker thread to finish... (timeout=10s)");
                blockerThread.join(10 * Misc.Durations.SECOND);
            } catch (InterruptedException e) {
                logger.error("Error when joining Blocker Thread", e);
            }

            if (blockerThread.isAlive()) {
                logger.warn("Blocker thread is still alive. Force stopping it now...");
                blockerThread.interrupt();
                try {
                    blockerThread.join(); // until thread stops
                } catch (InterruptedException e) {
                    logger.error("Error while force stopping", e);
                }
            }
        }

        if (browser != null) browser.close();
    }

    private void processCommand(String c) {
        String[] params = c.split(" ");
        switch (params[0]) {
            case "c": { // detect cost from screen
                browser.readScreen();
                int current = dungeon.detectCost();
                logger.info("Detected cost: " + current);

                if (params.length > 1) {
                    int goal = Integer.parseInt(params[1]);
                    logger.info("Goal cost: " + goal);
                    boolean result = dungeon.selectCost(current, goal);
                    logger.info("Cost change result: " + result);
                }
                break;
            }
            case "compare":
                dungeon.cueDifference();
                break;
            case "crash": {
                throw new RuntimeException("CRASH!");
            }
            case "d": { // detect difficulty from screen
                browser.readScreen();
                int current = dungeon.detectDifficulty();
                logger.info("Detected difficulty: " + current);

                if (params.length > 1) {
                    int goal = Integer.parseInt(params[1]);
                    logger.info("Goal difficulty: " + goal);
                    int result = dungeon.selectDifficulty(current, goal, BHBot.cues.get("SelectDifficulty"), 1, false);
                    logger.info("Difficulty change result: " + result);
                }
                break;
            }
            case "do":
                switch (params[1]) {
                    case "baits":
                        // force fishing baits
                        logger.info("Forcing fishing baits collection...");
                        scheduler.doFishingBaitsImmediately = true;
                        break;
                    case "bounties":
                        // force bounties
                        logger.info("Forcing Bounty collection...");
                        scheduler.collectBountiesImmediately = true;
                        break;
                    case "dungeon":
                        // force dungeon (regardless of energy)
                        logger.info("Forcing dungeon...");
                        scheduler.doDungeonImmediately = true;
                        break;
                    case "expedition":
                        // force dungeon (regardless of energy)
                        logger.info("Forcing expedition...");
                        scheduler.doExpeditionImmediately = true;
                        break;
                    case "fishing":
                        // force fishing
                        logger.info("Forcing fishing...");
                        scheduler.doFishingImmediately = true;
                        break;
                    case "gauntlet":
                        logger.info("Forcing gauntlet...");
                        scheduler.doGauntletImmediately = true;
                        break;
                    case "gvg":
                        // force gvg
                        logger.info("Forcing GVG...");
                        scheduler.doGVGImmediately = true;
                        break;
                    case "invasion":
                        // force invasion
                        logger.info("Forcing invasion...");
                        scheduler.doInvasionImmediately = true;
                        break;
                    case "pvp":
                        // force pvp
                        logger.info("Forcing PVP...");
                        scheduler.doPVPImmediately = true;
                        break;
                    case "raid":
                        // force raid (if we have at least 1 shard though)
                        logger.info("Forcing raid...");
                        scheduler.doRaidImmediately = true;
                        break;
                    case "trials":
                        // force 1 run of gauntlet/trials (regardless of tokens)
                        logger.info("Forcing trials...");
                        scheduler.doTrialsImmediately = true;
                        break;
                    case "worldboss":
                        // force invasion
                        logger.info("Forcing World Boss...");
                        scheduler.doWorldBossImmediately = true;
                        break;
                    default:
                        logger.warn("Unknown dungeon : '" + params[1] + "'");
                        break;
                }
                break;
            case "exit":
            case "quit":
            case "stop":
                finished = true;
                break;
            case "hide":
                browser.hideBrowser();
                settings.hideWindowOnRestart = true;
                break;
            case "loadsettings":
                String file = Settings.configurationFile;
                if (params.length > 1)
                    file = params[1];

                try {
                    settings.load(file);
                } catch (FileNotFoundException e) {
                    BHBot.logger.error("It was impossible to find setting file: " + file + ".");
                    break;
                }

                settings.checkDeprecatedSettings();
                settings.sanitizeSetting();
                reloadLogger();
                break;
            case "pause":
                if (params.length > 1) {
                    int pauseDuration = Integer.parseInt(params[1]) * Misc.Durations.MINUTE;
                    scheduler.pause(pauseDuration);
                } else {
                    scheduler.pause();
                }
                break;
            case "plan":
                try {
                    settings.load("plans/" + params[1] + ".ini");
                } catch (FileNotFoundException e) {
                    BHBot.logger.error("It was impossible to find plan plans/" + params[1] + ".ini" + "!");
                    break;
                }

                settings.checkDeprecatedSettings();
                settings.sanitizeSetting();
                reloadLogger();
                logger.info("Plan loaded from " + "<plans/" + params[1] + ".ini>.");
                break;
            case "pomessage":
                logger.info("This command is deprecated, use 'test notification [your_message]' instead");
                break;
            case "print":
                if (params.length < 2) {
                    BHBot.logger.error("Missing parameters for print command: print familiars|version");
                    break;
                }

                switch (params[1]) {
                    case "config-file":
                        BHBot.logger.info("Initial configuration file: " + Settings.initialConfigurationFile);
                        BHBot.logger.info("Current configuration file: " + Settings.configurationFile);
                        break;
                    case "familiars":
                    case "familiar":
                    case "fam":
                        DungeonThread.printFamiliars();
                        break;
                    case "fam-md5":
                        if (params.length == 2)
                            EncounterManager.printMD5();
                        else if (params.length == 3)
                            EncounterManager.printMD5(params[2]);
                        else
                            BHBot.logger.warn("USAGE: print fam-md5 [familiarName]");
                        break;
                    case "scheduling":
                        if (settings.activitiesSchedule.size() == 0) {
                            BHBot.logger.info("No scheduling are present at the moment.");
                        } else {
                            StringBuilder schedulingStr = new StringBuilder("Current available schedules are:\n");
                            for (Settings.ActivitiesScheduleSetting activeScheduling : settings.activitiesSchedule) {
                                schedulingStr.append(activeScheduling.toString()).append("\n");
                            }

                            if (currentScheduling != null) {
                                schedulingStr.append("\n")
                                        .append("Current scheduling is: ").append(currentScheduling);
                            }

                            BHBot.logger.info(schedulingStr.toString());
                        }
                        break;
                    case "screen-rect":
                        int minx = 0, miny = 0, maxx = 0, maxy = 0, cnt = 0;
                        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
                        for(GraphicsDevice device : environment.getScreenDevices()){

                            Rectangle bounds = device.getDefaultConfiguration().getBounds();
                            minx = Math.min(minx, bounds.x);
                            miny = Math.min(miny, bounds.y);
                            maxx = Math.max(maxx,  bounds.x+bounds.width);
                            maxy = Math.max(maxy, bounds.y+bounds.height);

                            double scale = (double) device.getDisplayMode().getWidth() / (double) device.getDefaultConfiguration().getBounds().width;

                            cnt += 1;

                            Rectangle screenRect = new Rectangle(minx, miny, maxx-minx, maxy-miny);
                            BHBot.logger.info("[Screen " + cnt + "] =>" + screenRect.toString() + " Scale => " + String.format("%.02f%%", scale));
                        }
                        break;
                    case "stats":
                    case "stat":
                    case "statistics":

                        StringBuilder aliveMsg = new StringBuilder();
                        aliveMsg.append("Current session statistics:\n\n");

                        for (State state : State.values()) {
                            if (dungeon.counters.get(state).getTotal() > 0) {
                                aliveMsg.append(state.getName()).append(" ")
                                        .append(dungeon.counters.get(state).successRateDesc())
                                        .append("\n");
                            }
                        }
                        logger.info(aliveMsg.toString());

                        break;
                    case "version":
                        try {
                            logger.info(PROGRAM_NAME + " v" + BHBotVersion + " build on " + new Date(Misc.classBuildTimeMillis()) + " started.");
                        } catch (URISyntaxException e) {
                            logger.info(PROGRAM_NAME + " v" + BHBotVersion + " started. Unknown build date.");
                        }

                        Properties gitPropertis = Misc.getGITInfo();
                        logger.info("GIT commit id: " + gitPropertis.get("git.commit.id") + "  time: " + gitPropertis.get("git.commit.time"));
                        break;
                    default:
                        logger.warn("Impossible to print : '" + params[1] + "'");
                        break;
                }
                break;
            case "resetini":
                try {
                    Settings.resetIniFile();
                } catch (IOException e) {
                    BHBot.logger.error("It was impossible to reset ini file: " + Settings.configurationFile);
                }
                break;
            case "restart":
                dungeon.restart(false);
                break;
            case "shot":
                String fileName = "shot";
                if (params.length > 1)
                    fileName = params[1];

                dungeon.bot.saveGameScreen(fileName);

                logger.info("Screenshot '" + fileName + "' saved.");
                break;
            case "start":
                dungeon = new DungeonThread(this);
                dungeonThread = new Thread(dungeon, "DungeonThread");
                dungeonThread.start();

                BlockerThread blocker = new BlockerThread(this);
                blockerThread = new Thread(blocker, "BlockerThread");
                blockerThread.start();
                break;
            case "softreset":
                dungeon.softReset();
                break;
            case "readouts":
            case "resettimers":
                dungeon.resetTimers();
                logger.info("Readout timers reset.");
                break;
            case "reload":
                settings.load();
                reloadLogger();
                logger.info("Settings reloaded from disk.");
                break;
            case "resume":
                scheduler.resume();
                break;
            case "set": {
                List<String> list = new ArrayList<>();
                int i = c.indexOf(" ");
                if (i == -1)
                    return;
                list.add(c.substring(i + 1));
                settings.load(list, false, "command");
                settings.checkDeprecatedSettings();
                settings.sanitizeSetting();
                reloadLogger();
                logger.info("Settings updated manually: <" + list.get(0) + ">");
                break;
            }
            case "show":
                browser.showBrowser();
                settings.hideWindowOnRestart = false;
                break;
            case "test":

                if (params.length <= 1) {
                    BHBot.logger.error("Not enough parameters for test command");
                    break;
                }

                switch (params[1]) {
                    case "ai":
                    case "autoignore":
                        boolean ignoreBoss = false;
                        boolean ignoreShrines = false;

                        if (params.length > 2) {
                            switch (params[2].toLowerCase()) {
                                case "off":
                                case "0":
                                case "no":
                                case "do":
                                    ignoreBoss = false;
                                    break;
                                case "on":
                                case "1":
                                case "yes":
                                case "y":
                                    ignoreBoss = true;
                                    break;
                            }
                        }

                        if (params.length > 3) {
                            switch (params[3].toLowerCase()) {
                                case "off":
                                case "0":
                                case "no":
                                case "do":
                                    ignoreShrines = false;
                                    break;
                                case "on":
                                case "1":
                                case "yes":
                                case "y":
                                    ignoreShrines = true;
                                    break;
                            }
                        }
                        if (!dungeon.shrineManager.updateShrineSettings(ignoreBoss, ignoreShrines)) {
                            logger.error("Something went wrong when checking auto ignore settings!");
                        }
                        break;
                    case "e":
                    case "expeditionread":
                        dungeon.expeditionReadTest();
                        break;
                    case "notification":
                        // We split on spaces so we re-build the original message
                        String notificationMessage = params.length > 2 ? String.join(" ", Arrays.copyOfRange(params, 2, params.length)) : "Test message from BHbot!";

                        notificationManager.sendTestNotification(notificationMessage);
                        break;
                    case "runes":
                        dungeon.runeManager.detectEquippedMinorRunes(true, true);
                        break;
                    case "scrollbar":
                        Misc.findScrollBarPositions(dungeon.bot);
                        break;
                    default:
                        break;
                }
                break;
            default:
                logger.warn("Unknown command: '" + c + "'");
                break;
        }
    }

    private boolean checkPaths() {
        String cuesPath = "./cues/";

        File chromiumExe = new File(chromiumExePath);
        File chromeDriverExe = new File(chromeDriverExePath);
        File cuePath = new File(cuesPath);
        File screenPath = new File(screenshotPath);

        if (!chromiumExe.exists()) {
            logger.fatal("Impossible to find Chromium executable in path " + chromiumExePath + ". Bot will be stopped!");
            return false;
        } else {
            try {
                logger.debug("Found Chromium in " + chromiumExe.getCanonicalPath());
            } catch (IOException e) {
                logger.error("Error while getting Canonical Path for Chromium", e);
            }
        }

        if (!chromeDriverExe.exists()) {
            logger.fatal("Impossible to find chromedriver executable in path " + chromeDriverExePath + ". Bot will be stopped!");
            return false;
        } else {
            try {
                logger.debug("Found chromedriver in " + chromeDriverExe.getCanonicalPath());
            } catch (IOException e) {
                logger.error("Error while getting Canonical Path for chromedriver", e);
            }
        }

        if (!screenPath.exists()) {
            if (!screenPath.mkdir()) {
                logger.fatal("Impossible to create screenshot folder in " + screenshotPath);
                return false;
            } else {
                try {
                    logger.info("Created screenshot folder in " + screenPath.getCanonicalPath());
                } catch (IOException e) {
                    logger.error("Error while getting Canonical Path for newly created screenshots", e);
                }
            }
        } else {
            try {
                logger.debug("Found screenshots in " + screenPath.getCanonicalPath());
            } catch (IOException e) {
                logger.error("Error while getting Canonical Path for screenshots", e);
            }
        }

        if (cuePath.exists() && !cuePath.isFile()) {
            try {
                logger.warn("Found cues in '" + cuePath.getCanonicalPath() +
                        "'. This folder is no longer required as all the cues are now part of the jar file.");
            } catch (IOException e) {
                logger.error("Error while checking cues folder", e);
            }
        }

        return true;
    }

    void reloadLogger() {
        ConfigurationFactory configFactory = new BHBotConfigurationFactory();
        ConfigurationFactory.setConfigurationFactory(configFactory);
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ctx.start(configFactory.getConfiguration(ctx, ConfigurationSource.NULL_SOURCE));
    }

    private static void checkNewRelease() {

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/ilpersi/BHBot/releases/latest"))
                .build();

        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            logger.error("Exception while getting latest version info from Git Hub", e);
        }

        Gson gson = new Gson();

        Double currentVersion = Double.parseDouble(BHBotVersion);

        if (response != null) {
            int statusCode = response.statusCode();
            if (statusCode != 200) {
                logger.error("GitHub version check failed with HTTP error code : " + statusCode);
                return;
            }

            GitHubRelease lastReleaseInfo;
            lastReleaseInfo = gson.fromJson(response.body(), GitHubRelease.class);

            String tagName = lastReleaseInfo.tagName;
            String tagUrl = "https://api.github.com/repos/ilpersi/BHBot/git/refs/tags/" + tagName;

            request = HttpRequest.newBuilder()
                    .uri(URI.create(tagUrl))
                    .build();

            response = null;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                logger.error("Exception while getting tag ref info from Git Hub", e);
            }

            if (response != null) {
                statusCode = response.statusCode();
                if (statusCode != 200) {
                    logger.error("GitHub version check failed with HTTP error code : " + statusCode);
                    return;
                }

                GitHubTag lastReleaseTagInfo;
                lastReleaseTagInfo = gson.fromJson(response.body(), GitHubTag.class);

                Double onlineVersion = Double.parseDouble(lastReleaseInfo.tagName.replace("v", ""));

                if (onlineVersion > currentVersion) {
                    logger.warn("A new BHBot version is available and you can get it from " + lastReleaseInfo.releaseURL);
                    logger.warn("Here are new features:");
                    for (String feature : lastReleaseInfo.releaseNotes.split("\n")) {
                        logger.warn(feature);
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        logger.warn("Error while waiting for GitHub release check");
                    }
                } else if (onlineVersion.equals(currentVersion)) {
                    if (lastReleaseTagInfo.object.sha.equals(gitProperties.get("git.commit.id"))) {
                        logger.debug("BHBot is running on the latest version.");
                    } else {
                        logger.warn("You are running on a bleeding edge version of BHBot and there may be bugs.");
                    }
                } else {
                    logger.warn("You are running on a bleeding edge version of BHBot and there may be bugs.");
                }
            }
        }
    }

    synchronized State getState() {
        return state;
    }

    synchronized State getLastJoinedState() {
        return lastJoinedState;
    }

    synchronized void setState(State state) {
        this.state = state;
    }

    synchronized void setLastJoinedState(State state) {
        this.lastJoinedState = state;
    }

    /**
     * Takes screenshot of current game and saves it to disk to a file with a given prefix (date will be added, and optionally a number at the end of file name).
     * In case of failure, it will just ignore the error.
     *
     * @param prefix The string used to prefix the screenshot name
     * @return name of the path in which the screenshot has been saved (successfully or not)
     */
    synchronized String saveGameScreen(String prefix) {
        return saveGameScreen(prefix, null, browser.takeScreenshot(true));
    }

    synchronized String saveGameScreen(String prefix, BufferedImage img) {
        return saveGameScreen(prefix, null, img);
    }

    synchronized String saveGameScreen(String prefix, String subFolder) {
        return saveGameScreen(prefix, subFolder, browser.takeScreenshot(true));
    }

    synchronized String saveGameScreen(String prefix, String subFolder, BufferedImage img) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        // sub-folder logic management
        String screenshotPath = BHBot.screenshotPath;
        if (subFolder != null) {
            File subFolderPath = new File(BHBot.screenshotPath + subFolder + "/");
            if (!subFolderPath.exists()) {
                if (!subFolderPath.mkdir()) {
                    logger.error("Impossible to create screenshot sub folder in " + subFolder);
                    return null;
                } else {
                    try {
                        logger.info("Created screenshot sub-folder " + subFolderPath.getCanonicalPath());
                    } catch (IOException e) {
                        logger.error("Error while getting Canonical Path for newly created screenshots sub-folder", e);
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
            logger.error("Impossible to take a screenshot!");
        }

        return f.getPath();
    }

    void dumpCrashLog(Exception originalException) {
        // save screen shot:
        String file = saveGameScreen("crash", "errors");

        if (file == null) {
            logger.error("Impossible to create crash screenshot");
            return;
        }

        String stackTrace;
        if (originalException != null) {
            StringWriter sw = new StringWriter();
            originalException.printStackTrace(new PrintWriter(sw));
            stackTrace = sw.toString();
        } else {
            stackTrace = Misc.getStackTrace();
        }

        boolean savedST = Misc.saveTextFile(file.substring(0, file.length() - 4) + ".txt", stackTrace);
        if (!savedST) {
            logger.info("Impossible to save the stack trace in dumpCrashLog!");
        }

        notificationManager.sendCrashNotification("BHBot has crashed and a driver emergency restart has been performed!\n\n" + stackTrace, file);

    }

    /**
     * @param emergency         true in case something bad happened (some kind of an error for which we had to do a restart)
     * @param useDoNotShareLink is the bot running with do_not_share link enabled?
     */
    void restart(boolean emergency, boolean useDoNotShareLink) {
        restart(emergency, useDoNotShareLink, null);
    }

    void restart(boolean emergency, boolean useDoNotShareLink, Exception originalException) {
        final int MAX_NUM_FAILED_RESTARTS = 5;

        // take emergency screenshot (which will have the developer to debug the problem):
        if (emergency) {
            logger.warn("Doing driver emergency restart...");
            dumpCrashLog(originalException);
        }

        try {
            browser.restart(useDoNotShareLink);
        } catch (Exception e) {

            if (e instanceof NoSuchElementException)
                logger.warn("Problem: web element with id 'game' not found!");
            if (e instanceof UnreachableBrowserException) {
                logger.error("Impossible to connect to the bot.browser. Make sure chromedirver is started. Will retry in a few minutes... (sleeping)");
                Misc.sleep(5 * Misc.Durations.MINUTE);
                restart(true, useDoNotShareLink, e);
                return;
            }

            numFailedRestarts++;
            if (numFailedRestarts > MAX_NUM_FAILED_RESTARTS) {
                logger.fatal("Something went wrong with driver restart. Number of restarts exceeded " + MAX_NUM_FAILED_RESTARTS + ", this is why I'm aborting...");
                finished = true;
            } else {
                logger.error("Something went wrong with driver restart. Will retry in a few minutes... (sleeping)", e);
                Misc.sleep(5 * Misc.Durations.MINUTE);
                restart(true, useDoNotShareLink, e);
            }
            return;
        }

        browser.detectSignInFormAndHandleIt(); // just in case (happens seldom though)

        browser.scrollGameIntoView();

        int counter = 0;
        boolean restart = false;
        while (true) {
            try {
                browser.readScreen();

                MarvinSegment seg = MarvinSegment.fromCue(cues.get("Login"), browser);
                browser.detectLoginFormAndHandleIt(seg);
            } catch (Exception e) {
                counter++;
                if (counter > 20) {
                    logger.error("Error: <" + e.getMessage() + "> while trying to detect and handle login form. Restarting...", e);
                    restart = true;
                    break;
                }

                Misc.sleep(10 * Misc.Durations.SECOND);
                continue;
            }
            break;
        }
        if (restart) {
            restart(true, useDoNotShareLink);
            return;
        }

        logger.info("Game element found. Starting to run bot..");

        setState(State.Loading);
        scheduler.resetIdleTime();
        scheduler.resume(); // in case it was paused
        numFailedRestarts = 0; // must be last line in this method!

        // we make sure that the shrinemanager is resetted at restart time and we
        // skip the initialization if idleMode is true
        dungeon.shrineManager = new AutoShrineManager(this, settings.idleMode);
        dungeon.runeManager = new AutoRuneManager(this, settings.idleMode);
        dungeon.encounterManager = new EncounterManager(this);
        dungeon.reviveManager.reset();
        dungeon.positionChecker = new DungeonPositionChecker();
    }

    enum State {
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
        WorldBoss("World Boss", "w");

        private final String name;
        private final String shortcut;

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
}
