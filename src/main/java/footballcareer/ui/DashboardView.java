package footballcareer.ui;

import footballcareer.model.Match;
import footballcareer.model.Team;
import footballcareer.service.CareerActivityService;
import footballcareer.service.CareerInsightService;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

public final class DashboardView {
    public record FormResult(String outcome, String detail) {}
    public record Model(String clubName, String seasonName, LocalDate date,
                        String reputation, String finance, String position,
                        Match nextMatch, long controlledTeamId, boolean matchToday,
                        String squadStatus, List<FormResult> recentForm,
                        List<CareerActivityService.Activity> activity, List<String> news,
                        List<CareerInsightService.Notification> notifications,
                        String advanceSummary) {}
    public record Actions(Runnable advanceDay, Runnable advanceWeek, Runnable simulateNext,
                          Runnable openInbox,
                          Consumer<CareerInsightService.Notification> openNotification) {}

    public VBox build(Model model, Actions actions) {
        VBox content = page("CENTRAL", model.clubName() + "  ·  " + model.seasonName());
        HBox context = new HBox(0,
                compactMetric("FECHA", model.date().toString()),
                compactMetric("POSICIÓN", model.position()),
                compactMetric("CONFIANZA", model.reputation()),
                compactMetric("PRESUPUESTO", model.finance()));
        context.getStyleClass().add("dashboard-context-strip");
        context.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
        VBox next = nextMatch(model);

        Button advance = button("AVANZAR UN DÍA", "primary-button", actions.advanceDay());
        Button week = button("AVANZAR HASTA 7 DÍAS", "secondary-button", actions.advanceWeek());
        advance.setDisable(model.matchToday());
        week.setDisable(model.matchToday());
        Button simulate = button(model.matchToday() ? "SIMULAR PARTIDO"
                : "IR AL PRÓXIMO PARTIDO", "secondary-button", actions.simulateNext());
        simulate.setDisable(model.nextMatch() == null && !model.matchToday());
        FlowPane actionBar = new FlowPane(12, 12, advance, week, simulate);

        VBox squad = new VBox(5, label("ESTADO DE LA PLANTILLA", "panel-title"),
                label(model.squadStatus(), "body-label"));
        squad.getStyleClass().add("dashboard-glance-item");
        VBox form = new VBox(7, label("FORMA RECIENTE", "panel-title"));
        form.getStyleClass().add("dashboard-glance-item");
        FlowPane chips = new FlowPane(8, 8);
        if (model.recentForm().isEmpty()) chips.getChildren().add(
                label("Aún no hay partidos disputados.", "muted-label"));
        for (FormResult result : model.recentForm()) {
            Label chip = label(result.outcome(), switch (result.outcome()) {
                case "V" -> "form-win";
                case "E" -> "form-draw";
                default -> "form-loss";
            });
            Tooltip.install(chip, new Tooltip(result.detail()));
            chips.getChildren().add(chip);
        }
        form.getChildren().add(chips);
        HBox secondary = new HBox(0, squad, form);
        secondary.getStyleClass().add("dashboard-glance-strip");
        HBox.setHgrow(squad, Priority.ALWAYS); HBox.setHgrow(form, Priority.ALWAYS);

        VBox news = panel("ACTUALIDAD DEL CLUB");
        model.news().forEach(item -> news.getChildren().add(label("•  " + item, "news-row")));
        model.activity().stream().limit(3).forEach(item -> news.getChildren().add(
                label(item.date() + "  ·  " + item.detail(), "dashboard-activity-line")));
        VBox inbox = panel("PENDIENTES");
        if (model.notifications().isEmpty()) inbox.getChildren().add(
                label("Todo al día. No tienes asuntos pendientes.", "muted-label"));
        else {
            model.notifications().stream().limit(3).forEach(item -> inbox.getChildren().add(
                    notificationRow(item, actions.openNotification())));
            inbox.getChildren().add(button("VER TODAS  //  " + model.notifications().size(),
                    "ghost-button", actions.openInbox()));
        }
        HBox management = new HBox(18, news, inbox);
        HBox.setHgrow(news, Priority.ALWAYS); HBox.setHgrow(inbox, Priority.ALWAYS);
        news.setMaxWidth(Double.MAX_VALUE); inbox.setMaxWidth(Double.MAX_VALUE);
        news.setPrefWidth(620); inbox.setPrefWidth(430);

        content.getChildren().addAll(next, actionBar, context, secondary, management);
        if (model.advanceSummary() != null) {
            VBox summary = panel("RESUMEN DEL AVANCE");
            summary.getChildren().add(label(model.advanceSummary(), "success-feedback"));
            content.getChildren().add(summary);
        }
        return content;
    }

