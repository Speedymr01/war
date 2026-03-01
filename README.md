# Team Deathmatch Plugin

A competitive Team Deathmatch plugin for Minecraft Paper 1.21.1 featuring class-based combat, multiple game modes, and comprehensive scoring systems.

## Overview

Team Deathmatch brings fast-paced PvP action to your server with support for both classic 4v4 team battles and free-for-all matches with up to 10 color-coded teams. Players choose from unique combat classes, compete for kills and assists, and track their performance on a live scoreboard.

## Game Modes

### 4v4 Mode
Classic Red vs Blue team battles with automatic team balancing. Perfect for organized competitive matches.

### Free-For-All Mode
Support for up to 10 color-coded teams (Red, Blue, Green, Yellow, Purple, Orange, Aqua, Gray, White, Black). Admins can enable/disable specific teams through the admin GUI to customize the match size.

## Player Commands

### Joining a Match
- `/tdm join` - Automatically assigns you to the team with the fewest players for balance
  - If all enabled teams have equal players, picks a random team
  - If random-only mode is enabled by admin, always picks randomly from enabled teams
- `/tdm join <team>` - Join a specific team (e.g., `/tdm join red`)
  - Available teams: red, blue, green, yellow, purple, orange, aqua, gray, white, black
  - Tab completion shows currently enabled teams

### Class Selection
When you join a match, you'll see a GUI to select your combat class. Choose wisely based on your playstyle!

## Combat Classes

### Sharpshooter
Long-range specialist with precision weaponry
- Bow with 20 arrows
- Leather armor (lightweight for mobility)
- Best for: Players who prefer ranged combat and positioning

### Berserker
Aggressive melee fighter with high damage output
- Iron axe (high damage)
- Chainmail armor
- Shield for defense
- Best for: Aggressive players who like close combat

### Knight
Balanced warrior with solid defense
- Iron sword
- Full iron armor
- Shield for blocking
- Best for: Players who want durability and versatility

### Glass Cannon
High-risk, high-reward ranged attacker
- Crossbow with Quick Charge II enchantment
- 3 tipped arrows (harming) for devastating damage
- 10 regular arrows
- No armor (vulnerable but deadly)
- Best for: Skilled players who can land shots while avoiding damage

## Scoring System

### Points
- Kill: 20 points 
- Assist: 10 points 
- Headshot Bonus: +5 points 

### Headshots
Land projectile or melee hits on the upper portion of an enemy to mark them for a headshot bonus. If you kill that enemy within 5 seconds of landing the headshot, you'll receive bonus points. The headshot must be the killing blow or part of the damage that leads to the kill.

### Kill Streaks
Build up consecutive kills without dying to trigger special announcements:
- 3 kills: Kill streak announcement
- 5 kills: Dominating announcement
- 10 kills: Legendary status
- Your streak ends when you die, and the server announces who ended it

### Scoreboard
A live sidebar scoreboard displays:
- Team scores (first to reach the win threshold wins)
- Your personal points
- Your kills, assists, and headshots
- Real-time updates throughout the match

## Match Flow

1. **Activation**: An admin activates a match with `/tdm activate` (FFA) or `/tdm activate 4v4`
2. **Joining**: Players see a clickable broadcast message and can join with `/tdm join`
3. **Class Selection**: Each player chooses their combat class from the GUI
4. **Start**: Once enough players join, an admin starts the match with `/tdm start` or by clicking the broadcast link
5. **Combat**: Fight to reach the target kill count (default: 3 team kills)
6. **Respawn**: When you die, you spectate for a countdown (default: 5 seconds) before respawning
7. **Victory**: First team to reach the kill threshold wins, and detailed statistics are displayed

## Admin Commands

- `/tdm activate [4v4]` - Activate a new match
  - No argument: Free-for-all mode
  - `4v4`: Classic red vs blue mode
  - Broadcasts clickable join links to all players
  - Sends clickable start link to admins only
  
- `/tdm start` - Manually start the activated match

- `/tdm end` - End the current match immediately and display final statistics

