package pl.zubrzycki.statki.ui;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pl.zubrzycki.statki.engine.GameEngine;
import pl.zubrzycki.statki.model.BotDifficulty;
import pl.zubrzycki.statki.model.BotPlayer;
import pl.zubrzycki.statki.model.Player;

public class BotDifficultyView {

    private final VBox root = new VBox(20);

    public BotDifficultyView(Stage stage) {
        root.setAlignment(Pos.CENTER);

        Button easy = new Button("Łatwy");
        easy.setPrefWidth(200);
        easy.setPrefHeight(45);

        Button normal = new Button("Normalny");
        normal.setPrefWidth(200);
        normal.setPrefHeight(45);

        Button hard = new Button("Trudny");
        hard.setPrefWidth(200);
        hard.setPrefHeight(45);

        easy.setOnAction(_ -> startBotGame(stage, BotDifficulty.EASY));
        normal.setOnAction(_ -> startBotGame(stage, BotDifficulty.NORMAL));
        hard.setOnAction(_ -> startBotGame(stage, BotDifficulty.HARD));

        Button back = new Button("Powrót do menu");
        back.setPrefWidth(200);
        back.setPrefHeight(45);
        back.setOnAction(_ -> {
            MenuView menu = new MenuView(stage);
            stage.getScene().setRoot(menu.getRoot());
        });

        root.getChildren().addAll(easy, normal, hard, back);
    }

    private void startBotGame(Stage stage, BotDifficulty diff) {

        Player human = new Player("Gracz");
        BotPlayer bot = new BotPlayer("Bot", diff);

        GameEngine engine = new GameEngine(human, bot);

        GameView gv = new GameView(stage, engine);
        stage.getScene().setRoot(gv.getRoot());
    }

    public Parent getRoot() { return root; }
}
