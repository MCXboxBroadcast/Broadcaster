# MCXboxBroadcast
[![License: GPL-3.0](https://img.shields.io/github/license/rtm516/MCXboxBroadcast)](LICENSE)
[![Build Release](https://github.com/rtm516/MCXboxBroadcast/actions/workflows/release.yml/badge.svg)](https://github.com/rtm516/MCXboxBroadcast/releases)
[![HitCount](https://hits.dwyl.com/rtm516/MCXboxBroadcast.svg?style=flat)](http://hits.dwyl.com/rtm516/MCXboxBroadcast)
[![Discord](https://img.shields.io/discord/1139621390908133396?label=discord&color=5865F2)](https://discord.gg/Tp3tA2kdCN)

A simple [Geyser](https://github.com/GeyserMC/Geyser) extension that broadcasts the server over Xbox Live.

This shows up to the authenticated accounts friends in-game as a joinable session and then anyone thats friends with someone who joined through that method will also see the session as joinable ingame.

![Example screenshot](https://user-images.githubusercontent.com/5401186/159083033-b965bfba-de17-4708-8979-1f33bfd5fa28.png)

# DISCLAIMER
You use this project at your own risk, the contributors are not responsible for any damage or loss caused by the software. We suggest you use an alt account for running the tool incase the account it banned as we emulate some features of a client which may or may not be against TOS.

## Features
 - Syncing of MOTD and other server details
 - Automatic friend list management
 - Easy Geyser integration (as an extension)
 - Shows as online and playing Minecraft in the Xbox app and website
 - Multi-account support

## Pterodactyl Panel
There is an egg for easy instance creation supplied for [Pterodactyl Panel](https://pterodactyl.io/), this being `egg-m-c-xbox-broadcast.json`

## Installation
### Extension
1. Download the latest release file `MCXboxBroadcastExtension.jar`
2. Drop the extension into the Geyser `extensions` folder
3. Restart the server
4. Wait for the extension to start and present you with an authentication code
   - `To sign in, use a web browser to open the page https://www.microsoft.com/link and enter the code XXXXXXXX to authenticate.`
5. Follow the link and enter the code
6. Login to the account you want to use
7. Follow the account on Xbox LIVE
8. Check the friends tab ingame and you should see the server listed

### Standalone
1. Download the latest release file `MCXboxBroadcastStandalone.jar`
2. Start the jar file using `java -jar MCXboxBroadcastStandalone.jar`
3. Wait for the extension to start and present you with an authentication code
    - `To sign in, use a web browser to open the page https://www.microsoft.com/link and enter the code XXXXXXXX to authenticate.`
4. Follow the link and enter the code
5. Login to the account you want to use
6. Follow the account on Xbox LIVE
7. Check the friends tab ingame and you should see the server listed


## Commands
For the extension version prefix with `/mcxboxbroadcast`

| Command | Description |
| --- | --- |
| `exit` (Standalone Only) | Exits the program |
| `restart` | Restarts the tool |
| `dumpsession` | Dumps the current session data to files for debugging |
| `accounts list` | Lists the accounts that are currently in use and their followers count |
| `accounts add <sub-session-id>` | Adds an account to the list of accounts to use |
| `accounts remove <sub-session-id>` | Removes an account from the list of accounts to use |
