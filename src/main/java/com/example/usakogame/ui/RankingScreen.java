package com.example.usakogame.ui;

import com.example.usakogame.UsakoGameApp;
import com.example.usakogame.manager.HighScoreManager;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

public class RankingScreen {

    public static Parent create(UsakoGameApp app) {
        VBox rankRoot = new VBox(20);
        rankRoot.setAlignment(Pos.CENTER);
        rankRoot.setStyle("-fx-background-color: rgba(255, 255, 255, 0.95); -fx-padding: 30;");

        Label title = new Label("ランキング");
        title.setFont(Font.font("Verdana", FontWeight.BOLD, 30));

        HBox tablesBox = new HBox(40);
        tablesBox.setAlignment(Pos.CENTER);

        VBox flappyBox = createRankingTable("Flappy Usako", "flappy");
        VBox runnerBox = createRankingTable("Usako Run!", "runner");

        tablesBox.getChildren().addAll(flappyBox, runnerBox);

        // Delete Data Button
        Button deleteBtn = new Button("ランキングデータを全削除");
        deleteBtn.setStyle("-fx-text-fill: red; -fx-border-color: red; -fx-background-color: white;");
        deleteBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("データ削除");
            alert.setHeaderText("本当に削除しますか？");
            alert.setContentText("すべてのランキングデータが消去されます。元に戻すことはできません。");
            
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    HighScoreManager.clearAllData();
                    // Refresh screen by calling showRankingScreen again
                    app.showRankingScreen();
                }
            });
        });

        Button backBtn = new Button("戻る");
        backBtn.setOnAction(e -> app.showTitleScreen());

        rankRoot.getChildren().addAll(title, tablesBox, new Separator(), deleteBtn, backBtn);
        return rankRoot;
    }

    private static VBox createRankingTable(String title, String mode) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.TOP_CENTER);
        box.setStyle("-fx-border-color: lightgray; -fx-padding: 10; -fx-min-width: 200;");

        Label label = new Label(title);
        label.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        box.getChildren().add(label);

        List<HighScoreManager.ScoreEntry> list = HighScoreManager.getTopScores(mode);
        if (list.isEmpty()) {
            box.getChildren().add(new Label("No Data"));
        } else {
            for (int i = 0; i < list.size(); i++) {
                HighScoreManager.ScoreEntry e = list.get(i);
                Label l = new Label((i + 1) + ". " + e.name + " : " + e.score);
                l.setFont(Font.font("Verdana", 14));
                box.getChildren().add(l);
            }
        }
        return box;
    }
}
