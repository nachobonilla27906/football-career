package footballcareer.service;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.model.enums.MatchEventType;

import java.util.*;

public class IncrementalLiveMatchService {
    private final Random random;

    public IncrementalLiveMatchService() { this(new Random()); }
    public IncrementalLiveMatchService(Random random) { this.random = random; }

    public Session start(long matchId) {
        Match match = new MatchRepository().findById(matchId);
        if (match == null || match.isPlayed()) throw new IllegalStateException("Match is not pending.");
        PlayerStateRepository states = new PlayerStateRepository();
        LineupService lineups = new LineupService(new PlayerRepository(), states);
        MatchLineup home = lineups.selectMatchLineup(matchId, match.getHomeTeam().getId());
        MatchLineup away = lineups.selectMatchLineup(matchId, match.getAwayTeam().getId());
        MatchTacticsRepository tactics = new MatchTacticsRepository();
        return new Session(match, home, away, tactics.find(matchId, match.getHomeTeam().getId()),
                tactics.find(matchId, match.getAwayTeam().getId()), states.findAll(), random);
    }

    public static final class Session {
        private final Match match;
        private final MatchLineup home;
        private final MatchLineup away;
        private MatchTacticsRepository.TacticalSetup homeTactics;
        private MatchTacticsRepository.TacticalSetup awayTactics;
        private final Map<Long, PlayerState> states;
        private final Random random;
        private final List<MatchEvent> events = new ArrayList<>();
        private final MatchTeamStats homeStats;
        private final MatchTeamStats awayStats;
        private int minute;
        private long temporaryEventId = -1;
        private boolean finished;

        private Session(Match match, MatchLineup home, MatchLineup away,
                MatchTacticsRepository.TacticalSetup homeTactics,
                MatchTacticsRepository.TacticalSetup awayTactics,
                Map<Long, PlayerState> states, Random random) {
            this.match = match; this.home = home; this.away = away;
            this.homeTactics = homeTactics; this.awayTactics = awayTactics;
            this.states = states; this.random = random;
            homeStats = stats(match, match.getHomeTeam()); awayStats = stats(match, match.getAwayTeam());
        }

        public Match match() { return match; }
        public MatchLineup lineup(long teamId) {
            return teamId == match.getHomeTeam().getId() ? home
                    : teamId == match.getAwayTeam().getId() ? away : null;
        }
        public MatchTacticsRepository.TacticalSetup tactics(long teamId) {
            return teamId == match.getHomeTeam().getId() ? homeTactics : awayTactics;
        }
        public MatchTeamStats homeStats() { return homeStats; }
        public MatchTeamStats awayStats() { return awayStats; }
        public List<MatchEvent> events() { return List.copyOf(events); }
        public int minute() { return minute; }
        public boolean finished() { return finished; }

        public List<MatchEvent> advanceTo(int targetMinute) {
            if (finished) return List.of();
            int target = Math.max(minute, Math.min(90, targetMinute));
            List<MatchEvent> generated = new ArrayList<>();
            while (minute < target) {
                minute++;
                playMinute(true, generated); playMinute(false, generated);
            }
            updatePossessionAndPassing();
            return List.copyOf(generated);
        }

        public void updateTactics(long teamId, MatchTacticsRepository.TacticalSetup setup) {
            if (teamId == match.getHomeTeam().getId()) homeTactics = setup;
            else if (teamId == match.getAwayTeam().getId()) awayTactics = setup;
            else throw new IllegalArgumentException("Team does not play this match.");
            new MatchTacticsRepository().save(match.getId(), teamId, setup);
        }

        public void addManualEvent(MatchEvent event) {
            if (finished) throw new IllegalStateException("Match has finished.");
            event.setId(temporaryEventId--); events.add(event);
        }

        public void recordSubstitution(long teamId, List<Player> starters,
                List<Player> substitutes, MatchEvent event) {
            new MatchLineupRepository().save(match.getId(), teamId, starters, substitutes);
            addManualEvent(event);
        }

        public void finish() {
            if (finished) return;
            advanceTo(90);
            match.setResult(goals(true), goals(false));
            finalizeStats(homeStats); finalizeStats(awayStats);
            MatchRepository matches = new MatchRepository();
            new LeagueStandingRepository().applyResult(match); matches.updateResult(match);
            MatchEventRepository eventRepository = new MatchEventRepository();
            events.stream().sorted(Comparator.comparingInt(MatchEvent::getMinute))
                    .forEach(eventRepository::save);
            MatchTeamStatsRepository stats = new MatchTeamStatsRepository();
            stats.save(homeStats); stats.save(awayStats);
            PlayerStateRepository stateRepository = new PlayerStateRepository();
            new PlayerSeasonStatsRepository().initializeForSeason(match.getCompetition().getSeason().getId());
            new PlayerMatchService(new LineupService(new PlayerRepository(), stateRepository),
                    new PlayerSeasonStatsRepository(), stateRepository, eventRepository).process(match);
            new PlayerAvailabilityService().processMatch(match);
            new SquadDynamicsService().processUnusedPlayers(match);
            finished = true;
        }

