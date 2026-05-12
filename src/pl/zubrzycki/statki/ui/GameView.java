package pl.zubrzycki.statki.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import pl.zubrzycki.statki.engine.GameEngine;
import pl.zubrzycki.statki.engine.PlayerTurn;
import pl.zubrzycki.statki.model.*;
import pl.zubrzycki.statki.network.*;
import pl.zubrzycki.statki.protocol.*;
import pl.zubrzycki.statki.storage.HistoryStorage;

import java.util.*;

public class GameView implements GameNetworkListener {
    @Override
    public void onPlayerJoinReceived(PlayerJoinEvent event) {}

    private boolean inputLocked = false;

    private final BorderPane root = new BorderPane();
    private final GameEngine engine;
    private final Stage stage;

    private Player placingPlayer;
    private List<Ship> ships;
    private Ship selectedShip;
    private boolean horizontal = true;

    private BoardView p1PlacementView;
    private BoardView p2PlacementView;
    private BoardView p1TargetView;
    private BoardView p2TargetView;

    private final VBox historyBox = new VBox(5);
    private final ScrollPane historyScroll = new ScrollPane();

    private final HistoryStorage history = new HistoryStorage();

    private final VBox warningBox = new VBox();
    private final Text warningText = new Text("Musisz ustawić wszystkie statki!");

    private final Map<Player, Integer> usedBoostCount = new HashMap<>();
    private final Map<Player, Set<BoostType>> usedBoosts = new HashMap<>();

    private boolean swapMode = false;
    private Ship swapTarget = null;

    private final GameServer netServer;
    private final GameClient netClient;
    private final LobbyServer lobbyServer;
    private final boolean onlineMode;
    private final boolean isHost;

    private boolean myReady = false;
    private boolean opponentReady = false;
    private volatile boolean gameEnded = false;

    public GameView(Stage stage, GameEngine engine) {
        this(stage, engine, null, null, null, true);
    }

    public GameView(Stage stage,
                    GameEngine engine,
                    GameServer netServer,
                    GameClient netClient,
                    LobbyServer lobbyServer,
                    boolean isHost) {

        this.stage = stage;
        this.engine = engine;
        this.netServer = netServer;
        this.netClient = netClient;
        this.lobbyServer = lobbyServer;
        this.isHost = isHost;

        this.onlineMode = (netServer != null || netClient != null);

        if (onlineMode) {
            placingPlayer = isHost ? engine.getState().getPlayer1() : engine.getState().getPlayer2();
        } else {
            placingPlayer = engine.getState().getPlayer1();
        }

        setupHistoryPane();
        setupWarningPane();
        initBoostState();

        if (netServer != null) netServer.setListener(this);
        if (netClient != null) netClient.setListener(this);

        showPlacementScreen();
    }

    public Parent getRoot() {
        return root;
    }

    private void setupHistoryPane() {
        historyScroll.setContent(historyBox);
        historyScroll.setFitToWidth(true);
        historyScroll.getStyleClass().addAll("history-pane", "history-scroll");
        historyBox.getStyleClass().add("history-pane");

        historyBox.heightProperty().addListener((_, _, _) -> historyScroll.setVvalue(1.0));
    }

    private void setupWarningPane() {
        warningText.getStyleClass().add("warning-text");
        warningBox.getChildren().add(warningText);
        warningBox.getStyleClass().add("warning-box");
        warningBox.setVisible(false);
    }

    private void initBoostState() {
        Player p1 = engine.getState().getPlayer1();
        Player p2 = engine.getState().getPlayer2();

        usedBoostCount.put(p1, 0);
        usedBoostCount.put(p2, 0);

        usedBoosts.put(p1, EnumSet.noneOf(BoostType.class));
        usedBoosts.put(p2, EnumSet.noneOf(BoostType.class));
    }

    private void addHistoryEntry(String text) {
        history.add(text);

        Text t = new Text(text);
        t.getStyleClass().add("history-entry");
        historyBox.getChildren().add(t);
    }

    private List<Ship> createDefaultShips() {
        return Arrays.stream(ShipType.values())
                .map(type -> new Ship(type.getName(), type.getSize()))
                .toList();
    }

    private void resetShips() {
        ships = new ArrayList<>(createDefaultShips());
        selectedShip = ships.getFirst();
    }

    private void autoPlaceForCurrentPlayer() {
        var board = placingPlayer.getBoard();
        board.clear();

        List<Ship> autoShips = createDefaultShips();
        Random r = new Random();

        for (Ship s : autoShips) {
            boolean placed = false;
            while (!placed) {
                boolean horiz = r.nextBoolean();
                int x = r.nextInt(10);
                int y = r.nextInt(10);
                if (board.placeShip(s, x, y, horiz)) placed = true;
            }
        }
    }

