package com.example.pakupakubird;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FlappyBirdApp extends Application {

    private static final int WINDOW_WIDTH = 600;
    private static final int WINDOW_HEIGHT = 600; // slightly wider than typical phone to fit desktop
    private static final double GRAVITY = 0.6;
    private static final double JUMP_STRENGTH = -10;
    private static final double PIPE_SPEED = 3;
    private static final double PIPE_WIDTH = 50;
    private static final double PIPE_GAP = 200;
    private static final int SPAWN_INTERVAL = 100; // frames

    private Pane root;
    private Rectangle bird;
    private List<Rectangle> pipes = new ArrayList<>();
    private AnimationTimer timer;
    private Text scoreText;
    private Text gameOverText;

    private double birdVelocity = 0;
    private int score = 0;
    private int ticks = 0;
    private boolean isGameOver = false;
    private boolean isGameStarted = false;

    private Random random = new Random();

    @Override
    public void start(Stage stage) {
        root = new Pane();
        root.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);

        createContent();

        Scene scene = new Scene(root);
        
        // Input handling
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                handleJump();
            }
        });

        scene.setOnMouseClicked(event -> handleJump());

        stage.setTitle("Flappy Bird Clone");
        stage.setScene(scene);
        stage.show();
    }

    private void createContent() {
        // Background
        Rectangle bg = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT);
        bg.setFill(Color.LIGHTBLUE);
        root.getChildren().add(bg);

        // Bird
        bird = new Rectangle(30, 30, Color.ORANGE);
        bird.setTranslateX(100);
        bird.setTranslateY(WINDOW_HEIGHT / 2.0);
        root.getChildren().add(bird);

        // UI
        scoreText = new Text("Score: 0");
        scoreText.setFont(Font.font(24));
        scoreText.setTranslateX(20);
        scoreText.setTranslateY(40);
        root.getChildren().add(scoreText);

        gameOverText = new Text("Game Over\nPress Space to Restart");
        gameOverText.setFont(Font.font(40));
        gameOverText.setFill(Color.RED);
        gameOverText.setTranslateX(WINDOW_WIDTH / 2.0 - 200);
        gameOverText.setTranslateY(WINDOW_HEIGHT / 2.0);
        gameOverText.setVisible(false);
        root.getChildren().add(gameOverText);

        // Game Loop
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
            }
        };
        timer.start();
    }

    private void handleJump() {
        if (isGameOver) {
            restartGame();
            return;
        }

        if (!isGameStarted) {
            isGameStarted = true;
        }

        birdVelocity = JUMP_STRENGTH;
    }

    private void update() {
        if (!isGameStarted || isGameOver) return;

        ticks++;

        // Bird Physics
        birdVelocity += GRAVITY;
        bird.setTranslateY(bird.getTranslateY() + birdVelocity);

        // Pipe Spawning
        if (ticks % SPAWN_INTERVAL == 0) {
            spawnPipe();
        }

        // Move Pipes & Collision
        List<Rectangle> toRemove = new ArrayList<>();
        for (Rectangle pipe : pipes) {
            pipe.setTranslateX(pipe.getTranslateX() - PIPE_SPEED);

            if (pipe.getTranslateX() + PIPE_WIDTH < 0) {
                toRemove.add(pipe);
            }

            // Simple Collision Detection
            if (pipe.getBoundsInParent().intersects(bird.getBoundsInParent())) {
                gameOver();
            }
        }
        
        // Remove off-screen pipes
        pipes.removeAll(toRemove);
        root.getChildren().removeAll(toRemove);
        
        // Score update
        for (Rectangle pipe : pipes) {
             // Only check top pipe to avoid double counting per pair
             if (pipe.getTranslateY() == 0) {
                 // Check if pipe passed the bird
                 if (pipe.getTranslateX() + PIPE_WIDTH < bird.getTranslateX()) {
                     if (!"scored".equals(pipe.getUserData())) {
                         score++;
                         scoreText.setText("Score: " + score);
                         pipe.setUserData("scored");
                     }
                 }
             }
        }

        // Floor/Ceiling Collision
        if (bird.getTranslateY() < 0 || bird.getTranslateY() >= WINDOW_HEIGHT - 30) {
            gameOver();
        }
    }

    private void spawnPipe() {
        int minHeight = 50;
        int maxHeight = WINDOW_HEIGHT - (int)PIPE_GAP - minHeight;
        int height = minHeight + random.nextInt(maxHeight - minHeight);

        Rectangle topPipe = new Rectangle(PIPE_WIDTH, height);
        topPipe.setFill(Color.GREEN);
        topPipe.setTranslateX(WINDOW_WIDTH);
        topPipe.setTranslateY(0);

        Rectangle bottomPipe = new Rectangle(PIPE_WIDTH, WINDOW_HEIGHT - height - PIPE_GAP);
        bottomPipe.setFill(Color.GREEN);
        bottomPipe.setTranslateX(WINDOW_WIDTH);
        bottomPipe.setTranslateY(height + PIPE_GAP);

        pipes.add(topPipe);
        pipes.add(bottomPipe);

        root.getChildren().add(1, topPipe); // Add behind UI but in front of bg? 
        // Index 1 is after BG (0). Correct.
        root.getChildren().add(1, bottomPipe);
    }

    private void gameOver() {
        isGameOver = true;
        gameOverText.setVisible(true);
    }

    private void restartGame() {
        isGameOver = false;
        isGameStarted = false;
        score = 0;
        scoreText.setText("Score: 0");
        ticks = 0;
        birdVelocity = 0;
        
        root.getChildren().removeAll(pipes);
        pipes.clear();
        
        bird.setTranslateY(WINDOW_HEIGHT / 2.0);
        gameOverText.setVisible(false);
    }

    public static void main(String[] args) {
        launch();
    }
}
