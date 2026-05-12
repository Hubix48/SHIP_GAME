package pl.zubrzycki.statki.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

public class BotAI {

    private final BotDifficulty difficulty;
    private final Random rand = new Random();

    private final Deque<int[]> huntTargets = new ArrayDeque<>();

    public BotAI(BotDifficulty diff) {
        this.difficulty = diff;
    }

    private boolean inside(int x, int y) {
        return x >= 0 && x < 10 && y >= 0 && y < 10;
    }

    private int[] huntAround(Board board) {

        if (huntTargets.isEmpty()) return null;

        int[] base = huntTargets.getFirst();
        int bx = base[0];
        int by = base[1];

        int[][] dirs = {{1,0}, {-1,0}, {0,1}, {0,-1}};

        for (int[] d : dirs) {
            int nx = bx + d[0];
            int ny = by + d[1];

            if (inside(nx, ny) && !board.getCell(nx, ny).isShot()) {
                return new int[]{nx, ny};
            }
        }

        huntTargets.removeFirst();
        return null;
    }

    public int[] chooseShot(Board board) {
        return switch (difficulty) {
            case EASY -> randomShot(board);
            case NORMAL -> normalShot(board);
            case HARD -> hardShot(board);
        };
    }

    private int[] randomShot(Board board) {
        while (true) {
            int x = rand.nextInt(10);
            int y = rand.nextInt(10);
            if (!board.getCell(x, y).isShot()) {
                return new int[]{x, y};
            }
        }
    }

    private int[] normalShot(Board board) {

        int[] hunt = huntAround(board);
        if (hunt != null) return hunt;

        return randomShot(board);
    }

    private int[] hardShot(Board board) {

        int[] hunt = huntAround(board);
        if (hunt != null) return hunt;

        List<int[]> candidates = new ArrayList<>();
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                if (!board.getCell(x, y).isShot() && ((x + y) % 2 == 0))
                    candidates.add(new int[]{x, y});
            }
        }

        if (!candidates.isEmpty()) {
            return candidates.get(rand.nextInt(candidates.size()));
        }

        return randomShot(board);
    }

    public void notifyHit(int x, int y) {
        huntTargets.add(new int[]{x, y});
    }
}