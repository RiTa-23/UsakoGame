package com.example.usakogame;

import javax.sound.sampled.*;

public class SoundManager {

    public static void playJump() {
        // Rising tone: 300Hz -> 600Hz, 100ms
        playTone(300, 600, 100, 0.5); 
    }

    public static void playScore() {
        // High ping: 1200Hz -> 1800Hz, 80ms
        playTone(1200, 1800, 80, 0.4); 
    }

    public static void playGameOver() {
        // Low noise/slide, 400ms
        playNoise(400); 
    }

    // Synthesize a tone with frequency slide (16-bit)
    private static void playTone(double startHz, double endHz, int ms, double volume) {
        new Thread(() -> {
            try {
                float sampleRate = 44100;
                int numSamples = (int)(sampleRate * ms / 1000);
                byte[] buf = new byte[numSamples * 2]; // 16-bit needs 2 bytes per sample
                
                double phase = 0;
                for (int i=0; i<numSamples; i++) {
                    double progress = i / (double)numSamples;
                    double currentFreq = startHz + (endHz - startHz) * progress;
                    
                    double pitch = currentFreq / sampleRate;
                    phase += pitch;
                    
                    double angle = 2.0 * Math.PI * phase;
                    double value = Math.sin(angle);
                    
                    // Simple decay
                    double decay = (1.0 - progress);
                    
                    // Scale to 16-bit range (max 32767)
                    short val = (short)(value * volume * 20000 * decay); 
                    
                    // Little Endian
                    buf[2*i] = (byte)(val & 0xFF);
                    buf[2*i+1] = (byte)((val >> 8) & 0xFF);
                }
                
                playRaw(buf, sampleRate);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    // Synthesize noise (16-bit)
    private static void playNoise(int ms) {
        new Thread(() -> {
            try {
                float sampleRate = 44100;
                int numSamples = (int)(sampleRate * ms / 1000);
                byte[] buf = new byte[numSamples * 2];
                
                java.util.Random r = new java.util.Random();
                
                for (int i=0; i<numSamples; i++) {
                     // White noise: -1.0 to 1.0
                     double value = (r.nextDouble() * 2.0) - 1.0;
                     
                     // Decay
                     double progress = (double)i / numSamples;
                     double decay = 1.0 - progress;
                     
                     short val = (short)(value * 0.2 * 20000 * decay);
                     
                     buf[2*i] = (byte)(val & 0xFF);
                     buf[2*i+1] = (byte)((val >> 8) & 0xFF);
                }

                playRaw(buf, sampleRate);
            } catch (Exception e) {
                 e.printStackTrace();
            }
        }).start();
    }
    
    private static void playRaw(byte[] data, float sampleRate) throws LineUnavailableException {
        // 16-bit PCM, Mono, Signed, Little Endian
        AudioFormat af = new AudioFormat(sampleRate, 16, 1, true, false);
        SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
        sdl.open(af);
        sdl.start();
        sdl.write(data, 0, data.length);
        sdl.drain();
        sdl.close();
    }
}
