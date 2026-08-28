package footballcareer.service;

import footballcareer.database.CareerRepository;
import footballcareer.database.SeasonRepository;
import footballcareer.database.TeamRepository;
import footballcareer.model.Career;
import footballcareer.model.Season;
import footballcareer.model.Team;

import java.time.LocalDate;
import java.util.List;

public class CareerService {

    private final CareerRepository careerRepository;
    private final TeamRepository teamRepository;
    private final SeasonRepository seasonRepository;
    private final WorldSimulationService worldSimulationService;
    private final SeasonTransitionService seasonTransitionService;

    public CareerService(
            CareerRepository careerRepository,
            TeamRepository teamRepository,
            SeasonRepository seasonRepository
    ) {
        this(careerRepository, teamRepository, seasonRepository, null);
    }

    public CareerService(
            CareerRepository careerRepository,
            TeamRepository teamRepository,
            SeasonRepository seasonRepository,
            MatchDayService matchDayService
    ) {
        this.careerRepository = careerRepository;
        this.teamRepository = teamRepository;
        this.seasonRepository = seasonRepository;
        this.worldSimulationService = new WorldSimulationService(matchDayService);
        this.seasonTransitionService = new SeasonTransitionService(
                seasonRepository, new footballcareer.database.CompetitionRepository(),
                new footballcareer.database.CompetitionTeamRepository(),
                careerRepository, new FootballWorldService());
    }

    public Career createCareer(
            String managerName,
            long teamId,
            long seasonId
    ) {

        if (managerName == null || managerName.isBlank()) {
            throw new IllegalArgumentException(
                    "Manager name is required."
            );
        }

        Team team = teamRepository.findById(teamId);

        if (team == null) {
            throw new IllegalArgumentException(
                    "Team does not exist."
            );
        }

        Season season = seasonRepository.findById(seasonId);

        if (season == null) {
            throw new IllegalArgumentException(
                    "Season does not exist."
            );
        }

        Career career = new Career(
                0,
                managerName,
                team,
                season,
                season.getStartDate()
        );

        careerRepository.save(career);
        new footballcareer.database.CareerMatchStateRepository().initialize(career, true);
        footballcareer.database.CareerContext.activate(career.getId());

        return career;
    }

    public Career loadCareer(long careerId) {

        Career career = careerRepository.findById(careerId);

        if (career == null) {
            throw new IllegalArgumentException(
                    "Career does not exist."
            );
        }

        new footballcareer.database.CareerMatchStateRepository().initialize(career, false);
        footballcareer.database.CareerContext.activate(career.getId());

        return career;
    }

    public void advanceDay(Career career) {
        advanceDate(career, false);
    }

    public void advanceDayForPlayer(Career career) {
        advanceDate(career, true);
    }

    private void advanceDate(Career career, boolean awaitControlledMatch) {

        if (career.getId() <= 0) {
            throw new IllegalArgumentException(
                    "Career must be saved before advancing time."
            );
        }

        LocalDate nextDate =
                career.getCurrentDate().plusDays(1);

        if (nextDate.isAfter(
                career.getCurrentSeason().getEndDate()
        )) {
            seasonTransitionService.startNextSeason(career);
            return;
        }

        career.setCurrentDate(nextDate);
        careerRepository.updateCurrentDate(career);

        if (awaitControlledMatch) {
            worldSimulationService.processDate(nextDate,
                    career.getCurrentSeason().getId(),
                    career.getControlledTeam().getId());
        } else {
            worldSimulationService.processDateFully(nextDate,
                    career.getCurrentSeason().getId(),
                    career.getControlledTeam().getId());
        }
    }

    public void advanceDays(Career career, int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Days to advance must be positive.");
        }
        for (int day = 0; day < days; day++) {
            advanceDay(career);
        }
    }

    public void advanceDaysForPlayer(Career career, int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Days to advance must be positive.");
        }
        for (int day = 0; day < days; day++) advanceDayForPlayer(career);
    }

    public List<footballcareer.model.Match> simulateControlledMatchesToday(Career career) {
        return worldSimulationService.simulateControlledMatches(
                career.getCurrentDate(), career.getControlledTeam().getId());
    }

    public List<Team> getAvailableTeams() {
        return teamRepository.findAll();
    }
}
