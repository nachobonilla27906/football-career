package footballcareer.ui;

import footballcareer.model.Competition;
import footballcareer.model.LeagueStanding;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Map;
import java.util.function.Supplier;

public final class StandingsRowCell extends ListCell<LeagueStanding> {
    private final Supplier<Competition> competition;
    private final Supplier<Integer> total;
    private final long controlledTeamId;
    private final Map<Long, String> recentForm;

    public StandingsRowCell(Supplier<Competition> competition, Supplier<Integer> total,
            long controlledTeamId, Map<Long, String> recentForm) {
        this.competition = competition;
        this.total = total;
        this.controlledTeamId = controlledTeamId;
        this.recentForm = recentForm;
        getStyleClass().add("standings-modern-cell");
    }

    @Override
    protected void updateItem(LeagueStanding row, boolean empty) {
        super.updateItem(row, empty);
        setText(null);
        setGraphic(null);
        getStyleClass().removeAll("controlled-team-row", "zone-champion", "zone-champions",
                "zone-europa", "zone-conference", "zone-relegation", "zone-europe-seeded",
                "zone-europe-qualified");
        if (empty || row == null) return;

        int position = getIndex() + 1;
        String zone = CompetitionVisuals.standingZone(competition.get(), getIndex(), total.get());
        if (zone != null) getStyleClass().add(zone);
        if (row.getTeam().getId() == controlledTeamId) getStyleClass().add("controlled-team-row");

        Label rank = value(String.format("%02d", position), "standing-rank");
        Node crest = TeamCrestView.create(row.getTeam(), 38);
        Label club = value(row.getTeam().getName(), "standing-club-name");
        Label form = value(recentForm.getOrDefault(row.getTeam().getId(), "—"),
                "standing-form");
        VBox identity = new VBox(2, club, form);
        HBox.setHgrow(identity, Priority.ALWAYS);
        HBox stats = new HBox(18,
                metric("PJ", row.getPlayed()), metric("G", row.getWins()),
                metric("E", row.getDraws()), metric("P", row.getLosses()),
                metric("DG", row.getGoalDifference()), metric("PTS", row.getPoints()));
        stats.setAlignment(Pos.CENTER_RIGHT);
        HBox line = new HBox(14, rank, crest, identity, stats);
        line.setAlignment(Pos.CENTER_LEFT);
        line.getStyleClass().add("standings-modern-row");
        setGraphic(line);
    }

    private static VBox metric(String name, int number) {
        Label value = value(String.valueOf(number), "standing-metric-value");
        Label caption = value(name, "standing-metric-label");
        VBox box = new VBox(1, value, caption);
        box.setAlignment(Pos.CENTER);
        box.setMinWidth("PTS".equals(name) ? 46 : 34);
        return box;
    }

    private static Label value(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        return label;
    }
}
