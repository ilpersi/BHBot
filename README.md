# BHBot

## What is BHBot?
BHBot is a program that automates different aspects of the [Bit Heroes](http://www.kongregate.com/games/juppiomenz/bit-heroes) game.
It is a non intrusive program that works by opening up a Chrome window (or a Firefox, or any other browser for which Selenium WebDriver
driver exists) and controls it as a normal user would. It works by taking screenshot every 500 ms, detects cues from screenshot and
then simulates mouse clicks. BHBot is a good example of how to create such bots to automate flash games or other browser games
(it can be easily adjusted to play some other game).

Once configured and running the bot is designed to run unattended for weeks at a time.

## Features
This bot will automatically farm schematics, familiars and cosmetics running the following:
* Dungeons
* Raids
* Gauntlet / Trials
* PvP / GvG
* Expedition / Invasion

The level/difficulty for each activity can be defined in the settings file. The bot can also:

* Automatically bribe specific familiars defined in the settings file (with limits so you dont waste gems)
* Use consumables to keep 25% IF/XP/GF running unattended
* Claim all weekly rewards
* Open skeleton chests
* Screenshot and close PMs
* Handle incomplete teams (for when your 4.4k tank leaves you)
* Collect daily fishing bait

If the bot detects a "Disconnected" dialog, it will pause its execution for 1 hour giving the user a chance to play manually.
Disconnects are usually result of another instance logging into the game. This is why bot pauses in case it detects it.

## Upcoming Features
* World Boss farming
* Fishing

## First time use

See [the wiki](https://github.com/Betalord/BHBot/wiki) for first time use guide.

## Important

While bot is running, do not interfere with the Chrome window that the bot has open via chrome driver. That means don't open
menus and click around since that may confuse the bot and it could fail to do its tasks (which could lead to crashing it). If you want to continue using your comuter while running the not use the 'hide' command to minimize the window. The bot clicks on certain buttons and cues and expects certain thing to pop up, and if they don't (due to user interaction), then it will fail to function properly. If you are running the bot in Windows, you may want to run it under a separate account in order for it to not interfere
with your work.

## Commands
Here is a list of most common commands used with the bot (must be typed in the console window, or, if you use web interface, in the
command input box):

- `stop`: stops the bot execution (may take a few seconds. Once it is stopped, the console will close automatically).
- `pause`: pauses bot's execution. Useful when you want to play the game yourself (either from the same Chrome window, or by starting another Chrome window).
- `resume`: resumes bot's execution.
- `reload`: will reload the 'settings.ini' file from disk and apply any changes on-the-fly.
- `shot`: takes a screenshot of the game and saves it to 'shot.png'.
- `restart`: restarts the Chrome driver (closes Chrome and opens a fresh Chrome window). Use only when something goes wrong (should restart automatically after some time in that case though).
- `hide`: hides Chrome window.
- `show`: shows Chrome window again after it has been hidden.
- `do`: force a dungeon/raid/pvp/gvg/gauntlet/trials. Example: "do raid". Used for debugging purposes more or less (bot will automatically attempt dungeons).
- `set`: sets a setting line, just like from a 'settings.ini' file. Example: "set raids 1 3 100", or "set difficulty 70". Note that this overwritten setting is NOT saved to the 'settings.ini' file! Once you issue <reload> command, it will get discharged.
- `plan`: will load a settings file from a plan folder. Some sample plan files are attached to this distribution. Example: "plan idle" (will load 'plans/idle.ini' and overwritte current settings with it). You should write your own plan files as needed.
- `readouts`: will reset readout timers (and hence immediately commence reading out resources).

## Advanced

If you want to run 2 instances of bot in parallel (or even more), then you'll probably need to run two instances of
ChromeDriver as well (at least on Windows). That can be done, but needs some adjustments. First of all, you'll need to run your
chrome driver like this:
`chromedriver.exe --port=9550`
(in case you want your chrome driver to run on port 9550). The next chrome driver instance will run e.g. on port 9551,
so we'll run it like this: `chromedriver.exe --port=9551`.
You will  need to run the chromedrivers in separate folders as they create a chrome_profile settings folder in the current directory.
Now, in order to tell the bot to connect to chrome driver on one of these ports, you'll need to run it like this:
`bhbot.jar chromedriveraddress 127.0.0.1:9550`. This will make sure that the bot will connect to the first chrome driver instance.
The second bot instance should be run like this:
`bhbot.jar chromedriveraddress 127.0.0.1:9551`. This should make two bots run in parallel without disturbing each other.

Note that due to Flash this can be quiet resource intensive. Due to the slow nature of energy regeneration if you wish to run multiple accounts on Windows it is more effective to use a cron-like service (E.G [Z-Cron](https://www.z-cron.com/)) to stagger running and stopping multiple accounts, generally you only need to run the bot for 30 minutes to use 8 hours of shards/energy/tokens/tickets regeneration.

## An example Linux setup

Here I give an example setup of the bot (and web interface) for Linux. Note that web interface is possible only when BHBot runs
in Linux, since it injects commands to `screen`, with which bot should be ran.

Lets see a typical folder structure:

* `/home/betalord/bhbot` root folder for the BHBot. This is where you put your bhbot.jar file, chromedriver, settings.ini, runbot file
(find it under [web/linux scripts](https://github.com/Betalord/BHBot/tree/master/web/linux%20scripts)), and the rest of the base files.
* `/home/betalord/bhbot/webserver` root folder for web interface. Here you need to put most of the files from
[web/linux scripts](https://github.com/Betalord/BHBot/tree/master/web/linux%20scripts) (all files except for `runbot`).
* `/home/betalord/bhbot/webserver/webroot` here goes contents of
[web/](https://github.com/Betalord/BHBot/tree/master/web/) (except for `linux_scripts` folder).
* `/home/betalord/bhbot/webserver/webroot/srv_bin` here you put the HTTPServer class files (and all the rest of class files that
compiling the HTTPServer produces, like `Misc` and `FletcherChecksum` classes).
* `/home/betalord/bhbot/marvin` here goes the marvin library files.
* `/home/betalord/bhbot/cues` here goes the cues (image files).
* `/home/betalord/bhbot/plans` here goes the plans files.

In order to run BHBot, you need to run it as following (or else web server won't be able to inject commands):

`screen -d -m -S bhbot`

If you need to access bot console, use the following command:

`screen -r bhbot`

You'll need to run chromedriver and HTTPServer (see
[web/linux scripts](https://github.com/Betalord/BHBot/tree/master/web/linux%20scripts) on how to run it).

## Download
You should download a release ready BHBot (for Windows and Linux) from RELEASES page:

https://github.com/Betalord/BHBot/releases

## Notes
* A brief demonstration of web interface can be viewed here:
https://youtu.be/TGXHvVJhZ7c
* The current ad system is not implemented and is unlikely to work.
* Currently consuming of major consumables doesn't work since I didn't have the cue files at the release time of
the bot. Average and minor consumables, however, work normally. If you require major consumables to work, you
should screenshot them and create the cue files yourself (see the cues/ folder on how they look like).
* The image recognition engine can have trouble with high resolution / non-standard DPI. If you're having issues with the bot not starting
try setting scaling to 100% and resolution 1920*1080 or less.

## Compiling
You can import the project into Eclipse (see the .project and .classpath files). It contains all the dependencies
needed to compile the project (under dependencies/ folder - if you import it into Eclipse, it will all be set up automatically).

The project uses only a fraction of the Marvin framework (does not depend on it - I've copied the required files into the src
folder) for image cue recognition. It heavily uses Selenium WebDriver for interaction with the web browser (jars included). It also
uses Selenium Shutterbug to take screenshots (jars included).

## Author
I, Betalord, am the original author of the bot. I have been using the bot until my character achieved level 165. On 29th
of September 2017 (a 1st year anniversary of the Bit Heroes game) I have quit the game and released the bot to the public.

Note that I do not offer support for this bot. If anyone is willing to continue maintaining and developing the bot (it will
soon become absolete, since BH developers often update the game), contact me so that I can give you the repository rights.

## Finally

Hopefully this bot will prove useful to you. Enjoy it :-)
