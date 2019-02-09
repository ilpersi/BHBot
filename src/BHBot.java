import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import com.assertthat.selenium_shutterbug.core.Shutterbug;

public class BHBot {

	public static final String PROGRAM_NAME = "BHBot";
	public static final String PROGRAM_VERSION = "29.72";
	public static final boolean REQUIRES_ACCESS_TOKEN = false; // obsolete since public release (was used to restrict bot usage)
	
	public static Thread mainThread;
	public static MainThread main;
	/** Set it to true to end main loop and end program gracefully */
	public static boolean finished = false;
	
	public static Settings settings = new Settings().setDebug();
	public static Scheduler scheduler = new Scheduler();
	
	public static String chromeDriverAddress = "127.0.0.1:9515";
	
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
			if (args[i].equals("settings")) {
				processCommand("loadsettings " + args[i+1]);
				settingsProcessed = true;
				i++;
			} else if (args[i].equals("chromedriveraddress")) { //change chrome driver port
				chromeDriverAddress = args[i+1];
				i++;
			}
			 else if (args[i].equals("init")) { //start bot in idle mode
				BHBot.settings.setIdle();
				settingsProcessed = true;
				i++;
			}
			 else if (args[i].equals("idle")) { //start bot in idle mode
				BHBot.settings.setIdle();
				settingsProcessed = true;
				i++;
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
			}
			if (mainThread.isAlive()) {
				log("Main thread is still alive. Force stopping it now...");
				mainThread.interrupt();
				try {
					mainThread.join(); // until thread stops
				} catch (InterruptedException e) {
				}
			}
		}
		log(PROGRAM_NAME + " has finished.");
	}
	
	public static final DecimalFormat num4Digits = new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.US));
	public static final DecimalFormat num3Digits = new DecimalFormat("#.###", new DecimalFormatSymbols(Locale.US));
	public static final DecimalFormat num2Digits = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
	public static final DecimalFormat num1Digit = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.US));
	
	public static void log(String s) {//prints with date and time in format
		System.out.println(new SimpleDateFormat("<yyyy/MM/dd HH:mm:ss>").format(new Date()) + " " + s);
	}
