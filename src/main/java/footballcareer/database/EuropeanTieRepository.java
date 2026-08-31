package footballcareer.database;

import footballcareer.model.Team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EuropeanTieRepository {
    public record Tie(long id, String stage, int order, Team home, Team away, Long winnerId) {}

    public void save(long careerId, long competitionId, String stage, int order,
            long homeId, long awayId) {
        String sql = """
                INSERT OR IGNORE INTO european_ties
                    (career_id, competition_id, stage, bracket_order, home_team_id, away_team_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, careerId); statement.setLong(2, competitionId);
            statement.setString(3, stage); statement.setInt(4, order);
            statement.setLong(5, homeId); statement.setLong(6, awayId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save European tie.", exception);
        }
    }

    public List<Tie> find(long careerId, long competitionId, String stage) {
        String sql = """
                SELECT e.*, h.*, a.id away_id, a.name away_name, a.short_name away_short_name,
                       a.country away_country, a.stadium_name away_stadium_name,
                       a.stadium_capacity away_stadium_capacity, a.reputation away_reputation
                FROM european_ties e JOIN teams h ON h.id=e.home_team_id
                JOIN teams a ON a.id=e.away_team_id
                WHERE e.career_id=? AND e.competition_id=? AND e.stage=? ORDER BY e.bracket_order
                """;
        List<Tie> ties = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, careerId); statement.setLong(2, competitionId);
            statement.setString(3, stage);
            try (ResultSet row = statement.executeQuery()) {
                while (row.next()) {
                    Team home = new Team(row.getLong("home_team_id"), row.getString("name"),
                            row.getString("short_name"), row.getString("country"),
                            row.getString("stadium_name"), row.getInt("stadium_capacity"),
                            row.getInt("reputation"));
                    Team away = new Team(row.getLong("away_id"), row.getString("away_name"),
                            row.getString("away_short_name"), row.getString("away_country"),
                            row.getString("away_stadium_name"), row.getInt("away_stadium_capacity"),
                            row.getInt("away_reputation"));
                    long winner = row.getLong("winner_team_id");
                    ties.add(new Tie(row.getLong("id"), stage, row.getInt("bracket_order"),
                            home, away, row.wasNull() ? null : winner));
                }
            }
            return ties;
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load European ties.", exception);
        }
    }

    public void setWinner(long tieId, long winnerId) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE european_ties SET winner_team_id=? WHERE id=?")) {
            statement.setLong(1, winnerId); statement.setLong(2, tieId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not close European tie.", exception);
        }
    }
}
