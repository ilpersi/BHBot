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
	boolean experimentalAutoRevive = false; // if true, game window will be hidden upon driver (re)start
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
	
	/** The Expedition difficulty */
    int expeditionDifficulty = 50;
	
	/** list of expedtion portals and chance to run, similar formatting to dungeons */
    List<String> expeditions;
	
	/**
	 * List of dungeons with percentages that we will attempt to do. Dungeon name must be in standard format, i.e. 'd2z4',
	 * followed by a space character and a difficulty level (1-3, where 1 is normal, 2 is hard, 3 is heroic), e.g. '3',
	 * and followed by a space character and percentage, e.g. '50'.
	 * Example of full string: 'z2d4 3 50'.
	 */
    List<String> dungeons;
	List<String> thursdayDungeons;
	
	/**
	 * List of raids we want to do (there are 3 raids: 1, 2 and 3) with a difficulty level and percentage.
	 * Examples:
	 * '1 3 70;2 1 30' ==> in 70% of cases it will do R1 on heroic, in 30% of cases it will do R2 normal
	 * '1 3 100' ==> in 100% of cases it will do R1 on heroic
	 */
    List<String> raids;
	List<String> thursdayRaids;
	
	/** World Boss Settings **/
    String worldBossType = "";
	int worldBossTier  =  0;
	int worldBossTimer = 0;
	int worldBossDifficulty = 0;
	boolean worldBossSolo = false;
	
	/** Autorevive Settings **/
    int autoRevive = 0;
	String potionOrder = "123";
	boolean tankPriority = false;
	
	/** Autoshrine settings **/
    boolean autoShrine = false;
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

	public Settings() {
		dungeons = new ArrayList<>();
		setDungeons("z1d4 3 100"); // some default value
		thursdayDungeons = new ArrayList<>();
		setThursdayDungeons(""); // default is empty, else if people delete the line it will load this value 
		raids = new ArrayList<>();
		setRaids("1 3 100"); // some default value
		thursdayRaids = new ArrayList<>();
		thursdayDungeons = new ArrayList<>();
		setThursdayRaids(""); // default is empty, else if people delete the line it will load this value 
		expeditions = new ArrayList<>();
		setExpeditions("h1 100"); // some default value
		pvpstrip = new ArrayList<>();
		gvgstrip = new ArrayList<>();
		consumables = new ArrayList<>();
		familiars = new ArrayList<>();
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
		expeditionDifficulty = 100;
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
//		BHBot.main.idleMode = true;
		BHBot.log("Idle mode started, no actions will be taken");
	}
	
	/* Cleans the data from the input and saves it at a string */
	
	private void setDungeons(String... dungeons) {
		this.dungeons.clear();
		for (String d : dungeons) {
			String add = d.trim();
			if (add.equals(""))
				continue;
			this.dungeons.add(add);
		}
	}
	
	private void setThursdayDungeons(String... thursdayDungeons) {
		this.thursdayDungeons.clear();
		for (String td : thursdayDungeons) {
			String add = td.trim();
			if (add.equals(""))
				continue;
			this.thursdayDungeons.add(add);
		}
	}
	
	private void setExpeditions(String... expeditions) {
		this.expeditions.clear();
		for (String e : expeditions) {
			String add = e.trim();
			if (add.equals(""))
				continue;
			this.expeditions.add(add);
		}
	}
	
	private void setRaids(String... raids) {
		this.raids.clear();
		for (String r : raids) {
			String add = r.trim();
			if (add.equals(""))
				continue;
			this.raids.add(add);
		}
	}
	
	private void setThursdayRaids(String... thursdayRaids) {
		this.thursdayRaids.clear();
		for (String tr : thursdayRaids) {
			String add = tr.trim();
			if (add.equals(""))
				continue;
			this.thursdayRaids.add(add);
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
		StringBuilder result = new StringBuilder();
		for (String d : dungeons)
			result.append(d).append(";");
		if (result.length() > 0)
			result = new StringBuilder(result.substring(0, result.length() - 1)); // remove last ";" character
		return result.toString();
	}
	
	private String getThursdayDungeonsAsString() {
		StringBuilder result = new StringBuilder();
		for (String td : thursdayDungeons)
			result.append(td).append(";");
		if (result.length() > 0)
			result = new StringBuilder(result.substring(0, result.length() - 1)); // remove last ";" character
		return result.toString();
	}
	
	private String getExpeditionsAsString() {
		StringBuilder result = new StringBuilder();
		for (String e : expeditions)
			result.append(e).append(";");
		if (result.length() > 0)
			result = new StringBuilder(result.substring(0, result.length() - 1)); // remove last ";" character
		return result.toString();
	}

	private String getRaidsAsString() {
		StringBuilder result = new StringBuilder();
		for (String r : raids)
			result.append(r).append(";");
		if (result.length() > 0)
			result = new StringBuilder(result.substring(0, result.length() - 1)); // remove last ";" character
		return result.toString();
	}
	
	private String getThursdayRaidsAsString() {
		StringBuilder result = new StringBuilder();
		for (String tr : thursdayRaids)
			result.append(tr).append(";");
		if (result.length() > 0)
			result = new StringBuilder(result.substring(0, result.length() - 1)); // remove last ";" character
		return result.toString();
	}

	private String getStripsAsString() {
		StringBuilder result = new StringBuilder();
		for (String s : pvpstrip)
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
	
	/* Cleans up the data in the list again */
	
	private void setDungeonsFromString(String s) {
		setDungeons(s.split(";"));
		// clean up (trailing spaces and remove if empty):
		for (int i = dungeons.size()-1; i >= 0; i--) {
			dungeons.set(i, dungeons.get(i).trim());
			if (dungeons.get(i).equals(""))
				dungeons.remove(i);
		}
	}
	
	private void setThursdayDungeonsFromString(String s) {
		setThursdayDungeons(s.split(";"));
		// clean up (trailing spaces and remove if empty):
		for (int i = thursdayDungeons.size()-1; i >= 0; i--) {
			thursdayDungeons.set(i, thursdayDungeons.get(i).trim());
			if (thursdayDungeons.get(i).equals(""))
				thursdayDungeons.remove(i);
		}
	}
	
	private void setExpeditionsFromString(String s) {
		setExpeditions(s.split(";"));
		// clean up (trailing spaces and remove if empty):
		for (int i = expeditions.size()-1; i >= 0; i--) {
			expeditions.set(i, expeditions.get(i).trim());
			if (expeditions.get(i).equals(""))
				expeditions.remove(i);
		}
	}
	
	private void setRaidsFromString(String s) {
		setRaids(s.split(";"));
		// clean up (trailing spaces and remove if empty):
		for (int i = raids.size()-1; i >= 0; i--) {
			raids.set(i, raids.get(i).trim());
			if (raids.get(i).equals(""))
				raids.remove(i);
		}
	}
	
	private void setThursdayRaidsFromString(String s) {
		setThursdayRaids(s.split(";"));
		// clean up (trailing spaces and remove if empty):
		for (int i = thursdayRaids.size()-1; i >= 0; i--) {
			thursdayRaids.set(i, thursdayRaids.get(i).trim());
			if (thursdayRaids.get(i).equals(""))
				thursdayRaids.remove(i);
		}
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
	
	/**
	 * Loads settings from list of string arguments (which are lines of the settings.ini file, for example)
	 */
	public void load(List<String> lines) {
		Map<String, String> map = new HashMap<>();
		for (String line : lines) {
			if (line.trim().equals("")) continue;
			if (line.startsWith("#")) continue; // a comment
			map.put(line.substring(0, line.indexOf(" ")), line.substring(line.indexOf(" ")+1));
		}

		checkDeprecatedSettings(map);
		
		username = map.getOrDefault("username", username);
		password = map.getOrDefault("password", password);
		poAppToken = map.getOrDefault("poAppToken", poAppToken);
		poUserToken = map.getOrDefault("poUserToken", poUserToken);
		useHeadlessMode = map.getOrDefault("headlessmode", useHeadlessMode ? "1" : "0").equals("1");
		restartAfterAdOfferTimeout = map.getOrDefault("restartAfterAdOfferTimeout", restartAfterAdOfferTimeout ? "1" : "0").equals("1");
		debugDetectionTimes = map.getOrDefault("debugDetectionTimes", debugDetectionTimes ? "1" : "0").equals("1");
		hideWindowOnRestart = map.getOrDefault("hideWindowOnRestart", hideWindowOnRestart ? "1" : "0").equals("1");
		experimentalAutoRevive = map.getOrDefault("experimentalAutoRevive", experimentalAutoRevive ? "1" : "0").equals("1");
		resetTimersOnBattleEnd = map.getOrDefault("resetTimersOnBattleEnd", resetTimersOnBattleEnd ? "1" : "0").equals("1");
		autoStartChromeDriver = map.getOrDefault("autoStartChromeDriver", autoStartChromeDriver ? "1" : "0").equals("1");
		reconnectTimer = Integer.parseInt(map.getOrDefault("reconnectTimer", ""+reconnectTimer));
		
		doRaids = map.getOrDefault("doRaids", doRaids ? "1" : "0").equals("1");
		doDungeons = map.getOrDefault("doDungeons", doDungeons ? "1" : "0").equals("1");
		doTrials = map.getOrDefault("doTrials", doTrials ? "1" : "0").equals("1");
		doGauntlet = map.getOrDefault("doGauntlet", doGauntlet ? "1" : "0").equals("1");
		doPVP = map.getOrDefault("doPVP", doPVP ? "1" : "0").equals("1");
		doGVG = map.getOrDefault("doGVG", doGVG ? "1" : "0").equals("1");
		doInvasion = map.getOrDefault("doInvasion", doInvasion ? "1" : "0").equals("1");
		doExpedition = map.getOrDefault("doExpedition", doExpedition ? "1" : "0").equals("1");
		doWorldBoss = map.getOrDefault("doWorldBoss", doWorldBoss ? "1" : "0").equals("1");
		enablePushover = map.getOrDefault("enablePushover", enablePushover ? "1" : "0").equals("1");
		poNotifyPM = map.getOrDefault("poNotifyPM", poNotifyPM ? "1" : "0").equals("1");
		poNotifyCrash = map.getOrDefault("poNotifyCrash", poNotifyCrash ? "1" : "0").equals("1");
		poNotifyErrors = map.getOrDefault("poNotifyErrors", poNotifyErrors ? "1" : "0").equals("1");
		poNotifyBribe = map.getOrDefault("poNotifyBribe", poNotifyBribe ? "1" : "0").equals("1");
		doAds = map.getOrDefault("doAds", doAds ? "1" : "0").equals("1");
		
		maxShards = Integer.parseInt(map.getOrDefault("maxShards", ""+maxShards));
		maxTokens = Integer.parseInt(map.getOrDefault("maxTokens", ""+maxTokens));
		maxTickets = Integer.parseInt(map.getOrDefault("maxTickets", ""+maxTickets));
		maxBadges = Integer.parseInt(map.getOrDefault("maxBadges", ""+maxBadges));
		
		minShards = Integer.parseInt(map.getOrDefault("minShards", ""+minShards));
		minTokens = Integer.parseInt(map.getOrDefault("minTokens", ""+minTokens));
		minEnergyPercentage = Integer.parseInt(map.getOrDefault("minEnergyPercentage", ""+minEnergyPercentage));
		minTickets = Integer.parseInt(map.getOrDefault("minTickets", ""+minTickets));
		minBadges = Integer.parseInt(map.getOrDefault("minBadges", ""+minBadges));

		poNotifyAlive = Integer.parseInt(map.getOrDefault("poNotifyAlive", ""+poNotifyAlive));

		costPVP = Integer.parseInt(map.getOrDefault("costPVP", ""+costPVP));
		costGVG = Integer.parseInt(map.getOrDefault("costGVG", ""+costGVG));
		costTrials = Integer.parseInt(map.getOrDefault("costTrials", ""+costTrials));
		costGauntlet = Integer.parseInt(map.getOrDefault("costGauntlet", ""+costGauntlet));
		costInvasion = Integer.parseInt(map.getOrDefault("costInvasion", ""+costInvasion));
		costExpedition = Integer.parseInt(map.getOrDefault("costExpedition", ""+costExpedition));
		
		worldBossType = map.getOrDefault("worldBossType", worldBossType);
		worldBossDifficulty = Integer.parseInt(map.getOrDefault("worldBossDifficulty", ""+worldBossDifficulty));
		worldBossTier = Integer.parseInt(map.getOrDefault("worldBossTier", ""+worldBossTier));
		worldBossTimer = Integer.parseInt(map.getOrDefault("worldBossTimer", ""+worldBossTimer));
		dungeonOnTimeout = map.getOrDefault("dungeonOnTimeout", dungeonOnTimeout ? "1" : "0").equals("1");
		worldBossSolo = map.getOrDefault("worldBossSolo", worldBossSolo ? "1" : "0").equals("1");
		
		autoRevive = Integer.parseInt(map.getOrDefault("autoRevive", ""+autoRevive));
		tankPriority = map.getOrDefault("tankPriority", tankPriority ? "1" : "0").equals("1");
		potionOrder  = map.getOrDefault("potionOrder", potionOrder);
		
		difficulty = Integer.parseInt(map.getOrDefault("difficulty", ""+difficulty));
		expeditionDifficulty = Integer.parseInt(map.getOrDefault("expeditionDifficulty", ""+expeditionDifficulty));
		minSolo  = Integer.parseInt(map.getOrDefault("minSolo", ""+minSolo));
		
		setDungeonsFromString(map.getOrDefault("dungeons", getDungeonsAsString()));
		setThursdayDungeonsFromString(map.getOrDefault("thursdayDungeons", getThursdayDungeonsAsString()));
		setRaidsFromString(map.getOrDefault("raids", getRaidsAsString()));
		setThursdayRaidsFromString(map.getOrDefault("thursdayRaids", getThursdayRaidsAsString()));
		setExpeditionsFromString(map.getOrDefault("expeditions", getExpeditionsAsString()));
		setStripsFromString(map.getOrDefault("pvpstrip", getStripsAsString()));
		setGVGStripsFromString(map.getOrDefault("gvgstrip", getGVGStripsAsString()));
		
		autoConsume = map.getOrDefault("autoconsume", autoConsume ? "1" : "0").equals("1");
		setConsumablesFromString(map.getOrDefault("consumables", getConsumablesAsString()));

		contributeFamiliars = map.getOrDefault("contributeFamiliars", contributeFamiliars ? "1" : "0").equals("1");
		setFamiliarsFromString(map.getOrDefault("familiars", getFamiliarsAsString()));
		familiarScreenshot  = Integer.parseInt(map.getOrDefault("familiarScreenshot", ""+familiarScreenshot));
		
		collectBounties = map.getOrDefault("collectBounties", collectBounties ? "1" : "0").equals("1");
		collectFishingBaits = map.getOrDefault("collectFishingBaits", collectFishingBaits ? "1" : "0").equals("1");

		openSkeleton = Integer.parseInt(map.getOrDefault("openSkeletonChest", ""+openSkeleton));	

		dungeonsRun = "dungeonsrun " + map.getOrDefault("dungeonsrun", dungeonsRun);
		worldBossRun = "worldbossrun " + map.getOrDefault("worldbossrun", worldBossRun);
		
		autoShrine = map.getOrDefault("autoShrine", autoShrine ? "1" : "0").equals("1");
		battleDelay = Integer.parseInt(map.getOrDefault("battleDelay", ""+battleDelay));
		shrineDelay = Integer.parseInt(map.getOrDefault("shrineDelay", ""+shrineDelay));

		persuasionLevel = Integer.parseInt(map.getOrDefault("persuasionLevel", ""+persuasionLevel));
		bribeLevel = Integer.parseInt(map.getOrDefault("bribeLevel", ""+bribeLevel));
	}
	
	/** Loads settings from disk. */
	public void load() {
		load(DEFAULT_SETTINGS_FILE);
	}
	
	/** Loads settings from disk. */
	public void load(String file) {
		List<String> lines = Misc.readTextFile2(file);
		if (lines == null || lines.size() == 0)
			return;
		
		load(lines);
	}

	private void checkDeprecatedSettings(Map<String, String> map) {
		if (map.getOrDefault("autoBribe", null) != null) {
			BHBot.log("Deprecated setting detected: autoBribe. Ignoring it, use a combination of bribeLevel and familiars instead.");
		}

		if (map.getOrDefault("pauseOnDisconnect", null) != null) {
			BHBot.log("Deprecated setting detected: pauseOnDisconnect. Ignoring it, use reconnectTimer instead.");
		}
	}
}
