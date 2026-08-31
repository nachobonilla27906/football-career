package footballcareer.ui;

import footballcareer.model.Team;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.InputStream;

public final class TeamCrestView {
    private TeamCrestView() {}

    public static Node create(Team team, double size) {
        StackPane frame = new StackPane();
        frame.getStyleClass().add("crest-frame");
        frame.setMinSize(size, size); frame.setPrefSize(size, size); frame.setMaxSize(size, size);
        String resource = "assets/crests/" + team.getShortName() + (size > 150 ? "_512.png" : "_128.png");
        try (InputStream stream = TeamCrestView.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream != null) {
                ImageView image = new ImageView(new Image(stream));
                image.setFitWidth(size * 0.82); image.setFitHeight(size * 0.82);
                image.setPreserveRatio(true); image.setSmooth(true);
                frame.getChildren().add(image);
                return frame;
            }
        } catch (java.io.IOException ignored) {
            // A typographic crest keeps every club usable if an external logo is unavailable.
        }
        Label fallback = new Label(team.getShortName());
        fallback.getStyleClass().add("crest-fallback");
        frame.getChildren().add(fallback); frame.setAlignment(Pos.CENTER);
        return frame;
    }
}
