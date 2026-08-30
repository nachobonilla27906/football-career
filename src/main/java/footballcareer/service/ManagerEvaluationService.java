package footballcareer.service;

import footballcareer.database.ClubFinanceRepository;
import footballcareer.database.CompetitionTeamRepository;
import footballcareer.database.LeagueStandingRepository;
import footballcareer.model.Career;
import footballcareer.model.Competition;
import footballcareer.model.LeagueStanding;

import java.util.ArrayList;
import java.util.List;

public class ManagerEvaluationService {
    public record Evaluation(int confidence, String status, boolean dismissalRisk,
                             List<String> reasons) {}

    public Evaluation evaluate(Career career) {
        int confidence = 70;
        List<String> reasons = new ArrayList<>();
        List<Competition> competitions = new CompetitionTeamRepository()
                .findCompetitionsByTeam(career.getControlledTeam().getId()).stream()
                .filter(item -> item.getSeason().getId() == career.getCurrentSeason().getId())
                .toList();
        if (!competitions.isEmpty()) {
            List<LeagueStanding> table = new LeagueStandingRepository()
                    .findByCompetition(competitions.getFirst().getId());
            int position = 0;
            for (int index = 0; index < table.size(); index++) {
                if (table.get(index).getTeam().getId() == career.getControlledTeam().getId()) {
                    position = index + 1;
                    break;
                }
            }
            boolean started = table.stream().anyMatch(row -> row.getPlayed() >= 5);
            int target = career.getControlledTeam().getReputation() >= 90 ? 2 : 6;
            if (started && position > target + 5) {
                confidence -= 35;
                reasons.add("La posición liguera está muy por debajo del objetivo.");
            } else if (started && position > target) {
                confidence -= 18;
                reasons.add("La directiva espera una mejora en liga.");
            } else if (started) {
                confidence += 10;
                reasons.add("El objetivo liguero está encarrilado.");
            }
        }
        var finance = new ClubFinanceRepository().findByTeam(career.getControlledTeam().getId());
        if (finance != null && finance.getAvailableWageBudget() < 0) {
            confidence -= 25;
            reasons.add("El presupuesto salarial está excedido.");
        }
        int score = Math.max(0, Math.min(100, confidence));
        String status = score < 30 ? "CRÍTICA" : score < 50 ? "EN RIESGO"
                : score < 75 ? "ESTABLE" : "ALTA";
        return new Evaluation(score, status, score < 25, List.copyOf(reasons));
    }
}
