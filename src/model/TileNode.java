package model;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * A single 2048 tile — a styled StackPane with a value label.
 * Color is assigned by CSS style class based on the tile value.
 */
public class TileNode extends StackPane {

    private static final int SIZE = 108;
    private final int value;

    public TileNode(int value) {
        this.value = value;
        setPrefSize(SIZE, SIZE);
        setMinSize(SIZE, SIZE);
        setMaxSize(SIZE, SIZE);

        Label label = new Label(String.valueOf(value));
        label.getStyleClass().add("tile-label");
        label.setStyle("-fx-font-size: " + fontSize(value) + "px;");

        getStyleClass().addAll("tile", tileClass(value));
        setAlignment(Pos.CENTER);
        getChildren().add(label);
    }

    public int getValue() { return value; }

    public void playMergeAnimation() {
        ScaleTransition bounce = new ScaleTransition(Duration.millis(180), this);
        bounce.setAutoReverse(true);
        bounce.setCycleCount(2);
        bounce.setFromX(1.0); bounce.setFromY(1.0);
        bounce.setToX(1.18); bounce.setToY(1.18);
        bounce.play();
    }

    // ── Tile CSS classes ──────────────────────────────────────────
    private static String tileClass(int v) {
        return switch (v) {
            case 2     -> "tile-2";
            case 4     -> "tile-4";
            case 8     -> "tile-8";
            case 16    -> "tile-16";
            case 32    -> "tile-32";
            case 64    -> "tile-64";
            case 128   -> "tile-128";
            case 256   -> "tile-256";
            case 512   -> "tile-512";
            case 1024  -> "tile-1024";
            case 2048  -> "tile-2048";
            default    -> "tile-high";
        };
    }

    private static double fontSize(int v) {
        if (v >= 10000) return 16;
        if (v >= 1000)  return 20;
        if (v >= 100)   return 26;
        if (v >= 10)    return 32;
        return 38;
    }

}
