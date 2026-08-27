package footballcareer.database;

import footballcareer.model.Player;
import footballcareer.model.enums.Position;
import footballcareer.model.enums.PreferredFoot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     java.sql.Statement.RETURN_GENERATED_KEYS
             )) {

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

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    player.setId(generatedKeys.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not save player.",
                    e
            );
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
            throw new RuntimeException(
                    "Could not find player.",
                    e
            );
        }
    }

    public Player findByName(
            String firstName,
            String lastName
    ) {

        String sql = """
                SELECT *
                FROM players
                WHERE first_name = ?
                  AND last_name = ?
                LIMIT 1
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, firstName);
            statement.setString(2, lastName);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                return mapPlayer(resultSet);
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not find player by name.",
                    e
            );
        }
    }

    public Player findByIdentity(
            String firstName,
            String lastName,
            LocalDate birthDate
    ) {
        String sql = """
                SELECT * FROM players
                WHERE first_name = ?
                  AND last_name = ?
                  AND birth_date = ?
                LIMIT 1
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, firstName);
            statement.setString(2, lastName);
            statement.setString(3, birthDate.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapPlayer(resultSet) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find player identity.", e);
        }
    }

    public List<Player> findCurrentPlayersByTeam(long teamId) {
        String sql = """
                SELECT p.*
                FROM players p
                JOIN player_team pt ON p.id = pt.player_id
                WHERE pt.team_id = ?
                  AND pt.end_date IS NULL
                ORDER BY p.position, p.overall DESC, p.last_name
                """;
        List<Player> players = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teamId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    players.add(mapPlayer(resultSet));
                }
            }
            return players;
        } catch (SQLException e) {
            throw new RuntimeException("Could not find team players.", e);
        }
    }

    public void updateDevelopment(Player player) {
        String sql = """
                UPDATE players SET overall = ?, pace = ?, shooting = ?,
                    passing = ?, dribbling = ?, defending = ?, physical = ?
                WHERE id = ?
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, player.getOverall());
            statement.setInt(2, player.getPace());
            statement.setInt(3, player.getShooting());
            statement.setInt(4, player.getPassing());
            statement.setInt(5, player.getDribbling());
            statement.setInt(6, player.getDefending());
            statement.setInt(7, player.getPhysical());
            statement.setLong(8, player.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not update player development.", e);
        }
    }

    private Player mapPlayer(ResultSet resultSet) throws SQLException {

        return new Player(
                resultSet.getLong("id"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                LocalDate.parse(
                        resultSet.getString("birth_date")
                ),
                resultSet.getString("nationality"),
                Position.valueOf(
                        resultSet.getString("position")
                ),
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
