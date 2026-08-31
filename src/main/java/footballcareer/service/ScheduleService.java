package footballcareer.service;

import footballcareer.database.CompetitionTeamRepository;
import footballcareer.database.MatchRepository;
import footballcareer.model.Competition;
import footballcareer.model.Match;
import footballcareer.model.Team;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ScheduleService {
    private static final int EUROPEAN_LEAGUE_PHASE_ROUNDS = 8;

    private final CompetitionTeamRepository competitionTeamRepository;
    private final MatchRepository matchRepository;

    public ScheduleService(
            CompetitionTeamRepository competitionTeamRepository,
            MatchRepository matchRepository
    ) {
        this.competitionTeamRepository = competitionTeamRepository;
        this.matchRepository = matchRepository;
    }

    public List<Match> generateLeagueSchedule(
            Competition competition
    ) {

        List<Match> existingMatches =
                matchRepository.findByCompetition(competition.getId());

        List<Team> teams = new ArrayList<>(
                competitionTeamRepository.findTeamsByCompetition(
                        competition.getId()
                )
        );

        if (teams.size() < 2) {
            throw new IllegalArgumentException(
                    "A competition needs at least two teams."
            );
        }

        int expectedMatches = teams.size() * (teams.size() - 1);
        if (existingMatches.size() >= expectedMatches) return existingMatches;
        java.util.Set<String> existingPairs = existingMatches.stream()
                .map(match -> match.getHomeTeam().getId() + ":" + match.getAwayTeam().getId())
                .collect(java.util.stream.Collectors.toSet());

        if (teams.size() % 2 != 0) {
            teams.add(null);
        }

        int roundsPerLeg = teams.size() - 1;
        int matchesPerRound = teams.size() / 2;
        List<Match> firstLeg = new ArrayList<>();

        for (int round = 0; round < roundsPerLeg; round++) {
            LocalDate date = competition.getSeason()
                    .getStartDate()
                    .plusWeeks(round + 1L);

            for (int pair = 0; pair < matchesPerRound; pair++) {
                Team first = teams.get(pair);
                Team second = teams.get(teams.size() - 1 - pair);

                if (first == null || second == null) {
                    continue;
                }

                Team homeTeam = round % 2 == 0 ? first : second;
                Team awayTeam = round % 2 == 0 ? second : first;

                firstLeg.add(new Match(
                        0,
                        competition,
                        homeTeam,
                        awayTeam,
                        date
                ));
            }

            rotateTeams(teams);
        }

        List<Match> schedule = new ArrayList<>(firstLeg);

        for (Match firstLegMatch : firstLeg) {
            schedule.add(new Match(
                    0,
                    competition,
                    firstLegMatch.getAwayTeam(),
                    firstLegMatch.getHomeTeam(),
                    firstLegMatch.getDate().plusWeeks(roundsPerLeg)
            ));
        }

        for (Match match : schedule) {
            String pair = match.getHomeTeam().getId() + ":" + match.getAwayTeam().getId();
            if (existingPairs.contains(pair)) continue;
            if (match.getDate().isAfter(
                    competition.getSeason().getEndDate()
            )) {
                throw new IllegalStateException(
                        "Generated schedule exceeds the season end date."
                );
            }

            matchRepository.save(match);
            existingMatches.add(match);
        }

        return existingMatches;
    }

    public List<Match> generateEuropeanLeaguePhase(Competition competition) {
        List<Match> existing = matchRepository.findByCompetition(competition.getId());
        if (!existing.isEmpty()) return existing;
        List<Team> teams = new ArrayList<>(competitionTeamRepository
                .findTeamsByCompetition(competition.getId()));
        if (teams.size() != 24) {
            throw new IllegalStateException(competition.getName() + " requires 24 clubs.");
        }

        // Circle pairings provide eight different rivals and four home games per club.
        LocalDate firstDate = nextWednesday(competition.getSeason().getStartDate().plusMonths(1));
        List<Match> schedule = new ArrayList<>();
        for (int round = 0; round < EUROPEAN_LEAGUE_PHASE_ROUNDS; round++) {
            LocalDate date = firstDate.plusWeeks(round * 3L);
            for (int pair = 0; pair < teams.size() / 2; pair++) {
                Team first = teams.get(pair);
                Team second = teams.get(teams.size() - 1 - pair);
                boolean reverse = (round + pair) % 2 != 0;
                Match match = new Match(0, competition,
                        reverse ? second : first, reverse ? first : second, date);
                match.setStage("LEAGUE_PHASE");
                matchRepository.save(match);
                schedule.add(match);
            }
            rotateTeams(teams);
        }
        return schedule;
    }

    private LocalDate nextWednesday(LocalDate date) {
        int days = Math.floorMod(java.time.DayOfWeek.WEDNESDAY.getValue()
                - date.getDayOfWeek().getValue(), 7);
        return date.plusDays(days);
    }

    private void rotateTeams(List<Team> teams) {
        Team last = teams.removeLast();
        teams.add(1, last);
    }
}
