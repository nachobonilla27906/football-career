package footballcareer.ui;

import footballcareer.model.Match;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class MatchResultCell extends ListCell<Match> {
    private final long controlledTeamId;

    public MatchResultCell(long controlledTeamId) {
        this.controlledTeamId = controlledTeamId;
        getStyleClass().add("result-match-cell");
    }

    @Override protected void updateItem(Match match, boolean empty) {
        super.updateItem(match, empty); setText(null); setGraphic(null);
        getStyleClass().remove("controlled-team-row");
        if (empty || match == null) return;
        if (match.getHomeTeam().getId() == controlledTeamId
                || match.getAwayTeam().getId() == controlledTeamId)
            getStyleClass().add("controlled-team-row");
        HBox home = team(match.getHomeTeam(), true); HBox away = team(match.getAwayTeam(), false);
        HBox.setHgrow(home, Priority.ALWAYS); HBox.setHgrow(away, Priority.ALWAYS);
        VBox score = new VBox(2, label(match.getHomeGoals() + "  —  " + match.getAwayGoals(),
                "result-score"), label("FINAL", "result-status"));
        score.setAlignment(Pos.CENTER); score.setMinWidth(92);
        HBox row = new HBox(18, home, score, away); row.setAlignment(Pos.CENTER);
        VBox card = new VBox(7, label(match.getCompetition().getName(), "result-competition"), row);
        card.getStyleClass().add("result-match-row"); setGraphic(card);
    }

    private HBox team(footballcareer.model.Team team, boolean home) {
        Label name = label(team.getName(), "result-team");
        HBox box = home ? new HBox(9, name, TeamCrestView.create(team, 34))
                : new HBox(9, TeamCrestView.create(team, 34), name);
        box.setAlignment(home ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT); return box;
    }

    private static Label label(String text, String style) {
        Label label = new Label(text); label.getStyleClass().add(style); return label;
    }
}
