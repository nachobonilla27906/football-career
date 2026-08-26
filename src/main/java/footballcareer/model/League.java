package footballcareer.model;

import java.util.ArrayList;
import java.util.List;

public class League {

    private long id;

    private String name;
    private String country;
    private int tier;

    private List<Team> teams;

    public League() {
        this.teams = new ArrayList<>();
    }

    public League(
            long id,
            String name,
            String country,
            int tier
    ) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.tier = tier;
        this.teams = new ArrayList<>();
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public void addTeam(Team team) {
        teams.add(team);
    }

    public void removeTeam(Team team) {
        teams.remove(team);
    }
}