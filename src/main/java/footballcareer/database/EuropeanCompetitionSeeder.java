package footballcareer.database;

import footballcareer.model.Competition;
import footballcareer.model.Season;
import footballcareer.model.Team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Creates the reduced 24-club UEFA pyramid used while five leagues exist. */
public final class EuropeanCompetitionSeeder {
    private static final List<String> COUNTRIES =
            List.of("England", "Spain", "Italy", "Germany", "France");
    private static final String[] NAMES = {
            "UEFA Champions League", "UEFA Europa League", "UEFA Conference League"
    };
    private static final int[][] PLACES = {
            {5, 5, 5, 5, 4},
            {5, 5, 5, 4, 5},
            {5, 5, 4, 5, 5}
    };

    private final CompetitionRepository competitions = new CompetitionRepository();
    private final CompetitionTeamRepository entries = new CompetitionTeamRepository();

    public void seed(Season season) {
        Map<String, List<Team>> ranking = rankedDomesticTeams(season.getId());
        Map<String, Integer> offset = new HashMap<>();
        for (String country : COUNTRIES) offset.put(country, 0);

        for (int tournament = 0; tournament < NAMES.length; tournament++) {
            Competition competition = competitions.findByNameAndSeason(NAMES[tournament], season.getId());
            if (competition == null) {
                competition = new Competition(0, NAMES[tournament], "Europe",
                        tournament + 1, season);
                competition.setFormat("EUROPEAN");
                competitions.save(competition);
            }
            entries.clearTeams(competition.getId());
            for (int league = 0; league < COUNTRIES.size(); league++) {
                String country = COUNTRIES.get(league);
                List<Team> teams = ranking.getOrDefault(country, List.of());
                int start = offset.get(country);
                int end = Math.min(start + PLACES[tournament][league], teams.size());
                for (int index = start; index < end; index++) {
                    entries.addTeamToCompetition(competition.getId(), teams.get(index).getId());
                }
                offset.put(country, end);
            }
        }
    }

    private Map<String, List<Team>> rankedDomesticTeams(long seasonId) {
        String sql = """
                SELECT t.*, AVG(p.overall) squad_rating
                FROM teams t
                JOIN competition_teams ct ON ct.team_id = t.id
                JOIN competitions c ON c.id = ct.competition_id AND c.league_id IS NOT NULL
                JOIN player_team pt ON pt.team_id = t.id AND pt.end_date IS NULL
                JOIN players p ON p.id = pt.player_id
                WHERE c.season_id = ?
                GROUP BY t.id
                ORDER BY t.country, (AVG(p.overall) * 0.75 + t.reputation * 0.25) DESC, t.name
                """;
        Map<String, List<Team>> result = new HashMap<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, seasonId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Team team = new Team(rows.getLong("id"), rows.getString("name"),
                            rows.getString("short_name"), rows.getString("country"),
                            rows.getString("stadium_name"), rows.getInt("stadium_capacity"),
                            rows.getInt("reputation"));
                    result.computeIfAbsent(team.getCountry(), ignored -> new ArrayList<>()).add(team);
                }
            }
            return result;
        } catch (SQLException exception) {
            throw new RuntimeException("Could not rank clubs for Europe.", exception);
        }
    }
}
