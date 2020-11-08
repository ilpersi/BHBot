package com.github.ilpersi.BHBot;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

class AutoRuneManager {
    private enum MinorRuneEffect {
        CAPTURE("Capture"),
        EXPERIENCE("Experience"),
        GOLD("Gold"),
        ITEM_FIND("Item_Find");

        private final String name;

        MinorRuneEffect(String name) {
            this.name = name;
        }

        public static MinorRuneEffect getEffectFromName(String name) {
            for (MinorRuneEffect effect : MinorRuneEffect.values())
                if (effect.name.toLowerCase().equals(name.toLowerCase()))
                    return effect;
            return null;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @SuppressWarnings("unused")
    enum MinorRune {
        EXP_COMMON(MinorRuneEffect.EXPERIENCE, DungeonThread.ItemGrade.COMMON),
        EXP_RARE(MinorRuneEffect.EXPERIENCE, DungeonThread.ItemGrade.RARE),
        EXP_EPIC(MinorRuneEffect.EXPERIENCE, DungeonThread.ItemGrade.EPIC),
        EXP_LEGENDARY(MinorRuneEffect.EXPERIENCE, DungeonThread.ItemGrade.LEGENDARY),
        EXP_MYTHIC(MinorRuneEffect.EXPERIENCE, DungeonThread.ItemGrade.MYTHIC),

        ITEM_COMMON(MinorRuneEffect.ITEM_FIND, DungeonThread.ItemGrade.COMMON),
        ITEM_RARE(MinorRuneEffect.ITEM_FIND, DungeonThread.ItemGrade.RARE),
        ITEM_EPIC(MinorRuneEffect.ITEM_FIND, DungeonThread.ItemGrade.EPIC),
        ITEM_LEGENDARY(MinorRuneEffect.ITEM_FIND, DungeonThread.ItemGrade.LEGENDARY),
        ITEM_MYTHIC(MinorRuneEffect.ITEM_FIND, DungeonThread.ItemGrade.MYTHIC),

        GOLD_COMMON(MinorRuneEffect.GOLD, DungeonThread.ItemGrade.COMMON),
        //		GOLD_RARE(MinorRuneEffect.GOLD, ItemGrade.RARE),
//		GOLD_EPIC(MinorRuneEffect.GOLD, ItemGrade.EPIC),
        GOLD_LEGENDARY(MinorRuneEffect.GOLD, DungeonThread.ItemGrade.LEGENDARY),
        GOLD_MYTHIC(MinorRuneEffect.GOLD, DungeonThread.ItemGrade.MYTHIC),

        CAPTURE_COMMON(MinorRuneEffect.CAPTURE, DungeonThread.ItemGrade.COMMON),
        CAPTURE_RARE(MinorRuneEffect.CAPTURE, DungeonThread.ItemGrade.RARE),
        CAPTURE_EPIC(MinorRuneEffect.CAPTURE, DungeonThread.ItemGrade.EPIC),
        CAPTURE_LEGENDARY(MinorRuneEffect.CAPTURE, DungeonThread.ItemGrade.LEGENDARY),
        CAPTURE_MYTHIC(MinorRuneEffect.CAPTURE, DungeonThread.ItemGrade.MYTHIC);

        public static DungeonThread.ItemGrade maxGrade = DungeonThread.ItemGrade.MYTHIC;
        private final MinorRuneEffect effect;
        private final DungeonThread.ItemGrade grade;

        MinorRune(MinorRuneEffect effect, DungeonThread.ItemGrade grade) {
            this.effect = effect;
            this.grade = grade;
        }

        public static MinorRune getRune(MinorRuneEffect effect, DungeonThread.ItemGrade grade) {
            for (MinorRune rune : MinorRune.values()) {
                if (rune.effect == effect && rune.grade == grade)
                    return rune;
            }
            return null;
        }

        public MinorRuneEffect getRuneEffect() {
            return effect;
        }

        public String getRuneCueName() {
            return "MinorRune" + effect + grade;
        }

        public String getRuneCueFileName() {
            return "cues/runes/minor" + effect + grade + ".png";
        }

        public Cue getRuneCue() {
            return BHBot.cues.get(getRuneCueName());
        }


        public String getRuneSelectCueName() {
            return "MinorRune" + effect + grade + "Select";
        }

        public String getRuneSelectCueFileName() {
            return "cues/runes/minor" + effect + grade + "Select.png";
        }

        public Cue getRuneSelectCue() {
            return BHBot.cues.get(getRuneSelectCueName());
        }

        @Override
        public String toString() {
            return grade.toString().toLowerCase() + "_" + effect.toString().toLowerCase();
        }
    }

    private final BHBot bot;
    private boolean initialized;
    private MinorRune leftMinorRune;
    private MinorRune rightMinorRune;

    private boolean autoBossRuned = false;

    AutoRuneManager(BHBot bot, boolean skipInitialization) {
        this.bot = bot;

        if (skipInitialization) {
            this.initialized = true;
        }
    }

    void initialize() {
        // One time check for equipped minor runes
        if (!bot.settings.autoRuneDefault.isEmpty() && !initialized) {

            BHBot.logger.info("Startup check to determined configured minor runes");
            if (!detectEquippedMinorRunes(true, true)) {
                BHBot.logger.error("It was not possible to perform the equipped runes start-up check! Disabling autoRune..");
                bot.settings.autoRuneDefault.clear();
                bot.settings.autoRune.clear();
                bot.settings.autoBossRune.clear();

            }
            BHBot.logger.info(getRuneName(leftMinorRune.getRuneCueName()) + " equipped in left slot.");
            BHBot.logger.info(getRuneName(rightMinorRune.getRuneCueName()) + " equipped in right slot.");
            initialized = true;
            bot.browser.readScreen(2 * Misc.Durations.SECOND); // delay to close the settings window completely before we check for raid button else the settings window is hiding it
        }

    }

    boolean detectEquippedMinorRunes(boolean enterRunesMenu, boolean exitRunesMenu) {

        if (enterRunesMenu && openRunesMenu())
            return false;

        // determine equipped runes
        leftMinorRune = null;
        rightMinorRune = null;

        bot.browser.readScreen();

        for (MinorRune rune : MinorRune.values()) {
            Cue runeCue = rune.getRuneCue();

            // left rune
            MarvinSegment seg = MarvinSegment.fromCue(runeCue, 0, new Bounds(230, 245, 320, 330), bot.browser);
            if (seg != null)
                leftMinorRune = rune;

            // right rune
            seg = MarvinSegment.fromCue(runeCue, 0, new Bounds(480, 245, 565, 330), bot.browser);
            if (seg != null)
                rightMinorRune = rune;

        }

        if (leftMinorRune == null || rightMinorRune == null) {
            bot.saveGameScreen("wrong-rune-detection", "errors");
            Misc.contributeImage(bot.browser.getImg(), "rune-contribution", Bounds.fromWidthHeight(205, 150, 385, 270));
        }

        if (exitRunesMenu) {
            Misc.sleep(500);
            bot.browser.closePopupSecurely(BHBot.cues.get("RunesLayout"), BHBot.cues.get("X"));
            Misc.sleep(500);
            bot.browser.closePopupSecurely(BHBot.cues.get("StripSelectorButton"), BHBot.cues.get("X"));
        }

        boolean success = true;
        if (leftMinorRune == null) {
            BHBot.logger.warn("Error: Unable to detect left minor rune!");
            success = false;
        } else {
            BHBot.logger.debug(leftMinorRune + " equipped in left slot.");
        }
        if (rightMinorRune == null) {
            BHBot.logger.warn("Error: Unable to detect right minor rune!");
            success = false;
        } else {
            BHBot.logger.debug(rightMinorRune + " equipped in right slot.");
        }

        return success;
    }

    /**
     * Function to return the name of the runes for console output
     */
    @Contract(pure = true)
    private @Nullable String getRuneName(@NotNull String runeName) {

        switch (runeName) {
            case "MinorRuneExperienceCommon":
                return "Common Experience";
            case "MinorRuneExperienceRare":
                return "Rare Experience";
            case "MinorRuneExperienceEpic":
                return "Epic Experience";
            case "MinorRuneExperienceLegendary":
                return "Legendary Experience";
            case "MinorRuneItem_FindCommon":
                return "Common Item Find";
            case "MinorRuneItem_FindRare":
                return "Rare Item Find";
            case "MinorRuneItem_FindEpic":
                return "Epic Item Find";
            case "MinorRuneItem_FindLegendary":
                return "Legendary Item Find";
            case "MinorRuneGoldCommon":
                return "Common Gold";
            case "MinorRuneGoldRare":
                return "Rare Gold";
            case "MinorRuneGoldEpic":
                return "Epic Gold";
            case "MinorRuneGoldLegendary":
                return "Legendary Gold";
            case "MinorRuneCaptureCommon":
                return "Common Capture";
            case "MinorRuneCaptureRare":
                return "Rare Capture";
            case "MinorRuneCaptureEpic":
                return "Epic Capture";
            case "MinorRuneCaptureLegendary":
                return "Legendary Capture";
            case "MinorRuneItem_FindMythic":
                return "Mythic Item Find";
            case "MinorRuneGoldMythic":
                return "Mythic Gold";
            case "MinorRuneCaptureMythic":
                return "Mythic Capture";
            case "MinorRuneExperienceMythic":
                return "Mythic Experience";
            default:
                return null;
        }
    }

    private boolean openRunesMenu() {
        // Open character menu
        bot.browser.clickInGame(55, 465);

        MarvinSegment seg = MarvinSegment.fromCue(BHBot.cues.get("Runes"), 5 * Misc.Durations.SECOND, bot.browser);
        if (seg == null) {
            BHBot.logger.warn("Error: unable to detect runes button! Skipping...");
            return true;
        }

        // We make sure to click on the runes button
        bot.browser.closePopupSecurely(BHBot.cues.get("Runes"), BHBot.cues.get("Runes"));

        seg = MarvinSegment.fromCue(BHBot.cues.get("RunesLayout"), 5 * Misc.Durations.SECOND, bot.browser);
        if (seg == null) {
            BHBot.logger.warn("Error: unable to detect rune layout! Skipping...");
            bot.saveGameScreen("no-rune-layout", "errors", bot.browser.getImg());
            seg = MarvinSegment.fromCue(BHBot.cues.get("X"), 5 * Misc.Durations.SECOND, bot.browser);
            if (seg != null) {
                bot.browser.clickOnSeg(seg);
            }
            return true;
        }

        return false;
    }

    void processAutoRune(String activity) {
        List<String> desiredRunesAsStrs;
        String activityName = bot.getState().getNameFromShortcut(activity);
        if (bot.settings.autoRuneDefault.isEmpty()) {
            BHBot.logger.debug("autoRunesDefault not defined; aborting autoRunes");
            return;
        }

        if (!bot.settings.autoRune.containsKey(activity)) {
            BHBot.logger.debug("No autoRunes assigned for " + activityName + ", using defaults.");
            desiredRunesAsStrs = bot.settings.autoRuneDefault;
        } else {
            BHBot.logger.info("Configuring autoRunes for " + activityName);
            desiredRunesAsStrs = bot.settings.autoRune.get(activity);
        }

        List<MinorRuneEffect> desiredRunes = resolveDesiredRunes(desiredRunesAsStrs);
        if (noRunesNeedSwitching(desiredRunes)) {
            return;
        }

        // Back out of any raid/gauntlet/trial/GvG/etc pre-menu
        MarvinSegment seg = MarvinSegment.fromCue(BHBot.cues.get("X"), 2 * Misc.Durations.SECOND, bot.browser);
        if (seg != null) {
            bot.browser.clickOnSeg(seg);
            bot.browser.readScreen(Misc.Durations.SECOND);
        }

        if (!switchMinorRunes(desiredRunes))
            BHBot.logger.info("AutoRune failed!");

    }

    void handleMinorBossRunes() {
        if (bot.settings.autoRuneDefault.isEmpty()) {
            BHBot.logger.debug("autoRunesDefault not defined; aborting autoBossRunes");
            return;
        }

        String activity = bot.getState().getShortcut();
        if (!bot.settings.autoBossRune.containsKey(activity)) {
            BHBot.logger.info("No autoBossRunes assigned for " + bot.getState().getName() + ", skipping.");
            return;
        }

        List<String> desiredRunesAsStrs = bot.settings.autoBossRune.get(activity);
        List<MinorRuneEffect> desiredRunes = resolveDesiredRunes(desiredRunesAsStrs);
        if (noRunesNeedSwitching(desiredRunes))
            return;

        if (!switchMinorRunes(desiredRunes))
            BHBot.logger.autorune("AutoBossRune failed!");

    }

    void handleAutoBossRune(long outOfEncounterTimestamp, long inEncounterTimestamp) { //seperate function so we can run autoRune without autoShrine
        MarvinSegment guildButtonSeg;
        guildButtonSeg = MarvinSegment.fromCue(BHBot.cues.get("GuildButton"), bot.browser);

        if ((bot.getState() == BHBot.State.Raid && !bot.settings.autoShrine.contains("r") && bot.settings.autoBossRune.containsKey("r")) ||
                (bot.getState() == BHBot.State.Trials && !bot.settings.autoShrine.contains("t") && bot.settings.autoBossRune.containsKey("t")) ||
                (bot.getState() == BHBot.State.Expedition && !bot.settings.autoShrine.contains("e") && bot.settings.autoBossRune.containsKey("e")) ||
                (bot.getState() == BHBot.State.Dungeon && bot.settings.autoBossRune.containsKey("d")) ) {
            if (!autoBossRuned) {
                if ((((outOfEncounterTimestamp - inEncounterTimestamp) >= bot.settings.battleDelay) && guildButtonSeg != null)) {
                    BHBot.logger.autorune(bot.settings.battleDelay + "s since last encounter, changing runes for boss encounter");

                    handleMinorBossRunes();

                    if (!bot.dungeon.shrineManager.updateShrineSettings(false, false)) {
                        BHBot.logger.error("Impossible to disable Ignore Boss in handleAutoBossRune!");
                        BHBot.logger.warn("Resetting encounter timer to try again in 30 seconds.");
                        // inEncounterTimestamp = Misc.getTime() / 1000;
                        return;
                    }

                    // We disable and re-enable the auto feature
                    while (MarvinSegment.fromCue(BHBot.cues.get("AutoOn"), 500, bot.browser) != null) {
                        bot.browser.clickInGame(780, 270); //auto off
                        bot.browser.readScreen(500);
                    }
                    while (MarvinSegment.fromCue(BHBot.cues.get("AutoOff"), 500, bot.browser) != null) {
                        bot.browser.clickInGame(780, 270); //auto on again
                        bot.browser.readScreen(500);
                    }
                    autoBossRuned = true;
                }
            }
        }
    }

    private @NotNull List<MinorRuneEffect> resolveDesiredRunes(@NotNull List<String> desiredRunesAsStrs) {
        List<MinorRuneEffect> desiredRunes = new ArrayList<>();

        if (desiredRunesAsStrs.size() != 2) {
            BHBot.logger.error("Got malformed autoRunes, using defaults: " + String.join(" ", desiredRunesAsStrs));
            desiredRunesAsStrs = bot.settings.autoRuneDefault;
        }

        String strLeftRune = desiredRunesAsStrs.get(0);
        MinorRuneEffect desiredLeftRune = MinorRuneEffect.getEffectFromName(strLeftRune);
        if (desiredLeftRune == null) {
            BHBot.logger.error("No rune type found for left rune name " + strLeftRune);
            desiredLeftRune = leftMinorRune.getRuneEffect();
        }
        desiredRunes.add(desiredLeftRune);

        String strRightRune = desiredRunesAsStrs.get(1);
        MinorRuneEffect desiredRightRune = MinorRuneEffect.getEffectFromName(strRightRune);
        if (desiredRightRune == null) {
            BHBot.logger.error("No rune type found for right rune name " + strRightRune);
            desiredRightRune = rightMinorRune.getRuneEffect();
        }

        desiredRunes.add(desiredRightRune);

        return desiredRunes;
    }

    private boolean noRunesNeedSwitching(@NotNull List<MinorRuneEffect> desiredRunes) {
        MinorRuneEffect desiredLeftRune = desiredRunes.get(0);
        MinorRuneEffect desiredRightRune = desiredRunes.get(1);

        if (leftMinorRune == null) {
            BHBot.logger.warn("Left minor rune is unknown");
        }

        if (rightMinorRune == null) {
            BHBot.logger.warn("Right minor rune is unknown");
        }

        if ( (leftMinorRune != null &&  desiredLeftRune.equals(leftMinorRune.getRuneEffect()) )
                && (rightMinorRune != null && desiredRightRune.equals(rightMinorRune.getRuneEffect()) )) {
            BHBot.logger.debug("No runes found that need switching.");
            return true; // Nothing to do
        }

        if (leftMinorRune == null || !desiredLeftRune.equals(leftMinorRune.getRuneEffect())) {
            BHBot.logger.debug("Left minor rune needs to be switched.");
        }
        if (rightMinorRune == null || !desiredRightRune.equals(rightMinorRune.getRuneEffect())) {
            BHBot.logger.debug("Right minor rune needs to be switched.");
        }

        return false;

    }

    private Boolean switchMinorRunes(@NotNull List<MinorRuneEffect> desiredRunes) {
        MinorRuneEffect desiredLeftRune = desiredRunes.get(0);
        MinorRuneEffect desiredRightRune = desiredRunes.get(1);

        if (!detectEquippedMinorRunes(true, false)) {
            BHBot.logger.error("Unable to detect runes, pre-equip.");
            return false;
        }

        if (desiredLeftRune != leftMinorRune.getRuneEffect()) {
            BHBot.logger.debug("Switching left minor rune.");
            Misc.sleep(500); //sleep for window animation to finish
            bot.browser.clickInGame(280, 290); // Click on left rune
            if (!switchSingleMinorRune(desiredLeftRune)) {
                BHBot.logger.error("Failed to switch left minor rune.");
                return false;
            }
        }

        if (desiredRightRune != rightMinorRune.getRuneEffect()) {
            BHBot.logger.debug("Switching right minor rune.");
            Misc.sleep(500); //sleep for window animation to finish
            bot.browser.clickInGame(520, 290); // Click on right rune
            if (!switchSingleMinorRune(desiredRightRune)) {
                BHBot.logger.error("Failed to switch right minor rune.");
                return false;
            }
        }

        Misc.sleep(Misc.Durations.SECOND); //sleep while we wait for window animation

        if (!detectEquippedMinorRunes(false, true)) {
            BHBot.logger.error("Unable to detect runes, post-equip.");
            return false;
        }

        Misc.sleep(2 * Misc.Durations.SECOND);
        boolean success = true;
        if (desiredLeftRune != leftMinorRune.getRuneEffect()) {
            BHBot.logger.error("Left minor rune failed to switch for unknown reason.");
            success = false;
        }
        if (desiredRightRune != rightMinorRune.getRuneEffect()) {
            BHBot.logger.error("Right minor rune failed to switch for unknown reason.");
            success = false;
        }

        return success;
    }

    @NotNull
    private Boolean switchSingleMinorRune(MinorRuneEffect desiredRune) {
        bot.browser.readScreen(500); //sleep for window animation to finish

        MarvinSegment seg = MarvinSegment.fromCue(BHBot.cues.get("RunesSwitch"), 5 * Misc.Durations.SECOND, bot.browser);
        if (seg == null) {
            BHBot.logger.error("Failed to find rune switch button.");
            return false;
        }
        bot.browser.clickOnSeg(seg);

        bot.browser.readScreen(500); //sleep for window animation to finish

        seg = MarvinSegment.fromCue(BHBot.cues.get("RunesPicker"), 5 * Misc.Durations.SECOND, bot.browser);
        if (seg == null) {
            BHBot.logger.error("Failed to find rune picker.");
            return false;
        }

        DungeonThread.ItemGrade maxRuneGrade = MinorRune.maxGrade;
        for (int runeGradeVal = maxRuneGrade.getValue(); runeGradeVal > 0; runeGradeVal--) {
            DungeonThread.ItemGrade runeGrade = DungeonThread.ItemGrade.getGradeFromValue(runeGradeVal);

            if (runeGrade == null) {
                BHBot.logger.error(runeGradeVal + " produced null runeGrade in switchSingleMinorRune");
                return false;
            }

            MinorRune thisRune = MinorRune.getRune(desiredRune, runeGrade);

            if (thisRune == null) {
                BHBot.logger.debug(desiredRune.toString() + " " + runeGrade.toString() + "not defined in switchSingleMinorRune");
                continue;
            }

            Cue runeCue = thisRune.getRuneSelectCue();
            if (runeCue == null) {
                BHBot.logger.error("Unable to find cue for rune " + getRuneName(thisRune.getRuneCueName()));
                continue;
            }
            seg = MarvinSegment.fromCue(runeCue, bot.browser);
            if (seg == null) {
                BHBot.logger.debug("Unable to find " + getRuneName(thisRune.getRuneCueName()) + " in rune picker.");
                continue;
            }
            BHBot.logger.autorune("Switched to " + getRuneName(thisRune.getRuneCueName()));
            bot.browser.clickOnSeg(seg);
            Misc.sleep(Misc.Durations.SECOND);
            return true;
        }

        BHBot.logger.error("Unable to find rune of type " + desiredRune);
        bot.browser.closePopupSecurely(BHBot.cues.get("RunesPicker"), BHBot.cues.get("X"));
        Misc.sleep(Misc.Durations.SECOND);
        return false;
    }

    void reset() {
        autoBossRuned = false;
    }
}
