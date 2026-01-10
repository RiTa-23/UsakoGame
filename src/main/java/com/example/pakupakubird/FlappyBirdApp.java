package com.example.pakupakubird;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class FlappyBirdApp extends Application {

    private static final int WINDOW_WIDTH = 600;
    private static final int WINDOW_HEIGHT = 600;
    private static final double GRAVITY = 0.6;
    private static final double JUMP_STRENGTH = -10;
    private static final double PIPE_SPEED = 3;
    private static final double PIPE_WIDTH = 60;
    private static final double PIPE_GAP = 230;
    private static final int SPAWN_INTERVAL = 110;

    private Canvas canvas;
    private GraphicsContext gc;
    private AnimationTimer timer;

    // Game State
    private boolean isGameRunning = false;
    private boolean isGameOver = false;
    private int score = 0;
    private int ticks = 0;

    // Physics
    private double birdY;
    private double birdVelocity = 0;
    private final double birdX = 100;
    private double birdDisplayWidth = 40;
    private double birdDisplayHeight = 40;

    // Objects
    private List<Pipe> pipes = new ArrayList<>();
    private Random random = new Random();

    // Assets
    private Image birdNormalImage;
    private Image birdJumpImage;
    // pipeImage removed

    // UI
    // Removed TextField and Button as requested

    @Override
    public void start(Stage stage) {
        // Layout
        BorderPane root = new BorderPane();

        // 1. MenuBar (Top) - Keeping menu for now as it's useful
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());
        fileMenu.getItems().add(exitItem);

        Menu gameMenu = new Menu("Game");
        MenuItem restartItem = new MenuItem("Restart");
        restartItem.setOnAction(e -> resetGame());
        gameMenu.getItems().add(restartItem);

        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showHelp());
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, gameMenu, helpMenu);
        root.setTop(menuBar);

        // 2. Canvas (Center)
        canvas = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        root.setCenter(canvas);

        loadAssets();

        Scene scene = new Scene(root);

        // Event Handling
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                handleInput();
            }
        });
        
        canvas.setOnMouseClicked(event -> handleInput());

        stage.setTitle("Flappy Bird Enhanced");
        stage.setScene(scene);
        stage.show();
        
        // Start loop immediately for rendering (e.g. title screen)
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
                render();
            }
        };
        timer.start();

        // Initialize state (bird center)
        resetGameStates();
    }
    
    private void handleInput() {
        if (!isGameRunning && !isGameOver) {
            startGame();
        } else if (isGameRunning) {
            jump();
        } else if (isGameOver) {
            resetGame();
        }
    }

    private void loadAssets() {
        try {
            birdNormalImage = new Image(getClass().getResourceAsStream("usako_normal.png"));
            birdJumpImage = new Image(getClass().getResourceAsStream("usako_jump.png"));
            // pipeImage removed

            if (birdNormalImage != null) {
                double ratio = birdNormalImage.getWidth() / birdNormalImage.getHeight();
                birdDisplayWidth = 40;
                birdDisplayHeight = 40 / ratio;
            }
        } catch (Exception e) {
            System.err.println("Could not load images: " + e.getMessage());
        }
    }

    private void startGame() {
        isGameRunning = true;
        isGameOver = false;
        // Bird might drop if we don't jump immediately, but handleInput usually means user pressed action
        jump();
    }

    private void resetGameStates() {
        birdY = canvas.getHeight() / 2;
        birdVelocity = 0;
        score = 0;
        ticks = 0;
        pipes.clear();
        isGameOver = false;
        isGameRunning = false;
    }
    
    private void resetGame() {
        resetGameStates();
        // Don't auto-start, wait for input again? Or auto-start?
        // User said "Space or click to start".
        // If "reset", we probably go back to ready state.
        isGameRunning = false;
        isGameOver = false;
    }

    private void jump() {
        birdVelocity = JUMP_STRENGTH;
    }

    private void update() {
        // If not running, maybe just hover bird?
        if (!isGameRunning && !isGameOver) {
             // Hover effect
             birdY = (canvas.getHeight() / 2) + Math.sin(System.currentTimeMillis() / 300.0) * 10;
             return;
        }

        if (isGameOver) return; // Stop updates on game over

        ticks++;
        birdVelocity += GRAVITY;
        birdY += birdVelocity;

        if (ticks % SPAWN_INTERVAL == 0) {
            spawnPipe();
        }

        Iterator<Pipe> iter = pipes.iterator();
        while (iter.hasNext()) {
            Pipe pipe = iter.next();
            pipe.x -= PIPE_SPEED;

            if (!pipe.scored && pipe.x + PIPE_WIDTH < birdX) {
                score++;
                pipe.scored = true;
            }

            if (pipe.x + PIPE_WIDTH < -10) {
                iter.remove();
            }

            if (checkCollision(pipe)) {
                gameOver();
            }
        }

        if (birdY < 0 || birdY + birdDisplayHeight > canvas.getHeight()) {
            gameOver();
        }
    }
    
    private boolean checkCollision(Pipe pipe) {
        double bx = birdX + 2;
        double by = birdY + 2;
        double bw = birdDisplayWidth - 4;
        double bh = birdDisplayHeight - 4;
        
        if (bx < pipe.x + PIPE_WIDTH && bx + bw > pipe.x &&
            by < pipe.topHeight && by + bh > 0) {
            return true;
        }
        
        double bottomPipeY = pipe.topHeight + PIPE_GAP;
        if (bx < pipe.x + PIPE_WIDTH && bx + bw > pipe.x &&
            by < canvas.getHeight() && by + bh > bottomPipeY) { 
            return true;
        }
        
        return false;
    }

    private void spawnPipe() {
        double minHeight = 50;
        double maxHeight = canvas.getHeight() - PIPE_GAP - minHeight;
        double height = minHeight + random.nextDouble() * (maxHeight - minHeight);
        pipes.add(new Pipe(WINDOW_WIDTH, height));
    }

    private void gameOver() {
        isGameOver = true;
        isGameRunning = false;
    }

    private void render() {
        gc.setFill(Color.SKYBLUE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (Pipe pipe : pipes) {
            // Draw Pipe Body
            gc.setFill(Color.web("#74BF2E")); // Classic pipe green
            gc.fillRect(pipe.x, 0, PIPE_WIDTH, pipe.topHeight);
            gc.fillRect(pipe.x, pipe.topHeight + PIPE_GAP, PIPE_WIDTH, canvas.getHeight() - (pipe.topHeight + PIPE_GAP));
            
            // Draw Pipe Borders
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);
            gc.strokeRect(pipe.x, 0, PIPE_WIDTH, pipe.topHeight);
            gc.strokeRect(pipe.x, pipe.topHeight + PIPE_GAP, PIPE_WIDTH, canvas.getHeight() - (pipe.topHeight + PIPE_GAP));
            
            // Optional: Draw Pipe Cap for detail
            double capHeight = 20;
            // Top pipe cap
            gc.fillRect(pipe.x - 2, pipe.topHeight - capHeight, PIPE_WIDTH + 4, capHeight);
            gc.strokeRect(pipe.x - 2, pipe.topHeight - capHeight, PIPE_WIDTH + 4, capHeight);
            
            // Bottom pipe cap
            gc.fillRect(pipe.x - 2, pipe.topHeight + PIPE_GAP, PIPE_WIDTH + 4, capHeight);
            gc.strokeRect(pipe.x - 2, pipe.topHeight + PIPE_GAP, PIPE_WIDTH + 4, capHeight);
        }

        Image currentBird = birdNormalImage;
        if (birdVelocity < 0 && birdJumpImage != null) {
            currentBird = birdJumpImage;
        } else if (birdNormalImage != null) {
            currentBird = birdNormalImage;
        }

        if (currentBird != null) {
            gc.drawImage(currentBird, birdX, birdY, birdDisplayWidth, birdDisplayHeight);
        } else {
            gc.setFill(Color.YELLOW);
            gc.fillRect(birdX, birdY, birdDisplayWidth, birdDisplayHeight);
        }
        
        // UI Layer
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", 24));
        gc.fillText("Score: " + score, 20, 40);

        if (!isGameRunning && !isGameOver) {
             gc.setFill(Color.WHITE);
             gc.setFont(Font.font("Arial", 30));
             gc.fillText("Press Space/Click to Start", WINDOW_WIDTH / 2.0 - 180, WINDOW_HEIGHT / 2.0 + 100);
        }

        if (isGameOver) {
            gc.setFill(Color.RED);
            gc.setFont(Font.font("Arial", 50));
            gc.fillText("GAME OVER", WINDOW_WIDTH / 2.0 - 150, WINDOW_HEIGHT / 2.0);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", 24));
            gc.fillText("Score: " + score, WINDOW_WIDTH / 2.0 - 50, WINDOW_HEIGHT / 2.0 + 50);
            gc.fillText("Press Space to Restart", WINDOW_WIDTH / 2.0 - 120, WINDOW_HEIGHT / 2.0 + 90);
        }
    }
    
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText("How to Play");
        alert.setContentText("Use SPACE or Click to jump.\nAvoid pixel pipes!");
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch();
    }

    private static class Pipe {
        double x;
        double topHeight;
        boolean scored = false;

        Pipe(double x, double topHeight) {
            this.x = x;
            this.topHeight = topHeight;
        }
    }
}
