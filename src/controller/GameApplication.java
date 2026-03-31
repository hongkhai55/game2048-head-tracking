package controller;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.Direction;
import model.GameBoard;
import view.GameView;

/**
 * JavaFX Application entry point.
 * Wires together: model.GameBoard ↔ controller.EyeTracker ↔ view.GameView.
 */
public class GameApplication extends Application {

    private EyeTracker tracker;
    private GameBoard board;

    @Override
    public void start(Stage stage) {
        board   = new GameBoard();
        tracker = new EyeTracker();

        GameView view = new GameView(board, tracker);

        Scene scene = new Scene(view.getRoot(), 1000, 850);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        // Keyboard fallback
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case UP, W -> board.move(Direction.UP);
                case DOWN, S -> board.move(Direction.DOWN);
                case LEFT, A -> board.move(Direction.LEFT);
                case RIGHT, D -> board.move(Direction.RIGHT);
                case R        -> board.newGame();
                case ESCAPE   -> stage.close();
            }
        });

        stage.setTitle("2048 · Eye Control");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        board.newGame();
    }

    @Override
    public void stop() {
        if (tracker != null) tracker.stop();
        Platform.exit();
        System.exit(0);
    }
}
