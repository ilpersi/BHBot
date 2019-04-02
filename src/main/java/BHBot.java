import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import net.pushover.client.*;


public class BHBot {

	private static final String PROGRAM_NAME = "BHBot";
	private static final String PROGRAM_VERSION = "34.2";

	private static Thread mainThread;
	static MainThread main;
	/** Set it to true to end main loop and end program gracefully */
	private static boolean finished = false;
	
	static Settings settings = new Settings().setDebug();
	static Scheduler scheduler = new Scheduler();

	static PushoverClient poClient = new PushoverRestClient();

	static String chromeDriverAddress = "127.0.0.1:9515";

	static String chromiumExePath = "C:\\Users\\"+System.getProperty("user.name")+"\\AppData\\Local\\Chromium\\Application\\chrome.exe";
	static String chromeDriverExePath = "./chromedriver.exe";
	private static String cuesPath = "./cues/";
	static String screenshotPath = "./screenshots/";

	public static void main(String[] args) {
		log(PROGRAM_NAME + " v" + PROGRAM_VERSION + " started.");
		
		MainThread.loadCues();
		
		// process launch arguments
		boolean settingsProcessed = false;
		for (int i = 0; i < args.length; i++) { //select settings file to load
			switch (args[i]) {
				case "settings":
					processCommand("loadsettings " + args[i + 1]);
					settingsProcessed = true;
					i++;
					break;
				case "init":  //start bot in idle mode
				case "idle":  //start bot in idle mode
					BHBot.settings.setIdle();
					settingsProcessed = true;
					i++;
					break;
				case "chromium":
				case "chromiumpath":
					chromiumExePath = args[i + 1];
					BHBot.log("Changed Chromium path to: " + args[i + 1]);
					break;
				case "chromedriver":
				case "chromedriverpath":
					chromeDriverExePath = args[i + 1];
					BHBot.log("Changed chromedriver path to: " + args[i + 1]);
					break;
				case "chromedriveraddress":  //change chrome driver port
					chromeDriverAddress = args[i + 1];
					i++;
					break;
			}
		}
		if (!settingsProcessed)
			processCommand("loadsettings");
		
		if (!checkPaths()) return;

		processCommand("start");
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (!finished) {
	        String s;
			try {
				//System.out.print("> ");
				s = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			try {
				log("User command: <" + s + ">");
				processCommand(s);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (mainThread.isAlive()) {
			try {
				// wait for 10 seconds for the main thread to terminate
				log("Waiting for main thread to finish... (timeout=10s)");
				mainThread.join(10*MainThread.SECOND);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (mainThread.isAlive()) {
				log("Main thread is still alive. Force stopping it now...");
				mainThread.interrupt();
				try {
					mainThread.join(); // until thread stops
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		log(PROGRAM_NAME + " has finished.");
	}
	
	public static void log(String s) {//prints with date and time in format
		System.out.println(new SimpleDateFormat("<yyyy/MM/dd HH:mm:ss>").format(new Date()) + " " + s);
	}

	private static void processCommand(String c) {
		String[] params = c.split(" ");
		switch (params[0]) {
			case "exit":
			case "quit":
			case "stop":
				main.finished = true;
				finished = true;
				break;
			case "start":
				main = new MainThread();
				mainThread = new Thread(main, "MainThread");
				mainThread.start();
				break;
			case "restart":
				main.restart(false);
				break;
			case "save":
				MainThread.saveCookies();
				break;
			case "load":
				MainThread.loadCookies();
				break;
			case "pomessage":
				String message = "Test message from BHbot!";

				// We split on spaces so we re-build the original message
				if (params.length > 1)
					message = String.join(" ", Arrays.copyOfRange(params, 1, params.length));

				if (BHBot.settings.enablePushover) {
					String poLogMessage = "Sending Pushover test message.";
					poLogMessage += "\n\n poUserToken is: " + BHBot.settings.poUserToken;
					poLogMessage += "\n poAppToken is: " + BHBot.settings.poAppToken;
					BHBot.log(poLogMessage);

					String poScreenName = main.saveGameScreen("pomessage");
					File poScreenFile = new File(poScreenName);

					main.sendPushOverMessage("Test Notification", message, MessagePriority.NORMAL, poScreenFile);
					if (!poScreenFile.delete()) BHBot.log("Impossible to delete tmp img for pomessage command.");

				} else {
					BHBot.log("Pushover integration is disabled in the settings!");
				}
				break;	
			case "shot":
				String fileName = "shot";
				if (params.length > 1)
					fileName = params[1];

				main.saveGameScreen(fileName);

				log("Screenshot '" + fileName + "' saved.");
				break;
			case "pause":
				if (params.length > 1) {
					int pauseDuration = Integer.parseInt(params[1]) * MainThread.MINUTE;
					scheduler.pause(pauseDuration);
				} else {
					scheduler.pause();
				}
				break;
			case "resume":
				scheduler.resume();
				break;
			case "reload":
				settings.load();
				log("Settings reloaded from disk.");
				break;
			case "loadsettings":
				String file = Settings.DEFAULT_SETTINGS_FILE;
				if (params.length > 1)
					file = params[1];
				settings.load(file);
				log("Settings loaded from file");
				log("Character: " + BHBot.settings.username);
				break;
			case "readouts":
			case "resettimers":
				main.resetTimers();
				log("Readout timers reset.");
				break;
			case "crash": {
				int i = 3 / 0;
				break;
			}
			case "hide":
				main.hideBrowser();
				settings.hideWindowOnRestart = true;
				break;
			case "show":
				main.showBrowser();
				settings.hideWindowOnRestart = false;
				break;
			case "set": {
				List<String> list = new ArrayList<>();
				int i = c.indexOf(" ");
				if (i == -1)
					return;
				list.add(c.substring(i + 1));
				settings.load(list);
				log("Settings updated manually: <" + list.get(0) + ">");
				break;
			}
			case "do":
				switch (params[1]) {
					case "raid":
						// force raid (if we have at least 1 shard though)
						log("Forcing raid...");
						scheduler.doRaidImmediately = true;
						break;
					case "expedition":
						// force dungeon (regardless of energy)
						log("Forcing expedition...");
						scheduler.doExpeditionImmediately = true;
						break;
					case "dungeon":
						// force dungeon (regardless of energy)
						log("Forcing dungeon...");
						scheduler.doDungeonImmediately = true;
						break;
					case "gauntlet":
					case "trials":
						// force 1 run of gauntlet/trials (regardless of tokens)
						log("Forcing gauntlet/trials...");
						scheduler.doTrialsOrGauntletImmediately = true;
						break;
					case "pvp":
						// force pvp
						log("Forcing PVP...");
						scheduler.doPVPImmediately = true;
						break;
					case "gvg":
						// force gvg
						log("Forcing GVG...");
						scheduler.doGVGImmediately = true;
						break;
					case "invasion":
						// force invasion
						log("Forcing invasion...");
						scheduler.doInvasionImmediately = true;
						break;
					case "worldboss":
						// force invasion
						log("Forcing World Boss...");
						scheduler.doWorldBossImmediately = true;
						break;
				}
				break;
			case "plan":
				settings.load("plans/" + params[1] + ".ini");
				log("Plan loaded from " + "<plans/" + params[1] + ".ini>.");
				break;
			case "d": { // detect difficulty from screen
				main.readScreen();
				int current = main.detectDifficulty();
				log("Detected difficulty: " + current);

				if (params.length > 1) {
					int goal = Integer.parseInt(params[1]);
					log("Goal difficulty: " + goal);
					boolean result = main.selectDifficulty(current, goal);
					log("Difficulty change result: " + result);
				}
				break;
			}
			case "c": { // detect cost from screen
				main.readScreen();
				int current = main.detectCost();
				log("Detected cost: " + current);

				if (params.length > 1) {
					int goal = Integer.parseInt(params[1]);
					log("Goal cost: " + goal);
					boolean result = main.selectCost(current, goal);
					log("Cost change result: " + result);
				}
				break;
			}
            case "print":
                switch (params[1]) {
                    case "familiars":
                    case "familiar":
                    case "fam":
                        MainThread.printFamiliars();
                        break;
                    default:
                        break;
                }
                break;
            case "test":
                switch (params[1]) {
                    case "s":
                    case "shrine":
                        main.checkShrineSettings("disable");
                        break;
                    case "r":
                    case "raidread":
                        main.raidReadTest();
                        break;
                    case "e":
                    case "expeditionread":
                        main.expeditionReadTest();
                        break;
                    case "wb":
                    case "worldboss":
                        main.wbTest();
                        break;
                    case "d":
                        main.updateActivityCounter("World Boss");
                        break;
                    case "ad":
                        main.trySkippingAd();
                        break;
                    default:
                        break;
                }
                break;
		}
	}

	private static boolean checkPaths() {
		File chromiumExe = new File(BHBot.chromiumExePath);
		File chromeDriverExe = new File(BHBot.chromeDriverExePath);
		File cuePath = new File(BHBot.cuesPath);
		File screenPath = new File(BHBot.screenshotPath);

		if (!chromiumExe.exists()) {
			log("Impossible to find Chromium executable in path " + BHBot.chromiumExePath + ". Bot will be stopped!");
			return false;
		} else {
			try {
				BHBot.log("Found Chromium in " + chromiumExe.getCanonicalPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (!chromeDriverExe.exists()) {
			log("Impossible to find chromedriver executable in path " + BHBot.chromeDriverExePath + ". Bot will be stopped!");
			return false;
		} else {
			try {
				BHBot.log("Found chromedriver in " + chromeDriverExe.getCanonicalPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (!cuePath.exists() || cuePath.isFile()) {
		    BHBot.log("Impossible to find cues path in " + BHBot.cuesPath + ". Bot will be stopped!");
		    return false;
        } else {
            try {
                BHBot.log("Found cues in " + cuePath.getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

		if (!screenPath.exists()) {
			if (!screenPath.mkdir()) {
				BHBot.log("Impossible to create screenshot folder in " + BHBot.screenshotPath);
				return false;
			} else {
				try {
					BHBot.log("Created screenshot folder in " + screenPath.getCanonicalPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			try {
				BHBot.log("Found screenshots in " + screenPath.getCanonicalPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return true;
	}
	
	/*public static void printSet(Set<String> set) {
		for (String s : set) {
			log("Set key: " + s);
		}
	}*/

}
