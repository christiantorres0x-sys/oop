import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {
    private static final String SOUNDS_DIR = "sounds/";
    
    // Audio clips cache
    private Map<String, Clip> soundClips = new HashMap<>();
    private Clip backgroundMusicClip;
    private float masterVolume = 1.0f;
    private float sfxVolume = 0.7f;
    private float musicVolume = 0.3f;

    // Sound file names
    private static final String BG_MUSIC = "bg_music.wav";
    private static final String BALL_HIT = "ball_hit.wav";
    private static final String BOX_HIT = "box_hit.wav";
    private static final String WIN_SOUND = "win_sound.wav";
    private static final String LOSE_SOUND = "lose_sound.wav";
    private static final String MENU_CLICK = "menu_click.wav";

    public SoundManager() {
        loadAllSounds();
    }

    /**
     * Load all sound files into memory for quick playback
     */
    private void loadAllSounds() {
        loadSound("ballHit", BALL_HIT);
        loadSound("boxHit", BOX_HIT);
        loadSound("win", WIN_SOUND);
        loadSound("lose", LOSE_SOUND);
        loadSound("menuClick", MENU_CLICK);
        loadSound("bgMusic", BG_MUSIC);
    }

    /**
     * Load a single sound file and cache it
     */
    private void loadSound(String key, String filename) {
        try {
            File soundFile = new File(SOUNDS_DIR + filename);
            if (!soundFile.exists()) {
                System.out.println("Warning: Sound file not found: " + soundFile.getAbsolutePath());
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            soundClips.put(key, clip);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error loading sound: " + filename);
            e.printStackTrace();
        }
    }

    /**
     * Play a sound effect (non-looping)
     */
    public void playSFX(String soundKey) {
        Clip clip = soundClips.get(soundKey);
        if (clip != null) {
            // Stop if already playing and restart
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            setClipVolume(clip, sfxVolume * masterVolume);
            clip.start();
        }
    }

    /**
     * Play background music (looping)
     */
    public void playBackgroundMusic() {
        Clip clip = soundClips.get("bgMusic");
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            setClipVolume(clip, musicVolume * masterVolume);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundMusicClip = clip;
        }
    }

    /**
     * Stop background music
     */
    public void stopBackgroundMusic() {
        if (backgroundMusicClip != null && backgroundMusicClip.isRunning()) {
            backgroundMusicClip.stop();
        }
    }

    /**
     * Pause background music (can be resumed)
     */
    public void pauseBackgroundMusic() {
        if (backgroundMusicClip != null && backgroundMusicClip.isRunning()) {
            backgroundMusicClip.stop();
        }
    }

    /**
     * Resume background music from pause
     */
    public void resumeBackgroundMusic() {
        if (backgroundMusicClip != null && !backgroundMusicClip.isRunning()) {
            backgroundMusicClip.start();
        }
    }

    /**
     * Set volume for a specific clip (0.0 to 1.0)
     */
    private void setClipVolume(Clip clip, float volume) {
        volume = Math.max(0f, Math.min(1f, volume)); // Clamp to [0, 1]
        try {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
            gainControl.setValue(dB);
        } catch (IllegalArgumentException e) {
            System.err.println("Volume control not supported for this audio format");
        }
    }

    /**
     * Set master volume (affects all sounds)
     */
    public void setMasterVolume(float volume) {
        masterVolume = Math.max(0f, Math.min(1f, volume));
    }

    /**
     * Set SFX volume
     */
    public void setSFXVolume(float volume) {
        sfxVolume = Math.max(0f, Math.min(1f, volume));
    }

    /**
     * Set music volume
     */
    public void setMusicVolume(float volume) {
        musicVolume = Math.max(0f, Math.min(1f, volume));
        if (backgroundMusicClip != null) {
            setClipVolume(backgroundMusicClip, musicVolume * masterVolume);
        }
    }

    /**
     * Get master volume
     */
    public float getMasterVolume() {
        return masterVolume;
    }

    /**
     * Clean up all audio resources (call on app exit)
     */
    public void cleanup() {
        stopBackgroundMusic();
        for (Clip clip : soundClips.values()) {
            if (clip.isOpen()) {
                clip.close();
            }
        }
        soundClips.clear();
    }
}