    private void showPlacementScreen() {

        resetShips();
        warningBox.setVisible(false);

        if (placingPlayer instanceof BotPlayer) {
            autoPlaceForCurrentPlayer();
            ships.clear();
            selectedShip = null;
            finishPlacement();
            return;
        }

        BoardView view = new BoardView(placingPlayer.getBoard());
        if (placingPlayer == engine.getState().getPlayer1()) {
            p1PlacementView = view;
        } else {
            p2PlacementView = view;
        }

        attachPlacementHandlers(view);

        Text title = new Text("Ustaw statki – " + placingPlayer.getName());
        title.getStyleClass().addAll("player-name", "player-name-placement");

        VBox center = new VBox(10, title, view.getGridPane());
        center.getStyleClass().add("box-center");

        HBox centerWrapper = new HBox(center);
        centerWrapper.getStyleClass().add("box-center");
        centerWrapper.setFillHeight(true);
        centerWrapper.setPrefWidth(Double.MAX_VALUE);

        rebuildShipPanel(view);

        root.setCenter(centerWrapper);
    }

    private void selectShipFromButton(Ship ship, VBox rightPanel) {
        selectedShip = ship;

        for (Node n : rightPanel.getChildren()) {
            if (n instanceof Button b) {
                Object ud = b.getUserData();
                if (ud instanceof Ship s) {
                    b.getStyleClass().remove("ship-button-selected");
                    if (s == ship) b.getStyleClass().add("ship-button-selected");
                }
            }
        }
    }

    private void attachPlacementHandlers(BoardView view) {

        var board = placingPlayer.getBoard();

        for (Node n : view.getGridPane().getChildren()) {

            n.setOnMouseEntered(_ -> {

                Integer gx = GridPane.getColumnIndex(n);
                Integer gy = GridPane.getRowIndex(n);

                if (gx == null || gy == null) {
                    view.clearPreview();
                    return;
                }

                int x = gx - 1;
                int y = gy - 1;

                if (x < 0 || y < 0) {
                    view.clearPreview();
                    return;
                }

                var cell = board.getCell(x, y);

                if (cell.hasShip()) {
                    highlightWholeShip(view, cell.getShip());
                    return;
                }

                if (selectedShip == null) {
                    view.clearPreview();
                    return;
                }

                boolean can = board.canPlaceShipWithSpacing(
                        x, y,
                        selectedShip.getSize(),
                        horizontal
                );

                view.showPreview(selectedShip, x, y, horizontal, can);
            });

            n.setOnMouseExited(_ -> view.clearPreview());

            n.setOnMouseClicked(_ -> {

                Integer gx = GridPane.getColumnIndex(n);
                Integer gy = GridPane.getRowIndex(n);
                if (gx == null || gy == null) return;

                int x = gx - 1;
                int y = gy - 1;

                if (x < 0 || y < 0) return;

                var cell = board.getCell(x, y);

                if (cell.hasShip()) {

                    Ship ship = cell.getShip();
                    board.removeShip(ship);

                    if (!ships.contains(ship)) ships.add(ship);
                    selectedShip = ship;

                    view.update();
                    rebuildShipPanel(view);
                    return;
                }

                if (selectedShip == null) return;

                boolean ok = board.placeShip(selectedShip, x, y, horizontal);

                if (ok) {
                    ships.remove(selectedShip);
                    selectedShip = ships.isEmpty() ? null : ships.getFirst();

                    view.update();
                    rebuildShipPanel(view);
                    if (ships.isEmpty()) warningBox.setVisible(false);
                }
            });
        }
    }

    private void rebuildShipPanel(BoardView view) {
        VBox shipList = new VBox(10);
        shipList.getStyleClass().add("box-center");
        shipList.getStyleClass().add("ship-list");

        for (Ship s : ships) {
            Button b = view.shipButton(s);
            b.setUserData(s);
            if (!b.getStyleClass().contains("ship-button"))
                b.getStyleClass().add("ship-button");

            if (s == selectedShip)
                b.getStyleClass().add("ship-button-selected");
            else
                b.getStyleClass().remove("ship-button-selected");

            b.setOnAction(_ -> selectShipFromButton(s, shipList));
            shipList.getChildren().add(b);
        }

        VBox bottomButtons = createPlacementControlButtons(view);

        VBox bottomArea = new VBox(10, bottomButtons, warningBox);
        bottomArea.getStyleClass().add("box-center");

        BorderPane side = new BorderPane();
        side.setTop(shipList);
        side.setBottom(bottomArea);

        Separator sep = new Separator();
        sep.setOrientation(javafx.geometry.Orientation.VERTICAL);

        BorderPane wrapper = new BorderPane();
        wrapper.setLeft(sep);
        wrapper.setCenter(side);

        root.setRight(wrapper);
    }

