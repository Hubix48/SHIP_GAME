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
import pl.zubrzycki.statki.network.GameClient;
import pl.zubrzycki.statki.network.GameNetworkListener;
import pl.zubrzycki.statki.network.LobbyClient;
import pl.zubrzycki.statki.protocol.*;

import java.io.IOException;

public class GuestView {

    private final BorderPane root = new BorderPane();

    private static final int DEFAULT_LOBBY_PORT = 9000;

    public GuestView(Stage stage) {
        root.getStyleClass().add("view-root");

        Text title = new Text("Dołącz do pokoju");
        title.getStyleClass().add("player-name");

        TextField nameField = new TextField("");

        TextField lobbyHostField = new TextField();
        lobbyHostField.setPromptText("np. 192.168.1.10 lub localhost");

        TextField lobbyPortField = new TextField(String.valueOf(DEFAULT_LOBBY_PORT));
        TextField roomCodeField = new TextField();
        roomCodeField.setPromptText("Kod pokoju");

        Label statusLabel = new Label();

        Button joinBtn = new Button("Dołącz");
        joinBtn.getStyleClass().add("action-button");

        Button backBtn = new Button("Powrót");
        backBtn.getStyleClass().add("action-button");
        backBtn.setOnAction(_ -> {
            MultiplayerView mpView = new MultiplayerView(stage);
            stage.getScene().setRoot(mpView.getRoot());
        });

        joinBtn.setOnAction(_ -> {
            String playerName = nameField.getText().trim();
            if (playerName.isEmpty()) playerName = "Gracz 2";

            String lobbyHost = lobbyHostField.getText().trim();
            if (lobbyHost.isEmpty()) {
                statusLabel.setText("Podaj adres IP hosta.");
                return;
            }

            int lobbyPort;
            try {
                lobbyPort = Integer.parseInt(lobbyPortField.getText().trim());
            } catch (NumberFormatException ex) {
                statusLabel.setText("Błędny port lobby.");
                return;
            }

            String code = roomCodeField.getText().trim().toUpperCase();
            if (code.isEmpty()) {
                statusLabel.setText("Podaj kod pokoju.");
                return;
            }

            final String fPlayerName = playerName;
            final String fLobbyHost = lobbyHost;
            final int fLobbyPort = lobbyPort;
            final String fCode = code;

            joinBtn.setDisable(true);
            statusLabel.setText("Łączenie z lobby...");

            new Thread(() -> {
                try {
                    LobbyClient lobbyClient = new LobbyClient(fLobbyHost, fLobbyPort);
                    LobbyClient.JoinRoomResult res = lobbyClient.joinRoom(fCode);

                    String hostIp = res.hostIp();
                    int gamePort = res.hostGamePort();

                    Platform.runLater(() -> statusLabel.setText("Łączenie z hostem..."));

                    GameClient client = createGameClient(stage, fPlayerName, statusLabel);

                    client.connect(hostIp, gamePort);

                    Platform.runLater(() -> {
                        statusLabel.setText("Połączono! Czekam na rozpoczęcie gry przez hosta...");
                        try {
                            client.send(new PlayerJoinEvent(fPlayerName));
                        } catch (Exception sendEx) {
                            System.err.println("Statki: [GuestView] Błąd wysyłania PlayerJoinEvent: " + sendEx.getMessage());
                        }
                    });

                } catch (IOException ex) {
                    System.err.println("Statki: [GuestView] Błąd: " + ex.getMessage());
                    Platform.runLater(() -> {
                        statusLabel.setText("Błąd: " + ex.getMessage());
                        joinBtn.setDisable(false);
                    });
                } catch (Exception ex) {
                    System.err.println("Statki: [GuestView] Błąd ogólny: " + ex.getMessage());
                    Platform.runLater(() -> {
                        statusLabel.setText("Nie można połączyć z hostem.");
                        joinBtn.setDisable(false);
                    });
                }
            }, "LobbyJoinRoom").start();
        });

        VBox form = new VBox(10,
                new Label("Nick gracza:"), nameField,
                new Label("Adres serwera (IP hosta):"), lobbyHostField,
                new Label("Port lobby:"), lobbyPortField,
                new Label("Kod pokoju:"), roomCodeField,
                joinBtn,
                statusLabel
        );
        form.getStyleClass().addAll("form-pane", "pane-background");

        VBox center = new VBox(30, title, form);
        center.getStyleClass().add("box-center");

        VBox bottom = new VBox(15, backBtn);
        bottom.getStyleClass().add("box-center");

        root.setCenter(center);
        root.setBottom(bottom);
    }

    private GameClient createGameClient(Stage stage, String playerName, Label statusLabel) {
        GameClient client = new GameClient();
        client.setListener(new GameNetworkListener() {
            @Override
            public void onPlayerJoinReceived(PlayerJoinEvent event) {}

            @Override
            public void onStartGameReceived(StartGameEvent event) {
                Platform.runLater(() -> {
                    Player p1 = new Player(event.hostName());
                    Player p2 = new Player(playerName);

                    GameEngine engine = new GameEngine(p1, p2);

                    GameView view = new GameView(stage, engine, null, client, null, false);
                    stage.getScene().setRoot(view.getRoot());
                });
            }

            @Override public void onShotReceived(ShotEvent event) {}
            @Override public void onBoostReceived(BoostEvent event) {}
            @Override public void onReadyReceived(ReadyEvent event) {}

            @Override
            public void onPlayerDisconnected() {
                Platform.runLater(() -> statusLabel.setText("Połączenie z hostem przerwane."));
            }
        });
        return client;
    }

    public Parent getRoot() {
        return root;
    }
}
