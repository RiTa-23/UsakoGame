package com.example.usakogame.runner;

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

public class RunnerGame {
    public Image[] runAnim = new Image[6];
    public Image[] squatAnim = new Image[5];
    public Image[] jumpAnim = new Image[6];
    
    private final UsakoGameApp app;
    
    private double playerX = 80; // Fixed X position
    private double playerY;      // Current Y position (represented as Feet Y)
    private double velocityY = 0;
    private double gravity = 0.8;
    private double jumpForce = -15;
    private double groundY = 500; // Floor Level Y
    
    // Dynamic sizes (Display)
    private double standDisplayW = 60, standDisplayH = 90;
    private double squatDisplayW = 50, squatDisplayH = 90; 
    
    // Scale helper
    private double globalScale = 1.0;
    private double jumpScaleFactor = 1.2; // Adjustable jump size multiplier
    private double squatHitboxH = 55;

    public boolean isRunning = false;
    private boolean isGameOver = false;
    private boolean isCrouching = false;
    
    private int score = 0;
    private int highScore = 0;
    private int tick = 0;
    private double animTick = 0;
    
    private List<RunnerObstacle> obstacles = new ArrayList<>();
    private Random random = new Random();
    private double obsSpeed = 6;
    private int spawnTimer = 0;
    
    private String milestoneMsg = "";
    private int milestoneTimer = 0;

    public RunnerGame(UsakoGameApp app) {
        this.app = app;
        loadAssets();
    }

    private void loadAssets() {
        try {
            for (int i=0; i<6; i++) {
                runAnim[i] = new Image(getClass().getResourceAsStream("/com/example/usakogame/run" + (i+1) + ".png"));
                if (i < 5) {
                    squatAnim[i] = new Image(getClass().getResourceAsStream("/com/example/usakogame/squat" + (i+1) + ".png"));
                }
                jumpAnim[i] = new Image(getClass().getResourceAsStream("/com/example/usakogame/jump" + (i+1) + ".png"));
            }
            
            double scale = 1.0;
            if (runAnim[0] != null) {
                standDisplayH = 90;
                scale = standDisplayH / runAnim[0].getHeight();
                standDisplayW = runAnim[0].getWidth() * scale;
            }
            this.globalScale = scale; 
            if (squatAnim[0] != null) {
                double squatScale = scale * 1.2;
                squatDisplayH = squatAnim[0].getHeight() * squatScale;
                squatDisplayW = squatAnim[0].getWidth() * squatScale;
                squatHitboxH = Math.min(squatDisplayH, 60);
            }
        } catch(Exception e) {
            System.err.println("Runner Assets Error: " + e.getMessage());
        }
    }

    public void resetGame() {
        obstacles.clear();
        playerY = groundY;
        velocityY = 0;
        score = 0;

        highScore = HighScoreManager.getHighScore("runner");
        tick = 0;
        animTick = 0;
        obsSpeed = 6;
        isRunning = false;
        isGameOver = false;
        isCrouching = false;
        milestoneMsg = "";
        milestoneTimer = 0;
    }

    public void handleKeyPress(KeyCode code) {
        if (isGameOver) {
            if (code == KeyCode.UP) {
                resetGame();
                isRunning = true;
                return;
            }
            if (code == KeyCode.ESCAPE) {
                app.showTitleScreen();
                return;
            }
            // Do not allow other keys to fall through and change state
            return;
        }
        if (!isRunning) {
             if (code == KeyCode.UP) isRunning = true;
             if (code == KeyCode.ESCAPE) {
                 app.showTitleScreen();
                 return;
             }
        }
        
        if (isRunning) {
            if (code == KeyCode.UP && Math.abs(playerY - groundY) < 1) { 
                velocityY = jumpForce;
                SoundManager.playJump();
            }
            if (code == KeyCode.DOWN) {
                isCrouching = true;
                if (playerY < groundY) velocityY += 5;
            }
        }
    }
    
