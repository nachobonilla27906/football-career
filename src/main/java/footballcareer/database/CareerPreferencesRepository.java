package footballcareer.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CareerPreferencesRepository {
    public record Preferences(boolean stopAtMatch, boolean stopOnOffer,
                              boolean stopOnFatigue, String assistanceLevel,
                              String difficulty, String managerIdentity) {
        public static Preferences defaults() {
            return new Preferences(true, true, true, "GUIDED", "NORMAL", "GENERALIST");
        }
    }

    public Preferences find(long careerId) {
        String sql = "SELECT * FROM career_preferences WHERE career_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, careerId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return Preferences.defaults();
                return new Preferences(rs.getInt("stop_at_match") == 1,
                        rs.getInt("stop_on_offer") == 1,
                        rs.getInt("stop_on_fatigue") == 1,
                        rs.getString("assistance_level"), rs.getString("difficulty"),
                        rs.getString("manager_identity"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load career preferences.", e);
        }
    }

    public void save(long careerId, Preferences preferences) {
        String sql = """
                INSERT INTO career_preferences
                    (career_id, stop_at_match, stop_on_offer, stop_on_fatigue,
                     assistance_level, difficulty, manager_identity)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(career_id) DO UPDATE SET
                    stop_at_match = excluded.stop_at_match,
                    stop_on_offer = excluded.stop_on_offer,
                    stop_on_fatigue = excluded.stop_on_fatigue,
                    assistance_level = excluded.assistance_level,
                    difficulty = excluded.difficulty,
                    manager_identity = excluded.manager_identity
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, careerId);
            statement.setInt(2, preferences.stopAtMatch() ? 1 : 0);
            statement.setInt(3, preferences.stopOnOffer() ? 1 : 0);
            statement.setInt(4, preferences.stopOnFatigue() ? 1 : 0);
            statement.setString(5, preferences.assistanceLevel());
            statement.setString(6, preferences.difficulty());
            statement.setString(7, preferences.managerIdentity());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not save career preferences.", e);
        }
    }
}
