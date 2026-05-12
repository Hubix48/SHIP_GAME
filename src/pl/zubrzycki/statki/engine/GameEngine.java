package pl.zubrzycki.statki.engine;

import pl.zubrzycki.statki.model.Player;
import pl.zubrzycki.statki.model.Board;

public class GameEngine {

    private final GameState state;

    public GameEngine(Player p1, Player p2) {
        this.state = new GameState(p1, p2);
    }

    public GameState getState() {
        return state;
    }

    public boolean shoot(int x, int y) {
        Player opponent = state.getOtherPlayer();
        Board board = opponent.getBoard();
        return board.shoot(x, y);
    }

    public boolean isGameFinished() {
        return state.getPlayer1().getBoard().allShipsSunk()
                || state.getPlayer2().getBoard().allShipsSunk();
    }

    public Player getWinner() {
        if (state.getPlayer1().getBoard().allShipsSunk()) return state.getPlayer2();
        if (state.getPlayer2().getBoard().allShipsSunk()) return state.getPlayer1();
        return null;
    }
}
