package com.github.ilpersi.BHBot;

public class AutoShrineManager {
    private final BHBot bot;

    // this variables are used to store the current status of the settings
    boolean ignoreBoss;
    boolean ignoreShrines;
    private boolean initialized;

    AutoShrineManager (BHBot bot, boolean skipInitialization) {
        this.bot = bot;

        if (skipInitialization) {
            this.initialized = true;
        }
    }

    void initialize() {
        if (!initialized) {
            BHBot.logger.info("Initializing autoShrine to make sure it is disabled");
            if (!updateShrineSettings(false, false)) {
                BHBot.logger.error("It was not possible to perform the autoShrine start-up check!");
            }
            initialized = true;
        }
    }

    boolean updateShrineSettings(boolean ignoreBoss, boolean ignoreShrines) {

        // We don't need to change settings as they are already as required
        if (initialized && ignoreBoss == this.ignoreBoss && ignoreShrines == this.ignoreShrines) {
            return true;
        }

        if (openSettings(Misc.Durations.SECOND)) {
            if (!initialized || ignoreBoss != this.ignoreBoss) {
                Bounds ignoreBossBounds = Bounds.fromWidthHeight(175, 339, 38, 35);
                MarvinSegment ignoreBossCheck = MarvinSegment.fromCue(BHBot.cues.get("IgnoreCheck"), 0, ignoreBossBounds, bot.browser);

                if (ignoreBoss && ignoreBossCheck == null) {
                    BHBot.logger.debug("Enabling Ignore Boss");
                    bot.browser.clickInGame(194, 366);
                } else if (!ignoreBoss && ignoreBossCheck != null) {
                    BHBot.logger.debug("Disabling Ignore Boss");
                    bot.browser.clickInGame(194, 366);
                    bot.browser.readScreen(1000);
                }
                this.ignoreBoss = ignoreBoss;
            }

            if (!initialized || ignoreShrines != this.ignoreShrines) {
                Bounds ignoreShrineBounds = Bounds.fromWidthHeight(174, 382, 37, 32);
                MarvinSegment ignoreShrineCheck = MarvinSegment.fromCue(BHBot.cues.get("IgnoreCheck"), 0, ignoreShrineBounds, bot.browser);

                if (ignoreShrines && ignoreShrineCheck == null) {
                    BHBot.logger.debug("Enabling Ignore Shrine");
                    bot.browser.clickInGame(194, 402);
                } else if (!ignoreShrines && ignoreShrineCheck != null) {
                    BHBot.logger.debug("Disabling Ignore Shrine");
                    bot.browser.clickInGame(194, 402);
                }
                this.ignoreShrines = ignoreShrines;
            }

            bot.browser.readScreen(Misc.Durations.SECOND);
            bot.browser.closePopupSecurely(BHBot.cues.get("Settings"), new Cue(BHBot.cues.get("X"), new Bounds(608, 39, 711, 131)));

            return true;
        } else {
            BHBot.logger.warn("Impossible to open settings menu!");
            return false;
        }
    }

    private void resetAutoButton() {
        // We disable and re-enable the auto feature
        MarvinSegment autoSeg = MarvinSegment.fromCue(BHBot.cues.get("AutoOn"), 500, bot.browser);

        if (autoSeg != null) {
            bot.browser.clickOnSeg(autoSeg);

            autoSeg = MarvinSegment.fromCue(BHBot.cues.get("AutoOff"), 5000, bot.browser);
            if (autoSeg != null) {
                bot.browser.clickOnSeg(autoSeg);
            } else {
                BHBot.logger.error("Impossible to find Auto Off button");
                bot.notificationManager.sendErrorNotification("Auto Shrine Error", "Impossible to find Auto Off button");
            }

        } else {
            BHBot.logger.error("Impossible to find Auto On button!");
            bot.notificationManager.sendErrorNotification("Auto Shrine Error", "Impossible to find Auto On button");
        }
    }

    private boolean openSettings(@SuppressWarnings("SameParameterValue") int delay) {
        bot.browser.readScreen();

        MarvinSegment seg = MarvinSegment.fromCue(BHBot.cues.get("SettingsGear"), bot.browser);
        if (seg != null) {
            bot.browser.clickOnSeg(seg);
            bot.browser.readScreen(delay);
            seg = MarvinSegment.fromCue(BHBot.cues.get("Settings"), Misc.Durations.SECOND * 3, bot.browser);
            return seg != null;
        } else {
            BHBot.logger.error("Impossible to find the settings button!");
            bot.saveGameScreen("open-settings-no-btn", "errors");
            return false;
        }
    }

    void processAutoShrine(long battleDelay) {
        MarvinSegment guildButtonSeg;

        /* All the flags are already disabled, this means that the current dungeon has already
        *  used the autoShrine feature */
        if (!ignoreBoss && !ignoreShrines) {
            return;
        }

        if ((bot.getState() == BHBot.State.Raid && bot.settings.autoShrine.contains("r")) ||
                (bot.getState() == BHBot.State.Trials && bot.settings.autoShrine.contains("t")) ||
                (bot.getState() == BHBot.State.Expedition && bot.settings.autoShrine.contains("e"))) {

            guildButtonSeg = MarvinSegment.fromCue(BHBot.cues.get("GuildButton"), bot.browser);

            if (battleDelay >= bot.settings.battleDelay && guildButtonSeg != null) {
                BHBot.logger.autoshrine(bot.settings.battleDelay + "s since last encounter, disabling ignore shrines");

                if (!updateShrineSettings(true, false)) {
                    BHBot.logger.error("Impossible to disable Ignore Shrines in handleAutoShrine!");
                    return;
                }

                //noinspection DuplicatedCode
                bot.browser.readScreen(100);

                resetAutoButton();

                BHBot.logger.autoshrine("Waiting " + bot.settings.shrineDelay + "s to disable ignore boss");
                long timeToWait = Misc.getTime() + (battleDelay * Misc.Durations.SECOND);

                if ((bot.getState() == BHBot.State.Raid && bot.settings.autoBossRune.containsKey("r")) || (bot.getState() == BHBot.State.Trials && bot.settings.autoBossRune.containsKey("t")) ||
                        (bot.getState() == BHBot.State.Expedition && bot.settings.autoBossRune.containsKey("e")) || (bot.getState() == BHBot.State.Dungeon && bot.settings.autoBossRune.containsKey("d"))) {

                    // TODO de-spagettify the boss rune feature
                     bot.dungeon.runeManager.handleMinorBossRunes();
                }

                while (Misc.getTime() < timeToWait) {
                    Misc.sleep(Misc.Durations.SECOND);
                }

                if (!updateShrineSettings(false, false)) {
                    BHBot.logger.error("Impossible to disable Ignore Boss in handleAutoShrine!");
                    return;
                }

                //noinspection DuplicatedCode,DuplicatedCode
                bot.browser.readScreen(100);

                resetAutoButton();

                bot.scheduler.resetIdleTime(true);
            }
        }
    }
}
