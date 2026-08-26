package footballcareer.model;

import java.time.LocalDate;

public class Career {

    private long id;

    private String managerName;

    private Team controlledTeam;
    private Season currentSeason;

    private LocalDate currentDate;

    public Career() {
    }

    public Career(
            long id,
            String managerName,
            Team controlledTeam,
            Season currentSeason,
            LocalDate currentDate
    ) {
        this.id = id;
        this.managerName = managerName;
        this.controlledTeam = controlledTeam;
        this.currentSeason = currentSeason;
        this.currentDate = currentDate;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public Team getControlledTeam() {
        return controlledTeam;
    }

    public void setControlledTeam(Team controlledTeam) {
        this.controlledTeam = controlledTeam;
    }

    public Season getCurrentSeason() {
        return currentSeason;
    }

    public void setCurrentSeason(Season currentSeason) {
        this.currentSeason = currentSeason;
    }

    public LocalDate getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(LocalDate currentDate) {
        this.currentDate = currentDate;
    }
}