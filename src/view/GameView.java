package view;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import controller.CameraNotFoundException;
import model.GameBoard;
import model.TileNode;
import model.Direction;
import model.GazeState;
import controller.EyeTracker;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.scene.control.ScrollPane;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds the complete UI:
 * ┌──────────────────────────────────────┐
 * │  Header: title + score cards         │
 * ├─────────────┬────────────────────────┤
 * │  Sidebar    │  Game board (4×4)      │
 * │  · Webcam   │                        │
 * │  · Gaze UI  │                        │
 * │  · Legend   │                        │
 * └─────────────┴────────────────────────┘
 */
public class GameView {

    private static final int TILE_SIZE = 108;
    private static final int GAP       = 10;
    private static final int BOARD_PAD = 12;

    private final GameBoard board;
    private final EyeTracker tracker;

    // Root
    private final BorderPane root = new BorderPane();

    // Score labels
    private Label scoreLabel;
    private Label bestLabel;

    // Board tiles
    private final Pane tilePane = new Pane();
    private final Map<String, TileNode> tileNodes = new HashMap<>();

    // Sidebar widgets
    private ImageView cameraView;
    private Label     statusLabel;
    private Label     gazeLabel;
    private Rectangle dwellBar;
    private Rectangle dwellTrack;
    private Label     calibLabel;
    private Button    camBtn;

    // Compass zones
    private final Map<Direction, StackPane> gazeZones = new HashMap<>();

    // Overlay
    private StackPane overlay;
    private Label     overlayTitle;
    private Label     overlayScore;

    public GameView(GameBoard board, EyeTracker tracker) {
        this.board   = board;
        this.tracker = tracker;
        buildUI();
        wireListeners();
    }

    public Parent getRoot() { return root; }

    // ── Build UI ──────────────────────────────────────────────────
    private void buildUI() {
        root.getStyleClass().add("root-pane");

        // Header
        root.setTop(buildHeader());

        // Center: sidebar + game
        HBox center = new HBox(0);
        center.getStyleClass().add("center-area");
        center.getChildren().addAll(buildSidebar(), buildGameArea());
        root.setCenter(center);
    }

