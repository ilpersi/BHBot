public class ExceptionManager {
    BHBot bot;

    /**
     * Number of consecutive exceptions. We need to track it in order to detect crash loops that we must break by restarting the Chrome driver. Or else it could get into loop and stale.
     */
    int numConsecutiveException = 0;

    ExceptionManager (BHBot bot) {
        this.bot = bot;
    }

    /**
     * @param e The exception that has to be managed by the Exception manager
     * @return true if the class was able to manage the exception. Please note that, whenever true is returned,
     *         the numConsecutiveException counter is increased by one and never reset to zero. Setting it to
     *         zero is up to the caller of this method. Once that the exception limit defined in
     *         MAX_CONSECUTIVE_EXCEPTIONS is reached, the bot will restart itself.
     */
    synchronized boolean manageException(Exception e) {

        numConsecutiveException++;
        int MAX_CONSECUTIVE_EXCEPTIONS = 10;
        if (numConsecutiveException > MAX_CONSECUTIVE_EXCEPTIONS) {
            numConsecutiveException = 0; // reset it
            BHBot.logger.warn("Problem detected: number of consecutive exceptions is higher than " + MAX_CONSECUTIVE_EXCEPTIONS + ". This probably means we're caught in a loop. Restarting...");
            bot.restart(true, false);
            return false;
        }

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
