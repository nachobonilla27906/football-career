package footballcareer.database;

import footballcareer.model.Career;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CareerMatchStateRepository {

    public void initialize(Career career, boolean newCareer) {
        if (hasState(career.getId())) return;
        String sql = """
                INSERT INTO career_match_states
                    (career_id, match_id, home_goals, away_goals, played)
                SELECT ?, m.id,
                       CASE WHEN ? = 0 AND m.played = 1 AND m.date <= ? THEN m.home_goals ELSE 0 END,
                       CASE WHEN ? = 0 AND m.played = 1 AND m.date <= ? THEN m.away_goals ELSE 0 END,
                       CASE WHEN ? = 0 AND m.played = 1 AND m.date <= ? THEN 1 ELSE 0 END
                FROM matches m
                JOIN competitions c ON c.id = m.competition_id
                WHERE c.season_id = ?
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, career.getId());
            int existing = newCareer ? 1 : 0;
            statement.setInt(2, existing);
            statement.setString(3, career.getCurrentDate().toString());
            statement.setInt(4, existing);
            statement.setString(5, career.getCurrentDate().toString());
            statement.setInt(6, existing);
            statement.setString(7, career.getCurrentDate().toString());
            statement.setLong(8, career.getCurrentSeason().getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not initialize career match state.", exception);
        }
    }

    private boolean hasState(long careerId) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM career_match_states WHERE career_id = ? LIMIT 1")) {
            statement.setLong(1, careerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not inspect career match state.", exception);
        }
    }
}
