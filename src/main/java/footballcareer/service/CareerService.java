package footballcareer.service;

import footballcareer.database.CareerRepository;
import footballcareer.database.SeasonRepository;
import footballcareer.database.TeamRepository;
import footballcareer.model.Career;
import footballcareer.model.Season;
import footballcareer.model.Team;

import java.time.LocalDate;

public class CareerService {

    private final CareerRepository careerRepository;
    private final TeamRepository teamRepository;
    private final SeasonRepository seasonRepository;

    public CareerService(
            CareerRepository careerRepository,
            TeamRepository teamRepository,
            SeasonRepository seasonRepository
    ) {
        this.careerRepository = careerRepository;
        this.teamRepository = teamRepository;
        this.seasonRepository = seasonRepository;
    }

    public Career createCareer(
            String managerName,
            long teamId,
            long seasonId
    ) {

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

        LocalDate nextDate =
                career.getCurrentDate().plusDays(1);

        career.setCurrentDate(nextDate);
    }
}