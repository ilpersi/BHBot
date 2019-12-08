public class BlockerThread implements Runnable {
    BHBot bot;
    MarvinSegment seg;

    BlockerThread(BHBot bot) {
        this.bot = bot;
    }

    @Override
    public void run() {
        while (!bot.finished) {

            // We wait for the cues to be loaded!
            if (BrowserManager.cues.size() == 0) {
                Misc.sleep(1000);
                continue;
            }
            
            seg = MarvinSegment.fromCue(BrowserManager.cues.get("UnableToConnect"), bot.browser);
            if (seg != null) {
                BHBot.logger.info("'Unable to connect' dialog detected. Reconnecting...");
                seg = MarvinSegment.fromCue(BrowserManager.cues.get("Reconnect"), 5 * DungeonThread.SECOND, bot.browser);
                bot.browser.clickOnSeg(seg);
                Misc.sleep(5 * DungeonThread.SECOND);
                bot.setState(BHBot.State.Loading);
                continue;
            }

            // check for "Bit Heroes is currently down for maintenance. Please check back shortly!" window:
            seg = MarvinSegment.fromCue(BrowserManager.cues.get("Maintenance"), bot.browser);
            if (seg != null) {
                seg = MarvinSegment.fromCue(BrowserManager.cues.get("Reconnect"), 5 * DungeonThread.SECOND, bot.browser);
                bot.browser.clickOnSeg(seg);
                BHBot.logger.info("Maintenance dialog dismissed.");
                Misc.sleep(5 * DungeonThread.SECOND);
                bot.setState(BHBot.State.Loading);
                continue;
            }

            // check for "You have been disconnected" dialog:
            MarvinSegment uhoh = MarvinSegment.fromCue(BrowserManager.cues.get("UhOh"), bot.browser);
            MarvinSegment dc = MarvinSegment.fromCue(BrowserManager.cues.get("Disconnected"), bot.browser);
            if (uhoh != null && dc != null) {
                if (bot.scheduler.isUserInteracting || bot.scheduler.dismissReconnectOnNextIteration) {
                    bot.scheduler.isUserInteracting = false;
                    bot.scheduler.dismissReconnectOnNextIteration = false;
                    seg = MarvinSegment.fromCue(BrowserManager.cues.get("Reconnect"), 5 * DungeonThread.SECOND, bot.browser);
                    bot.browser.clickOnSeg(seg);
                    BHBot.logger.info("Disconnected dialog dismissed (reconnecting).");
                    Misc.sleep(5 * DungeonThread.SECOND);
                } else {
                    bot.scheduler.isUserInteracting = true;
                    // probably user has logged in, that's why we got disconnected. Lets leave him alone for some time and then resume!
                    BHBot.logger.info("Disconnect has been detected. Probably due to user interaction. Sleeping for " + Misc.millisToHumanForm((long) bot.settings.reconnectTimer * DungeonThread.MINUTE) + "...");
                    bot.scheduler.pause(bot.settings.reconnectTimer * DungeonThread.MINUTE);
                }
                bot.setState(BHBot.State.Loading);
                continue;
            }

            // TODO ensure this field is properly synchronized
            bot.scheduler.dismissReconnectOnNextIteration = false; // must be done after checking for "Disconnected" dialog!

            // check for "There is a new update required to play" and click on "Reload" button:
            seg = MarvinSegment.fromCue(BrowserManager.cues.get("Reload"), bot.browser);
            if (seg != null) {
                bot.browser.clickOnSeg(seg);
                BHBot.logger.info("Update dialog dismissed.");
                Misc.sleep(5 * DungeonThread.SECOND);
                bot.setState(BHBot.State.Loading);
                continue;
            }

            // check for "Are you still there?" popup:
            seg = MarvinSegment.fromCue(BrowserManager.cues.get("AreYouThere"), bot.browser);
            if (seg != null) {
                bot.scheduler.restoreIdleTime();
                seg = MarvinSegment.fromCue(BrowserManager.cues.get("Yes"), 2 * DungeonThread.SECOND, bot.browser);
                if (seg != null)
                    bot.browser.clickOnSeg(seg);
                else {
                    BHBot.logger.info("Problem: 'Are you still there?' popup detected, but 'Yes' button not detected. Ignoring...");
                    continue;
                }
                Misc.sleep(2 * DungeonThread.SECOND);
                continue; // skip other stuff, we must first get rid of this popup!
            }

            // check for "News" popup:
            seg = MarvinSegment.fromCue(BrowserManager.cues.get("News"), bot.browser);
            if (seg != null) {
                seg = MarvinSegment.fromCue(BrowserManager.cues.get("Close"), 2 * DungeonThread.SECOND, bot.browser);
                bot.browser.clickOnSeg(seg);
                BHBot.logger.info("News popup dismissed.");
                bot.browser.readScreen(2 * DungeonThread.SECOND);
            }
        }
    }
    
    
}
