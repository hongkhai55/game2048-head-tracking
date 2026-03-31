package model;

import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pure game logic: 4x4 grid, merge rules, scoring.
 * No UI dependencies — fully testable.
 */
public class GameBoard {

    public GameBoard() {
        loadHighScore(); // Tải điểm Highscore khi vừa mở game
        newGame();
    }

    public static final int SIZE = 4;

    private final int[][] grid = new int[SIZE][SIZE];
    private int score = 0;
    private int best = 0;
    private boolean gameOver = false;
    private boolean won = false;
    private final Random rng = new Random();

    // Listeners
    private final List<GameListener> listeners = new ArrayList<>();

    public interface GameListener {
        void onBoardChanged(int[][] grid, int score, int best);
        void onGameOver(int score);
        void onWin(int score);
    }

    public void addListener(GameListener l) { listeners.add(l); }

    // ── Initialise ────────────────────────────────────────────────
    public void newGame() {
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                grid[r][c] = 0;
        score = 0;
        gameOver = false;
        won = false;
        spawnTile();
        spawnTile();
        notifyChanged();
    }

    // ── Spawn a 2 (90%) or 4 (10%) in a random empty cell ────────
    public boolean spawnTile() {
        List<int[]> empty = new ArrayList<>();
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (grid[r][c] == 0) empty.add(new int[]{r, c});
        if (empty.isEmpty()) return false;
        int[] pos = empty.get(rng.nextInt(empty.size()));
        grid[pos[0]][pos[1]] = rng.nextDouble() < 0.9 ? 2 : 4;
        return true;
    }

    // ── Move ──────────────────────────────────────────────────────
    public boolean move(Direction dir) {
        if (gameOver) return false;
        boolean changed = false;
        int gained = 0;

        switch (dir) {
            case LEFT -> { for (int r = 0; r < SIZE; r++) { MergeResult mr = mergeLine(getRow(r));    setRow(r, mr.line); gained += mr.score; if (mr.changed) changed = true; } }
            case RIGHT -> { for (int r = 0; r < SIZE; r++) { MergeResult mr = mergeLineReversed(getRow(r));    setRow(r, mr.line); gained += mr.score; if (mr.changed) changed = true; } }
            case UP -> { for (int c = 0; c < SIZE; c++) { MergeResult mr = mergeLine(getCol(c));    setCol(c, mr.line); gained += mr.score; if (mr.changed) changed = true; } }
            case DOWN -> { for (int c = 0; c < SIZE; c++) { MergeResult mr = mergeLineReversed(getCol(c)); setCol(c, mr.line); gained += mr.score; if (mr.changed) changed = true; } }
        }

        if (!changed) return false;

        // Cập nhật điểm
        score += gained;

        // Kiểm tra kỷ lục và lưu file ngay lập tức
        if (score > best) {
            best = score;
            saveHighScore();
        }

        spawnTile();
        notifyChanged();
        checkEndConditions();
        return true;
    }

    // ── Merge one line left ───────────────────────────────────────
    private MergeResult mergeLine(int[] line) {
        int[] vals = compact(line);
        int[] out = new int[SIZE];
        int score = 0;
        boolean changed = false;
        int idx = 0;
        for (int i = 0; i < vals.length; i++) {
            if (i + 1 < vals.length && vals[i] == vals[i + 1]) {
                out[idx++] = vals[i] * 2;
                score += vals[i] * 2;
                i++;
            } else {
                out[idx++] = vals[i];
            }
        }
        for (int i = 0; i < SIZE; i++) if (out[i] != line[i]) { changed = true; break; }
        return new MergeResult(out, score, changed);
    }

    private MergeResult mergeLineReversed(int[] line) {
        int[] rev = reverse(line);
        MergeResult mr = mergeLine(rev);
        mr.line = reverse(mr.line);
        return mr;
    }

    private int[] compact(int[] line) {
        return java.util.Arrays.stream(line).filter(v -> v != 0).toArray();
    }

    private int[] reverse(int[] a) {
        int[] r = new int[a.length];
        for (int i = 0; i < a.length; i++) r[i] = a[a.length - 1 - i];
        return r;
    }

    // ── Row / Col helpers ─────────────────────────────────────────
    private int[] getRow(int r) { return grid[r].clone(); }
    private void  setRow(int r, int[] v) { System.arraycopy(v, 0, grid[r], 0, SIZE); }
    private int[] getCol(int c) { int[] col = new int[SIZE]; for (int r = 0; r < SIZE; r++) col[r] = grid[r][c]; return col; }
    private void  setCol(int c, int[] v) { for (int r = 0; r < SIZE; r++) grid[r][c] = v[r]; }

    // ── End conditions ────────────────────────────────────────────
    private void checkEndConditions() {
        if (!won) {
            outer:
            for (int r = 0; r < SIZE; r++)
                for (int c = 0; c < SIZE; c++)
                    if (grid[r][c] == 2048) { won = true; listeners.forEach(l -> l.onWin(score)); break outer; }
        }
        if (!canMove()) {
            gameOver = true;
            int s = score;
            listeners.forEach(l -> l.onGameOver(s));
        }
    }

    private boolean canMove() {
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] == 0) return true;
                if (c < SIZE - 1 && grid[r][c] == grid[r][c + 1]) return true;
                if (r < SIZE - 1 && grid[r][c] == grid[r + 1][c]) return true;
            }
        return false;
    }

    // ── Accessors ─────────────────────────────────────────────────
    public int[][] getGrid() {
        int[][] copy = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) copy[r] = grid[r].clone();
        return copy;
    }
    public int  getScore()   { return score; }
    public int  getBest()    { return best; }
    public boolean isOver()  { return gameOver; }
    public boolean isWon()   { return won; }

    private void notifyChanged() {
        int[][] snap = getGrid();
        listeners.forEach(l -> l.onBoardChanged(snap, score, best));
    }

    // ── Inner types ───────────────────────────────────────────────
    private static class MergeResult {
        int[] line; int score; boolean changed;
        MergeResult(int[] line, int score, boolean changed) { this.line = line; this.score = score; this.changed = changed; }
    }

    // ── Xử lý File I/O (Lưu điểm Highscore) ───────────────────────

    // 1. Hàm đọc điểm từ file khi mở game
    private void loadHighScore() {
        try {
            File file = new File("highscore.txt");
            if (file.exists()) {
                Scanner scanner = new Scanner(file);
                if (scanner.hasNextInt()) {
                    best = scanner.nextInt();
                }
                scanner.close();
            } else {
                best = 0;
            }
        } catch (Exception e) {
            System.err.println("Lỗi đọc file Highscore: " + e.getMessage());
            best = 0;
        }
    }

    // 2. Hàm ghi đè điểm mới xuống file
    private void saveHighScore() {
        try {
            FileWriter writer = new FileWriter("highscore.txt");
            writer.write(String.valueOf(best));
            writer.close();
        } catch (Exception e) {
            System.err.println("Lỗi ghi file Highscore: " + e.getMessage());
        }
    }
}