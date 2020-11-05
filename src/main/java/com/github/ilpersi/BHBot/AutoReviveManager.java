package com.github.ilpersi.BHBot;

import org.openqa.selenium.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AutoReviveManager {
    private final BHBot bot;

    enum PotionType {
        MINOR("Minor", '1'),
        AVERAGE("Average", '2'),
        MAJOR("Major", '3'),
        SUPER("Super", '4'),
        UNKNOWN("Unknown", '5');

        private final String name;
        private final char level;

        PotionType(String name, char level) {
            this.name = name;
            this.level = level;
        }

        char getChar() {return level;}

        public String toString() {
            return this.name;
        }

        static PotionType fromChar(char potionType) {
            for (PotionType pt : PotionType.values()) {
                if (pt.getChar() == potionType) return pt;
            }
            return PotionType.UNKNOWN;
        }

    }

    // Counters for total used potions
    // The key is composed by the potion char ('1', '2', '3', '4') concatenated to the possible state (r, t, g.....)
    private final HashMap<String, Integer> totalUsed = new HashMap<>();

    // Dungeon vars
    // Counter for dungeon used potions
    private int usedPotions = 0;
    // Revived party members
    private final boolean[] revived = {false, false, false, false, false};

    AutoReviveManager (BHBot bot) {
        this.bot = bot;

        for (PotionType pt : PotionType.values()) {
            for (BHBot.State state : BHBot.State.values()) {
                String key = pt.toString() + "-" + state.toString();
                totalUsed.put(key, 0);
            }
        }
    }

    void processAutoRevive() {

        MarvinSegment seg;

        // we make sure that we stick with the limits
        if (usedPotions >= bot.settings.potionLimit) {
            BHBot.logger.autorevive("Potion limit reached, skipping revive check");
            bot.scheduler.resetIdleTime(true);
            return;
        }

        seg = MarvinSegment.fromCue(BHBot.cues.get("Potions"), Misc.Durations.SECOND, bot.browser);
        if (seg != null) {
            bot.browser.clickOnSeg(seg);
            bot.browser.readScreen(Misc.Durations.SECOND);

            // If no potions are needed, we re-enable the Auto function
            seg = MarvinSegment.fromCue(BHBot.cues.get("NoPotions"), Misc.Durations.SECOND, bot.browser); // Everyone is Full HP
            if (seg != null) {
                seg = MarvinSegment.fromCue("Close", Misc.Durations.SECOND, new Bounds(300, 330, 500, 400), bot.browser);
                if (seg != null) {
                    BHBot.logger.autorevive("None of the team members need a consumable, exiting from autoRevive");
                    bot.browser.clickOnSeg(seg);
                } else {
                    BHBot.logger.error("No potions cue detected, without close button, restarting!");
                    bot.saveGameScreen("autorevive-no-potions-no-close", bot.browser.getImg());
                    bot.restart(true, false);
                }
                bot.scheduler.resetIdleTime(true);
                return;
            }

            // Based on the state we get the team size
            HashMap<Integer, Point> revivePositions = new HashMap<>();
            switch (bot.getState()) {
                case Trials:
                case Gauntlet:
                case Expedition:
                    revivePositions.put(1, new Point(290, 315));
                    revivePositions.put(2, new Point(200, 340));
                    revivePositions.put(3, new Point(115, 285));
                    break;
                case Raid:
                    revivePositions.put(1, new Point(305, 320));
                    revivePositions.put(2, new Point(250, 345));
                    revivePositions.put(3, new Point(200, 267));
                    revivePositions.put(4, new Point(150, 325));
                    revivePositions.put(5, new Point(90, 295));
                    break;
                default:
                    break;
            }

            if ((bot.getState() == BHBot.State.Trials && bot.settings.autoRevive.contains("t")) ||
                    (bot.getState() == BHBot.State.Gauntlet && bot.settings.autoRevive.contains("g")) ||
                    (bot.getState() == BHBot.State.Raid && bot.settings.autoRevive.contains("r")) ||
                    (bot.getState() == BHBot.State.Expedition && bot.settings.autoRevive.contains("e"))) {

                // from char to potion name
//                HashMap<Character, String> potionTranslate = new HashMap<>();
//                potionTranslate.put('1', "Minor");
//                potionTranslate.put('2', "Average");
//                potionTranslate.put('3', "Major");

                //for loop for each entry in revivePositions
                for (Map.Entry<Integer, Point> item : revivePositions.entrySet()) {
                    Integer slotNum = item.getKey();
                    Point slotPos = item.getValue();

                    //if we have reached potionLimit we exit autoRevive
                    if (usedPotions == bot.settings.potionLimit) {
                        BHBot.logger.autorevive("Potion limit reached, exiting from Auto Revive");
                        bot.browser.readScreen(Misc.Durations.SECOND);
                        break;
                    }

                    //if position has been revived don't check it again
                    if (revived[slotNum - 1]) continue;

                    //check if there is a gravestone to see if we need to revive
                    //we MouseOver to make sure the grave is in the foreground and not covered
                    bot.browser.moveMouseToPos(slotPos.x, slotPos.y);
                    if (MarvinSegment.fromCue(BHBot.cues.get("GravestoneHighlighted"), 3 * Misc.Durations.SECOND, bot.browser) == null)
                        continue;

                    // If we revive a team member we need to reopen the potion menu again
                    seg = MarvinSegment.fromCue(BHBot.cues.get("UnitSelect"), Misc.Durations.SECOND, bot.browser);
                    if (seg == null) {
                        seg = MarvinSegment.fromCue(BHBot.cues.get("Potions"), Misc.Durations.SECOND * 2, bot.browser);
                        if (seg != null) {
                            bot.browser.clickOnSeg(seg);
                            bot.browser.readScreen(Misc.Durations.SECOND);

                            // If no potions are needed, we re-enable the Auto function
                            seg = MarvinSegment.fromCue(BHBot.cues.get("NoPotions"), Misc.Durations.SECOND, bot.browser); // Everyone is Full HP
                            if (seg != null) {
                                seg = MarvinSegment.fromCue(BHBot.cues.get("Close"), Misc.Durations.SECOND, new Bounds(300, 330, 500, 400), bot.browser);
                                if (seg != null) {
                                    BHBot.logger.autorevive("None of the team members need a consumable, exiting from autoRevive");
                                    bot.browser.clickOnSeg(seg);
                                } else {
                                    BHBot.logger.error("Error while reopening the potions menu: no close button found!");
                                    bot.saveGameScreen("autorevive-no-potions-for-error", bot.browser.getImg());
                                    bot.restart(true, false);
                                }
                                return;
                            }
                        }
                    }

                    bot.browser.readScreen(Misc.Durations.SECOND);
                    bot.browser.clickInGame(slotPos.x, slotPos.y);
                    bot.browser.readScreen(Misc.Durations.SECOND);

                    MarvinSegment superHealSeg = MarvinSegment.fromCue(BHBot.cues.get("SuperAvailable"), bot.browser);

                    if (superHealSeg != null) {
                        // If super potion is available, we skip it
                        int superPotionMaxChecks = 10, superPotionCurrentCheck = 0;
                        while (superPotionCurrentCheck < superPotionMaxChecks && MarvinSegment.fromCue(BHBot.cues.get("SuperAvailable"), bot.browser) != null) {
                            bot.browser.clickInGame(656, 434);
                            bot.browser.readScreen(500);
                            superPotionCurrentCheck++;
                        }
                    }

                    // We check what revives are available, and we save the seg for future reuse
                    HashMap<PotionType, MarvinSegment> availablePotions = new HashMap<>();
                    availablePotions.put(PotionType.MINOR, MarvinSegment.fromCue(BHBot.cues.get("MinorAvailable"), bot.browser));
                    availablePotions.put(PotionType.AVERAGE, MarvinSegment.fromCue(BHBot.cues.get("AverageAvailable"), bot.browser));
                    availablePotions.put(PotionType.MAJOR, MarvinSegment.fromCue(BHBot.cues.get("MajorAvailable"), bot.browser));

                    // No more potions are available
                    if (availablePotions.get(PotionType.MINOR) == null && availablePotions.get(PotionType.AVERAGE) == null && availablePotions.get(PotionType.MAJOR) == null) {
                        BHBot.logger.warn("No potions are avilable, autoRevive well be temporary disabled!");
                        bot.settings.autoRevive = new ArrayList<>();
                        bot.scheduler.resetIdleTime(true);

                        bot.notificationManager.sendErrorNotification("AutoRevive Error", "No more potions are available, please restock them!");
                        return;
                    }

                    // We manage tank priority using the best potion we have
                    if (slotNum == (bot.settings.tankPosition) &&
                            ((bot.getState() == BHBot.State.Trials && bot.settings.tankPriority.contains("t")) ||
                                    (bot.getState() == BHBot.State.Gauntlet && bot.settings.tankPriority.contains("g")) ||
                                    (bot.getState() == BHBot.State.Raid && bot.settings.tankPriority.contains("r")) ||
                                    (bot.getState() == BHBot.State.Expedition && bot.settings.tankPriority.contains("e")))) {
                        for (char potion : "321".toCharArray()) {
                            PotionType pt = PotionType.fromChar(potion);
                            seg = availablePotions.get(pt);
                            if (seg != null) {
                                BHBot.logger.autorevive("Handling tank priority (position: " + bot.settings.tankPosition + ") with " + pt.toString() + " revive.");
                                bot.browser.clickOnSeg(seg);
                                bot.browser.readScreen(Misc.Durations.SECOND);
                                seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), Misc.Durations.SECOND, new Bounds(230, 320, 550, 410), bot.browser);
                                bot.browser.clickOnSeg(seg);
                                revived[bot.settings.tankPosition - 1] = true;
                                increaseUsedPotion(pt, bot.getState());
                                usedPotions++;
                                bot.browser.readScreen(Misc.Durations.SECOND);
                                bot.scheduler.resetIdleTime(true);
                                break;
                            }
                        }
                    }

                    if (!revived[slotNum - 1]) { // This is only false when tank priory kicks in
                        for (char potion : bot.settings.potionOrder.toCharArray()) {
                            // BHBot.logger.info("Checking potion " + potion);
                            PotionType pt = PotionType.fromChar(potion);
                            seg = availablePotions.get(pt);
                            if (seg != null) {
                                BHBot.logger.autorevive("Using " + pt.toString() + " revive on slot " + slotNum + ".");
                                bot.browser.clickOnSeg(seg);
                                bot.browser.readScreen(Misc.Durations.SECOND);
                                seg = MarvinSegment.fromCue(BHBot.cues.get("YesGreen"), Misc.Durations.SECOND, new Bounds(230, 320, 550, 410), bot.browser);
                                bot.browser.clickOnSeg(seg);
                                revived[slotNum - 1] = true;
                                increaseUsedPotion(pt, bot.getState());
                                usedPotions++;
                                bot.browser.readScreen(Misc.Durations.SECOND);
                                bot.scheduler.resetIdleTime(true);
                                break;
                            }
                        }
                    }
                }
            }
        } else { // Impossible to find the potions button
            bot.saveGameScreen("auto-revive-no-potions", "errors", bot.browser.getImg());
            BHBot.logger.autorevive("Impossible to find the potions button!");
        }

        // If the unit selection screen is still open, we need to close it
        seg = MarvinSegment.fromCue(BHBot.cues.get("UnitSelect"), Misc.Durations.SECOND, bot.browser);
        if (seg != null) {
            seg = MarvinSegment.fromCue(BHBot.cues.get("X"), Misc.Durations.SECOND, bot.browser);
            if (seg != null) {
                bot.browser.clickOnSeg(seg);
                bot.browser.readScreen(Misc.Durations.SECOND);
            }
        }
    }

    void reset () {
        usedPotions = 0;

        revived[0] = false;
        revived[1] = false;
        revived[2] = false;
        revived[3] = false;
        revived[4] = false;
    }

    private void increaseUsedPotion(PotionType pt, BHBot.State state) {
        String key = pt.toString() + "-" + state.toString();
        totalUsed.put(key, totalUsed.get(key) +1);
    }

    Integer getCounter(PotionType pt, BHBot.State state) {
        String key = pt.toString() + "-" + state.toString();
        return totalUsed.get(key);
    }

}
