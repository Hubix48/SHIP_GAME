package pl.zubrzycki.statki.ui;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.scene.shape.Rectangle;
import pl.zubrzycki.statki.model.Board;
import pl.zubrzycki.statki.model.Ship;

public class BoardView {

    private final Board board;
    private final GridPane gridPane = new GridPane();
    private final boolean targetView;

    public BoardView(Board board, boolean targetView) {
        this.board = board;
        this.targetView = targetView;
        createGrid();
    }

    public BoardView(Board board) {
        this(board, false);
    }

    public GridPane getGridPane() {
        return gridPane;
    }

    private void createGrid() {
        gridPane.setHgap(2);
        gridPane.setVgap(2);

        for (int y = 0; y < 11; y++) {
            for (int x = 0; x < 11; x++) {

                if (x == 0 && y == 0) {
                    continue;
                }

                if (y == 0) {
                    Text t = new Text(String.valueOf(x));
                    GridPane.setHalignment(t, HPos.CENTER);
                    GridPane.setValignment(t, VPos.CENTER);
                    gridPane.add(t, x, y);
                    continue;
                }

                if (x == 0) {
                    char row = (char) ('A' + (y - 1));
                    Text t = new Text(String.valueOf(row));
                    GridPane.setHalignment(t, HPos.CENTER);
                    GridPane.setValignment(t, VPos.CENTER);
                    gridPane.add(t, x, y);
                    continue;
                }

                Rectangle r = new Rectangle(40, 40);
                r.getStyleClass().add("cell");

                if (targetView) {
                    r.getStyleClass().add("cell-target");
                }

                GridPane.setHalignment(r, HPos.CENTER);
                GridPane.setValignment(r, VPos.CENTER);
                gridPane.add(r, x, y);
            }
        }

        update();
    }

    private void resetCellStyle(Rectangle r) {
        r.getStyleClass().removeAll(
                "cell-water",
                "cell-ship",
                "cell-hit",
                "cell-miss",
                "cell-preview-ok",
                "cell-preview-bad"
        );
    }

    public void update() {
        for (Node n : gridPane.getChildren()) {

            Integer gx = GridPane.getColumnIndex(n);
            Integer gy = GridPane.getRowIndex(n);

            if (gx == null || gy == null) continue;

            int x = gx - 1;
            int y = gy - 1;

            if (x < 0 || y < 0) continue;
            if (!(n instanceof Rectangle r)) continue;

            resetCellStyle(r);

            var cell = board.getCell(x, y);

            if (cell.isShot()) {
                if (cell.hasShip()) {
                    r.getStyleClass().add("cell-hit");
                } else {
                    r.getStyleClass().add("cell-miss");
                }
            } else {
                if (targetView) {
                    r.getStyleClass().add("cell-water");
                } else {
                    if (cell.hasShip()) {
                        r.getStyleClass().add("cell-ship");
                    } else {
                        r.getStyleClass().add("cell-water");
                    }
                }
            }
        }
    }

    public void showPreview(Ship ship, int startX, int startY, boolean horizontal, boolean canPlace) {

        update();

        int size = ship.getSize();

        for (int i = 0; i < size; i++) {

            int x = horizontal ? startX + i : startX;
            int y = horizontal ? startY : startY + i;

            if (x < 0 || x >= 10 || y < 0 || y >= 10) continue;

            int gridX = x + 1;
            int gridY = y + 1;

            for (Node n : gridPane.getChildren()) {
                Integer gx = GridPane.getColumnIndex(n);
                Integer gy = GridPane.getRowIndex(n);
                if (gx == null || gy == null) continue;
                if (!(n instanceof Rectangle r)) continue;

                if (gx == gridX && gy == gridY) {
                    resetCellStyle(r);
                    r.getStyleClass().add(canPlace ? "cell-preview-ok" : "cell-preview-bad");
                }
            }
        }
    }

    public void clearPreview() {
        update();
    }

    public Button shipButton(Ship s) {
        Button b = new Button(s.getName() + " (" + s.getSize() + ")");
        b.getStyleClass().addAll("ship-button", "action-button");
        return b;
    }
}
