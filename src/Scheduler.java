
public class Scheduler {
	/** Instructs bot to do 1 raid immediately (after which this flag will get reset to 'false' */
	boolean doRaidImmediately;
	/** Instructs bot to do 1 dungeon immediately (after which this flag will get reset to 'false' */
	boolean doDungeonImmediately;
	/** Instructs bot to do 1 trials/gauntlet immediately (after which this flag will get reset to 'false' */
	boolean doTrialsOrGauntletImmediately;
	/** Instructs bot to do 1 PVP battle immediately (after which this flag will get reset to 'false' */
	boolean doPVPImmediately;
	/** Instructs bot to do 1 GVG battle immediately (after which this flag will get reset to 'false' */
	boolean doGVGImmediately;
	/** Instructs bot to do 1 Invasion immediately (after which this flag will get reset to 'false' */
	boolean doInvasionImmediately;
	/** Instructs bot to do 1 Expedition immediately (after which this flag will get reset to 'false' */
	boolean doExpeditionImmediately;
	
	private static final long PAUSED_INDEFINITELY = Long.MAX_VALUE;
	private boolean paused = false;
	private long pauseDuration; // in milliseconds
	private long pauseStart; // in milliseconds (related to Misc.getTime())
	
	/** Note that this is not idle time duration but rather a start time stamp! */
	private long idleTime = 0;
	private long idleTimeBackup = 0; // temp variable
	
	/** Set it to true when user logs in and we must wait for him to finish interacting. */
	public boolean isUserInteracting = false;
	
	public boolean dismissReconnectOnNextIteration = false;
	
	/**
	 * Must be called often enough in order to update internals of the scheduler.
	 */
	public void process() {
		// check if pause has expired:
		if (paused && pauseDuration != PAUSED_INDEFINITELY && (Misc.getTime() - pauseStart > pauseDuration)) {
			resume();
		}
	}
	
	public boolean isPaused() {
		if (!paused)
			return false;
		
		if (pauseDuration == PAUSED_INDEFINITELY)
			return true;
		
		// check if pause has expired:
		if (Misc.getTime() - pauseStart > pauseDuration) {
			paused = false;
			return false;
		}
		
		return true;
	}
	
	public void pause() {
		paused = true;
		pauseDuration = PAUSED_INDEFINITELY;
		BHBot.log("Paused.");
	}
	
	public void pause(int duration) {
		paused = true;
		pauseDuration = duration;
		pauseStart = Misc.getTime();
		BHBot.log("Paused for " + Misc.millisToHumanForm(duration) + ".");
	}
	
	public void resume() {
		if (!paused)
			return;
		paused = false;
		resetIdleTime();
		backupIdleTime(); // we need this because at the end of main loop idleTime will get assigned to lastIdleTime (which actually happens when game is unpaused after more than 30 minutes, for example)
		BHBot.main.timeLastAdOffer = Misc.getTime(); // reset it
		/* we need this because of this situation: user issues command "pause", 
		 * then connect to the game (so that "reconnect" dialog pops up in the bot),
		 * after a while he disconnects and issues command "resume".
		 * Now bot must dismiss the reconnect dialog and not enter 1h pause on encountering
		 * the "reconnect" dialog. With this flag, we make sure he does just that.
		 */
		dismissReconnectOnNextIteration = true; 
		BHBot.log("Resumed.");
	}
	
	/** Returns time stamp (and not duration)! */
	public long getIdleTime() {
		return idleTime;
	}
	
	public void resetIdleTime() {
		idleTime = Misc.getTime();
	}
	
	public void backupIdleTime() {
		idleTimeBackup = idleTime;
	}
	
	public void restoreIdleTime() {
		idleTime = idleTimeBackup;
	}
}
