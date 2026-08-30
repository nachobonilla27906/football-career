package footballcareer.database;

import footballcareer.model.*;
import footballcareer.model.enums.TransferOfferStatus;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransferOfferRepository {
    public void save(TransferOffer offer) {
        String sql = """
                INSERT INTO transfer_offers
                (career_id, player_id, buying_team_id, selling_team_id, amount, offer_date, status,
                 upfront_percent, appearance_bonus)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            Long careerId = CareerContext.getCareerId();
            if (careerId == null) statement.setNull(1, Types.INTEGER);
            else statement.setLong(1, careerId);
            statement.setLong(2, offer.getPlayer().getId());
            statement.setLong(3, offer.getBuyingTeam().getId());
            statement.setLong(4, offer.getSellingTeam().getId());
            statement.setDouble(5, offer.getAmount());
            statement.setString(6, offer.getOfferDate().toString());
            statement.setString(7, offer.getStatus().name());
            statement.setInt(8, offer.getUpfrontPercent());
            statement.setDouble(9, offer.getAppearanceBonus());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) offer.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not save transfer offer.", e);
        }
    }

    public TransferOffer findById(long id) {
        String sql = "SELECT * FROM transfer_offers WHERE id = ? AND " + careerScope();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find transfer offer.", e);
        }
    }

    public void updateStatus(long offerId, TransferOfferStatus status) {
        String sql = "UPDATE transfer_offers SET status = ? WHERE id = ? AND status = 'PENDING' AND " + careerScope();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setLong(2, offerId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("Offer is not pending.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not update offer status.", e);
        }
    }

    public void setCounterOffer(long offerId, double counterAmount) {
        String sql = """
                UPDATE transfer_offers SET counter_amount = ?
                WHERE id = ? AND status = 'PENDING' AND %s
                """.formatted(careerScope());
        updateExactlyOne(sql, counterAmount, offerId,
                "Offer is not available for a counteroffer.");
    }

    public void reviseBuyerAmount(long offerId, double amount) {
        String sql = """
                UPDATE transfer_offers SET amount = ?, counter_amount = NULL
                WHERE id = ? AND status = 'PENDING' AND counter_amount IS NOT NULL AND %s
                """.formatted(careerScope());
        updateExactlyOne(sql, amount, offerId, "Offer is not available for revision.");
    }

    public void saveRound(long offerId, int round, String proposedBy,
            double amount, LocalDate date) {
        String sql = """
                INSERT INTO transfer_negotiation_rounds
                    (career_id, offer_id, round_number, proposed_by, amount, created_date)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            Long careerId = CareerContext.getCareerId();
            if (careerId == null) statement.setNull(1, Types.INTEGER);
            else statement.setLong(1, careerId);
            statement.setLong(2, offerId); statement.setInt(3, round);
            statement.setString(4, proposedBy); statement.setDouble(5, amount);
            statement.setString(6, date.toString()); statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save negotiation round.", exception);
        }
    }

    public int countBuyerRounds(long offerId) {
        String sql = "SELECT COUNT(*) FROM transfer_negotiation_rounds "
                + "WHERE offer_id = ? AND proposed_by = 'BUYER' AND " + careerScope();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, offerId);
            try (ResultSet rows = statement.executeQuery()) { return rows.next() ? rows.getInt(1) : 0; }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not count negotiation rounds.", exception);
        }
    }

    public void acceptCounterOffer(long offerId) {
        String sql = """
                UPDATE transfer_offers SET amount = counter_amount,
                    counter_amount = NULL, status = 'ACCEPTED'
                WHERE id = ? AND status = 'PENDING' AND counter_amount IS NOT NULL AND %s
                """.formatted(careerScope());
        updateExactlyOne(sql, offerId, "Counteroffer is not available.");
    }

    public List<TransferOffer> findPendingBySellingTeam(long teamId) {
        String sql = """
                SELECT id FROM transfer_offers
                WHERE selling_team_id = ? AND status = 'PENDING' AND %s
                ORDER BY offer_date DESC, id DESC
                """.formatted(careerScope());
        List<TransferOffer> offers = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teamId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) offers.add(findById(rs.getLong("id")));
            }
            return offers;
        } catch (SQLException e) {
            throw new RuntimeException("Could not find incoming offers.", e);
        }
    }

    public int countPendingBySellingTeam(long teamId) {
        String sql = "SELECT COUNT(*) FROM transfer_offers WHERE selling_team_id = ? "
                + "AND status = 'PENDING' AND " + careerScope();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teamId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not count incoming offers.", e);
        }
    }

    public List<TransferOffer> findByBuyingTeam(long teamId) {
        return findForTeam("buying_team_id = ?", teamId);
    }

    public List<TransferOffer> findHistoryByTeam(long teamId) {
        return findForTeam("(buying_team_id = ? OR selling_team_id = ?)", teamId, teamId);
    }

    public void withdraw(long offerId, long buyingTeamId, String reason) {
        String sql = """
                UPDATE transfer_offers SET status = 'WITHDRAWN', resolution_reason = ?
                WHERE id = ? AND buying_team_id = ? AND status = 'PENDING' AND %s
                """.formatted(careerScope());
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reason);
            statement.setLong(2, offerId);
            statement.setLong(3, buyingTeamId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("Offer cannot be cancelled.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not cancel transfer offer.", e);
        }
    }

    public int expirePending(LocalDate currentDate) {
        String sql = """
                UPDATE transfer_offers SET status = 'WITHDRAWN', resolution_reason = 'EXPIRED'
                WHERE status = 'PENDING' AND date(offer_date, '+7 days') < date(?) AND %s
                """.formatted(careerScope());
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, currentDate.toString());
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not expire transfer offers.", e);
        }
    }

    public boolean hasPendingOfferForPlayer(long playerId) {
        String sql = "SELECT 1 FROM transfer_offers WHERE player_id = ? AND status = 'PENDING' AND "
                + careerScope() + " LIMIT 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, playerId);
            try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
        } catch (SQLException e) {
            throw new RuntimeException("Could not check pending player offers.", e);
        }
    }

    private String careerScope() {
        Long careerId = CareerContext.getCareerId();
        return careerId == null ? "career_id IS NULL" : "career_id = " + careerId;
    }

    private List<TransferOffer> findForTeam(String condition, long... teamIds) {
        String sql = "SELECT * FROM transfer_offers WHERE " + condition + " AND "
                + careerScope() + " ORDER BY offer_date DESC, id DESC";
        List<TransferOffer> offers = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < teamIds.length; index++) {
                statement.setLong(index + 1, teamIds[index]);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) offers.add(map(rs));
            }
            return offers;
        } catch (SQLException e) {
            throw new RuntimeException("Could not find transfer offer history.", e);
        }
    }

    private TransferOffer map(ResultSet rs) throws SQLException {
        TransferOffer offer = new TransferOffer();
        offer.setId(rs.getLong("id"));
        Player player = new Player(); player.setId(rs.getLong("player_id"));
        Team buyer = new Team(); buyer.setId(rs.getLong("buying_team_id"));
        Team seller = new Team(); seller.setId(rs.getLong("selling_team_id"));
        offer.setPlayer(player); offer.setBuyingTeam(buyer); offer.setSellingTeam(seller);
        offer.setAmount(rs.getDouble("amount"));
        offer.setOfferDate(LocalDate.parse(rs.getString("offer_date")));
        offer.setStatus(TransferOfferStatus.valueOf(rs.getString("status")));
        offer.setCounterAmount((Double) rs.getObject("counter_amount"));
        offer.setResolutionReason(rs.getString("resolution_reason"));
        offer.setUpfrontPercent(rs.getInt("upfront_percent"));
        offer.setAppearanceBonus(rs.getDouble("appearance_bonus"));
        return offer;
    }

    private void updateExactlyOne(String sql, Object value, long offerId, String message) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, value); statement.setLong(2, offerId);
            if (statement.executeUpdate() != 1) throw new IllegalStateException(message);
        } catch (SQLException e) {
            throw new RuntimeException("Could not update transfer offer.", e);
        }
    }

    private void updateExactlyOne(String sql, long offerId, String message) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, offerId);
            if (statement.executeUpdate() != 1) throw new IllegalStateException(message);
        } catch (SQLException e) {
            throw new RuntimeException("Could not update transfer offer.", e);
        }
    }
}
