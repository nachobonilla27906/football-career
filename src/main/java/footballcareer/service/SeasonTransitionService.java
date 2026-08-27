package footballcareer.service;

import footballcareer.database.CareerRepository;
import footballcareer.database.CompetitionRepository;
import footballcareer.database.CompetitionTeamRepository;
import footballcareer.database.SeasonRepository;
import footballcareer.model.Career;
import footballcareer.model.Competition;
import footballcareer.model.Season;

public class SeasonTransitionService {
    private final SeasonRepository seasonRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionTeamRepository competitionTeamRepository;
    private final CareerRepository careerRepository;
    private final FootballWorldService footballWorldService;

    public SeasonTransitionService(SeasonRepository seasonRepository,
            CompetitionRepository competitionRepository,
            CompetitionTeamRepository competitionTeamRepository,
            CareerRepository careerRepository,
            FootballWorldService footballWorldService) {
        this.seasonRepository = seasonRepository;
        this.competitionRepository = competitionRepository;
        this.competitionTeamRepository = competitionTeamRepository;
        this.careerRepository = careerRepository;
        this.footballWorldService = footballWorldService;
    }

    public Season startNextSeason(Career career) {
        Season current = career.getCurrentSeason();
        if (career.getCurrentDate().isBefore(current.getEndDate())) {
            throw new IllegalStateException("Current season has not ended.");
        }
        seasonRepository.markFinished(current.getId());
        current.setFinished(true);

        Season next = seasonRepository.findByYears(
                current.getStartYear() + 1, current.getEndYear() + 1);
        if (next == null) {
            next = new Season(0, current.getStartYear() + 1,
                    current.getEndYear() + 1, current.getStartDate().plusYears(1),
                    current.getEndDate().plusYears(1));
            seasonRepository.save(next);
            cloneCompetitions(current, next);
            footballWorldService.prepareSeason(next.getId());
        }

        career.setCurrentSeason(next);
        career.setCurrentDate(next.getStartDate());
        careerRepository.updateSeasonAndDate(career);
        return next;
    }

    private void cloneCompetitions(Season current, Season next) {
        for (Competition oldCompetition :
                competitionRepository.findBySeason(current.getId())) {
            Competition newCompetition = new Competition(0,
                    oldCompetition.getName(), oldCompetition.getCountry(),
                    oldCompetition.getTier(), next, oldCompetition.getLeague());
            competitionRepository.save(newCompetition);
            competitionTeamRepository.findTeamsByCompetition(oldCompetition.getId())
                    .forEach(team -> competitionTeamRepository.addTeamToCompetition(
                            newCompetition.getId(), team.getId()));
        }
    }
}
