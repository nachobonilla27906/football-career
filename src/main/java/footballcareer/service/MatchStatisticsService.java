package footballcareer.service;

import footballcareer.database.MatchEventRepository;
import footballcareer.database.MatchTeamStatsRepository;
import footballcareer.model.Match;
import footballcareer.model.MatchEvent;
import footballcareer.model.MatchTeamStats;
import footballcareer.model.Team;
import footballcareer.model.enums.MatchEventType;

import java.util.List;
import java.util.Random;

public class MatchStatisticsService {
    private final MatchEventRepository eventRepository;
    private final MatchTeamStatsRepository statsRepository;
    private final Random random;

    public MatchStatisticsService(MatchEventRepository eventRepository,
            MatchTeamStatsRepository statsRepository, Random random) {
        this.eventRepository = eventRepository;
        this.statsRepository = statsRepository;
        this.random = random;
    }

    public void generate(Match match) {
        if (!match.isPlayed()) throw new IllegalArgumentException("Match has not been played.");
        if (!statsRepository.findByMatch(match.getId()).isEmpty()) {
            throw new IllegalStateException("Match statistics already exist.");
        }
        List<MatchEvent> events = eventRepository.findByMatch(match.getId());
        int reputationDifference = match.getHomeTeam().getReputation()
                - match.getAwayTeam().getReputation();
        int homePossession = clamp(50 + reputationDifference / 3
                + random.nextInt(9) - 4, 35, 65);
        statsRepository.save(create(match, match.getHomeTeam(), homePossession,
                match.getHomeGoals(), events));
        statsRepository.save(create(match, match.getAwayTeam(), 100 - homePossession,
                match.getAwayGoals(), events));
    }

    private MatchTeamStats create(Match match, Team team, int possession,
            int goals, List<MatchEvent> events) {
        int shotsOnTarget = Math.max(goals, goals + 2 + random.nextInt(5));
        int shots = shotsOnTarget + 3 + random.nextInt(8);
        MatchTeamStats stats = new MatchTeamStats();
        stats.setMatch(match); stats.setTeam(team); stats.setPossession(possession);
        stats.setShots(shots); stats.setShotsOnTarget(shotsOnTarget);
        stats.setCorners(Math.max(0, shots / 3 + random.nextInt(4) - 1));
        stats.setFouls(7 + random.nextInt(10));
        stats.setYellowCards(count(events, team.getId(), MatchEventType.YELLOW_CARD));
        stats.setRedCards(count(events, team.getId(), MatchEventType.RED_CARD));
        return stats;
    }

    private int count(List<MatchEvent> events, long teamId, MatchEventType type) {
        return (int) events.stream().filter(event -> event.getTeam().getId() == teamId)
                .filter(event -> event.getType() == type).count();
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
