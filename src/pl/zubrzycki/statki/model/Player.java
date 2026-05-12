package pl.zubrzycki.statki.model;

public class Player {

    private String name;
    private final Board board;

    public Player(String name) {
        this.name = name;
        this.board = new Board();
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public Board getBoard() {
        return board;
    }
}
