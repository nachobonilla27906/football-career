package footballcareer.database;

import footballcareer.model.Competition;
import footballcareer.model.Match;
import footballcareer.model.Season;
import footballcareer.model.Team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MatchRepository {

    public void save(Match match) {

        String sql = """
                INSERT INTO matches (
                    competition_id,
                    home_team_id,
                    away_team_id,
                    date,
                    home_goals,
                    away_goals,
                    played
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     java.sql.Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setLong(1, match.getCompetition().getId());
            statement.setLong(2, match.getHomeTeam().getId());
            statement.setLong(3, match.getAwayTeam().getId());
            statement.setString(4, match.getDate().toString());
            statement.setInt(5, match.getHomeGoals());
            statement.setInt(6, match.getAwayGoals());
            statement.setInt(7, match.isPlayed() ? 1 : 0);

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    match.setId(generatedKeys.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Could not save match.", e);
        }
    }

    public Match findById(long id) {
        String sql = baseSelect() + " WHERE m.id = ?";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapMatch(resultSet) : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Could not find match.", e);
        }
    }

    public List<Match> findByCompetition(long competitionId) {
        String sql = baseSelect()
                + " WHERE m.competition_id = ? ORDER BY m.date, m.id";

        return findMany(sql, competitionId, null);
    }

    public List<Match> findByDate(LocalDate date) {
        String sql = baseSelect()
                + " WHERE m.date = ? ORDER BY m.id";

        return findMany(sql, null, date);
    }

    public void updateResult(Match match) {

        if (!match.isPlayed()) {
            throw new IllegalArgumentException(
                    "Match must have a result before it can be updated."
            );
        }

        String sql = """
                UPDATE matches
                SET home_goals = ?,
                    away_goals = ?,
                    played = 1
                WHERE id = ?
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, match.getHomeGoals());
            statement.setInt(2, match.getAwayGoals());
            statement.setLong(3, match.getId());

            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Match does not exist.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Could not update match result.", e);
        }
    }

    private List<Match> findMany(
            String sql,
            Long competitionId,
            LocalDate date
    ) {
        List<Match> matches = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (competitionId != null) {
                statement.setLong(1, competitionId);
            } else {
                statement.setString(1, date.toString());
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    matches.add(mapMatch(resultSet));
                }
            }

            return matches;

        } catch (SQLException e) {
            throw new RuntimeException("Could not find matches.", e);
        }
    }

    private String baseSelect() {
        return """
                SELECT
                    m.id,
                    m.date,
                    m.home_goals,
                    m.away_goals,
                    m.played,
                    c.id AS competition_id,
                    c.name AS competition_name,
                    c.country AS competition_country,
                    c.tier AS competition_tier,
                    s.id AS season_id,
                    s.start_year,
                    s.end_year,
                    s.start_date,
                    s.end_date,
                    s.finished,
                    h.id AS home_id,
                    h.name AS home_name,
                    h.short_name AS home_short_name,
                    h.country AS home_country,
                    h.stadium_name AS home_stadium_name,
                    h.stadium_capacity AS home_stadium_capacity,
                    h.reputation AS home_reputation,
                    a.id AS away_id,
                    a.name AS away_name,
                    a.short_name AS away_short_name,
                    a.country AS away_country,
                    a.stadium_name AS away_stadium_name,
                    a.stadium_capacity AS away_stadium_capacity,
                    a.reputation AS away_reputation
                FROM matches m
                JOIN competitions c ON m.competition_id = c.id
                JOIN seasons s ON c.season_id = s.id
                JOIN teams h ON m.home_team_id = h.id
                JOIN teams a ON m.away_team_id = a.id
                """;
    }

    private Match mapMatch(ResultSet resultSet) throws SQLException {
        Season season = new Season(
                resultSet.getLong("season_id"),
                resultSet.getInt("start_year"),
                resultSet.getInt("end_year"),
                LocalDate.parse(resultSet.getString("start_date")),
                LocalDate.parse(resultSet.getString("end_date"))
        );
        season.setFinished(resultSet.getInt("finished") == 1);

        Competition competition = new Competition(
                resultSet.getLong("competition_id"),
                resultSet.getString("competition_name"),
                resultSet.getString("competition_country"),
                resultSet.getInt("competition_tier"),
                season
        );

        Team homeTeam = mapTeam(resultSet, "home_");
        Team awayTeam = mapTeam(resultSet, "away_");

        Match match = new Match(
                resultSet.getLong("id"),
                competition,
                homeTeam,
                awayTeam,
                LocalDate.parse(resultSet.getString("date"))
        );

        if (resultSet.getInt("played") == 1) {
            match.setResult(
                    resultSet.getInt("home_goals"),
                    resultSet.getInt("away_goals")
            );
        }

        return match;
    }

    private Team mapTeam(
            ResultSet resultSet,
            String prefix
    ) throws SQLException {
        return new Team(
                resultSet.getLong(prefix + "id"),
                resultSet.getString(prefix + "name"),
                resultSet.getString(prefix + "short_name"),
                resultSet.getString(prefix + "country"),
                resultSet.getString(prefix + "stadium_name"),
                resultSet.getInt(prefix + "stadium_capacity"),
                resultSet.getInt(prefix + "reputation")
        );
    }
}
