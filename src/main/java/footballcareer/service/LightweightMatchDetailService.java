package footballcareer.service;

import footballcareer.database.MatchEventRepository;
import footballcareer.database.PlayerRepository;
import footballcareer.model.Match;
import footballcareer.model.MatchEvent;
import footballcareer.model.Player;
import footballcareer.model.Team;
import footballcareer.model.enums.MatchEventType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Produces report-quality events for AI matches without building or persisting lineups. */
public class LightweightMatchDetailService {
    private final PlayerRepository players;
    private final MatchEventRepository events;
    private final Random random;

    public LightweightMatchDetailService() {
        this(new PlayerRepository(), new MatchEventRepository(), new Random());
    }

    LightweightMatchDetailService(PlayerRepository players, MatchEventRepository events,
            Random random) {
        this.players = players;
        this.events = events;
        this.random = random;
    }

    public void generate(Match match) {
        if (!match.isPlayed()) throw new IllegalArgumentException("Match has not been played.");
        if (!events.findByMatch(match.getId()).isEmpty()) return;
        List<Player> home = players.findCurrentPlayersByTeam(match.getHomeTeam().getId());
        List<Player> away = players.findCurrentPlayersByTeam(match.getAwayTeam().getId());
        addGoals(match, match.getHomeTeam(), home, match.getHomeGoals());
        addGoals(match, match.getAwayTeam(), away, match.getAwayGoals());
        addDiscipline(match, match.getHomeTeam(), home);
        addDiscipline(match, match.getAwayTeam(), away);
    }

    private void addGoals(Match match, Team team, List<Player> squad, int goals) {
        List<Player> attackers = squad.stream().sorted(Comparator
                .comparingInt((Player player) -> player.getShooting() * 2
                        + player.getOverall()).reversed()).limit(8).toList();
        if (attackers.isEmpty()) return;
        List<Integer> minutes = new ArrayList<>();
        for (int goal = 0; goal < goals; goal++) minutes.add(4 + random.nextInt(86));
        minutes.sort(Integer::compareTo);
        for (int minute : minutes) {
            Player scorer = attackers.get(random.nextInt(Math.min(4, attackers.size())));
            Player assistant = attackers.size() < 2 ? null
                    : differentPlayer(attackers, scorer);
            save(match, team, scorer, assistant, minute, MatchEventType.GOAL);
        }
    }

    private void addDiscipline(Match match, Team team, List<Player> squad) {
        if (squad.isEmpty()) return;
        int yellows = random.nextInt(4);
        for (int card = 0; card < yellows; card++) save(match, team,
                squad.get(random.nextInt(squad.size())), null, 15 + random.nextInt(76),
                MatchEventType.YELLOW_CARD);
        if (random.nextDouble() < 0.055) save(match, team,
                squad.get(random.nextInt(squad.size())), null, 25 + random.nextInt(66),
                MatchEventType.RED_CARD);
    }

    private Player differentPlayer(List<Player> players, Player excluded) {
        Player selected;
        do selected = players.get(random.nextInt(players.size()));
        while (selected.getId() == excluded.getId());
        return selected;
    }

    private void save(Match match, Team team, Player player, Player secondary,
            int minute, MatchEventType type) {
        MatchEvent event = new MatchEvent();
        event.setMatch(match); event.setTeam(team); event.setPlayer(player);
        event.setSecondaryPlayer(secondary); event.setMinute(minute); event.setType(type);
        events.save(event);
    }
}