    public void handleKeyRelease(KeyCode code) {
        if (code == KeyCode.DOWN) {
            isCrouching = false;
        }
    }

    public void handleInput() {
        if (isGameOver) { resetGame(); isRunning = true; return;}
        if (!isRunning) isRunning = true;
        if (Math.abs(playerY - groundY) < 1) velocityY = jumpForce;
    }

    public void update() {
        if (!isRunning || isGameOver) return;
        
        tick++;
        
        // Animation speed based on running speed
        animTick += (obsSpeed / 6.0);
        
        velocityY += gravity;
        playerY += velocityY;
        
        if (playerY > groundY) {
            playerY = groundY;
            velocityY = 0;
        }
        
        spawnTimer++;
        if (spawnTimer > (1200 / obsSpeed) + random.nextInt(30)) { 
             spawnObstacle();
             spawnTimer = 0;
        }
        
        Iterator<RunnerObstacle> iter = obstacles.iterator();
        while(iter.hasNext()) {
            RunnerObstacle obs = iter.next();
            obs.x -= obsSpeed;
            if (obs.x < -100) iter.remove();
            if (checkCollision(obs)) {
                isGameOver = true;
                SoundManager.playGameOver();
                app.showGameOverOverlay("runner", score);
            }
        }
        

        if (tick % 10 == 0) {
             score++;
             if (score % 100 == 0) {
                 SoundManager.playScore();
                 milestoneMsg = score + " POINTS!";
                 milestoneTimer = 60; // Display for ~1 second (60 frames)
             }
        }
        if (milestoneTimer > 0) milestoneTimer--;
        
        if (tick % 300 == 0) obsSpeed += 0.5;
    }

    private void spawnObstacle() {
        boolean isSky = random.nextDouble() > 0.6; 
        
        double ox = UsakoGameApp.WINDOW_WIDTH;
        double oy;
        double ow = 50; 
        double oh = 60; 
        
        if (isSky) {
            double offset = 50 + random.nextInt(100);
            oy = groundY - offset; 
            oh = 45;
            ow = 45;  
        } else {
            oy = groundY - 60;
            oh = 60;
            ow = 50;
        }
        obstacles.add(new RunnerObstacle(ox, oy, ow, oh, isSky));
    }

    private boolean checkCollision(RunnerObstacle obs) {
        boolean inAir = Math.abs(playerY - groundY) > 5;
        double h, w;
        if (inAir) {
            // Dynamic Jump Size
            Image img = getCurrentSprite();
            if (img != null) {
                 h = img.getHeight() * globalScale * jumpScaleFactor;
                 w = img.getWidth() * globalScale * jumpScaleFactor;
            } else {
                 h = standDisplayH; w = standDisplayW;
            }
        } else if (isCrouching) {
            h = squatHitboxH; w = squatDisplayW;
        } else {
            h = standDisplayH; w = standDisplayW;
        }
        double px = playerX;
        double py = playerY - h; // Top-left
        
        // Allow slight leeway (hitbox reduction)
        double buf = 5;
        return px + buf < obs.x + obs.w && px + w - buf > obs.x &&
               py + buf < obs.y + obs.h && py + h - buf > obs.y;
    }