    private VBox createPlacementControlButtons(BoardView view) {
        Button rotate = new Button("Obróć");
        rotate.getStyleClass().add("action-button");
        rotate.setOnAction(_ -> horizontal = !horizontal);

        Button auto = new Button("Auto");
        auto.getStyleClass().add("action-button");
        auto.setOnAction(_ -> autoPlaceShips(view));

        Button clear = new Button("Wyczyść");
        clear.getStyleClass().add("action-button");
        clear.setOnAction(_ -> {
            clearBoard(view);
            warningBox.setVisible(false);
        });

        Button ready = new Button("Gotowy");
        ready.getStyleClass().add("action-button");
        ready.setOnAction(_ -> finishPlacement());

        VBox bottomButtons = new VBox(10, rotate, auto, clear, ready);
        bottomButtons.getStyleClass().add("box-center");
        return bottomButtons;
    }

    private void autoPlaceShips(BoardView view) {

        var board = placingPlayer.getBoard();
        board.clear();

        List<Ship> autoShips = createDefaultShips();
        Random r = new Random();

        for (Ship s : autoShips) {
            boolean placed = false;

            while (!placed) {
                boolean horiz = r.nextBoolean();
                int x = r.nextInt(10);
                int y = r.nextInt(10);

                if (board.placeShip(s, x, y, horiz)) {
                    placed = true;
                }
            }
        }

        ships.clear();
        selectedShip = null;

        view.update();
        rebuildShipPanel(view);
        warningBox.setVisible(false);
    }

    private void clearBoard(BoardView view) {
        placingPlayer.getBoard().clear();
        ships = new ArrayList<>(createDefaultShips());
        selectedShip = ships.getFirst();

        view.update();
        rebuildShipPanel(view);
        warningBox.setVisible(false);
    }

    private void finishPlacement() {
        if (!ships.isEmpty()) {
            warningBox.setVisible(true);
            return;
        }

        warningBox.setVisible(false);
        root.setBottom(null);

        if (!onlineMode) {
            Player p1 = engine.getState().getPlayer1();
            Player p2 = engine.getState().getPlayer2();

            if (placingPlayer == p1) {
                placingPlayer = p2;
                engine.getState().setTurn(PlayerTurn.PLAYER2);

                root.setRight(null);
                root.setCenter(new Text("Czekaj na drugiego gracza..."));

                showPlacementScreen();
                return;
            }

            engine.getState().setTurn(PlayerTurn.PLAYER1);

            historyBox.getChildren().clear();

            p1TargetView = new BoardView(p2.getBoard(), true);
            p2TargetView = new BoardView(p1.getBoard(), true);

            p1PlacementView = new BoardView(p1.getBoard());
            p2PlacementView = new BoardView(p2.getBoard());

            inputLocked = false;
            showBattleScreen();
            return;
        }
        myReady = true;

        try {
            List<Ship> placedShips = placingPlayer.getBoard().getShips();

            ReadyEvent readyEvent = new ReadyEvent(placingPlayer.getName(), placedShips);
            if (netServer != null) netServer.send(readyEvent);
            if (netClient != null) netClient.send(readyEvent);
        } catch (Exception e) {
            System.err.println("Statki: [GameView] Błąd podczas wysyłania ReadyEvent: " + e.getMessage());
        }

        checkBothReady();
    }

    private void checkBothReady() {
        if (myReady && opponentReady) {
            Platform.runLater(() -> {
                Player p1 = engine.getState().getPlayer1();
                Player p2 = engine.getState().getPlayer2();

                engine.getState().setTurn(PlayerTurn.PLAYER1);

                historyBox.getChildren().clear();

                p1TargetView = new BoardView(p2.getBoard(), true);
                p2TargetView = new BoardView(p1.getBoard(), true);

                p1PlacementView = new BoardView(p1.getBoard());
                p2PlacementView = new BoardView(p2.getBoard());
                inputLocked = !isHost;

                addHistoryEntry("Gra rozpoczęta!");
                String turnName = engine.getState().getCurrentPlayer().getName();
                addHistoryEntry("Tura gracza: " + turnName);

                showBattleScreen();
            });
        } else if (myReady) {
            root.setRight(null);
            Text waitText = new Text("Gotowe! Czekam na przeciwnika...");
            waitText.getStyleClass().add("player-name");
            VBox waitBox = new VBox(waitText);
            waitBox.getStyleClass().add("box-center");
            root.setCenter(waitBox);
        }
    }

