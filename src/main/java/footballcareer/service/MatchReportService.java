package footballcareer.service;

import footballcareer.database.MatchEventRepository;
import footballcareer.database.MatchRepository;
import footballcareer.database.MatchTeamStatsRepository;
import footballcareer.database.PlayerRepository;
import footballcareer.model.Match;
import footballcareer.model.MatchEvent;
import footballcareer.model.MatchReport;
import footballcareer.model.MatchTeamStats;
import footballcareer.model.Player;
import footballcareer.model.enums.MatchEventType;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class MatchReportService {
    private final MatchRepository matchRepository;
    private final MatchEventRepository eventRepository;
    private final MatchTeamStatsRepository statsRepository;
    private final PlayerRepository playerRepository;

    public MatchReportService() {
        this(new MatchRepository(), new MatchEventRepository(),
                new MatchTeamStatsRepository(), new PlayerRepository());
    }

    public MatchReportService(MatchRepository matchRepository,
            MatchEventRepository eventRepository,
            MatchTeamStatsRepository statsRepository,
            PlayerRepository playerRepository) {
        this.matchRepository = matchRepository;
        this.eventRepository = eventRepository;
        this.statsRepository = statsRepository;
        this.playerRepository = playerRepository;
    }

    public MatchReport build(long matchId) {
        Match match = matchRepository.findById(matchId);
        if (match == null || !match.isPlayed()) {
            throw new IllegalArgumentException("Played match does not exist.");
        }
        MatchTeamStats home = statsRepository.find(matchId, match.getHomeTeam().getId());
        MatchTeamStats away = statsRepository.find(matchId, match.getAwayTeam().getId());
        if (home == null || away == null) {
            throw new IllegalStateException("Match statistics are incomplete.");
        }
        List<MatchEvent> events = eventRepository.findByMatch(matchId);
        Map<Long, Player> players = hydrateEventPlayers(events);
        return new MatchReport(match, events, home, away,
                selectPlayerOfTheMatch(match, events, players));
    }

    private Map<Long, Player> hydrateEventPlayers(List<MatchEvent> events) {
        if (events.isEmpty()) return Map.of();
        Map<Long, Player> players = playerRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Player::getId, player -> player));
        for (MatchEvent event : events) {
            event.setPlayer(required(players, event.getPlayer().getId()));
            if (event.getSecondaryPlayer() != null) event.setSecondaryPlayer(
                    required(players, event.getSecondaryPlayer().getId()));
        }
        return players;
    }

    private Player required(Map<Long, Player> players, long playerId) {
        Player player = players.get(playerId);
        if (player == null) throw new IllegalStateException("Match event player no longer exists.");
        return player;
    }

    private Player selectPlayerOfTheMatch(Match match, List<MatchEvent> events,
            Map<Long, Player> players) {
        Map<Long, Double> scores = new HashMap<>();
        for (MatchEvent event : events) {
            double primaryScore = switch (event.getType()) {
                case GOAL -> 5;
                case YELLOW_CARD -> -0.5;
                case RED_CARD -> -3;
                case SUBSTITUTION -> 0;
            };
            scores.merge(event.getPlayer().getId(), primaryScore, Double::sum);
            if (event.getType() == MatchEventType.GOAL
                    && event.getSecondaryPlayer() != null) {
                scores.merge(event.getSecondaryPlayer().getId(), 3.0, Double::sum);
            }
        }
        if (!scores.isEmpty()) {
            long playerId = scores.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).orElseThrow().getKey();
            return required(players, playerId);
        }
        return Stream.concat(
                playerRepository.findCurrentPlayersByTeam(match.getHomeTeam().getId()).stream(),
                playerRepository.findCurrentPlayersByTeam(match.getAwayTeam().getId()).stream())
                .max(Comparator.comparingInt(Player::getOverall)).orElseThrow();
    }
}
