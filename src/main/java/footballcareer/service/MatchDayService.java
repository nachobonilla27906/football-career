package footballcareer.service;

import footballcareer.database.LeagueStandingRepository;
import footballcareer.database.MatchRepository;
import footballcareer.model.Match;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MatchDayService {

    private final MatchRepository matchRepository;
    private final LeagueStandingRepository standingRepository;
    private final MatchSimulationService simulationService;

    public MatchDayService(
            MatchRepository matchRepository,
            LeagueStandingRepository standingRepository,
            MatchSimulationService simulationService
    ) {
        this.matchRepository = matchRepository;
        this.standingRepository = standingRepository;
        this.simulationService = simulationService;
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
            processed.add(match);
        }

        return processed;
    }
}