    // ── Header ────────────────────────────────────────────────────
    private HBox buildHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER_LEFT);

        // Logo
        Label title = new Label("2048");
        title.getStyleClass().add("logo-title");
        Label tag = new Label("eye control");
        tag.getStyleClass().add("logo-tag");
        HBox logo = new HBox(10, title, tag);
        logo.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Scorecards
        scoreLabel = new Label("0");
        bestLabel  = new Label("0");
        HBox scores = new HBox(12, scoreCard("SCORE", scoreLabel), scoreCard("BEST", bestLabel));
        scores.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(logo, spacer, scores);
        return header;
    }

    private VBox scoreCard(String lbl, Label value) {
        Label l = new Label(lbl);
        l.getStyleClass().add("score-label");
        value.getStyleClass().add("score-value");
        VBox card = new VBox(2, l, value);
        card.getStyleClass().add("score-card");
        card.setAlignment(Pos.CENTER);
        return card;
    }

    // ── Sidebar ───────────────────────────────────────────────────
    private ScrollPane buildSidebar() {
        VBox sidebar = new VBox(14);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(290);

        sidebar.getChildren().addAll(
                buildCameraPanel(),
                buildGazePanel(),
                buildLegendPanel(),
                buildButtons()
        );

        // Lắp thanh cuộn bọc bên ngoài sidebar
        ScrollPane scrollPane = new ScrollPane(sidebar);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        return scrollPane;
    }

    private VBox buildCameraPanel() {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("panel");

        HBox header = panelHeader("WEBCAM");
        Label dot = new Label("●");
        dot.getStyleClass().add("cam-dot");
        header.getChildren().add(dot);

        cameraView = new ImageView();
        cameraView.setFitWidth(266);
        cameraView.setFitHeight(180);
        cameraView.setPreserveRatio(true);
        cameraView.getStyleClass().add("camera-view");

        // Placeholder
        StackPane camArea = new StackPane(cameraView);
        camArea.getStyleClass().add("camera-area");
        camArea.setPrefHeight(180);

        calibLabel = new Label("Nhấn 'Bật Camera' để bắt đầu");
        calibLabel.getStyleClass().add("calib-label");
        camArea.getChildren().add(calibLabel);

        panel.getChildren().addAll(header, camArea);
        return panel;
    }

    private VBox buildGazePanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(14));

        Label title = new Label("HƯỚNG NHÌN");
        title.getStyleClass().add("panel-title");

        // Compass 3×3
        GridPane compass = new GridPane();
        compass.setHgap(6); compass.setVgap(6);
        compass.setAlignment(Pos.CENTER);

        addCompassCell(compass, null,           0, 0);
        addCompassCell(compass, Direction.UP,   1, 0);
        addCompassCell(compass, null,           2, 0);
        addCompassCell(compass, Direction.LEFT, 0, 1);

        // center cell
        StackPane center = new StackPane(new Label("•"));
        center.getStyleClass().addAll("compass-cell", "compass-center");
        center.setPrefSize(56, 46);
        compass.add(center, 1, 1);

        addCompassCell(compass, Direction.RIGHT, 2, 1);
        addCompassCell(compass, null,            0, 2);
        addCompassCell(compass, Direction.DOWN,  1, 2);
        addCompassCell(compass, null,            2, 2);

        // Dwell progress bar
        dwellTrack = new Rectangle(0, 8);
        dwellTrack.getStyleClass().add("dwell-track");
        dwellBar = new Rectangle(0, 8);
        dwellBar.getStyleClass().add("dwell-bar");
        StackPane barWrap = new StackPane();
        barWrap.getStyleClass().add("dwell-wrap");
        barWrap.setMinHeight(10);
        barWrap.getChildren().addAll(dwellTrack, dwellBar);
        StackPane.setAlignment(dwellBar, Pos.CENTER_LEFT);

        gazeLabel = new Label("chờ camera...");
        gazeLabel.getStyleClass().add("gaze-status");

        statusLabel = new Label("");
        statusLabel.getStyleClass().add("status-label");

        panel.getChildren().addAll(title, compass, barWrap, gazeLabel, statusLabel);
        return panel;
    }

    private void addCompassCell(GridPane g, Direction dir, int col, int row) {
        if (dir == null) {
            Pane empty = new Pane(); empty.setPrefSize(56, 46);
            g.add(empty, col, row);
            return;
        }
        String arrow = dir.arrow();
        Label lbl = new Label(arrow);
        lbl.getStyleClass().add("compass-arrow");
        StackPane cell = new StackPane(lbl);
        cell.getStyleClass().add("compass-cell");
        cell.setPrefSize(56, 46);
        gazeZones.put(dir, cell);
        g.add(cell, col, row);
    }

    private VBox buildLegendPanel() {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(14));

        Label title = new Label("ĐIỀU KHIỂN");
        title.getStyleClass().add("panel-title");
        title.setPadding(new Insets(0, 0, 8, 0));

        panel.getChildren().addAll(title,
                legendRow("Nhìn + giữ 1 giây", "→ di chuyển"),
                legendRow("Phím ↑↓←→ / WASD", "→ fallback"),
                legendRow("Phím R", "→ chơi lại"),
                legendRow("ESC", "→ thoát")
        );
        return panel;
    }

    private HBox legendRow(String key, String val) {
        Label k = new Label(key); k.getStyleClass().add("legend-key");
        Label v = new Label(val); v.getStyleClass().add("legend-val");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(k, sp, v);
        row.getStyleClass().add("legend-row");
        return row;
    }

    private HBox buildButtons() {
        camBtn = new Button("Bật Camera");
        camBtn.getStyleClass().addAll("btn", "btn-primary");
        camBtn.setMaxWidth(Double.MAX_VALUE);
        camBtn.setOnAction(e -> startCamera());

        Button resetBtn = new Button("Chơi Mới  R");
        resetBtn.getStyleClass().addAll("btn", "btn-ghost");
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.setOnAction(e -> board.newGame());

        HBox row = new HBox(10, camBtn, resetBtn);
        HBox.setHgrow(camBtn, Priority.ALWAYS);
        HBox.setHgrow(resetBtn, Priority.ALWAYS);
        return row;
    }

    // ── Game Area ─────────────────────────────────────────────────
    private StackPane buildGameArea() {
        // Background grid (empty cells)
        GridPane bgGrid = new GridPane();
        bgGrid.setHgap(GAP); bgGrid.setVgap(GAP);
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++) {
                Rectangle cell = new Rectangle(TILE_SIZE, TILE_SIZE);
                cell.getStyleClass().add("cell-bg");
                bgGrid.add(cell, c, r);
            }

        // Tile pane overlays exactly the same area
        tilePane.setPrefSize(
                4 * TILE_SIZE + 3 * GAP,
                4 * TILE_SIZE + 3 * GAP);
        tilePane.setMouseTransparent(true);

        // Container
        StackPane boardInner = new StackPane(bgGrid, tilePane);
        boardInner.getStyleClass().add("board-inner");
        boardInner.setPadding(new Insets(BOARD_PAD));

        // Overlay (game over / win)
        overlayTitle = new Label("Game Over");
        overlayTitle.getStyleClass().add("overlay-title");
        overlayScore = new Label("");
        overlayScore.getStyleClass().add("overlay-score");
        Button restartBtn = new Button("Chơi Lại");
        restartBtn.getStyleClass().addAll("btn", "btn-primary");
        restartBtn.setOnAction(e -> board.newGame());

        VBox overlayContent = new VBox(14, overlayTitle, overlayScore, restartBtn);
        overlayContent.setAlignment(Pos.CENTER);

        overlay = new StackPane(overlayContent);
        overlay.getStyleClass().add("game-overlay");
        overlay.setVisible(false);
        overlay.setPrefSize(
                4 * TILE_SIZE + 3 * GAP + 2 * BOARD_PAD,
                4 * TILE_SIZE + 3 * GAP + 2 * BOARD_PAD);

        StackPane boardStack = new StackPane(boardInner, overlay);

        // Wrap in a centred VBox
        VBox gameArea = new VBox();
        gameArea.getStyleClass().add("game-area");
        gameArea.setAlignment(Pos.CENTER);
        HBox.setHgrow(gameArea, Priority.ALWAYS);
        gameArea.getChildren().add(boardStack);
        return boardStack;
    }

    // ── Wire listeners ────────────────────────────────────────────
    private void wireListeners() {
        board.addListener(new GameBoard.GameListener() {
            @Override public void onBoardChanged(int[][] grid, int score, int best) {
                Platform.runLater(() -> {
                    overlay.setVisible(false);
                    scoreLabel.setText(String.valueOf(score));
                    bestLabel.setText(String.valueOf(best));
                    renderTiles(grid);
                });
            }
            @Override public void onGameOver(int score) {
                Platform.runLater(() -> showOverlay("Game Over", score, false));
            }
            @Override public void onWin(int score) {
                Platform.runLater(() -> showOverlay("🎉  2048!", score, true));
            }
        });

        tracker.setOnGazeUpdate(this::handleGazeState);
        tracker.setOnMove(dir -> Platform.runLater(() -> board.move(dir)));
        tracker.setOnFrame(img -> Platform.runLater(() -> {
            cameraView.setImage(img);
            calibLabel.setVisible(false);
        }));
    }

    // ── Gaze State → UI ───────────────────────────────────────────
    private void handleGazeState(GazeState state) {
        // Update dwell bar
        double barW = (4 * TILE_SIZE + 3 * GAP) * state.dwellProgress();
        // Use sidebar width
        double trackW = 240;
        dwellTrack.setWidth(trackW);
        dwellBar.setWidth(trackW * state.dwellProgress());

        // Color: accent → green when nearly done
        if (state.dwellProgress() > 0.8) {
            dwellBar.getStyleClass().removeAll("dwell-bar");
            dwellBar.getStyleClass().add("dwell-bar-done");
        } else {
            dwellBar.getStyleClass().removeAll("dwell-bar-done");
            dwellBar.getStyleClass().add("dwell-bar");
        }

        // Compass highlights
        for (var entry : gazeZones.entrySet()) {
            StackPane zone = entry.getValue();
            zone.getStyleClass().removeAll("compass-active", "compass-triggered");
            if (state.gazeDirection() == entry.getKey()) {
                if (state.dwellProgress() >= 1f) zone.getStyleClass().add("compass-triggered");
                else if (state.dwellProgress() > 0) zone.getStyleClass().add("compass-active");
            }
        }

        // Status text
        if (!state.calibrated()) {
            calibLabel.setVisible(true);
            int pct = (int)(state.calibProgress() * 100);
            calibLabel.setText("Hiệu chỉnh... " + pct + "%");
            gazeLabel.setText("nhìn thẳng vào camera...");
        } else {
            calibLabel.setVisible(false);
            if (state.gazeDirection() != null) {
                String d = state.gazeDirection().arrow() + " " + switch(state.gazeDirection()) {
                    case Direction.UP    -> "nhìn lên";
                    case Direction.DOWN  -> "nhìn xuống";
                    case Direction.LEFT  -> "nhìn trái";
                    case Direction.RIGHT -> "nhìn phải";
                };
                gazeLabel.setText(d + "  " + (int)(state.dwellProgress()*100) + "%");
            } else if (!state.faceDetected()) {
                gazeLabel.setText("không thấy khuôn mặt");
            } else {
                gazeLabel.setText("nhìn vào một hướng...");
            }
        }
    }

    // ── Tile rendering ────────────────────────────────────────────
    private void renderTiles(int[][] grid) {
        tilePane.getChildren().clear();
        tileNodes.clear();

        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++) {
                int v = grid[r][c];
                if (v == 0) continue;
                TileNode tile = new TileNode(v);
                positionTile(tile, r, c);
                tilePane.getChildren().add(tile);
                tileNodes.put(r + "," + c, tile);

                // Appear animation
                tile.setScaleX(0.6); tile.setScaleY(0.6); tile.setOpacity(0);
                ScaleTransition st = new ScaleTransition(Duration.millis(160), tile);
                st.setToX(1); st.setToY(1);
                FadeTransition ft = new FadeTransition(Duration.millis(120), tile);
                ft.setToValue(1);
                new ParallelTransition(st, ft).play();
            }
    }

    private void positionTile(TileNode tile, int row, int col) {
        double x = col * (TILE_SIZE + GAP);
        double y = row * (TILE_SIZE + GAP);
        tile.setLayoutX(x);
        tile.setLayoutY(y);
    }

    // ── Overlay ───────────────────────────────────────────────────
    private void showOverlay(String title, int score, boolean win) {
        overlayTitle.setText(title);
        overlayScore.setText("Điểm: " + score);
        overlayTitle.getStyleClass().removeAll("overlay-win", "overlay-over");
        overlayTitle.getStyleClass().add(win ? "overlay-win" : "overlay-over");
        overlay.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(350), overlay);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    // ── Camera start ──────────────────────────────────────────────
    // ── Camera start ──────────────────────────────────────────────
    private void startCamera() {
        camBtn.setDisable(true);
        camBtn.setText("Đang kết nối...");

        new Thread(() -> {
            try {
                tracker.start(); // Gọi hàm bật camera

                // NẾU BẬT THÀNH CÔNG: Đổi tên nút thành "Camera đang chạy"
                Platform.runLater(() -> {
                    camBtn.setText("Camera đang chạy •");
                    camBtn.setStyle("-fx-background-color: #2e7d32;"); // Đổi màu xanh lá
                });

            } catch (CameraNotFoundException ex) {
                // NẾU LỖI CAMERA: Đẩy lệnh hiện Popup về luồng chính bằng Platform.runLater
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Lỗi Thiết Bị");
                    alert.setHeaderText("Lỗi Khởi Động Camera");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait(); // Hiển thị popup

                    // Nhả nút bấm ra để người chơi có thể thử lại
                    camBtn.setDisable(false);
                    camBtn.setText("Bật Camera");
                    camBtn.setStyle("");
                });

            } catch (Exception ex) {
                // Bắt các lỗi khác (thiếu file XML...)
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Lỗi Hệ Thống");
                    alert.setHeaderText("Lỗi dữ liệu AI");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();

                    camBtn.setDisable(false);
                    camBtn.setText("Bật Camera");
                    camBtn.setStyle("");
                });
            }
        }, "CameraStart").start();
    }

    // ── Panel builder helper ──────────────────────────────────────
    private HBox panelHeader(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("panel-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox h = new HBox(l, sp);
        h.getStyleClass().add("panel-header");
        return h;
    }
}