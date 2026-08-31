package footballcareer.ui;

import footballcareer.model.ClubFinance;
import footballcareer.model.Team;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Map;

public final class TeamSelectionCell extends ListCell<Team> {
    private final Map<Long, String> leagues;
    private final Map<Long, ClubFinance> finances;

    public TeamSelectionCell(Map<Long, String> leagues, Map<Long, ClubFinance> finances) {
        this.leagues = leagues; this.finances = finances;
    }

    @Override protected void updateItem(Team team, boolean empty) {
        super.updateItem(team, empty);
        if (empty || team == null) { setGraphic(null); setText(null); return; }
        ClubFinance finance = finances.get(team.getId());
        Label name = new Label(team.getName()); name.getStyleClass().add("club-picker-name");
        Label details = new Label(leagues.getOrDefault(team.getId(), team.getCountry())
                + "  ·  REP " + team.getReputation() + (finance == null ? ""
                : String.format("  ·  €%.1fM", finance.getTransferBudget() / 1_000_000)));
        details.getStyleClass().add("club-picker-detail");
        HBox row = new HBox(13, TeamCrestView.create(team, 48), new VBox(3, name, details));
        row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("club-picker-row");
        setGraphic(row); setText(null);
    }
}
