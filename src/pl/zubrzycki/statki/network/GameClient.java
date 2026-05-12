package pl.zubrzycki.statki.network;

import pl.zubrzycki.statki.protocol.*;
import java.net.Socket;

public class GameClient {

    private GameConnection connection;
    private GameNetworkListener listener;

    public void setListener(GameNetworkListener listener) {
        this.listener = listener;
        if (connection != null) {
            connection.setListener(listener);
        }
    }

    public void connect(String host, int port) throws Exception {
        System.out.println("[4/6] Łączenie z hostem " + host + ":" + port + "...");
        Socket socket = new Socket(host, port);
        System.out.println("[5/6] Połączono");
        this.connection = new GameConnection(socket, listener);
    }

    public void send(GameEvent event) throws Exception {
        if (connection != null) {
            connection.send(event);
        }
    }

    public void close() {
        if (connection != null) {
            connection.close();
        }
    }
}
