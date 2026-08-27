package footballcareer.database;

import footballcareer.model.Competition;
import footballcareer.model.League;
import footballcareer.model.Season;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CompetitionRepository {

    public void save(Competition competition) {

        String sql = """
                INSERT INTO competitions (
                    name,
                    country,
                    tier,
                    season_id,
                    league_id
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     java.sql.Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setString(1, competition.getName());
            statement.setString(2, competition.getCountry());
            statement.setInt(3, competition.getTier());
            statement.setLong(
                    4,
                    competition.getSeason().getId()
            );

            if (competition.getLeague() == null) {
                statement.setNull(5, java.sql.Types.INTEGER);
            } else {
                statement.setLong(
                        5,
                        competition.getLeague().getId()
                );
            }

            statement.executeUpdate();

            try (ResultSet generatedKeys =
                         statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    competition.setId(
                            generatedKeys.getLong(1)
                    );
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not save competition.",
                    e
            );
        }
    }

    public Competition findById(long id) {

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
                    s.finished,
                    l.id AS league_id,
                    l.name AS league_name,
                    l.country AS league_country,
                    l.tier AS league_tier
                FROM competitions c
                JOIN seasons s
                    ON c.season_id = s.id
                LEFT JOIN leagues l
                    ON c.league_id = l.id
                WHERE c.id = ?
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                return mapCompetition(resultSet);
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not find competition.",
                    e
            );
        }
    }

    public Competition findByNameAndSeason(
            String name,
            long seasonId
    ) {

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
                    s.finished,
                    l.id AS league_id,
                    l.name AS league_name,
                    l.country AS league_country,
                    l.tier AS league_tier
                FROM competitions c
                JOIN seasons s
                    ON c.season_id = s.id
                LEFT JOIN leagues l
                    ON c.league_id = l.id
                WHERE c.name = ?
                  AND c.season_id = ?
                LIMIT 1
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, name);
            statement.setLong(2, seasonId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                return mapCompetition(resultSet);
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not find competition.",
                    e
            );
        }
    }

    public List<Competition> findBySeason(long seasonId) {
        String sql = competitionSelect()
                + " WHERE c.season_id = ? ORDER BY c.name";
        List<Competition> competitions = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, seasonId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    competitions.add(mapCompetition(resultSet));
                }
            }
            return competitions;
        } catch (SQLException e) {
            throw new RuntimeException("Could not find competitions.", e);
        }
    }

    private String competitionSelect() {
        return """
                SELECT c.id, c.name, c.country, c.tier,
                       s.id AS season_id, s.start_year, s.end_year,
                       s.start_date, s.end_date, s.finished,
                       l.id AS league_id, l.name AS league_name,
                       l.country AS league_country, l.tier AS league_tier
                FROM competitions c
                JOIN seasons s ON c.season_id = s.id
                LEFT JOIN leagues l ON c.league_id = l.id
                """;
    }

    private Competition mapCompetition(
            ResultSet resultSet
    ) throws SQLException {

        Season season = new Season();

        season.setId(
                resultSet.getLong("season_id")
        );

        season.setStartYear(
                resultSet.getInt("start_year")
        );

        season.setEndYear(
                resultSet.getInt("end_year")
        );

        season.setStartDate(
                java.time.LocalDate.parse(
                        resultSet.getString("start_date")
                )
        );

        season.setEndDate(
                java.time.LocalDate.parse(
                        resultSet.getString("end_date")
                )
        );

        season.setFinished(
                resultSet.getInt("finished") == 1
        );

        League league = null;
        long leagueId = resultSet.getLong("league_id");

        if (!resultSet.wasNull()) {
            league = new League(
                    leagueId,
                    resultSet.getString("league_name"),
                    resultSet.getString("league_country"),
                    resultSet.getInt("league_tier")
            );
        }

        return new Competition(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("country"),
                resultSet.getInt("tier"),
                season,
                league
        );
    }
}
