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
                SELECT
                    c.id,
                    c.manager_name,
                    c.current_date,
                    t.id AS team_id,
                    t.name AS team_name,
                    t.short_name,
                    t.country AS team_country,
                    t.stadium_name,
                    t.stadium_capacity,
                    t.reputation,
                    s.id AS season_id,
                    s.start_year,
                    s.end_year,
                    s.start_date,
                    s.end_date,
                    s.finished
                FROM careers c
                JOIN teams t
                    ON c.controlled_team_id = t.id
                JOIN seasons s
                    ON c.current_season_id = s.id
                WHERE c.id = ?
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                Team controlledTeam = new Team(
                        resultSet.getLong("team_id"),
                        resultSet.getString("team_name"),
                        resultSet.getString("short_name"),
                        resultSet.getString("team_country"),
                        resultSet.getString("stadium_name"),
                        resultSet.getInt("stadium_capacity"),
                        resultSet.getInt("reputation")
                );

                Season currentSeason = new Season(
                        resultSet.getLong("season_id"),
                        resultSet.getInt("start_year"),
                        resultSet.getInt("end_year"),
                        LocalDate.parse(
                                resultSet.getString("start_date")
                        ),
                        LocalDate.parse(
                                resultSet.getString("end_date")
                        )
                );
                currentSeason.setFinished(
                        resultSet.getInt("finished") == 1
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

    public void updateCurrentDate(Career career) {

        String sql = """
                UPDATE careers
                SET current_date = ?
                WHERE id = ?
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    career.getCurrentDate().toString()
            );
            statement.setLong(2, career.getId());

            int updatedRows = statement.executeUpdate();

            if (updatedRows == 0) {
                throw new IllegalArgumentException(
                        "Career does not exist."
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not update career date.",
                    e
            );
        }
    }
}
