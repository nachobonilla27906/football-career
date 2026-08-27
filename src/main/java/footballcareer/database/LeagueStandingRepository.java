package footballcareer.database;

import footballcareer.model.Competition;
import footballcareer.model.LeagueStanding;
import footballcareer.model.Match;
import footballcareer.model.Team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LeagueStandingRepository {

    public void initialize(Competition competition, List<Team> teams) {
        String sql = """
                INSERT OR IGNORE INTO league_standings (
                    competition_id, team_id
                ) VALUES (?, ?)
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Team team : teams) {
                statement.setLong(1, competition.getId());
                statement.setLong(2, team.getId());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Could not initialize standings.", e);
        }
    }

    public List<LeagueStanding> findByCompetition(long competitionId) {
        String sql = """
                SELECT ls.*, t.*
                FROM league_standings ls
                JOIN teams t ON ls.team_id = t.id
                WHERE ls.competition_id = ?
                ORDER BY ls.points DESC,
                         (ls.goals_for - ls.goals_against) DESC,
                         ls.goals_for DESC,
                         t.name
                """;
        List<LeagueStanding> standings = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, competitionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    LeagueStanding standing = new LeagueStanding();
                    standing.setId(resultSet.getLong("id"));
                    Competition competition = new Competition();
                    competition.setId(competitionId);
                    standing.setCompetition(competition);
                    standing.setTeam(new Team(
                            resultSet.getLong("team_id"),
                            resultSet.getString("name"),
                            resultSet.getString("short_name"),
                            resultSet.getString("country"),
                            resultSet.getString("stadium_name"),
                            resultSet.getInt("stadium_capacity"),
                            resultSet.getInt("reputation")
                    ));
                    standing.setPlayed(resultSet.getInt("played"));
                    standing.setWins(resultSet.getInt("wins"));
                    standing.setDraws(resultSet.getInt("draws"));
                    standing.setLosses(resultSet.getInt("losses"));
                    standing.setGoalsFor(resultSet.getInt("goals_for"));
                    standing.setGoalsAgainst(resultSet.getInt("goals_against"));
                    standing.setPoints(resultSet.getInt("points"));
                    standings.add(standing);
                }
            }
            return standings;
        } catch (SQLException e) {
            throw new RuntimeException("Could not find standings.", e);
        }
    }

    public void applyResult(Match match) {
        if (!match.isPlayed()) {
            throw new IllegalArgumentException("Match has not been played.");
        }

        String sql = """
                UPDATE league_standings
                SET played = played + 1,
                    wins = wins + ?,
                    draws = draws + ?,
                    losses = losses + ?,
                    goals_for = goals_for + ?,
                    goals_against = goals_against + ?,
                    points = points + ?
                WHERE competition_id = ? AND team_id = ?
                """;

        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                boolean draw = match.getHomeGoals() == match.getAwayGoals();
                boolean homeWin = match.getHomeGoals() > match.getAwayGoals();

                setResultParameters(statement, homeWin, draw,
                        match.getHomeGoals(), match.getAwayGoals(),
                        match.getCompetition().getId(), match.getHomeTeam().getId());
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Home standing does not exist.");
                }

                setResultParameters(statement, !homeWin && !draw, draw,
                        match.getAwayGoals(), match.getHomeGoals(),
                        match.getCompetition().getId(), match.getAwayTeam().getId());
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Away standing does not exist.");
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not apply match result.", e);
        }
    }

    private void setResultParameters(
            PreparedStatement statement, boolean win, boolean draw,
            int goalsFor, int goalsAgainst, long competitionId, long teamId
    ) throws SQLException {
        statement.setInt(1, win ? 1 : 0);
        statement.setInt(2, draw ? 1 : 0);
        statement.setInt(3, !win && !draw ? 1 : 0);
        statement.setInt(4, goalsFor);
        statement.setInt(5, goalsAgainst);
        statement.setInt(6, win ? 3 : draw ? 1 : 0);
        statement.setLong(7, competitionId);
        statement.setLong(8, teamId);
    }
}
