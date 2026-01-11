# Usako Game 内部システム解説ドキュメント

## 概要
本プロジェクトは、JavaFXを使用して構築された2Dアクションゲームアプリケーションです。
以下の機能を提供します：
1. **Flappy Bird風ゲーム**: 重力とジャンプ操作による障害物回避。
2. **ランナーゲーム**: 横スクロールで迫りくる障害物をジャンプ・しゃがみで回避。
3. **ランキング機能**: スコアの永続化とTOP 5表示。
4. **サウンド合成機能**: 外部音声ファイルを使用せず、プログラムで波形を生成して再生。

## 使用ライブラリ
*   **JavaFX (org.openjfx)**: GUI、グラフィックス描画、イベント処理、アニメーションに使用。
    *   `javafx.application`: アプリケーションのライフサイクル管理。
    *   `javafx.scene`: シーングラフ、キャンバス (`Canvas`) への描画。
    *   `javafx.scene.control`: ボタン、テキストフィールドなどのUI部品。
*   **Java Standard Library (java.desktop)**: 音声再生に使用。
    *   `javax.sound.sampled`: バイト配列からのPCM音声合成と再生（`SourceDataLine`）。

---

## 各ファイルの役割と詳細

### 1. `com.example.usakogame.FlappyBirdApp.java`
**役割**: アプリケーションのエントリーポイントであり、メインのゲームロジック、画面遷移、描画ループを管理します。

#### クラス全体の構造
*   `extends Application`: JavaFXアプリケーションとして動作します。
*   `AnimationTimer timer`: ゲームループ（約60FPSで動作）。`handle(long now)` 内で `update()` (計算) と `render()` (描画) を呼び出します。
*   `BorderPane root`, `StackPane gameStack`: 画面レイアウトのルート。通常は `root` を使用し、ゲーム中は `gameStack` (Canvas + Overlay) を使用します。

#### 主要メソッド
*   **`start(Stage stage)`**:
    *   アプリケーションの初期化。`Canvas`、`MenuBar`、`AnimationTimer` のセットアップを行います。
    *   `gameStack` にゲーム描画用の `Canvas` と、ゲームオーバー用の `overlayBox` (VBox) を重ねて配置します。
    *   入力イベント (`setOnKeyPressed`, `setOnMouseClicked`) を現在のゲームモード (`currentState`) に応じて各ゲームロジックへ振り分けます。
*   **`showTitleScreen()`**:
    *   タイトル画面（メニュー）を構築し、`root.setCenter(menuBox)` で表示します。
    *   アニメーション付きボタン（マウスホバーで画像変更）を生成します。
*   **`showGameOverOverlay(String gameMode, int currentScore)`**:
    *   ゲームオーバー時に呼び出され、半透明のオーバーレイを表示します。
    *   `HighScoreManager` からランキングを取得して表示。
    *   ランクインしている場合のみ、名前入力用の `TextField` を表示します。

#### 内部クラス: `FlappyBirdGame`
Flappy Birdモードのロジックをカプセル化したクラスです。
*   **変数**:
    *   `birdY`, `velocityY`: プレイヤーの垂直位置と速度。
    *   `pipes`: 障害物（土管）のリスト。
*   **メソッド**:
    *   `update()`: `velocityY` に `GRAVITY` を加算し、位置を更新。土管の移動と生成管理。
    *   `handleKeyPress()`: ジャンプ処理（`velocityY` を上向きに設定）。

#### 内部クラス: `RunnerGame`
Usako Run!モードのロジックをカプセル化したクラスです。
*   **変数**:
    *   `playerY`, `velocityY`: プレイヤー位置（基本は地面 `groundY`）。
    *   `isCrouching`: しゃがみ状態フラグ。しゃがんでいる間は当たり判定が小さくなります。
    *   `obstacles`: 障害物のリスト。
*   **メソッド**:
    *   `update()`: 障害物の移動。スコアに応じた速度上昇 (`obsSpeed`)。
    *   `getCurrentSprite()`: 現在の状態（走り、ジャンプ、しゃがみ）に応じた画像を返します。

---

### 2. `com.example.usakogame.HighScoreManager.java`
**役割**: ハイスコアの読み込み、保存、ランキング管理を行うユーティリティクラス（staticメソッドのみ）。

#### 主要機能
*   **データ保存**: `usako_save/scores.properties` ファイルにデータを保存します。
    *   形式: `gameMode.{rank}.name`, `gameMode.{rank}.score` (例: `flappy.0.score=100`)
*   **`getTopScores(String gameMode)`**:
    *   指定されたゲームモードの上位5件を `List<ScoreEntry>` として返します。
*   **`submitScore(String gameMode, String name, int score)`**:
    *   スコアをランキングに追加し、ソートを行い、上位5件のみを残して保存します。

---

### 3. `com.example.usakogame.SoundManager.java`
**役割**: 効果音（SE）をプログラム的に生成して再生します。外部音声ファイルを必要としません。

#### 技術詳細
*   **16-bit PCM合成**: 波形データをバイト配列として生成し、`AudioSystem` に書き込みます。
*   **`playTone(startHz, endHz, ms, volume)`**:
    *   正弦波（サイン波）を生成します。周波数を時間とともに変化させることで、ジャンプ音（上昇音）やスコア音（「ピロン」という音）を表現します。
*   **`playNoise(ms)`**:
    *   ホワイトノイズ（ランダムな値）を生成し、減衰させることで、衝突音（「ドスッ」という音）を表現します。
*   **メソッド**:
    *   `playJump()`: ジャンプ音 (300Hz -> 600Hz)。
    *   `playScore()`: スコア獲得音 (1200Hz -> 1800Hz)。
    *   `playGameOver()`: ゲームオーバー音 (ノイズ)。

---

### 4. `com.example.usakogame.Launcher.java`
**役割**: アプリケーションの起動用ラッパー。
*   JavaFXアプリケーションを含むJARファイルを作成する際、メインクラスが `Application` クラスを継承していると、JVMの起動チェックでエラーになる場合があるため、その回避策として単純な `main` メソッドのみを持つこのクラスを経由して `FlappyBirdApp` を起動します。

---
