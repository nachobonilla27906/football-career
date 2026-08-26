package footballcareer.database;

import footballcareer.model.Career;
import footballcareer.model.Season;
import footballcareer.model.Team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class CareerRepository {

    public void save(Career career) {

    String sql = """
            INSERT INTO careers (
                manager_name,
                controlled_team_id,
                current_season_id,
                current_date
            )
            VALUES (?, ?, ?, ?)
            """;

    try (Connection connection = Database.getConnection();
         PreparedStatement statement = connection.prepareStatement(
                 sql,
                 java.sql.Statement.RETURN_GENERATED_KEYS
         )) {

        statement.setString(1, career.getManagerName());
        statement.setLong(2, career.getControlledTeam().getId());
        statement.setLong(3, career.getCurrentSeason().getId());
        statement.setString(4, career.getCurrentDate().toString());

        statement.executeUpdate();

        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                career.setId(generatedKeys.getLong(1));
            }
        }

    } catch (SQLException e) {
        throw new RuntimeException("Could not save career.", e);
    }
}

    public Career findById(long id) {

        String sql = """
                SELECT *
                FROM careers
                WHERE id = ?
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                Team controlledTeam = new Team();
                controlledTeam.setId(
                        resultSet.getLong("controlled_team_id")
                );

                Season currentSeason = new Season();
                currentSeason.setId(
                        resultSet.getLong("current_season_id")
                );

                return new Career(
                        resultSet.getLong("id"),
                        resultSet.getString("manager_name"),
                        controlledTeam,
                        currentSeason,
                        LocalDate.parse(
                                resultSet.getString("current_date")
                        )
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException("Could not find career.", e);
        }
    }
}