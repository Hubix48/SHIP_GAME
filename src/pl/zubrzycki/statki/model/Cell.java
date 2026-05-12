package pl.zubrzycki.statki.model;

public class Cell {

    private Ship ship;
    private boolean shot = false;

    public boolean hasShip() { return ship != null; }
    public void setShip(Ship ship) { this.ship = ship; }
    public Ship getShip() { return ship; }

    public boolean isShot() { return shot; }
    public void setShot(boolean shot) { this.shot = shot; }
}