    private void showBattleScreen() {
        Player current = engine.getState().getCurrentPlayer();
        Player opponent = engine.getState().getOtherPlayer();

        BoardView myBoardView;
        BoardView targetView;
        String myNameLabel;
        String enemyNameLabel;

        if (onlineMode) {
            Player me = isHost ? engine.getState().getPlayer1() : engine.getState().getPlayer2();
            Player enemy = isHost ? engine.getState().getPlayer2() : engine.getState().getPlayer1();

            if (isHost) {
                myBoardView = p1PlacementView;
                targetView = p1TargetView;
            } else {
                myBoardView = p2PlacementView;
                targetView = p2TargetView;
            }

            myNameLabel = "Twoja plansza: " + me.getName();
            enemyNameLabel = "Plansza przeciwnika: " + enemy.getName();

        } else {
            if (current == engine.getState().getPlayer1()) {
                myBoardView = p1PlacementView;
                targetView = p1TargetView;
            } else {
                myBoardView = p2PlacementView;
                targetView = p2TargetView;
            }
            myNameLabel = "Twoja plansza: " + current.getName();
            enemyNameLabel = "Plansza przeciwnika: " + opponent.getName();
        }

        attachShootingHandlers(targetView);
        attachSwapHandlers(myBoardView);

        targetView.update();
        myBoardView.update();

        Text myLabel = new Text(myNameLabel);
        myLabel.getStyleClass().addAll("player-name", "player-name-current");

        Text enemyLabel = new Text(enemyNameLabel);
        enemyLabel.getStyleClass().addAll("player-name", "player-name-enemy");

        VBox leftPane = new VBox(10, myLabel, myBoardView.getGridPane());
        leftPane.getStyleClass().add("box-center");

        VBox rightPane = new VBox(10, enemyLabel, targetView.getGridPane());
        rightPane.getStyleClass().add("box-center");

        Rectangle sep = new Rectangle(3, 600);
        sep.getStyleClass().add("board-separator");

        VBox sepWrapper = new VBox(sep);
        sepWrapper.getStyleClass().add("box-center");

        HBox boards = new HBox(40, leftPane, sepWrapper, rightPane);
        boards.getStyleClass().add("box-center");

        root.setCenter(boards);

        historyScroll.setFitToWidth(true);
        historyScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(historyScroll, Priority.ALWAYS);

        VBox historyWrapper = new VBox(historyScroll);
        root.setRight(historyWrapper);

        boolean showBoosts = !inputLocked
                && engine.getState().isBoostsEnabled()
                && !(current instanceof BotPlayer)
                && !(opponent instanceof BotPlayer);

        if (onlineMode) {
            Player me = isHost ? engine.getState().getPlayer1() : engine.getState().getPlayer2();
            if (current != me) {
                showBoosts = false;
            }
        }

        if (showBoosts) {
            HBox boostBar = new HBox(10);
            boostBar.getStyleClass().add("boost-bar");

            Button radarBtn = new Button("RADAR");
            Button swapBtn = new Button("SWAP");
            Button trollBtn = new Button("TROLL");

            int used = getUsedBoostCount(current);
            Set<BoostType> usedSet = getUsedBoosts(current);
            boolean blockAll = used >= 2;

            radarBtn.setDisable(blockAll || usedSet.contains(BoostType.RADAR));
            swapBtn.setDisable(blockAll || usedSet.contains(BoostType.SWAP));
            trollBtn.setDisable(blockAll || usedSet.contains(BoostType.TROLL));

            radarBtn.setOnAction(_ -> {
                useRadar();
                showBattleScreen();
            });

            swapBtn.setOnAction(_ -> {
                useSwap();
                showBattleScreen();
            });

            trollBtn.setOnAction(_ -> {
                useTroll();
                showBattleScreen();
            });

            boostBar.getChildren().addAll(radarBtn, swapBtn, trollBtn);
            root.setBottom(boostBar);

        } else {
            root.setBottom(null);
        }
    }

    private void attachShootingHandlers(BoardView targetView) {

        for (Node n : targetView.getGridPane().getChildren()) {

            n.setOnMouseClicked(_ -> {

                if (inputLocked) return;

                Integer gx = GridPane.getColumnIndex(n);
                Integer gy = GridPane.getRowIndex(n);
                if (gx == null || gy == null) return;

                int x = gx - 1;
                int y = gy - 1;
                if (x < 0 || y < 0) return;

                Player current = engine.getState().getCurrentPlayer();
                Player opponent = engine.getState().getOtherPlayer();

                boolean hit = engine.shoot(x, y);
                updateTargetViews();

                String coord = "" + (char) ('A' + y) + (x + 1);

                var cell = opponent.getBoard().getCell(x, y);
                boolean sunk = cell.hasShip() && cell.getShip().isSunk();
                String shipName = sunk ? cell.getShip().getName() : null;

                if (onlineMode) {
                    try {
                        ShotEvent shotEvent = new ShotEvent(x, y, hit, sunk, shipName);
                        if (netServer != null) netServer.send(shotEvent);
                        if (netClient != null) netClient.send(shotEvent);
                    } catch (Exception ignored) {}
                }

                if (hit) {
                    addHistoryEntry(current.getName() + ": " + coord + " – Trafienie");
                    if (sunk) addHistoryEntry("Zatopiony: " + shipName);

                    if (engine.isGameFinished()) {
                        root.setBottom(null);
                        showEndScreen();
                        return;
                    }

                    showBattleScreen();
                    return;
                }

                addHistoryEntry(current.getName() + ": " + coord + " – Pudło");
                inputLocked = true;

                if (engine.isGameFinished()) {
                    showEndScreen();
                    return;
                }

                engine.getState().swapTurn();

                if (engine.getState().getCurrentPlayer() instanceof BotPlayer) {
                    inputLocked = true;
                    PauseTransition pause = new PauseTransition(Duration.seconds(1.0));
                    pause.setOnFinished(_ -> botMove());
                    pause.play();
                    return;
                }

                if (onlineMode) {
                    inputLocked = true;
                    String currentName = engine.getState().getCurrentPlayer().getName();
                    addHistoryEntry("Tura gracza: " + currentName);
                    showBattleScreen();
                    return;
                }

                inputLocked = false;
                showTurnOverlay(engine.getState().getCurrentPlayer());

            });
        }
    }

