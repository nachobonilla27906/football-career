package footballcareer.model;

import java.util.List;

public class MatchLineup {
    private final Team team;
    private final List<Player> starters;
    private final List<Player> substitutes;

    public MatchLineup(Team team, List<Player> starters, List<Player> substitutes) {
        this.team = team;
        this.starters = List.copyOf(starters);
        this.substitutes = List.copyOf(substitutes);
    }

    public Team getTeam() { return team; }
    public List<Player> getStarters() { return starters; }
    public List<Player> getSubstitutes() { return substitutes; }
}
