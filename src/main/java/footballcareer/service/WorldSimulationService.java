package footballcareer.service;

import footballcareer.database.PlayerRepository;
import footballcareer.database.PlayerStateRepository;
import footballcareer.model.Match;

import java.time.LocalDate;
import java.util.List;

public class WorldSimulationService {
    private final MatchDayService matchDayService;
    private final PlayerRepository playerRepository;
    private final PlayerStateRepository stateRepository;
    private final PlayerDevelopmentService developmentService;
    private final ClubTransferAiService transferAiService;

    public WorldSimulationService(MatchDayService matchDayService) {
        this(matchDayService, new PlayerRepository(), new PlayerStateRepository(),
                new ClubTransferAiService());
    }

    public WorldSimulationService(MatchDayService matchDayService,
            PlayerRepository playerRepository,
            PlayerStateRepository stateRepository) {
        this(matchDayService, playerRepository, stateRepository,
                new ClubTransferAiService());
    }

    public WorldSimulationService(MatchDayService matchDayService,
            PlayerRepository playerRepository,
            PlayerStateRepository stateRepository,
            ClubTransferAiService transferAiService) {
        this.matchDayService = matchDayService;
        this.playerRepository = playerRepository;
        this.stateRepository = stateRepository;
        this.developmentService = new PlayerDevelopmentService(playerRepository);
        this.transferAiService = transferAiService;
    }

    public List<Match> processDate(LocalDate date) {
        if (date == null) throw new IllegalArgumentException("Date is required.");
        stateRepository.recoverAllFitness(3);
        if (date.getDayOfMonth() == 1) {
            playerRepository.findAll().forEach(player ->
                    developmentService.applyMonthlyDevelopment(player, date));
        }
        return matchDayService == null ? List.of()
                : matchDayService.processMatchesOn(date);
    }

    public List<Match> processDate(LocalDate date, long seasonId,
            long controlledTeamId) {
        List<Match> matches = processDate(date);
        transferAiService.processMarket(date, seasonId, controlledTeamId);
        return matches;
    }
}
