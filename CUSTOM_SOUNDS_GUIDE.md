# Custom Sounds Guide

## Adding Custom Sounds to Your Plugin

The plugin now supports fully configurable sounds! You can use either Minecraft's built-in sounds OR add your own custom sound files.

### Current Configuration

All sounds are now configurable in `config.yml` under the `sounds:` section. Each sound has:
- **Enable/disable toggle** (e.g., `kill-sound: true`)
- **Sound name** (e.g., `kill-sound-name: "ENTITY_PLAYER_LEVELUP"`)
- **Volume** (e.g., `kill-sound-volume: 1.0`)
- **Pitch** (e.g., `kill-sound-pitch: 1.5`)

### Option 1: Use Built-in Minecraft Sounds (Current Default)

The plugin currently uses these Minecraft sounds by default:
- Kill: `ENTITY_PLAYER_LEVELUP` (pitch 1.5)
- Assist: `ENTITY_EXPERIENCE_ORB_PICKUP` (pitch 1.2)
- Death: `ENTITY_PLAYER_DEATH`
- Respawn: `ITEM_TOTEM_USE` (volume 0.5)
- Game Start: `ENTITY_ENDER_DRAGON_GROWL`
- Victory: `UI_TOAST_CHALLENGE_COMPLETE`
- Defeat: `ENTITY_WITHER_SPAWN` (volume 0.5, pitch 0.8)
- Headshot: `BLOCK_GLASS_BREAK` (pitch 1.8)

You can change these to any Minecraft sound by editing the config.yml.

### Option 2: Add Custom Sound Files

To add your own custom sounds:

1. **Prepare your sound files:**
   - Format: `.ogg` (Vorbis codec) - Minecraft only supports OGG format
   - Recommended: Mono audio, 44.1kHz sample rate
   - Keep file sizes small for better performance

2. **Create the sounds directory structure:**
   ```
   src/main/resources/assets/tdm/sounds/
   ```

3. **Add your sound files:**
   Place your `.ogg` files in the sounds directory:
   - `kill.ogg`
   - `assist.ogg`
   - `death.ogg`
   - `respawn.ogg`
   - `game_start.ogg`
   - `victory.ogg`
   - `defeat.ogg`
   - `headshot.ogg`

4. **The sounds.json is already configured!**
   The file at `src/main/resources/sounds.json` already defines all the custom sounds.

5. **Update config.yml to use custom sounds:**
   Change the sound names in config.yml from Minecraft sounds to custom sounds:
   ```yaml
   sounds:
     kill-sound-name: "tdm.kill"
     assist-sound-name: "tdm.assist"
     death-sound-name: "tdm.death"
     respawn-sound-name: "tdm.respawn"
     game-start-sound-name: "tdm.game_start"
     victory-sound-name: "tdm.victory"
     defeat-sound-name: "tdm.defeat"
     headshot-sound-name: "tdm.headshot"
   ```

6. **Rebuild the plugin:**
   Run `./build.ps1` to compile the plugin with your custom sounds included.

7. **Install on server:**
   The sounds are embedded in the plugin JAR - no resource pack needed!
   Players will automatically hear your custom sounds.

### How It Works

- Custom sounds are embedded directly in the plugin JAR file
- The plugin uses Minecraft's sound system to play them
- No client-side resource pack required
- Sounds work for all players automatically

### Finding/Creating Custom Sounds

- **Free sound effects:** freesound.org, zapsplat.com, soundbible.com
- **Convert to OGG:** Use Audacity (free) - File > Export > Export as OGG Vorbis
- **Edit sounds:** Audacity can trim, adjust volume, change pitch, etc.

### Configuring Sounds

You can customize each sound in `config.yml`:

```yaml
sounds:
  # Enable/disable
  kill-sound: true
  
  # Sound name (Minecraft sound or custom "tdm.soundname")
  kill-sound-name: "tdm.kill"
  
  # Volume (0.0 to 1.0)
  kill-sound-volume: 1.0
  
  # Pitch (0.5 to 2.0, 1.0 = normal)
  kill-sound-pitch: 1.5
```

### Troubleshooting

- **Sound not playing?** Check the sound file is in the correct directory and is .ogg format
- **Wrong sound playing?** Verify the sound name in config.yml matches sounds.json
- **Sound too quiet/loud?** Adjust the volume setting in config.yml
- **Sound too high/low pitched?** Adjust the pitch setting in config.yml

### Unicode Symbols Available

Current symbols used in scoring messages:
- ⚔ (Crossed Swords) - for kills
- ⭐ (Star) - for assists
- 💢 (Headshot) - for headshots

You can customize these in `config.yml`:

```yaml
scoring:
  kill-points: 20
  assist-points: 10
  headshot-bonus: 5
  kill-message: "+%points% ⚔ KILL"
  assist-message: "+%points% ⭐ ASSIST"
  headshot-message: "+%points% 💢 HEADSHOT"
```

Other Unicode symbols you can use:
- 💀 (Skull) - for deaths
- 🏆 (Trophy) - for victory
- 🎯 (Target) - for accuracy
- ⚡ (Lightning) - for speed
- 🛡️ (Shield) - for defense
- 🏹 (Bow) - for archer class
- ⚔️ (Sword) - for knight class
- 🔥 (Fire) - for kill streaks
- 💥 (Explosion) - for critical hits

Just replace the symbols in the `config.yml` scoring messages!
