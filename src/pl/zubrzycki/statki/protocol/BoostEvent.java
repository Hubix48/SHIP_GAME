package pl.zubrzycki.statki.protocol;

import java.io.Serializable;

public record BoostEvent(Kind kind, int x, int y, int size, boolean horizontal) implements GameEvent, Serializable {
    public static final String TYPE = "BOOST";

    public enum Kind {
        RADAR,
        SWAP_SELECT,
        SWAP_MOVE,
        TROLL_PLACE
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
