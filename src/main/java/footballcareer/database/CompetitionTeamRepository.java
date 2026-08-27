package footballcareer.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CompetitionTeamRepository {

    public void addTeamToCompetition(
            long competitionId,
            long teamId
    ) {

        String sql = """
                INSERT OR IGNORE INTO competition_teams (
                    competition_id,
                    team_id
                )
                VALUES (?, ?)
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, competitionId);
            statement.setLong(2, teamId);

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not add team to competition.",
                    e
            );
        }
    }

    public boolean belongsToCompetition(
            long competitionId,
            long teamId
    ) {

        String sql = """
                SELECT 1
                FROM competition_teams
                WHERE competition_id = ?
                  AND team_id = ?
                LIMIT 1
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, competitionId);
            statement.setLong(2, teamId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not check competition team.",
                    e
            );
        }
    }
}
