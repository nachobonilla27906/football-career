package footballcareer.model;

import footballcareer.model.enums.Position;
import footballcareer.model.enums.PreferredFoot;

import java.time.LocalDate;
import java.time.Period;

public class Player {

    private long id;

    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String nationality;

    private Position position;
    private PreferredFoot preferredFoot;
    private int heightCm = 180;
    private Position secondaryPosition;

    private int overall;
    private int potential;

    private int pace;
    private int shooting;
    private int passing;
    private int dribbling;
    private int defending;
    private int physical;

    private double marketValue;
    private double salary;

    public Player() {
    }

    public Player(
            long id,
            String firstName,
            String lastName,
            LocalDate birthDate,
            String nationality,
            Position position,
            PreferredFoot preferredFoot,
            int overall,
            int potential,
            int pace,
            int shooting,
            int passing,
            int dribbling,
            int defending,
            int physical,
            double marketValue,
            double salary
    ) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.nationality = nationality;
        this.position = position;
        this.preferredFoot = preferredFoot;
        this.overall = overall;
        this.potential = potential;
        this.pace = pace;
        this.shooting = shooting;
        this.passing = passing;
        this.dribbling = dribbling;
        this.defending = defending;
        this.physical = physical;
        this.marketValue = marketValue;
        this.salary = salary;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public int getAge(LocalDate date) {
        return Period.between(birthDate, date).getYears();
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public PreferredFoot getPreferredFoot() {
        return preferredFoot;
    }

    public void setPreferredFoot(PreferredFoot preferredFoot) {
        this.preferredFoot = preferredFoot;
    }

    public int getHeightCm() { return heightCm; }
    public void setHeightCm(int heightCm) { this.heightCm = heightCm; }
    public Position getSecondaryPosition() { return secondaryPosition; }
    public void setSecondaryPosition(Position secondaryPosition) {
        this.secondaryPosition = secondaryPosition;
    }

    public int getOverall() {
        return overall;
    }

    public void setOverall(int overall) {
        this.overall = overall;
    }

    public int getPotential() {
        return potential;
    }

    public void setPotential(int potential) {
        this.potential = potential;
    }

    public int getPace() {
        return pace;
    }

    public void setPace(int pace) {
        this.pace = pace;
    }

    public int getShooting() {
        return shooting;
    }

    public void setShooting(int shooting) {
        this.shooting = shooting;
    }

    public int getPassing() {
        return passing;
    }

    public void setPassing(int passing) {
        this.passing = passing;
    }

    public int getDribbling() {
        return dribbling;
    }

    public void setDribbling(int dribbling) {
        this.dribbling = dribbling;
    }

    public int getDefending() {
        return defending;
    }

    public void setDefending(int defending) {
        this.defending = defending;
    }

    public int getPhysical() {
        return physical;
    }

    public void setPhysical(int physical) {
        this.physical = physical;
    }

    public double getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(double marketValue) {
        this.marketValue = marketValue;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
