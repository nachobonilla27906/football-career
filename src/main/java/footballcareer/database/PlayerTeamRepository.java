package footballcareer.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class PlayerTeamRepository {

    public void assignPlayerToTeam(
            long playerId,
            long teamId,
            LocalDate startDate
    ) {

        String sql = """
                INSERT INTO player_team (
                    player_id,
                    team_id,
                    start_date
                )
                VALUES (?, ?, ?)
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, playerId);
            statement.setLong(2, teamId);
            statement.setString(3, startDate.toString());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not assign player to team.",
                    e
            );
        }
    }

    public Long findCurrentTeamId(long playerId) {

        String sql = """
                SELECT team_id
                FROM player_team
                WHERE player_id = ?
                  AND end_date IS NULL
                ORDER BY start_date DESC
                LIMIT 1
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, playerId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                return resultSet.getLong("team_id");
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not find current team for player.",
                    e
            );
        }
    }

    public void transferPlayer(
            long playerId,
            long newTeamId,
            LocalDate transferDate
    ) {

        String updateSql = """
                UPDATE player_team
                SET end_date = ?
                WHERE player_id = ?
                  AND end_date IS NULL
                """;

        String insertSql = """
                INSERT INTO player_team (
                    player_id,
                    team_id,
                    start_date
                )
                VALUES (?, ?, ?)
                """;

        try (Connection connection = Database.getConnection()) {

            connection.setAutoCommit(false);

            try (
                    PreparedStatement updateStatement =
                            connection.prepareStatement(updateSql);
                    PreparedStatement insertStatement =
                            connection.prepareStatement(insertSql)
            ) {

                updateStatement.setString(
                        1,
                        transferDate.toString()
                );
                updateStatement.setLong(2, playerId);

                updateStatement.executeUpdate();

                insertStatement.setLong(1, playerId);
                insertStatement.setLong(2, newTeamId);
                insertStatement.setString(
                        3,
                        transferDate.toString()
                );

                insertStatement.executeUpdate();

                connection.commit();

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not transfer player.",
                    e
            );
        }
    }
}