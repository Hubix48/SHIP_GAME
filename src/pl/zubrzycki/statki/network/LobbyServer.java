package pl.zubrzycki.statki.network;

import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class LobbyServer {

    private final int port;
    private volatile boolean running = false;
    private ServerSocket serverSocket;

    private final Map<String, RoomInfo> rooms = new ConcurrentHashMap<>();

    public LobbyServer(int port) {
        this.port = port;
    }

    public void startAsync() {
        Thread t = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                running = true;
                while (running) {
                    Socket client = serverSocket.accept();
                    new Thread(() -> handleClient(client), "LobbyClientHandler").start();
                }
            } catch (IOException e) {

            }
        }, "LobbyServer-Main");

        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
    }

    private void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String line = in.readLine();
            if (line == null) return;

            String[] parts = line.trim().split(" ");
            if (parts.length < 2) {
                out.println("ERROR BAD_REQUEST");
                return;
            }

            String cmd = parts[0].toUpperCase();

            if (cmd.equals("CREATE")) {
                int gamePort = Integer.parseInt(parts[1]);

                String code = generateRoomCode();
                rooms.put(code, new RoomInfo(gamePort));

                out.println("ROOM " + code);
                return;
            }

            if (cmd.equals("JOIN")) {
                String code = parts[1].toUpperCase();

                RoomInfo info = rooms.get(code);
                if (info == null) {
                    out.println("Podany kod jest nieprawidłowy.");
                    return;
                }

                String hostIp = InetAddress.getLocalHost().getHostAddress();

                out.println("HOST " + hostIp + " " + info.gamePort);

                rooms.remove(code);
                return;
            }

            out.println("ERROR UNKNOWN_CMD");

        } catch (Exception e) {}
    }

    private record RoomInfo(int gamePort) {
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random r = new Random();
        String code;
        do {
            StringBuilder sb = new StringBuilder(4);
            for (int i = 0; i < 4; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
            code = sb.toString();
        } while (rooms.containsKey(code));
        return code;
    }
}
