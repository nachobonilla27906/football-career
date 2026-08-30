package footballcareer.service;

import footballcareer.database.Database;
import footballcareer.database.CareerContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class TransferExecutionService {
    public record ContractTerms(double salary, double signingBonus,
                                Double releaseClause, String squadRole) {}
    private final TransferWindowService transferWindowService;

    public TransferExecutionService() {
        this(new TransferWindowService());
    }

    TransferExecutionService(TransferWindowService transferWindowService) {
        this.transferWindowService = transferWindowService;
    }

    public void completeTransfer(long offerId, double newSalary,
            LocalDate contractEndDate, long seasonId, LocalDate transferDate) {
        completeTransfer(offerId, new ContractTerms(newSalary, 0, null, "ROTATION"),
                contractEndDate, seasonId, transferDate);
    }

    public void completeTransfer(long offerId, ContractTerms terms,
            LocalDate contractEndDate, long seasonId, LocalDate transferDate) {
        validateTerms(terms);
        if (!contractEndDate.isAfter(transferDate)) {
            throw new IllegalArgumentException("Contract must end after the transfer date.");
        }
        transferWindowService.requireOpen(transferDate);

        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Long careerId = CareerContext.getCareerId();
                String membership = careerId == null ? "player_team" : "career_player_team";
                String membershipScope = careerId == null ? "" : " AND career_id = " + careerId;
                String contracts = careerId == null ? "contracts" : "career_contracts";
                String finances = careerId == null ? "club_finances" : "career_club_finances";
                String careerScope = careerId == null ? "" : " AND career_id = " + careerId;
                OfferData offer = loadAcceptedOffer(connection, offerId);
                requireMedicalClearance(offer.playerId(), transferDate);
                if (terms.releaseClause() != null && terms.releaseClause() < offer.amount()) {
                    throw new IllegalArgumentException(
                            "Release clause cannot be lower than the transfer fee.");
                }
                verifyPlayerStillBelongsToSeller(connection, offer, membership, membershipScope);
                verifyBuyerCanAffordTransfer(connection, offer, terms.salary(),
                        terms.signingBonus() + offer.appearanceBonus(), finances, careerScope);
                double oldSalary = findActiveSalary(connection, offer.playerId(), offer.sellerId(),
                        contracts, careerScope);

                update(connection, """
                        UPDATE %s SET active = 0, end_date = ?
                        WHERE player_id = ? AND team_id = ? AND active = 1%s
                        """.formatted(contracts, careerScope), transferDate.toString(),
                        offer.playerId(), offer.sellerId());
                update(connection, """
                        UPDATE %s SET end_date = ?
                        WHERE player_id = ? AND team_id = ? AND end_date IS NULL%s
                        """.formatted(membership, membershipScope), transferDate.toString(),
                        offer.playerId(), offer.sellerId());
                if (careerId == null) update(connection, """
                            INSERT INTO player_team (player_id, team_id, start_date)
                            VALUES (?, ?, ?)
                            """, offer.playerId(), offer.buyerId(), transferDate.toString());
                else update(connection, """
                            INSERT INTO career_player_team
                                (career_id, player_id, team_id, start_date)
                            VALUES (?, ?, ?, ?)
                            """, careerId, offer.playerId(), offer.buyerId(), transferDate.toString());
                if (careerId == null) update(connection, """
                        INSERT INTO contracts
                            (player_id, team_id, start_date, end_date, salary,
                             signing_bonus, release_clause, squad_role, active)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)
                        """, offer.playerId(), offer.buyerId(), transferDate.toString(),
                        contractEndDate.toString(), terms.salary(), terms.signingBonus(),
                        terms.releaseClause(), terms.squadRole());
                else update(connection, """
                        INSERT INTO career_contracts
                            (career_id, player_id, team_id, start_date, end_date, salary,
                             signing_bonus, release_clause, squad_role, active)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                        """, careerId, offer.playerId(), offer.buyerId(), transferDate.toString(),
                        contractEndDate.toString(), terms.salary(), terms.signingBonus(),
                        terms.releaseClause(), terms.squadRole());

                updateExactlyOne(connection, """
                        UPDATE %s
                        SET transfer_budget = transfer_budget - ?, balance = balance - ?,
                            current_wage_spend = current_wage_spend + ?
                        WHERE team_id = ? AND transfer_budget >= ?
                          AND wage_budget - current_wage_spend >= ? %s
                        """.formatted(finances, careerScope), "Buyer can no longer afford the transfer.",
                        offer.amount() + terms.signingBonus() + offer.appearanceBonus(),
                        offer.upfrontAmount() + terms.signingBonus(), terms.salary(), offer.buyerId(),
                        offer.amount() + terms.signingBonus() + offer.appearanceBonus(), terms.salary());
                updateExactlyOne(connection, """
                        UPDATE %s
                        SET transfer_budget = transfer_budget + ?, balance = balance + ?,
                            current_wage_spend = MAX(0, current_wage_spend - ?)
                        WHERE team_id = ? %s
                        """.formatted(finances, careerScope), "Seller finances do not exist.",
                        offer.amount(), offer.upfrontAmount(),
                        oldSalary, offer.sellerId());

                if (careerId == null) update(connection, """
                            INSERT INTO player_market_status (player_id, status, asking_price)
                            VALUES (?, 'NOT_LISTED', NULL)
                            ON CONFLICT(player_id) DO UPDATE
                            SET status = 'NOT_LISTED', asking_price = NULL
                            """, offer.playerId());
                else update(connection, """
                            INSERT INTO career_player_market_status
                                (career_id, player_id, status, asking_price)
                            VALUES (?, ?, 'NOT_LISTED', NULL)
                            ON CONFLICT(career_id, player_id) DO UPDATE
                            SET status = 'NOT_LISTED', asking_price = NULL
                            """, careerId, offer.playerId());
                updateExactlyOne(connection, """
                        UPDATE transfer_offers SET status = 'COMPLETED'
                        WHERE id = ? AND status = 'ACCEPTED' AND %s
                        """.formatted(careerScope()), "Offer is no longer accepted.", offerId);
                update(connection, """
                        INSERT INTO transfers
                            (career_id, player_id, from_team_id, to_team_id, amount,
                             transfer_date, season_id, offer_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """, CareerContext.getCareerId(), offer.playerId(), offer.sellerId(), offer.buyerId(), offer.amount(),
                        transferDate.toString(), seasonId, offerId);
                createObligations(connection, careerId, offerId, offer, transferDate);

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not complete transfer.", e);
        }
    }

    private OfferData loadAcceptedOffer(Connection connection, long offerId) throws SQLException {
        String sql = """
                SELECT player_id, buying_team_id, selling_team_id, amount,
                       upfront_percent, appearance_bonus
                FROM transfer_offers WHERE id = ? AND status = 'ACCEPTED' AND %s
                """.formatted(careerScope());
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, offerId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Offer is not accepted.");
                return new OfferData(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getDouble(4),
                        rs.getInt(5), rs.getDouble(6));
            }
        }
    }

    private void verifyPlayerStillBelongsToSeller(Connection connection, OfferData offer,
            String membership, String membershipScope)
            throws SQLException {
        String sql = "SELECT 1 FROM " + membership
                + " WHERE player_id = ? AND team_id = ? AND end_date IS NULL" + membershipScope;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, offer.playerId());
            statement.setLong(2, offer.sellerId());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Player no longer belongs to seller.");
            }
        }
    }

    private void verifyBuyerCanAffordTransfer(Connection connection, OfferData offer, double salary,
            double signingBonus, String finances, String careerScope)
            throws SQLException {
        String sql = """
                SELECT 1 FROM club_finances
                WHERE team_id = ? AND transfer_budget >= ?
                  AND wage_budget - current_wage_spend >= ?
                """.replace("club_finances", finances) + careerScope;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, offer.buyerId());
            statement.setDouble(2, offer.amount() + signingBonus);
            statement.setDouble(3, salary);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Buyer cannot afford transfer and salary.");
            }
        }
    }

    private void validateTerms(ContractTerms terms) {
        if (terms == null || terms.salary() < 0 || terms.signingBonus() < 0) {
            throw new IllegalArgumentException("Salary and signing bonus cannot be negative.");
        }
        if (terms.releaseClause() != null && terms.releaseClause() <= 0) {
            throw new IllegalArgumentException("Release clause must be positive.");
        }
        if (!java.util.Set.of("CRUCIAL", "IMPORTANT", "ROTATION", "PROSPECT")
                .contains(terms.squadRole())) {
            throw new IllegalArgumentException("Invalid squad role.");
        }
    }

    private void requireMedicalClearance(long playerId, LocalDate transferDate) {
        footballcareer.model.PlayerState state =
                new footballcareer.database.PlayerStateRepository().findByPlayer(playerId);
        if (state == null) return;
        if (state.getFitness() < 35 || ("INJURY".equals(state.getUnavailableReason())
                && state.getUnavailableUntil() != null
                && !state.getUnavailableUntil().isBefore(transferDate))) {
            throw new IllegalStateException(
                    "Player failed the medical examination due to an active injury.");
        }
    }

    private double findActiveSalary(Connection connection, long playerId, long teamId,
            String contracts, String careerScope)
            throws SQLException {
        String sql = "SELECT salary FROM " + contracts
                + " WHERE player_id = ? AND team_id = ? AND active = 1" + careerScope;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, playerId);
            statement.setLong(2, teamId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Player has no active contract with seller.");
                return rs.getDouble(1);
            }
        }
    }

    private void update(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            statement.executeUpdate();
        }
    }

    private void updateExactlyOne(Connection connection, String sql, String message,
            Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            if (statement.executeUpdate() != 1) throw new IllegalStateException(message);
        }
    }

    private void bind(PreparedStatement statement, Object... values) throws SQLException {
        for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
    }

    private String careerScope() {
        Long careerId = CareerContext.getCareerId();
        return careerId == null ? "career_id IS NULL" : "career_id = " + careerId;
    }

    private void createObligations(Connection connection, Long careerId, long offerId,
            OfferData offer, LocalDate date) throws SQLException {
        double remaining = offer.amount() - offer.upfrontAmount();
        if (remaining > 0) {
            obligation(connection, careerId, offerId, offer, remaining / 2,
                    date.plusMonths(6), "DATE", 0);
            obligation(connection, careerId, offerId, offer, remaining - remaining / 2,
                    date.plusMonths(12), "DATE", 0);
        }
        if (offer.appearanceBonus() > 0) obligation(connection, careerId, offerId, offer,
                offer.appearanceBonus(), null, "APPEARANCES", 10);
    }

    private void obligation(Connection connection, Long careerId, long offerId, OfferData offer,
            double amount, LocalDate due, String type, int value) throws SQLException {
        update(connection, """
                INSERT INTO transfer_obligations
                    (career_id, offer_id, debtor_team_id, creditor_team_id, player_id,
                     amount, due_date, condition_type, condition_value)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, careerId, offerId, offer.buyerId(), offer.sellerId(), offer.playerId(),
                amount, due == null ? null : due.toString(), type, value);
    }

    private record OfferData(long playerId, long buyerId, long sellerId, double amount,
                             int upfrontPercent, double appearanceBonus) {
        double upfrontAmount() { return amount * upfrontPercent / 100.0; }
    }
}
