package footballcareer.ui;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class LiveMatchLayout {
    public Node build(Node scoreboard, Node goalBanner, FlowPane controls, Node stats,
            Node tactics, VBox eventFeed, Node reportButton) {
        StackPane broadcast = new StackPane(scoreboard, goalBanner);
        broadcast.getStyleClass().add("live-broadcast");
        VBox matchStage = new VBox(12, broadcast, controls, stats);
        matchStage.getStyleClass().add("live-match-stage");
        HBox.setHgrow(matchStage, Priority.ALWAYS);
        ScrollPane feed = new ScrollPane(eventFeed);
        feed.setFitToWidth(true); feed.setPrefWidth(340); feed.setMinWidth(280);
        feed.getStyleClass().add("live-feed-scroll");
        HBox experience = new HBox(14, matchStage, feed);
        TitledPane tacticalDrawer = new TitledPane("AJUSTES TÁCTICOS", tactics);
        tacticalDrawer.setExpanded(false); tacticalDrawer.getStyleClass().add("tactical-drawer");
        return new VBox(14, experience, tacticalDrawer, reportButton);
    }
}
