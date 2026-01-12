package com.example.usakogame;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class HighScoreManager {
    private static final String DIR_PATH;
    private static final String FILE_PATH;
    private static final int MAX_RANKING = 5;
    private static Properties properties = new Properties();

    static {
        // Determine the OS-specific data directory
        String os = System.getProperty("os.name").toLowerCase();
        String baseDir;

        if (os.contains("win")) {
            // Windows: %APPDATA%\UsakoGame
            baseDir = System.getenv("APPDATA");
            if (baseDir == null) {
                baseDir = System.getProperty("user.home");
            }
        } else if (os.contains("mac")) {
            // Mac: ~/Library/Application Support/UsakoGame
            baseDir = System.getProperty("user.home") + "/Library/Application Support";
        } else {
            // Linux/Other: ~/.usakogame
            baseDir = System.getProperty("user.home");
        }

        DIR_PATH = baseDir + "/UsakoGame";
        FILE_PATH = DIR_PATH + "/scores.properties";

        load();
    }

    private static void load() {
        File dir = new File(DIR_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(FILE_PATH);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            } catch (IOException e) {
                System.err.println("Failed to load high scores: " + e.getMessage());
            }
        }
    }

    private static void save() {
        File dir = new File(DIR_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(FILE_PATH)) {
            properties.store(fos, "Usako Game High Scores");
        } catch (IOException e) {
            System.err.println("Failed to save high scores: " + e.getMessage());
        }
    }

    public static int getHighScore(String gameMode) {
        List<ScoreEntry> list = getTopScores(gameMode);
        if (list.isEmpty()) return 0;
        return list.get(0).score;
    }

    public static List<ScoreEntry> getTopScores(String gameMode) {
        List<ScoreEntry> list = new ArrayList<>();
        for (int i = 0; i < MAX_RANKING; i++) {
            String nameKey = gameMode + "." + i + ".name";
            String scoreKey = gameMode + "." + i + ".score";
            
            if (properties.containsKey(scoreKey)) {
                String name = properties.getProperty(nameKey, "NoName");
                int score = Integer.parseInt(properties.getProperty(scoreKey, "0"));
                list.add(new ScoreEntry(name, score));
            }
        }
        // Ensure sorted (though save logic keeps it sorted)
        list.sort((a, b) -> Integer.compare(b.score, a.score));
        return list;
    }

    public static void submitScore(String gameMode, String name, int score) {
        List<ScoreEntry> list = getTopScores(gameMode);
        list.add(new ScoreEntry(name, score));
        list.sort((a, b) -> Integer.compare(b.score, a.score));
        
        // Trim to MAX
        if (list.size() > MAX_RANKING) {
            list = list.subList(0, MAX_RANKING);
        }
        
        // Save back
        for (int i = 0; i < MAX_RANKING; i++) {
            String nameKey = gameMode + "." + i + ".name";
            String scoreKey = gameMode + "." + i + ".score";
            
            if (i < list.size()) {
                properties.setProperty(nameKey, list.get(i).name);
                properties.setProperty(scoreKey, String.valueOf(list.get(i).score));
            } else {
                // Clear extra slots if list shrank (unlikely but safe)
                properties.remove(nameKey);
                properties.remove(scoreKey);
            }
        }
        save();
    }
    
    // For compatibility with old setHighScore calls (assumes "Anonymous")
    public static void setHighScore(String gameMode, int score) {
        // We will not auto-submit anonymous high scores anymore if we want user input.
        // However, if we want to track 'current high' during game, we might need to check logic.
        // For now, this method simply ignores saving if it doesn't have a name, 
        // OR we can save as "Unknown".
        // Let's deprecate this side-effect. The Game Over screen will handle submission.
    }

    public static void clearAllData() {
        properties.clear();
        File file = new File(FILE_PATH);
        if (file.exists()) {
            file.delete();
        }
    }

    public static class ScoreEntry {
        public String name;
        public int score;

        public ScoreEntry(String name, int score) {
            this.name = name;
            this.score = score;
        }
    }
}
