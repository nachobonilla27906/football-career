package footballcareer.database;

import footballcareer.model.Player;
import footballcareer.model.PlayerState;

import java.sql.*;

public class PlayerStateRepository {
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
        String sql = "SELECT * FROM player_state WHERE player_id = ?";
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
                return state;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find player state.", e);
        }
    }

    public void update(PlayerState state) {
        String sql = """
                UPDATE player_state SET form = ?, morale = ?, fitness = ?
                WHERE player_id = ?
                """;
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

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
