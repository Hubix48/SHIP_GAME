package pl.zubrzycki.statki.network;

import pl.zubrzycki.statki.protocol.BoostEvent;
import pl.zubrzycki.statki.protocol.ReadyEvent;
import pl.zubrzycki.statki.protocol.ShotEvent;
import pl.zubrzycki.statki.protocol.StartGameEvent;
import pl.zubrzycki.statki.protocol.PlayerJoinEvent;

public interface GameNetworkListener {
    void onShotReceived(ShotEvent event);
    void onBoostReceived(BoostEvent event);
    void onReadyReceived(ReadyEvent event);
    void onStartGameReceived(StartGameEvent event);
    void onPlayerJoinReceived(PlayerJoinEvent event);
    void onPlayerDisconnected();
}
