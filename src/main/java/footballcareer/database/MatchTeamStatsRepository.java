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
        String sql = """
                INSERT INTO match_team_stats
                    (match_id, team_id, possession, shots, shots_on_target,
                     corners, fouls, yellow_cards, red_cards)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, stats.getMatch().getId());
            statement.setLong(2, stats.getTeam().getId());
            statement.setInt(3, stats.getPossession());
            statement.setInt(4, stats.getShots());
            statement.setInt(5, stats.getShotsOnTarget());
            statement.setInt(6, stats.getCorners());
            statement.setInt(7, stats.getFouls());
            statement.setInt(8, stats.getYellowCards());
            statement.setInt(9, stats.getRedCards());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not save match team statistics.", e);
        }
    }

    public List<MatchTeamStats> findByMatch(long matchId) {
        String sql = "SELECT * FROM match_team_stats WHERE match_id = ? ORDER BY team_id";
        List<MatchTeamStats> result = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, matchId);
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
        return stats;
    }
}
