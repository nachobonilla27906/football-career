package footballcareer.model;

public class LeagueStanding {
    private long id;
    private Competition competition;
    private Team team;
    private int played;
    private int wins;
    private int draws;
    private int losses;
    private int goalsFor;
    private int goalsAgainst;
    private int points;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public Competition getCompetition() { return competition; }
    public void setCompetition(Competition competition) { this.competition = competition; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public int getPlayed() { return played; }
    public void setPlayed(int played) { this.played = played; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public int getDraws() { return draws; }
    public void setDraws(int draws) { this.draws = draws; }
    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }
    public int getGoalsFor() { return goalsFor; }
    public void setGoalsFor(int goalsFor) { this.goalsFor = goalsFor; }
    public int getGoalsAgainst() { return goalsAgainst; }
    public void setGoalsAgainst(int goalsAgainst) { this.goalsAgainst = goalsAgainst; }
    public int getGoalDifference() { return goalsFor - goalsAgainst; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
}
