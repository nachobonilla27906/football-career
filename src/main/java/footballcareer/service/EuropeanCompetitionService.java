package footballcareer.service;

import footballcareer.database.CareerContext;
import footballcareer.database.CareerRepository;
import footballcareer.database.CompetitionRepository;
import footballcareer.database.EuropeanTieRepository;
import footballcareer.database.LeagueStandingRepository;
import footballcareer.database.MatchRepository;
import footballcareer.model.Competition;
import footballcareer.model.Match;
import footballcareer.model.Team;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Advances each career-specific UEFA bracket after its league phase. */
public class EuropeanCompetitionService {
    private final CompetitionRepository competitions = new CompetitionRepository();
    private final LeagueStandingRepository standings = new LeagueStandingRepository();
    private final MatchRepository matches = new MatchRepository();
    private final EuropeanTieRepository ties = new EuropeanTieRepository();

    public void progressCurrentCareer(LocalDate currentDate) {
        Long careerId = CareerContext.getCareerId();
        if (careerId == null) return;
        var career = new CareerRepository().findById(careerId);
        if (career != null) progress(career.getCurrentSeason().getId(), currentDate);
    }

    public void progress(long seasonId, LocalDate currentDate) {
        Long careerId = CareerContext.getCareerId();
        if (careerId == null || currentDate.getDayOfWeek() != java.time.DayOfWeek.WEDNESDAY
                || currentDate.getMonthValue() > 5) return;
        for (Competition competition : competitions.findBySeason(seasonId).stream()
                .filter(Competition::isEuropean).toList()) {
            progressCompetition(careerId, competition, currentDate);
        }
    }

    private void progressCompetition(long careerId, Competition competition, LocalDate date) {
        List<Match> fixtures = matches.findByCompetition(competition.getId());
        List<Match> leaguePhase = stage(fixtures, "LEAGUE_PHASE");
        if (leaguePhase.size() != 96 || leaguePhase.stream().anyMatch(match -> !match.isPlayed())) return;
        if (ties.find(careerId, competition.getId(), "ROUND_OF_16").isEmpty()) {
            List<Team> qualified = standings.findByCompetition(competition.getId()).stream()
                    .limit(16).map(row -> row.getTeam()).toList();
            createRound(careerId, competition, "ROUND_OF_16", qualified,
                    midweek(competition, 2, 18));
            return;
        }
        advanceRound(careerId, competition, fixtures, "ROUND_OF_16", "QUARTER_FINAL",
                midweek(competition, 3, 18));
        fixtures = matches.findByCompetition(competition.getId());
        advanceRound(careerId, competition, fixtures, "QUARTER_FINAL", "SEMI_FINAL",
                midweek(competition, 4, 22));
        fixtures = matches.findByCompetition(competition.getId());
        advanceRound(careerId, competition, fixtures, "SEMI_FINAL", "FINAL",
                midweek(competition, 5, 20));
        resolveWinners(careerId, competition, fixtures, "FINAL");
    }

    private void advanceRound(long careerId, Competition competition, List<Match> fixtures,
            String previous, String next, LocalDate date) {
        List<EuropeanTieRepository.Tie> previousTies = ties.find(careerId,
                competition.getId(), previous);
        if (previousTies.isEmpty() || stage(fixtures, previous).stream()
                .anyMatch(match -> !match.isPlayed())) return;
        List<Team> winners = resolveWinners(careerId, competition, fixtures, previous);
        if (!ties.find(careerId, competition.getId(), next).isEmpty()) return;
        createRound(careerId, competition, next, winners, date);
    }

    private List<Team> resolveWinners(long careerId, Competition competition,
            List<Match> fixtures, String stage) {
        List<Team> winners = new ArrayList<>();
        List<Match> roundMatches = stage(fixtures, stage);
        for (EuropeanTieRepository.Tie tie : ties.find(careerId, competition.getId(), stage)) {
            Match match = roundMatches.stream().filter(candidate ->
                    samePair(candidate, tie.home().getId(), tie.away().getId())).findFirst().orElse(null);
            if (match == null || !match.isPlayed()) return List.of();
            Team winner = match.getHomeGoals() > match.getAwayGoals() ? match.getHomeTeam()
                    : match.getAwayGoals() > match.getHomeGoals() ? match.getAwayTeam()
                    : penaltyWinner(match.getHomeTeam(), match.getAwayTeam());
            ties.setWinner(tie.id(), winner.getId());
            winners.add(winner);
        }
        return winners;
    }

    private void createRound(long careerId, Competition competition, String stage,
            List<Team> entrants, LocalDate date) {
        int tiesCount = entrants.size() / 2;
        for (int index = 0; index < tiesCount; index++) {
            Team first = entrants.get(index);
            Team second = entrants.get(entrants.size() - 1 - index);
            Team home = first;
            Team away = second;
            if (directedPairExists(competition.getId(), home.getId(), away.getId())) {
                home = second; away = first;
            }
            ties.save(careerId, competition.getId(), stage, index, home.getId(), away.getId());
            Match match = new Match(0, competition, home, away, date);
            match.setStage(stage);
            match.setCareerId(careerId);
            matches.save(match);
        }
    }

    private boolean directedPairExists(long competitionId, long homeId, long awayId) {
        return matches.findByCompetition(competitionId).stream().anyMatch(match ->
                match.getHomeTeam().getId() == homeId && match.getAwayTeam().getId() == awayId);
    }

    private boolean samePair(Match match, long first, long second) {
        return (match.getHomeTeam().getId() == first && match.getAwayTeam().getId() == second)
                || (match.getHomeTeam().getId() == second && match.getAwayTeam().getId() == first);
    }

    private List<Match> stage(List<Match> fixtures, String stage) {
        return fixtures.stream().filter(match -> stage.equals(match.getStage())).toList();
    }

    private Team penaltyWinner(Team home, Team away) {
        if (home.getReputation() != away.getReputation()) {
            return home.getReputation() > away.getReputation() ? home : away;
        }
        return home.getId() < away.getId() ? home : away;
    }

    private LocalDate midweek(Competition competition, int monthOffset, int day) {
        LocalDate candidate = LocalDate.of(competition.getSeason().getStartYear() + 1,
                monthOffset, Math.min(day, 28));
        int shift = Math.floorMod(java.time.DayOfWeek.WEDNESDAY.getValue()
                - candidate.getDayOfWeek().getValue(), 7);
        return candidate.plusDays(shift);
    }
}
