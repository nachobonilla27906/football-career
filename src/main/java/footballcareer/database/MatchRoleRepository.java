package footballcareer.database;

import footballcareer.model.MatchLineup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

public class MatchRoleRepository {
    public record Assignment(long captainId, long penaltyTakerId, long cornerTakerId) {}

    public void save(long matchId, long teamId, Assignment assignment) {
        MatchLineup lineup = new MatchLineupRepository().find(matchId, teamId);
        Set<Long> starters = lineup == null ? Set.of() : lineup.getStarters().stream()
                .map(player -> player.getId()).collect(Collectors.toSet());
        if (!starters.contains(assignment.captainId())
                || !starters.contains(assignment.penaltyTakerId())
                || !starters.contains(assignment.cornerTakerId()))
            throw new IllegalArgumentException("Match roles must belong to the starting eleven.");
        Long careerId = CareerContext.getCareerId();
        String sql = careerId == null ? """
                INSERT INTO match_roles
                    (match_id, team_id, captain_id, penalty_taker_id, corner_taker_id)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(match_id, team_id) DO UPDATE SET captain_id=excluded.captain_id,
                    penalty_taker_id=excluded.penalty_taker_id,
                    corner_taker_id=excluded.corner_taker_id
                """ : """
                INSERT INTO career_match_roles
                    (career_id, match_id, team_id, captain_id, penalty_taker_id, corner_taker_id)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(career_id, match_id, team_id) DO UPDATE SET
                    captain_id=excluded.captain_id, penalty_taker_id=excluded.penalty_taker_id,
                    corner_taker_id=excluded.corner_taker_id
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 0;
            if (careerId != null) statement.setLong(++index, careerId);
            statement.setLong(++index, matchId); statement.setLong(++index, teamId);
            statement.setLong(++index, assignment.captainId());
            statement.setLong(++index, assignment.penaltyTakerId());
            statement.setLong(++index, assignment.cornerTakerId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save match roles.", exception);
        }
    }

    public Assignment find(long matchId, long teamId) {
        Long careerId = CareerContext.getCareerId();
        String sql = careerId == null
                ? "SELECT * FROM match_roles WHERE match_id=? AND team_id=?"
                : "SELECT * FROM career_match_roles WHERE career_id=? AND match_id=? AND team_id=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 0;
            if (careerId != null) statement.setLong(++index, careerId);
            statement.setLong(++index, matchId); statement.setLong(++index, teamId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? new Assignment(result.getLong("captain_id"),
                        result.getLong("penalty_taker_id"), result.getLong("corner_taker_id")) : null;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load match roles.", exception);
        }
    }
}
