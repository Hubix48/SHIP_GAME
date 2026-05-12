package pl.zubrzycki.statki.protocol;

import java.io.Serializable;

public interface GameEvent extends Serializable {
    String getType();
}
