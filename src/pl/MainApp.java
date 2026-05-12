package pl;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pl.zubrzycki.statki.ui.MenuView;

import java.util.Objects;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        MenuView menu = new MenuView(primaryStage);
        Parent root = menu.getRoot();

        Scene scene = new Scene(root, 1300, 750);

        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/pl/zubrzycki/statki/ui/style.css")

                ).toExternalForm()
        );

        primaryStage.setScene(scene);
        primaryStage.setTitle("Statki");
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    static void main(String[] args) {
        launch(args);
    }
}
