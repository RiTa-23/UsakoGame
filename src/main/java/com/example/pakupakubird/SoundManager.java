package com.example.pakupakubird;

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

    // Synthesize a tone with frequency slide
    private static void playTone(double startHz, double endHz, int ms, double volume) {
        new Thread(() -> {
            try {
                float sampleRate = 44100;
                int numSamples = (int)(sampleRate * ms / 1000);
                byte[] buf = new byte[numSamples];
                
                double phase = 0;
                for (int i=0; i<numSamples; i++) {
                    double progress = i / (double)numSamples;
                    double currentFreq = startHz + (endHz - startHz) * progress;
                    
                    // Phase increment amount
                    double pitch = currentFreq / sampleRate;
                    phase += pitch;
                    
                    // Sine wave
                    double angle = 2.0 * Math.PI * phase;
                    double value = Math.sin(angle);
                    
                    // Square wave option (retro style)
                    // if (value > 0) value = 1; else value = -1; 
                    
                    // Apply volume and decay
                    double vol = volume * 100; // max ~127
                    double decay = (1.0 - progress);
                    
                    buf[i] = (byte)(value * vol * decay);
                }
                
                playRaw(buf, sampleRate);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    // Synthesize noise
    private static void playNoise(int ms) {
        new Thread(() -> {
            try {
                float sampleRate = 44100;
                int numSamples = (int)(sampleRate * ms / 1000);
                byte[] buf = new byte[numSamples];
                
                java.util.Random r = new java.util.Random();
                r.nextBytes(buf);
                
                for (int i=0; i<buf.length; i++) {
                     // Simple decay
                     double progress = (double)i / buf.length;
                     buf[i] = (byte)(buf[i] * 0.5 * (1.0 - progress));
                }

                playRaw(buf, sampleRate);
            } catch (Exception e) {
                 e.printStackTrace();
            }
        }).start();
    }
    
    private static void playRaw(byte[] data, float sampleRate) throws LineUnavailableException {
        AudioFormat af = new AudioFormat(sampleRate, 8, 1, true, false);
        SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
        sdl.open(af);
        sdl.start();
        sdl.write(data, 0, data.length);
        sdl.drain();
        sdl.close();
    }
}
