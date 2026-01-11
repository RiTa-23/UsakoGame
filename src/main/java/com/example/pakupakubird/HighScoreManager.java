package com.example.pakupakubird;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class HighScoreManager {
    private static final String DIR_PATH = "usako_save";
    private static final String FILE_PATH = DIR_PATH + "/scores.properties";
    private static Properties properties = new Properties();

    static {
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
        String val = properties.getProperty(gameMode, "0");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static void setHighScore(String gameMode, int score) {
        int currentHigh = getHighScore(gameMode);
        if (score > currentHigh) {
            properties.setProperty(gameMode, String.valueOf(score));
            save();
        }
    }
}
