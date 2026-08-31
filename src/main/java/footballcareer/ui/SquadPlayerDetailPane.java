package footballcareer.ui;

import footballcareer.model.Player;
import footballcareer.model.PlayerState;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

public final class SquadPlayerDetailPane extends VBox {
    public SquadPlayerDetailPane() {
        setSpacing(14); getStyleClass().add("squad-detail-pane");
        getChildren().setAll(label("SELECCIONA UN JUGADOR", "squad-detail-empty"),
                label("Su ficha deportiva aparecerá aquí.", "muted-label"));
    }

    public void show(Player player, PlayerState state, String role, LocalDate date) {
        Label overall = label(String.valueOf(player.getOverall()), "squad-detail-overall");
        VBox identity = new VBox(3, label(player.getFullName(), "squad-detail-name"),
                label(player.getPosition() + "  ·  " + player.getAge(date) + " AÑOS  ·  " + role,
                        "squad-player-detail"));
        HBox hero = new HBox(12, overall, identity); hero.setAlignment(Pos.CENTER_LEFT);
        GridPane attributes = new GridPane(); attributes.getStyleClass().add("squad-detail-grid");
        attributes.setHgap(8); attributes.setVgap(8);
        int[] values = {player.getPace(), player.getShooting(), player.getPassing(),
                player.getDribbling(), player.getDefending(), player.getPhysical()};
        String[] names = {"RIT", "TIR", "PAS", "REG", "DEF", "FÍS"};
        for (int index = 0; index < names.length; index++)
            attributes.add(metric(names[index], values[index]), index % 3, index / 3);
        VBox condition = new VBox(6,
                line("FORMA", state == null ? 0 : state.getForm()),
                line("MORAL", state == null ? 0 : state.getMorale()),
                line("FITNESS", state == null ? 0 : state.getFitness()));
        getChildren().setAll(hero, label("ATRIBUTOS", "field-caption"), attributes,
                label("ESTADO", "field-caption"), condition,
                label(String.format("VALOR DE MERCADO  ·  €%.1fM",
                        player.getMarketValue() / 1_000_000), "squad-detail-market"));
    }

    private VBox metric(String name, int value) {
        VBox box = new VBox(2, label(String.valueOf(value), "squad-detail-attribute"),
                label(name, "squad-metric-caption"));
        box.setAlignment(Pos.CENTER); box.getStyleClass().add("squad-detail-metric"); return box;
    }

    private HBox line(String name, int value) {
        Label number = label(String.valueOf(value), "squad-condition-value");
        HBox line = new HBox(10, label(name, "squad-condition-name"), number);
        line.setAlignment(Pos.CENTER_LEFT); return line;
    }

    private static Label label(String text, String style) {
        Label label = new Label(text); label.getStyleClass().add(style); label.setWrapText(true);
        return label;
    }
}
