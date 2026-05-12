package pl.zubrzycki.statki.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Board {

    private final Cell[][] grid = new Cell[10][10];

    public Board() {
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                grid[y][x] = new Cell();
            }
        }
    }

    public Cell getCell(int x, int y) {
        return grid[y][x];
    }

    public boolean canPlaceShipWithSpacing(int startX, int startY, int size, boolean horizontal) {
        if (horizontal) {
            if (startX + size > 10) return false;
        } else {
            if (startY + size > 10) return false;
        }

        for (int i = -1; i <= size; i++) {
            for (int j = -1; j <= 1; j++) {
                int xi = horizontal ? startX + i : startX + j;
                int yi = horizontal ? startY + j : startY + i;
                if (xi >= 0 && xi < 10 && yi >= 0 && yi < 10) {
                    if (grid[yi][xi].hasShip()) return false;
                }
            }
        }
        return true;
    }

    public boolean placeShip(Ship ship, int startX, int startY, boolean horizontal) {
        int size = ship.getSize();
        if (!canPlaceShipWithSpacing(startX, startY, size, horizontal)) return false;

        if (horizontal) {
            for (int i = 0; i < size; i++) {
                grid[startY][startX + i].setShip(ship);
            }
        } else {
            for (int i = 0; i < size; i++) {
                grid[startY + i][startX].setShip(ship);
            }
        }

        ship.setPosition(startX, startY, horizontal);
        return true;
    }

    public void removeShip(Ship ship) {
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                Cell c = grid[y][x];
                if (c.getShip() == ship) {
                    c.setShip(null);
                }
            }
        }
    }

    public void clear() {
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                grid[y][x].setShip(null);
                grid[y][x].setShot(false);
            }
        }
    }

    public boolean shoot(int x, int y) {
        Cell c = getCell(x, y);
        if (c.isShot()) return false;
        c.setShot(true);

        if (c.hasShip()) {
            c.getShip().hit();
            return true;
        }
        return false;
    }

    public boolean allShipsSunk() {
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                Cell c = grid[y][x];
                if (c.hasShip()) {
                    Ship s = c.getShip();
                    if (s.isFake() && !s.isSunk()) return false;
                }
            }
        }
        return true;
    }

    public Cell revealRandomShipCell() {
        List<Cell> shipCells = new ArrayList<>();

        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                if (grid[y][x].hasShip()) {
                    shipCells.add(grid[y][x]);
                }
            }
        }

        if (shipCells.isEmpty()) return null;

        return shipCells.get(new Random().nextInt(shipCells.size()));
    }

    public boolean moveShip(Ship ship, int newX, int newY, boolean horizontal) {
        if (ship.isSunk()) return false;

        int size = ship.getSize();

        if (!canPlaceShipWithSpacing(newX, newY, size, horizontal)) {
            return false;
        }

        removeShip(ship);

        boolean ok = placeShip(ship, newX, newY, horizontal);
        if (!ok) {
            placeShip(ship, ship.getX(), ship.getY(), ship.isHorizontal());
            return false;
        }

        ship.resetHits();

        if (horizontal) {
            for (int i = 0; i < size; i++) {
                grid[newY][newX + i].setShot(false);
            }
        } else {
            for (int i = 0; i < size; i++) {
                grid[newY + i][newX].setShot(false);
            }
        }

        return true;
    }

    public Ship placeFakeShip(int size, int x, int y, boolean horizontal) {
        if (!canPlaceShipWithSpacing(x, y, size, horizontal)) return null;

        Ship fakeShip = new Ship("TROLL", size);
        fakeShip.setFake(true);
        placeShip(fakeShip, x, y, horizontal);

        return fakeShip;
    }

    public List<Ship> getShips() {
        List<Ship> ships = new ArrayList<>();
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                Cell c = grid[y][x];
                if (c.hasShip()) {
                    Ship s = c.getShip();
                    if (!ships.contains(s)) {
                        ships.add(s);
                    }
                }
            }
        }
        return ships;
    }
}