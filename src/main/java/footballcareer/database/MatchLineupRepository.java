package footballcareer.database;

import footballcareer.model.MatchLineup;
import footballcareer.model.Player;
import footballcareer.model.Team;
import footballcareer.model.enums.Position;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MatchLineupRepository {
    public void save(long matchId, long teamId, List<Player> starters,
            List<Player> substitutes) {
        if (starters.size() != 11) throw new IllegalArgumentException("Exactly eleven starters required.");
        if (starters.stream().noneMatch(player -> player.getPosition() == Position.GK))
            throw new IllegalArgumentException("Starting eleven requires a goalkeeper.");
        if (substitutes.size() > 7) throw new IllegalArgumentException("Maximum seven substitutes.");
        String delete = "DELETE FROM match_lineups WHERE match_id = ? AND team_id = ?";
        String insert = """
                INSERT INTO match_lineups (match_id, team_id, player_id, role, position_order)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteStatement = connection.prepareStatement(delete);
                 PreparedStatement insertStatement = connection.prepareStatement(insert)) {
                deleteStatement.setLong(1, matchId); deleteStatement.setLong(2, teamId);
                deleteStatement.executeUpdate();
                addPlayers(insertStatement, matchId, teamId, starters, "STARTER");
                addPlayers(insertStatement, matchId, teamId, substitutes, "SUBSTITUTE");
                insertStatement.executeBatch();
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not save match lineup.", e);
        }
    }

    public MatchLineup find(long matchId, long teamId) {
        String sql = """
                SELECT player_id, role FROM match_lineups
                WHERE match_id = ? AND team_id = ?
                ORDER BY CASE role WHEN 'STARTER' THEN 0 ELSE 1 END, position_order
                """;
        List<Player> starters = new ArrayList<>();
        List<Player> substitutes = new ArrayList<>();
        PlayerRepository players = new PlayerRepository();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, matchId); statement.setLong(2, teamId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Player player = players.findById(rs.getLong("player_id"));
                    if ("STARTER".equals(rs.getString("role"))) starters.add(player);
                    else substitutes.add(player);
                }
            }
            if (starters.isEmpty()) return null;
            Team team = new Team(); team.setId(teamId);
            return new MatchLineup(team, starters, substitutes);
        } catch (SQLException e) {
            throw new RuntimeException("Could not find match lineup.", e);
        }
    }

    private void addPlayers(PreparedStatement statement, long matchId, long teamId,
            List<Player> players, String role) throws SQLException {
        for (int i = 0; i < players.size(); i++) {
            statement.setLong(1, matchId); statement.setLong(2, teamId);
            statement.setLong(3, players.get(i).getId()); statement.setString(4, role);
            statement.setInt(5, i); statement.addBatch();
        }
    }
}
