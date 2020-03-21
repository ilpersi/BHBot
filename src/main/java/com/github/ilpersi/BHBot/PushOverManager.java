package com.github.ilpersi.BHBot;

import net.pushover.client.*;

import java.io.File;

public class PushOverManager {
    private BHBot bot;
    private PushoverClient poClient = new PushoverRestClient();

    PushOverManager(BHBot bot) {
        this.bot = bot;
    }

    synchronized void sendPushOverMessage(String title, String msg, MessagePriority priority, File attachment) {
        sendPushOverMessage(title, msg, "pushover", priority, attachment);
    }

    synchronized void sendPushOverMessage(String title, String msg, @SuppressWarnings("SameParameterValue") String sound) {
        sendPushOverMessage(title, msg, sound, MessagePriority.NORMAL, null);
    }

    synchronized void sendPushOverMessage(String title, String msg, String sound, MessagePriority priority, File attachment) {
        if (bot.settings.enablePushover) {

            if (!"".equals(bot.settings.username) && !"yourusername".equals(bot.settings.username)) {
                title = "[" + bot.settings.username + "] " + title;
            }

            try {
                poClient.pushMessage(
                        PushoverMessage.builderWithApiToken(bot.settings.poAppToken)
                                .setUserId(bot.settings.poUserToken)
                                .setTitle(title)
                                .setMessage(msg)
                                .setPriority(priority)
                                .setSound(sound)
                                .setImage(attachment)
                                .build());
            } catch (PushoverException e) {
                BHBot.logger.error("Error while sending Pushover message", e);
            }
        }
    }
}
