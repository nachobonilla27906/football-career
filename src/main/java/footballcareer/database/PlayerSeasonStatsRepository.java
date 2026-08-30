package footballcareer.database;

import footballcareer.model.*;

import java.sql.*;

public class PlayerSeasonStatsRepository {
    public void initializeForSeason(long seasonId) {
        Long careerId = CareerContext.getCareerId();
        String sql = careerId == null ? """
                INSERT INTO player_season_stats (player_id, season_id, team_id)
                SELECT pt.player_id, ?, pt.team_id
                FROM player_team pt
                WHERE pt.end_date IS NULL
                  AND NOT EXISTS (
                    SELECT 1 FROM player_season_stats ps
                    WHERE ps.player_id = pt.player_id AND ps.season_id = ?
                  )
                """ : """
                INSERT INTO career_player_season_stats
                    (career_id, player_id, season_id, team_id)
                SELECT ?, pt.player_id, ?, pt.team_id
                FROM career_player_team pt
                WHERE pt.career_id = ? AND pt.end_date IS NULL
                  AND NOT EXISTS (
                    SELECT 1 FROM career_player_season_stats ps
                    WHERE ps.career_id = ? AND ps.player_id = pt.player_id AND ps.season_id = ?
                  )
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (careerId == null) {
                statement.setLong(1, seasonId); statement.setLong(2, seasonId);
            } else {
                statement.setLong(1, careerId); statement.setLong(2, seasonId);
                statement.setLong(3, careerId); statement.setLong(4, careerId);
                statement.setLong(5, seasonId);
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not initialize player stats.", e);
        }
    }

    public PlayerSeasonStats find(long playerId, long seasonId) {
        Long careerId = CareerContext.getCareerId();
        String table = careerId == null ? "player_season_stats" : "career_player_season_stats";
        String sql = "SELECT * FROM " + table + " WHERE player_id = ? AND season_id = ?"
                + (careerId == null ? "" : " AND career_id = " + careerId);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, playerId);
            statement.setLong(2, seasonId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return null;
                PlayerSeasonStats stats = new PlayerSeasonStats();
                stats.setId(rs.getLong("id"));
                Player player = new Player(); player.setId(playerId);
                Season season = new Season(); season.setId(seasonId);
                Team team = new Team(); team.setId(rs.getLong("team_id"));
                stats.setPlayer(player); stats.setSeason(season); stats.setTeam(team);
                stats.setAppearances(rs.getInt("appearances"));
                stats.setStarts(rs.getInt("starts"));
                stats.setMinutes(rs.getInt("minutes"));
                stats.setGoals(rs.getInt("goals"));
                stats.setAssists(rs.getInt("assists"));
                stats.setYellowCards(rs.getInt("yellow_cards"));
                stats.setRedCards(rs.getInt("red_cards"));
                stats.setAverageRating(rs.getDouble("average_rating"));
                return stats;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find player stats.", e);
        }
    }

    public void recordAppearance(
            long playerId, long seasonId, boolean started, int minutes,
            int goals, int assists, int yellowCards, int redCards, double rating
    ) {
        Long careerId = CareerContext.getCareerId();
        String table = careerId == null ? "player_season_stats" : "career_player_season_stats";
        String sql = """
                UPDATE %s SET
                    average_rating = ((average_rating * appearances) + ?) / (appearances + 1),
                    appearances = appearances + 1,
                    starts = starts + ?, minutes = minutes + ?,
                    goals = goals + ?, assists = assists + ?,
                    yellow_cards = yellow_cards + ?, red_cards = red_cards + ?
                WHERE player_id = ? AND season_id = ? %s
                """.formatted(table, careerId == null ? "" : "AND career_id = " + careerId);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, Math.max(0, Math.min(10, rating)));
            statement.setInt(2, started ? 1 : 0);
            statement.setInt(3, Math.max(0, minutes));
            statement.setInt(4, Math.max(0, goals));
            statement.setInt(5, Math.max(0, assists));
            statement.setInt(6, Math.max(0, yellowCards));
            statement.setInt(7, Math.max(0, redCards));
            statement.setLong(8, playerId);
            statement.setLong(9, seasonId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Player season stats do not exist.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not record appearance.", e);
        }
    }
}
