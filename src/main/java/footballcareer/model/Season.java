package footballcareer.model;

import java.time.LocalDate;

public class Season {

    private long id;

    private int startYear;
    private int endYear;

    private LocalDate startDate;
    private LocalDate endDate;

    private boolean finished;

    public Season() {
    }

    public Season(
            long id,
            int startYear,
            int endYear,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this.id = id;
        this.startYear = startYear;
        this.endYear = endYear;
        this.startDate = startDate;
        this.endDate = endDate;
        this.finished = false;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getStartYear() {
        return startYear;
    }

    public void setStartYear(int startYear) {
        this.startYear = startYear;
    }

    public int getEndYear() {
        return endYear;
    }

    public void setEndYear(int endYear) {
        this.endYear = endYear;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public String getName() {
        return startYear + "/" + String.valueOf(endYear).substring(2);
    }
}