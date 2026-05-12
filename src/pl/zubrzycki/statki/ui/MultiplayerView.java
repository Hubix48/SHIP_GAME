package pl.zubrzycki.statki.ui;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class MultiplayerView {

    private final BorderPane root = new BorderPane();

    public MultiplayerView(Stage stage) {
        root.getStyleClass().add("view-root");

        Text title = new Text("Multiplayer");
        title.getStyleClass().add("player-name");

        Button hostBtn = new Button("HOST");
        hostBtn.getStyleClass().add("large-button");
        hostBtn.setPrefHeight(60);
        hostBtn.setOnAction(_ -> {
            HostView hostView = new HostView(stage);
            stage.getScene().setRoot(hostView.getRoot());
        });

        Button guestBtn = new Button("GRACZ");
        guestBtn.getStyleClass().add("large-button");
        guestBtn.setPrefHeight(60);
        guestBtn.setOnAction(_ -> {
            GuestView guestView = new GuestView(stage);
            stage.getScene().setRoot(guestView.getRoot());
        });

        Button backBtn = new Button("Powrót do menu");
        backBtn.getStyleClass().add("action-button");
        backBtn.setOnAction(_ -> {
            MenuView menu = new MenuView(stage);
            stage.getScene().setRoot(menu.getRoot());
        });

        VBox center = new VBox(30, title, hostBtn, guestBtn);
        center.getStyleClass().add("box-center");

        VBox bottom = new VBox(15, backBtn);
        bottom.getStyleClass().add("box-center");

        root.setCenter(center);
        root.setBottom(bottom);
    }

    public Parent getRoot() {
        return root;
    }
}
