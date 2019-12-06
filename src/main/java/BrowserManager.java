import com.assertthat.selenium_shutterbug.core.Shutterbug;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrowserManager {
    private static WebDriver driver;
    private static By byElement;
    private static JavascriptExecutor jsExecutor;
    private static WebElement game;
    private static String doNotShareUrl = "";

    private BufferedImage img; // latest screen capture

    boolean isDoNotShareUrl() {
        return !"".equals(doNotShareUrl);
    }

    ReadWriteLock lock = new ReentrantReadWriteLock();

    private void connect() throws MalformedURLException {
        lock.writeLock().lock();
        try {
            ChromeOptions options = new ChromeOptions();

            options.addArguments("user-data-dir=./chrome_profile"); // will create this profile folder where chromedriver.exe is located!
            options.setBinary(BHBot.chromiumExePath); //set Chromium v69 binary location

            if (BHBot.settings.autoStartChromeDriver) {
                System.setProperty("webdriver.chrome.driver", BHBot.chromeDriverExePath);
            } else {
                BHBot.logger.info("chromedriver auto start is off, make sure it is started before running BHBot");
                if (System.getProperty("webdriver.chrome.driver", null) != null) {
                    System.clearProperty("webdriver.chrome.driver");
                }
            }

            // disable ephemeral flash permissions flag
            options.addArguments("--disable-features=EnableEphemeralFlashPermission");
            options.addArguments("disable-infobars");

            Map<String, Object> prefs = new HashMap<>();
            // Enable flash for all sites for Chrome 69
            prefs.put("profile.content_settings.exceptions.plugins.*,*.setting", 1);
            options.setExperimentalOption("prefs", prefs);

            DesiredCapabilities capabilities = DesiredCapabilities.chrome();

            /* When we connect the driver, if we don't know the do_not_share_url and if the configs require it,
             * the bot will enable the logging of network events so that when it is fully loaded, it will be possible
             * to analyze them searching for the magic URL
             */
            if (!isDoNotShareUrl() && BHBot.settings.useDoNotShareURL) {
                LoggingPreferences logPrefs = new LoggingPreferences();
                logPrefs.enable(LogType.PERFORMANCE, Level.ALL);
                options.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);
            }

            capabilities.setCapability("chrome.verbose", false);
            capabilities.setCapability(ChromeOptions.CAPABILITY, options);
            if (BHBot.settings.autoStartChromeDriver) {
                driver = new ChromeDriver(options);
            } else {
                driver = new RemoteWebDriver(new URL("http://" + BHBot.chromeDriverAddress), capabilities);
            }
            jsExecutor = (JavascriptExecutor) driver;
        } finally {
            lock.writeLock().unlock();
        }
    }

    void restart(boolean useDoNotShareUrl) throws MalformedURLException {
        lock.writeLock().lock();
        try {
            if (useDoNotShareUrl) {
                Pattern regex = Pattern.compile("\"(https://.+?\\?DO_NOT_SHARE_THIS_LINK[^\"]+?)\"");
                for (LogEntry le : driver.manage().logs().get(LogType.PERFORMANCE)) {
                    Matcher regexMatcher = regex.matcher(le.getMessage());
                    if (regexMatcher.find()) {
                        BHBot.logger.debug("DO NOT SHARE URL found!");
                        doNotShareUrl = regexMatcher.group(1);
                        break;
                    }
                }
            }

            try {
                if (driver != null) {
                    driver.close();
                    driver.quit();
                }
            } catch (Exception e) {
                BHBot.logger.error("Error while quitting from Chromium", e);
            }

            // disable some annoying INFO messages:
            Logger.getLogger("").setLevel(Level.WARNING);

            connect();
            if (BHBot.settings.hideWindowOnRestart)
                hideBrowser();
            if ("".equals(doNotShareUrl)) {
                driver.navigate().to("http://www.kongregate.com/games/Juppiomenz/bit-heroes");
                byElement = By.id("game");
            } else {
                driver.navigate().to(doNotShareUrl);
                byElement = By.xpath("//div[1]");
            }

            game = driver.findElement(byElement);

            int vw = Math.toIntExact((Long) jsExecutor.executeScript("return window.outerWidth - window.innerWidth + arguments[0];", game.getSize().width));
            int vh = Math.toIntExact((Long) jsExecutor.executeScript("return window.outerHeight - window.innerHeight + arguments[0];", game.getSize().height));
            vw += 50; // compensate for scrollbars 70
            vh += 30; // compensate for scrollbars 50
            driver.manage().window().setSize(new Dimension(vw, vh));
        } finally {
            lock.writeLock().unlock();
        }
    }

    void close() {
        lock.writeLock().lock();
        try {
            driver.close();
            driver.quit();
        } finally {
            lock.writeLock().unlock();
        }
    }

    void hideBrowser() {
        lock.writeLock().lock();
        try {
            driver.manage().window().setPosition(new Point(-10000, 0)); // just to make sure
            BHBot.logger.info("Chrome window has been hidden.");
        } finally {
            lock.writeLock().unlock();
        }
    }

    void showBrowser() {
        lock.writeLock().lock();
        try {
            driver.manage().window().setPosition(new Point(0, 0));
            BHBot.logger.info("Chrome window has been restored.");
        } finally {
            lock.writeLock().unlock();
        }
    }

    void scrollGameIntoView() {
        lock.writeLock().lock();
        try {
            WebElement element = driver.findElement(byElement);

            String scrollElementIntoMiddle = "var viewPortHeight = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);"
                    + "var elementTop = arguments[0].getBoundingClientRect().top;"
                    + "window.scrollBy(0, elementTop-(viewPortHeight/2));";

            jsExecutor.executeScript(scrollElementIntoMiddle, element);
            Misc.sleep(1000);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * This form opens only seldom (haven't figured out what triggers it exactly - perhaps some cookie expired?). We need to handle it!
     */
    void detectSignInFormAndHandleIt() {
        lock.writeLock().lock();
        try {
            // close the popup "create new account" form (that hides background):
            WebElement btnClose;
            try {
                btnClose = driver.findElement(By.cssSelector("#kongregate_lightbox_wrapper > div.header_bar > a"));
            } catch (NoSuchElementException e) {
                return;
            }
            btnClose.click();

            // fill in username and password:
            WebElement weUsername;
            try {
                weUsername = driver.findElement(By.xpath("//*[@id='username']"));
            } catch (NoSuchElementException e) {
                return;
            }
            weUsername.clear();
            weUsername.sendKeys(BHBot.settings.username);

            WebElement wePassword;
            try {
                wePassword = driver.findElement(By.xpath("//*[@id='password']"));
            } catch (NoSuchElementException e) {
                return;
            }
            wePassword.clear();
            wePassword.sendKeys(BHBot.settings.password);

            // press the "sign-in" button:
            WebElement btnSignIn;
            try {
                btnSignIn = driver.findElement(By.id("sessions_new_form_spinner"));
            } catch (NoSuchElementException e) {
                return;
            }
            btnSignIn.click();

            BHBot.logger.info("Signed-in manually (sign-in prompt was open).");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Handles login screen (it shows seldom though. Perhaps because some cookie expired or something... anyway, we must handle it or else bot can't play the game anymore).
     */
    void detectLoginFormAndHandleIt(MarvinSegment seg) {
        lock.writeLock().lock();
        try {
            if (seg == null)
                return;

            // open login popup window:
            jsExecutor.executeScript("active_user.activateInlineLogin(); return false;"); // I found this code within page source itself (it gets triggered upon clicking on some button)

            Misc.sleep(5000); // if we don't sleep enough, login form may still be loading and code bellow will not get executed!

            // fill in username:
            WebElement weUsername;
            try {
                weUsername = driver.findElement(By.cssSelector("body#play > div#lightbox > div#lbContent > div#kongregate_lightbox_wrapper > div#lightbox_form > div#lightboxlogin > div#new_session_shared_form > form > dl > dd > input#username"));
            } catch (NoSuchElementException e) {
                BHBot.logger.warn("Problem: username field not found in the login form (perhaps it was not loaded yet?)!");
                return;
            }
            weUsername.clear();
            weUsername.sendKeys(BHBot.settings.username);
            BHBot.logger.info("Username entered into the login form.");

            WebElement wePassword;
            try {
                wePassword = driver.findElement(By.cssSelector("body#play > div#lightbox > div#lbContent > div#kongregate_lightbox_wrapper > div#lightbox_form > div#lightboxlogin > div#new_session_shared_form > form > dl > dd > input#password"));
            } catch (NoSuchElementException e) {
                BHBot.logger.warn("Problem: password field not found in the login form (perhaps it was not loaded yet?)!");
                return;
            }
            wePassword.clear();
            wePassword.sendKeys(BHBot.settings.password);
            BHBot.logger.info("Password entered into the login form.");

            // press the "sign-in" button:
            WebElement btnSignIn;
            try {
                btnSignIn = driver.findElement(By.cssSelector("body#play > div#lightbox > div#lbContent > div#kongregate_lightbox_wrapper > div#lightbox_form > div#lightboxlogin > div#new_session_shared_form > form > dl > dt#signin > input"));
            } catch (NoSuchElementException e) {
                return;
            }
            btnSignIn.click();

            BHBot.logger.info("Signed-in manually (we were signed-out).");

            scrollGameIntoView();
        } finally {
            lock.writeLock().unlock();
        }
    }

    BufferedImage takeScreenshot(boolean ofGame) {
        lock.readLock().lock();
        try {
            try {
                if (ofGame)
                    return Shutterbug.shootElement(driver, game).getImage();
                else
                    return Shutterbug.shootPage(driver).getImage();
            } catch (StaleElementReferenceException e) {
                // sometimes the game element is not available, if this happen we just return an empty image
                BHBot.logger.debug("Stale image detected while taking a screenshott", e);

                return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            } catch (TimeoutException e) {
                // sometimes Chrome/Chromium crashes and it is impossible to take screenshots from it
                BHBot.logger.warn("Selenium timeout detected while taking a screenshot. A monitor screenshot will be taken", e);

                if (BHBot.settings.hideWindowOnRestart) showBrowser();

                java.awt.Rectangle screenRect = new java.awt.Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                BufferedImage screen;
                try {
                    screen = new Robot().createScreenCapture(screenRect);
                } catch (AWTException ex) {
                    BHBot.logger.error("Impossible to perform a monitor screenshot", ex);
                    screen = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
                }

                if (BHBot.settings.hideWindowOnRestart) showBrowser();
                return screen;
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Moves mouse to position (0,0) in the 'game' element (so that it doesn't trigger any highlight popups or similar
     */
    void moveMouseAway() {
        lock.writeLock().lock();
        try {
            Actions act = new Actions(driver);
            act.moveToElement(game, 0, 0);
            act.perform();
        } catch (Exception e) {
            // do nothing
        } finally {
            lock.writeLock().unlock();
        }
    }

    //moves mouse to XY location (for triggering hover text)

    void moveMouseToPos(int x, int y) {
        lock.writeLock().lock();
        try {
            Actions act = new Actions(driver);
            act.moveToElement(game, x, y);
            act.perform();
        } catch (Exception e) {
            // do nothing
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Performs a mouse click on the center of the given segment
     */
    void clickOnSeg(MarvinSegment seg) {
        lock.writeLock().lock();
        try {
            Actions act = new Actions(driver);
            act.moveToElement(game, seg.getCenterX(), seg.getCenterY());
            act.click();
            act.moveToElement(game, 0, 0); // so that the mouse doesn't stay on the button, for example. Or else button will be highlighted and cue won't get detected!
            act.perform();
        } finally {
            lock.writeLock().unlock();
        }
    }

    void clickInGame(int x, int y) {
        lock.writeLock().lock();
        try {
            Actions act = new Actions(driver);
            act.moveToElement(game, x, y);
            act.click();
            act.moveToElement(game, 0, 0); // so that the mouse doesn't stay on the button, for example. Or else button will be highlighted and cue won't get detected!
            act.perform();
        } finally {
            lock.writeLock().unlock();
        }
    }

    void readScreen() {
        lock.writeLock().lock();
        try {
            readScreen(true);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param game if true, then screenshot of a WebElement will be taken that contains the flash game. If false, then simply a screenshot of a browser will be taken.
     */
    void readScreen(boolean game) {
        lock.writeLock().lock();
        try {
            readScreen(0, game);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * First sleeps 'wait' milliseconds and then reads the screen. It's a handy utility method that does two things in one command.
     */
    void readScreen(int wait) {
        lock.writeLock().lock();
        try {
            readScreen(wait, true);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param wait first sleeps 'wait' milliseconds and then reads the screen. It's a handy utility method that does two things in one command.
     * @param game if true, then screenshot of a WebElement will be taken that contains the flash game. If false, then simply a screenshot of a browser will be taken.
     */
    void readScreen(int wait, boolean game) {
        lock.writeLock().lock();
        try {
            if (wait != 0)
                Misc.sleep(wait);
            img = takeScreenshot(game);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * This method is meant to be used for development purpose. In some situations you want to "fake" the readScreen result
     * with an hand-crafted image. If this is the case, this method is here to help with it.
     *
     * @param screenFilePath the path to the image to be used to load the screen
     */
    @SuppressWarnings("unused")
    void loadScreen(String screenFilePath) {
        File screenImgFile = new File(screenFilePath);

        if (screenImgFile.exists()) {
            BufferedImage screenImg = null;
            try {
                screenImg = ImageIO.read(screenImgFile);
            } catch (IOException e) {
                BHBot.logger.error("Error when loading game screen ", e);
            }

            img = screenImg;
        } else {
            BHBot.logger.error("Impossible to load screen file: " + screenImgFile.getAbsolutePath());
        }
    }

    public BufferedImage getImg() {
        lock.readLock().lock();
        try {
            return img;
        } finally {
            lock.readLock().unlock();
        }
    }
}
