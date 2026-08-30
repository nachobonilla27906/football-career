package footballcareer.service;

import footballcareer.database.*;
import footballcareer.model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CareerInsightService {
    public enum Status { ON_TRACK, AT_RISK, PENDING }
    public enum NotificationType { MATCH, TRANSFER, FITNESS, TRAINING, MEDICAL, PLAYER, BOARD }
    public record Objective(String title, String detail, Status status) {}
    public record Notification(NotificationType type, String title, String detail,
                               boolean urgent) {}

    public List<Objective> objectives(Career career) {
        List<Objective> objectives = new ArrayList<>();
        List<Competition> competitions = competitions(career);
        if (!competitions.isEmpty()) {
            List<LeagueStanding> table = new LeagueStandingRepository()
                    .findByCompetition(competitions.getFirst().getId());
            int position = position(table, career.getControlledTeam().getId());
            int target = career.getControlledTeam().getReputation() >= 90 ? 1 : 2;
            boolean seasonStarted = table.stream().anyMatch(row -> row.getPlayed() > 0);
            objectives.add(new Objective("OBJETIVO DE LIGA",
                    "Terminar entre los " + target + " primeros  •  Posición actual: "
                            + (position == 0 ? "—" : position + "º"),
                    !seasonStarted ? Status.PENDING
                            : position <= target ? Status.ON_TRACK : Status.AT_RISK));
        }
        ClubFinance finance = new ClubFinanceRepository()
                .findByTeam(career.getControlledTeam().getId());
        if (finance != null) {
            objectives.add(new Objective("CONTROL FINANCIERO",
                    String.format("Margen salarial disponible: €%.1fM",
                            finance.getAvailableWageBudget() / 1_000_000),
                    finance.getAvailableWageBudget() >= 0 ? Status.ON_TRACK : Status.AT_RISK));
        }
        double fitness = averageFitness(career.getControlledTeam().getId());
        objectives.add(new Objective("GESTIÓN FÍSICA",
                String.format("Mantener el fitness medio por encima de 75  •  Actual: %.0f", fitness),
                fitness >= 75 ? Status.ON_TRACK : Status.AT_RISK));
        return objectives;
    }

    public List<String> news(Career career) {
        List<String> news = new ArrayList<>();
        long teamId = career.getControlledTeam().getId();
        List<Match> clubMatches = competitions(career).stream()
                .flatMap(competition -> new MatchRepository()
                        .findByCompetition(competition.getId()).stream())
                .filter(match -> match.getHomeTeam().getId() == teamId
                        || match.getAwayTeam().getId() == teamId).toList();
        clubMatches.stream().filter(match -> !match.isPlayed()
                        && !match.getDate().isBefore(career.getCurrentDate()))
                .min(Comparator.comparing(Match::getDate)).ifPresent(match -> news.add(
                        "Próximo partido: " + match.getHomeTeam().getShortName() + " vs "
                                + match.getAwayTeam().getShortName() + " el " + match.getDate() + "."));
        clubMatches.stream().filter(Match::isPlayed)
                .max(Comparator.comparing(Match::getDate)).ifPresent(match -> news.add(
                        "Último resultado: " + match.getHomeTeam().getShortName() + " "
                                + match.getHomeGoals() + "–" + match.getAwayGoals() + " "
                                + match.getAwayTeam().getShortName() + "."));
        int offers = new TransferOfferRepository().findPendingBySellingTeam(teamId).size();
        if (offers > 0) news.add("Tienes " + offers + " oferta(s) de fichaje esperando respuesta.");
        long tired = tiredPlayers(teamId);
        if (tired > 0) news.add("Alerta física: " + tired + " jugador(es) tienen fitness inferior a 70.");
        TrainingService.TrainingType training = new TrainingService().findToday(career);
        if (training == null) {
            news.add("Entrenamiento pendiente: elige la carga de trabajo para hoy.");
        } else {
            news.add("Entrenamiento diario completado: " + training.name().toLowerCase() + ".");
        }
        if (news.isEmpty()) news.add("Sin novedades importantes. Puedes preparar el próximo encuentro.");
        return news;
    }

    public List<Notification> notifications(Career career) {
        List<Notification> notifications = new ArrayList<>();
        long teamId = career.getControlledTeam().getId();
        List<TransferOffer> offers = new TransferOfferRepository()
                .findPendingBySellingTeam(teamId);
        if (!offers.isEmpty()) {
            notifications.add(new Notification(NotificationType.TRANSFER,
                    offers.size() == 1 ? "Nueva oferta recibida"
                            : offers.size() + " ofertas recibidas",
                    "Hay propuestas pendientes de respuesta en Traspasos.", true));
        }

        competitions(career).stream()
                .flatMap(competition -> new MatchRepository()
                        .findByCompetition(competition.getId()).stream())
                .filter(match -> !match.isPlayed())
                .filter(match -> match.getHomeTeam().getId() == teamId
                        || match.getAwayTeam().getId() == teamId)
                .filter(match -> !match.getDate().isBefore(career.getCurrentDate()))
                .min(Comparator.comparing(Match::getDate)).ifPresent(match -> {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(
                            career.getCurrentDate(), match.getDate());
                    if (days <= 3) notifications.add(new Notification(NotificationType.MATCH,
                            days == 0 ? "Partido hoy" : "Partido en " + days + " días",
                            match.getHomeTeam().getShortName() + " vs "
                                    + match.getAwayTeam().getShortName() + " · "
                                    + match.getCompetition().getName(), days == 0));
                });

        long tired = tiredPlayers(teamId);
        if (tired > 0) notifications.add(new Notification(NotificationType.FITNESS,
                "Plantilla fatigada", tired + " jugador(es) están por debajo de 70 de fitness.",
                tired >= 4));
        long unavailable = unavailablePlayers(teamId, career.getCurrentDate());
        if (unavailable > 0) notifications.add(new Notification(NotificationType.MEDICAL,
                "Bajas en la plantilla", unavailable + " jugador(es) no están disponibles.", true));
        if (new TrainingService().findToday(career) == null) {
            notifications.add(new Notification(NotificationType.TRAINING,
                    "Entrenamiento pendiente",
                    "Todavía no has elegido la sesión de entrenamiento de hoy.", false));
        }
        List<SquadDynamicsService.Concern> concerns = new SquadDynamicsService()
                .concerns(teamId);
        if (!concerns.isEmpty()) {
            SquadDynamicsService.Concern concern = concerns.getFirst();
            notifications.add(new Notification(NotificationType.PLAYER,
                    "Jugador descontento: " + concern.player().getFullName(),
                    concern.role() + "  •  Moral " + concern.morale()
                            + "  •  " + concern.message(), concern.morale() < 30));
        }
        ManagerEvaluationService.Evaluation evaluation = new ManagerEvaluationService()
                .evaluate(career);
        if (evaluation.confidence() < 50) notifications.add(new Notification(
                NotificationType.BOARD, "La directiva evalúa tu continuidad",
                "Confianza " + evaluation.confidence() + "/100  •  " + evaluation.status(),
                evaluation.dismissalRisk()));
        return notifications;
    }

    private List<Competition> competitions(Career career) {
        return new CompetitionTeamRepository().findCompetitionsByTeam(
                career.getControlledTeam().getId()).stream()
                .filter(competition -> competition.getSeason().getId()
                        == career.getCurrentSeason().getId()).toList();
    }

    private int position(List<LeagueStanding> table, long teamId) {
        for (int index = 0; index < table.size(); index++)
            if (table.get(index).getTeam().getId() == teamId) return index + 1;
        return 0;
    }

    private double averageFitness(long teamId) {
        PlayerStateRepository states = new PlayerStateRepository();
        java.util.Map<Long, PlayerState> allStates = states.findAll();
        return new PlayerRepository().findCurrentPlayersByTeam(teamId).stream()
                .map(player -> allStates.get(player.getId()))
                .filter(java.util.Objects::nonNull).mapToInt(PlayerState::getFitness)
                .average().orElse(0);
    }

    private long tiredPlayers(long teamId) {
        PlayerStateRepository states = new PlayerStateRepository();
        java.util.Map<Long, PlayerState> allStates = states.findAll();
        return new PlayerRepository().findCurrentPlayersByTeam(teamId).stream()
                .map(player -> allStates.get(player.getId()))
                .filter(java.util.Objects::nonNull).filter(state -> state.getFitness() < 70).count();
    }

    private long unavailablePlayers(long teamId, java.time.LocalDate date) {
        java.util.Map<Long, PlayerState> allStates = new PlayerStateRepository().findAll();
        return new PlayerRepository().findCurrentPlayersByTeam(teamId).stream()
                .map(player -> allStates.get(player.getId()))
                .filter(java.util.Objects::nonNull)
                .filter(state -> !state.isAvailableOn(date)).count();
    }
}
