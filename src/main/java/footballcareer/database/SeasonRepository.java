package footballcareer.database;

import footballcareer.model.Season;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class SeasonRepository {

    public void save(Season season) {

        String sql = """
                INSERT INTO seasons (
                    start_year,
                    end_year,
                    start_date,
                    end_date,
                    finished
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     java.sql.Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setInt(1, season.getStartYear());
            statement.setInt(2, season.getEndYear());
            statement.setString(3, season.getStartDate().toString());
            statement.setString(4, season.getEndDate().toString());
            statement.setInt(5, season.isFinished() ? 1 : 0);

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    season.setId(generatedKeys.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Could not save season.", e);
        }
    }

    public Season findById(long id) {

        String sql = """
                SELECT *
                FROM seasons
                WHERE id = ?
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                return mapSeason(resultSet);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Could not find season.", e);
        }
    }

    public Season findFirst() {

        String sql = """
                SELECT *
                FROM seasons
                ORDER BY id
                LIMIT 1
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (!resultSet.next()) {
                return null;
            }

            return mapSeason(resultSet);

        } catch (SQLException e) {
            throw new RuntimeException("Could not find first season.", e);
        }
    }

    private Season mapSeason(ResultSet resultSet) throws SQLException {

        Season season = new Season();

        season.setId(resultSet.getLong("id"));
        season.setStartYear(resultSet.getInt("start_year"));
        season.setEndYear(resultSet.getInt("end_year"));
        season.setStartDate(
                LocalDate.parse(resultSet.getString("start_date"))
        );
        season.setEndDate(
                LocalDate.parse(resultSet.getString("end_date"))
        );
        season.setFinished(
                resultSet.getInt("finished") == 1
        );

        return season;
    }
}