# Custom Sounds Guide

## Adding Custom Sounds to Your Plugin

### Option 1: Use Built-in Minecraft Sounds (Current Implementation)
The plugin currently uses Minecraft's built-in sounds:
- Kill: `ENTITY_PLAYER_LEVELUP`
- Assist: `ENTITY_EXPERIENCE_ORB_PICKUP`
- Death: `ENTITY_PLAYER_DEATH`
- Respawn: `ITEM_TOTEM_USE`
- Game Start: `ENTITY_ENDER_DRAGON_GROWL`
- Victory: `UI_TOAST_CHALLENGE_COMPLETE`
- Defeat: `ENTITY_WITHER_SPAWN`

### Option 2: Add Custom Sound Files

1. **Prepare your sound files:**
   - Format: `.ogg` (Vorbis codec)
   - Place in: `src/main/resources/assets/tdm/sounds/`
   - Example files:
     - `kill.ogg`
     - `assist.ogg`
     - `victory.ogg`

2. **Update sounds.json:**
   Already created at `src/main/resources/sounds.json`

3. **Modify SoundManager.java to use custom sounds:**
   ```java
   player.playSound(player.getLocation(), "tdm.kill", SoundCategory.PLAYERS, 1.0f, 1.0f);
   ```

4. **Create a resource pack:**
   - Build your plugin JAR
   - Extract the assets folder
   - Package as a resource pack
   - Players must have the resource pack enabled

### Unicode Symbols Available

Current symbols used:
- ⚔ (Crossed Swords) - for kills
- ⭐ (Star) - for assists

Other options you can use:
- 💀 (Skull) - for deaths
- 🏆 (Trophy) - for victory
- 🎯 (Target) - for accuracy
- ⚡ (Lightning) - for speed
- 🛡️ (Shield) - for defense
- 🏹 (Bow) - for archer class
- ⚔️ (Sword) - for knight class

Just replace the symbols in the Component.text() calls in GameManager.java!
