package footballcareer.ui;

import footballcareer.model.Competition;

import java.util.Locale;

/** Stable competition identity used by compact calendar cards. */
public final class CompetitionVisuals {
    private CompetitionVisuals() {}

    public static String calendarStyle(Competition competition) {
        String name = competition.getName().toLowerCase(Locale.ROOT);
        if (name.contains("champions")) return "competition-champions";
        if (name.contains("europa")) return "competition-europa";
        if (name.contains("conference")) return "competition-conference";
        if (name.contains("premier")) return "competition-premier";
        if (name.contains("laliga")) return "competition-laliga";
        if (name.contains("serie")) return "competition-serie-a";
        if (name.contains("bundesliga")) return "competition-bundesliga";
        if (name.contains("ligue")) return "competition-ligue-1";
        return "competition-generic";
    }

    public static String shortName(Competition competition) {
        String name = competition.getName().toLowerCase(Locale.ROOT);
        if (name.contains("champions")) return "UCL";
        if (name.contains("europa")) return "UEL";
        if (name.contains("conference")) return "UECL";
        if (name.contains("premier")) return "PL";
        if (name.contains("laliga")) return "LALIGA";
        if (name.contains("serie")) return "SERIE A";
        if (name.contains("bundesliga")) return "BUNDES";
        if (name.contains("ligue")) return "LIGUE 1";
        return competition.getName().length() <= 9 ? competition.getName().toUpperCase(Locale.ROOT)
                : competition.getName().substring(0, 9).toUpperCase(Locale.ROOT);
    }

    public static String standingZone(Competition competition, int position, int total) {
        if (competition.isEuropean()) {
            if (position < 8) return "zone-europe-seeded";
            if (position < 16) return "zone-europe-qualified";
            return null;
        }
        int champions = "France".equals(competition.getCountry()) ? 4 : 5;
        int europa = "Germany".equals(competition.getCountry()) ? 4 : 5;
        int conference = "Italy".equals(competition.getCountry()) ? 4 : 5;
        if (position < champions) return "zone-champions";
        if (position < champions + europa) return "zone-europa";
        if (position < champions + europa + conference) return "zone-conference";
        return position >= total - 3 ? "zone-relegation" : null;
    }

}
