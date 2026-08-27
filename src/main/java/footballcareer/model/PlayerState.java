package footballcareer.model;

public class PlayerState {
    private Player player;
    private int form;
    private int morale;
    private int fitness;

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
    public int getForm() { return form; }
    public void setForm(int form) { this.form = form; }
    public int getMorale() { return morale; }
    public void setMorale(int morale) { this.morale = morale; }
    public int getFitness() { return fitness; }
    public void setFitness(int fitness) { this.fitness = fitness; }
}
