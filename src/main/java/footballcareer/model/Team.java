package footballcareer.model;

import java.util.ArrayList;
import java.util.List;

public class Team {

    private long id;

    private String name;
    private String shortName;
    private String country;

    private String stadiumName;
    private int stadiumCapacity;

    private int reputation;

    private List<Player> squad;

    public Team() {
        this.squad = new ArrayList<>();
    }

    public Team(
            long id,
            String name,
            String shortName,
            String country,
            String stadiumName,
            int stadiumCapacity,
            int reputation
    ) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.country = country;
        this.stadiumName = stadiumName;
        this.stadiumCapacity = stadiumCapacity;
        this.reputation = reputation;
        this.squad = new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getStadiumName() {
        return stadiumName;
    }

    public void setStadiumName(String stadiumName) {
        this.stadiumName = stadiumName;
    }

    public int getStadiumCapacity() {
        return stadiumCapacity;
    }

    public void setStadiumCapacity(int stadiumCapacity) {
        this.stadiumCapacity = stadiumCapacity;
    }

    public int getReputation() {
        return reputation;
    }

    public void setReputation(int reputation) {
        this.reputation = reputation;
    }

    public List<Player> getSquad() {
        return squad;
    }

    public void addPlayer(Player player) {
        squad.add(player);
    }

    public void removePlayer(Player player) {
        squad.remove(player);
    }
}