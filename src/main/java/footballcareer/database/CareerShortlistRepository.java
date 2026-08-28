package footballcareer.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class CareerShortlistRepository {

    public void add(long careerId, long playerId, LocalDate date) {
        String sql = """
                INSERT OR IGNORE INTO career_shortlist (career_id, player_id, added_date)
                VALUES (?, ?, ?)
                """;
        update(sql, careerId, playerId, date.toString());
    }

    public void remove(long careerId, long playerId) {
        update("DELETE FROM career_shortlist WHERE career_id = ? AND player_id = ?",
                careerId, playerId);
    }

    public boolean contains(long careerId, long playerId) {
        String sql = "SELECT 1 FROM career_shortlist WHERE career_id = ? AND player_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, careerId);
            statement.setLong(2, playerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not check career shortlist.", exception);
        }
    }

    public Set<Long> findPlayerIds(long careerId) {
        Set<Long> ids = new HashSet<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT player_id FROM career_shortlist WHERE career_id = ?")) {
            statement.setLong(1, careerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) ids.add(resultSet.getLong("player_id"));
            }
            return ids;
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load career shortlist.", exception);
        }
    }

    private void update(String sql, Object... values) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++)
                statement.setObject(index + 1, values[index]);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not update career shortlist.", exception);
        }
    }
}
