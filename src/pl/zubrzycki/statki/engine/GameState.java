package pl.zubrzycki.statki.engine;

import pl.zubrzycki.statki.model.Player;

public class GameState {

    private final Player player1;
    private final Player player2;
    private PlayerTurn turn = PlayerTurn.PLAYER1;
    private boolean boostsEnabled = true;

    public GameState(Player p1, Player p2) {
        this.player1 = p1;
        this.player2 = p2;
    }

    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }

    public Player getCurrentPlayer() {
        return turn == PlayerTurn.PLAYER1 ? player1 : player2;
    }
    public Player getOtherPlayer() {
        return turn == PlayerTurn.PLAYER1 ? player2 : player1;
    }
    public void swapTurn() {
        turn = (turn == PlayerTurn.PLAYER1) ? PlayerTurn.PLAYER2 : PlayerTurn.PLAYER1;
    }
    public void setTurn(PlayerTurn turn) {
        this.turn = turn;
    }
    public boolean isBoostsEnabled() {
        return boostsEnabled;
    }
    public void setBoostsEnabled(boolean enabled) {
        this.boostsEnabled = enabled;
    }
}