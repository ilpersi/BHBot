import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


import net.pushover.client.PushoverClient;
import net.pushover.client.PushoverException;
import net.pushover.client.PushoverMessage;
import net.pushover.client.PushoverRestClient;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;


public class BHBot {

	private static final String PROGRAM_NAME = "BHBot";
	private static final String PROGRAM_VERSION = "33.2";
	private static final boolean REQUIRES_ACCESS_TOKEN = false; // obsolete since public release (was used to restrict bot usage)
	
	private static Thread mainThread;
	static MainThread main;
	/** Set it to true to end main loop and end program gracefully */
	private static boolean finished = false;
	
	static Settings settings = new Settings().setDebug();
	static Scheduler scheduler = new Scheduler();

	static PushoverClient poClient = new PushoverRestClient();
	
	static String chromeDriverAddress = "127.0.0.1:9515";
	
	public static void main(String[] args) {
		log(PROGRAM_NAME + " v" + PROGRAM_VERSION + " started.");
		
		MainThread.loadCues();
		
		if (REQUIRES_ACCESS_TOKEN) {
			int timeout = 10000;
			log("Requesting access token... (timeout=" + Misc.millisToHumanForm(timeout) + ")");
			boolean access = AccessControl.check(timeout);
			if (!access) {
				log("Error: access token expired or was unable to retrieve a new one. Quiting...");
				return ;
			}
		}
		
		// process launch arguments
		boolean settingsProcessed = false;
		for (int i = 0; i < args.length; i++) { //select settings file to load
			switch (args[i]) {
				case "settings":
					processCommand("loadsettings " + args[i + 1]);
					settingsProcessed = true;
					i++;
					break;
				case "chromedriveraddress":  //change chrome driver port
					chromeDriverAddress = args[i + 1];
					i++;
					break;
				case "init":  //start bot in idle mode
					BHBot.settings.setIdle();
					settingsProcessed = true;
					i++;
					break;
				case "idle":  //start bot in idle mode
					BHBot.settings.setIdle();
					settingsProcessed = true;
					i++;
					break;
			}
		}
		if (!settingsProcessed)
			processCommand("loadsettings");
		
		processCommand("start");
		
//	    Console console = System.console();
//	    Scanner scanner = new Scanner(System.in);
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (!finished) {
	        String s;
			try {
				//System.out.print("> ");
				s = br.readLine();
				//s = console.readLine();
				//s = scanner.nextLine();
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
	
/*	public static final DecimalFormat num4Digits = new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.US));
	public static final DecimalFormat num3Digits = new DecimalFormat("#.###", new DecimalFormatSymbols(Locale.US));
	public static final DecimalFormat num2Digits = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
	public static final DecimalFormat num1Digit = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.US));*/
	
	public static void log(String s) {//prints with date and time in format
		System.out.println(new SimpleDateFormat("<yyyy/MM/dd HH:mm:ss>").format(new Date()) + " " + s);
	}
//	public static void logint(int s) {//int logger
//	}
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
			case "session":
			/*
			SessionStorage ss = main.driver.getSessionStorage();
			printSet(ss.keySet());
			LocalStorage ls = main.driver.getLocalStorage();
			printSet(ls.keySet());
			SessionId si = main.driver.getSessionId();
			log("Session id: " + si.toString());
			*/
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
					try {
						poClient.pushMessage(
								PushoverMessage.builderWithApiToken(BHBot.settings.poAppToken)
										.setUserId(BHBot.settings.poUserToken)
										.setMessage(message)
										.build());
					} catch (PushoverException e) {
						e.printStackTrace();
					}
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
			case "test2":
				log("Detecting CLAIM YOUR REWARD BUTTON...");
				WebElement btnClose = null;
				boolean done;
				long timer = Misc.getTime();
				do {
					if (Misc.getTime() - timer > 60 * MainThread.SECOND) break;
					main.sleep(500);
					try {
						done = MainThread.driver.findElement(By.id("ssaInterstitialNotification")).getAttribute("style").contains("opacity: 1");
						if (done) {
							log("AD CAN NOW BE CLOSED! CLOSING...");

							// click the close button:
							try {
								btnClose = MainThread.driver.findElement(By.id("ssaInterstitialClose"));
							} catch (NoSuchElementException e) {
								BHBot.log("Error: Cannot find close button on finished ad window! HUH :(");
								break;
							}
							btnClose.click();

							break;
						}
					} catch (Exception e) {
						btnClose = null;
					}
				} while (btnClose == null);

				if (btnClose == null) {
					BHBot.log("Could not find CLAIM YOUR REWARD button!");
				} else {
					BHBot.log("Found CLAIM YOUR REWARD button!");
				}
				break;
			case "test3":
				log("Detecting character dialog cues...");

				final Color cuec1 = new Color(238, 241, 249); // white

				final Color cuec2 = new Color(82, 90, 98); // gray

//			Color col;

				BufferedImage img = MainThread.loadImage("C:/Tomaz/Bit Heroes/yeti dialog 2.png");
				MarvinSegment right = MainThread.findSubimage(img, MainThread.cues.get("DialogRight"));
				MarvinSegment left = MainThread.findSubimage(img, MainThread.cues.get("DialogLeft"));

				log("Image test right: " + right);
				log("Image test left: " + left);

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

				log("Image test right: " + right);
				log("Image test left: " + left);
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
			case "shrinetest":
				main.checkShrineSettings("disable");
				break;
			case "rtest":
				main.raidReadTest();
				break;
			case "etest":
				main.expeditionReadTest();
				break;
			case "wbtest":
				main.wbTest();
				break;
			case "dtest":
				main.updateActivityCounter("World Boss");
				break;
			case "adtest":
				main.trySkippingAd();
				break;
		}
	}
	
	/*public static void printSet(Set<String> set) {
		for (String s : set) {
			log("Set key: " + s);
		}
	}*/

}
