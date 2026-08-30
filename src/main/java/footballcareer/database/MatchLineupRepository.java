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
        Long careerId = CareerContext.getCareerId();
        String table = careerId == null ? "match_lineups" : "career_match_lineups";
        String delete = "DELETE FROM " + table + " WHERE "
                + (careerId == null ? "" : "career_id = ? AND ") + "match_id = ? AND team_id = ?";
        String insert = careerId == null ? """
                INSERT INTO match_lineups (match_id, team_id, player_id, role, position_order)
                VALUES (?, ?, ?, ?, ?)
                """ : """
                INSERT INTO career_match_lineups
                    (career_id, match_id, team_id, player_id, role, position_order)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteStatement = connection.prepareStatement(delete);
                 PreparedStatement insertStatement = connection.prepareStatement(insert)) {
                int deleteOffset = 0;
                if (careerId != null) deleteStatement.setLong(++deleteOffset, careerId);
                deleteStatement.setLong(++deleteOffset, matchId);
                deleteStatement.setLong(++deleteOffset, teamId);
                deleteStatement.executeUpdate();
                addPlayers(insertStatement, careerId, matchId, teamId, starters, "STARTER");
                addPlayers(insertStatement, careerId, matchId, teamId, substitutes, "SUBSTITUTE");
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
        Long careerId = CareerContext.getCareerId();
        String sql = careerId == null ? """
                SELECT player_id, role FROM match_lineups
                WHERE match_id = ? AND team_id = ?
                ORDER BY CASE role WHEN 'STARTER' THEN 0 ELSE 1 END, position_order
                """ : """
                SELECT player_id, role FROM career_match_lineups
                WHERE career_id = ? AND match_id = ? AND team_id = ?
                ORDER BY CASE role WHEN 'STARTER' THEN 0 ELSE 1 END, position_order
                """;
        List<Player> starters = new ArrayList<>();
        List<Player> substitutes = new ArrayList<>();
        PlayerRepository players = new PlayerRepository();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int offset = 0;
            if (careerId != null) statement.setLong(++offset, careerId);
            statement.setLong(++offset, matchId);
            statement.setLong(++offset, teamId);
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

    private void addPlayers(PreparedStatement statement, Long careerId, long matchId, long teamId,
            List<Player> players, String role) throws SQLException {
        for (int i = 0; i < players.size(); i++) {
            int offset = 0;
            if (careerId != null) statement.setLong(++offset, careerId);
            statement.setLong(++offset, matchId);
            statement.setLong(++offset, teamId);
            statement.setLong(++offset, players.get(i).getId());
            statement.setString(++offset, role);
            statement.setInt(++offset, i);
            statement.addBatch();
        }
    }
}
