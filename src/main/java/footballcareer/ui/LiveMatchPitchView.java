package footballcareer.ui;

import footballcareer.model.MatchEvent;
import footballcareer.service.LivePitchPositionService;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class LiveMatchPitchView {
    private final LivePitchPositionService positions = new LivePitchPositionService();
    private final Pane pitch = new Pane();
    private final Circle marker = new Circle(8);
    private final Label zone = new Label("CENTRO DEL CAMPO");
    private final StackPane root = new StackPane();

    public LiveMatchPitchView(String home, String away) {
        pitch.getStyleClass().add("live-pitch"); pitch.setPrefSize(760, 250);
        marker.getStyleClass().add("live-ball-marker"); marker.relocate(372, 117);
        Label center = new Label("│"); center.getStyleClass().add("live-pitch-center");
        center.relocate(378, 15); pitch.getChildren().addAll(center, marker);
        Label teams = new Label(home + "  →                         ←  " + away);
        teams.getStyleClass().add("live-pitch-teams");
        zone.getStyleClass().add("live-pitch-zone");
        StackPane.setAlignment(teams, Pos.TOP_CENTER); StackPane.setAlignment(zone, Pos.BOTTOM_CENTER);
        root.getStyleClass().add("live-pitch-frame"); root.getChildren().addAll(pitch, teams, zone);
    }

    public StackPane node() { return root; }

    public void show(MatchEvent event, boolean homeTeam) {
        var target = positions.position(event, homeTeam);
        TranslateTransition move = new TranslateTransition(Duration.millis(420), marker);
        move.setToX(target.x() * 720 - 372); move.setToY(target.y() * 210 - 117); move.play();
        zone.setText(event.getMinute() + "'  •  " + target.zone());
    }
}
