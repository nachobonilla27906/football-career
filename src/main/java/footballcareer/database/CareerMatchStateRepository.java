package footballcareer.database;

import footballcareer.model.Career;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CareerMatchStateRepository {

    public void initialize(Career career, boolean newCareer) {
        initializeRoster(career);
        String sql = """
                INSERT OR IGNORE INTO career_match_states
                    (career_id, match_id, home_goals, away_goals, played)
                SELECT ?, m.id,
                       CASE WHEN ? = 0 AND m.played = 1 AND m.date <= ? THEN m.home_goals ELSE 0 END,
                       CASE WHEN ? = 0 AND m.played = 1 AND m.date <= ? THEN m.away_goals ELSE 0 END,
                       CASE WHEN ? = 0 AND m.played = 1 AND m.date <= ? THEN 1 ELSE 0 END
                FROM matches m
                JOIN competitions c ON c.id = m.competition_id
                WHERE c.season_id = ?
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, career.getId());
            int existing = newCareer ? 1 : 0;
            statement.setInt(2, existing);
            statement.setString(3, career.getCurrentDate().toString());
            statement.setInt(4, existing);
            statement.setString(5, career.getCurrentDate().toString());
            statement.setInt(6, existing);
            statement.setString(7, career.getCurrentDate().toString());
            statement.setLong(8, career.getCurrentSeason().getId());
            statement.executeUpdate();
            if (!newCareer) migrateLegacyMatchDetails(connection, career.getId());
        } catch (SQLException exception) {
            throw new RuntimeException("Could not initialize career match state.", exception);
        }
    }

    private void initializeRoster(Career career) {
        long careerId = career.getId();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT OR IGNORE INTO career_player_team
                         (career_id, player_id, team_id, start_date)
                     SELECT ?, player_id, team_id, start_date FROM initial_player_team
                     """)) {
            statement.setLong(1, careerId);
            statement.executeUpdate();
            try (PreparedStatement market = connection.prepareStatement("""
                    INSERT OR IGNORE INTO career_player_market_status (career_id, player_id)
                    SELECT ?, id FROM players
                    """)) {
                market.setLong(1, careerId);
                market.executeUpdate();
            }
            try (PreparedStatement states = connection.prepareStatement("""
                    INSERT OR IGNORE INTO career_player_state
                        (career_id, player_id, form, morale, fitness)
                    SELECT ?, player_id, form, morale, fitness FROM player_state
                    """)) {
                states.setLong(1, careerId);
                states.executeUpdate();
            }
            try (PreparedStatement development = connection.prepareStatement("""
                    INSERT OR IGNORE INTO career_player_development
                        (career_id, player_id, overall, pace, shooting, passing,
                         dribbling, defending, physical)
                    SELECT ?, id, overall, pace, shooting, passing,
                           dribbling, defending, physical FROM players
                    """)) {
                development.setLong(1, careerId);
                development.executeUpdate();
            }
            try (PreparedStatement history = connection.prepareStatement("""
                    INSERT OR IGNORE INTO career_player_progress_history
                        (career_id, player_id, snapshot_date, overall, market_value)
                    SELECT ?, id, ?, overall, market_value FROM players
                    """)) {
                history.setLong(1, careerId);
                history.setString(2, career.getCurrentDate().toString());
                history.executeUpdate();
            }
            try (PreparedStatement seasonStats = connection.prepareStatement("""
                    INSERT OR IGNORE INTO career_player_season_stats
                        (career_id, player_id, season_id, team_id, appearances, starts,
                         minutes, goals, assists, yellow_cards, red_cards, average_rating)
                    SELECT ?, player_id, season_id, team_id, appearances, starts,
                           minutes, goals, assists, yellow_cards, red_cards, average_rating
                    FROM player_season_stats
                    """)) {
                seasonStats.setLong(1, careerId);
                seasonStats.executeUpdate();
            }
            try (PreparedStatement missingStats = connection.prepareStatement("""
                    INSERT OR IGNORE INTO career_player_season_stats
                        (career_id, player_id, season_id, team_id)
                    SELECT ?, pt.player_id, ?, pt.team_id
                    FROM career_player_team pt
                    WHERE pt.career_id = ? AND pt.end_date IS NULL
                    """)) {
                missingStats.setLong(1, careerId);
                missingStats.setLong(2, career.getCurrentSeason().getId());
                missingStats.setLong(3, careerId);
                missingStats.executeUpdate();
            }
            try (PreparedStatement contracts = connection.prepareStatement("""
                    INSERT OR IGNORE INTO career_contracts
                        (career_id, player_id, team_id, start_date, end_date, salary, active)
                    SELECT ?, player_id, team_id, start_date, end_date, salary, active FROM contracts
                    """)) {
                contracts.setLong(1, careerId);
                contracts.executeUpdate();
            }
            try (PreparedStatement missingContracts = connection.prepareStatement("""
                    INSERT OR IGNORE INTO career_contracts
                        (career_id, player_id, team_id, start_date, end_date, salary, active)
                    SELECT ?, pt.player_id, pt.team_id, ?, ?, p.salary, 1
                    FROM career_player_team pt
                    JOIN players p ON p.id = pt.player_id
                    WHERE pt.career_id = ? AND pt.end_date IS NULL
                      AND NOT EXISTS (
                          SELECT 1 FROM career_contracts c
                          WHERE c.career_id = ? AND c.player_id = pt.player_id
                            AND c.active = 1
                      )
                    """)) {
                missingContracts.setLong(1, careerId);
                missingContracts.setString(2, career.getCurrentSeason().getStartDate().toString());
                missingContracts.setString(3, career.getCurrentSeason().getEndDate()
                        .plusYears(3).toString());
                missingContracts.setLong(4, careerId);
                missingContracts.setLong(5, careerId);
                missingContracts.executeUpdate();
            }
            try (PreparedStatement finances = connection.prepareStatement("""
                    INSERT OR IGNORE INTO career_club_finances
                        (career_id, team_id, transfer_budget, wage_budget,
                         current_wage_spend, balance)
                    SELECT ?, team_id, transfer_budget, wage_budget,
                           current_wage_spend, balance FROM club_finances
                    """)) {
                finances.setLong(1, careerId);
                finances.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not initialize career squads.", exception);
        }
    }

    private void migrateLegacyMatchDetails(Connection connection, long careerId)
            throws SQLException {
        String[] migrations = {
                """
                INSERT INTO career_match_events
                    (career_id, match_id, team_id, player_id, secondary_player_id, minute, type)
                SELECT ?, e.match_id, e.team_id, e.player_id, e.secondary_player_id, e.minute, e.type
                FROM match_events e JOIN career_match_states s ON s.match_id = e.match_id
                WHERE s.career_id = ? AND s.played = 1
                """,
                """
                INSERT OR IGNORE INTO career_match_team_stats
                    (career_id, match_id, team_id, possession, shots, shots_on_target,
                     corners, fouls, yellow_cards, red_cards)
                SELECT ?, x.match_id, x.team_id, x.possession, x.shots, x.shots_on_target,
                       x.corners, x.fouls, x.yellow_cards, x.red_cards
                FROM match_team_stats x JOIN career_match_states s ON s.match_id = x.match_id
                WHERE s.career_id = ? AND s.played = 1
                """,
                """
                INSERT OR IGNORE INTO career_match_lineups
                    (career_id, match_id, team_id, player_id, role, position_order)
                SELECT ?, x.match_id, x.team_id, x.player_id, x.role, x.position_order
                FROM match_lineups x JOIN career_match_states s ON s.match_id = x.match_id
                WHERE s.career_id = ? AND s.played = 1
                """,
                """
                INSERT OR IGNORE INTO career_match_tactics
                    (career_id, match_id, team_id, formation)
                SELECT ?, x.match_id, x.team_id, x.formation
                FROM match_tactics x JOIN career_match_states s ON s.match_id = x.match_id
                WHERE s.career_id = ? AND s.played = 1
                """
        };
        for (String sql : migrations) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, careerId);
                statement.setLong(2, careerId);
                statement.executeUpdate();
            }
        }
    }

}
