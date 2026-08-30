package footballcareer.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public class MatchTacticsRepository {
    private static final Set<String> FORMATIONS = Set.of("4-3-3", "4-2-3-1", "4-4-2");
    private static final Set<String> MENTALITIES = Set.of("DEFENSIVE", "BALANCED", "ATTACKING");
    private static final Set<String> PRESSING = Set.of("LOW", "MEDIUM", "HIGH");
    private static final Set<String> TEMPOS = Set.of("SLOW", "NORMAL", "FAST");
    public record TacticalSetup(String formation, String mentality, String pressing,
                                String tempo) {}

    public void saveFormation(long matchId, long teamId, String formation) {
        if (!FORMATIONS.contains(formation)) {
            throw new IllegalArgumentException("Unsupported formation: " + formation);
        }
        Long careerId = CareerContext.getCareerId();
        String sql = careerId == null ? """
                INSERT INTO match_tactics (match_id, team_id, formation)
                VALUES (?, ?, ?)
                ON CONFLICT(match_id, team_id) DO UPDATE SET formation = excluded.formation
                """ : """
                INSERT INTO career_match_tactics (career_id, match_id, team_id, formation)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(career_id, match_id, team_id) DO UPDATE SET formation = excluded.formation
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int offset = 0;
            if (careerId != null) statement.setLong(++offset, careerId);
            statement.setLong(++offset, matchId);
            statement.setLong(++offset, teamId);
            statement.setString(++offset, formation);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save match formation.", exception);
        }
    }

    public String findFormation(long matchId, long teamId) {
        return find(matchId, teamId).formation();
    }

    public void save(long matchId, long teamId, TacticalSetup setup) {
        if (!FORMATIONS.contains(setup.formation()) || !MENTALITIES.contains(setup.mentality())
                || !PRESSING.contains(setup.pressing()) || !TEMPOS.contains(setup.tempo())) {
            throw new IllegalArgumentException("Unsupported tactical setup.");
        }
        Long careerId = CareerContext.getCareerId();
        String sql = careerId == null ? """
                INSERT INTO match_tactics (match_id, team_id, formation, mentality, pressing, tempo)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(match_id, team_id) DO UPDATE SET formation = excluded.formation,
                    mentality = excluded.mentality, pressing = excluded.pressing, tempo = excluded.tempo
                """ : """
                INSERT INTO career_match_tactics
                    (career_id, match_id, team_id, formation, mentality, pressing, tempo)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(career_id, match_id, team_id) DO UPDATE SET
                    formation = excluded.formation, mentality = excluded.mentality,
                    pressing = excluded.pressing, tempo = excluded.tempo
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int offset = 0;
            if (careerId != null) statement.setLong(++offset, careerId);
            statement.setLong(++offset, matchId); statement.setLong(++offset, teamId);
            statement.setString(++offset, setup.formation());
            statement.setString(++offset, setup.mentality());
            statement.setString(++offset, setup.pressing());
            statement.setString(++offset, setup.tempo());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save match tactics.", exception);
        }
    }

    public TacticalSetup find(long matchId, long teamId) {
        Long careerId = CareerContext.getCareerId();
        String sql = careerId == null
                ? "SELECT formation, mentality, pressing, tempo FROM match_tactics WHERE match_id = ? AND team_id = ?"
                : "SELECT formation, mentality, pressing, tempo FROM career_match_tactics WHERE career_id = ? AND match_id = ? AND team_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int offset = 0;
            if (careerId != null) statement.setLong(++offset, careerId);
            statement.setLong(++offset, matchId);
            statement.setLong(++offset, teamId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new TacticalSetup(resultSet.getString("formation"),
                        resultSet.getString("mentality"), resultSet.getString("pressing"),
                        resultSet.getString("tempo"))
                        : new TacticalSetup("4-3-3", "BALANCED", "MEDIUM", "NORMAL");
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load match formation.", exception);
        }
    }
}
