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
    private final MatchDayService matchDayService;

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
        this.matchDayService = matchDayService;
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

        return career;
    }

    public Career loadCareer(long careerId) {

        Career career = careerRepository.findById(careerId);

        if (career == null) {
            throw new IllegalArgumentException(
                    "Career does not exist."
            );
        }

        return career;
    }

    public void advanceDay(Career career) {

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
            throw new IllegalStateException(
                    "Career has reached the end of the season."
            );
        }

        career.setCurrentDate(nextDate);
        careerRepository.updateCurrentDate(career);

        if (matchDayService != null) {
            matchDayService.processMatchesOn(nextDate);
        }
    }

    public List<Team> getAvailableTeams() {
        return teamRepository.findAll();
    }
}
