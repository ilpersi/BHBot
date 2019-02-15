import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Settings {
	public static final String DEFAULT_SETTINGS_FILE = "settings.ini";
	
	public String username = "";
	public String password = "";
	/** Experimental feature. Better use 'false' for now. */
	public boolean useHeadlessMode = false; // run Chrome with --headless switch?
	public boolean restartAfterAdOfferTimeout = true; // if true, then bot will automatically restart itself if it hasn't claimed any ad offer in a time longer than defined. This is needed because ads don't appear anymore if Chrome doesn't get restarted.
	public boolean debugDetectionTimes = false; // if true, then each time a cue detection from game screenshot will be attempted, a time taken will be displayed together with a name of the cue
	public boolean hideWindowOnRestart = false; // if true, game window will be hidden upon driver (re)start
	public boolean resetTimersOnBattleEnd = true; // if true, readout timers will get reset once dungeon is cleared (or pvp or gvg or any other type of battle)
	public int openSkeleton = 0;
	public boolean autoBribe  = false;
	public boolean collectBounties  = false;
	public boolean dungeonOnTimeout = true;
	public boolean countActivities = false;
	
	public boolean doRaids = false;
	public boolean doDungeons = false;
	public boolean doTrials = false;
	public boolean doGauntlet = false;
	public boolean doPVP = false;
	public boolean doGVG = false;
	public boolean doAds = false;
	public boolean doInvasion = false;
	public boolean doExpedition = false;
	public boolean doWorldBoss = false;
	
	/** This is the minimum amount of shards that the bot must leave for the user. If shards get above this value, bot will play the raids in case raiding is enabled of course. */
	public int minShards = 2;
	/** This is the minimum amount of tokens that the bot must leave for the user. If tokens get above this value, bot will play the trials/gauntlet in case trials/gauntlet is enabled of course. */
	public int minTokens = 5;
	/** This is the minimum amount of energy as percentage that the bot must leave for the user. If energy is higher than that, then bot will attempt to play dungeons. */
	public int minEnergyPercentage = 70;
	/** This is the minimum amount of tickets that the bot must leave for the user. If tickets get above this value, bot will play the pvp in case pvp is enabled of course. */
	public int minTickets = 5;
	/** This is the minimum amount of badges that the bot must leave for the user. If badges get above this value, bot will play the gvg in case gvg is enabled of course. */
	public int minBadges = 5;
	
	// Max for various expendables for correct calculation if not default
	public int maxShards = 4;
	public int maxTokens =  10;
	public int maxTickets = 10;
	public int maxBadges = 10;
	
	// costs (1..5) for various events:
	public int costPVP = 1;
	public int costGVG = 1;
	public int costTrials = 1;
	public int costGauntlet = 1;
	public int costInvasion = 1;
	public int costExpedition = 1;
	
	/** The trials/gauntlet difficulty */
	public int difficulty = 60;
	
	/** The Expedition difficulty */
	public int expeditionDifficulty = 50;
	
	/** list of expedtion portals and chance to run, similar formatting to dungeons */
	public List<String> expeditions;
	
	/**
	 * List of dungeons with percentages that we will attempt to do. Dungeon name must be in standard format, i.e. 'd2z4',
	 * followed by a space character and a difficulty level (1-3, where 1 is normal, 2 is hard, 3 is heroic), e.g. '3',
	 * and followed by a space character and percentage, e.g. '50'.
	 * Example of full string: 'z2d4 3 50'.
	 */
	public List<String> dungeons;
	public List<String> thursdayDungeons;
	
	/**
	 * List of raids we want to do (there are 3 raids: 1, 2 and 3) with a difficulty level and percentage.
	 * Examples:
	 * '1 3 70;2 1 30' ==> in 70% of cases it will do R1 on heroic, in 30% of cases it will do R2 normal
	 * '1 3 100' ==> in 100% of cases it will do R1 on heroic
	 */
	public List<String> raids;
	public List<String> thursdayRaids;
	
	/** World Boss Settings **/
	public String worldBossType = "";
	public int worldBossTier  =  0;
	public int worldBossTimer = 0;
	public int worldBossDifficulty = 0;
	
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
	public List<String> pvpstrip;
	
	/** If true, then bot will try to auto consume consumables as specified by the 'consumables' list. */
	public boolean autoConsume = false;
	/** List of consumables that we want activate at all times. */
	public List<String> consumables;
	public List<String> familiars;
	
	/** Development Settings **/
	public boolean familiarScreenshot = false;
	public int minSolo = 2;
	public String dungeonsRun = "dungeonsrun 0";
	public String worldBossRun = "worldbossrun 0";

	/** This tells us how much time will we sleep when disconnect has been detected (which happens when a user logs in). This interval should be an hour or so, so that user can play the game in peace without being disconnected due to us reconnecting to the game. */
	public int pauseOnDisconnect = 60*MainThread.MINUTE;
	
	public Settings() {
		dungeons = new ArrayList<String>();
		setDungeons(""); // some default value
		thursdayDungeons = new ArrayList<String>();
		setThursdayDungeons(""); // some default value
		raids = new ArrayList<String>();
		setRaids(""); // some default value
		thursdayRaids = new ArrayList<String>();
		thursdayDungeons = new ArrayList<String>();
		setThursdayRaids(""); // some default value("z1d1 3 100"); // some default value
		expeditions = new ArrayList<String>();
		setExpeditions("h1 100"); // some default value
		pvpstrip = new ArrayList<String>();
		consumables = new ArrayList<String>();
		familiars = new ArrayList<String>();
	}
	
	public void set(Settings settings) {
		this.username = settings.username;
		this.password = settings.password;
		this.useHeadlessMode = settings.useHeadlessMode;
		this.restartAfterAdOfferTimeout = settings.restartAfterAdOfferTimeout;
		this.debugDetectionTimes = settings.debugDetectionTimes;
		this.hideWindowOnRestart = settings.hideWindowOnRestart;
		this.resetTimersOnBattleEnd = settings.resetTimersOnBattleEnd;
		this.dungeonOnTimeout = settings.dungeonOnTimeout;
		
		this.doRaids = settings.doRaids;
		this.doDungeons = settings.doDungeons;
		this.doTrials = settings.doTrials;
		this.doGauntlet = settings.doGauntlet;
		this.doPVP = settings.doPVP;
		this.doGVG = settings.doGVG;
		this.doInvasion = settings.doInvasion;
		this.doAds = settings.doAds;
		this.doExpedition = settings.doExpedition;
		this.doWorldBoss =  settings.doWorldBoss;
		
		this.maxShards = settings.maxShards;
		this.maxTokens = settings.maxTokens;
		this.maxTickets = settings.maxTickets;
		this.maxBadges = settings.maxBadges;		
		
		this.minShards = settings.minShards;
		this.minTokens = settings.minTokens;
		this.minEnergyPercentage = settings.minEnergyPercentage;
		this.minTickets = settings.minTickets;
		this.minBadges = settings.minBadges;
		
		this.costPVP = settings.costPVP;
		this.costGVG = settings.costGVG;
		this.costTrials = settings.costTrials;
		this.costGauntlet = settings.costGauntlet;
		this.costInvasion = settings.costInvasion;
		this.costExpedition = settings.costExpedition;
		
		this.difficulty = settings.difficulty;
		this.expeditionDifficulty = settings.expeditionDifficulty;
		
		this.dungeons = new ArrayList<String>(settings.dungeons);
		this.raids = new ArrayList<String>(settings.raids);
		this.thursdayDungeons = new ArrayList<String>(settings.thursdayDungeons);
		this.thursdayRaids = new ArrayList<String>(settings.thursdayRaids);
		this.expeditions = new ArrayList<String>(settings.expeditions);	
		this.pvpstrip = new ArrayList<String>(settings.pvpstrip);
		this.minSolo  = settings.minSolo;
		
		this.autoConsume = settings.autoConsume;
		this.consumables = new ArrayList<String>(settings.consumables);
		
		this.autoBribe = settings.autoBribe;
		this.familiars = new ArrayList<String>(settings.familiars);
		
		this.pauseOnDisconnect = settings.pauseOnDisconnect;
		this.openSkeleton = settings.openSkeleton;
		this.collectBounties = settings.collectBounties;
		this.dungeonsRun = settings.dungeonsRun;
		this.worldBossRun = settings.worldBossRun;
		
		this.worldBossType = settings.worldBossType;
		this.worldBossTier  =  settings.worldBossTier;
		this.worldBossTimer = settings.worldBossTimer;
		this.worldBossDifficulty = settings.worldBossTimer;
	}
	
	// a handy shortcut for some debug settings:
	public Settings setDebug() {
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
	public Settings setIdle() {
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
		autoConsume = false;
		collectBounties = false;
		BHBot.log("Idle mode started, no actions will be taken");
		
		return this; // for chaining
	}
	
	/* Cleans the data from the input and saves it at a string */
	
	public void setDungeons(String... dungeons) {
		this.dungeons.clear();
		for (String d : dungeons) {
			String add = d.trim();
			if (add.equals(""))
				continue;
			this.dungeons.add(add);
		}
	}
	
	public void setThursdayDungeons(String... thursdayDungeons) {
		this.thursdayDungeons.clear();
		for (String td : thursdayDungeons) {
			String add = td.trim();
			if (add.equals(""))
				continue;
			this.thursdayDungeons.add(add);
		}
	}
	
	public void setExpeditions(String... expeditions) {
		this.expeditions.clear();
		for (String e : expeditions) {
			String add = e.trim();
			if (add.equals(""))
				continue;
			this.expeditions.add(add);
		}
	}
	
	public void setRaids(String... raids) {
		this.raids.clear();
		for (String r : raids) {
			String add = r.trim();
			if (add.equals(""))
				continue;
			this.raids.add(add);
		}
	}
	
	public void setThursdayRaids(String... thursdayRaids) {
		this.thursdayRaids.clear();
		for (String tr : thursdayRaids) {
			String add = tr.trim();
			if (add.equals(""))
				continue;
			this.thursdayRaids.add(add);
		}
	}
	
	public void setStrips(String... types) {
		this.pvpstrip.clear();
		for (String t : types) {
			String add = t.trim();
			if (add.equals(""))
				continue;
			this.pvpstrip.add(add);
		}
	}
	
	public void setConsumables(String... items) {
		this.consumables.clear();
		for (String i : items) {
			String add = i.trim();
			if (add.equals(""))
				continue;
			this.consumables.add(add);
		}
	}
	
	public void setFamiliars(String... fams) {
		this.familiars.clear();
		for (String f : fams) {
			String add = f.trim();
			if (add.equals(""))
				continue;
			this.familiars.add(add);
		}
	}
	
	/* Gets the string from the previous method and creates a list if there are multiple items */
	
	public String getDungeonsAsString() {
		String result = "";
		for (String d : dungeons)
			result += d + ";";
		if (result.length() > 0)
			result = result.substring(0, result.length()-1); // remove last ";" character
		return result;
	}
	
	public String getThursdayDungeonsAsString() {
		String result = "";
		for (String td : thursdayDungeons)
			result += td + ";";
		if (result.length() > 0)
			result = result.substring(0, result.length()-1); // remove last ";" character
		return result;
	}
	
	public String getExpeditionsAsString() {
		String result = "";
		for (String e : expeditions)
			result += e + ";";
		if (result.length() > 0)
			result = result.substring(0, result.length()-1); // remove last ";" character
		return result;
	}

	public String getRaidsAsString() {
		String result = "";
		for (String r : raids)
			result += r + ";";
		if (result.length() > 0)
			result = result.substring(0, result.length()-1); // remove last ";" character
		return result;
	}
	
	public String getThursdayRaidsAsString() {
		String result = "";
		for (String tr : thursdayRaids)
			result += tr + ";";
		if (result.length() > 0)
			result = result.substring(0, result.length()-1); // remove last ";" character
		return result;
	}

	public String getStripsAsString() {
		String result = "";
		for (String s : pvpstrip)
			result += s + " ";
		if (result.length() > 0)
			result = result.substring(0, result.length()-1); // remove last " " character
		return result;
	}
	
	public String getConsumablesAsString() {
		String result = "";
		for (String s : consumables)
			result += s + " ";
		if (result.length() > 0)
			result = result.substring(0, result.length()-1); // remove last " " character
		return result;
	}
	
	public String getFamiliarsAsString() {
		String result = "";
		for (String f : familiars)
			result += f + ";";
		if (result.length() > 0)
			result = result.substring(0, result.length()-1); // remove last ";" character
		return result;
	}
	
	/* Cleans up the data in the list again */
	
	public void setDungeonsFromString(String s) {
		setDungeons(s.split(";"));
		// clean up (trailing spaces and remove if empty):
		for (int i = dungeons.size()-1; i >= 0; i--) {
			dungeons.set(i, dungeons.get(i).trim());
			if (dungeons.get(i).equals(""))
				dungeons.remove(i);
		}
	}
	
	public void setThursdayDungeonsFromString(String s) {
		setThursdayDungeons(s.split(";"));
		// clean up (trailing spaces and remove if empty):
		for (int i = thursdayDungeons.size()-1; i >= 0; i--) {
			thursdayDungeons.set(i, thursdayDungeons.get(i).trim());
			if (thursdayDungeons.get(i).equals(""))
				thursdayDungeons.remove(i);
		}
	}
	
	public void setExpeditionsFromString(String s) {
		setExpeditions(s.split(";"));
		// clean up (trailing spaces and remove if empty):
		for (int i = expeditions.size()-1; i >= 0; i--) {
			expeditions.set(i, expeditions.get(i).trim());
			if (expeditions.get(i).equals(""))
				expeditions.remove(i);
		}
	}
	
	public void setRaidsFromString(String s) {
		setRaids(s.split(";"));
		// clean up (trailing spaces and remove if empty):
		for (int i = raids.size()-1; i >= 0; i--) {
			raids.set(i, raids.get(i).trim());
			if (raids.get(i).equals(""))
				raids.remove(i);
		}
	}
	
	public void setThursdayRaidsFromString(String s) {
		setThursdayRaids(s.split(";"));
		// clean up (trailing spaces and remove if empty):
		for (int i = thursdayRaids.size()-1; i >= 0; i--) {
			thursdayRaids.set(i, thursdayRaids.get(i).trim());
			if (thursdayRaids.get(i).equals(""))
				thursdayRaids.remove(i);
		}
	}

	public void setStripsFromString(String s) {
		setStrips(s.split(" "));
		// clean up (trailing spaces and remove if empty):
		for (int i = pvpstrip.size()-1; i >= 0; i--) {
			pvpstrip.set(i, pvpstrip.get(i).trim());
			if (pvpstrip.get(i).equals(""))
				pvpstrip.remove(i);
		}
	}
	
	public void setConsumablesFromString(String s) {
		setConsumables(s.split(" "));
		// clean up (trailing spaces and remove if empty):
		for (int i = consumables.size()-1; i >= 0; i--) {
			consumables.set(i, consumables.get(i).trim());
			if (consumables.get(i).equals(""))
				consumables.remove(i);
		}
	}
	
	public void setFamiliarsFromString(String s) {
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
		Map<String, String> map = new HashMap<String, String>();
		for (String line : lines) {
			if (line.trim().equals("")) continue;
			if (line.startsWith("#")) continue; // a comment
			map.put(line.substring(0, line.indexOf(" ")), line.substring(line.indexOf(" ")+1));
		}
		
		username = map.getOrDefault("username", username);
		password = map.getOrDefault("password", password);
		useHeadlessMode = map.getOrDefault("headlessmode", useHeadlessMode ? "1" : "0").equals("0") ? false : true ;
		restartAfterAdOfferTimeout = map.getOrDefault("restartAfterAdOfferTimeout", restartAfterAdOfferTimeout ? "1" : "0").equals("0") ? false : true ;
		debugDetectionTimes = map.getOrDefault("debugDetectionTimes", debugDetectionTimes ? "1" : "0").equals("0") ? false : true ;
		hideWindowOnRestart = map.getOrDefault("hideWindowOnRestart", hideWindowOnRestart ? "1" : "0").equals("0") ? false : true ;
		resetTimersOnBattleEnd = map.getOrDefault("resetTimersOnBattleEnd", resetTimersOnBattleEnd ? "1" : "0").equals("0") ? false : true ;
		
		
		doRaids = map.getOrDefault("doRaids", doRaids ? "1" : "0").equals("0") ? false : true;
		doDungeons = map.getOrDefault("doDungeons", doDungeons ? "1" : "0").equals("0") ? false : true;
		doTrials = map.getOrDefault("doTrials", doTrials ? "1" : "0").equals("0") ? false : true;
		doGauntlet = map.getOrDefault("doGauntlet", doGauntlet ? "1" : "0").equals("0") ? false : true;
		doPVP = map.getOrDefault("doPVP", doPVP ? "1" : "0").equals("0") ? false : true;
		doGVG = map.getOrDefault("doGVG", doGVG ? "1" : "0").equals("0") ? false : true;
		doInvasion = map.getOrDefault("doInvasion", doInvasion ? "1" : "0").equals("0") ? false : true;
		doExpedition = map.getOrDefault("doExpedition", doExpedition ? "1" : "0").equals("0") ? false : true;
		doWorldBoss = map.getOrDefault("doWorldBoss", doWorldBoss ? "1" : "0").equals("0") ? false : true;
		doAds = map.getOrDefault("doAds", doAds ? "1" : "0").equals("0") ? false : true;
		
		maxShards = Integer.parseInt(map.getOrDefault("maxShards", ""+minShards));
		maxTokens = Integer.parseInt(map.getOrDefault("maxTokens", ""+minTokens));
		maxTickets = Integer.parseInt(map.getOrDefault("maxTickets", ""+minTickets));
		maxBadges = Integer.parseInt(map.getOrDefault("maxBadges", ""+minBadges));
		
		minShards = Integer.parseInt(map.getOrDefault("minShards", ""+minShards));
		minTokens = Integer.parseInt(map.getOrDefault("minTokens", ""+minTokens));
		minEnergyPercentage = Integer.parseInt(map.getOrDefault("minEnergyPercentage", ""+minEnergyPercentage));
		minTickets = Integer.parseInt(map.getOrDefault("minTickets", ""+minTickets));
		minBadges = Integer.parseInt(map.getOrDefault("minBadges", ""+minBadges));
		
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
		dungeonOnTimeout = map.getOrDefault("dungeonOnTimeout", dungeonOnTimeout ? "1" : "0").equals("0") ? false : true;
		
		difficulty = Integer.parseInt(map.getOrDefault("difficulty", ""+difficulty));
		expeditionDifficulty = Integer.parseInt(map.getOrDefault("expeditionDifficulty", ""+expeditionDifficulty));
		minSolo  = Integer.parseInt(map.getOrDefault("minSolo", ""+minSolo));
		
		setDungeonsFromString(map.getOrDefault("dungeons", getDungeonsAsString()));
		setThursdayDungeonsFromString(map.getOrDefault("thursdayDungeons", getThursdayDungeonsAsString()));
		setRaidsFromString(map.getOrDefault("raids", getRaidsAsString()));
		setThursdayRaidsFromString(map.getOrDefault("thursdayRaids", getThursdayRaidsAsString()));
		setExpeditionsFromString(map.getOrDefault("expeditions", getExpeditionsAsString()));
		setStripsFromString(map.getOrDefault("pvpstrip", getStripsAsString()));
		
		autoConsume = map.getOrDefault("autoconsume", autoConsume ? "1" : "0").equals("0") ? false : true;
		setConsumablesFromString(map.getOrDefault("consumables", getConsumablesAsString()));
		
		autoBribe = map.getOrDefault("autoBribe", autoBribe ? "1" : "0").equals("0") ? false : true ;
		setFamiliarsFromString(map.getOrDefault("familiars", getFamiliarsAsString()));
		familiarScreenshot = map.getOrDefault("familiarScreenshot", familiarScreenshot ? "1" : "0").equals("0") ? false : true;
		
		collectBounties = map.getOrDefault("collectBounties", collectBounties ? "1" : "0").equals("0") ? false : true ;
		
		openSkeleton = Integer.parseInt(map.getOrDefault("openSkeletonChest", ""+openSkeleton));	
		pauseOnDisconnect = Integer.parseInt(map.getOrDefault("pauseOnDisconnect", ""+pauseOnDisconnect));
		dungeonsRun = "dungeonsrun " + map.getOrDefault("dungeonsrun", dungeonsRun);
		worldBossRun = "worldbossrun " + map.getOrDefault("worldbossrun", worldBossRun);
		
		
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
}
