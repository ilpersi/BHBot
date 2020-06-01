package com.github.ilpersi.BHBot;

import net.pushover.client.MessagePriority;


import java.awt.image.BufferedImage;
import java.io.File;

public class NotificationManager {

    private final BHBot bot;
    private final PushOverManager poManager;
    private final DiscordManager discordManager;

    private long timeLastPOAlive; // when did we check for fishing last time?
    private long timeLastDiscordAlive; // when did we check for fishing last time?

    // private DiscordManager discordManager = new DiscordManager();

    NotificationManager(BHBot bot) {
        this.bot = bot;
        this.poManager =  new PushOverManager(bot);
        this.discordManager = new DiscordManager(bot);

        this.timeLastPOAlive = 0;
        this.timeLastDiscordAlive = 0;
    }

    void notifyStartUp() {

        // We check if Pushover or Discord require a startup notification
        if ((bot.settings.enablePushover && bot.settings.poNotifyAlive > 0 && timeLastPOAlive == 0) ||
                (bot.settings.enableDiscord && bot.settings.discordNotifyAlive > 0 && timeLastDiscordAlive == 0)) {

            String aliveScreenName = bot.saveGameScreen("alive-screen");
            File aliveScreenFile = aliveScreenName != null ? new File(aliveScreenName) : null;
            String startUpMsg = "BHBot has been successfully started!";

            // we need to send pushover notifications
            if (bot.settings.enablePushover && bot.settings.poNotifyAlive > 0 && timeLastPOAlive == 0) {
                // startup notification
                timeLastPOAlive = Misc.getTime();
                poManager.sendPushOverMessage("Startup notification", startUpMsg, MessagePriority.QUIET, aliveScreenFile);
            }

            if (bot.settings.enableDiscord && bot.settings.discordNotifyAlive > 0 && timeLastDiscordAlive == 0) {
                timeLastDiscordAlive = Misc.getTime();
                discordManager.sendDiscordMessage(startUpMsg, aliveScreenFile);
            }

            if (aliveScreenFile != null && !aliveScreenFile.delete())
                BHBot.logger.warn("Impossible to delete tmp img for startup notification.");
        }
    }

