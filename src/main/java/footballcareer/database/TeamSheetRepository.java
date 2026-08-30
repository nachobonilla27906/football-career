package footballcareer.database;

import footballcareer.model.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class TeamSheetRepository {
    public record Sheet(List<Player> starters, List<Player> substitutes,
                        MatchTacticsRepository.TacticalSetup tactics,
                        MatchRoleRepository.Assignment roles) {}

    public void save(long careerId, long teamId, Sheet sheet) {
        Objects.requireNonNull(sheet, "Base sheet is required.");
        Objects.requireNonNull(sheet.tactics(), "Tactical setup is required.");
        Objects.requireNonNull(sheet.roles(), "Match roles are required.");
        if (sheet.starters().size() != 11 || sheet.substitutes().size() > 7)
            throw new IllegalArgumentException("Base sheet requires eleven starters and up to seven substitutes.");
        Set<Long> starterIds = new HashSet<>();
        sheet.starters().forEach(player -> starterIds.add(player.getId()));
        Set<Long> allIds = new HashSet<>(starterIds);
        sheet.substitutes().forEach(player -> allIds.add(player.getId()));
        if (starterIds.size() != 11 || allIds.size() != sheet.starters().size() + sheet.substitutes().size())
            throw new IllegalArgumentException("A player cannot occupy two places in the base sheet.");
        if (!starterIds.contains(sheet.roles().captainId())
                || !starterIds.contains(sheet.roles().penaltyTakerId())
                || !starterIds.contains(sheet.roles().cornerTakerId()))
            throw new IllegalArgumentException("Captain and set-piece takers must be starters.");
        String metadata = """
                INSERT INTO career_team_sheets
                    (career_id, team_id, formation, mentality, pressing, tempo,
                     captain_id, penalty_taker_id, corner_taker_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(career_id, team_id) DO UPDATE SET formation=excluded.formation,
                    mentality=excluded.mentality, pressing=excluded.pressing, tempo=excluded.tempo,
                    captain_id=excluded.captain_id, penalty_taker_id=excluded.penalty_taker_id,
                    corner_taker_id=excluded.corner_taker_id
                """;
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement meta = connection.prepareStatement(metadata);
                 PreparedStatement delete = connection.prepareStatement(
                         "DELETE FROM career_team_sheet_players WHERE career_id=? AND team_id=?");
                 PreparedStatement insert = connection.prepareStatement("""
                         INSERT INTO career_team_sheet_players
                             (career_id, team_id, player_id, role, position_order)
                         VALUES (?, ?, ?, ?, ?)
                         """)) {
                bindMetadata(meta, careerId, teamId, sheet); meta.executeUpdate();
                delete.setLong(1, careerId); delete.setLong(2, teamId); delete.executeUpdate();
                addPlayers(insert, careerId, teamId, sheet.starters(), "STARTER");
                addPlayers(insert, careerId, teamId, sheet.substitutes(), "SUBSTITUTE");
                insert.executeBatch(); connection.commit();
            } catch (Exception exception) { connection.rollback(); throw exception; }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save base team sheet.", exception);
        }
    }

    public Sheet find(long careerId, long teamId) {
        String sql = "SELECT * FROM career_team_sheets WHERE career_id=? AND team_id=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, careerId); statement.setLong(2, teamId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                var tactics = new MatchTacticsRepository.TacticalSetup(result.getString("formation"),
                        result.getString("mentality"), result.getString("pressing"), result.getString("tempo"));
                var roles = new MatchRoleRepository.Assignment(result.getLong("captain_id"),
                        result.getLong("penalty_taker_id"), result.getLong("corner_taker_id"));
                return new Sheet(players(connection, careerId, teamId, "STARTER"),
                        players(connection, careerId, teamId, "SUBSTITUTE"), tactics, roles);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load base team sheet.", exception);
        }
    }

    private List<Player> players(Connection connection, long careerId, long teamId, String role)
            throws SQLException {
        List<Player> players = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT player_id FROM career_team_sheet_players
                WHERE career_id=? AND team_id=? AND role=? ORDER BY position_order
                """)) {
            statement.setLong(1, careerId); statement.setLong(2, teamId); statement.setString(3, role);
            try (ResultSet result = statement.executeQuery()) {
                PlayerRepository repository = new PlayerRepository();
                while (result.next()) players.add(repository.findById(result.getLong(1)));
            }
        }
        return players;
    }

    private void bindMetadata(PreparedStatement statement, long careerId, long teamId, Sheet sheet)
            throws SQLException {
        statement.setLong(1, careerId); statement.setLong(2, teamId);
        statement.setString(3, sheet.tactics().formation()); statement.setString(4, sheet.tactics().mentality());
        statement.setString(5, sheet.tactics().pressing()); statement.setString(6, sheet.tactics().tempo());
        statement.setLong(7, sheet.roles().captainId()); statement.setLong(8, sheet.roles().penaltyTakerId());
        statement.setLong(9, sheet.roles().cornerTakerId());
    }

    private void addPlayers(PreparedStatement statement, long careerId, long teamId,
            List<Player> players, String role) throws SQLException {
        for (int index = 0; index < players.size(); index++) {
            statement.setLong(1, careerId); statement.setLong(2, teamId);
            statement.setLong(3, players.get(index).getId()); statement.setString(4, role);
            statement.setInt(5, index); statement.addBatch();
        }
    }
}
