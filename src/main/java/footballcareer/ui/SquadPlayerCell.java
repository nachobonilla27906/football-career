package footballcareer.ui;

import footballcareer.model.Player;
import footballcareer.model.PlayerState;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;

public final class SquadPlayerCell extends ListCell<Player> {
    private final LocalDate date;
    private final Map<Long, PlayerState> states;
    private final Function<Player, String> role;

    public SquadPlayerCell(LocalDate date, Map<Long, PlayerState> states,
            Function<Player, String> role) {
        this.date = date;
        this.states = states;
        this.role = role;
        getStyleClass().add("squad-player-cell");
    }

    @Override protected void updateItem(Player player, boolean empty) {
        super.updateItem(player, empty);
        setText(null); setGraphic(null);
        if (empty || player == null) return;
        PlayerState state = states.get(player.getId());
        Label position = label(player.getPosition().name(), "squad-position");
        VBox identity = new VBox(3, label(player.getFullName(), "squad-player-name"),
                label(player.getAge(date) + " años  ·  " + role.apply(player), "squad-player-detail"));
        HBox.setHgrow(identity, Priority.ALWAYS);
        String availability = state == null || state.isAvailableOn(date) ? "DISPONIBLE"
                : "SUSPENSION".equals(state.getUnavailableReason()) ? "SANCIONADO" : "LESIONADO";
        HBox metrics = new HBox(14,
                metric("GRL", player.getOverall()), metric("FOR", state == null ? 0 : state.getForm()),
                metric("MOR", state == null ? 0 : state.getMorale()),
                metric("FIT", state == null ? 0 : state.getFitness()));
        metrics.setAlignment(Pos.CENTER_RIGHT);
        VBox finance = new VBox(3, label(String.format("€%.1fM", player.getMarketValue() / 1_000_000),
                "squad-player-value"), label(availability, availability.equals("DISPONIBLE")
                ? "squad-available" : "squad-unavailable"));
        finance.setAlignment(Pos.CENTER_RIGHT); finance.setMinWidth(100);
        HBox row = new HBox(13, position, identity, metrics, finance);
        row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("squad-player-row");
        setGraphic(row);
    }

    private static VBox metric(String caption, int value) {
        VBox metric = new VBox(1, label(String.valueOf(value), "squad-metric-value"),
                label(caption, "squad-metric-caption"));
        metric.setAlignment(Pos.CENTER); metric.setMinWidth(34); return metric;
    }

    private static Label label(String text, String style) {
        Label label = new Label(text); label.getStyleClass().add(style); return label;
    }
}
