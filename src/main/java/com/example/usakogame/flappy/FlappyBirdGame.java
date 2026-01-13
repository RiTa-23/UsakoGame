package com.example.usakogame.flappy;

import com.example.usakogame.UsakoGameApp;
import com.example.usakogame.manager.HighScoreManager;
import com.example.usakogame.manager.SoundManager;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class FlappyBirdGame {
    private static final double GRAVITY = 0.6;
    private static final double JUMP_STRENGTH = -10;
    private static final double PIPE_SPEED = 3;
    private static final double PIPE_WIDTH = 60;
    private static final double PIPE_GAP = 230; 
    private static final int SPAWN_INTERVAL = 110;

    private final UsakoGameApp app;
    
    private boolean isRunning = false;
    private boolean isGameOver = false;
    private int score = 0;
    private int highScore = 0;
    private int ticks = 0;
    
    private double birdY;
    private double birdVelocity = 0;
    private final double birdX = 100;
    private double birdDisplayWidth = 40;
    private double birdDisplayHeight = 40;
    
    private List<Pipe> pipes = new ArrayList<>();
    private Random random = new Random();
    
    public Image birdNormal, birdJump;

    public FlappyBirdGame(UsakoGameApp app) {
        this.app = app;
        loadAssets();
    }

    private void loadAssets() {
        try {
            birdNormal = new Image(getClass().getResourceAsStream("/com/example/usakogame/usako_normal.png"));
            birdJump = new Image(getClass().getResourceAsStream("/com/example/usakogame/usako_jump.png"));
            
            if (birdNormal != null) {
                double ratio = birdNormal.getWidth() / birdNormal.getHeight();
                birdDisplayWidth = 40;
                birdDisplayHeight = 40 / ratio;
            }
        } catch (Exception e) {
            System.err.println("FlappyAssets Error: " + e.getMessage());
        }
    }

    public void resetGame() {
        birdY = UsakoGameApp.WINDOW_HEIGHT / 2.0;
        birdVelocity = 0;
        score = 0;
        highScore = HighScoreManager.getHighScore("flappy");
        ticks = 0;
        pipes.clear();
        isRunning = false;
        isGameOver = false;
    }

    public void handleKeyPress(KeyCode code) {
        if (code == KeyCode.ESCAPE) {
             if (isGameOver || !isRunning) {
                 app.showTitleScreen();
                 return;
             }
        }
        if (code == KeyCode.SPACE || code == KeyCode.UP) {
            handleInput();
        }
    }

    public void handleInput() {
        if (!isRunning && !isGameOver) {
            isRunning = true;
            birdVelocity = JUMP_STRENGTH;
            SoundManager.playJump();
        } else if (isRunning) {
            birdVelocity = JUMP_STRENGTH;
            SoundManager.playJump();
        } else if (isGameOver) {
            resetGame();
        }
    }

    public void update() {
         if (!isRunning && !isGameOver) {
             birdY = (UsakoGameApp.WINDOW_HEIGHT / 2.0) + Math.sin(System.currentTimeMillis() / 300.0) * 10;
             return;
         }
         if (isGameOver) return;

         ticks++;
         birdVelocity += GRAVITY;
         birdY += birdVelocity;

         if (ticks % SPAWN_INTERVAL == 0) spawnPipe();

         Iterator<Pipe> iter = pipes.iterator();
         while (iter.hasNext()) {
             Pipe p = iter.next();
             p.x -= PIPE_SPEED;
             if (!p.scored && p.x + PIPE_WIDTH < birdX) {
                 score++;
                 p.scored = true;
                 SoundManager.playScore();
             }
             if (p.x + PIPE_WIDTH < -10) iter.remove();
             if (checkCollision(p)) gameOver();
         }

         if (birdY < 0 || birdY + birdDisplayHeight > UsakoGameApp.WINDOW_HEIGHT) gameOver();
    }

    private boolean checkCollision(Pipe p) {
        double bx = birdX + 2;
        double by = birdY + 2;
        double bw = birdDisplayWidth - 4;
        double bh = birdDisplayHeight - 4;
        if (bx < p.x + PIPE_WIDTH && bx + bw > p.x && by < p.topHeight && by + bh > 0) return true;
        if (bx < p.x + PIPE_WIDTH && bx + bw > p.x && by < UsakoGameApp.WINDOW_HEIGHT && by + bh > p.topHeight + PIPE_GAP) return true;
        return false;
    }

    private void spawnPipe() {
        double minHeight = 50;
        double maxHeight = UsakoGameApp.WINDOW_HEIGHT - PIPE_GAP - minHeight;
        double h = minHeight + random.nextDouble() * (maxHeight - minHeight);
        pipes.add(new Pipe(UsakoGameApp.WINDOW_WIDTH, h));
    }

    private void gameOver() {
        isGameOver = true;
        isRunning = false;
        SoundManager.playGameOver();
        // Show Overlay
        app.showGameOverOverlay("flappy", score);
    }

    public void render(GraphicsContext gc) {
        gc.setFill(Color.SKYBLUE);
        gc.fillRect(0, 0, UsakoGameApp.WINDOW_WIDTH, UsakoGameApp.WINDOW_HEIGHT);

        for (Pipe p : pipes) {
            gc.setFill(Color.web("#74BF2E"));
            gc.fillRect(p.x, 0, PIPE_WIDTH, p.topHeight);
            gc.fillRect(p.x, p.topHeight + PIPE_GAP, PIPE_WIDTH, UsakoGameApp.WINDOW_HEIGHT - (p.topHeight + PIPE_GAP));
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);
            gc.strokeRect(p.x, 0, PIPE_WIDTH, p.topHeight);
            gc.strokeRect(p.x, p.topHeight + PIPE_GAP, PIPE_WIDTH, UsakoGameApp.WINDOW_HEIGHT - (p.topHeight + PIPE_GAP));
             gc.fillRect(p.x - 2, p.topHeight - 20, PIPE_WIDTH + 4, 20);
             gc.strokeRect(p.x - 2, p.topHeight - 20, PIPE_WIDTH + 4, 20);
             gc.fillRect(p.x - 2, p.topHeight + PIPE_GAP, PIPE_WIDTH + 4, 20);
             gc.strokeRect(p.x - 2, p.topHeight + PIPE_GAP, PIPE_WIDTH + 4, 20);
        }

        Image currentBird = birdNormal;
        if (birdVelocity < 0 && birdJump != null) currentBird = birdJump;
        else if (birdNormal != null) currentBird = birdNormal;
        
        if (currentBird != null) gc.drawImage(currentBird, birdX, birdY, birdDisplayWidth, birdDisplayHeight);
        else { gc.setFill(Color.YELLOW); gc.fillRect(birdX, birdY, birdDisplayWidth, birdDisplayHeight); }

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 24));
        gc.fillText("Score: " + score, UsakoGameApp.WINDOW_WIDTH - 220, 50);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        gc.fillText("High Score: " + highScore, UsakoGameApp.WINDOW_WIDTH - 220, 80);

        if (!isRunning && !isGameOver) {
            gc.setFill(Color.WHITE);
            Font f = Font.font("Verdana", FontWeight.BOLD, 30);
            gc.setFont(f);
            String text = "スペース/上矢印でスタート";
            Text t = new Text(text); t.setFont(f);
            double w = t.getLayoutBounds().getWidth();
            gc.fillText(text, (UsakoGameApp.WINDOW_WIDTH - w) / 2, 300);
            
            Font fEsc = Font.font("Verdana", FontWeight.BOLD, 20);
            gc.setFont(fEsc);
            String tEsc = "ESCでタイトルへ";
            Text txtEsc = new Text(tEsc); txtEsc.setFont(fEsc);
            double wEsc = txtEsc.getLayoutBounds().getWidth();
            gc.fillText(tEsc, (UsakoGameApp.WINDOW_WIDTH - wEsc) / 2, 350);
        }
        // Overlay handled by App
    }
}