- `/tdm setspawn <team>` - Set the spawn point for a team
  - Stand where you want the spawn point and run the command
  - Works for all 10 team colors
  - Spawn locations are saved to config.yml
  
- `/tdm gui` - Open the admin control panel
  - Activate matches
  - View team rosters
  - Move players between teams
  - Toggle team availability (FFA mode)
  - Configure game settings

## Configuration

The plugin is highly configurable through `config.yml`. Key settings include:

### Game Settings
- `wins-needed`: Kills required to win (default: 3)
- `respawn-time`: Respawn countdown in seconds (default: 5)
- `min-players`: Minimum players to start (default: 2)
- `max-players`: Maximum players allowed (default: 20)
- `auto-balance-teams`: Enable automatic team balancing

### Team Settings
Enable or disable specific teams for FFA mode:
```yaml
teams:
  enabled:
    red: true
    blue: true
    green: false
    # ... etc
```

### Scoring
Customize point values for kills, assists, and headshots:
```yaml
scoring:
  kill-points: 20
  assist-points: 10
  headshot-bonus: 5
```

### Game Rules
- `friendly-fire`: Allow team damage (default: false)
- `fall-damage`: Enable fall damage (default: true)
- `spectator-death`: Keep dead players in spectator mode until respawn (default: true)

### Damage Multipliers
Fine-tune damage values:
- `global-damage-multiplier`: Multiply all damage
- `fall-damage-multiplier`: Adjust fall damage
- `projectile-damage-multiplier`: Adjust arrow/crossbow damage
- `melee-damage-multiplier`: Adjust sword/axe damage

### Headshot Detection
- `head-region-percentage`: Fraction of entity height considered "head" (default: 0.20 = top 20%)

### Sounds
Enable/disable sound effects and adjust volume/pitch:
```yaml
sounds:
  death-sound: true
  kill-sound: true
  victory-sound: true
  sound-volume: 1.0
```
### Class Customization
Each class can be fully customized in `config.yml`. You can modify existing classes or add new ones:

```yaml
classes:
  # Class identifier (used internally)
  sharpshooter:
    enabled: true
    name: "Sharpshooter"  # Display name in GUI
    helmet: LEATHER_HELMET
    chestplate: LEATHER_CHESTPLATE
    leggings: LEATHER_LEGGINGS
    boots: LEATHER_BOOTS
    weapon: BOW
    off-hand: NONE  # or SHIELD, etc.
    extra-items:
      - "ARROW:20"  # Format: "MATERIAL:AMOUNT"
    enchantments:
      - "BOW:POWER:1"  # Format: "ITEM:ENCHANTMENT:LEVEL"
    health-bonus: 0  # Additional hearts (2 = 1 heart)
    armor-bonus: 0  # Additional armor points
```

To add a new class:
1. Copy an existing class section in `config.yml`
2. Change the class identifier (e.g., `my_custom_class`)
3. Customize the name, armor, weapons, and items
4. Set `enabled: true`
5. Reload the server or restart

The plugin will automatically load all enabled classes from the config and display them in the class selection GUI.

## Features

-  Multiple game modes (4v4 and FFA)
-  4 unique combat classes with distinct playstyles
-  Headshot detection for bonus points
-  Kill streak tracking and announcements
-  Live scoreboard with detailed statistics
-  Respawn countdown system
-  Assist tracking (damage contributors get credit)
-  Admin GUI for easy match management
-  Clickable chat messages for quick joining
-  Configurable spawn points per team
-  Team balancing options
-  Comprehensive end-game statistics
-  Sound effects for game events
-  Tab completion for commands
-  Highly configurable through config.yml

## Permissions

- `tdm.admin` - Access to admin commands and GUI (default: op)

## Requirements

- Minecraft Server: Paper 1.21.1 or compatible fork
- Java: 21 or higher

## Installation

1. Download the plugin JAR file
2. Place it in your server's `plugins` folder
3. Restart your server
4. Configure spawn points with `/tdm setspawn <team>`
5. Customize settings in `plugins/TeamDeathmatch/config.yml`
6. Reload with `/reload confirm` or restart the server
