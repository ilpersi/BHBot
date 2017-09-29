# BHBot

## What is BHBot?
BHBot is a program that automates different aspects of the [Bit Heroes](http://www.kongregate.com/games/juppiomenz/bit-heroes) game.
It is a non intrusive program that works by opening up a Chrome window (or a Firefox, or any other browser for which Selenium WebDriver
driver exists) and controls it as a normal user would. It works by taking screenshot every 500 ms, detects cues from screenshot and
then simulates mouse clicks. BHBot is a good example of how to create such bots to automate flash games or other browser games
(it can be easily adjusted to play some other game).

## Features
Bot can automatically play the following types of encounters:
* dungeons
* raids
* gauntlet
* trials
* PvP
* GvG
* Invasion

The difficulty for each such encounter can be (and should be) adjusted in the settings file. Within encounters, the bot will automatically:

* claim ad chests
* decline merchants
* decline red (skeleton) chests
* persuade any familiar that offers himself

Bot will also try to watch any ad offer in the main screen and claim reward. Note that as of  september 2017, the ad system has been
reworked and bot is not able to claim al types of ads anymore. On failure, it will restart however.

It will also claim daily reward and all week rewards automatically (for PvP, GvG and other types of events). It will handle
maintenance/update dialogs, "Are you still here?" dialog, and pretty much any other dialog (adding new dialog handlers is also very easy).
In case it encounters a new, unknown type of dialog, it will time out after a while and auto-restart itself.

It will consume consumables as specified in the settings.ini file (potions, tomes, pots), like item find, exp, gold boosts, etc.

It will automatically screenshot any private messages and weekly rewards as they will pop up (it will close them automatically),
as well as serious crashes (it drop these screenshots in the same folder it has been ran from).

It will detect when teams are incomplete (team that you use to play GvG, PvP, dungeon, raid, etc.) and click on AUTO button. This
will not put tanks in front, so you should adjust it manually when possible. (Note for version v20: this feature might currently
be broken).

In case bot detects a "Disconnected" dialog, it will pause its execution for 1 hour and thus give chance to the user to play the game.
Disconnects are usually result of another Chrome instance (or android app) logging into the game. This is why bot pauses in case it 
detects it.

## First time use

1) Before actually running the bot, you need to run a [ChromeDriver](https://sites.google.com/a/chromium.org/chromedriver/) instance.
You need to download correct version (32/64 bit), or else it may not work.
2) You will need Java runtime installed on your system in order for the bot to run. You may run it by directly running the jar file,
or by running pre-compiled exe file (Windows only, requires JRE as well).
3) Inspect `settings.ini` file and adjust to your needs.
4) When you run the bot for the first time, it is best that you run it using the `init.bat` file. This will make sure the
bot doesn't perform any operations (it will go into idle mode). It will leave chrome window open for you to set up certain
settings within the game (like familiars filter, notifications filter, etc.). Once you're done setting it, close the chrome
window and the bot's console window. This will create certain cookies within your Chrome profile folder that will be used
the next time you run the bot. Alternativelly to using `init.bat`, you can run bot normally and immediatelly after it loads,
issue command `pause` which will make sure bot leave Chrome window to you.
5) As described in the previous step, you should turn off notifications in the game's settings, since that obscures the energy
and PvP ticket bars and may interfere with with energy/tickets detection routine.
6) When you first run the bot, a 'chrome_profile' folder will be created by the chrome driver, where your chrome profile will
be saved (cookies, etc.). You should not touch that folder.

## Important

While bot is running, do not interfere with the Chrome window that the bot has open via chrome driver. That means don't open
menus and click around since that may confuse the bot and it could fail to do its tasks (which could lead to crashing it).
Bot clicks on certain buttons and cues and expects certain thing to pop up, and if they don't (due to user interaction), then he
will fail to function properly (however he will detect any such inconsistent states and will auto-restart). In general, bot always
restarts when it detects some kind of an error or when nothing happens in 30 minutes (time-out detection). In such cases, it will
restart the Chrome driver and itself. This ensures that the bot will run uninterrupted for weeks without user interaction (tested - it
works). If you are running the bot in Windows, you may want to run it under a separate account in order for it to not interfere
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
- `readouts`: will reset readout timers (and hence immediatelly commence reading out resources).

## Advanced

If you want to run 2 instances of bot in parallel (or even more), then you'll probably need to run two instances of
ChromeDriver as well (at least on Windows). That can be done, but needs some adjustments. First of all, you'll need to run your
chrome driver like this:
`chromedriver.exe --port=9550`
(in case you want your chrome driver to run on port 9550). The next chrome driver instance will run e.g. on port 9551,
so we'll run it like this: `chromedriver.exe --port=9551`.
Now, in order to tell the bot to connect to chrome driver on one of these ports, you'll need to run it like this:
`bhbot.exe chromedriveraddress 127.0.0.1:9550`. This will make sure that the bot will connect to the first chrome driver instance.
The second bot instance should be run like this:
`bhbot.exe chromedriveraddress 127.0.0.1:9551`. This should make two bots run in parallel without disturbing each other.

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
* I've noticed that in version v20 the "Enter GVG event" at the start of the week is broken, so bot will fail to
play GvG until you manually enter the GvG event. Once you enter it, it will continue to play GvG normally.
* A brief demonstration of web interface can be viewed here:
https://youtu.be/TGXHvVJhZ7c
* As of september 2017, Kongregate has changed the ad system and the bot can't handle all types of ads anymore.
It will still handle simple ads, the ones when one needs to wait for the close button to appear, but more complex
ads (like the ones that require clicking on them) won't work. If this should slow down your bot too much (due to
constant restarts and timeouts), disable the ads within the game settings.
* Currently consuming of major consumables doesn't work since I didn't have the cue files at the release time of
the bot. Average and minor consumables, however, work normally. If you require major consumables to work, you
should screenshot them and create the cue files yourself (see the cues/ folder on how they look like).

## Author
I, Betalord, am the original author of the bot. I have been using the bot until my character achieved level 165. On 29th
of September 2017 (a 1st year anniversary of the Bit Heroes game) I have quit the game and released the bot to the public.

Note that I do not offer support for this bot. If anyone is willing to continue maintaining and developing the bot (it will
soon become absolete, since BH developers often update the game), contact me so that I can give you the repository rights.

## Finally

Hopefully this bot will prove useful to you. Enjoy it :-)
