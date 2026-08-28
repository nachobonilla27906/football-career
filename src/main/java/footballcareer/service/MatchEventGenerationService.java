package footballcareer.service;

import footballcareer.database.MatchEventRepository;
import footballcareer.model.Match;
import footballcareer.model.MatchEvent;
import footballcareer.model.MatchLineup;
import footballcareer.model.Player;
import footballcareer.model.Team;
import footballcareer.model.enums.MatchEventType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class MatchEventGenerationService {
    private final LineupService lineupService;
    private final MatchEventRepository eventRepository;
    private final Random random;

    public MatchEventGenerationService(LineupService lineupService,
            MatchEventRepository eventRepository, Random random) {
        this.lineupService = lineupService;
        this.eventRepository = eventRepository;
        this.random = random;
    }

    public List<MatchEvent> generate(Match match) {
        if (!match.isPlayed()) {
            throw new IllegalArgumentException("Match must be played before generating events.");
        }
        if (!eventRepository.findByMatch(match.getId()).isEmpty()) {
            throw new IllegalStateException("Match events have already been generated.");
        }

        MatchLineup home = lineupService.selectMatchLineup(
                match.getId(), match.getHomeTeam().getId());
        MatchLineup away = lineupService.selectMatchLineup(
                match.getId(), match.getAwayTeam().getId());
        List<MatchEvent> events = new ArrayList<>();
        generateGoals(match, match.getHomeTeam(), home.getStarters(),
                match.getHomeGoals(), events);
        generateGoals(match, match.getAwayTeam(), away.getStarters(),
                match.getAwayGoals(), events);
        generateCards(match, match.getHomeTeam(), home.getStarters(), events);
        generateCards(match, match.getAwayTeam(), away.getStarters(), events);
        events.sort(Comparator.comparingInt(MatchEvent::getMinute));
        events.forEach(eventRepository::save);
        return events;
    }

    private void generateGoals(Match match, Team team, List<Player> starters,
            int goals, List<MatchEvent> events) {
        for (int i = 0; i < goals; i++) {
            Player scorer = weightedPlayer(starters, true);
            MatchEvent goal = createEvent(match, team, scorer,
                    1 + random.nextInt(90), MatchEventType.GOAL);
            if (random.nextDouble() < 0.72) {
                List<Player> possibleAssistants = starters.stream()
                        .filter(player -> player.getId() != scorer.getId()).toList();
                goal.setSecondaryPlayer(weightedPlayer(possibleAssistants, false));
            }
            events.add(goal);
        }
    }

    private void generateCards(Match match, Team team, List<Player> starters,
            List<MatchEvent> events) {
        for (Player player : starters) {
            double roll = random.nextDouble();
            if (roll < 0.018) {
                events.add(createEvent(match, team, player,
                        1 + random.nextInt(90), MatchEventType.RED_CARD));
            } else if (roll < 0.16) {
                events.add(createEvent(match, team, player,
                        1 + random.nextInt(90), MatchEventType.YELLOW_CARD));
            }
        }
    }

    private Player weightedPlayer(List<Player> players, boolean scorer) {
        int total = players.stream().mapToInt(player -> weight(player, scorer)).sum();
        int selected = random.nextInt(total);
        for (Player player : players) {
            selected -= weight(player, scorer);
            if (selected < 0) return player;
        }
        return players.getLast();
    }

    private int weight(Player player, boolean scorer) {
        if (!scorer) return Math.max(1, player.getPassing());
        int positionBonus = switch (player.getPosition()) {
            case ST -> 35;
            case LW, RW, CAM -> 20;
            case CM -> 8;
            case GK -> -25;
            default -> 0;
        };
        return Math.max(1, player.getShooting() + positionBonus);
    }

    private MatchEvent createEvent(Match match, Team team, Player player,
            int minute, MatchEventType type) {
        MatchEvent event = new MatchEvent();
        event.setMatch(match); event.setTeam(team); event.setPlayer(player);
        event.setMinute(minute); event.setType(type);
        return event;
    }
}
