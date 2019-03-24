# BHBot

## What is BHBot?
BHBot is a program that automates different aspects of the [Bit Heroes](http://www.kongregate.com/games/juppiomenz/bit-heroes) game.
It is a non intrusive program that works by opening up a Chromium window and controls it as a normal user would. It works by taking a screenshot every few seconds, detecting cues from the screenshot and
then simulating mouse clicks. BHBot is a good example of how to create such bots to automate flash games or other browser games
(it can be easily adjusted to play some other game).

Once configured and running the bot is designed to run unattended indefinitely.

## Features
This bot will automatically farm items, familiars and schematics running the following:
* Dungeons (zones 1 to 9)
* World Bosses (Orlag, Nether or Melvin)
* Raids (1 to 7)
* Gauntlet / Trials
* PvP / GvG
* Expedition / Invasion

The level/difficulty for each activity can be defined in the settings file. The bot can also:

* Bribe specific familiars defined in the settings file (with limits so you dont waste gems)
* Revive fallen party members in Trials/Gauntlets/Raids
* Save shrines for use before the boss in Trials and Raids
* Use consumables to keep 25% IF/XP/Gold boosts running unattended
* Strip items for PvP to lower TS
* Solo certain zones for bounty requirements
* Collect completed bounties
* Collect fishing baits
* Claim all weekly rewards
* Screenshot and close PMs
* Open skeleton chests
* Handle incomplete teams
* Notify you on your mobile devices through [Pushover](https://github.com/Betalord/BHBot/wiki/Pushover-integration-Documentation) on how it is doing and if any issue is present

If the bot detects a "Disconnected" dialog, it will pause its execution for an hour giving the user a chance to play manually.
Disconnects are usually result of another instance logging into the game. This is why bot pauses in case it detects it.

## Download
You can download a release ready BHBot.jar from the [releases](https://github.com/Betalord/BHBot/releases) page.

For the latest functionality it is recommended to compile your own version as there are frequent commits with bug-fixes and new functionality between major releases.

## First time setup

See [the wiki](https://github.com/Betalord/BHBot/wiki) for the first time setup guide.

## Important

- While bot is running, do not interfere with the Chromium window that the bot has open via chromedriver. That means don't open menus and click around since that may confuse the bot and it could fail to do its tasks (which could lead to crashing it).
- If you want to continue using your computer while running the bot use, the 'hide' command to minimize the window. The bot clicks on certain buttons and cues and expects certain thing to pop up, and if they don't (due to user interaction), then it will fail to function properly. 
- If you are running the bot in Windows, you may want to run it under a separate account in order for it to not interfere with your work.

## Commands
Here is a list of most common commands used with the bot (must be typed in the console window, or, if you use web interface, in the
command input box):

- `stop`: stops the bot execution (may take a few seconds. Once it is stopped, the console will close automatically).
- `pause [mins]`: pauses bot's execution. Useful when you want to play the game yourself (either from the same Chromium window, or by starting another Chromium window). The bots is paused untill are resue command is issued or, if specified, for a number of minutes equal to _mins_
- `resume`: resumes bot's execution.
- `reload`: will reload the 'settings.ini' file from disk and apply any changes on-the-fly.
- `shot [prefix]`: takes a screenshot of the game and saves it to 'shot.png'. If a _prefix_ is specified it will be used instead of the default _shot_ one.
- `restart`: restarts the chromedriver (closes Chromium and opens a fresh Chromium window). Use only when something goes wrong (should restart automatically after some time in that case though).
- `hide`: hides Chromium window.
- `show`: shows Chromium window again after it has been hidden.
- `do`: force a dungeon/raid/pvp/gvg/gauntlet/trials. Example: "do raid". Used for debugging purposes more or less (bot will automatically attempt dungeons).
- `set`: sets a setting line, just like from a 'settings.ini' file. Example: "set raids 1 3 100", or "set difficulty 70". Note that this overwritten setting is NOT saved to the 'settings.ini' file! Once you issue <reload> command, it will get discharged.
- `readouts`: will reset readout timers (and hence immediately commence reading out resources).
- `pomessage [message]`: use this command to verify that the Pushover integration is correctly configured. Message parameter is optional and if not specified, a standard messabe will be sent.

## Finally

Hopefully this bot will prove useful to you, if you have any questions just raise an issue! Enjoy :-)

## Author
I, Betalord, am the original author of the bot. I have been using the bot until my character achieved level 165. On 29th
of September 2017 (a 1st year anniversary of the Bit Heroes game) I have quit the game and released the bot to the public.

If anyone is willing to continue maintaining and developing the bot contact me so that I can give you the repository rights.
