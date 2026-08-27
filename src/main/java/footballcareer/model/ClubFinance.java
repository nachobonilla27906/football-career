package footballcareer.model;

public class ClubFinance {
    private Team team;
    private double transferBudget;
    private double wageBudget;
    private double currentWageSpend;
    private double balance;

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public double getTransferBudget() { return transferBudget; }
    public void setTransferBudget(double transferBudget) { this.transferBudget = transferBudget; }
    public double getWageBudget() { return wageBudget; }
    public void setWageBudget(double wageBudget) { this.wageBudget = wageBudget; }
    public double getCurrentWageSpend() { return currentWageSpend; }
    public void setCurrentWageSpend(double currentWageSpend) { this.currentWageSpend = currentWageSpend; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public double getAvailableWageBudget() { return wageBudget - currentWageSpend; }
}
