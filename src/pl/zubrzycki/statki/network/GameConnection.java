package pl.zubrzycki.statki.network;

import pl.zubrzycki.statki.protocol.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class GameConnection {

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private volatile GameNetworkListener listener;
    private volatile boolean running;

    public GameConnection(Socket socket, GameNetworkListener listener) throws Exception {
        this.socket = socket;
        this.listener = listener;
        
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
        
        this.running = true;
        startReadLoop();
    }

    public synchronized void send(GameEvent event) throws Exception {
        if (out == null) return;
        out.writeObject(event);
        out.flush();
    }

    public void setListener(GameNetworkListener listener) {
        this.listener = listener;
    }

    public void close() {
        running = false;
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }

    private void startReadLoop() {
        Thread t = new Thread(() -> {
            try {
                while (running && !Thread.currentThread().isInterrupted()) {
                    Object obj = in.readObject();
                    if (listener == null) continue;

                    if (obj instanceof ShotEvent ev) {
                        listener.onShotReceived(ev);
                    } else if (obj instanceof BoostEvent ev) {
                        listener.onBoostReceived(ev);
                    } else if (obj instanceof ReadyEvent ev) {
                        listener.onReadyReceived(ev);
                    } else if (obj instanceof StartGameEvent ev) {
                        listener.onStartGameReceived(ev);
                    } else if (obj instanceof PlayerJoinEvent ev) {
                        listener.onPlayerJoinReceived(ev);
                    }
                }
            } catch (Exception e) {
                if (running && listener != null) {
                    listener.onPlayerDisconnected();
                }
            }
        }, "GameConnection-Receiver");

        t.setDaemon(true);
        t.start();
    }
}