    void notifyAlive() {

        // periodic notification
        if (((Misc.getTime() - timeLastPOAlive) > (bot.settings.poNotifyAlive * Misc.Durations.HOUR)) && timeLastPOAlive != 0 ||
                ((Misc.getTime() - timeLastDiscordAlive) > (bot.settings.discordNotifyAlive * Misc.Durations.HOUR)) && timeLastDiscordAlive != 0 ) {

            String aliveScreenName = bot.saveGameScreen("alive-screen");
            File aliveScreenFile = aliveScreenName != null ? new File(aliveScreenName) : null;

            StringBuilder aliveMsg = new StringBuilder();
            aliveMsg.append("I am alive and doing fine since ")
                    .append(Misc.millisToHumanForm(Misc.getTime() - bot.botStartTime))
                    .append("!\n\n");

            for (BHBot.State state : BHBot.State.values()) {
                if (bot.dungeon.counters.get(state).getTotal() > 0) {
                    aliveMsg.append(state.getName()).append(" ")
                            .append(bot.dungeon.counters.get(state).successRateDesc())
                            .append("\n");
                }
            }

            // We notify the used potions
            StringBuilder usedPotionsMsg = new StringBuilder();
            for (AutoReviveManager.PotionType pt : AutoReviveManager.PotionType.values()) {
                // For each potion type we have a message
                StringBuilder potionTypeMsg  = new StringBuilder();
                potionTypeMsg.append(pt.toString())
                        .append(" revive potion used: ");

                // We save the initial len
                int ptMsgInitLen = potionTypeMsg.length();

                // We loop on the used potions in dungeons
                for (BHBot.State state : BHBot.State.values()) {
                    Integer tmpUsedPotion = bot.dungeon.reviveManager.getCounter(pt, state);
                    if (tmpUsedPotion > 0) {
                        if (potionTypeMsg.length() > ptMsgInitLen) potionTypeMsg.append(" ");
                        potionTypeMsg.append(state.getShortcut())
                                .append(":")
                                .append(tmpUsedPotion);
                    }
                }

                // If a type has been ued in one or more dungeon we append it to the dedicated StringBuilder
                if (potionTypeMsg.length() > ptMsgInitLen) {
                    usedPotionsMsg.append(potionTypeMsg)
                            .append("\n");
                }
            }
            if (usedPotionsMsg.length() > 0) {
                aliveMsg.append("\n")
                        .append(usedPotionsMsg);
            }

            // Notify level for T/G
            if (bot.dungeon.counters.get(BHBot.State.Trials).getTotal() > 0 ||
                    bot.dungeon.counters.get(BHBot.State.Gauntlet).getTotal() > 0) {

                aliveMsg.append("\n");
                // We append trial level if we did at least one trial
                if (bot.dungeon.counters.get(BHBot.State.Trials).getTotal() > 0) {
                    aliveMsg.append("Trial Level: ")
                        .append(bot.settings.difficultyTrials)
                        .append("\n");
                }

                // We append gauntlet level if we did at least one trial
                if (bot.dungeon.counters.get(BHBot.State.Gauntlet).getTotal() > 0) {
                    aliveMsg.append("Gauntlet Level: ")
                            .append(bot.settings.difficultyGauntlet)
                            .append("\n");
                }
            }
            aliveMsg.append("\n");

            if ((Misc.getTime() - timeLastPOAlive) > (bot.settings.poNotifyAlive * Misc.Durations.HOUR) && timeLastPOAlive != 0) {
                timeLastPOAlive = Misc.getTime();
                poManager.sendPushOverMessage("Alive notification", aliveMsg.toString(), MessagePriority.QUIET, aliveScreenFile);
            }

            if ((Misc.getTime() - timeLastDiscordAlive) > (bot.settings.discordNotifyAlive * Misc.Durations.HOUR) && timeLastDiscordAlive != 0) {
                timeLastDiscordAlive = Misc.getTime();
                discordManager.sendDiscordMessage(aliveMsg.toString(), aliveScreenFile);
            }

            if (aliveScreenFile != null && !aliveScreenFile.delete())
                BHBot.logger.warn("Impossible to delete tmp img for alive notification.");
        }

    }

    void notifyCrash(String crashMsg, String crashPrintScreenPath) {

        if ((bot.settings.enablePushover && bot.settings.poNotifyCrash) || (bot.settings.enableDiscord && bot.settings.discordNotifyCrash)) {

            File crashPrintScreen = new File(crashPrintScreenPath);
            boolean crashScreenExists = crashPrintScreen.exists();

            if (bot.settings.enablePushover && bot.settings.poNotifyCrash) {
                poManager.sendPushOverMessage("BHBot CRASH!", crashMsg, "falling", MessagePriority.HIGH,
                        crashScreenExists ? crashPrintScreen : null);
            }

            if (bot.settings.enableDiscord && bot.settings.discordNotifyCrash) {
                discordManager.sendDiscordMessage(crashMsg, crashScreenExists ? crashPrintScreen : null);
            }
        }

    }

    void notifyError(String errorTitle, String errorMsg) {
        if ((bot.settings.enablePushover && bot.settings.poNotifyErrors) ||
                (bot.settings.enableDiscord && bot.settings.discordNotifyErrors)) {

            String errorPrintScreenPath = bot.saveGameScreen("error-screen", bot.browser.getImg());
            File errorPrintScreen = errorPrintScreenPath != null ? new File(errorPrintScreenPath) : null;

            if (bot.settings.enablePushover && bot.settings.poNotifyErrors) {
                poManager.sendPushOverMessage(errorTitle, errorMsg, "siren", MessagePriority.NORMAL, errorPrintScreen);
            }

            if (bot.settings.enableDiscord && bot.settings.discordNotifyErrors) {
                discordManager.sendDiscordMessage(errorMsg, errorPrintScreen);
            }

            if (errorPrintScreen != null && !errorPrintScreen.delete()) {
                BHBot.logger.error("Impossible to delete error notification img.");
            }
        }
    }

