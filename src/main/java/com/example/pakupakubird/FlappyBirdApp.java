package com.example.pakupakubird;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class FlappyBirdApp extends Application {

    private static final int WINDOW_WIDTH = 600;
    private static final int WINDOW_HEIGHT = 600;

    private Canvas canvas;
    private GraphicsContext gc;
    private AnimationTimer timer;
    private BorderPane root;

    // Game Mode Management
    private enum GameState {
        TITLE, FLAPPY, RUN
    }
    private GameState currentState = GameState.TITLE;

    private FlappyBirdGame flappyGame;
    private RunnerGame runnerGame;

    @Override
    public void start(Stage stage) {
        root = new BorderPane();
        
        // Setup Menu
        MenuBar menuBar = createMenuBar();
        root.setTop(menuBar);

        // Setup Canvas
        canvas = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        root.setCenter(canvas);

        // Operations
        flappyGame = new FlappyBirdGame();
        runnerGame = new RunnerGame();

        Scene scene = new Scene(root);
        
        // Input Handling
        scene.setOnKeyPressed(event -> {
            if (currentState == GameState.FLAPPY) flappyGame.handleKeyPress(event.getCode());
            else if (currentState == GameState.RUN) runnerGame.handleKeyPress(event.getCode());
        });
        
        scene.setOnKeyReleased(event -> {
            if (currentState == GameState.RUN) runnerGame.handleKeyRelease(event.getCode());
        });
        
        canvas.setOnMouseClicked(event -> {
             if (currentState == GameState.FLAPPY) flappyGame.handleInput();
             else if (currentState == GameState.RUN) runnerGame.handleInput();
        });

        stage.setTitle("UsakoGame");
        stage.setScene(scene);
        stage.show();

        // Game Loop
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                render();
                update();
            }
        };
        timer.start();
        
        showTitleScreen();
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("ゲーム");
        MenuItem titleItem = new MenuItem("タイトルに戻る");
        titleItem.setOnAction(e -> showTitleScreen());
        MenuItem exitItem = new MenuItem("終了");
        exitItem.setOnAction(e -> Platform.exit());
        fileMenu.getItems().addAll(titleItem, exitItem);
        
        Menu helpMenu = new Menu("ヘルプ");
        MenuItem aboutItem = new MenuItem("遊び方");
        aboutItem.setOnAction(e -> showHelp());
        helpMenu.getItems().add(aboutItem);
        
        menuBar.getMenus().addAll(fileMenu, helpMenu);
        return menuBar;
    }

    private void showTitleScreen() {
        currentState = GameState.TITLE;
        
        VBox menuBox = new VBox(20);
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8); -fx-padding: 50;");
        
        Label titleLabel = new Label("ゲーム選択");
        titleLabel.setFont(Font.font("Arial", 30));
        
        Button flappyBtn = new Button("Flappy Usako");
        flappyBtn.setStyle("-fx-font-size: 20px; -fx-min-width: 200px;");
        flappyBtn.setOnAction(e -> startFlappyBird());
        
        Button runBtn = new Button("Run Usako");
        runBtn.setStyle("-fx-font-size: 20px; -fx-min-width: 200px;");
        runBtn.setOnAction(e -> startRunnerGame());
        
        menuBox.getChildren().addAll(titleLabel, flappyBtn, runBtn);
        root.setCenter(menuBox); 
    }

    private void startFlappyBird() {
        currentState = GameState.FLAPPY;
        root.setCenter(canvas); 
        canvas.requestFocus();
        flappyGame.resetGame();
    }

    private void startRunnerGame() {
        currentState = GameState.RUN;
        root.setCenter(canvas);
        canvas.requestFocus();
        runnerGame.resetGame();
    }

    private void update() {
        if (root.getCenter() != canvas) return; 

        if (currentState == GameState.FLAPPY) {
            flappyGame.update();
        } else if (currentState == GameState.RUN) {
            runnerGame.update();
        }
    }

    private void render() {
        if (root.getCenter() != canvas) return;

        // Clear
        gc.clearRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        if (currentState == GameState.FLAPPY) {
            flappyGame.render(gc);
        } else if (currentState == GameState.RUN) {
            runnerGame.render(gc);
        }
    }

    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("遊び方");
        alert.setHeaderText("操作方法");
        alert.setContentText("Flappy Usako: スペース/クリック/上矢印でジャンプ\nRun Usako: 上矢印=ジャンプ, 下矢印=しゃがむ");
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch();
    }

    // ==========================================
    // FLAPPY BIRD IMPLEMENTATION
    // ==========================================
    class FlappyBirdGame {
        private static final double GRAVITY = 0.6;
        private static final double JUMP_STRENGTH = -10;
        private static final double PIPE_SPEED = 3;
        private static final double PIPE_WIDTH = 60;
        private static final double PIPE_GAP = 230; 
        private static final int SPAWN_INTERVAL = 110;

        boolean isRunning = false;
        boolean isGameOver = false;
        int score = 0;
        int ticks = 0;
        
        double birdY;
        double birdVelocity = 0;
        final double birdX = 100;
        double birdDisplayWidth = 40;
        double birdDisplayHeight = 40;
        
        List<Pipe> pipes = new ArrayList<>();
        Random random = new Random();
        
        Image birdNormal, birdJump;

        FlappyBirdGame() {
            loadAssets();
        }

        void loadAssets() {
            try {
                birdNormal = new Image(getClass().getResourceAsStream("usako_normal.png"));
                birdJump = new Image(getClass().getResourceAsStream("usako_jump.png"));
                
                if (birdNormal != null) {
                    double ratio = birdNormal.getWidth() / birdNormal.getHeight();
                    birdDisplayWidth = 40;
                    birdDisplayHeight = 40 / ratio;
                }
            } catch (Exception e) {
                System.err.println("FlappyAssets Error: " + e.getMessage());
            }
        }

        void resetGame() {
            birdY = WINDOW_HEIGHT / 2.0;
            birdVelocity = 0;
            score = 0;
            ticks = 0;
            pipes.clear();
            isRunning = false;
            isGameOver = false;
        }

        void handleKeyPress(KeyCode code) {
            if (code == KeyCode.SPACE || code == KeyCode.UP) {
                handleInput();
            }
        }

        void handleInput() {
            if (!isRunning && !isGameOver) {
                isRunning = true;
                birdVelocity = JUMP_STRENGTH;
            } else if (isRunning) {
                birdVelocity = JUMP_STRENGTH;
            } else if (isGameOver) {
                resetGame();
            }
        }

        void update() {
             if (!isRunning && !isGameOver) {
                 birdY = (WINDOW_HEIGHT / 2.0) + Math.sin(System.currentTimeMillis() / 300.0) * 10;
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
                 }
                 if (p.x + PIPE_WIDTH < -10) iter.remove();
                 if (checkCollision(p)) gameOver();
             }

             if (birdY < 0 || birdY + birdDisplayHeight > WINDOW_HEIGHT) gameOver();
        }

        boolean checkCollision(Pipe p) {
            double bx = birdX + 2;
            double by = birdY + 2;
            double bw = birdDisplayWidth - 4;
            double bh = birdDisplayHeight - 4;
            if (bx < p.x + PIPE_WIDTH && bx + bw > p.x && by < p.topHeight && by + bh > 0) return true;
            if (bx < p.x + PIPE_WIDTH && bx + bw > p.x && by < WINDOW_HEIGHT && by + bh > p.topHeight + PIPE_GAP) return true;
            return false;
        }

        void spawnPipe() {
            double minHeight = 50;
            double maxHeight = WINDOW_HEIGHT - PIPE_GAP - minHeight;
            double h = minHeight + random.nextDouble() * (maxHeight - minHeight);
            pipes.add(new Pipe(WINDOW_WIDTH, h));
        }

        void gameOver() {
            isGameOver = true;
            isRunning = false;
        }

        void render(GraphicsContext gc) {
            gc.setFill(Color.SKYBLUE);
            gc.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

            for (Pipe p : pipes) {
                gc.setFill(Color.web("#74BF2E"));
                gc.fillRect(p.x, 0, PIPE_WIDTH, p.topHeight);
                gc.fillRect(p.x, p.topHeight + PIPE_GAP, PIPE_WIDTH, WINDOW_HEIGHT - (p.topHeight + PIPE_GAP));
                gc.setStroke(Color.BLACK);
                gc.setLineWidth(2);
                gc.strokeRect(p.x, 0, PIPE_WIDTH, p.topHeight);
                gc.strokeRect(p.x, p.topHeight + PIPE_GAP, PIPE_WIDTH, WINDOW_HEIGHT - (p.topHeight + PIPE_GAP));
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

            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("Arial", 24));
            gc.fillText("スコア: " + score, 20, 40);

            if (!isRunning && !isGameOver) {
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Arial", 30));
                gc.fillText("スペースか上矢印でスタート", 150, 350);
            }
            if (isGameOver) {
                gc.setFill(Color.RED);
                gc.setFont(Font.font("Arial", 50));
                gc.fillText("ゲームオーバー", 130, 300);
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Arial", 24));
                gc.fillText("スコア: " + score, 240, 350);
                gc.fillText("スペースでリスタート", 180, 390);
            }
        }
    }
    
    class Pipe {
        double x, topHeight;
        boolean scored = false;
        Pipe(double x, double h) { this.x = x; this.topHeight = h; }
    }

    // ==========================================
    // RUNNER GAME IMPLEMENTATION
    // ==========================================
    class RunnerGame {
        Image[] runAnim = new Image[6];
        Image[] squatAnim = new Image[5];
        Image[] jumpAnim = new Image[6];
        
        double playerX = 80; // Fixed X position
        double playerY;      // Current Y position (represented as Feet Y)
        double velocityY = 0;
        double gravity = 0.8;
        double jumpForce = -15;
        double groundY = 500; // Floor Level Y
        
        // Dynamic sizes (Display)
        double standDisplayW = 60, standDisplayH = 90;
        double squatDisplayW = 50, squatDisplayH = 90; 
        // jumpDisplay variables removed to use dynamic calculation per frame
        
        // Scale helper
        double globalScale = 1.0;
        double jumpScaleFactor = 1.2; // Adjustable jump size multiplier
        double squatHitboxH = 55;

        boolean isRunning = false;
        boolean isGameOver = false;
        boolean isCrouching = false;
        
        int score = 0;
        int tick = 0;
        
        List<RunnerObstacle> obstacles = new ArrayList<>();
        Random random = new Random();
        double obsSpeed = 6;
        int spawnTimer = 0;

        RunnerGame() {
            loadAssets();
        }

        void loadAssets() {
            try {
                for (int i=0; i<6; i++) {
                    runAnim[i] = new Image(getClass().getResourceAsStream("run" + (i+1) + ".png"));
                    if (i < 5) {
                        squatAnim[i] = new Image(getClass().getResourceAsStream("squat" + (i+1) + ".png"));
                    }
                    jumpAnim[i] = new Image(getClass().getResourceAsStream("jump" + (i+1) + ".png"));
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

        void resetGame() {
            obstacles.clear();
            playerY = groundY;
            velocityY = 0;
            score = 0;
            tick = 0;
            obsSpeed = 6;
            isRunning = false;
            isGameOver = false;
            isCrouching = false;
        }

        void handleKeyPress(KeyCode code) {
            if (isGameOver && code == KeyCode.UP) {
                resetGame();
                isRunning = true;
                return;
            }
            if (!isRunning) {
                 if (code == KeyCode.UP || code == KeyCode.DOWN) isRunning = true;
            }
            
            if (isRunning) {
                if (code == KeyCode.UP && Math.abs(playerY - groundY) < 1) { 
                    velocityY = jumpForce;
                }
                if (code == KeyCode.DOWN) {
                    isCrouching = true;
                    if (playerY < groundY) velocityY += 5;
                }
            }
        }
        
        void handleKeyRelease(KeyCode code) {
            if (code == KeyCode.DOWN) {
                isCrouching = false;
            }
        }

        void handleInput() {
            if (isGameOver) { resetGame(); isRunning = true; return;}
            if (!isRunning) isRunning = true;
            if (Math.abs(playerY - groundY) < 1) velocityY = jumpForce;
        }

        void update() {
            if (!isRunning || isGameOver) return;
            
            tick++;
            
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
                }
            }
            
            if (tick % 10 == 0) score++;
            if (tick % 1000 == 0) obsSpeed += 0.5;
        }

        void spawnObstacle() {
            boolean isSky = random.nextDouble() > 0.6; 
            
            double ox = WINDOW_WIDTH;
            double oy;
            double ow = 50; 
            double oh = 60; 
            
            if (isSky) {
                oy = groundY - 115; 
                oh = 45;
                ow = 45; 
            } else {
                oy = groundY - 60;
                oh = 60;
                ow = 50;
            }
            obstacles.add(new RunnerObstacle(ox, oy, ow, oh, isSky));
        }

        boolean checkCollision(RunnerObstacle obs) {
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

        void render(GraphicsContext gc) {
            gc.setFill(Color.WHITE);
            gc.fillRect(0,0, WINDOW_WIDTH, WINDOW_HEIGHT);
            
            // Draw Ground
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);
            gc.strokeLine(0, groundY, WINDOW_WIDTH, groundY);
            
            // Obstacles
            for (RunnerObstacle obs : obstacles) {
                if (obs.isSky) {
                    gc.setFill(Color.ORANGE); // Bird/Missile
                    // Draw somewhat bird-like?
                    gc.fillOval(obs.x, obs.y, obs.w, obs.h);
                } else {
                    gc.setFill(Color.web("#74BF2E")); // Cactus/Pipe
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
            gc.setFont(Font.font("Arial", 20));
            gc.fillText("スコア: " + score, WINDOW_WIDTH - 150, 40);
            
            if (!isRunning && !isGameOver) {
                 gc.setFont(Font.font("Arial", 40));
                 gc.fillText("上矢印でスタート", 130, 200);
            }
            if (isGameOver) {
                gc.setFill(Color.RED);
                gc.setFont(Font.font("Arial", 50));
                gc.fillText("ゲームオーバー", 130, 250);
                gc.setFont(Font.font("Arial", 20));
                gc.fillText("上矢印でリスタート", 200, 300);
                gc.fillText("ESC/Menuで終了", 210, 330);
            }
        }
        
        Image getCurrentSprite() {
            // Jump
            if (Math.abs(playerY - groundY) > 5) { // In air
                // Map velocityY (-15 to ~15) to frames (0 to 5) to play once
                // jumpForce is -15. Expected landing velocity approx +15.
                double maxVy = -jumpForce; 
                double v = velocityY;
                
                // Normalize v from [-15, 15] to [0.0, 1.0]
                double progress = (v - jumpForce) / (maxVy - jumpForce);
                
                int frame = (int)(progress * 6);
                // Clamp to valid range (0-5)
                if (frame < 0) frame = 0;
                if (frame > 5) frame = 5;
                
                if (jumpAnim[frame] != null) return jumpAnim[frame];
            }
            // Squat
            if (isCrouching) {
                // Animation
                int frame = (tick / 5) % 5;
                if (squatAnim[frame] != null) return squatAnim[frame];
            }
            // Run (6 frames)
            int frame = (tick / 5) % 6;
            if (runAnim[frame] != null) return runAnim[frame];
            
            return null; // Fallback
        }
    }
    
    class RunnerObstacle {
        double x, y, w, h;
        boolean isSky;
        RunnerObstacle(double x, double y, double w, double h, boolean sky) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.isSky = sky;
        }
    }
}
