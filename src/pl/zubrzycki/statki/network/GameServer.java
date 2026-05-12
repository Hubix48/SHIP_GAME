package pl.zubrzycki.statki.network;

import pl.zubrzycki.statki.protocol.*;
import java.net.ServerSocket;
import java.net.Socket;

public class GameServer {

    private ServerSocket serverSocket;
    private GameConnection connection;
    private GameNetworkListener listener;

    public void setListener(GameNetworkListener listener) {
        this.listener = listener;
        if (connection != null) {
            connection.setListener(listener);
        }
    }

    public void start(int port) throws Exception {
        serverSocket = new ServerSocket(port);
        System.out.println("[4/6] Oczekiwanie na połączenie gracza...");
        Socket clientSocket = serverSocket.accept();
        this.connection = new GameConnection(clientSocket, listener);
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
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
    }
}