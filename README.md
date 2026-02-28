# Team Deathmatch Plugin

A 4v4 Team Deathmatch plugin for Paper 1.21.1 with class selection, spawn management, and scoring.

## Features

- 4v4 Red vs Blue team battles (plus optional Free‑For‑All mode)
- Free‑for‑All can support up to **10 coloured teams**; admins choose which teams are enabled.
- Automatic team balancing; can force random assignment with the admin GUI toggle.
- First to configurable number of kills wins.
- 4 unique classes: Archer, Berserker, Knight, Sharpshooter (plus future expansion easy).
- GUI class selection and full admin inventory menus.
- Configurable respawn timer, points for kills/assists/headshots.
- Sidebar scoreboard showing team scores, player points, kills, assists, and headshots.
- Admin spawn point configuration for every team (FFA or 4v4) stored in config.
- In‑game broadcasts with clickable join/start links.
- Full statistics at end of match including rankings.

## Commands

- `/tdm activate [4v4]` - Activate a new match. Without arguments activates FFA, `4v4` forces 4v4 mode.
    - Broadcasts clickable join options for enabled teams and a clickable start link.
- `/tdm join [team]` - Join the active match. If no team specified, picks smallest team; in FFA will choose among enabled teams. When random-only mode is toggled, a plain `/tdm join` picks a random enabled team.
- `/tdm start [4v4]` - Manually start the active match (admins only).
- `/tdm end` - End the current match early (admins only).
- `/tdm setspawn <team>` - Set the spawn point for the given team (any of the ten colours).
- `/tdm gui` - Open the admin inventory GUI for game control.

Tab completion for `/tdm join` and `/tdm setspawn` suggests valid team names based on configuration.

## Setup

1. Build with Maven: `mvn clean package`.
2. Place the resulting JAR in your server's `plugins` folder and restart.
3. Configure spawns for every team you plan to use (e.g. `/tdm setspawn red`, `/tdm setspawn blue`, etc.). Use `/tdm gui` → Set Team Spawns for easier GUI‑based setup.
4. Use `/tdm gui` to activate matches, toggle teams, broadcast join links and manage players.
5. Players join with `/tdm join` (or via broadcast links) and choose their class.

The `config.yml` stores enabled teams and all spawn coordinates automatically.

## Classes

- **Archer**: Bow, leather armor, 20 arrows

### New Features (2026 update)
- Free‑for‑all mode now supports up to **10 colour teams**.
- Admins can toggle which FFA teams are enabled via `/tdm gui` → Team Settings.
- Autocomplete and join broadcasts dynamically update with available teams.
- Use `/tdm setspawn <team>` to configure any team’s spawn point; locations are saved to `config.yml`.
- All scoring information (team scores, points, kills, assists, headshots) is shown in a sidebar scoreboard instead of the action bar.
- **Berserker**: Iron axe, chain armor, shield
- **Knight**: Iron sword, iron armor, shield
- **Sharpshooter**: Crossbow (Quick Charge II), 3 harming arrows, 10 regular arrows

## Permissions

- `tdm.admin` - Access to admin commands (default: op)
