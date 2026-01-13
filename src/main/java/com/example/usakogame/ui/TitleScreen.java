package com.example.usakogame.ui;

import com.example.usakogame.UsakoGameApp;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class TitleScreen {

    public static Parent create(UsakoGameApp app) {
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
        fPane.setPadding(new javafx.geometry.Insets(0, 20, 0, 20)); 
        
        Label fLabel = new Label("Flappy Usako");
        fLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 24));
        
        ImageView fView = new ImageView();
        Image birdNormal = app.getFlappyGame().birdNormal;
        Image birdJump = app.getFlappyGame().birdJump;

        if (birdNormal != null) {
            fView.setImage(birdNormal);
            fView.setFitWidth(50);
            fView.setFitHeight(50);
            fView.setPreserveRatio(true);
            
            flappyBtn.setOnMouseEntered(e -> {
                if (birdJump != null) fView.setImage(birdJump);
            });
            flappyBtn.setOnMouseExited(e -> {
               if (birdNormal != null) fView.setImage(birdNormal);
            });
        }
        
        StackPane.setAlignment(fView, Pos.CENTER_LEFT);
        StackPane.setAlignment(fLabel, Pos.CENTER);
        fPane.getChildren().addAll(fView, fLabel);
        flappyBtn.setGraphic(fPane);
        
        flappyBtn.setOnAction(e -> app.startFlappyBird());
        
        // Run Button
        Button runBtn = new Button();
        runBtn.setStyle("-fx-pref-width: 300px; -fx-pref-height: 80px; -fx-padding: 0;");
        
        StackPane rPane = new StackPane();
        rPane.setPrefSize(300, 80);
        rPane.setPadding(new javafx.geometry.Insets(0, 20, 0, 20));
        
        Label rLabel = new Label("Usako Run!");
        rLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 24));
        
        ImageView rView = new ImageView();
        Image[] runAnim = app.getRunnerGame().runAnim;
        
        if (runAnim != null && runAnim.length > 0 && runAnim[0] != null) {
            rView.setImage(runAnim[0]);
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
                         if (frame < runAnim.length && runAnim[frame] != null) {
                             rView.setImage(runAnim[frame]);
                         }
                     }
                }
            };
            
            runBtn.setOnMouseEntered(e -> {
                runBtnTimer.start();
            });
            runBtn.setOnMouseExited(e -> {
                runBtnTimer.stop();
                rView.setImage(runAnim[0]);
            });
            runBtn.setOnAction(e -> {
                runBtnTimer.stop();
                app.startRunnerGame();
            });
        } else {
             runBtn.setOnAction(e -> app.startRunnerGame());
        }
        
        StackPane.setAlignment(rView, Pos.CENTER_LEFT);
        StackPane.setAlignment(rLabel, Pos.CENTER);
        rPane.getChildren().addAll(rView, rLabel);
        runBtn.setGraphic(rPane);
        
        // Ranking Button
        Button rankBtn = new Button("ランキング");
        rankBtn.setStyle("-fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: blue; -fx-underline: true; -fx-cursor: hand;");
        rankBtn.setOnAction(e -> app.showRankingScreen());

        // Developer Credit
        HBox creditBox = new HBox(10);
        creditBox.setAlignment(Pos.CENTER);
        VBox.setMargin(creditBox, new javafx.geometry.Insets(20, 0, 0, 0));

        try {
            Image iconImg = new Image(TitleScreen.class.getResourceAsStream("/com/example/usakogame/ritaneko.png"), 60, 60, true, true);
            if (iconImg != null) {
                ImageView iconView = new ImageView(iconImg);
                iconView.setFitWidth(30);
                iconView.setFitHeight(30);
                iconView.setSmooth(true);
                
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

        creditBox.setCursor(Cursor.HAND);
        creditBox.setOnMouseClicked(e -> {
            app.getHostServices().showDocument("https://rita-s-portfolio.vercel.app/");
        });

        menuBox.getChildren().addAll(titleLabel, subLabel, flappyBtn, runBtn, rankBtn, creditBox);
        return menuBox;
    }
}