    void notifyPM(String pmScreenPath) {

        if ((bot.settings.enablePushover && bot.settings.poNotifyPM) ||
                (bot.settings.enableDiscord && bot.settings.discordNotifyPM)) {

            String pmTitle = "New PM";
            String pmMessage = "You've just received a new PM, check it out!";
            File pmScreen = pmScreenPath != null ? new File(pmScreenPath) : null;

            if (bot.settings.enablePushover && bot.settings.poNotifyPM) {
                poManager.sendPushOverMessage(pmTitle, pmMessage, MessagePriority.NORMAL, pmScreen);
            }

            if (bot.settings.enableDiscord && bot.settings.discordNotifyPM) {
                discordManager.sendDiscordMessage(pmMessage, pmScreen);
            }

        }

    }

    void notifyBribe(BufferedImage bribeImg) {
        if ((bot.settings.enablePushover && bot.settings.poNotifyBribe) ||
                (bot.settings.enableDiscord && bot.settings.discordNotifyBribe)) {

            String bribeTitle = "Creature Bribe";
            String bribeMessage = "A familiar has been bribed!";
            String bribeScreenPath = bot.saveGameScreen("bribe-screen", bribeImg);
            File bribeScreen = bribeScreenPath != null ? new File(bribeScreenPath) : null;

            if (bot.settings.enablePushover && bot.settings.poNotifyBribe) {
                poManager.sendPushOverMessage(bribeTitle, bribeMessage, "bugle", MessagePriority.NORMAL, bribeScreen);
            }

            if (bot.settings.enableDiscord && bot.settings.discordNotifyBribe) {
                discordManager.sendDiscordMessage(bribeMessage, bribeScreen);
            }

            if (bribeScreen != null && !bribeScreen.delete())
                BHBot.logger.warn("Impossible to delete tmp img file for bribe notification.");

        }
    }

    void notifyDrop(String dropTitle, String dropMsg, BufferedImage dropScreenImg) {
        String victoryScreenName = bot.saveGameScreen("victory-screen", dropScreenImg);
        File victoryScreenFile = victoryScreenName != null ? new File(victoryScreenName) : null;

        poManager.sendPushOverMessage(dropTitle, dropMsg, "magic", MessagePriority.NORMAL, victoryScreenFile);
        discordManager.sendDiscordMessage(dropMsg, victoryScreenFile);

        if (victoryScreenFile != null && !victoryScreenFile.delete())
            BHBot.logger.warn("Impossible to delete tmp img file for victory drop.");
    }

    void testNotification(String testNotificationMsg) {
        if (bot.settings.enablePushover || bot.settings.enableDiscord) {
            String testScreenName = bot.saveGameScreen("test-notification");
            File testScreenFile = testScreenName != null ? new File(testScreenName) : null;

            if (bot.settings.enablePushover) {
                String poLogMessage = "Sending Pushover test message.";
                poLogMessage += "\n\n poUserToken is: " + bot.settings.poUserToken;
                poLogMessage += "\n poAppToken is: " + bot.settings.poAppToken;
                BHBot.logger.info(poLogMessage);

                poManager.sendPushOverMessage("Test Notification", testNotificationMsg, MessagePriority.NORMAL, testScreenFile);
            } else {
                BHBot.logger.warn("Pushover integration is disabled in the settings!");
            }

            if (bot.settings.enableDiscord) {
                String discordLogMessage = "Sending Discord test message";
                discordLogMessage += "\n\n discordWebHookUrl is " + bot.settings.discordWebHookUrl;
                BHBot.logger.info(discordLogMessage);

                discordManager.sendDiscordMessage(testNotificationMsg, testScreenFile);
            } else {
                BHBot.logger.warn("Discord integration is disabled in the settings!");
            }

            if (testScreenFile != null && !testScreenFile.delete())
                BHBot.logger.warn("Impossible to delete tmp img for pomessage command.");
        }
    }

}