    private VBox compactMetric(String title, String value) {
        VBox metric = new VBox(3, label(title, "dashboard-metric-title"),
                label(value, "dashboard-metric-value"));
        metric.getStyleClass().add("dashboard-metric");
        metric.setMaxWidth(Double.MAX_VALUE);
        return metric;
    }

    private VBox nextMatch(Model model) {
        VBox panel = panel("PRÓXIMO PARTIDO");
        panel.getStyleClass().add("featured-match");
        Match match = model.nextMatch();
        if (match == null) {
            panel.getChildren().add(label("No hay encuentros próximos.", "empty-title"));
            return panel;
        }
        long days = ChronoUnit.DAYS.between(model.date(), match.getDate());
        VBox home = team(match.getHomeTeam(), match.getHomeTeam().getId() == model.controlledTeamId());
        VBox away = team(match.getAwayTeam(), match.getAwayTeam().getId() == model.controlledTeamId());
        VBox versus = new VBox(2, label("VS", "versus"),
                label(days == 0 ? "HOY" : "EN " + days + " DÍAS", "match-countdown"));
        versus.setAlignment(Pos.CENTER);
        HBox pairing = new HBox(24, home, versus, away);
        pairing.setAlignment(Pos.CENTER);
        HBox.setHgrow(home, Priority.ALWAYS); HBox.setHgrow(away, Priority.ALWAYS);
        panel.getChildren().addAll(pairing, label(match.getCompetition().getName() + "  •  "
                + match.getDate() + "  •  " + match.getHomeTeam().getStadiumName(),
                "match-metadata"));
        return panel;
    }

    private VBox activity(List<CareerActivityService.Activity> items) {
        VBox activity = panel("HISTORIAL RECIENTE");
        if (items.isEmpty()) activity.getChildren().add(
                label("La actividad de tu carrera aparecerá aquí.", "body-label"));
        for (CareerActivityService.Activity item : items) {
            Label type = label(switch (item.type()) {
                case MATCH -> "PARTIDO"; case TRANSFER -> "FICHAJE"; case LOAN -> "CESIÓN";
                case TRAINING -> "SESIÓN"; case REPUTATION -> "DIRECTIVA";
            }, "objective-title");
            VBox copy = new VBox(2, label(item.detail(), "body-label"),
                    label(item.date().toString(), "muted-label"));
            HBox row = new HBox(14, type, copy); row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("objective-row"); activity.getChildren().add(row);
        }
        return activity;
    }

    private HBox notificationRow(CareerInsightService.Notification item,
            Consumer<CareerInsightService.Notification> action) {
        Label type = label(item.type().name(), item.urgent()
                ? "notification-type-urgent" : "notification-type");
        VBox copy = new VBox(3, label(item.title(), "notification-title"),
                label(item.detail(), "muted-label"));
        HBox.setHgrow(copy, Priority.ALWAYS);
        Button open = button("ABRIR", "ghost-button", () -> action.accept(item));
        HBox row = new HBox(12, type, copy, open); row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("notification-row", "notification-row-compact");
        return row;
    }

    private VBox team(Team team, boolean controlled) {
        VBox box = new VBox(8, TeamCrestView.create(team, controlled ? 104 : 96),
                label(team.getName(), "match-team-name"));
        if (controlled) box.getStyleClass().add("controlled-club-hero");
        box.setAlignment(Pos.CENTER); return box;
    }

    private VBox page(String title, String subtitle) {
        VBox header = new VBox(6, label(title, "page-title"), label(subtitle, "page-subtitle"));
        header.getStyleClass().add("page-header");
        VBox page = new VBox(22, header); page.getStyleClass().add("page"); return page;
    }
    private VBox panel(String title) {
        VBox panel = new VBox(12, label(title, "panel-title")); panel.getStyleClass().add("panel");
        return panel;
    }
    private VBox statCard(String title, String value) {
        VBox card = new VBox(8, label(title, "stat-title"), label(value, "stat-value"));
        card.getStyleClass().add("stat-card"); card.setMaxWidth(Double.MAX_VALUE); return card;
    }
    private Label label(String text, String style) {
        Label label = new Label(text); label.getStyleClass().add(style); label.setWrapText(true);
        return label;
    }
    private Button button(String text, String style, Runnable action) {
        Button button = new Button(text); button.getStyleClass().add(style);
        button.setTooltip(new Tooltip(text)); button.setOnAction(event -> action.run()); return button;
    }
}
