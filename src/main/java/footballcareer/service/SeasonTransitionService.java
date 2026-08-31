package footballcareer.service;

import footballcareer.database.CareerRepository;
import footballcareer.database.CompetitionRepository;
import footballcareer.database.CompetitionTeamRepository;
import footballcareer.database.SeasonRepository;
import footballcareer.model.Career;
import footballcareer.model.Competition;
import footballcareer.model.Season;
import footballcareer.model.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        List<Competition> oldCompetitions = competitionRepository.findBySeason(current.getId());
        for (Competition oldCompetition : oldCompetitions.stream()
                .filter(competition -> !competition.isEuropean()).toList()) {
            Competition newCompetition = new Competition(0,
                    oldCompetition.getName(), oldCompetition.getCountry(),
                    oldCompetition.getTier(), next, oldCompetition.getLeague());
            newCompetition.setFormat(oldCompetition.getFormat());
            competitionRepository.save(newCompetition);
            competitionTeamRepository.findTeamsByCompetition(oldCompetition.getId())
                    .forEach(team -> competitionTeamRepository.addTeamToCompetition(
                            newCompetition.getId(), team.getId()));
        }
        qualifyForEurope(oldCompetitions, next);
    }

    private void qualifyForEurope(List<Competition> oldCompetitions, Season next) {
        int[][] places = {{5, 5, 5, 5, 4}, {5, 5, 5, 4, 5}, {5, 5, 4, 5, 5}};
        List<String> countries = List.of("England", "Spain", "Italy", "Germany", "France");
        Map<String, List<Team>> rankings = new HashMap<>();
        oldCompetitions.stream().filter(competition -> !competition.isEuropean())
                .forEach(competition -> rankings.put(competition.getCountry(),
                        new footballcareer.database.LeagueStandingRepository()
                                .findByCompetition(competition.getId()).stream()
                                .map(row -> row.getTeam()).toList()));
        Map<String, Integer> offsets = new HashMap<>();
        countries.forEach(country -> offsets.put(country, 0));
        List<Competition> europe = oldCompetitions.stream().filter(Competition::isEuropean)
                .sorted(java.util.Comparator.comparingInt(Competition::getTier)).toList();
        for (int tournament = 0; tournament < europe.size(); tournament++) {
            Competition old = europe.get(tournament);
            Competition created = new Competition(0, old.getName(), old.getCountry(),
                    old.getTier(), next);
            created.setFormat("EUROPEAN");
            competitionRepository.save(created);
            for (int league = 0; league < countries.size(); league++) {
                String country = countries.get(league);
                List<Team> table = rankings.getOrDefault(country, List.of());
                int start = offsets.get(country);
                int end = Math.min(start + places[tournament][league], table.size());
                for (int position = start; position < end; position++) {
                    competitionTeamRepository.addTeamToCompetition(created.getId(),
                            table.get(position).getId());
                }
                offsets.put(country, end);
            }
        }
    }
}
