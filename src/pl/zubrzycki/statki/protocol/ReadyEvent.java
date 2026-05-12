package pl.zubrzycki.statki.protocol;

import pl.zubrzycki.statki.model.Ship;
import java.io.Serializable;
import java.util.List;

public record ReadyEvent(String playerName, List<Ship> ships) implements GameEvent, Serializable {
    public static final String TYPE = "READY";

    @Override
    public String getType() {
        return TYPE;
    }
}
