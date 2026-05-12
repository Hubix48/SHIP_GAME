package pl.zubrzycki.statki.ui;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import pl.zubrzycki.statki.engine.GameEngine;
import pl.zubrzycki.statki.model.Player;
import pl.zubrzycki.statki.network.GameServer;
import pl.zubrzycki.statki.network.LobbyClient;
import pl.zubrzycki.statki.network.LobbyServer;
import pl.zubrzycki.statki.network.GameNetworkListener;
import pl.zubrzycki.statki.protocol.StartGameEvent;
import pl.zubrzycki.statki.protocol.PlayerJoinEvent;
import pl.zubrzycki.statki.protocol.ShotEvent;
import pl.zubrzycki.statki.protocol.BoostEvent;
import pl.zubrzycki.statki.protocol.ReadyEvent;
import pl.zubrzycki.statki.protocol.*;

public class HostView {
    private final BorderPane root = new BorderPane();

    private static final int DEFAULT_LOBBY_PORT = 9000;
    private static final int DEFAULT_GAME_PORT = 5000;

    private GameServer gameServer;
    private LobbyServer lobbyServer;
    private String playerName;
    private volatile boolean clientConnected = false;
    private volatile String guestName = null;

    public HostView(Stage stage) {
        root.getStyleClass().add("view-root");

        Text title = new Text("Utwórz pokój");
        title.getStyleClass().add("player-name");

        TextField nameField = new TextField("");
        TextField lobbyPortField = new TextField(String.valueOf(DEFAULT_LOBBY_PORT));
        TextField gamePortField = new TextField(String.valueOf(DEFAULT_GAME_PORT));

        Label roomCodeLabel = new Label("Kod pokoju: ____");
        Label statusLabel = new Label();

        Button createBtn = new Button("Utwórz pokój");
        createBtn.getStyleClass().add("action-button");

        Button startGameBtn = new Button("Rozpocznij grę");
        startGameBtn.getStyleClass().add("action-button");
        startGameBtn.setDisable(true);

        Button backBtn = new Button("Powrót");
        backBtn.getStyleClass().add("action-button");
        backBtn.setOnAction(_ -> {
            if (lobbyServer != null) lobbyServer.stop();
            if (gameServer != null) gameServer.close();
            MultiplayerView mpView = new MultiplayerView(stage);
            stage.getScene().setRoot(mpView.getRoot());
        });

        createBtn.setOnAction(_ -> {
            playerName = nameField.getText().trim();
            if (playerName.isEmpty()) playerName = "Gracz 1";

            int lobbyPort, gamePort;
            try {
                lobbyPort = Integer.parseInt(lobbyPortField.getText().trim());
                gamePort = Integer.parseInt(gamePortField.getText().trim());
            } catch (NumberFormatException ex) {
                statusLabel.setText("Błędny numer portu.");
                return;
            }

            createBtn.setDisable(true);
            statusLabel.setText("Uruchamianie serwerów...");

            initializeServers(lobbyPort, gamePort, createBtn, statusLabel, roomCodeLabel, startGameBtn);
        });

        startGameBtn.setOnAction(_ -> {
            if (!clientConnected) {
                statusLabel.setText("Poczekaj aż gracz dołączy!");
                return;
            }

            if (guestName == null) guestName = "Gracz online";

            String currentName = nameField.getText().trim();
            if (!currentName.isEmpty()) {
                playerName = currentName;
            }

            try {
                gameServer.send(new StartGameEvent(playerName));
            } catch (Exception ex) {
                System.err.println("Statki: [HostView] Błąd wysyłania StartGameEvent: " + ex.getMessage());
            }

            Player p1 = new Player(playerName);
            Player p2 = new Player(guestName);

            GameEngine engine = new GameEngine(p1, p2);

            GameView view = new GameView(stage, engine, gameServer, null, lobbyServer, true);
            stage.getScene().setRoot(view.getRoot());
        });

        VBox form = new VBox(10,
                new Label("Nick gracza:"), nameField,
                new Label("Port lobby:"), lobbyPortField,
                new Label("Port gry:"), gamePortField,
                createBtn,
                roomCodeLabel,
                statusLabel,
                startGameBtn
        );
        form.getStyleClass().addAll("form-pane", "pane-background");

        VBox center = new VBox(30, title, form);
        center.getStyleClass().add("box-center");

        VBox bottom = new VBox(15, backBtn);
        bottom.getStyleClass().add("box-center");

        root.setCenter(center);
        root.setBottom(bottom);
    }

    private void initializeServers(int lobbyPort, int gamePort, Button createBtn, Label statusLabel, Label roomCodeLabel, Button startGameBtn) {
        lobbyServer = new LobbyServer(lobbyPort);
        lobbyServer.startAsync();

        gameServer = new GameServer();
        Thread gameServerThread = createGameServerThread(gamePort, createBtn, statusLabel);
        gameServerThread.start();

        gameServer.setListener(new GameNetworkListener() {
            @Override
            public void onPlayerJoinReceived(PlayerJoinEvent event) {
                guestName = event.playerName();
                Platform.runLater(() -> {
                    statusLabel.setText("Gracz " + guestName + " dołączył! Możesz rozpocząć grę.");
                    startGameBtn.setDisable(false);
                });
            }
            @Override public void onShotReceived(ShotEvent event) {}
            @Override public void onBoostReceived(BoostEvent event) {}
            @Override public void onReadyReceived(ReadyEvent event) {}
            @Override public void onStartGameReceived(StartGameEvent event) {}
            @Override
            public void onPlayerDisconnected() {
                clientConnected = false;
                guestName = null;
                Platform.runLater(() -> {
                    statusLabel.setText("Gracz rozłączony. Czekam na gracza...");
                    startGameBtn.setDisable(true);
                });
            }
        });

        new Thread(() -> {
            try {
                Thread.sleep(200);
                LobbyClient lobbyClient = new LobbyClient("localhost", lobbyPort);
                LobbyClient.CreateRoomResult res = lobbyClient.createRoom(gamePort);
                String code = res.roomCode();
                Platform.runLater(() -> {
                    roomCodeLabel.setText("Kod pokoju: " + code);
                    statusLabel.setText("Pokój utworzony. Czekam na gracza...");
                });
            } catch (Exception ex) {
                System.err.println("Statki: [HostView] Błąd lobby: " + ex.getMessage());
                Platform.runLater(() -> {
                    statusLabel.setText("Błąd lobby: " + ex.getMessage());
                    createBtn.setDisable(false);
                });
            }
        }, "LobbyCreateRoom").start();
    }

    private Thread createGameServerThread(int gamePort, Button createBtn, Label statusLabel) {
        Thread thread = new Thread(() -> {
            try {
                gameServer.start(gamePort);
                clientConnected = true;
            } catch (Exception ex) {
                System.err.println("Statki: [HostView] Błąd serwera gry: " + ex.getMessage());
                Platform.runLater(() -> {
                    statusLabel.setText("Błąd serwera: " + ex.getMessage());
                    createBtn.setDisable(false);
                });
            }
        }, "GameServer-Main");
        thread.setDaemon(true);
        return thread;
    }

    public Parent getRoot() {
        return root;
    }
}