    private int getUsedBoostCount(Player p) {
        return usedBoostCount.getOrDefault(p, 0);
    }

    private Set<BoostType> getUsedBoosts(Player p) {
        return usedBoosts.computeIfAbsent(p, _ -> EnumSet.noneOf(BoostType.class));
    }

    private boolean isBoostForbidden(Player p, BoostType type) {
        if (getUsedBoostCount(p) >= 2) return true;
        return getUsedBoosts(p).contains(type);
    }

    private void markBoostUsed(Player p, BoostType type) {
        Set<BoostType> set = getUsedBoosts(p);
        if (!set.contains(type)) {
            set.add(type);
            usedBoostCount.put(p, getUsedBoostCount(p) + 1);
        }
    }

    private VBox createBoostButtons() {

        Player current = engine.getState().getCurrentPlayer();

        VBox box = new VBox(10);
        box.getStyleClass().add("boost-pane");

        Text title = new Text("Boosty (max 2)");
        title.getStyleClass().add("boost-title");

        Button radarBtn = new Button("RADAR");
        Button swapBtn = new Button("SWAP");
        Button trollBtn = new Button("TROLL");

        int used = getUsedBoostCount(current);
        Set<BoostType> usedSet = getUsedBoosts(current);
        boolean blockAll = used >= 2;

        radarBtn.setDisable(blockAll || usedSet.contains(BoostType.RADAR));
        swapBtn.setDisable(blockAll || usedSet.contains(BoostType.SWAP));
        trollBtn.setDisable(blockAll || usedSet.contains(BoostType.TROLL));

        radarBtn.setOnAction(_ -> useRadar());
        swapBtn.setOnAction(_ -> useSwap());
        trollBtn.setOnAction(_ -> useTroll());

        box.getChildren().addAll(title, radarBtn, swapBtn, trollBtn);
        return box;
    }

