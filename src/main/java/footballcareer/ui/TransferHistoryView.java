package footballcareer.ui;

import footballcareer.service.TransferHistoryService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Function;

public final class TransferHistoryView {
    public VBox build(String clubName, long teamId) {
        TransferHistoryService service = new TransferHistoryService();
        VBox content = new VBox(20, label("HISTORIAL DE FICHAJES", "page-title"),
                label(clubName + "  •  Fichajes y negociaciones", "page-subtitle"));
        content.getStyleClass().add("page");
        TableView<TransferHistoryService.Entry> completed = table(
                service.completed(teamId), "Todavía no hay fichajes completados.");
        TableView<TransferHistoryService.Entry> negotiations = table(
                service.negotiations(teamId), "Todavía no hay negociaciones registradas.");
        VBox completedPanel = panel("OPERACIONES COMPLETADAS", completed);
        VBox negotiationPanel = panel("NEGOCIACIONES", negotiations);
        SplitPane records = new SplitPane(completedPanel, negotiationPanel);
        records.setOrientation(javafx.geometry.Orientation.VERTICAL);
        records.setDividerPositions(0.48);
        VBox.setVgrow(records, Priority.ALWAYS);
        content.getChildren().add(records);
        return content;
    }

    private TableView<TransferHistoryService.Entry> table(
            List<TransferHistoryService.Entry> rows, String emptyText) {
        TableView<TransferHistoryService.Entry> table = new TableView<>();
        column(table, "TIPO", 90, TransferHistoryService.Entry::direction);
        column(table, "JUGADOR", 220, TransferHistoryService.Entry::player);
        column(table, "ORIGEN", 90, TransferHistoryService.Entry::fromClub);
        column(table, "DESTINO", 90, TransferHistoryService.Entry::toClub);
        column(table, "IMPORTE", 110, row -> String.format("€%.1fM", row.amount() / 1_000_000));
        column(table, "FECHA", 110, row -> row.date().toString());
        column(table, "ESTADO", 110, TransferHistoryService.Entry::status);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(label(emptyText, "muted-label"));
        table.getItems().setAll(rows);
        return table;
    }

    private <T> void column(TableView<TransferHistoryService.Entry> table, String title,
            double width, Function<TransferHistoryService.Entry, T> value) {
        TableColumn<TransferHistoryService.Entry, T> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        table.getColumns().add(column);
    }

    private VBox panel(String title, TableView<TransferHistoryService.Entry> table) {
        VBox panel = new VBox(10, label(title, "panel-title"), table);
        panel.getStyleClass().add("panel"); VBox.setVgrow(table, Priority.ALWAYS); return panel;
    }

    private Label label(String text, String style) {
        Label label = new Label(text); label.getStyleClass().add(style); label.setWrapText(true);
        return label;
    }
}
