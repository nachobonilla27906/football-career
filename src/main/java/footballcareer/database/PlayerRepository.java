package footballcareer.database;

import footballcareer.model.Player;
import footballcareer.model.enums.Position;
import footballcareer.model.enums.PreferredFoot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class PlayerRepository {

    public void save(Player player) {

        String sql = """
                INSERT INTO players (
                    first_name,
                    last_name,
                    birth_date,
                    nationality,
                    position,
                    preferred_foot,
                    overall,
                    potential,
                    pace,
                    shooting,
                    passing,
                    dribbling,
                    defending,
                    physical,
                    market_value,
                    salary
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, player.getFirstName());
            statement.setString(2, player.getLastName());
            statement.setString(3, player.getBirthDate().toString());
            statement.setString(4, player.getNationality());
            statement.setString(5, player.getPosition().name());
            statement.setString(6, player.getPreferredFoot().name());
            statement.setInt(7, player.getOverall());
            statement.setInt(8, player.getPotential());
            statement.setInt(9, player.getPace());
            statement.setInt(10, player.getShooting());
            statement.setInt(11, player.getPassing());
            statement.setInt(12, player.getDribbling());
            statement.setInt(13, player.getDefending());
            statement.setInt(14, player.getPhysical());
            statement.setDouble(15, player.getMarketValue());
            statement.setDouble(16, player.getSalary());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Could not save player.", e);
        }
    }

    public Player findById(long id) {

        String sql = """
                SELECT *
                FROM players
                WHERE id = ?
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                return mapPlayer(resultSet);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Could not find player.", e);
        }
    }

    private Player mapPlayer(ResultSet resultSet) throws SQLException {

        return new Player(
                resultSet.getLong("id"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                LocalDate.parse(resultSet.getString("birth_date")),
                resultSet.getString("nationality"),
                Position.valueOf(resultSet.getString("position")),
                PreferredFoot.valueOf(
                        resultSet.getString("preferred_foot")
                ),
                resultSet.getInt("overall"),
                resultSet.getInt("potential"),
                resultSet.getInt("pace"),
                resultSet.getInt("shooting"),
                resultSet.getInt("passing"),
                resultSet.getInt("dribbling"),
                resultSet.getInt("defending"),
                resultSet.getInt("physical"),
                resultSet.getDouble("market_value"),
                resultSet.getDouble("salary")
        );
    }
}