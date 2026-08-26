package footballcareer.model;

import java.time.LocalDate;

public class Match {

    private long id;

    private Competition competition;

    private Team homeTeam;
    private Team awayTeam;

    private LocalDate date;

    private int homeGoals;
    private int awayGoals;

    private boolean played;

    public Match() {
    }

    public Match(
            long id,
            Competition competition,
            Team homeTeam,
            Team awayTeam,
            LocalDate date
    ) {
        this.id = id;
        this.competition = competition;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.date = date;
        this.homeGoals = 0;
        this.awayGoals = 0;
        this.played = false;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Competition getCompetition() {
        return competition;
    }

    public void setCompetition(Competition competition) {
        this.competition = competition;
    }

    public Team getHomeTeam() {
        return homeTeam;
    }

    public void setHomeTeam(Team homeTeam) {
        this.homeTeam = homeTeam;
    }

    public Team getAwayTeam() {
        return awayTeam;
    }

    public void setAwayTeam(Team awayTeam) {
        this.awayTeam = awayTeam;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getHomeGoals() {
        return homeGoals;
    }

    public int getAwayGoals() {
        return awayGoals;
    }

    public boolean isPlayed() {
        return played;
    }

    public void setResult(int homeGoals, int awayGoals) {
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
        this.played = true;
    }
}