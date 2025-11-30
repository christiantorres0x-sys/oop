# Brick Break - Sound & Music Setup Guide

## Overview
The game uses a `SoundManager` class to handle all audio. All sound files should be placed in the `sounds/` directory as WAV files.

## Required Audio Files

Place these files in the `sounds/` directory:

### 1. **bg_music.wav**
- **Type**: Background Music
- **Duration**: 30-60 seconds (will loop)
- **Volume**: Moderate energy (SoundManager plays at 0.3 volume by default)
- **Style**: Arcade-style, energetic but not aggressive
- **Format**: WAV (16-bit, 44.1 kHz recommended)

### 2. **ball_hit.wav**
- **Type**: Sound Effect
- **Duration**: 0.1-0.3 seconds
- **Style**: Soft "pop" or "ping" sound
- **Use**: When ball hits paddle or walls
- **Format**: WAV (16-bit, 44.1 kHz recommended)

### 3. **box_hit.wav**
- **Type**: Sound Effect
- **Duration**: 0.1-0.3 seconds
- **Style**: Slightly sharper "click" or "tap"
- **Use**: When ball hits a brick
- **Format**: WAV (16-bit, 44.1 kHz recommended)

### 4. **win_sound.wav**
- **Type**: Sound Effect / Jingle
- **Duration**: 0.5-1.5 seconds
- **Style**: Celebratory chime or victory fanfare
- **Use**: When all bricks are destroyed
- **Format**: WAV (16-bit, 44.1 kHz recommended)

### 5. **lose_sound.wav**
- **Type**: Sound Effect / Chime
- **Duration**: 0.3-0.8 seconds
- **Style**: Low-tone "fail" or "game over" sound
- **Use**: When lives reach 0 (game over)
- **Format**: WAV (16-bit, 44.1 kHz recommended)

### 6. **menu_click.wav**
- **Type**: Sound Effect
- **Duration**: 0.1-0.2 seconds
- **Style**: Light "click" or "whoosh"
- **Use**: When START button is clicked
- **Format**: WAV (16-bit, 44.1 kHz recommended)

## Creating Audio Files

### Option 1: Use Free Online Tools
- **Beepbox** (beepbox.co) - Great for chiptune/retro sounds
- **Bfxr** (bfxr.net) - Specialized for game sound effects
- **Audacity** (audacity.sourceforge.net) - Free audio editor

### Option 2: Recommended Sound Sources
- **Freesound.org** - Creative Commons sounds
- **Zapsplat.com** - Royalty-free sound effects
- **Opengameart.org** - Game audio assets
- **NotchNelson** - Retro game music generator

### Quick Setup Instructions

#### Using Bfxr (Recommended for SFX):
1. Go to bfxr.net
2. Click "Randomize" to generate random sounds
3. Click "Export WAV" to download
4. Save to `sounds/` directory with appropriate name

#### Using Beepbox (Recommended for Music):
1. Go to beepbox.co
2. Create a short melody (30-60 seconds)
3. Use "Export" → "Download as .wav"
4. Save as `bg_music.wav` in `sounds/` directory

#### Using Audacity (For Advanced Editing):
1. Create or import audio
2. Export as "Other Uncompressed Files" → "WAV (Microsoft)" 16-bit PCM
3. Save to `sounds/` directory

## SoundManager API Reference

```java
// Play a one-time sound effect
soundManager.playSFX("ballHit");        // Ball hits paddle/wall
soundManager.playSFX("boxHit");         // Ball hits brick
soundManager.playSFX("win");            // All bricks destroyed
soundManager.playSFX("lose");           // Game over (0 lives)
soundManager.playSFX("menuClick");      // START button clicked

// Background music control
soundManager.playBackgroundMusic();     // Start looping music
soundManager.stopBackgroundMusic();     // Stop music
soundManager.pauseBackgroundMusic();    // Pause (can resume)
soundManager.resumeBackgroundMusic();   // Resume from pause

// Volume control
soundManager.setMasterVolume(0.5f);     // 0.0 (silent) to 1.0 (full)
soundManager.setSFXVolume(0.7f);        // SFX volume multiplier
soundManager.setMusicVolume(0.3f);      // Music volume multiplier

// Cleanup (on app exit)
soundManager.cleanup();
```

## Volume Levels (Defaults)
- **Master Volume**: 1.0 (100%)
- **SFX Volume**: 0.7 (70% of master)
- **Music Volume**: 0.3 (30% of master)

You can adjust these in `SoundManager.java` if needed:
```java
private float masterVolume = 1.0f;  // Change this
private float sfxVolume = 0.7f;     // Or this
private float musicVolume = 0.3f;   // Or this
```

## Troubleshooting

### "Warning: Sound file not found"
- Make sure files are in the `sounds/` directory
- Verify exact filenames match (case-sensitive on some systems):
  - `bg_music.wav`
  - `ball_hit.wav`
  - `box_hit.wav`
  - `win_sound.wav`
  - `lose_sound.wav`
  - `menu_click.wav`

### Audio Not Playing
- Check file format: Must be WAV (not MP3, OGG, or FLAC)
- Check file integrity: Try opening in an audio editor
- Check file path: Print to console with `System.out.println(soundFile.getAbsolutePath())`

### No Sound on Linux/Mac
- Java's audio system varies by OS
- Ensure audio device is working: `aplay -l` (Linux) or `afplay` (Mac)
- Try different WAV encodings (8-bit, 16-bit mono/stereo)

### Sound Distorts at High Volume
- Reduce volume levels in SoundManager defaults
- Re-export audio files with lower peak levels

## Integration Points in Code

### GameBoard.java
```java
soundManager.playSFX("ballHit");        // Line 92, 94, 96
soundManager.playSFX("boxHit");         // Line 109
soundManager.playSFX("lose");           // Line 156
soundManager.playSFX("win");            // Line 137
soundManager.playBackgroundMusic();     // Line 312
soundManager.pauseBackgroundMusic();    // Line 178
soundManager.resumeBackgroundMusic();   // Line 185
```

### MainMenu.java
```java
soundManager.playSFX("menuClick");      // Line 28
```

## Future Enhancements
- [ ] Settings menu for volume control
- [ ] Toggle sound on/off
- [ ] Different music tracks for different levels
- [ ] Victory/defeat music variations
- [ ] Voice announcements ("Game Over", "You Win")
- [ ] Background ambience (subtle hum, minimal clicking)

---

**Created**: November 13, 2025  
**Status**: MVP Complete - All sound hooks integrated  
**Next Steps**: Generate/acquire WAV files and test
