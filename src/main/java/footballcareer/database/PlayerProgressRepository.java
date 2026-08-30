package footballcareer.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PlayerProgressRepository {
    public record Snapshot(LocalDate date, int overall, double marketValue) {}

    public void record(long playerId, LocalDate date, int overall, double marketValue) {
        Long careerId = CareerContext.getCareerId();
        if (careerId == null) return;
        String sql = """
                INSERT INTO career_player_progress_history
                    (career_id, player_id, snapshot_date, overall, market_value)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(career_id, player_id, snapshot_date) DO UPDATE SET
                    overall = excluded.overall, market_value = excluded.market_value
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, careerId); statement.setLong(2, playerId);
            statement.setString(3, date.toString()); statement.setInt(4, overall);
            statement.setDouble(5, marketValue); statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not record player progress.", e);
        }
    }

    public List<Snapshot> find(long playerId) {
        Long careerId = CareerContext.getCareerId();
        if (careerId == null) return List.of();
        List<Snapshot> snapshots = new ArrayList<>();
        String sql = """
                SELECT snapshot_date, overall, market_value
                FROM career_player_progress_history
                WHERE career_id = ? AND player_id = ? ORDER BY snapshot_date
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, careerId); statement.setLong(2, playerId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) snapshots.add(new Snapshot(
                        LocalDate.parse(rs.getString(1)), rs.getInt(2), rs.getDouble(3)));
            }
            return snapshots;
        } catch (SQLException e) {
            throw new RuntimeException("Could not load player progress.", e);
        }
    }
}