    private void useRadar() {
        Player current = engine.getState().getCurrentPlayer();
        Player opponent = engine.getState().getOtherPlayer();

        if (isBoostForbidden(current, BoostType.RADAR)) return;

        if (onlineMode) {
            try {
                BoostEvent cmd = new BoostEvent(
                        BoostEvent.Kind.RADAR,
                        -1, -1,
                        0, false
                );

                if (netServer != null) netServer.send(cmd);
                if (netClient != null) netClient.send(cmd);

            } catch (Exception ignored) {}
        }

        var cell = opponent.getBoard().revealRandomShipCell();
        if (cell == null) return;

        int fx = -1, fy = -1;

        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                if (opponent.getBoard().getCell(x, y) == cell) {
                    fx = x;
                    fy = y;
                    break;
                }
            }
        }

        if (fx != -1) {
            String coord = "" + (char) ('A' + fy) + (fx + 1);
            addHistoryEntry(current.getName() + " : namierza pole " + coord);
        }

        markBoostUsed(current, BoostType.RADAR);
        root.setBottom(createBoostButtons());
    }

    private void useSwap() {
        Player current = engine.getState().getCurrentPlayer();

        if (isBoostForbidden(current, BoostType.SWAP)) return;

        swapMode = true;
        swapTarget = null;

        markBoostUsed(current, BoostType.SWAP);

        addHistoryEntry(current.getName() + " : zmienia położenie statku");
        addHistoryEntry("Gra: wybierz statek");

        root.setBottom(createBoostButtons());
    }

    private void attachSwapHandlers(BoardView view) {

        var board = engine.getState().getCurrentPlayer().getBoard();

        for (Node n : view.getGridPane().getChildren()) {

            n.setOnMouseEntered(_ -> {

                if (!swapMode) return;

                Integer gx = GridPane.getColumnIndex(n);
                Integer gy = GridPane.getRowIndex(n);
                if (gx == null || gy == null) return;

                int x = gx - 1;
                int y = gy - 1;

                if (swapTarget == null) {

                    var cell = board.getCell(x, y);
                    if (cell.hasShip()) highlightSwapShip(view, cell.getShip());
                    else view.clearPreview();
                    return;
                }

                boolean can = board.canPlaceShipWithSpacing(
                        x, y,
                        swapTarget.getSize(),
                        swapTarget.isHorizontal()
                );

                view.showPreview(
                        swapTarget,
                        x,
                        y,
                        swapTarget.isHorizontal(),
                        can
                );
            });

            n.setOnMouseExited(_ -> {
                if (swapMode) view.update();
            });

            n.setOnMouseClicked(_ -> {

                if (!swapMode) return;

                Integer gx = GridPane.getColumnIndex(n);
                Integer gy = GridPane.getRowIndex(n);
                if (gx == null || gy == null) return;

                int x = gx - 1;
                int y = gy - 1;

                if (swapTarget == null) {

                    var cell = board.getCell(x, y);
                    if (!cell.hasShip()) return;

                    Ship s = cell.getShip();
                    if (s.isSunk()) {
                        addHistoryEntry("Nie można przenieść zatopionego statku!");
                        return;
                    }

                    swapTarget = s;
                    addHistoryEntry("Gra: wybrano " + swapTarget.getName());
                    highlightSwapShip(view, swapTarget);
                    return;
                }

                boolean ok = board.moveShip(
                        swapTarget,
                        x, y,
                        swapTarget.isHorizontal()
                );

                if (onlineMode) {
                    try {
                        BoostEvent cmd = new BoostEvent(
                                BoostEvent.Kind.SWAP_MOVE,
                                x, y,
                                swapTarget.getSize(),
                                swapTarget.isHorizontal()
                        );

                        if (netServer != null) netServer.send(cmd);
                        if (netClient != null) netClient.send(cmd);
                    } catch (Exception ignored) {}
                }

                if (!ok) {
                    addHistoryEntry("Gra: wybierz inne miejsce");
                    return;
                }

                addHistoryEntry("Gra: statek przeniesiony");

                swapMode = false;
                swapTarget = null;

                view.update();
            });
        }
    }

    private void highlightSwapShip(BoardView view, Ship ship) {

        view.update();
        var board = engine.getState().getCurrentPlayer().getBoard();

        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {

                var cell = board.getCell(x, y);
                if (cell.getShip() != ship) continue;

                int gx = x + 1;
                int gy = y + 1;

                for (Node n : view.getGridPane().getChildren()) {

                    Integer cx = GridPane.getColumnIndex(n);
                    Integer cy = GridPane.getRowIndex(n);
                    if (cx == null || cy == null) continue;
                    if (!(n instanceof Rectangle r)) continue;

                    if (cx == gx && cy == gy) {
                        r.getStyleClass().removeAll(
                                "cell-water",
                                "cell-ship",
                                "cell-hit",
                                "cell-miss",
                                "cell-preview-ok",
                                "cell-preview-bad"
                        );
                        r.getStyleClass().add("cell-preview-bad");
                    }
                }
            }
        }
    }

    private void useTroll() {
        Player current = engine.getState().getCurrentPlayer();

        if (isBoostForbidden(current, BoostType.TROLL)) return;

        var board = current.getBoard();
        Random r = new Random();
        boolean placed = false;

        while (!placed) {
            boolean horiz = r.nextBoolean();
            int x = r.nextInt(10);
            int y = r.nextInt(10);

            if (onlineMode) {
                try {
                    BoostEvent cmd = new BoostEvent(
                            BoostEvent.Kind.TROLL_PLACE,
                            x, y,
                            2,
                            horiz
                    );

                    if (netServer != null) netServer.send(cmd);
                    if (netClient != null) netClient.send(cmd);

                } catch (Exception ignored) {}
            }

            var troll = board.placeFakeShip(2, x, y, horiz);
            if (troll != null) placed = true;
        }

        markBoostUsed(current, BoostType.TROLL);
        addHistoryEntry(current.getName() + " : ustawia fałszywy statek");

        if (p1PlacementView != null) p1PlacementView.update();
        if (p2PlacementView != null) p2PlacementView.update();
    }

    private void updateTargetViews() {
        if (p1TargetView != null) p1TargetView.update();
        if (p2TargetView != null) p2TargetView.update();
    }

    private void highlightWholeShip(BoardView view, Ship ship) {

        view.update();
        var board = placingPlayer.getBoard();

        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {

                var cell = board.getCell(x, y);
                if (cell.getShip() != ship) continue;

                int gx = x + 1;
                int gy = y + 1;

                for (Node n : view.getGridPane().getChildren()) {

                    Integer cx = GridPane.getColumnIndex(n);
                    Integer cy = GridPane.getRowIndex(n);
                    if (cx == null || cy == null) continue;
                    if (!(n instanceof Rectangle r)) continue;

                    if (cx == gx && cy == gy) {
                        r.getStyleClass().removeAll(
                                "cell-water",
                                "cell-ship",
                                "cell-hit",
                                "cell-miss",
                                "cell-preview-ok",
                                "cell-preview-bad"
                        );
                        r.getStyleClass().add("cell-preview-bad");
                    }
                }
            }
        }
    }

    private void botMove() {

        Player current = engine.getState().getCurrentPlayer();
        Player human = engine.getState().getOtherPlayer();

        if (!(current instanceof BotPlayer bot)) return;

        PauseTransition pause = new PauseTransition(Duration.seconds(1.1));
        pause.setOnFinished(_ -> {

            int[] shot = bot.getAI().chooseShot(human.getBoard());
            int x = shot[0];
            int y = shot[1];

            boolean hit = engine.shoot(x, y);

            updateTargetViews();

            String coord = "" + (char) ('A' + y) + (x + 1);

            if (hit) {

                addHistoryEntry(bot.getName() + ": " + coord + " – Trafienie");

                var cell = human.getBoard().getCell(x, y);
                if (cell.hasShip() && cell.getShip().isSunk())
                    addHistoryEntry("Zatopiony: " + cell.getShip().getName());

                bot.getAI().notifyHit(x, y);

                if (engine.isGameFinished()) {
                    showEndScreen();
                    return;
                }

                botMove();
                return;
            }

            addHistoryEntry(bot.getName() + ": " + coord + " – Pudło");

            if (engine.isGameFinished()) {
                showEndScreen();
                return;
            }

            engine.getState().swapTurn();
            inputLocked = false;
            showBattleScreen();

        });

        pause.play();
    }

    private void showTurnOverlay(Player nextPlayer) {
        if (engine.getState().getCurrentPlayer() instanceof BotPlayer
                || engine.getState().getOtherPlayer() instanceof BotPlayer) {

            inputLocked = false;
            showBattleScreen();
            return;
        }

        inputLocked = true;
        int SECONDS = 5;

        PauseTransition showResultPause = new PauseTransition(Duration.seconds(2));

        showResultPause.setOnFinished(_ -> {

            int[] timeLeft = {SECONDS};

            StackPane overlay = new StackPane();
            overlay.getStyleClass().add("turn-overlay");

            root.setBottom(null);

            VBox box = new VBox(25);
            box.setAlignment(Pos.CENTER);

            Text turnInfo = new Text("Tura gracza: " + nextPlayer.getName());
            turnInfo.getStyleClass().add("turn-info");

            Text countdown = new Text(String.valueOf(timeLeft[0]));
            countdown.getStyleClass().add("turn-countdown");

            box.getChildren().addAll(turnInfo, countdown);
            overlay.getChildren().add(box);

            root.setCenter(overlay);
            root.setRight(null);

            PauseTransition tick = new PauseTransition(Duration.seconds(1));
            tick.setOnFinished(_ -> {
                timeLeft[0]--;
                if (timeLeft[0] <= 0) {
                    inputLocked = false;
                    showBattleScreen();
                } else {
                    countdown.setText(String.valueOf(timeLeft[0]));
                    tick.play();
                }
            });

            tick.play();
        });

        showResultPause.play();
    }

    private void showEndScreen() {
        showEndScreen(engine.getWinner());
    }

    private void showEndScreen(Player winner) {

        String winnerName = (winner != null) ? winner.getName() : "Nikt?";
        Text endText = new Text("KONIEC!\nWygrał: " + winnerName);
        endText.getStyleClass().add("game-end-text");

        VBox buttons = createEndGameButtons(winnerName);

        VBox wrapper = new VBox(25, endText, buttons);
        wrapper.getStyleClass().add("box-center");

        root.setRight(null);
        root.setCenter(wrapper);
        root.setBottom(null);

        addHistoryEntry("Wygrał: " + winnerName);
        this.gameEnded = true;
    }

    private VBox createEndGameButtons(String winnerName) {
        Button menuBtn = new Button("Powrót do menu");
        menuBtn.getStyleClass().add("large-button");
        menuBtn.setOnAction(_ -> {
            cleanupNetwork();
            MenuView menu = new MenuView(stage);
            stage.getScene().setRoot(menu.getRoot());
        });

        Button exportBtn = new Button("Eksportuj wyniki");
        exportBtn.getStyleClass().add("large-button");
        exportBtn.setOnAction(_ -> {
            try {
                history.exportToFile(winnerName);
            } catch (Exception e) {
                System.err.println("Statki: [GameView] Błąd podczas eksportowania historii: " + e.getMessage());
            }
        });

        Button exitBtn = new Button("Wyjdź z gry");
        exitBtn.getStyleClass().add("large-button");
        exitBtn.setOnAction(_ -> {
            cleanupNetwork();
            stage.close();
        });

        VBox buttons = new VBox(10, menuBtn, exportBtn, exitBtn);
        buttons.getStyleClass().add("box-center");
        return buttons;
    }

    private void cleanupNetwork() {
        if (netServer != null) netServer.close();
        if (netClient != null) netClient.close();
        if (lobbyServer != null) lobbyServer.stop();
    }

    @Override
    public void onShotReceived(ShotEvent event) {
        Platform.runLater(() -> {
            Player victim;
            Player shooter;
            if (isHost) {
                victim = engine.getState().getPlayer1();
                shooter = engine.getState().getPlayer2();
            } else {
                victim = engine.getState().getPlayer2();
                shooter = engine.getState().getPlayer1();
            }

            Cell cell = victim.getBoard().getCell(event.x(), event.y());
            cell.setShot(true);

            if (cell.hasShip() && event.hit()) {
                cell.getShip().hit();
            }

            String coord = "" + (char) ('A' + event.y()) + (event.x() + 1);

            if (event.hit()) {
                addHistoryEntry(shooter.getName() + ": " + coord + " – Trafienie");
                if (event.sunk() && event.shipName() != null) {
                    addHistoryEntry("Zatopiony: " + event.shipName());
                }
            } else {
                addHistoryEntry(shooter.getName() + ": " + coord + " – Pudło");
            }

            updateTargetViews();
            if (p1PlacementView != null) p1PlacementView.update();
            if (p2PlacementView != null) p2PlacementView.update();

            if (engine.isGameFinished()) {
                showEndScreen();
                return;
            }

            if (!event.hit()) {
                engine.getState().swapTurn();
                inputLocked = false;
                String myName = engine.getState().getCurrentPlayer().getName();
                addHistoryEntry("Tura gracza: " + myName);
            }

            showBattleScreen();
        });
    }

    @Override
    public void onBoostReceived(BoostEvent cmd) {
        Platform.runLater(() -> {
            Player remoteUpponent = isHost ? engine.getState().getPlayer2() : engine.getState().getPlayer1();

            switch (cmd.kind()) {
                case RADAR -> addHistoryEntry(remoteUpponent.getName() + " użył RADARU.");
                case SWAP_SELECT -> addHistoryEntry(remoteUpponent.getName() + " wybrał statek do przeniesienia.");
                case SWAP_MOVE -> {
                    addHistoryEntry(remoteUpponent.getName() + " przeniósł statek.");
                    Board enemyBoard = remoteUpponent.getBoard();
                    Ship toMove = null;
                    outer:
                    for (int y = 0; y < 10; y++) {
                        for (int x = 0; x < 10; x++) {
                            Cell c = enemyBoard.getCell(x, y);
                            if (c.hasShip()) {
                                Ship s = c.getShip();
                                if (s.isFake() && s.getSize() == cmd.size()) {
                                    toMove = s;
                                    break outer;
                                }
                            }
                        }
                    }
                    if (toMove != null) {
                        enemyBoard.moveShip(toMove, cmd.x(), cmd.y(), cmd.horizontal());
                    } else {
                        System.out.println("Błąd: nie znaleziono statku do zamiany");
                    }
                    updateTargetViews();
                }
                case TROLL_PLACE -> {
                    addHistoryEntry(remoteUpponent.getName() + " ustawił fałszywy statek");
                    remoteUpponent.getBoard().placeFakeShip(cmd.size(), cmd.x(), cmd.y(), cmd.horizontal());
                    updateTargetViews();
                }
            }
        });
    }

    @Override
    public void onReadyReceived(ReadyEvent event) {
        Platform.runLater(() -> {
            opponentReady = true;

            Player enemyPlayer = isHost ? engine.getState().getPlayer2() : engine.getState().getPlayer1();

            if (event.playerName() != null) {
                enemyPlayer.setName(event.playerName());
            }

            if (event.ships() != null) {
                Board enemyBoard = enemyPlayer.getBoard();
                enemyBoard.clear();
                for (Ship s : event.ships()) {
                    enemyBoard.placeShip(s, s.getX(), s.getY(), s.isHorizontal());
                }
            }
            System.out.println("[6/6] Gra rozpoczęta!");
            checkBothReady();
        });
    }

    @Override
    public void onStartGameReceived(StartGameEvent event) {}

    @Override
    public void onPlayerDisconnected() {
        Platform.runLater(() -> {
            if (gameEnded || engine.isGameFinished()) return;

            Player localPlayer = isHost ? engine.getState().getPlayer1() : engine.getState().getPlayer2();
            Player remotePlayer = isHost ? engine.getState().getPlayer2() : engine.getState().getPlayer1();

            addHistoryEntry("Gracz " + remotePlayer.getName() + " opuścił grę.");
            addHistoryEntry("Walkower - wygrywasz!");

            inputLocked = true;
            warningText.setText("Połączenie utracone!");

            showEndScreen(localPlayer);
        });
    }
}
