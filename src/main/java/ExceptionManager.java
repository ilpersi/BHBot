public class ExceptionManager {
    BHBot bot;

    ExceptionManager (BHBot bot) {
        this.bot = bot;
    }

    synchronized boolean manageException(Exception e) {
        if (e instanceof org.openqa.selenium.WebDriverException && e.getMessage().startsWith("chrome not reachable")) {
            // this happens when user manually closes the Chrome window, for example
            BHBot.logger.error("Error: chrome is not reachable! Restarting...", e);
            bot.restart(true, false);
            return true;
        } else if (e instanceof java.awt.image.RasterFormatException) {
            // not sure in what cases this happen, but it happens
            BHBot.logger.error("Error: RasterFormatException. Attempting to re-align the window...", e);
            Misc.sleep(500);
            bot.browser.scrollGameIntoView();
            Misc.sleep(500);
            try {
                bot.browser.readScreen();
            } catch (Exception e2) {
                BHBot.logger.error("Error: re-alignment failed(" + e2.getMessage() + "). Restarting...");
                bot.restart(true, false);
                return true;
            }
            BHBot.logger.info("Realignment seems to have worked.");
            return true;
        } else if (e instanceof org.openqa.selenium.StaleElementReferenceException) {
            // this is a rare error, however it happens. See this for more info:
            // http://www.seleniumhq.org/exceptions/stale_element_reference.jsp
            BHBot.logger.error("Error: StaleElementReferenceException. Restarting...", e);
            bot.restart(true, false);
            return true;
        } else if (e instanceof com.assertthat.selenium_shutterbug.utils.web.ElementOutsideViewportException) {
            BHBot.logger.info("Error: ElementOutsideViewportException. Ignoring...");
            //added this 1 second delay as attempting ads often triggers this
            //will trigger the restart in the if statement below after 30 seconds
            Misc.sleep(Misc.Durations.SECOND);
            // we must not call 'continue' here, because this error could be a loop error, this is why we need to increase numConsecutiveException bellow in the code!
        } else if (e instanceof org.openqa.selenium.TimeoutException) {
            /* When we get time out errors it may be possible that the bot.browser has crashed so it is impossible to take screenshots
             * For this reason we do a standard restart.
             */
            bot.restart(true, false);
            return true;
        } else {
            // unknown error!
            BHBot.logger.error("Unmanaged exception in main run loop", e);
            bot.restart(true, false);
        }

        bot.scheduler.resetIdleTime(true);

        return false;
    }
}
