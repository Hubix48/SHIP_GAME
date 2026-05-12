package pl.zubrzycki.statki.model;

import java.io.Serializable;

public class Ship implements Serializable {

    private final String name;
    private final int size;
    private int hits = 0;
    private boolean horizontal = true;

    private int x;
    private int y;

    private boolean fake = false;

    public Ship(String name, int size) {
        this.name = name;
        this.size = size;
    }

    public String getName() { return name; }
    public int getSize() { return size; }
    public boolean isHorizontal() { return horizontal; }

    public int getX() { return x; }
    public int getY() { return y; }

    public void setPosition(int x, int y, boolean horizontal) {
        this.x = x;
        this.y = y;
        this.horizontal = horizontal;
    }

    public void hit() { hits++; }
    public void resetHits() { hits = 0; }
    public boolean isSunk() { return hits >= size; }
    public boolean isFake() {
        return !fake;
    }
    public void setFake(boolean fake) {
        this.fake = fake;
    }
}