import org.apache.logging.log4j.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Settings {
	static final String DEFAULT_SETTINGS_FILE = "settings.ini";
	
	String username = "";
	String password = "";

	// Pushover info
	String poAppToken = "";
	String poUserToken = "";

	/** Experimental feature. Better use 'false' for now. */
    private boolean useHeadlessMode = false; // run Chrome with --headless switch?
	boolean restartAfterAdOfferTimeout = true; // if true, then bot will automatically restart itself if it hasn't claimed any ad offer in a time longer than defined. This is needed because ads don't appear anymore if Chrome doesn't get restarted.
	boolean debugDetectionTimes = false; // if true, then each time a cue detection from game screenshot will be attempted, a time taken will be displayed together with a name of the cue
	boolean hideWindowOnRestart = false; // if true, game window will be hidden upon driver (re)start
	private boolean resetTimersOnBattleEnd = true; // if true, readout timers will get reset once dungeon is cleared (or pvp or gvg or any other type of battle)
	int reconnectTimer = 60;

	// chromedriver autostart
	boolean autoStartChromeDriver = true; // if true, BHBot will automatically run chromedriver at startup

	//Various settings
    int openSkeleton = 0;
	boolean contributeFamiliars  = true;
	boolean collectBounties  = false;
	boolean collectFishingBaits  = false;
	boolean dungeonOnTimeout = true;
	boolean countActivities = false;
	
	boolean doRaids = false;
	boolean doDungeons = false;
	boolean doTrials = false;
	boolean doGauntlet = false;
	boolean doPVP = false;
	boolean doGVG = false;
	boolean doAds = false;
	boolean doInvasion = false;
	boolean doExpedition = false;
	boolean doWorldBoss = false;

	// Pushover settings
	boolean enablePushover = false;
	boolean poNotifyPM = false;
	boolean poNotifyCrash = false;
	boolean poNotifyErrors = false;
	boolean poNotifyBribe = false;
	int poNotifyAlive = 0;

	/** This is the minimum amount of shards that the bot must leave for the user. If shards get above this value, bot will play the raids in case raiding is enabled of course. */
    int minShards = 2;
	/** This is the minimum amount of tokens that the bot must leave for the user. If tokens get above this value, bot will play the trials/gauntlet in case trials/gauntlet is enabled of course. */
    int minTokens = 5;
	/** This is the minimum amount of energy as percentage that the bot must leave for the user. If energy is higher than that, then bot will attempt to play dungeons. */
    int minEnergyPercentage = 70;
	/** This is the minimum amount of tickets that the bot must leave for the user. If tickets get above this value, bot will play the pvp in case pvp is enabled of course. */
    int minTickets = 5;
	/** This is the minimum amount of badges that the bot must leave for the user. If badges get above this value, bot will play the gvg in case gvg is enabled of course. */
    int minBadges = 5;
	
	// Max for various expendables for correct calculation if not default
    int maxShards = 4;
	int maxTokens =  10;
	int maxTickets = 10;
	int maxBadges = 10;
	
	// costs (1..5) for various events:
    int costPVP = 1;
	int costGVG = 1;
	int costTrials = 1;
	int costGauntlet = 1;
	int costInvasion = 1;
	int costExpedition = 1;
	
	/** The trials/gauntlet difficulty */
    int difficulty = 60;
    
	/** PvP Opponent */
    int pvpOpponent = 1;

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
	RandomCollection<String> thursdayDungeons;
	
	/**
	 * List of raids we want to do (there are 3 raids: 1, 2 and 3) with a difficulty level and percentage.
	 * Examples:
	 * '1 3 70;2 1 30' ==> in 70% of cases it will do R1 on heroic, in 30% of cases it will do R2 normal
	 * '1 3 100' ==> in 100% of cases it will do R1 on heroic
	 */
	RandomCollection<String> raids;
	RandomCollection<String> thursdayRaids;
	
	/** World Boss Settings **/
    String worldBossType = "";
	int worldBossTier  =  0;
	int worldBossTimer = 0;
	int worldBossDifficulty = 0;
	boolean worldBossSolo = false;
	
	/** Autorevive Settings **/
	List<String> autoRevive;
	String potionOrder = "123";
	boolean tankPriority = false;
	int potionLimit = 3;
	
	/** Autoshrine settings **/
    List<String> autoShrine;
	int battleDelay = 60;
	int shrineDelay = 20;
	
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
	
	/** If true, then bot will try to auto consume consumables as specified by the 'consumables' list. */
    boolean autoConsume = false;
	/** List of consumables that we want activate at all times. */
    List<String> consumables;

    // List of familiars to bribe
	List<String> familiars;

	/**
	 * The level at which we want to try to automatically persuade the familiar
	 * 1 is for Common, 2 is for Rare, 3 is for Epic, 4 is for Legendary
	 */
	int persuasionLevel = 1;
	int bribeLevel = 0;

	/** Development Settings **/
    int familiarScreenshot = 2;
	int minSolo = 2;
	String dungeonsRun = "dungeonsrun 0";
	String worldBossRun = "worldbossrun 0";

	/** log4j settings */
	// Where do we save the logs?
	String logBaseDir = "logs";
	// What is the default level of the logs
	Level logLevel = Level.INFO;
	// How many days of logs do we store?
	int logMaxDays = 30;

	private Map<String, String> lastUsedMap = new HashMap<>();

	public Settings() {
		dungeons = new RandomCollection<>();
		setDungeons("z1d4 3 100"); // some default value
		thursdayDungeons = new RandomCollection<>();
		setThursdayDungeons(""); // default is empty, else if people delete the line it will load this value 
		raids = new RandomCollection<>();
		setRaids("1 3 100"); // some default value
		thursdayRaids = new RandomCollection<>();
		setThursdayRaids(""); // default is empty, else if people delete the line it will load this value 
		expeditions = new RandomCollection<>();
		setExpeditions("p1 100 100"); // some default value
		pvpstrip = new ArrayList<>();
		gvgstrip = new ArrayList<>();
		consumables = new ArrayList<>();
		familiars = new ArrayList<>();
		autoRevive = new ArrayList<>();
		autoShrine = new ArrayList<>();
	}
	
	// a handy shortcut for some debug settings:
    Settings setDebug() {
		doRaids = true;
		doDungeons = true;
		doGauntlet = true;
		doTrials = true;
		doPVP = true;
		doGVG = true;
		doInvasion = true;
		doAds = true;
		
		difficulty = 60;
		setDungeons("z2d1 3 50", "z2d2 3 50");
		setRaids("1 3 100");
		
		return this; // for chaining
	}
	
	/** Does nothing except collect ads */
    void setIdle() {
		doRaids = false;
		doDungeons = false;
		doTrials = false;
		doGauntlet = false;
		doPVP = false;
		doGVG = false;
		doInvasion = false;
		doAds = false;
		doExpedition = false;
		doWorldBoss = false;
		enablePushover = false;
		poNotifyPM = false;
		poNotifyCrash = false;
		poNotifyErrors = false;
		poNotifyBribe = false;
		autoConsume = false;
		collectBounties = false;
		collectFishingBaits = false;
	}
	
	/* Cleans the data from the input and saves it at a string */

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
	
	private void setThursdayDungeons(String... thursdayDungeons) {
		this.thursdayDungeons.clear();
		double weight;
		String name;
		String[] config;

		for (String d : thursdayDungeons) {
			String add = d.trim();
			if ("".equals(add))
				continue;
			config = add.split(" ");
			weight = Double.parseDouble(config[2]);
			name = config[0] + " " + config[1];
			this.thursdayDungeons.add(weight, name);
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
	
	private void setThursdayRaids(String... thursdayRaids) {
		this.thursdayRaids.clear();
		double weight;
		String name;
		String[] config;

		for (String d : thursdayRaids) {
			String add = d.trim();
			if ("".equals(add))
				continue;
			config = add.split(" ");
			weight = Double.parseDouble(config[2]);
			name = config[0] + " " + config[1];
			this.thursdayRaids.add(weight, name);
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
	
	private void setAutoShrine(String... types) {
		this.autoShrine.clear();
		for (String t : types) {
			String add = t.trim();
			if (add.equals(""))
				continue;
			this.autoShrine.add(add);
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

	private String getDungeonsAsString() {
		return dungeons.toString();
	}
	
	private String getThursdayDungeonsAsString() {
		return thursdayDungeons.toString();
	}

	private String getExpeditionsAsString() {
		return expeditions.toString();
	}

	private String getRaidsAsString() {
		return raids.toString();
	}
	
	private String getThursdayRaidsAsString() { return thursdayRaids.toString(); }

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
	
	private String getAutoShrineAsString() {
		StringBuilder result = new StringBuilder();
		for (String s : autoShrine)
			result.append(s).append(" ");
		if (result.length() > 0)
			result = new StringBuilder(result.substring(0, result.length() - 1)); // remove last " " character
		return result.toString();
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

    private String  getLogLevelAsString() {
        return logLevel.toString();
    }
	
	/* Cleans up the data in the list again */

	private void setDungeonsFromString(String s) {
		setDungeons(s.split(";"));
	}
	
	private void setThursdayDungeonsFromString(String s) {
		setThursdayDungeons(s.split(";"));
	}

	private void setExpeditionsFromString(String s) {
		setExpeditions(s.split(";"));
	}

	private void setRaidsFromString(String s) {
		setRaids(s.split(";"));
	}
	
	private void setThursdayRaidsFromString(String s) {
		setThursdayRaids(s.split(";"));
	}

	private void setStripsFromString(String s) {
		setStrips(s.split(" "));
		// clean up (trailing spaces and remove if empty):
		for (int i = pvpstrip.size()-1; i >= 0; i--) {
			pvpstrip.set(i, pvpstrip.get(i).trim());
			if (pvpstrip.get(i).equals(""))
				pvpstrip.remove(i);
		}
	}

	private void setAutoReviveFromString(String s) {
		setAutoRevive(s.split(" "));
		// clean up (trailing spaces and remove if empty):
		for (int i = autoRevive.size()-1; i >= 0; i--) {
			autoRevive.set(i, autoRevive.get(i).trim());
			if (autoRevive.get(i).equals(""))
				autoRevive.remove(i);
		}
	}
	
	private void setAutoShrineFromString(String s) {
		setAutoShrine(s.split(" "));
		// clean up (trailing spaces and remove if empty):
		for (int i = autoShrine.size()-1; i >= 0; i--) {
			autoShrine.set(i, autoShrine.get(i).trim());
			if (autoShrine.get(i).equals(""))
				autoShrine.remove(i);
		}
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
		for (int i = consumables.size()-1; i >= 0; i--) {
			consumables.set(i, consumables.get(i).trim());
			if (consumables.get(i).equals(""))
				consumables.remove(i);
		}
	}
	
	private void setFamiliarsFromString(String s) {
		setFamiliars(s.split(";"));
		// clean up (trailing spaces and remove if empty):
		for (int i = familiars.size()-1; i >= 0; i--) {
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
	public void load(List<String> lines) {
		for (String line : lines) {
			if (line.trim().equals("")) continue;
			if (line.startsWith("#")) continue; // a comment
			lastUsedMap.put(line.substring(0, line.indexOf(" ")), line.substring(line.indexOf(" ")+1));
		}
		
		username = lastUsedMap.getOrDefault("username", username);
		password = lastUsedMap.getOrDefault("password", password);
		poAppToken = lastUsedMap.getOrDefault("poAppToken", poAppToken);
		poUserToken = lastUsedMap.getOrDefault("poUserToken", poUserToken);
		useHeadlessMode = lastUsedMap.getOrDefault("headlessmode", useHeadlessMode ? "1" : "0").equals("1");
		restartAfterAdOfferTimeout = lastUsedMap.getOrDefault("restartAfterAdOfferTimeout", restartAfterAdOfferTimeout ? "1" : "0").equals("1");
		debugDetectionTimes = lastUsedMap.getOrDefault("debugDetectionTimes", debugDetectionTimes ? "1" : "0").equals("1");
		hideWindowOnRestart = lastUsedMap.getOrDefault("hideWindowOnRestart", hideWindowOnRestart ? "1" : "0").equals("1");
		resetTimersOnBattleEnd = lastUsedMap.getOrDefault("resetTimersOnBattleEnd", resetTimersOnBattleEnd ? "1" : "0").equals("1");
		autoStartChromeDriver = lastUsedMap.getOrDefault("autoStartChromeDriver", autoStartChromeDriver ? "1" : "0").equals("1");
		reconnectTimer = Integer.parseInt(lastUsedMap.getOrDefault("reconnectTimer", ""+reconnectTimer));
		
		doRaids = lastUsedMap.getOrDefault("doRaids", doRaids ? "1" : "0").equals("1");
		doDungeons = lastUsedMap.getOrDefault("doDungeons", doDungeons ? "1" : "0").equals("1");
		doTrials = lastUsedMap.getOrDefault("doTrials", doTrials ? "1" : "0").equals("1");
		doGauntlet = lastUsedMap.getOrDefault("doGauntlet", doGauntlet ? "1" : "0").equals("1");
		doPVP = lastUsedMap.getOrDefault("doPVP", doPVP ? "1" : "0").equals("1");
		doGVG = lastUsedMap.getOrDefault("doGVG", doGVG ? "1" : "0").equals("1");
		doInvasion = lastUsedMap.getOrDefault("doInvasion", doInvasion ? "1" : "0").equals("1");
		doExpedition = lastUsedMap.getOrDefault("doExpedition", doExpedition ? "1" : "0").equals("1");
		doWorldBoss = lastUsedMap.getOrDefault("doWorldBoss", doWorldBoss ? "1" : "0").equals("1");
		enablePushover = lastUsedMap.getOrDefault("enablePushover", enablePushover ? "1" : "0").equals("1");
		poNotifyPM = lastUsedMap.getOrDefault("poNotifyPM", poNotifyPM ? "1" : "0").equals("1");
		poNotifyCrash = lastUsedMap.getOrDefault("poNotifyCrash", poNotifyCrash ? "1" : "0").equals("1");
		poNotifyErrors = lastUsedMap.getOrDefault("poNotifyErrors", poNotifyErrors ? "1" : "0").equals("1");
		poNotifyBribe = lastUsedMap.getOrDefault("poNotifyBribe", poNotifyBribe ? "1" : "0").equals("1");
		doAds = lastUsedMap.getOrDefault("doAds", doAds ? "1" : "0").equals("1");
		
		maxShards = Integer.parseInt(lastUsedMap.getOrDefault("maxShards", ""+maxShards));
		maxTokens = Integer.parseInt(lastUsedMap.getOrDefault("maxTokens", ""+maxTokens));
		maxTickets = Integer.parseInt(lastUsedMap.getOrDefault("maxTickets", ""+maxTickets));
		maxBadges = Integer.parseInt(lastUsedMap.getOrDefault("maxBadges", ""+maxBadges));
		
		minShards = Integer.parseInt(lastUsedMap.getOrDefault("minShards", ""+minShards));
		minTokens = Integer.parseInt(lastUsedMap.getOrDefault("minTokens", ""+minTokens));
		minEnergyPercentage = Integer.parseInt(lastUsedMap.getOrDefault("minEnergyPercentage", ""+minEnergyPercentage));
		minTickets = Integer.parseInt(lastUsedMap.getOrDefault("minTickets", ""+minTickets));
		minBadges = Integer.parseInt(lastUsedMap.getOrDefault("minBadges", ""+minBadges));

		poNotifyAlive = Integer.parseInt(lastUsedMap.getOrDefault("poNotifyAlive", ""+poNotifyAlive));

		costPVP = Integer.parseInt(lastUsedMap.getOrDefault("costPVP", ""+costPVP));
		costGVG = Integer.parseInt(lastUsedMap.getOrDefault("costGVG", ""+costGVG));
		costTrials = Integer.parseInt(lastUsedMap.getOrDefault("costTrials", ""+costTrials));
		costGauntlet = Integer.parseInt(lastUsedMap.getOrDefault("costGauntlet", ""+costGauntlet));
		costInvasion = Integer.parseInt(lastUsedMap.getOrDefault("costInvasion", ""+costInvasion));
		costExpedition = Integer.parseInt(lastUsedMap.getOrDefault("costExpedition", ""+costExpedition));
		
		worldBossType = lastUsedMap.getOrDefault("worldBossType", worldBossType);
		worldBossDifficulty = Integer.parseInt(lastUsedMap.getOrDefault("worldBossDifficulty", ""+worldBossDifficulty));
		worldBossTier = Integer.parseInt(lastUsedMap.getOrDefault("worldBossTier", ""+worldBossTier));
		worldBossTimer = Integer.parseInt(lastUsedMap.getOrDefault("worldBossTimer", ""+worldBossTimer));
		dungeonOnTimeout = lastUsedMap.getOrDefault("dungeonOnTimeout", dungeonOnTimeout ? "1" : "0").equals("1");
		worldBossSolo = lastUsedMap.getOrDefault("worldBossSolo", worldBossSolo ? "1" : "0").equals("1");

		setAutoReviveFromString(lastUsedMap.getOrDefault("autoRevive", getAutoReviveAsString()));
		tankPriority = lastUsedMap.getOrDefault("tankPriority", tankPriority ? "1" : "0").equals("1");
		potionOrder  = lastUsedMap.getOrDefault("potionOrder", potionOrder);
		potionLimit = Integer.parseInt(lastUsedMap.getOrDefault("potionLimit", ""+potionLimit));
		
		pvpOpponent = Integer.parseInt(lastUsedMap.getOrDefault("pvpOpponent", ""+pvpOpponent));
		difficulty = Integer.parseInt(lastUsedMap.getOrDefault("difficulty", ""+difficulty));
		minSolo  = Integer.parseInt(lastUsedMap.getOrDefault("minSolo", ""+minSolo));

        setDungeonsFromString(lastUsedMap.getOrDefault("dungeons", getDungeonsAsString()));
        setThursdayDungeonsFromString(lastUsedMap.getOrDefault("thursdayDungeons", getThursdayDungeonsAsString()));
        setRaidsFromString(lastUsedMap.getOrDefault("raids", getRaidsAsString()));
        setThursdayRaidsFromString(lastUsedMap.getOrDefault("thursdayRaids", getThursdayRaidsAsString()));
        setExpeditionsFromString(lastUsedMap.getOrDefault("expeditions", getExpeditionsAsString()));
        setStripsFromString(lastUsedMap.getOrDefault("pvpstrip", getStripsAsString()));
        setGVGStripsFromString(lastUsedMap.getOrDefault("gvgstrip", getGVGStripsAsString()));

        autoConsume = lastUsedMap.getOrDefault("autoconsume", autoConsume ? "1" : "0").equals("1");
        setConsumablesFromString(lastUsedMap.getOrDefault("consumables", getConsumablesAsString()));

        contributeFamiliars = lastUsedMap.getOrDefault("contributeFamiliars", contributeFamiliars ? "1" : "0").equals("1");
        setFamiliarsFromString(lastUsedMap.getOrDefault("familiars", getFamiliarsAsString()));
        familiarScreenshot  = Integer.parseInt(lastUsedMap.getOrDefault("familiarScreenshot", ""+familiarScreenshot));

        collectBounties = lastUsedMap.getOrDefault("collectBounties", collectBounties ? "1" : "0").equals("1");
        collectFishingBaits = lastUsedMap.getOrDefault("collectFishingBaits", collectFishingBaits ? "1" : "0").equals("1");

        openSkeleton = Integer.parseInt(lastUsedMap.getOrDefault("openSkeletonChest", ""+openSkeleton));

        dungeonsRun = "dungeonsrun " + lastUsedMap.getOrDefault("dungeonsrun", dungeonsRun);
        worldBossRun = "worldbossrun " + lastUsedMap.getOrDefault("worldbossrun", worldBossRun);

		setAutoShrineFromString(lastUsedMap.getOrDefault("autoShrine", getAutoShrineAsString()));
        battleDelay = Integer.parseInt(lastUsedMap.getOrDefault("battleDelay", ""+battleDelay));
        shrineDelay = Integer.parseInt(lastUsedMap.getOrDefault("shrineDelay", ""+shrineDelay));

        persuasionLevel = Integer.parseInt(lastUsedMap.getOrDefault("persuasionLevel", ""+persuasionLevel));
        bribeLevel = Integer.parseInt(lastUsedMap.getOrDefault("bribeLevel", ""+bribeLevel));

        logMaxDays  = Integer.parseInt(lastUsedMap.getOrDefault("logMaxDays", ""+ logMaxDays));
        logBaseDir = lastUsedMap.getOrDefault("logBaseDir", logBaseDir);
        setLogLevelFromString(lastUsedMap.getOrDefault("logLevel", getLogLevelAsString()));
    }
	
	/** Loads settings from disk. */
	public void load() {
		load(DEFAULT_SETTINGS_FILE);
		checkDeprecatedSettings();
		sanitizeSetting();
	}
	
	/** Loads settings from disk. */
	public void load(String file) {
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
	}

	void sanitizeSetting() {
		// we check and sanitize expeditions values
		String expeditions = lastUsedMap.getOrDefault("expeditions", "");
		if (expeditions.contains("h") || expeditions.contains("i")) {
			BHBot.logger.warn("WARNING: invalid format detected for expeditions settings '" + expeditions + "': " +
					"a standard value of 'p1 100 100' will be used" );
			lastUsedMap.put("expeditions", "p1 100 25;p2 100 25;p3 100 25;p4 100 25");
			setExpeditionsFromString("p1 100 25;p2 100 25;p3 100 25;p4 100 25");
		}

		// sanitize autorevive settings
		String autoRevive = lastUsedMap.getOrDefault("autoRevive", "");
		if (autoRevive.contains("1") || autoRevive.contains("2") || autoRevive.contains("3")) {
            BHBot.logger.warn("WARNING: invalid format detected for autoRevive setting '" + autoRevive + "': " +
					"this feature will be disabled" );
			lastUsedMap.put("autoRevive", "");
			setAutoReviveFromString("");
		}
		
		// sanitize autoshrine settings
		String autoShrine= lastUsedMap.getOrDefault("autoShrine", "");
		if (autoShrine.contains("1") || autoShrine.contains("2") || autoShrine.contains("3")) {
            BHBot.logger.warn("WARNING: invalid format detected for autoRevive setting '" + autoShrine + "': " +
					"this feature will be disabled" );
			lastUsedMap.put("autoShrine", "");
			setAutoShrineFromString("");
		}

		// sanitize pvpOpponent setting
		int pvpOpponentTmp = Integer.parseInt(lastUsedMap.getOrDefault("pvpOpponent", ""+pvpOpponent));
		if (pvpOpponentTmp <1 || pvpOpponent > 4) {
			BHBot.logger.warn("WARNING: invalid value for pvpOpponent setting '" + pvpOpponentTmp + "': " +
					"setting it to '1'" );
			lastUsedMap.put("pvpOpponent", "1");
			pvpOpponent = 1;
		}

	}
}
