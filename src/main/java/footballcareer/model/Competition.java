package footballcareer.model;

public class Competition {

    private long id;

    private String name;
    private String country;

    private int tier;

    private Season season;
    private League league;
    private String format = "DOMESTIC_LEAGUE";

    public Competition() {
    }

    public Competition(
            long id,
            String name,
            String country,
            int tier,
            Season season
    ) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.tier = tier;
        this.season = season;
    }

    public Competition(
            long id,
            String name,
            String country,
            int tier,
            Season season,
            League league
    ) {
        this(id, name, country, tier, season);
        this.league = league;
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

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    public League getLeague() {
        return league;
    }

    public void setLeague(League league) {
        this.league = league;
    }

    public String getFormat() { return format; }

    public void setFormat(String format) {
        this.format = format == null ? "DOMESTIC_LEAGUE" : format;
    }

    public boolean isEuropean() { return "EUROPEAN".equals(format); }
}