//	public static void logint(int s) {//int logger
//	}
	public static void processCommand(String c) {
		String[] params = c.split(" ");
		if (c.equals("exit") || c.equals("quit") || c.equals("stop")) {
			main.finished = true;
			finished = true;
		} else if (c.equals("start")) {
			main = new MainThread();
			mainThread = new Thread(main, "MainThread");
			mainThread.start();
		} else if (c.equals("restart")) {
			main.restart(false);
		}else if (c.equals("session")) {
			/*
			SessionStorage ss = main.driver.getSessionStorage();
			printSet(ss.keySet());
			LocalStorage ls = main.driver.getLocalStorage();
			printSet(ls.keySet());
			SessionId si = main.driver.getSessionId();
			log("Session id: " + si.toString());
			*/
		} else if (c.equals("hijack")) {
			String[] cc = c.split(" ");

			//*** not finished
		} else if (c.equals("save")) {
			main.saveCookies();
		} else if (c.equals("load")) {
			main.loadCookies();
		} else if (c.equals("shot")) {
			try {
				Shutterbug.shootElement(main.driver, main.driver.findElement(By.id("game")), false)
				.withName("shot")
				.save("./screenshots/")
				;
			} catch (Exception e) {
				Shutterbug.shootPage(main.driver)
				.withName("custom  screenshot")
				.save(".")
				;
			}

			log("Screenshot saved.");
		} else if (c.equals("pause")) {
			scheduler.pause();
		} else if (c.equals("resume")) {
			scheduler.resume();
		} else if (c.equals("test")) {
//			BufferedImage src = MainThread.loadImage("C:/Tomaz/BHBot/screenshots/(shot) trials on right side of screen.png");
//			BufferedImage cue = MainThread.loadImage("E:/Eclipse/workspace/BHBot/cues/cueTrials2.png");
//			MarvinSegment seg = MainThread.findSubimage(src, new Cue("test", cue));
			
//			log("Image test: " + seg);
		} else if (c.equals("reload")) {
			settings.load();
			log("Settings reloaded from disk.");
		} else if (c.equals("test2")) {
			log("Detecting CLAIM YOUR REWARD BUTTON...");
			WebElement btnClose = null;
			boolean done = false;
			long timer = Misc.getTime();
			do {
				if (Misc.getTime() - timer > 60*MainThread.SECOND) break;
				main.sleep(500);
				try {
					done = main.driver.findElement(By.id("ssaInterstitialNotification")).getAttribute("style").contains("opacity: 1");
					if (done) {
						log("AD CAN NOW BE CLOSED! CLOSING...");
						
						// click the close button:
						try {
							btnClose = main.driver.findElement(By.id("ssaInterstitialClose"));
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
		} else if (c.equals("test3")) {
			log("Detecting character dialog cues...");
			
			final Color cuec1 = new Color(238, 241, 249); // white
			final Color cuec2 = new Color(82, 90, 98); // gray
			Color col;
			
			BufferedImage img = MainThread.loadImage("C:/Tomaz/Bit Heroes/yeti dialog 2.png");
			MarvinSegment right = MainThread.findSubimage(img, MainThread.cues.get("DialogRight"));
			MarvinSegment left = MainThread.findSubimage(img, MainThread.cues.get("DialogLeft"));

			log("Image test right: " + right);
			log("Image test left: " + left);
			
			// double check right-side dialog cue:
			if (right != null) {
				if (
						!(new Color(img.getRGB(right.x2+1, right.y1+24))).equals(cuec1) ||
						!(new Color(img.getRGB(right.x2+4, right.y1+24))).equals(cuec2)
						)
					right = null;
			}

			// double check left-side dialog cue:
			if (left != null) {
				if (
						!(new Color(img.getRGB(left.x1-1, left.y1+24))).equals(cuec1) ||
						!(new Color(img.getRGB(left.x1-4, left.y1+24))).equals(cuec2)
						)
					left = null;
			}
			
			log("Image test right: " + right);
			log("Image test left: " + left);
		} else if (params[0].equals("loadsettings")) {
			String file = Settings.DEFAULT_SETTINGS_FILE;
			if (params.length > 1)
				file = params[1];
			settings.load(file);
			log("Settings loaded from file");
			log("Character: " + BHBot.settings.username);
		} else if (c.equals("readouts") || c.equals("resettimers")) {
			main.resetTimers();
			log("Readout timers reset.");
		} else if (c.equals("crash")) {
			int i = 3/0;
		} else if (c.equals("hide")) {
			main.hideBrowser();
			settings.hideWindowOnRestart = true;
		} else if (c.equals("show")) {
			main.showBrowser();
			settings.hideWindowOnRestart = false;
		} else if (params[0].equals("set")) {
			List<String> list = new ArrayList<String>();
			int i = c.indexOf(" ");
			if (i == -1)
				return;
			list.add(c.substring(i+1));
			settings.load(list);
			log("Settings updated manually: <" + list.get(0) + ">");
		} else if (params[0].equals("do")) {
			if (params[1].equals("raid")) {
				// force raid (if we have at least 1 shard though)
				log("Forcing raid...");
				scheduler.doRaidImmediately = true;
			} else if (params[1].equals("expedition")) {
				// force dungeon (regardless of energy)
				log("Forcing expedition...");
				scheduler.doExpeditionImmediately = true;
			} else if (params[1].equals("dungeon")) {
				// force dungeon (regardless of energy)
				log("Forcing dungeon...");
				scheduler.doDungeonImmediately = true;
			} else if (params[1].equals("gauntlet") || params[1].equals("trials")) {
				// force 1 run of gauntlet/trials (regardless of tokens)
				log("Forcing gauntlet/trials...");
				scheduler.doTrialsOrGauntletImmediately = true;
			} else if (params[1].equals("pvp")) {
				// force pvp
				log("Forcing PVP...");
				scheduler.doPVPImmediately = true;
			} else if (params[1].equals("gvg")) {
				// force gvg
				log("Forcing GVG...");
				scheduler.doGVGImmediately = true;
			} else if (params[1].equals("invasion")) {
				// force invasion
				log("Forcing invasion...");
				scheduler.doInvasionImmediately = true;
			} else if (params[1].equals("worldboss")) {
				// force invasion
				log("Forcing World Boss...");
				scheduler.doWorldBossImmediately = true;
			}
		} else if (params[0].equals("plan")) {
			settings.load("plans/" + params[1] + ".ini");
			log("Plan loaded from " + "<plans/" + params[1] + ".ini>.");
		} else if (params[0].equals("d")) { // detect difficulty from screen
			main.readScreen();
			int current = main.detectDifficulty();
			log("Detected difficulty: " + current);

			if (params.length > 1) {
				int goal = Integer.parseInt(params[1]);
				log("Goal difficulty: " + goal);
				boolean result = main.selectDifficulty(current, goal);
				log("Difficulty change result: " + result);
			}
		} else if (params[0].equals("c")) { // detect cost from screen
			main.readScreen();
			int current = main.detectCost();
			log("Detected cost: " + current);

			if (params.length > 1) {
				int goal = Integer.parseInt(params[1]);
				log("Goal cost: " + goal);
				boolean result = main.selectCost(current, goal);
				log("Cost change result: " + result);
			}
		} else if (params[0].equals("mtest")) {
			main.handleTeamMalformedWarning();
		} else if (params[0].equals("rtest")) {
			main.raidReadTest();
		} else if (params[0].equals("etest")) {
			main.expeditionReadTest();
		} else if (params[0].equals("wbtest")) {
			main.wbReady();
		}
	}
	
	public static void printSet(Set<String> set) {
		for (String s : set) {
			log("Set key: " + s);
		}
	}

}
