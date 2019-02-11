# BHBot

## What is BHBot?
BHBot is a program that automates different aspects of the [Bit Heroes](http://www.kongregate.com/games/juppiomenz/bit-heroes) game.
It is a non intrusive program that works by opening up a Chromium window and controls it as a normal user would. It works by taking a screenshot every 500 ms, detecting cues from the screenshot and
then simulating mouse clicks. BHBot is a good example of how to create such bots to automate flash games or other browser games
(it can be easily adjusted to play some other game).

Once configured and running the bot is designed to run unattended indefinitely.

## Features
This bot will automatically farm items, familiars and schematics running the following:
* Dungeons
* World Bosses
* Raids
* Gauntlet / Trials
* PvP / GvG
* Expedition / Invasion

The level/difficulty for each activity can be defined in the settings file. The bot can also:

* Automatically bribe specific familiars defined in the settings file (with limits so you dont waste gems)
* Use consumables to keep 25% IF/XP/Gold boosts running unattended
* Strip items for PvP to lower TS
* Solo certain zones for bounty requirements
* Collect completed bounties
* Claim all weekly rewards
* Screenshot and close PMs
* Handle incomplete teams

If the bot detects a "Disconnected" dialog, it will pause its execution for 1 hour giving the user a chance to play manually.
Disconnects are usually result of another instance logging into the game. This is why bot pauses in case it detects it.

## Upcoming Features
* Watching and Collecting Ads
* Opening skeleton chests

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
- `pause`: pauses bot's execution. Useful when you want to play the game yourself (either from the same Chromium window, or by starting another Chromium window).
- `resume`: resumes bot's execution.
- `reload`: will reload the 'settings.ini' file from disk and apply any changes on-the-fly.
- `shot`: takes a screenshot of the game and saves it to 'shot.png'.
- `restart`: restarts the chromedriver (closes Chromium and opens a fresh Chromium window). Use only when something goes wrong (should restart automatically after some time in that case though).
- `hide`: hides Chromium window.
- `show`: shows Chromium window again after it has been hidden.
- `do`: force a dungeon/raid/pvp/gvg/gauntlet/trials. Example: "do raid". Used for debugging purposes more or less (bot will automatically attempt dungeons).
- `set`: sets a setting line, just like from a 'settings.ini' file. Example: "set raids 1 3 100", or "set difficulty 70". Note that this overwritten setting is NOT saved to the 'settings.ini' file! Once you issue <reload> command, it will get discharged.
- `readouts`: will reset readout timers (and hence immediately commence reading out resources).

## Multiple Instances

If you want to run 2 instances of bot in parallel (or even more), then you'll probably need to run two instances of
chromedriver as well (at least on Windows). That can be done, but needs some adjustments. First of all, you'll need to run your
chromedriver like this:
`chromedriver.exe --port=9550`
(in case you want your chromedriver to run on port 9550). The next chromedriver instance will run e.g. on port 9551,
so we'll run it like this: `chromedriver.exe --port=9551`.
You will  need to run the chromedrivers in separate folders as they create a chrome_profile settings folder in the current directory.
Now, in order to tell the bot to connect to chromedriver on one of these ports, you'll need to run it like this:
`bhbot.jar chromedriveraddress 127.0.0.1:9550`. This will make sure that the bot will connect to the first chromedriver instance.
The second bot instance should be run like this:
`bhbot.jar chromedriveraddress 127.0.0.1:9551`. This should make two bots run in parallel without disturbing each other.

Note that due to Flash this can be quiet resource intensive. Due to the slow nature of energy regeneration if you wish to run multiple accounts on Windows it is more effective to use a cron-like service (E.G [Z-Cron](https://www.z-cron.com/)) to stagger running and stopping multiple accounts, generally you only need to run the bot for 30 minutes to use 8 hours of shards/energy/tokens/tickets regeneration.

## Download
You can download a release ready BHBot.jar from the RELEASES page:

https://github.com/Betalord/BHBot/releases

For the latest functionality it is recommended to compile your own version as there are frequent commits with bug-fixes and new functionality between major releases.

## Notes
* The current ad system needs to be completely rewritten and is unlikely to work.
* The image recognition engine can have trouble with high resolution / non-standard DPI. If you're having issues with the bot not starting
try setting scaling to 100% and resolution 1920*1080 or less.

## Finally

Hopefully this bot will prove useful to you, if you have any questions just raise an issue! Enjoy :-)

## Author
I, Betalord, am the original author of the bot. I have been using the bot until my character achieved level 165. On 29th
of September 2017 (a 1st year anniversary of the Bit Heroes game) I have quit the game and released the bot to the public.

If anyone is willing to continue maintaining and developing the bot contact me so that I can give you the repository rights.
