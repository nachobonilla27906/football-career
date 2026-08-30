package footballcareer.database;

import footballcareer.model.Match;
import footballcareer.model.MatchEvent;
import footballcareer.model.Player;
import footballcareer.model.Team;
import footballcareer.model.enums.MatchEventType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MatchEventRepository {
    public void save(MatchEvent event) {
        validate(event);
        Long careerId = CareerContext.getCareerId();
        String sql = careerId == null ? """
                INSERT INTO match_events
                    (match_id, team_id, player_id, secondary_player_id, minute, type)
                VALUES (?, ?, ?, ?, ?, ?)
                """ : """
                INSERT INTO career_match_events
                    (career_id, match_id, team_id, player_id, secondary_player_id, minute, type)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            int offset = 0;
            if (careerId != null) statement.setLong(++offset, careerId);
            statement.setLong(++offset, event.getMatch().getId());
            statement.setLong(++offset, event.getTeam().getId());
            statement.setLong(++offset, event.getPlayer().getId());
            if (event.getSecondaryPlayer() == null) statement.setNull(++offset, java.sql.Types.INTEGER);
            else statement.setLong(++offset, event.getSecondaryPlayer().getId());
            statement.setInt(++offset, event.getMinute());
            statement.setString(++offset, event.getType().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) event.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not save match event.", e);
        }
    }

    public List<MatchEvent> findByMatch(long matchId) {
        Long careerId = CareerContext.getCareerId();
        String sql = careerId == null
                ? "SELECT * FROM match_events WHERE match_id = ? ORDER BY minute, id"
                : "SELECT * FROM career_match_events WHERE career_id = ? AND match_id = ? ORDER BY minute, id";
        List<MatchEvent> events = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (careerId == null) statement.setLong(1, matchId);
            else { statement.setLong(1, careerId); statement.setLong(2, matchId); }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) events.add(mapEvent(rs));
            }
            return events;
        } catch (SQLException e) {
            throw new RuntimeException("Could not find match events.", e);
        }
    }

    private MatchEvent mapEvent(ResultSet rs) throws SQLException {
        MatchEvent event = new MatchEvent();
        event.setId(rs.getLong("id"));
        Match match = new Match(); match.setId(rs.getLong("match_id"));
        Team team = new Team(); team.setId(rs.getLong("team_id"));
        Player player = new Player(); player.setId(rs.getLong("player_id"));
        event.setMatch(match); event.setTeam(team); event.setPlayer(player);
        long secondaryId = rs.getLong("secondary_player_id");
        if (!rs.wasNull()) {
            Player secondary = new Player(); secondary.setId(secondaryId);
            event.setSecondaryPlayer(secondary);
        }
        event.setMinute(rs.getInt("minute"));
        event.setType(MatchEventType.valueOf(rs.getString("type")));
        return event;
    }

    private void validate(MatchEvent event) {
        if (event == null || event.getMatch() == null || event.getTeam() == null
                || event.getPlayer() == null || event.getType() == null) {
            throw new IllegalArgumentException("Match event is incomplete.");
        }
        if (event.getMinute() < 1 || event.getMinute() > 120) {
            throw new IllegalArgumentException("Event minute must be between 1 and 120.");
        }
        if (event.getType() == MatchEventType.SUBSTITUTION
                && event.getSecondaryPlayer() == null) {
            throw new IllegalArgumentException("Substitution requires the incoming player.");
        }
    }
}
