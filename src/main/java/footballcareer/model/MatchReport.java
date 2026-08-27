package footballcareer.model;

import java.util.List;

public class MatchReport {
    private final Match match;
    private final List<MatchEvent> events;
    private final MatchTeamStats homeStats;
    private final MatchTeamStats awayStats;
    private final Player playerOfTheMatch;

    public MatchReport(Match match, List<MatchEvent> events,
            MatchTeamStats homeStats, MatchTeamStats awayStats,
            Player playerOfTheMatch) {
        this.match = match;
        this.events = List.copyOf(events);
        this.homeStats = homeStats;
        this.awayStats = awayStats;
        this.playerOfTheMatch = playerOfTheMatch;
    }

    public Match getMatch() { return match; }
    public List<MatchEvent> getEvents() { return events; }
    public MatchTeamStats getHomeStats() { return homeStats; }
    public MatchTeamStats getAwayStats() { return awayStats; }
    public Player getPlayerOfTheMatch() { return playerOfTheMatch; }
}
