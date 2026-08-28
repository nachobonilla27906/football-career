package footballcareer.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public class MatchTacticsRepository {
    private static final Set<String> FORMATIONS = Set.of("4-3-3", "4-2-3-1", "4-4-2");

    public void saveFormation(long matchId, long teamId, String formation) {
        if (!FORMATIONS.contains(formation)) {
            throw new IllegalArgumentException("Unsupported formation: " + formation);
        }
        String sql = """
                INSERT INTO match_tactics (match_id, team_id, formation)
                VALUES (?, ?, ?)
                ON CONFLICT(match_id, team_id) DO UPDATE SET formation = excluded.formation
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, matchId);
            statement.setLong(2, teamId);
            statement.setString(3, formation);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save match formation.", exception);
        }
    }

    public String findFormation(long matchId, long teamId) {
        String sql = "SELECT formation FROM match_tactics WHERE match_id = ? AND team_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, matchId);
            statement.setLong(2, teamId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("formation") : "4-3-3";
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load match formation.", exception);
        }
    }
}
