package footballcareer.service;

import footballcareer.database.LeagueStandingRepository;
import footballcareer.database.MatchRepository;
import footballcareer.database.MatchEventRepository;
import footballcareer.database.MatchTeamStatsRepository;
import footballcareer.database.PlayerRepository;
import footballcareer.database.PlayerStateRepository;
import footballcareer.model.Match;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MatchDayService {

    private final MatchRepository matchRepository;
    private final LeagueStandingRepository standingRepository;
    private final MatchSimulationService simulationService;
    private final PlayerMatchService playerMatchService;
    private final MatchEventGenerationService eventGenerationService;
    private final MatchStatisticsService statisticsService;
    private final LightweightMatchSimulationService lightweightSimulationService;

    public MatchDayService(
            MatchRepository matchRepository,
            LeagueStandingRepository standingRepository,
            MatchSimulationService simulationService
    ) {
        this(matchRepository, standingRepository, simulationService, null,
                defaultEventGenerationService(), defaultStatisticsService());
    }

    public MatchDayService(
            MatchRepository matchRepository,
            LeagueStandingRepository standingRepository,
            MatchSimulationService simulationService,
            PlayerMatchService playerMatchService
    ) {
        this(matchRepository, standingRepository, simulationService,
                playerMatchService, defaultEventGenerationService(),
                defaultStatisticsService());
    }

    public MatchDayService(MatchRepository matchRepository,
            LeagueStandingRepository standingRepository,
            MatchSimulationService simulationService,
            PlayerMatchService playerMatchService,
            MatchEventGenerationService eventGenerationService) {
        this(matchRepository, standingRepository, simulationService,
                playerMatchService, eventGenerationService,
                defaultStatisticsService());
    }

    public MatchDayService(MatchRepository matchRepository,
            LeagueStandingRepository standingRepository,
            MatchSimulationService simulationService,
            PlayerMatchService playerMatchService,
            MatchEventGenerationService eventGenerationService,
            MatchStatisticsService statisticsService) {
        this.matchRepository = matchRepository;
        this.standingRepository = standingRepository;
        this.simulationService = simulationService;
        this.playerMatchService = playerMatchService;
        this.eventGenerationService = eventGenerationService;
        this.statisticsService = statisticsService;
        this.lightweightSimulationService = new LightweightMatchSimulationService();
    }

    public List<Match> processMatchesOn(LocalDate date) {
        List<Match> processed = new ArrayList<>();

        for (Match match : matchRepository.findByDate(date)) {
            if (match.isPlayed()) {
                continue;
            }

            simulationService.simulate(match);
            standingRepository.applyResult(match);
            matchRepository.updateResult(match);
            eventGenerationService.generate(match);
            statisticsService.generate(match);
            if (playerMatchService != null) {
                playerMatchService.process(match);
            }
            processed.add(match);
        }

        return processed;
    }

    public List<Match> processBackgroundMatchesOn(LocalDate date, long controlledTeamId) {
        List<Match> processed = new ArrayList<>();
        for (Match match : matchRepository.findByDate(date)) {
            if (match.isPlayed() || involves(match, controlledTeamId)) continue;
            lightweightSimulationService.simulate(match);
            standingRepository.applyResult(match);
            matchRepository.updateResult(match);
            processed.add(match);
        }
        return processed;
    }

    public List<Match> processControlledMatchesOn(LocalDate date, long controlledTeamId) {
        List<Match> processed = new ArrayList<>();
        for (Match match : matchRepository.findByDate(date)) {
            if (match.isPlayed() || !involves(match, controlledTeamId)) continue;
            simulationService.simulate(match);
            standingRepository.applyResult(match);
            matchRepository.updateResult(match);
            eventGenerationService.generate(match);
            statisticsService.generate(match);
            if (playerMatchService != null) playerMatchService.process(match);
            processed.add(match);
        }
        return processed;
    }

    private boolean involves(Match match, long teamId) {
        return match.getHomeTeam().getId() == teamId
                || match.getAwayTeam().getId() == teamId;
    }

    private static MatchEventGenerationService defaultEventGenerationService() {
        PlayerStateRepository states = new PlayerStateRepository();
        return new MatchEventGenerationService(
                new LineupService(new PlayerRepository(), states),
                new MatchEventRepository(), new Random());
    }

    private static MatchStatisticsService defaultStatisticsService() {
        return new MatchStatisticsService(new MatchEventRepository(),
                new MatchTeamStatsRepository(), new Random());
    }
}
