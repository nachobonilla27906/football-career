package footballcareer.ui;

import footballcareer.model.Player;
import footballcareer.model.Team;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public final class MarketPlayerCell extends ListCell<Player> {
    private final Map<Long, Double> prices;
    private final Map<Long, Team> clubs;
    private final Set<Long> shortlist;
    private final LocalDate date;

    public MarketPlayerCell(Map<Long, Double> prices, Map<Long, Team> clubs,
            Set<Long> shortlist, LocalDate date) {
        this.prices = prices;
        this.clubs = clubs;
        this.shortlist = shortlist;
        this.date = date;
        getStyleClass().add("market-player-cell");
    }

    @Override
    protected void updateItem(Player player, boolean empty) {
        super.updateItem(player, empty);
        setText(null);
        setGraphic(null);
        if (empty || player == null) return;
        Team club = clubs.get(player.getId());
        Double price = prices.get(player.getId());
        Node crest = club == null ? badge(player.getPosition().name(), "market-position-badge")
                : TeamCrestView.create(club, 40);
        Label name = label((shortlist.contains(player.getId()) ? "★  " : "")
                + player.getFullName(), "market-row-name");
        Label detail = label(player.getPosition() + "  ·  " + player.getAge(date) + " años  ·  "
                + (club == null ? "Sin club" : club.getName()), "market-row-detail");
        VBox identity = new VBox(3, name, detail);
        HBox.setHgrow(identity, Priority.ALWAYS);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label overall = badge(String.valueOf(player.getOverall()), "market-overall-badge");
        Label value = label(price == null
                ? String.format("VALOR  €%.1fM", player.getMarketValue() / 1_000_000)
                : String.format("EN VENTA  €%.1fM", price / 1_000_000),
                price == null ? "market-value-label" : "market-listed-label");
        VBox finance = new VBox(3, overall, value);
        finance.setAlignment(Pos.CENTER_RIGHT);
        HBox row = new HBox(12, crest, identity, spacer, finance);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("market-player-row");
        setGraphic(row);
    }

    private static Label badge(String text, String style) {
        Label label = label(text, style);
        label.setAlignment(Pos.CENTER);
        return label;
    }

    private static Label label(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        return label;
    }
}
