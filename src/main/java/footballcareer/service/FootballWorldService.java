package footballcareer.service;

import footballcareer.database.CompetitionRepository;
import footballcareer.database.CompetitionTeamRepository;
import footballcareer.database.LeagueStandingRepository;
import footballcareer.database.MatchRepository;
import footballcareer.database.PlayerSeasonStatsRepository;
import footballcareer.model.Competition;

public class FootballWorldService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionTeamRepository competitionTeamRepository;
    private final LeagueStandingRepository standingRepository;
    private final ScheduleService scheduleService;

    public FootballWorldService() {
        MatchRepository matchRepository = new MatchRepository();
        competitionRepository = new CompetitionRepository();
        competitionTeamRepository = new CompetitionTeamRepository();
        standingRepository = new LeagueStandingRepository();
        scheduleService = new ScheduleService(
                competitionTeamRepository,
                matchRepository
        );
    }

    public void prepareSeason(long seasonId) {
        new PlayerSeasonStatsRepository().initializeForSeason(seasonId);
        for (Competition competition :
                competitionRepository.findBySeason(seasonId)) {
            var teams = competitionTeamRepository
                    .findTeamsByCompetition(competition.getId());
            standingRepository.initialize(competition, teams);
            if (competition.isEuropean()) {
                scheduleService.generateEuropeanLeaguePhase(competition);
            } else {
                scheduleService.generateLeagueSchedule(competition);
            }
        }
    }
}
