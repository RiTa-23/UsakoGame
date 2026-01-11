package com.example.usakogame;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
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
    private StackPane gameStack;
    private VBox overlayBox;
    private boolean isOverlayActive = false;

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
        // Setup Canvas and Overlay
        canvas = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        
        overlayBox = new VBox(15);
        overlayBox.setAlignment(Pos.CENTER);
        overlayBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-padding: 30;");
        overlayBox.setVisible(false);
        overlayBox.setMaxSize(400, 500);

        gameStack = new StackPane(canvas, overlayBox);
        root.setCenter(gameStack);

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
             if (isOverlayActive) return; // Ignore game clicks if overlay is on
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
        MenuItem infoItem = new MenuItem("うさこについて");
        infoItem.setOnAction(e -> showUsakoInfo());
        helpMenu.getItems().addAll(aboutItem, infoItem);
        
        menuBar.getMenus().addAll(fileMenu, helpMenu);
        return menuBar;
    }

    private void showTitleScreen() {
        currentState = GameState.TITLE;
        
        VBox menuBox = new VBox(20);
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-padding: 50;");
        
        // Title
        Label titleLabel = new Label("Usako Game");
        titleLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 50));
        
        // Subtitle
        Label subLabel = new Label("ゲームを選択");
        subLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
        
        // Flappy Button
        Button flappyBtn = new Button();
        flappyBtn.setStyle("-fx-pref-width: 300px; -fx-pref-height: 80px; -fx-padding: 0;");
        
        StackPane fPane = new StackPane();
        fPane.setPrefSize(300, 80);
        fPane.setPadding(new javafx.geometry.Insets(0, 20, 0, 20)); // Padding inside stackpane
        
        Label fLabel = new Label("Flappy Usako");
        fLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 24));
        // Allow clicks on label to pass through or just part of graphic
        
        ImageView fView = new ImageView();
        if (flappyGame.birdNormal != null) {
            fView.setImage(flappyGame.birdNormal);
            fView.setFitWidth(50);
            fView.setFitHeight(50);
            fView.setPreserveRatio(true);
            
            flappyBtn.setOnMouseEntered(e -> {
                if (flappyGame.birdJump != null) fView.setImage(flappyGame.birdJump);
            });
            flappyBtn.setOnMouseExited(e -> {
               if (flappyGame.birdNormal != null) fView.setImage(flappyGame.birdNormal);
            });
        }
        
        StackPane.setAlignment(fView, Pos.CENTER_LEFT);
        StackPane.setAlignment(fLabel, Pos.CENTER);
        fPane.getChildren().addAll(fView, fLabel);
        flappyBtn.setGraphic(fPane);
        
        flappyBtn.setOnAction(e -> startFlappyBird());
        
        // Run Button
        Button runBtn = new Button();
        runBtn.setStyle("-fx-pref-width: 300px; -fx-pref-height: 80px; -fx-padding: 0;");
        
        StackPane rPane = new StackPane();
        rPane.setPrefSize(300, 80);
        rPane.setPadding(new javafx.geometry.Insets(0, 20, 0, 20));
        
        Label rLabel = new Label("Usako Run!");
        rLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 24));
        
        ImageView rView = new ImageView();
        if (runnerGame.runAnim != null && runnerGame.runAnim[0] != null) {
            rView.setImage(runnerGame.runAnim[0]);
            rView.setFitWidth(50);
            rView.setFitHeight(50);
            rView.setPreserveRatio(true);
            
            // Animation for Run Button
            AnimationTimer runBtnTimer = new AnimationTimer() {
                private long lastUpdate = 0;
                private int frame = 0;
                @Override
                public void handle(long now) {
                     if (now - lastUpdate >= 100_000_000) { // 100ms
                         lastUpdate = now;
                         frame = (frame + 1) % 6;
                         if (runnerGame.runAnim[frame] != null) {
                             rView.setImage(runnerGame.runAnim[frame]);
                         }
                     }
                }
            };
            
            runBtn.setOnMouseEntered(e -> {
                runBtnTimer.start();
            });
            runBtn.setOnMouseExited(e -> {
                runBtnTimer.stop();
                rView.setImage(runnerGame.runAnim[0]);
            });
            // Stop logic just in case
            runBtn.setOnAction(e -> {
                runBtnTimer.stop();
                startRunnerGame();
            });
        } else {
             runBtn.setOnAction(e -> startRunnerGame());
        }
        
        StackPane.setAlignment(rView, Pos.CENTER_LEFT);
        StackPane.setAlignment(rLabel, Pos.CENTER);
        rPane.getChildren().addAll(rView, rLabel);
        runBtn.setGraphic(rPane);
        
        // Developer Credit
        HBox creditBox = new HBox(10);
        creditBox.setAlignment(Pos.CENTER);
        VBox.setMargin(creditBox, new javafx.geometry.Insets(20, 0, 0, 0));

        try {
            // Load image with specific size (60x60 for Retina support on 30x30 view) and smoothing enabled
            Image iconImg = new Image(getClass().getResourceAsStream("ritaneko.png"), 60, 60, true, true);
            if (iconImg != null) {
                ImageView iconView = new ImageView(iconImg);
                iconView.setFitWidth(30);
                iconView.setFitHeight(30);
                iconView.setSmooth(true); // Ensure smoothing is applied during rendering
                
                // Circular Clip
                Circle clip = new Circle(15, 15, 15);
                iconView.setClip(clip);
                
                creditBox.getChildren().add(iconView);
            }
        } catch (Exception e) {
            // Ignore if icon fails
        }

        Label creditLabel = new Label("Created by Rita");
        creditLabel.setFont(Font.font("Verdana", FontWeight.NORMAL, 14));
        creditLabel.setTextFill(Color.GRAY);
        creditBox.getChildren().add(creditLabel);

        // Make clickable
        creditBox.setCursor(Cursor.HAND);
        creditBox.setOnMouseClicked(e -> {
            getHostServices().showDocument("https://rita-s-portfolio.vercel.app/");
        });

        menuBox.getChildren().addAll(titleLabel, subLabel, flappyBtn, runBtn, creditBox);
        
        // Ensure overlay is hidden on title
        if (overlayBox != null) overlayBox.setVisible(false);
        isOverlayActive = false;
        
        root.setCenter(menuBox); 
    }

    private void startFlappyBird() {
        currentState = GameState.FLAPPY;
        overlayBox.setVisible(false);
        isOverlayActive = false;
        root.setCenter(gameStack); 
        canvas.requestFocus();
        flappyGame.resetGame();
    }
    
    // ==========================================
    // RANKING / GAME OVER OVERLAY
    // ==========================================
    private void showGameOverOverlay(String gameMode, int currentScore) {
        showGameOverOverlay(gameMode, currentScore, false);
    }

    private void showGameOverOverlay(String gameMode, int currentScore, boolean isSubmitted) {
        isOverlayActive = true;
        overlayBox.getChildren().clear();
        overlayBox.setVisible(true);
        
        Label title = new Label("GAME OVER");
        title.setTextFill(Color.RED);
        title.setFont(Font.font("Verdana", FontWeight.BOLD, 40));
        
        Label scoreLabel = new Label("Score: " + currentScore);
        scoreLabel.setTextFill(Color.WHITE);
        scoreLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 24));
        
        // Ranking List
        VBox rankingBox = new VBox(5);
        rankingBox.setAlignment(Pos.CENTER);
        rankingBox.setStyle("-fx-padding: 10; -fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 10;");
        
        List<HighScoreManager.ScoreEntry> tops = HighScoreManager.getTopScores(gameMode);
        Label rankTitle = new Label("--- RANKING ---");
        rankTitle.setTextFill(Color.YELLOW);
        rankTitle.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        rankingBox.getChildren().add(rankTitle);
        
        if (tops.isEmpty()) {
            Label l = new Label("No records yet");
            l.setTextFill(Color.LIGHTGRAY);
            rankingBox.getChildren().add(l);
        } else {
            for (int i=0; i<tops.size(); i++) {
                HighScoreManager.ScoreEntry e = tops.get(i);
                Label l = new Label((i+1) + ". " + e.name + " : " + e.score);
                l.setTextFill(Color.WHITE);
                l.setFont(Font.font("Verdana", 14));
                rankingBox.getChildren().add(l);
            }
        }
        
        overlayBox.getChildren().addAll(title, scoreLabel, rankingBox);
        
        // Check Rank In
        boolean isRankIn = false;
        if (tops.size() < 5) {
            isRankIn = true;
        } else {
            // Must be strictly greater to beat the 5th place (ties favor older records)
            if (currentScore > tops.get(tops.size() - 1).score) {
                isRankIn = true;
            }
        }
        
        // Input Area (Only if not submitted AND is Rank In)
        if (!isSubmitted && isRankIn) {
            HBox inputBox = new HBox(10);
            inputBox.setAlignment(Pos.CENTER);
            
            TextField nameField = new TextField();
            nameField.setPromptText("Enter Name");
            nameField.setPrefWidth(150);
            
            Button registerBtn = new Button("登録");
            registerBtn.setOnAction(e -> {
                String name = nameField.getText().trim();
                if (name.isEmpty()) name = "NoName";
                
                HighScoreManager.submitScore(gameMode, name, currentScore);
                // Refresh with submitted flag
                showGameOverOverlay(gameMode, currentScore, true);
            });
            
            inputBox.getChildren().addAll(nameField, registerBtn);
            overlayBox.getChildren().add(inputBox);
        } else if (isSubmitted) {
            Label successLabel = new Label("Registered!");
            successLabel.setTextFill(Color.LIGHTGREEN);
            successLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
            overlayBox.getChildren().add(successLabel);
        } else {
            // Rank Out 
            Label outLabel = new Label("Rank Out");
            outLabel.setTextFill(Color.GRAY);
            outLabel.setFont(Font.font("Verdana", FontWeight.NORMAL, 14));
            overlayBox.getChildren().add(outLabel);
        }
        
        // Buttons
        HBox btnBox = new HBox(20);
        btnBox.setAlignment(Pos.CENTER);
        
        Button retryBtn = new Button("リトライ");
        retryBtn.setOnAction(e -> {
            if (gameMode.equals("flappy")) {
                startFlappyBird();
            } else {
                startRunnerGame();
            }
        });
        
        Button titleBtn = new Button("タイトルへ");
        titleBtn.setOnAction(e -> showTitleScreen());
        
        btnBox.getChildren().addAll(retryBtn, titleBtn);
        
        overlayBox.getChildren().add(btnBox);
    }

    private void startRunnerGame() {
        currentState = GameState.RUN;
        overlayBox.setVisible(false);
        isOverlayActive = false;
        root.setCenter(gameStack);
        canvas.requestFocus();
        runnerGame.resetGame();
        // runnerGame.isRunning = true; // Wait for input to start?
    }

    private void update() {
        if (currentState == GameState.TITLE) return; 

        if (currentState == GameState.FLAPPY) {
            flappyGame.update();
        } else if (currentState == GameState.RUN) {
            runnerGame.update();
        }
    }

    private void render() {
        if (currentState == GameState.TITLE) return;

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
        alert.setContentText("Flappy Usako： スペース/クリック/上矢印でジャンプ\nUsako Run!： 上矢印=ジャンプ, 下矢印=しゃがむ");
        alert.showAndWait();
    }

    private void showUsakoInfo() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("うさこについて");
        alert.setHeaderText("キャラクター紹介");
        
        HBox content = new HBox(20);
        content.setAlignment(Pos.CENTER_LEFT);
        
        // Image
        try {
            Image img = new Image(getClass().getResourceAsStream("usako_normal.png"));
            ImageView view = new ImageView(img);
            view.setFitWidth(100);
            view.setPreserveRatio(true);
            content.getChildren().add(view);
        } catch (Exception e) {
            // Ignore
        }
        
        // Text
        String desc = "福岡工業大学の情報技術研究部、通称じょぎと呼ばれる情報系サークルの公式キャラクター。\n" +
                      "2025年に創設されたうさこ部（うさこに関する歴史調査部）の調査により、2004年から存在しているキャラクターであることが判明した。実はかなり歴史の長いキャラクターであり、何代も受け継がれて姿を変えている。\n" +
                      "初期時点では本ゲームのうさこが抱えているぬいぐるみが本来の姿であったが、2014年に擬人化がなされた模様。\n" +
                      "実は性別は男であるとか...";
        
        Label label = new Label(desc);
        label.setWrapText(true);
        label.setMaxWidth(400);
        
        content.getChildren().add(label);
        
        alert.getDialogPane().setContent(content);
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
        int highScore = 0;
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
            highScore = HighScoreManager.getHighScore("flappy");
            ticks = 0;
            pipes.clear();
            isRunning = false;
            isGameOver = false;
        }

        void handleKeyPress(KeyCode code) {
            if (code == KeyCode.ESCAPE) {
                 if (isGameOver || !isRunning) {
                     showTitleScreen();
                     return;
                 }
            }
            if (code == KeyCode.SPACE || code == KeyCode.UP) {
                handleInput();
            }
        }

        void handleInput() {
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
                     SoundManager.playScore();
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
            SoundManager.playGameOver();
            // Show Overlay
            showGameOverOverlay("flappy", score);
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

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Verdana", FontWeight.BOLD, 24));
            gc.fillText("Score: " + score, WINDOW_WIDTH - 220, 50);
            gc.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
            gc.fillText("High Score: " + highScore, WINDOW_WIDTH - 220, 80);

            if (!isRunning && !isGameOver) {
                gc.setFill(Color.WHITE);
                Font f = Font.font("Verdana", FontWeight.BOLD, 30);
                gc.setFont(f);
                String text = "スペース/上矢印でスタート";
                Text t = new Text(text); t.setFont(f);
                double w = t.getLayoutBounds().getWidth();
                gc.fillText(text, (WINDOW_WIDTH - w) / 2, 300);
                
                Font fEsc = Font.font("Verdana", FontWeight.BOLD, 20);
                gc.setFont(fEsc);
                String tEsc = "ESCでタイトルへ";
                Text txtEsc = new Text(tEsc); txtEsc.setFont(fEsc);
                double wEsc = txtEsc.getLayoutBounds().getWidth();
                gc.fillText(tEsc, (WINDOW_WIDTH - wEsc) / 2, 350);
            }
            if (isGameOver) {
                // Overlay is shown, do nothing here regarding text
                // gc.setFill(Color.RED); ...
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
        int highScore = 0;
        int tick = 0;
        double animTick = 0;
        
        List<RunnerObstacle> obstacles = new ArrayList<>();
        Random random = new Random();
        double obsSpeed = 6;
        int spawnTimer = 0;
        
        String milestoneMsg = "";
        int milestoneTimer = 0;

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

        void handleKeyPress(KeyCode code) {
            if (isGameOver) {
                if (code == KeyCode.UP) {
                    resetGame();
                    isRunning = true;
                    return;
                }
                if (code == KeyCode.ESCAPE) {
                    showTitleScreen();
                    return;
                }
                // Do not allow other keys to fall through and change state
                return;
            }
            if (!isRunning) {
                 if (code == KeyCode.UP) isRunning = true;
                 if (code == KeyCode.ESCAPE) {
                     showTitleScreen();
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
            
            // Animation speed based on running speed
            // Base speed 6.0 -> 1.0 increment. Speed 12.0 -> 2.0 increment.
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
                    showGameOverOverlay("runner", score);
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

        void spawnObstacle() {
            boolean isSky = random.nextDouble() > 0.6; 
            
            double ox = WINDOW_WIDTH;
            double oy;
            double ow = 50; 
            double oh = 60; 
            
            if (isSky) {
                // Random height: 105 to 130 (Dodgable by crouching, but hits head if standing)
                // groundY = 500. 
                // offset 105 -> oy=395, oh=45 -> bottom=440. CrouchTop=445. Safe (445 > 440).
                // offset 130 -> oy=370, oh=45 -> bottom=415. StandTop=410. Hits Head (410 < 415).
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
            gc.fillText("Score: " + score, WINDOW_WIDTH - 220, 50);
            gc.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
            gc.fillText("High Score: " + highScore, WINDOW_WIDTH - 220, 80);
            
            if (!isRunning && !isGameOver) {
                 Font fStart = Font.font("Verdana", FontWeight.BOLD, 40);
                 gc.setFont(fStart);
                 String tStart = "上矢印でスタート";
                 Text txtStart = new Text(tStart); txtStart.setFont(fStart);
                 double wStart = txtStart.getLayoutBounds().getWidth();
                 gc.fillText(tStart, (WINDOW_WIDTH - wStart) / 2, 200);
                 
                 gc.setFill(Color.BLACK);
                 Font fInst = Font.font("Verdana", FontWeight.BOLD, 20);
                 gc.setFont(fInst);
                 String tInst = "上矢印: ジャンプ / 下矢印: しゃがむ";
                 Text txtInst = new Text(tInst); txtInst.setFont(fInst);
                 double wInst = txtInst.getLayoutBounds().getWidth();
                 gc.fillText(tInst, (WINDOW_WIDTH - wInst) / 2, 250);
                 
                 String tEsc = "ESCでタイトルへ";
                 Text txtEsc = new Text(tEsc); txtEsc.setFont(fInst);
                 double wEsc = txtEsc.getLayoutBounds().getWidth();
                 gc.fillText(tEsc, (WINDOW_WIDTH - wEsc) / 2, 290);
            }
            
            // Milestone Text
            if (isRunning && !isGameOver && milestoneTimer > 0) {
                gc.setFill(Color.ORANGE);
                Font fMile = Font.font("Verdana", FontWeight.BOLD, 40);
                gc.setFont(fMile);
                
                Text tMile = new Text(milestoneMsg); tMile.setFont(fMile);
                double wMile = tMile.getLayoutBounds().getWidth();
                double x = (WINDOW_WIDTH - wMile) / 2;
                double y = 150;
                
                gc.fillText(milestoneMsg, x, y);
            }
            
            if (isGameOver) {
               // Handled by Overlay
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
            if (isCrouching && !isGameOver) {
                // Animation
                int frame = ((int)animTick / 5) % 5;
                if (squatAnim[frame] != null) return squatAnim[frame];
            }
            // Run (6 frames)
            int frame = ((int)animTick / 5) % 6;
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
