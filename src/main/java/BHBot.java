import com.google.gson.Gson;
import net.pushover.client.MessagePriority;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.remote.UnreachableBrowserException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;


public class BHBot {

    private static final String PROGRAM_NAME = "BHBot";
    private static String BHBotVersion;
    private static Properties gitPropertis;
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

    Settings settings = new Settings().setDebug();
    Scheduler scheduler = new Scheduler();
    PushOverManager poManager;
    ExceptionManager excManager;
    String chromeDriverAddress = "127.0.0.1:9515";
    String chromiumExePath = "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\Chromium\\Application\\chrome.exe";
    String chromeDriverExePath = "./chromedriver.exe";
    private Thread dungeonThread;
    private Thread blockerThread;

    private BHBot.State state; // at which stage of the game/menu are we currently?
    /**
     * Set it to true to end main loop and end program gracefully
     */
    boolean finished = false;
    BrowserManager browser;

    public static void main(String[] args) {
        BHBot bot = new BHBot();
        bot.browser = new BrowserManager(bot);
        bot.poManager = new PushOverManager(bot);
        bot.excManager = new ExceptionManager(bot);

        // We make sure that our configurationFactory is added to the list of configuration factories.
        System.setProperty("log4j.configurationFactory", "BHBotConfigurationFactory");
        // We enable the log4j2 debug output if we need to
        if (bot.settings.logPringStatusMessages) System.setProperty("log4j2.debug", "true");

        for (int i = 0; i < args.length; i++) { //select settings file to load
            switch (args[i]) {
                case "settings":
                    Settings.configurationFile = args[i + 1];
                    i++;
                    continue;
                case "init":  //start bot in idle mode
                case "idle":  //start bot in idle mode
                    Settings.configurationFile = "LOAD_IDLE_SETTINGS";
                    i++;
                    continue;
                case "chromium":
                case "chromiumpath":
                    bot.chromiumExePath = args[i + 1];
                    continue;
                case "chromedriver":
                case "chromedriverpath":
                    bot.chromeDriverExePath = args[i + 1];
                    continue;
                case "chromedriveraddress":  //change chrome driver port
                    bot.chromeDriverAddress = args[i + 1];
                    i++;
            }
        }

        if ("LOAD_IDLE_SETTINGS".equals(Settings.configurationFile)) {
            bot.settings.setIdle();
        } else {
            try {
                bot.settings.load(Settings.configurationFile);
            } catch (FileNotFoundException e) {
                System.out.println("It was impossible to find file " + Settings.configurationFile + ".");

                // We handle the default configuration file and we generate an empty one
                if ("bot.settings.ini".equals(Settings.configurationFile)) {

                    try {
                        Settings.resetIniFile();
                    } catch (IOException ex) {
                        System.out.println("Error while creating bot.settings.ini in main folder");
                        ex.printStackTrace();
                        return;
                    }
                }
                return;
            }
        }

        // settings are now loaded
        debugDetectionTimes = bot.settings.debugDetectionTimes;
        logBaseDir = bot.settings.logBaseDir;
        logMaxDays = bot.settings.logMaxDays;
        logLevel = bot.settings.logLevel;

        logger = BHBotLogger.create();
        BrowserManager.buildCues();

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

        gitPropertis = Misc.getGITInfo();
        logger.info("GIT commit id: " + gitPropertis.get("git.commit.id") + "  time: " + gitPropertis.get("git.commit.time"));

        if (!"UNKNOWN".equals(BHBotVersion)) {
            checkNewRelease();
        } else {
            logger.warn("Unknown BHBotVersion, impossible to check for updates.");
        }

        logger.info("Settings loaded from file");

        bot.settings.checkDeprecatedSettings();
        bot.settings.sanitizeSetting();

        if (!bot.settings.username.equals("") && !bot.settings.username.equals("yourusername")) {
            logger.info("Character: " + bot.settings.username);
        }

        if (!bot.checkPaths()) return;

        bot.processCommand("start");

        Scanner scanner = new Scanner(System.in);
        while (!bot.finished) {
            String s;
            try {
                s = scanner.nextLine();
            } catch (IllegalStateException | NoSuchElementException e) {
                logger.error("Impossible to read user input", e);
                return;
            }

            if (s != null) {
                try {
                    logger.info("User command: <" + s + ">");
                    bot.processCommand(s);
                } catch (Exception e) {
                    logger.error("Impossible to process user command: " + s, e);
                }
            }

            Misc.sleep(500);
        }

        if (bot.dungeonThread.isAlive()) {
            try {
                // wait for 10 seconds for the main thread to terminate
                logger.info("Waiting for blocker thread to finish... (timeout=10s)");
                bot.blockerThread.join(10 * Misc.Durations.SECOND);
            } catch (InterruptedException e) {
                logger.error("Error when joining Blocker Thread", e);
            }

            if (bot.blockerThread.isAlive()) {
                logger.warn("Blocker thread is still alive. Force stopping it now...");
                bot.blockerThread.interrupt();
                try {
                    bot.blockerThread.join(); // until thread stops
                } catch (InterruptedException e) {
                    logger.error("Error while force stopping", e);
                }
            }

            try {
                // wait for 10 seconds for the main thread to terminate
                logger.info("Waiting for dungeon thread to finish... (timeout=10s)");
                bot.dungeonThread.join(10 * Misc.Durations.SECOND);
            } catch (InterruptedException e) {
                logger.error("Error when joining Main Thread", e);
            }

            if (bot.dungeonThread.isAlive()) {
                logger.warn("Dungeon thread is still alive. Force stopping it now...");
                bot.dungeonThread.interrupt();
                try {
                    bot.dungeonThread.join(); // until thread stops
                } catch (InterruptedException e) {
                    logger.error("Error while force stopping", e);
                }
            }

            bot.browser.close();

        }
        logger.info(PROGRAM_NAME + " has bot.finished.");
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
                    boolean result = dungeon.selectDifficulty(current, goal);
                    logger.info("Difficulty change result: " + result);
                }
                break;
            }
            case "do":
                switch (params[1]) {
                    case "raid":
                        // force raid (if we have at least 1 shard though)
                        logger.info("Forcing raid...");
                        scheduler.doRaidImmediately = true;
                        break;
                    case "expedition":
                        // force dungeon (regardless of energy)
                        logger.info("Forcing expedition...");
                        scheduler.doExpeditionImmediately = true;
                        break;
                    case "dungeon":
                        // force dungeon (regardless of energy)
                        logger.info("Forcing dungeon...");
                        scheduler.doDungeonImmediately = true;
                        break;
                    case "gauntlet":
                        logger.info("Forcing gauntlet...");
                        scheduler.doGauntletImmediately = true;
                        break;
                    case "trials":
                        // force 1 run of gauntlet/trials (regardless of tokens)
                        logger.info("Forcing trials...");
                        scheduler.doTrialsImmediately = true;
                        break;
                    case "pvp":
                        // force pvp
                        logger.info("Forcing PVP...");
                        scheduler.doPVPImmediately = true;
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
                    case "worldboss":
                        // force invasion
                        logger.info("Forcing World Boss...");
                        scheduler.doWorldBossImmediately = true;
                        break;
                    case "bounties":
                        // force bounties
                        logger.info("Forcing Bounty collection...");
                        scheduler.collectBountiesImmediately = true;
                        break;
                    case "baits":
                        // force fishing baits
                        logger.info("Forcing fishing baits collection...");
                        scheduler.doFishingBaitsImmediately = true;
                        break;
                    case "fishing":
                        // force fishing
                        logger.info("Forcing fishing...");
                        scheduler.doFishingImmediately = true;
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
                String message = "Test message from BHbot!";

                // We split on spaces so we re-build the original message
                if (params.length > 1)
                    message = String.join(" ", Arrays.copyOfRange(params, 1, params.length));

                if (settings.enablePushover) {
                    String poLogMessage = "Sending Pushover test message.";
                    poLogMessage += "\n\n poUserToken is: " + settings.poUserToken;
                    poLogMessage += "\n poAppToken is: " + settings.poAppToken;
                    logger.info(poLogMessage);

                    String poScreenName = dungeon.bot.saveGameScreen("pomessage");
                    File poScreenFile = poScreenName != null ? new File(poScreenName) : null;

                    poManager.sendPushOverMessage("Test Notification", message, MessagePriority.NORMAL, poScreenFile);
                    if (poScreenFile != null && !poScreenFile.delete())
                        logger.warn("Impossible to delete tmp img for pomessage command.");

                } else {
                    logger.warn("Pushover integration is disabled in the settings!");
                }
                break;
            case "print":
                if (params.length < 2) {
                    BHBot.logger.error("Missing parameters for print command: print familiars|version");
                    break;
                }

                switch (params[1]) {
                    case "familiars":
                    case "familiar":
                    case "fam":
                        DungeonThread.printFamiliars();
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
            case "readouts":
            case "resettimers":
                dungeon.resetTimers();
                logger.info("Readout timers reset.");
                break;
            case "compare":
                dungeon.cueDifference();
                break;
            case "softreset":
                dungeon.softReset();
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
                settings.load(list);
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
                    case "runes":
                        dungeon.detectEquippedMinorRunes(true, true);
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

    private void reloadLogger() {
        ConfigurationFactory configFactory = new BHBotConfigurationFactory();
        ConfigurationFactory.setConfigurationFactory(configFactory);
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ctx.start(configFactory.getConfiguration(ctx, ConfigurationSource.NULL_SOURCE));
    }

    private static void checkNewRelease() {

        HttpClient httpClient = HttpClients.custom().useSystemProperties().build();
        final HttpGet releaseGetReq = new HttpGet("https://api.github.com/repos/ilpersi/BHBot/releases/latest");

        Gson gson = new Gson();

        Double currentVersion = Double.parseDouble(BHBotVersion);

        HttpResponse response;
        try {
            response = httpClient.execute(releaseGetReq);
        } catch (IOException e) {
            logger.error("Impossible to reach GitHub latest release endpoing", e);
            return;
        }

        if (response != null) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                logger.error("GitHub version check failed with HTTP error code : " + statusCode);
                return;
            }

            GitHubRelease lastReleaseInfo;
            try {
                lastReleaseInfo = gson.fromJson(EntityUtils.toString(response.getEntity()), GitHubRelease.class);

            } catch (IOException e) {
                logger.error("Error while parsing GitHub latest release JSON.", e);
                return;
            }

            String tagName = lastReleaseInfo.tagName;
            String tagUrl = "https://api.github.com/repos/ilpersi/BHBot/git/refs/tags/" + tagName;

            final HttpGet tagGetReq = new HttpGet(tagUrl);
            try {
                response = httpClient.execute(tagGetReq);
            } catch (IOException e) {
                logger.error("Impossible to reach GitHub latest tag endpoing", e);
                return;
            }

            GitHubTag lastReleaseTagInfo;
            try {
                lastReleaseTagInfo = gson.fromJson(EntityUtils.toString(response.getEntity()), GitHubTag.class);
            } catch (IOException e) {
                logger.error("Error while parsing GitHub latest tag JSON.", e);
                return;
            }

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
                if (lastReleaseTagInfo.object.sha.equals(gitPropertis.get("git.commit.id"))) {
                    logger.debug("BHBot is running on the latest version.");
                } else {
                    logger.warn("You are running on a bleeding edge version of BHBot and there may be bugs.");
                }
            } else {
                logger.warn("You are running on a bleeding edge version of BHBot and there may be bugs.");
            }
        }

    }

    synchronized State getState() {
        return state;
    }

    synchronized void setState(State state) {
        this.state = state;
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

    void dumpCrashLog() {
        // save screen shot:
        String file = saveGameScreen("crash", "errors");

        if (file == null) {
            logger.error("Impossible to create crash screenshot");
            return;
        }

        String stackTrace = Misc.getStackTrace();

        // save stack trace:
        boolean savedST = Misc.saveTextFile(file.substring(0, file.length() - 4) + ".txt", stackTrace);
        if (!savedST) {
            logger.info("Impossible to save the stack trace in dumpCrashLog!");
        }

        if (settings.enablePushover && settings.poNotifyCrash) {
            File poCrashScreen = new File(file);
            poManager.sendPushOverMessage("BHBot CRASH!",
                    "BHBot has crashed and a driver emergency restart has been performed!\n\n" + stackTrace, "falling",
                    MessagePriority.HIGH, poCrashScreen.exists() ? poCrashScreen : null);
        }
    }

    /**
     * @param emergency true in case something bad happened (some kind of an error for which we had to do a restart)
     * @param useDoNotShareLink is the bot running with do_not_share link enabled?
     */
    void restart(boolean emergency, boolean useDoNotShareLink) {
        final int MAX_NUM_FAILED_RESTARTS = 5;

        // take emergency screenshot (which will have the developer to debug the problem):
        if (emergency) {
            logger.warn("Doing driver emergency restart...");
            dumpCrashLog();
        }

        try {
            browser.restart(useDoNotShareLink);
        } catch (Exception e) {

            if (e instanceof NoSuchElementException)
                logger.warn("Problem: web element with id 'game' not found!");
            if (e instanceof MalformedURLException)
                logger.warn("Problem: malformed url detected!");
            if (e instanceof UnreachableBrowserException) {
                logger.error("Impossible to connect to the bot.browser. Make sure chromedirver is started. Will retry in a few minutes... (sleeping)");
                Misc.sleep(5 * Misc.Durations.MINUTE);
                restart(true, useDoNotShareLink);
                return;
            }

            numFailedRestarts++;
            if (numFailedRestarts > MAX_NUM_FAILED_RESTARTS) {
                logger.fatal("Something went wrong with driver restart. Number of restarts exceeded " + MAX_NUM_FAILED_RESTARTS + ", this is why I'm aborting...");
                finished = true;
            } else {
                logger.error("Something went wrong with driver restart. Will retry in a few minutes... (sleeping)", e);
                Misc.sleep(5 * Misc.Durations.MINUTE);
                restart(true, useDoNotShareLink);
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

                MarvinSegment seg = MarvinSegment.fromCue(BrowserManager.cues.get("Login"), browser);
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

        if ((settings.activitiesEnabled.contains("d")) && (settings.activitiesEnabled.contains("w"))) {
            logger.info("Both Dungeons and World Boss selected, disabling World Boss.");
            logger.info("To run a mixture of both use a low lobby timer and enable dungeonOnTimeout");
            settings.activitiesEnabled.remove("w");
        }
        setState(State.Loading);
        scheduler.resetIdleTime();
        scheduler.resume(); // in case it was paused
        numFailedRestarts = 0; // must be last line in this method!

        // we make sure that the shrinemanager is resetted at restart time and we
        // skip the initialization if idleMode is true
        dungeon.shrineManager = new AutoShrineManager(this, settings.idleMode);
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
}
