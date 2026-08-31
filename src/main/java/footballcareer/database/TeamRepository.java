package footballcareer.database;

import footballcareer.model.Team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TeamRepository {

    public void save(Team team) {

        String sql = """
                INSERT INTO teams (
                    name,
                    short_name,
                    country,
                    stadium_name,
                    stadium_capacity,
                    reputation
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     java.sql.Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setString(1, team.getName());
            statement.setString(2, team.getShortName());
            statement.setString(3, team.getCountry());
            statement.setString(4, team.getStadiumName());
            statement.setInt(5, team.getStadiumCapacity());
            statement.setInt(6, team.getReputation());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    team.setId(generatedKeys.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Could not save team.", e);
        }
    }

    public Team findById(long id) {

        String sql = """
                SELECT *
                FROM teams
                WHERE id = ?
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                return mapTeam(resultSet);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Could not find team.", e);
        }
    }

    public Team findByShortName(String shortName) {

        String sql = """
                SELECT *
                FROM teams
                WHERE short_name = ?
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, shortName);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                return mapTeam(resultSet);
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not find team by short name.",
                    e
            );
        }
    }

    public List<Team> findAll() {

        String sql = """
                SELECT *
                FROM teams
                ORDER BY name
                """;

        List<Team> teams = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                teams.add(mapTeam(resultSet));
            }

            return teams;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not find teams.",
                    e
            );
        }
    }

    public List<Team> findCompetitionTeamsBySeason(long seasonId) {
        String sql = """
                SELECT DISTINCT t.* FROM teams t
                JOIN competition_teams ct ON ct.team_id = t.id
                JOIN competitions c ON c.id = ct.competition_id
                WHERE c.season_id = ?
                ORDER BY t.country, t.name
                """;
        List<Team> currentTeams = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, seasonId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) currentTeams.add(mapTeam(resultSet));
            }
            return currentTeams;
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load current competition teams.", exception);
        }
    }

    private Team mapTeam(ResultSet resultSet) throws SQLException {

        Team team = new Team();

        team.setId(resultSet.getLong("id"));
        team.setName(resultSet.getString("name"));
        team.setShortName(resultSet.getString("short_name"));
        team.setCountry(resultSet.getString("country"));
        team.setStadiumName(resultSet.getString("stadium_name"));
        team.setStadiumCapacity(
                resultSet.getInt("stadium_capacity")
        );
        team.setReputation(
                resultSet.getInt("reputation")
        );

        return team;
    }
}
