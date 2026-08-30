package footballcareer.model;

import java.time.LocalDate;

public class PlayerState {
    private Player player;
    private int form;
    private int morale;
    private int fitness;
    private LocalDate unavailableUntil;
    private String unavailableReason;

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
    public int getForm() { return form; }
    public void setForm(int form) { this.form = form; }
    public int getMorale() { return morale; }
    public void setMorale(int morale) { this.morale = morale; }
    public int getFitness() { return fitness; }
    public void setFitness(int fitness) { this.fitness = fitness; }
    public LocalDate getUnavailableUntil() { return unavailableUntil; }
    public void setUnavailableUntil(LocalDate unavailableUntil) { this.unavailableUntil = unavailableUntil; }
    public String getUnavailableReason() { return unavailableReason; }
    public void setUnavailableReason(String unavailableReason) { this.unavailableReason = unavailableReason; }
    public boolean isAvailableOn(LocalDate date) {
        return unavailableUntil == null || date.isAfter(unavailableUntil);
    }
}
