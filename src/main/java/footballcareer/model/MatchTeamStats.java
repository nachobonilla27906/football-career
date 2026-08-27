package footballcareer.model;

public class MatchTeamStats {
    private Match match;
    private Team team;
    private int possession;
    private int shots;
    private int shotsOnTarget;
    private int corners;
    private int fouls;
    private int yellowCards;
    private int redCards;

    public Match getMatch() { return match; }
    public void setMatch(Match match) { this.match = match; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public int getPossession() { return possession; }
    public void setPossession(int possession) { this.possession = possession; }
    public int getShots() { return shots; }
    public void setShots(int shots) { this.shots = shots; }
    public int getShotsOnTarget() { return shotsOnTarget; }
    public void setShotsOnTarget(int shotsOnTarget) { this.shotsOnTarget = shotsOnTarget; }
    public int getCorners() { return corners; }
    public void setCorners(int corners) { this.corners = corners; }
    public int getFouls() { return fouls; }
    public void setFouls(int fouls) { this.fouls = fouls; }
    public int getYellowCards() { return yellowCards; }
    public void setYellowCards(int yellowCards) { this.yellowCards = yellowCards; }
    public int getRedCards() { return redCards; }
    public void setRedCards(int redCards) { this.redCards = redCards; }
}
