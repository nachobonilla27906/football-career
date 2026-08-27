package footballcareer.database;

import footballcareer.model.League;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LeagueRepository {

    public void save(League league) {

        String sql = """
                INSERT INTO leagues (
                    name,
                    country,
                    tier
                )
                VALUES (?, ?, ?)
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     java.sql.Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setString(1, league.getName());
            statement.setString(2, league.getCountry());
            statement.setInt(3, league.getTier());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    league.setId(generatedKeys.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not save league.",
                    e
            );
        }
    }

    public League findById(long id) {

        String sql = """
                SELECT *
                FROM leagues
                WHERE id = ?
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                return mapLeague(resultSet);
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not find league.",
                    e
            );
        }
    }

    public League findFirst() {

        String sql = """
                SELECT *
                FROM leagues
                ORDER BY id
                LIMIT 1
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (!resultSet.next()) {
                return null;
            }

            return mapLeague(resultSet);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not find first league.",
                    e
            );
        }
    }

    public League findByName(
            String name,
            String country
    ) {

        String sql = """
                SELECT *
                FROM leagues
                WHERE name = ?
                  AND country = ?
                LIMIT 1
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, name);
            statement.setString(2, country);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                return mapLeague(resultSet);
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not find league by name.",
                    e
            );
        }
    }

    private League mapLeague(
            ResultSet resultSet
    ) throws SQLException {

        League league = new League();

        league.setId(resultSet.getLong("id"));
        league.setName(resultSet.getString("name"));
        league.setCountry(resultSet.getString("country"));
        league.setTier(resultSet.getInt("tier"));

        return league;
    }
}