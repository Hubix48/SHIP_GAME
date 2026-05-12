package pl.zubrzycki.statki.model;

public enum ShipType {
    CARRIER("Lotniskowiec", 5),
    BATTLESHIP("Okręt wojenny", 4),
    DESTROYER("Niszczyciel", 3),
    CRUISER("Krążownik", 2),
    SUBMARINE("Batyskaf", 1);

    private final String name;
    private final int size;

    ShipType(String name, int size) {
        this.name = name;
        this.size = size;
    }

    public String getName() { return name; }
    public int getSize() { return size; }
}
