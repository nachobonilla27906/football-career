package footballcareer.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class PlayerTeamRepository {

    public Map<Long, Long> findAllCurrentTeamIds() {
        Long careerId = CareerContext.getCareerId();
        String table = careerId == null ? "player_team" : "career_player_team";
        String sql = "SELECT player_id, team_id FROM " + table + " WHERE end_date IS NULL"
                + (careerId == null ? "" : " AND career_id = " + careerId);
        Map<Long, Long> teams = new HashMap<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) teams.put(resultSet.getLong("player_id"),
                    resultSet.getLong("team_id"));
            return teams;
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load current player teams.", exception);
        }
    }

    public void ensureInitialAssignment(long playerId, long teamId, LocalDate startDate) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO initial_player_team (player_id, team_id, start_date)
                     VALUES (?, ?, ?)
                     ON CONFLICT(player_id) DO UPDATE SET
                         team_id = excluded.team_id, start_date = excluded.start_date
                     """)) {
            statement.setLong(1, playerId); statement.setLong(2, teamId);
            statement.setString(3, startDate.toString()); statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not preserve initial player team.", exception);
        }
    }

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
        Long careerId = CareerContext.getCareerId();
        String table = careerId == null ? "player_team" : "career_player_team";
        String sql = """
                SELECT team_id
                FROM %s
                WHERE player_id = ?
                  %s
                  AND end_date IS NULL
                ORDER BY start_date DESC
                LIMIT 1
                """.formatted(table, careerId == null ? "" : "AND career_id = " + careerId);

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

        Long careerId = CareerContext.getCareerId();
        String table = careerId == null ? "player_team" : "career_player_team";
        String updateSql = """
                UPDATE %s
                SET end_date = ?
                WHERE player_id = ?
                  %s
                  AND end_date IS NULL
                """.formatted(table, careerId == null ? "" : "AND career_id = " + careerId);

        String insertSql = careerId == null ? """
                INSERT INTO player_team (
                    player_id,
                    team_id,
                    start_date
                )
                VALUES (?, ?, ?)
                """ : """
                INSERT INTO career_player_team (career_id, player_id, team_id, start_date)
                VALUES (?, ?, ?, ?)
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

                int offset = 0;
                if (careerId != null) insertStatement.setLong(++offset, careerId);
                insertStatement.setLong(++offset, playerId);
                insertStatement.setLong(++offset, newTeamId);
                insertStatement.setString(++offset, transferDate.toString());

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
