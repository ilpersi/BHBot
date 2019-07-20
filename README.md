# BHBot

[![Gitter chat](https://img.shields.io/gitter/room/ilpersi/BHBot.svg?color=red&style=popout)](https://gitter.im/BHBot/community)
[![GitHub release](https://img.shields.io/github/release/ilpersi/BHBot.svg?label=last%20release&style=popout) ![GitHub Release Date](https://img.shields.io/github/release-date/ilpersi/BHBot.svg?label=released&style=popout)](https://github.com/ilpersi/BHBot/releases/latest)
[![GitHub issues by-label](https://img.shields.io/github/issues/ilpersi/BHBot/bug.svg?label=bug%28s%29&style=popout)](https://github.com/ilpersi/BHBot/labels/bug)
[![GitHub last commit](https://img.shields.io/github/last-commit/ilpersi/BHBot.svg?style=popout)](https://github.com/ilpersi/BHBot/commits)
![GitHub All Releases](https://img.shields.io/github/downloads/ilpersi/BHBot/total.svg?label=total%20downloads&style=popout)
![GitHub top language](https://img.shields.io/github/languages/top/ilpersi/BHBot.svg?style=popout)

## What is BHBot?
BHBot is a program that automates the [Bit Heroes](http://www.kongregate.com/games/juppiomenz/bit-heroes) game.
It is a non intrusive program that works by opening up a Chromium window and controls the game by taking a screenshot every few seconds, detecting cues from the screenshot and
then simulating mouse clicks. BHBot is a good example of how to create such bots to automate flash games or other browser games
(it can be easily adjusted to play other games).

Once configured and running the bot is designed to run unattended indefinitely.

## Features
This bot will automatically farm items, familiars and schematics running all current content:
* Dungeons
* World Bosses
* Raids
* Gauntlet / Trials
* PvP / GvG
* Expedition / Invasion

The level/difficulty for each activity can be defined in the settings file. The bot can also:

* Bribe specific familiars with gems
* Revive fallen party members
* Save shrines for use before the boss in Trials and Raids
* Switch runes based on activity
* Notify you on your mobile devices through [Pushover](https://github.com/ilpersi/BHBot/wiki/Pushover-integration-Documentation) on how it is doing and if any issue is present
* Use consumables to keep boosts running unattended
* Strip items for PvP/GvG
* Solo certain zones for bounty requirements
* Collect completed bounties
* Collect fishing baits
* Claim all weekly rewards
* Screenshot and close PMs
* Open skeleton chests
* Handle incomplete teams

If the bot detects a "Disconnected" dialog, it will pause its execution for an hour giving the user a chance to play manually.
Disconnects are usually result of another instance logging into the game. This is why bot pauses in case it detects it.

## Download
You can download the latest stable release BHBot.jar from the [releases](https://github.com/ilpersi/BHBot/releases) page.

For the latest functionality it is recommended to compile your own version as there are frequent commits with bug-fixes and new functionality between major releases.

## First time setup

See [the wiki](https://github.com/ilpersi/BHBot/wiki) for the first time setup guide.

## Important

- While bot is running, do not interfere with the Chromium window that the bot has open via chromedriver. That means don't open menus and click around since that may confuse the bot and it could fail to do its tasks (which could lead to crashing it).
- If you want to continue using your computer while running the bot use, the 'hide' command to minimize the window. The bot clicks on certain buttons and cues and expects certain thing to pop up, and if they don't (due to user interaction), then it will fail to function properly. 
- If you are running the bot in Windows, you may want to run it under a separate account in order for it to not interfere with your work.

## Commands
Here is a list of most common commands used with the bot (must be typed in the console window, or, if you use web interface, in the
command input box):

- `do dungeon|expedition|gauntlet|gvg|pvp|raid|trials`: force the bot to perform a dungeon . Example: "do raid". Used for debugging purposes more or less (bot will automatically attempt dungeons).
- `hide`: hides Chromium window.
- `pause [mins]`: pauses bot's execution. Useful when you want to play the game yourself (either from the same Chromium window, or by starting another Chromium window). The bots is paused untill are resue command is issued or, if specified, for a number of minutes equal to _mins_
- `plan <plan_name>`: if you have different configurations you can use this command to swith between them. BHBot will look for file named <plan_name.ini> in the plans/ subfolder
- `pomessage [message]`: use this command to verify that the Pushover integration is correctly configured. Message parameter is optional and if not specified, a standard messabe will be sent.
- `print`: Using this command, you can print different informations regarding the bot
  - `familiars`: output the full list of supported familiars in the encounter management system
  - `version`: output the version of BHBot. This is is useful when reporting a bug
- `readouts`: will reset readout timers (and hence immediately commence reading out resources).
- `reload`: will reload the 'settings.ini' file from disk and apply any changes on-the-fly.
- `restart`: restarts the chromedriver (closes Chromium and opens a fresh Chromium window). Use only when something goes wrong (should restart automatically after some time in that case though).
- `resume`: resumes bot's execution.
- `set`: sets a setting line, just like from a 'settings.ini' file. Example: "set raids 1 3 100", or "set difficulty 70". Note that this overwritten setting is NOT saved to the 'settings.ini' file! Once you issue <reload> command, it will get discharged.
- `shot [prefix]`: takes a screenshot of the game and saves it to 'shot.png'. If a _prefix_ is specified it will be used instead of the default _shot_ one.
- `show`: shows Chromium window again after it has been hidden.
- `stop`: stops the bot execution (may take a few seconds. Once it is stopped, the console will close automatically).
  
## Authors
BHBot is an original idea by [Betalord](https://github.com/Betalord) and he used it until his character achieved level 165. On 29th of September 2017 (a 1st year anniversary of the Bit Heroes game) he quit the game and released the bot to the public. In december 2018 [Fortigate](https://github.com/Fortigate) decided to pick on the development and from march 2019 [ilpersi](https://github.com/ilpersi) joined him to make the bot what it is today. Starting from june 2019 ilpersi is the new owner of the project.

## Special thanks
This bot would not exit without all the people that use it or that contribuited to it. A special mention goes to the [JProfiler team](https://www.ej-technologies.com/products/jprofiler/overview.html), a Java profiler tool, that granted us a free open-source license to help make this tool even better.  

## Finally

Hopefully this bot will prove useful to you, if you have any questions just raise an issue or join our gitter room in which you can talk directly with the developers! Enjoy :-)
