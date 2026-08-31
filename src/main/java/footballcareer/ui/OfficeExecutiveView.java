package footballcareer.ui;

import footballcareer.model.ClubFinance;
import footballcareer.service.CareerInsightService;
import footballcareer.service.ManagerEvaluationService;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public final class OfficeExecutiveView {
    public record Model(ManagerEvaluationService.Evaluation evaluation, ClubFinance finance,
                        String leaguePosition, List<CareerInsightService.Objective> objectives,
                        int pendingDecisions) {}
    public record Actions(Runnable market, Runnable squad, Runnable training) {}

    public VBox build(Model model, Actions actions) {
        VBox confidence = confidence(model.evaluation(), model.leaguePosition());
        VBox finance = finance(model.finance());
        VBox priorities = priorities(model.objectives(), model.pendingDecisions());
        HBox grid = new HBox(14, confidence, finance, priorities);
        for (javafx.scene.Node node : grid.getChildren()) HBox.setHgrow(node, Priority.ALWAYS);
        Button market = action("ABRIR MERCADO", actions.market());
        Button squad = action("GESTIONAR PLANTILLA", actions.squad());
        Button training = action("PLANIFICAR ENTRENO", actions.training());
        HBox decisions = new HBox(10, label("DECISIONES RÁPIDAS", "office-section-label"),
                market, squad, training); decisions.setAlignment(Pos.CENTER_LEFT);
        decisions.getStyleClass().add("office-decision-bar");
        VBox executive = new VBox(14, grid, decisions);
        executive.getStyleClass().add("office-executive"); return executive;
    }

    private VBox confidence(ManagerEvaluationService.Evaluation evaluation, String leaguePosition) {
        ProgressBar progress = new ProgressBar(evaluation.confidence() / 100.0);
        progress.setMaxWidth(Double.MAX_VALUE);
        String context = evaluation.status() + "  ·  LIGA " + leaguePosition;
        VBox box = card("CONFIANZA DE LA DIRECTIVA", String.valueOf(evaluation.confidence()),
                context); box.getChildren().add(progress);
        if (!evaluation.reasons().isEmpty()) box.getChildren().add(
                label(evaluation.reasons().getFirst(), "office-card-note"));
        return box;
    }

    private VBox finance(ClubFinance finance) {
        if (finance == null) return card("FINANZAS", "—", "SIN DATOS");
        VBox box = card("PRESUPUESTO DE FICHAJES",
                money(finance.getTransferBudget()), "BALANCE  " + money(finance.getBalance()));
        box.getChildren().add(label("MARGEN SALARIAL  "
                + money(finance.getAvailableWageBudget()), "office-card-note"));
        return box;
    }

    private VBox priorities(List<CareerInsightService.Objective> objectives, int pending) {
        long risk = objectives.stream().filter(item -> item.status()
                == CareerInsightService.Status.AT_RISK).count();
        VBox box = card("PRIORIDADES", String.valueOf(risk + pending), "REQUIEREN ATENCIÓN");
        objectives.stream().filter(item -> item.status() == CareerInsightService.Status.AT_RISK)
                .limit(2).forEach(item -> box.getChildren().add(
                        label("•  " + item.title(), "office-priority-risk")));
        if (risk == 0 && pending == 0) box.getChildren().add(
                label("El proyecto está bajo control.", "office-card-note"));
        return box;
    }

    private VBox card(String title, String value, String subtitle) {
        VBox box = new VBox(6, label(title, "office-section-label"),
                label(value, "office-hero-value"), label(subtitle, "office-card-note"));
        box.getStyleClass().add("office-executive-card"); box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }
    private Button action(String text, Runnable action) { Button button = new Button(text);
        button.getStyleClass().add("ghost-button"); button.setOnAction(event -> action.run()); return button; }
    private String money(double value) { return String.format("€%.1fM", value / 1_000_000); }
    private Label label(String text, String style) { Label label = new Label(text);
        label.getStyleClass().add(style); label.setWrapText(true); return label; }
}
