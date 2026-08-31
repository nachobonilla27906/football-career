package footballcareer.service;

import footballcareer.database.MatchEventRepository;
import footballcareer.database.MatchTeamStatsRepository;
import footballcareer.model.Match;

import java.util.Random;

/** Materializes optional report detail only when an AI match is opened. */
public class BackgroundMatchReportService {
    private final MatchEventRepository events = new MatchEventRepository();
    private final MatchTeamStatsRepository statistics = new MatchTeamStatsRepository();

    public void prepare(Match match) {
        if (match == null || !match.isPlayed()) return;
        if (events.findByMatch(match.getId()).isEmpty()) {
            new LightweightMatchDetailService().generate(match);
        }
        if (statistics.findByMatch(match.getId()).isEmpty()) {
            new MatchStatisticsService(events, statistics, new Random()).generate(match);
        }
    }
}
