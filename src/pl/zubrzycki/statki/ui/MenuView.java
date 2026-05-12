package pl.zubrzycki.statki.ui;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Objects;

public class MenuView {

    private final StackPane root = new StackPane();

    public MenuView(Stage stage) {
        root.getStyleClass().add("view-root");
        Image bgImage = new Image(
                Objects.requireNonNull(
                        getClass().getResource("/images/background.jpg"),
                        "Nie znaleziono obrazu: /images/background.jpg"
                ).toExternalForm()
        );

        ImageView bg = new ImageView(bgImage);
        bg.setPreserveRatio(false);
        bg.setSmooth(true);
        bg.setFitWidth(1300);
        bg.setFitHeight(750);
        bg.setEffect(new GaussianBlur(18));

        VBox menuBox = new VBox(20);
        menuBox.getStyleClass().add("box-center");

        Button localBtn = new Button("Gra lokalna");
        localBtn.getStyleClass().add("action-button");
        localBtn.setPrefHeight(45);
        localBtn.setOnAction(_ -> {
            PlayerSetupView setup = new PlayerSetupView(stage);
            stage.getScene().setRoot(setup.getRoot());
        });

        Button vsBotBtn = new Button("Gra z botem");
        vsBotBtn.getStyleClass().add("action-button");
        vsBotBtn.setPrefHeight(45);
        vsBotBtn.setOnAction(_ -> {
            BotDifficultyView bot = new BotDifficultyView(stage);
            stage.getScene().setRoot(bot.getRoot());
        });

        Button multiBtn = new Button("Multiplayer");
        multiBtn.getStyleClass().add("action-button");
        multiBtn.setPrefHeight(45);
        multiBtn.setOnAction(_ -> {
            MultiplayerView mp = new MultiplayerView(stage);
            stage.getScene().setRoot(mp.getRoot());
        });

        menuBox.getChildren().addAll(localBtn, vsBotBtn, multiBtn);

        root.getChildren().addAll(bg, menuBox);
        StackPane.setAlignment(menuBox, Pos.CENTER);

        root.widthProperty().addListener((_, _, newV) -> bg.setFitWidth(newV.doubleValue()));
        root.heightProperty().addListener((_, _, newV) -> bg.setFitHeight(newV.doubleValue()));
    }

    public Parent getRoot() { return root; }
}
