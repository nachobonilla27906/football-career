package footballcareer.ui;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public final class TransferContractFields {
    public FlowPane build(Node salary, Node years, Node signingBonus, Node releaseClause,
            Node squadRole, Node upfront, Node appearanceBonus) {
        return new FlowPane(10, 10,
                field("SALARIO ANUAL (€M)", "Cantidad anual que ocupará margen salarial.", salary),
                field("DURACIÓN (AÑOS)", "Temporadas de vigencia del contrato.", years),
                field("PRIMA DE FICHAJE (€M)", "Pago inmediato al jugador al firmar.", signingBonus),
                field("CLÁUSULA (€M)", "Importe por el que otros clubes podrán liberarlo.", releaseClause),
                field("ROL PROMETIDO", "Importancia y minutos que espera el jugador.", squadRole),
                field("PAGO INICIAL (%)", "50/75% aplaza el resto en dos cuotas semestrales.", upfront),
                field("BONUS 10 PARTIDOS (€M)", "Variable pagada cuando alcance diez apariciones.", appearanceBonus));
    }

    private VBox field(String title, String explanation, Node control) {
        Label label = new Label(title); label.getStyleClass().add("field-label");
        Tooltip tooltip = new Tooltip(explanation); Tooltip.install(label, tooltip);
        if (control instanceof javafx.scene.control.Control input) input.setTooltip(tooltip);
        VBox box = new VBox(5, label, control); box.getStyleClass().add("contract-field"); return box;
    }
}
