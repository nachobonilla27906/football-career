package footballcareer.model;

import java.time.LocalDate;

public class Transfer {
    private long id;
    private Player player;
    private Team fromTeam;
    private Team toTeam;
    private double amount;
    private LocalDate transferDate;
    private Season season;
    private long offerId;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
    public Team getFromTeam() { return fromTeam; }
    public void setFromTeam(Team fromTeam) { this.fromTeam = fromTeam; }
    public Team getToTeam() { return toTeam; }
    public void setToTeam(Team toTeam) { this.toTeam = toTeam; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public LocalDate getTransferDate() { return transferDate; }
    public void setTransferDate(LocalDate transferDate) { this.transferDate = transferDate; }
    public Season getSeason() { return season; }
    public void setSeason(Season season) { this.season = season; }
    public long getOfferId() { return offerId; }
    public void setOfferId(long offerId) { this.offerId = offerId; }
}
