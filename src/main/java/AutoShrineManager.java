public class AutoShrineManager {
    private BHBot bot;

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

        //open settings
        int ignoreBossCnt = 0;
        int ignoreShrineCnt = 0;

        if (openSettings(Misc.Durations.SECOND)) {
            if (!initialized || ignoreBoss != this.ignoreBoss) {
                if (ignoreBoss) {
                    while (MarvinSegment.fromCue(BrowserManager.cues.get("IgnoreBoss"), Misc.Durations.SECOND, bot.browser) != null) {
                        BHBot.logger.debug("Enabling Ignore Boss");
                        bot.browser.clickInGame(194, 366);
                        bot.browser.readScreen(500);

                        if (ignoreBossCnt++ > 10) {
                            BHBot.logger.error("Impossible to enable Ignore Boss");
                            return false;
                        }
                    }
                    BHBot.logger.debug("Ignore Boss Enabled");
                } else {
                    while (MarvinSegment.fromCue(BrowserManager.cues.get("IgnoreBoss"), Misc.Durations.SECOND, bot.browser) == null) {
                        BHBot.logger.debug("Disabling Ignore Boss");
                        bot.browser.clickInGame(194, 366);
                        bot.browser.readScreen(500);

                        if (ignoreBossCnt++ > 10) {
                            BHBot.logger.error("Impossible to Disable Ignore Boss");
                            return false;
                        }
                    }
                    BHBot.logger.debug("Ignore Boss Disabled");
                }
                this.ignoreBoss = ignoreBoss;
            }

            if (!initialized || ignoreShrines != this.ignoreShrines) {
                if (ignoreShrines) {
                    while (MarvinSegment.fromCue(BrowserManager.cues.get("IgnoreShrines"), Misc.Durations.SECOND, bot.browser) != null) {
                        BHBot.logger.debug("Enabling Ignore Shrine");
                        bot.browser.clickInGame(194, 402);
                        bot.browser.readScreen(500);

                        if (ignoreShrineCnt++ > 10) {
                            BHBot.logger.error("Impossible to enable Ignore Shrines");
                            return false;
                        }
                    }
                    BHBot.logger.debug("Ignore Shrine Enabled");
                } else {
                    while (MarvinSegment.fromCue(BrowserManager.cues.get("IgnoreShrines"), Misc.Durations.SECOND, bot.browser) == null) {
                        BHBot.logger.debug("Disabling Ignore Shrine");
                        bot.browser.clickInGame(194, 402);
                        bot.browser.readScreen(500);

                        if (ignoreShrineCnt++ > 10) {
                            BHBot.logger.error("Impossible to disable Ignore Shrines");
                            return false;
                        }
                    }
                    BHBot.logger.debug("Ignore Shrine Disabled");
                }
                this.ignoreShrines = ignoreShrines;
            }

            bot.browser.readScreen(Misc.Durations.SECOND);

            bot.browser.closePopupSecurely(BrowserManager.cues.get("Settings"), new Cue(BrowserManager.cues.get("X"), new Bounds(608, 39, 711, 131)));

            return true;
        } else {
            BHBot.logger.warn("Impossible to open settings menu!");
            return false;
        }
    }

    private boolean openSettings(@SuppressWarnings("SameParameterValue") int delay) {
        bot.browser.readScreen();

        MarvinSegment seg = MarvinSegment.fromCue(BrowserManager.cues.get("SettingsGear"), bot.browser);
        if (seg != null) {
            bot.browser.clickOnSeg(seg);
            bot.browser.readScreen(delay);
            seg = MarvinSegment.fromCue(BrowserManager.cues.get("Settings"), Misc.Durations.SECOND * 3, bot.browser);
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
                (bot.getState() == BHBot.State.Expedition && bot.settings.autoShrine.contains("e")) ||
                (bot.getState() == BHBot.State.UnidentifiedDungeon)) {

            guildButtonSeg = MarvinSegment.fromCue(BrowserManager.cues.get("GuildButton"), bot.browser);

            if (battleDelay >= bot.settings.battleDelay && guildButtonSeg != null) {
                BHBot.logger.autoshrine(bot.settings.battleDelay + "s since last encounter, disabling ignore shrines");

                if (!updateShrineSettings(true, false)) {
                    BHBot.logger.error("Impossible to disable Ignore Shrines in handleAutoShrine!");
                    return;
                }

                //noinspection DuplicatedCode
                bot.browser.readScreen(100);

                // We disable and re-enable the auto feature
                while (MarvinSegment.fromCue(BrowserManager.cues.get("AutoOn"), 500, bot.browser) != null) {
                    bot.browser.clickInGame(780, 270); //auto off
                    bot.browser.readScreen(500);
                }
                while (MarvinSegment.fromCue(BrowserManager.cues.get("AutoOff"), 500, bot.browser) != null) {
                    bot.browser.clickInGame(780, 270); //auto on again
                    bot.browser.readScreen(500);
                }

                BHBot.logger.autoshrine("Waiting " + bot.settings.shrineDelay + "s to disable ignore boss");
                long timeToWait = Misc.getTime() + (battleDelay * Misc.Durations.SECOND);

                if ((bot.getState() == BHBot.State.Raid && bot.settings.autoBossRune.containsKey("r")) || (bot.getState() == BHBot.State.Trials && bot.settings.autoBossRune.containsKey("t")) ||
                        (bot.getState() == BHBot.State.Expedition && bot.settings.autoBossRune.containsKey("e")) || (bot.getState() == BHBot.State.Dungeon && bot.settings.autoBossRune.containsKey("d"))) {

                    // TODO de-spagettify the boss rune feature
                     bot.dungeon.handleMinorBossRunes();
                }

                while (Misc.getTime() < timeToWait) {
                    Misc.sleep(Misc.Durations.SECOND);
                }

                if (!updateShrineSettings(false, false)) {
                    BHBot.logger.error("Impossible to disable Ignore Boss in handleAutoShrine!");
                    return;
                }

                //noinspection DuplicatedCode
                bot.browser.readScreen(100);

                // We disable and re-enable the auto feature
                while (MarvinSegment.fromCue(BrowserManager.cues.get("AutoOn"), 500, bot.browser) != null) {
                    bot.browser.clickInGame(780, 270); //auto off
                    bot.browser.readScreen(500);
                }
                while (MarvinSegment.fromCue(BrowserManager.cues.get("AutoOff"), 500, bot.browser) != null) {
                    bot.browser.clickInGame(780, 270); //auto on again
                    bot.browser.readScreen(500);
                }

                bot.scheduler.resetIdleTime(true);
            }
        }
    }
}
