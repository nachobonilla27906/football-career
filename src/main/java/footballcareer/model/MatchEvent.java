package footballcareer.model;

import footballcareer.model.enums.MatchEventType;

public class MatchEvent {
    private long id;
    private Match match;
    private Team team;
    private Player player;
    private Player secondaryPlayer;
    private int minute;
    private MatchEventType type;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public Match getMatch() { return match; }
    public void setMatch(Match match) { this.match = match; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
    public Player getSecondaryPlayer() { return secondaryPlayer; }
    public void setSecondaryPlayer(Player secondaryPlayer) { this.secondaryPlayer = secondaryPlayer; }
    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }
    public MatchEventType getType() { return type; }
    public void setType(MatchEventType type) { this.type = type; }
}