    public void render(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        gc.fillRect(0,0, UsakoGameApp.WINDOW_WIDTH, UsakoGameApp.WINDOW_HEIGHT);
        
        // Draw Ground
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeLine(0, groundY, UsakoGameApp.WINDOW_WIDTH, groundY);
        
        // Obstacles
        for (RunnerObstacle obs : obstacles) {
            if (obs.isSky) {
                gc.setFill(Color.WHITE); 
                gc.fillOval(obs.x, obs.y, obs.w, obs.h);
                gc.strokeOval(obs.x, obs.y, obs.w, obs.h);
            } else {
                gc.setFill(Color.WHITE);
                gc.fillRect(obs.x, obs.y, obs.w, obs.h);
                gc.strokeRect(obs.x, obs.y, obs.w, obs.h);
            }
        }
        
        // Player
        Image img = getCurrentSprite();
        boolean inAir = Math.abs(playerY - groundY) > 5;
        double h, w;
        if (inAir && img != null) {
             // Use image natural ratio scaled * jump modifier
             h = img.getHeight() * globalScale * jumpScaleFactor;
             w = img.getWidth() * globalScale * jumpScaleFactor;
        } else if (isCrouching && !inAir) {
             h = squatDisplayH; w = squatDisplayW;
        } else {
             h = standDisplayH; w = standDisplayW;
        }
        double py = playerY - h;
        
        if (img != null) {
            // Preserve aspect ratio of image?
            // Just draw it in the box
            gc.drawImage(img, playerX, py, w, h);
        } else {
            gc.setFill(Color.BLUE);
            gc.fillRect(playerX, py, w, h);
        }
        
        // UI
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
        gc.fillText("Score: " + score, UsakoGameApp.WINDOW_WIDTH - 220, 50);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        gc.fillText("High Score: " + highScore, UsakoGameApp.WINDOW_WIDTH - 220, 80);
        
        if (!isRunning && !isGameOver) {
             Font fStart = Font.font("Verdana", FontWeight.BOLD, 40);
             gc.setFont(fStart);
             String tStart = "上矢印でスタート";
             Text txtStart = new Text(tStart); txtStart.setFont(fStart);
             double wStart = txtStart.getLayoutBounds().getWidth();
             gc.fillText(tStart, (UsakoGameApp.WINDOW_WIDTH - wStart) / 2, 200);
             
             gc.setFill(Color.BLACK);
             Font fInst = Font.font("Verdana", FontWeight.BOLD, 20);
             gc.setFont(fInst);
             String tInst = "上矢印: ジャンプ / 下矢印: しゃがむ";
             Text txtInst = new Text(tInst); txtInst.setFont(fInst);
             double wInst = txtInst.getLayoutBounds().getWidth();
             gc.fillText(tInst, (UsakoGameApp.WINDOW_WIDTH - wInst) / 2, 250);
             
             String tEsc = "ESCでタイトルへ";
             Text txtEsc = new Text(tEsc); txtEsc.setFont(fInst);
             double wEsc = txtEsc.getLayoutBounds().getWidth();
             gc.fillText(tEsc, (UsakoGameApp.WINDOW_WIDTH - wEsc) / 2, 290);
        }
        
        // Milestone Text
        if (isRunning && !isGameOver && milestoneTimer > 0) {
            gc.setFill(Color.ORANGE);
            Font fMile = Font.font("Verdana", FontWeight.BOLD, 40);
            gc.setFont(fMile);
            
            Text tMile = new Text(milestoneMsg); tMile.setFont(fMile);
            double wMile = tMile.getLayoutBounds().getWidth();
            double x = (UsakoGameApp.WINDOW_WIDTH - wMile) / 2;
            double y = 150;
            
            gc.fillText(milestoneMsg, x, y);
        }
        
        // Overlay handled by App
    }
    
    private Image getCurrentSprite() {
        // Jump
        if (Math.abs(playerY - groundY) > 5) { // In air
            double maxVy = -jumpForce; 
            double v = velocityY;
            double progress = (v - jumpForce) / (maxVy - jumpForce);
            int frame = (int)(progress * 6);
            if (frame < 0) frame = 0;
            if (frame > 5) frame = 5;
            
            if (jumpAnim[frame] != null) return jumpAnim[frame];
        }
        // Squat
        if (isCrouching && !isGameOver) {
            int frame = ((int)animTick / 5) % 5;
            if (squatAnim[frame] != null) return squatAnim[frame];
        }
        // Run (6 frames)
        int frame = ((int)animTick / 5) % 6;
        if (runAnim[frame] != null) return runAnim[frame];
        
        return null;
    }
}