        private void playMinute(boolean homeAttack, List<MatchEvent> generated) {
            MatchLineup attacking = homeAttack ? home : away;
            MatchTeamStats stats = homeAttack ? homeStats : awayStats;
            double attack = strength(attacking) + tacticalAttack(homeAttack ? homeTactics : awayTactics)
                    + (homeAttack ? 2.5 : 0);
            double defence = strength(homeAttack ? away : home)
                    + tacticalDefence(homeAttack ? awayTactics : homeTactics);
            double shotChance = clamp(0.105 + (attack - defence) * 0.003, 0.045, 0.22);
            if (random.nextDouble() < shotChance) {
                stats.setShots(stats.getShots() + 1);
                boolean onTarget = random.nextDouble() < clamp(0.34 + (attack - defence) * 0.006, 0.22, 0.58);
                if (onTarget) stats.setShotsOnTarget(stats.getShotsOnTarget() + 1);
                double xg = onTarget ? 0.22 : 0.055;
                stats.setExpectedGoals(Math.round((stats.getExpectedGoals() + xg) * 100) / 100.0);
                if (onTarget && random.nextDouble() < clamp(0.27 + (attack - defence) * 0.008, 0.12, 0.55))
                    generated.add(addEvent(homeAttack, MatchEventType.GOAL, attacking));
                else if (random.nextDouble() < 0.28) stats.setCorners(stats.getCorners() + 1);
            }
            if (random.nextDouble() < 0.012) generated.add(addEvent(homeAttack,
                    random.nextDouble() < 0.045 ? MatchEventType.RED_CARD : MatchEventType.YELLOW_CARD,
                    attacking));
            if (random.nextDouble() < 0.12) stats.setFouls(stats.getFouls() + 1);
        }

        private MatchEvent addEvent(boolean homeTeam, MatchEventType type, MatchLineup lineup) {
            MatchEvent event = new MatchEvent(); event.setId(temporaryEventId--);
            event.setMatch(match); event.setTeam(homeTeam ? match.getHomeTeam() : match.getAwayTeam());
            event.setMinute(minute); event.setType(type);
            Player primary = type == MatchEventType.GOAL ? attacker(lineup) : randomPlayer(lineup);
            event.setPlayer(primary);
            if (type == MatchEventType.GOAL && random.nextDouble() < 0.72)
                event.setSecondaryPlayer(different(lineup, primary));
            events.add(event);
            if (type == MatchEventType.YELLOW_CARD) stat(homeTeam).setYellowCards(
                    stat(homeTeam).getYellowCards() + 1);
            if (type == MatchEventType.RED_CARD) stat(homeTeam).setRedCards(
                    stat(homeTeam).getRedCards() + 1);
            return event;
        }

        private int goals(boolean homeTeam) {
            long teamId = (homeTeam ? match.getHomeTeam() : match.getAwayTeam()).getId();
            return (int) events.stream().filter(event -> event.getType() == MatchEventType.GOAL)
                    .filter(event -> event.getTeam().getId() == teamId).count();
        }

        private double strength(MatchLineup lineup) {
            return lineup.getStarters().stream().mapToDouble(player -> {
                PlayerState state = states.get(player.getId());
                return player.getOverall() * 0.72 + state.getForm() * 0.12
                        + state.getMorale() * 0.07 + state.getFitness() * 0.09;
            }).average().orElse(70);
        }

        private double tacticalAttack(MatchTacticsRepository.TacticalSetup setup) {
            return ("ATTACKING".equals(setup.mentality()) ? 3 : "DEFENSIVE".equals(setup.mentality()) ? -2 : 0)
                    + ("HIGH".equals(setup.pressing()) ? 1 : 0) + ("FAST".equals(setup.tempo()) ? 1 : 0);
        }
        private double tacticalDefence(MatchTacticsRepository.TacticalSetup setup) {
            return ("DEFENSIVE".equals(setup.mentality()) ? 3 : "ATTACKING".equals(setup.mentality()) ? -1 : 0)
                    + ("LOW".equals(setup.pressing()) ? 1 : 0);
        }

        private void updatePossessionAndPassing() {
            double homePower = strength(home) + tacticalAttack(homeTactics);
            double awayPower = strength(away) + tacticalAttack(awayTactics);
            int homePossession = (int) Math.round(clamp(50 + (homePower - awayPower) * 0.7, 35, 65));
            homeStats.setPossession(homePossession); awayStats.setPossession(100 - homePossession);
            homeStats.setPasses(minute * homePossession / 9); awayStats.setPasses(minute * (100 - homePossession) / 9);
            homeStats.setPassAccuracy((int) clamp(70 + homePossession / 4.0, 70, 92));
            awayStats.setPassAccuracy((int) clamp(70 + (100 - homePossession) / 4.0, 70, 92));
            homeStats.setTackles(minute * (100 - homePossession) / 500);
            awayStats.setTackles(minute * homePossession / 500);
        }

        private Player attacker(MatchLineup lineup) {
            List<Player> candidates = lineup.getStarters().stream().sorted(Comparator
                    .comparingInt((Player player) -> player.getShooting() + player.getOverall()).reversed())
                    .limit(5).toList();
            return candidates.get(random.nextInt(candidates.size()));
        }
        private Player randomPlayer(MatchLineup lineup) {
            return lineup.getStarters().get(random.nextInt(lineup.getStarters().size()));
        }
        private Player different(MatchLineup lineup, Player primary) {
            Player player; do player = randomPlayer(lineup); while (player.getId() == primary.getId());
            return player;
        }
        private MatchTeamStats stat(boolean homeTeam) { return homeTeam ? homeStats : awayStats; }
        private static MatchTeamStats stats(Match match, Team team) {
            MatchTeamStats stats = new MatchTeamStats(); stats.setMatch(match); stats.setTeam(team);
            stats.setPossession(50); stats.setPassAccuracy(75); return stats;
        }
        private void finalizeStats(MatchTeamStats stats) {
            stats.setShotsOnTarget(Math.max(stats.getShotsOnTarget(), goals(
                    stats.getTeam().getId() == match.getHomeTeam().getId())));
        }
        private double clamp(double value, double low, double high) {
            return Math.max(low, Math.min(high, value));
        }
    }
}
