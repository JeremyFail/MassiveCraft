![FactionsChat Logo](https://factions.wiki/FactionsChat/img/hero.png)

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)

FactionsChat is a Minecraft Spigot/Paper server plugin that integrates with MassiveCraft Factions to provide advanced chat features and channels for faction-based servers.

## Features

- **Configurable Chat Formatting:**  
  The chat format is fully configurable via `config.yml`, supporting both PlaceholderAPI placeholders and built-in Factions tags.
- **Multiple Chat Channels:**  
  Supports Ally, Truce, Faction, Enemy, Neutral, Local, Global, Staff, and World chat channels.
- **Channel Permissions:**  
  Access to each chat channel is controlled by permissions (e.g. `factionschat.ally`, `factionschat.faction`, etc).
- **Channel Switching and Quick Messaging:**  
  Use `/f c <channel>` to switch your active chat mode, or `/f c <channel> <message>` to send a one-off message.
- **Customizable Prefixes and Colors:**  
  Prefixes and text colors for each channel are configurable in `config.yml`.
- **Integration with Essentials and DiscordSRV:**  
  SocialSpy support for Essentials, and optional DiscordSRV integration for staff chat relay.
- **Integration with PlaceholderAPI:**  
  Supports PlaceholderAPI for chat formatting and custom placeholders. If PlaceholderAPI is installed, all placeholders in the chat format will be parsed using PAPI. See the wiki for more details.
- **Built-in Tag Parsing:**  
  If PlaceholderAPI is not installed, FactionsChat will parse built-in tags for faction names, ranks, relation colors, and more. See the wiki for more details.
- **Per-Player Chat Mode Persistence:**  
  Remembers each player's last used chat mode across restarts.
- **Local Chat Range:**  
  Configurable range for local chat visibility.

## Usage and Configuration

Please read the [Wiki](https://factions.wiki/FactionsChat) for details about supported commands, permissions, configuration instructions, FAQs, and troubleshooting steps.

## Requirements

As a Factions add-on, this plugin requires both MassiveCore and Factions, and has optional requirements if you wish to use other integration features that this plugin supports.

- MassiveCore
- Factions
- (Optional) [EssentialsX](https://essentialsx.net/)
- (Optional) [DiscordSRV](https://modrinth.com/plugin/discordsrv)
- (Optional) [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) for advanced chat formatting and placeholder support

## Compilation

See the [main README](../README.md) for instructions on compiling the plugin.

## About
This plugin is based on the plugin `Factions3Chat`, created in 2020 by eirikh1996 for the original Factions3 by Madus. It has been rewritten in Java (from the original's Kotlin) and updated/extended/modified to be part of the MassiveCraft projects that are part of this repository.

The original plugin download can be found here: https://dev.bukkit.org/projects/factions3chat

The original source can be found here: https://github.com/eirikh1996/Factions3Chat