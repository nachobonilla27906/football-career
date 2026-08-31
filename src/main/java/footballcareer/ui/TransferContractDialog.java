package footballcareer.ui;

import footballcareer.model.Player;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.function.Consumer;

public final class TransferContractDialog {
    public record Terms(double salary, int years, double signingBonus,
                        double releaseClause, String role) {}

    public VBox build(Player player, double agreedFee, Consumer<Terms> submit, Runnable cancel) {
        TextField salary = field(String.format(Locale.ROOT, "%.2f", player.getSalary() / 1_000_000));
        TextField bonus = field("0.50");
        TextField clause = field(String.format(Locale.ROOT, "%.1f",
                Math.max(agreedFee * 1.5, player.getMarketValue() * 1.8) / 1_000_000));
        ComboBox<Integer> years = new ComboBox<>(); years.getItems().addAll(1, 2, 3, 4, 5); years.setValue(3);
        ComboBox<String> role = new ComboBox<>();
        role.getItems().addAll("CRUCIAL", "IMPORTANT", "ROTATION", "PROSPECT");
        role.setValue(player.getOverall() >= 82 ? "CRUCIAL" : player.getOverall() >= 76 ? "IMPORTANT" : "ROTATION");
        GridPane terms = new GridPane(); terms.setHgap(12); terms.setVgap(12);
        add(terms, 0, "SALARIO ANUAL (€M)", "Su sueldo actual es la referencia mínima.", salary);
        add(terms, 1, "DURACIÓN", "Años de contrato ofrecidos.", years);
        add(terms, 2, "PRIMA DE FIRMA (€M)", "Pago inmediato al jugador.", bonus);
        add(terms, 3, "ROL EN PLANTILLA", "Influye directamente en su disposición.", role);
        add(terms, 4, "CLÁUSULA (€M)", "Importe por el que podrá salir sin negociar.", clause);
        Label feedback = label("El agente comparará la propuesta con sueldo, nivel y rol actuales.", "muted-label");
        Button offer = button("OFRECER CONTRATO", "primary-button");
        Button back = button("RETIRARSE", "ghost-button");
        offer.setOnAction(event -> {
            try {
                submit.accept(new Terms(number(salary), years.getValue(), number(bonus),
                        number(clause), role.getValue()));
            } catch (NumberFormatException exception) {
                feedback.setText("Revisa las cantidades de la propuesta.");
            }
        });
        back.setOnAction(event -> cancel.run());
        VBox card = new VBox(16, label("ACUERDO CON EL CLUB", "contract-step-done"),
                label("NEGOCIAR CONTRATO", "form-title"),
                label(player.getFullName(), "match-highlight"),
                label(String.format("Traspaso acordado por €%.1fM", agreedFee / 1_000_000), "comparison-label"),
                terms, feedback, new HBox(10, offer, back));
        card.getStyleClass().addAll("in-app-dialog", "contract-dialog"); card.setPrefWidth(560);
        return card;
    }

    private void add(GridPane grid, int row, String title, String help, javafx.scene.Node field) {
        VBox copy = new VBox(2, label(title, "field-label"), label(help, "field-help"));
        grid.add(copy, 0, row); grid.add(field, 1, row);
    }
    private TextField field(String value) { return new TextField(value); }
    private double number(TextField field) { return Double.parseDouble(field.getText().replace(',', '.')) * 1_000_000; }
    private Label label(String text, String style) { Label label = new Label(text); label.getStyleClass().add(style); label.setWrapText(true); return label; }
    private Button button(String text, String style) { Button button = new Button(text); button.getStyleClass().add(style); return button; }
}
