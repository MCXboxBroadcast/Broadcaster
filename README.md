# MCXboxBroadcast
[![forthebadge made-with-java](https://forthebadge.com/images/badges/made-with-java.svg)](https://java.com/)

[![License: GPL-3.0](https://img.shields.io/github/license/rtm516/MCXboxBroadcast)](LICENSE)   [![HitCount](https://hits.dwyl.com/rtm516/MCXboxBroadcast.svg?style=flat)](http://hits.dwyl.com/rtm516/MCXboxBroadcast)

A simple [Geyser](https://github.com/GeyserMC/Geyser) extension that broadcasts the server over Xbox Live.

This shows up to the authenticated accounts friends ingame as a joinable session and then anyone thats friends with someone who joined through that method will also see the session as joinable ingame.

![Example screenshot](https://user-images.githubusercontent.com/5401186/159083033-b965bfba-de17-4708-8979-1f33bfd5fa28.png)

## Config
* `remote-address` - The IP address to broadcast, you likely want to change this to your servers public IP
* `remote-port` - The port to broadcast, this should be left as auto unless your manipulating the port using network rules or reverse proxies
* `update-interval` - The amount of time in seconds to update session information and sync other data
* `whitelist-friends` - Should Xbox Live friends automatically be whitelisted
