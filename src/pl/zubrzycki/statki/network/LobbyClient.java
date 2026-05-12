package pl.zubrzycki.statki.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public record LobbyClient(String lobbyHost, int lobbyPort) {
    public record CreateRoomResult(String roomCode) { }
    public record JoinRoomResult(String hostIp, int hostGamePort) { }

    public CreateRoomResult createRoom(int gamePort) throws IOException {
        try (Socket socket = new Socket(lobbyHost, lobbyPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        ) {
            out.println("CREATE " + gamePort);

            String resp = in.readLine();
            if (resp == null)
                throw new IOException("Brak odpowiedzi z lobby");

            if (resp.startsWith("ROOM "))
                return new CreateRoomResult(resp.substring(5).trim());

            throw new IOException(resp);
        }
    }

    public JoinRoomResult joinRoom(String code) throws IOException {
        try (Socket socket = new Socket(lobbyHost, lobbyPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        ) {
            System.out.println("[1/6] Łączenie z serwerem lobby...");
            System.out.println("[2/6] Wysłano żądanie dołączenia do pokoju: " + code);
            out.println("JOIN " + code);

            String resp = in.readLine();
            if (resp == null)
                throw new IOException("Brak odpowiedzi z lobby");

            if (resp.startsWith("HOST ")) {
                String[] p = resp.split("\\s+");
                if (p.length != 3)
                    throw new IOException("Zły format HOST: " + resp);

                String hostIp = p[1];
                int hostPort = Integer.parseInt(p[2]);
                System.out.println("[3/6] Otrzymano adres hosta: " + hostIp + ":" + hostPort);
                return new JoinRoomResult(hostIp, hostPort);
            }

            throw new IOException(resp);
        }
    }
}
