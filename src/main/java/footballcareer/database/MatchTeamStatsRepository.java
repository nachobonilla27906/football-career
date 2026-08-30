package footballcareer.database;

import footballcareer.model.Match;
import footballcareer.model.MatchTeamStats;
import footballcareer.model.Team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MatchTeamStatsRepository {
    public void save(MatchTeamStats stats) {
        Long careerId = CareerContext.getCareerId();
        String sql = careerId == null ? """
                INSERT INTO match_team_stats
                    (match_id, team_id, possession, shots, shots_on_target,
                     corners, fouls, yellow_cards, red_cards, expected_goals,
                     passes, pass_accuracy, tackles)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """ : """
                INSERT INTO career_match_team_stats
                    (career_id, match_id, team_id, possession, shots, shots_on_target,
                     corners, fouls, yellow_cards, red_cards, expected_goals,
                     passes, pass_accuracy, tackles)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int offset = 0;
            if (careerId != null) statement.setLong(++offset, careerId);
            statement.setLong(++offset, stats.getMatch().getId());
            statement.setLong(++offset, stats.getTeam().getId());
            statement.setInt(++offset, stats.getPossession());
            statement.setInt(++offset, stats.getShots());
            statement.setInt(++offset, stats.getShotsOnTarget());
            statement.setInt(++offset, stats.getCorners());
            statement.setInt(++offset, stats.getFouls());
            statement.setInt(++offset, stats.getYellowCards());
            statement.setInt(++offset, stats.getRedCards());
            statement.setDouble(++offset, stats.getExpectedGoals());
            statement.setInt(++offset, stats.getPasses());
            statement.setInt(++offset, stats.getPassAccuracy());
            statement.setInt(++offset, stats.getTackles());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not save match team statistics.", e);
        }
    }

    public List<MatchTeamStats> findByMatch(long matchId) {
        Long careerId = CareerContext.getCareerId();
        String sql = careerId == null
                ? "SELECT * FROM match_team_stats WHERE match_id = ? ORDER BY team_id"
                : "SELECT * FROM career_match_team_stats WHERE career_id = ? AND match_id = ? ORDER BY team_id";
        List<MatchTeamStats> result = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (careerId == null) statement.setLong(1, matchId);
            else { statement.setLong(1, careerId); statement.setLong(2, matchId); }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Could not find match team statistics.", e);
        }
    }

    public MatchTeamStats find(long matchId, long teamId) {
        return findByMatch(matchId).stream()
                .filter(stats -> stats.getTeam().getId() == teamId)
                .findFirst().orElse(null);
    }

    private MatchTeamStats map(ResultSet rs) throws SQLException {
        MatchTeamStats stats = new MatchTeamStats();
        Match match = new Match(); match.setId(rs.getLong("match_id"));
        Team team = new Team(); team.setId(rs.getLong("team_id"));
        stats.setMatch(match); stats.setTeam(team);
        stats.setPossession(rs.getInt("possession"));
        stats.setShots(rs.getInt("shots"));
        stats.setShotsOnTarget(rs.getInt("shots_on_target"));
        stats.setCorners(rs.getInt("corners"));
        stats.setFouls(rs.getInt("fouls"));
        stats.setYellowCards(rs.getInt("yellow_cards"));
        stats.setRedCards(rs.getInt("red_cards"));
        stats.setExpectedGoals(rs.getDouble("expected_goals"));
        stats.setPasses(rs.getInt("passes"));
        stats.setPassAccuracy(rs.getInt("pass_accuracy"));
        stats.setTackles(rs.getInt("tackles"));
        return stats;
    }
}
