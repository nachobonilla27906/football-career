package footballcareer.database;

import footballcareer.model.Career;
import footballcareer.model.Season;
import footballcareer.model.Team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CareerRepository {

    public List<Career> findAll() {
        List<Career> careers = new ArrayList<>();
        String sql = "SELECT id FROM careers ORDER BY id DESC";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Career career = findById(resultSet.getLong("id"));
                if (career != null) careers.add(career);
            }
            return careers;
        } catch (SQLException e) {
            throw new RuntimeException("Could not find careers.", e);
        }
    }

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

    public void updateSeasonAndDate(Career career) {
        String sql = """
                UPDATE careers SET current_season_id = ?, current_date = ?
                WHERE id = ?
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, career.getCurrentSeason().getId());
            statement.setString(2, career.getCurrentDate().toString());
            statement.setLong(3, career.getId());
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Career does not exist.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not update career season.", e);
        }
    }

    public void rename(long careerId, String managerName) {
        if (managerName == null || managerName.isBlank()) {
            throw new IllegalArgumentException("Manager name is required.");
        }
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE careers SET manager_name = ? WHERE id = ?")) {
            statement.setString(1, managerName.trim());
            statement.setLong(2, careerId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Career does not exist.");
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not rename career.", exception);
        }
    }

    public long duplicate(long careerId) {
        String[] scopedTables = {
                "career_player_team", "career_match_states", "career_match_events",
                "career_match_team_stats", "career_match_lineups", "career_match_tactics",
                "career_match_roles",
                "career_team_sheets", "career_team_sheet_players",
                "career_shortlist", "training_sessions", "career_contracts",
                "career_player_state", "career_player_development", "career_club_finances",
                "career_player_progress_history",
                "career_player_market_status", "career_preferences", "medical_treatments",
                "player_conversations",
                "career_player_season_stats", "career_loans", "career_scouts",
                "career_youth_candidates"
                , "career_staff", "career_manager_reputation"
        };
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long duplicateId;
                try (PreparedStatement copy = connection.prepareStatement("""
                        INSERT INTO careers
                            (manager_name, controlled_team_id, current_season_id, current_date)
                        SELECT manager_name || ' (copia)', controlled_team_id,
                               current_season_id, current_date
                        FROM careers WHERE id = ?
                        """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    copy.setLong(1, careerId);
                    if (copy.executeUpdate() != 1) {
                        throw new IllegalArgumentException("Career does not exist.");
                    }
                    try (ResultSet keys = copy.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Missing duplicated career id.");
                        duplicateId = keys.getLong(1);
                    }
                }
                for (String table : scopedTables) {
                    copyScopedTable(connection, table, careerId, duplicateId);
                }
                java.util.Map<Long, Long> offerIds = copyOffers(
                        connection, careerId, duplicateId);
                copyNegotiationRounds(connection, careerId, duplicateId, offerIds);
                copyTransfers(connection, careerId, duplicateId, offerIds);
                connection.commit();
                return duplicateId;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not duplicate career.", exception);
        }
    }

    private void copyScopedTable(Connection connection, String table,
            long sourceCareerId, long targetCareerId) throws SQLException {
        java.util.List<String> columns = new java.util.ArrayList<>();
        try (java.sql.Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                String column = rs.getString("name");
                if (!"id".equals(column)) columns.add(column);
            }
        }
        if (!columns.contains("career_id")) return;
        String names = String.join(", ", columns);
        String values = columns.stream().map(column -> "career_id".equals(column)
                ? "?" : column).collect(java.util.stream.Collectors.joining(", "));
        String sql = "INSERT INTO " + table + " (" + names + ") SELECT " + values
                + " FROM " + table + " WHERE career_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, targetCareerId);
            statement.setLong(2, sourceCareerId);
            statement.executeUpdate();
        }
    }

    private java.util.Map<Long, Long> copyOffers(Connection connection,
            long sourceCareerId, long targetCareerId) throws SQLException {
        java.util.Map<Long, Long> ids = new java.util.HashMap<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT * FROM transfer_offers WHERE career_id = ? ORDER BY id");
             PreparedStatement insert = connection.prepareStatement("""
                     INSERT INTO transfer_offers
                         (career_id, player_id, buying_team_id, selling_team_id, amount,
                          offer_date, status, counter_amount, resolution_reason)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            select.setLong(1, sourceCareerId);
            try (ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    insert.setLong(1, targetCareerId);
                    insert.setLong(2, rs.getLong("player_id"));
                    insert.setLong(3, rs.getLong("buying_team_id"));
                    insert.setLong(4, rs.getLong("selling_team_id"));
                    insert.setDouble(5, rs.getDouble("amount"));
                    insert.setString(6, rs.getString("offer_date"));
                    insert.setString(7, rs.getString("status"));
                    insert.setObject(8, rs.getObject("counter_amount"));
                    insert.setString(9, rs.getString("resolution_reason"));
                    insert.executeUpdate();
                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        if (keys.next()) ids.put(rs.getLong("id"), keys.getLong(1));
                    }
                }
            }
        }
        return ids;
    }

    private void copyNegotiationRounds(Connection connection, long sourceCareerId,
            long targetCareerId, java.util.Map<Long, Long> offerIds) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement("""
                SELECT * FROM transfer_negotiation_rounds
                WHERE career_id = ? ORDER BY id
                """); PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO transfer_negotiation_rounds
                    (career_id, offer_id, round_number, proposed_by, amount, created_date)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            select.setLong(1, sourceCareerId);
            try (ResultSet rows = select.executeQuery()) {
                while (rows.next()) {
                    Long newOfferId = offerIds.get(rows.getLong("offer_id"));
                    if (newOfferId == null) continue;
                    insert.setLong(1, targetCareerId); insert.setLong(2, newOfferId);
                    insert.setInt(3, rows.getInt("round_number"));
                    insert.setString(4, rows.getString("proposed_by"));
                    insert.setDouble(5, rows.getDouble("amount"));
                    insert.setString(6, rows.getString("created_date")); insert.addBatch();
                }
                insert.executeBatch();
            }
        }
    }

    private void copyTransfers(Connection connection, long sourceCareerId,
            long targetCareerId, java.util.Map<Long, Long> offerIds) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT * FROM transfers WHERE career_id = ? ORDER BY id");
             PreparedStatement insert = connection.prepareStatement("""
                     INSERT INTO transfers
                         (career_id, player_id, from_team_id, to_team_id, amount,
                          transfer_date, season_id, offer_id)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {
            select.setLong(1, sourceCareerId);
            try (ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    insert.setLong(1, targetCareerId);
                    insert.setLong(2, rs.getLong("player_id"));
                    insert.setLong(3, rs.getLong("from_team_id"));
                    insert.setLong(4, rs.getLong("to_team_id"));
                    insert.setDouble(5, rs.getDouble("amount"));
                    insert.setString(6, rs.getString("transfer_date"));
                    insert.setLong(7, rs.getLong("season_id"));
                    long oldOfferId = rs.getLong("offer_id");
                    Long newOfferId = rs.wasNull() ? null : offerIds.get(oldOfferId);
                    if (newOfferId == null) {
                        throw new SQLException("Transfer has no duplicated offer: " + oldOfferId);
                    }
                    insert.setLong(8, newOfferId);
                    insert.addBatch();
                }
                insert.executeBatch();
            }
        }
    }

    public void delete(long careerId) {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement shortlist = connection.prepareStatement(
                    "DELETE FROM career_shortlist WHERE career_id = ?");
                 PreparedStatement matchStates = connection.prepareStatement(
                         "DELETE FROM career_match_states WHERE career_id = ?");
                 PreparedStatement matchEvents = connection.prepareStatement(
                         "DELETE FROM career_match_events WHERE career_id = ?");
                 PreparedStatement matchStats = connection.prepareStatement(
                         "DELETE FROM career_match_team_stats WHERE career_id = ?");
                 PreparedStatement matchLineups = connection.prepareStatement(
                         "DELETE FROM career_match_lineups WHERE career_id = ?");
                 PreparedStatement matchTactics = connection.prepareStatement(
                         "DELETE FROM career_match_tactics WHERE career_id = ?");
                 PreparedStatement training = connection.prepareStatement(
                         "DELETE FROM training_sessions WHERE career_id = ?");
                 PreparedStatement transfers = connection.prepareStatement(
                         "DELETE FROM transfers WHERE career_id = ?");
                 PreparedStatement offers = connection.prepareStatement(
                         "DELETE FROM transfer_offers WHERE career_id = ?");
                 PreparedStatement squads = connection.prepareStatement(
                         "DELETE FROM career_player_team WHERE career_id = ?");
                 PreparedStatement marketStatus = connection.prepareStatement(
                         "DELETE FROM career_player_market_status WHERE career_id = ?");
                 PreparedStatement playerStates = connection.prepareStatement(
                         "DELETE FROM career_player_state WHERE career_id = ?");
                 PreparedStatement development = connection.prepareStatement(
                         "DELETE FROM career_player_development WHERE career_id = ?");
                 PreparedStatement seasonStats = connection.prepareStatement(
                         "DELETE FROM career_player_season_stats WHERE career_id = ?");
                 PreparedStatement careerContracts = connection.prepareStatement(
                         "DELETE FROM career_contracts WHERE career_id = ?");
                 PreparedStatement careerFinances = connection.prepareStatement(
                         "DELETE FROM career_club_finances WHERE career_id = ?");
                 PreparedStatement career = connection.prepareStatement(
                         "DELETE FROM careers WHERE id = ?")) {
                shortlist.setLong(1, careerId);
                shortlist.executeUpdate();
                matchStates.setLong(1, careerId);
                matchStates.executeUpdate();
                matchEvents.setLong(1, careerId); matchEvents.executeUpdate();
                matchStats.setLong(1, careerId); matchStats.executeUpdate();
                matchLineups.setLong(1, careerId); matchLineups.executeUpdate();
                matchTactics.setLong(1, careerId); matchTactics.executeUpdate();
                training.setLong(1, careerId);
                training.executeUpdate();
                transfers.setLong(1, careerId); transfers.executeUpdate();
                offers.setLong(1, careerId); offers.executeUpdate();
                squads.setLong(1, careerId); squads.executeUpdate();
                marketStatus.setLong(1, careerId); marketStatus.executeUpdate();
                playerStates.setLong(1, careerId); playerStates.executeUpdate();
                development.setLong(1, careerId); development.executeUpdate();
                seasonStats.setLong(1, careerId); seasonStats.executeUpdate();
                careerContracts.setLong(1, careerId); careerContracts.executeUpdate();
                careerFinances.setLong(1, careerId); careerFinances.executeUpdate();
                career.setLong(1, careerId);
                if (career.executeUpdate() != 1) {
                    throw new IllegalArgumentException("Career does not exist.");
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not delete career.", exception);
        }
    }
}
