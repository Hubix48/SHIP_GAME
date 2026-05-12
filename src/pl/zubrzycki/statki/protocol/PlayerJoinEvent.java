package pl.zubrzycki.statki.protocol;

import java.io.Serializable;

public record PlayerJoinEvent(String playerName) implements GameEvent, Serializable {
    
    @Override
    public String getType() {
        return "PLAYER_JOIN";
    }
}
