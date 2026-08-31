package footballcareer.ui;

import footballcareer.model.Competition;
import javafx.scene.Node;
import javafx.scene.control.Label;

import java.util.List;

public final class StandingsLegendView {
    private StandingsLegendView() {}

    public static List<Node> items(Competition competition) {
        if (competition.isEuropean()) {
            return List.of(item("■ CABEZA DE SERIE · 1–8", "legend-europe-seeded"),
                    item("■ OCTAVOS · 9–16", "legend-europe-qualified"));
        }
        return List.of(item("■ CHAMPIONS", "legend-champions"),
                item("■ EUROPA", "legend-europa"),
                item("■ CONFERENCE", "legend-conference"),
                item("■ DESCENSO", "legend-relegation"));
    }

    private static Label item(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        return label;
    }
}
