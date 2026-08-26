package footballcareer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        Label title = new Label("FOOTBALL CAREER");

        StackPane root = new StackPane(title);

        Scene scene = new Scene(root, 1000, 650);

        stage.setTitle("Football Career");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}