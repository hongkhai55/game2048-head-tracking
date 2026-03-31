package model;

public enum Direction {
    UP, DOWN, LEFT, RIGHT;

    public String arrow() {
        return switch (this) {
            case UP    -> "↑";
            case DOWN  -> "↓";
            case LEFT  -> "←";
            case RIGHT -> "→";
        };
    }
}
