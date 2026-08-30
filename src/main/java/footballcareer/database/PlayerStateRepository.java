package footballcareer.database;

import footballcareer.model.Player;
import footballcareer.model.PlayerState;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class PlayerStateRepository {
    public Map<Long, PlayerState> findAll() {
        Long careerId = CareerContext.getCareerId();
        String table = careerId == null ? "player_state" : "career_player_state";
        String sql = "SELECT * FROM " + table
                + (careerId == null ? "" : " WHERE career_id = " + careerId);
        Map<Long, PlayerState> states = new LinkedHashMap<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                long playerId = resultSet.getLong("player_id");
                Player player = new Player(); player.setId(playerId);
                PlayerState state = new PlayerState(); state.setPlayer(player);
                state.setForm(resultSet.getInt("form"));
                state.setMorale(resultSet.getInt("morale"));
                state.setFitness(resultSet.getInt("fitness"));
                readAvailability(resultSet, state);
                states.put(playerId, state);
            }
            return states;
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load player states.", exception);
        }
    }
    public void initializeMissingStates() {
        String sql = """
                INSERT OR IGNORE INTO player_state (player_id)
                SELECT id FROM players
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not initialize player states.", e);
        }
    }

    public PlayerState findByPlayer(long playerId) {
        Long careerId = CareerContext.getCareerId();
        String sql = careerId == null
                ? "SELECT * FROM player_state WHERE player_id = ?"
                : "SELECT * FROM career_player_state WHERE player_id = ? AND career_id = " + careerId;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, playerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return null;
                Player player = new Player(); player.setId(playerId);
                PlayerState state = new PlayerState();
                state.setPlayer(player);
                state.setForm(resultSet.getInt("form"));
                state.setMorale(resultSet.getInt("morale"));
                state.setFitness(resultSet.getInt("fitness"));
                readAvailability(resultSet, state);
                return state;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find player state.", e);
        }
    }

    public void update(PlayerState state) {
        Long careerId = CareerContext.getCareerId();
        String table = careerId == null ? "player_state" : "career_player_state";
        String sql = "UPDATE " + table + " SET form = ?, morale = ?, fitness = ? WHERE player_id = ?"
                + (careerId == null ? "" : " AND career_id = " + careerId);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, clamp(state.getForm()));
            statement.setInt(2, clamp(state.getMorale()));
            statement.setInt(3, clamp(state.getFitness()));
            statement.setLong(4, state.getPlayer().getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not update player state.", e);
        }
    }

    public void recoverAllFitness(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Recovery must be positive.");
        Long careerId = CareerContext.getCareerId();
        String table = careerId == null ? "player_state" : "career_player_state";
        String sql = "UPDATE " + table + " SET fitness = MIN(100, fitness + ?)"
                + (careerId == null ? "" : " WHERE career_id = " + careerId);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, amount);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not recover player fitness.", e);
        }
    }

    public void setUnavailable(long playerId, java.time.LocalDate until, String reason) {
        if (until == null || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Availability date and reason are required.");
        }
        Long careerId = CareerContext.getCareerId();
        String table = careerId == null ? "player_state" : "career_player_state";
        String sql = "UPDATE " + table
                + " SET unavailable_until = ?, unavailable_reason = ? WHERE player_id = ?"
                + (careerId == null ? "" : " AND career_id = " + careerId);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, until.toString());
            statement.setString(2, reason);
            statement.setLong(3, playerId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not update player availability.", e);
        }
    }

    private void readAvailability(ResultSet resultSet, PlayerState state) throws SQLException {
        String until = resultSet.getString("unavailable_until");
        state.setUnavailableUntil(until == null ? null : java.time.LocalDate.parse(until));
        state.setUnavailableReason(resultSet.getString("unavailable_reason"));
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
