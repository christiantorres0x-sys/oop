#!/usr/bin/env python3
"""
Generate simple WAV sound files for Brick Break game.
Requires: pip install numpy

This script creates placeholder audio files that work immediately.
For production, replace with actual audio files.
"""

import os
import wave
import struct
import math

# Directory for sounds
SOUNDS_DIR = "sounds"
SAMPLE_RATE = 44100

def create_sound_dir():
    """Create sounds directory if it doesn't exist."""
    if not os.path.exists(SOUNDS_DIR):
        os.makedirs(SOUNDS_DIR)
        print(f"Created {SOUNDS_DIR}/ directory")

def generate_sine_wave(frequency, duration, amplitude=0.3):
    """Generate a sine wave at given frequency."""
    num_samples = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(num_samples):
        sample = amplitude * math.sin(2 * math.pi * frequency * i / SAMPLE_RATE)
        samples.append(sample)
    return samples

def generate_noise(duration, amplitude=0.2):
    """Generate white noise."""
    import random
    num_samples = int(SAMPLE_RATE * duration)
    samples = [random.uniform(-amplitude, amplitude) for _ in range(num_samples)]
    return samples

def write_wav(filename, samples, sample_rate=SAMPLE_RATE):
    """Write samples to WAV file."""
    filepath = os.path.join(SOUNDS_DIR, filename)
    
    # Convert float samples to 16-bit integers
    int_samples = [int(s * 32767) for s in samples]
    
    with wave.open(filepath, 'w') as wav_file:
        wav_file.setnchannels(1)  # Mono
        wav_file.setsampwidth(2)   # 16-bit
        wav_file.setframerate(sample_rate)
        
        # Pack samples as 16-bit little-endian integers
        packed_data = b''.join(struct.pack('<h', s) for s in int_samples)
        wav_file.writeframesraw(packed_data)
    
    print(f"✓ Generated {filename} ({len(samples)/sample_rate:.2f}s)")

def main():
    create_sound_dir()
    
    print("\nGenerating sound effects...")
    print("-" * 40)
    
    # Ball hit (soft pop/ping) - 150ms
    samples = generate_sine_wave(800, 0.15)
    # Fade out to avoid clicks
    fade_len = int(SAMPLE_RATE * 0.05)
    for i in range(fade_len):
        samples[-(i+1)] *= (1 - i/fade_len)
    write_wav("ball_hit.wav", samples)
    
    # Box hit (sharper click) - 100ms
    samples = generate_sine_wave(1200, 0.10)
    fade_len = int(SAMPLE_RATE * 0.03)
    for i in range(fade_len):
        samples[-(i+1)] *= (1 - i/fade_len)
    write_wav("box_hit.wav", samples)
    
    # Win sound (celebratory chime) - 800ms
    # Two ascending tones
    samples = generate_sine_wave(523, 0.4)  # C5
    samples += generate_sine_wave(659, 0.4)  # E5
    fade_len = int(SAMPLE_RATE * 0.1)
    for i in range(fade_len):
        samples[-(i+1)] *= (1 - i/fade_len)
    write_wav("win_sound.wav", samples)
    
    # Lose sound (failure tone) - 500ms
    samples = generate_sine_wave(200, 0.5)  # Low tone
    fade_len = int(SAMPLE_RATE * 0.1)
    for i in range(fade_len):
        samples[-(i+1)] *= (1 - i/fade_len)
    write_wav("lose_sound.wav", samples)
    
    # Menu click (light click) - 100ms
    samples = generate_sine_wave(600, 0.08)
    fade_len = int(SAMPLE_RATE * 0.02)
    for i in range(fade_len):
        samples[-(i+1)] *= (1 - i/fade_len)
    write_wav("menu_click.wav", samples)
    
    # Background music (simple arcade loop) - 30 seconds
    # Repeating pattern: C5 (523Hz) - E5 (659Hz) - G5 (784Hz)
    music_samples = []
    note_duration = 0.25  # Quarter note
    for _ in range(16):  # Repeat 16 times for ~30 seconds
        for freq in [523, 659, 784]:  # C, E, G
            music_samples += generate_sine_wave(freq, note_duration, 0.2)
    
    fade_len = int(SAMPLE_RATE * 0.5)
    for i in range(fade_len):
        music_samples[-(i+1)] *= (1 - i/fade_len)
    write_wav("bg_music.wav", music_samples)
    
    print("-" * 40)
    print(f"\n✓ All sound files generated in {SOUNDS_DIR}/ directory!")
    print("\nNote: These are temporary placeholder sounds.")
    print("For better audio quality, replace them with professional sound files from:")
    print("  • Freesound.org (CC0/CC-BY licensed)")
    print("  • OpenGameArt.org (game-specific assets)")
    print("  • Bfxr.net (generate your own retro sounds)")

if __name__ == "__main__":
    main()
