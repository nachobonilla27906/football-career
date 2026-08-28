package footballcareer.model;

import footballcareer.model.enums.TransferOfferStatus;
import java.time.LocalDate;

public class TransferOffer {
    private long id;
    private Player player;
    private Team buyingTeam;
    private Team sellingTeam;
    private double amount;
    private LocalDate offerDate;
    private TransferOfferStatus status;
    private Double counterAmount;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
    public Team getBuyingTeam() { return buyingTeam; }
    public void setBuyingTeam(Team buyingTeam) { this.buyingTeam = buyingTeam; }
    public Team getSellingTeam() { return sellingTeam; }
    public void setSellingTeam(Team sellingTeam) { this.sellingTeam = sellingTeam; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public LocalDate getOfferDate() { return offerDate; }
    public void setOfferDate(LocalDate offerDate) { this.offerDate = offerDate; }
    public TransferOfferStatus getStatus() { return status; }
    public void setStatus(TransferOfferStatus status) { this.status = status; }
    public Double getCounterAmount() { return counterAmount; }
    public void setCounterAmount(Double counterAmount) { this.counterAmount = counterAmount; }
}
