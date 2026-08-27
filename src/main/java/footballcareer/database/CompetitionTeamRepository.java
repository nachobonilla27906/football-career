package footballcareer.database;

import footballcareer.model.Competition;
import footballcareer.model.Season;
import footballcareer.model.Team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CompetitionTeamRepository {

    public void addTeamToCompetition(
            long competitionId,
            long teamId
    ) {

        String sql = """
                INSERT OR IGNORE INTO competition_teams (
                    competition_id,
                    team_id
                )
                VALUES (?, ?)
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, competitionId);
            statement.setLong(2, teamId);

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not add team to competition.",
                    e
            );
        }
    }

    public boolean belongsToCompetition(
            long competitionId,
            long teamId
    ) {

        String sql = """
                SELECT 1
                FROM competition_teams
                WHERE competition_id = ?
                  AND team_id = ?
                LIMIT 1
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, competitionId);
            statement.setLong(2, teamId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not check competition team.",
                    e
            );
        }
    }

    public List<Team> findTeamsByCompetition(long competitionId) {

        String sql = """
                SELECT t.*
                FROM teams t
                JOIN competition_teams ct
                    ON t.id = ct.team_id
                WHERE ct.competition_id = ?
                ORDER BY t.name
                """;

        List<Team> teams = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, competitionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    teams.add(mapTeam(resultSet));
                }
            }

            return teams;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not find teams for competition.",
                    e
            );
        }
    }

    public List<Competition> findCompetitionsByTeam(long teamId) {

        String sql = """
                SELECT
                    c.id,
                    c.name,
                    c.country,
                    c.tier,
                    s.id AS season_id,
                    s.start_year,
                    s.end_year,
                    s.start_date,
                    s.end_date,
                    s.finished
                FROM competitions c
                JOIN competition_teams ct
                    ON c.id = ct.competition_id
                JOIN seasons s
                    ON c.season_id = s.id
                WHERE ct.team_id = ?
                ORDER BY s.start_date, c.name
                """;

        List<Competition> competitions = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, teamId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Season season = new Season(
                            resultSet.getLong("season_id"),
                            resultSet.getInt("start_year"),
                            resultSet.getInt("end_year"),
                            LocalDate.parse(resultSet.getString("start_date")),
                            LocalDate.parse(resultSet.getString("end_date"))
                    );
                    season.setFinished(resultSet.getInt("finished") == 1);

                    competitions.add(new Competition(
                            resultSet.getLong("id"),
                            resultSet.getString("name"),
                            resultSet.getString("country"),
                            resultSet.getInt("tier"),
                            season
                    ));
                }
            }

            return competitions;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not find competitions for team.",
                    e
            );
        }
    }

    private Team mapTeam(ResultSet resultSet) throws SQLException {
        return new Team(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("short_name"),
                resultSet.getString("country"),
                resultSet.getString("stadium_name"),
                resultSet.getInt("stadium_capacity"),
                resultSet.getInt("reputation")
        );
    }
}
