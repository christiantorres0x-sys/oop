import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GenerateSounds {
    private static final int SAMPLE_RATE = 44100;
    private static final String SOUNDS_DIR = "sounds";

    public static void main(String[] args) {
        createSoundsDirectory();
        System.out.println("\nGenerating sound effects...");
        System.out.println("-".repeat(40));

        // Ball hit (soft pop) - 150ms at 800Hz
        generateToneAndSave("ball_hit.wav", 800, 0.15f, 0.3f);

        // Box hit (sharper click) - 100ms at 1200Hz
        generateToneAndSave("box_hit.wav", 1200, 0.10f, 0.3f);

        // Win sound (celebration) - 800ms (two ascending tones)
        generateWinSound();

        // Lose sound (failure tone) - 500ms at 200Hz
        generateToneAndSave("lose_sound.wav", 200, 0.5f, 0.3f);

        // Menu click - 100ms at 600Hz
        generateToneAndSave("menu_click.wav", 600, 0.08f, 0.3f);

        // Background music - 30 second arcade loop
        generateBackgroundMusic();

        System.out.println("-".repeat(40));
        System.out.println("\n✓ All sound files generated in " + SOUNDS_DIR + "/ directory!");
        System.out.println("\nNote: These are placeholder sounds for testing.");
        System.out.println("For better quality, replace with professional audio from:");
        System.out.println("  • Freesound.org (CC0 licensed)");
        System.out.println("  • OpenGameArt.org (game assets)");
        System.out.println("  • Bfxr.net (retro sound generator)");
    }

    private static void createSoundsDirectory() {
        File dir = new File(SOUNDS_DIR);
        if (!dir.exists()) {
            if (dir.mkdir()) {
                System.out.println("Created " + SOUNDS_DIR + "/ directory");
            }
        }
    }

    private static void generateToneAndSave(String filename, float frequency, float duration, float amplitude) {
        byte[] audioData = generateTone(frequency, duration, amplitude);
        saveWavFile(filename, audioData);
    }

    private static byte[] generateTone(float frequency, float duration, float amplitude) {
        int numSamples = (int) (SAMPLE_RATE * duration);
        ByteBuffer buffer = ByteBuffer.allocate(numSamples * 2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Fade out to avoid clicks
        int fadeLen = (int) (SAMPLE_RATE * Math.min(0.05f, duration * 0.33f));

        for (int i = 0; i < numSamples; i++) {
            double sample = amplitude * Math.sin(2 * Math.PI * frequency * i / SAMPLE_RATE);

            // Apply fade out
            if (i >= numSamples - fadeLen) {
                float fadeRatio = (float) (numSamples - i) / fadeLen;
                sample *= fadeRatio;
            }

            // Convert to 16-bit integer
            short intSample = (short) (sample * 32767);
            buffer.putShort(intSample);
        }

        return buffer.array();
    }

    private static void generateWinSound() {
        // Two ascending tones: C5 (523Hz) then E5 (659Hz)
        byte[] part1 = generateTone(523, 0.4f, 0.2f);
        byte[] part2 = generateTone(659, 0.4f, 0.2f);

        ByteBuffer buffer = ByteBuffer.allocate(part1.length + part2.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(part1);
        buffer.put(part2);

        saveWavFile("win_sound.wav", buffer.array());
    }

    private static void generateBackgroundMusic() {
        // Simple arcade loop: repeating C5 (523Hz) - E5 (659Hz) - G5 (784Hz)
        float noteDuration = 0.25f;
        float[] frequencies = {523f, 659f, 784f};

        ByteBuffer buffer = ByteBuffer.allocate((int) (SAMPLE_RATE * 30 * 2));
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int numRepeats = 16;  // ~30 seconds total
        for (int r = 0; r < numRepeats; r++) {
            for (float freq : frequencies) {
                byte[] tone = generateTone(freq, noteDuration, 0.15f);
                buffer.put(tone);
            }
        }

        byte[] audioData = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(audioData);

        // Apply fade out at the end
        int fadeLen = (int) (SAMPLE_RATE * 0.5f);
        for (int i = 0; i < fadeLen; i++) {
            int pos = audioData.length - (fadeLen - i) * 2;
            if (pos >= 0 && pos < audioData.length - 1) {
                float fadeRatio = (float) i / fadeLen;
                short sample = (short) (((audioData[pos + 1] & 0xFF) << 8) | (audioData[pos] & 0xFF));
                sample = (short) (sample * fadeRatio);
                audioData[pos] = (byte) (sample & 0xFF);
                audioData[pos + 1] = (byte) ((sample >> 8) & 0xFF);
            }
        }

        saveWavFile("bg_music.wav", audioData);
    }

    private static void saveWavFile(String filename, byte[] audioData) {
        try {
            // Create WAV header
            ByteBuffer header = ByteBuffer.allocate(44);
            header.order(ByteOrder.LITTLE_ENDIAN);

            int numSamples = audioData.length / 2;
            int fileSize = 36 + audioData.length;
            int byteRate = SAMPLE_RATE * 2;  // 16-bit mono
            int blockAlign = 2;

            // RIFF header
            header.put("RIFF".getBytes());
            header.putInt(fileSize);
            header.put("WAVE".getBytes());

            // fmt sub-chunk
            header.put("fmt ".getBytes());
            header.putInt(16);      // Subchunk1Size (16 for PCM)
            header.putShort((short) 1);  // AudioFormat (1 = PCM)
            header.putShort((short) 1);  // NumChannels (1 = mono)
            header.putInt(SAMPLE_RATE);  // SampleRate
            header.putInt(byteRate);     // ByteRate
            header.putShort((short) blockAlign);  // BlockAlign
            header.putShort((short) 16);  // BitsPerSample

            // data sub-chunk
            header.put("data".getBytes());
            header.putInt(audioData.length);

            // Write file
            String filepath = SOUNDS_DIR + File.separator + filename;
            java.nio.file.Files.write(
                new File(filepath).toPath(),
                concat(header.array(), audioData)
            );

            System.out.println("✓ Generated " + filename + " (" + String.format("%.2f", numSamples / (float) SAMPLE_RATE) + "s)");
        } catch (IOException e) {
            System.err.println("Error saving " + filename + ": " + e.getMessage());
        }
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
