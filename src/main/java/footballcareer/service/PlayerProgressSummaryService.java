package footballcareer.service;

import footballcareer.database.PlayerProgressRepository;

import java.util.List;

public class PlayerProgressSummaryService {
    public record Summary(int overallChange, double valueChange, String trend) {}

    public Summary summarize(List<PlayerProgressRepository.Snapshot> history) {
        if (history == null || history.size() < 2) return new Summary(0, 0, "Sin tendencia todavía");
        var first = history.getFirst();
        var last = history.getLast();
        int overall = last.overall() - first.overall();
        double value = last.marketValue() - first.marketValue();
        String trend = overall > 0 || value > 0 ? "EN ASCENSO"
                : overall < 0 || value < 0 ? "EN DESCENSO" : "ESTABLE";
        return new Summary(overall, value, trend);
    }
}
