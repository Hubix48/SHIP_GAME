package pl.zubrzycki.statki.protocol;

import java.io.Serializable;

public record StartGameEvent(String hostName) implements GameEvent, Serializable {
    public static final String TYPE = "START_GAME";

    @Override
    public String getType() {
        return TYPE;
    }
}
