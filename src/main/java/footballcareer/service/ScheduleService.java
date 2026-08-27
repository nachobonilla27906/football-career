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

        if (!existingMatches.isEmpty()) {
            return existingMatches;
        }

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
            if (match.getDate().isAfter(
                    competition.getSeason().getEndDate()
            )) {
                throw new IllegalStateException(
                        "Generated schedule exceeds the season end date."
                );
            }

            matchRepository.save(match);
        }

        return schedule;
    }

    private void rotateTeams(List<Team> teams) {
        Team last = teams.removeLast();
        teams.add(1, last);
    }
}
