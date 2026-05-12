package pl.zubrzycki.statki.protocol;

import java.io.Serializable;

public record ShotEvent(int x, int y, boolean hit, boolean sunk, String shipName) implements GameEvent, Serializable {
    public static final String TYPE = "SHOT";

    @Override
    public String getType() {
        return TYPE;
    }
}
