package footballcareer.model;

import java.time.LocalDate;

public class Contract {
    private long id;
    private Player player;
    private Team team;
    private LocalDate startDate;
    private LocalDate endDate;
    private double salary;
    private double signingBonus;
    private Double releaseClause;
    private String squadRole = "ROTATION";
    private boolean active;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }
    public double getSigningBonus() { return signingBonus; }
    public void setSigningBonus(double signingBonus) { this.signingBonus = signingBonus; }
    public Double getReleaseClause() { return releaseClause; }
    public void setReleaseClause(Double releaseClause) { this.releaseClause = releaseClause; }
    public String getSquadRole() { return squadRole; }
    public void setSquadRole(String squadRole) { this.squadRole = squadRole; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
