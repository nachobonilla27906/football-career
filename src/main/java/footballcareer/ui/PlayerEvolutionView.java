package footballcareer.ui;

import footballcareer.database.PlayerProgressRepository;
import footballcareer.model.Player;
import footballcareer.service.PlayerProgressSummaryService;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

public class PlayerEvolutionView {
    public VBox build(Player player, LocalDate date) {
        PlayerProgressRepository repository = new PlayerProgressRepository();
        repository.record(player.getId(), date, player.getOverall(), player.getMarketValue());
        var history = repository.find(player.getId());
        var summary = new PlayerProgressSummaryService().summarize(history);
        VBox panel = new VBox(12); panel.getStyleClass().add("panel");
        Label title = new Label("EVOLUCIÓN EN LA CARRERA"); title.getStyleClass().add("panel-title");
        Label trend = new Label(summary.trend() + "  •  GRL " + signed(summary.overallChange())
                + "  •  VALOR " + signedMillions(summary.valueChange()));
        trend.getStyleClass().add(summary.overallChange() < 0 ? "warning-feedback" : "success-feedback");
        LineChart<String, Number> overall = chart("MEDIA", new NumberAxis(40, 100, 10));
        XYChart.Series<String, Number> overallSeries = new XYChart.Series<>();
        history.forEach(point -> overallSeries.getData().add(
                new XYChart.Data<>(point.date().toString(), point.overall())));
        overall.getData().add(overallSeries);
        LineChart<String, Number> value = chart("VALOR (€M)", new NumberAxis());
        XYChart.Series<String, Number> valueSeries = new XYChart.Series<>();
        history.forEach(point -> valueSeries.getData().add(
                new XYChart.Data<>(point.date().toString(), point.marketValue() / 1_000_000)));
        value.getData().add(valueSeries);
        HBox charts = new HBox(14, overall, value);
        HBox.setHgrow(overall, Priority.ALWAYS); HBox.setHgrow(value, Priority.ALWAYS);
        panel.getChildren().addAll(title, trend, charts, muted(history.size() == 1
                ? "Punto inicial registrado; los cambios mensuales aparecerán automáticamente."
                : history.size() + " hitos desde " + history.getFirst().date() + "."));
        return panel;
    }

    private LineChart<String, Number> chart(String title, NumberAxis values) {
        LineChart<String, Number> chart = new LineChart<>(new CategoryAxis(), values);
        chart.setLegendVisible(false); chart.setAnimated(false); chart.setTitle(title);
        chart.setPrefHeight(230); chart.setMaxWidth(Double.MAX_VALUE); return chart;
    }

    private Label muted(String text) {
        Label label = new Label(text); label.getStyleClass().add("muted-label"); return label;
    }

    private String signed(int value) { return (value > 0 ? "+" : "") + value; }
    private String signedMillions(double value) {
        return (value > 0 ? "+" : "") + String.format("€%.1fM", value / 1_000_000);
    }
}
