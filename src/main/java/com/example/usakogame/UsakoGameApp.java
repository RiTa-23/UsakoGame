package com.example.usakogame;

import com.example.usakogame.flappy.FlappyBirdGame;
import com.example.usakogame.manager.HighScoreManager;
import com.example.usakogame.runner.RunnerGame;
import com.example.usakogame.ui.RankingScreen;
import com.example.usakogame.ui.TitleScreen;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.util.List;

public class UsakoGameApp extends Application {

    public static final int WINDOW_WIDTH = 600;
    public static final int WINDOW_HEIGHT = 600;

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
        flappyGame = new FlappyBirdGame(this);
        runnerGame = new RunnerGame(this);

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

    public void showTitleScreen() {
        currentState = GameState.TITLE;
        overlayBox.setVisible(false);
        isOverlayActive = false;
        
        root.setCenter(TitleScreen.create(this)); 
    }

    public void showRankingScreen() {
        root.setCenter(RankingScreen.create(this));
    }

    public void startFlappyBird() {
        currentState = GameState.FLAPPY;
        overlayBox.setVisible(false);
        isOverlayActive = false;
        root.setCenter(gameStack); 
        canvas.requestFocus();
        flappyGame.resetGame();
    }
    
    public void startRunnerGame() {
        currentState = GameState.RUN;
        overlayBox.setVisible(false);
        isOverlayActive = false;
        root.setCenter(gameStack);
        canvas.requestFocus();
        runnerGame.resetGame();
    }

    // ==========================================
    // GAME OVER OVERLAY
    // ==========================================
    public void showGameOverOverlay(String gameMode, int currentScore) {
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
            if (currentScore > tops.get(tops.size() - 1).score) {
                isRankIn = true;
            }
        }
        
        // Input Area
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
        
        try {
            javafx.scene.image.Image img = new javafx.scene.image.Image(getClass().getResourceAsStream("usako_normal.png"));
            ImageView view = new ImageView(img);
            view.setFitWidth(100);
            view.setPreserveRatio(true);
            content.getChildren().add(view);
        } catch (Exception e) {
            // Ignore
        }
        
        String desc = "福岡工業大学の情報技術研究部、通称じょぎと呼ばれる情報系サークルの公式キャラクター。\n" +
                      "2025年に創設されたうさこ部（うさこに関する歴史調査部）の調査により、2004年から存在しているキャラクターであることが判明した。実はかなり歴史の長いキャラクターであり、何代も受け継がれて姿を変えている。\n" +
                      "初期時点では本ゲームのうさこが抱えているぬいぐるみが本来の姿であった。\n" +
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
    
    // Getters for Games to access resources if needed by TitleScreen
    public FlappyBirdGame getFlappyGame() { return flappyGame; }
    public RunnerGame getRunnerGame() { return runnerGame; }
}
