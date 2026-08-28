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
                (player_id, buying_team_id, selling_team_id, amount, offer_date, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, offer.getPlayer().getId());
            statement.setLong(2, offer.getBuyingTeam().getId());
            statement.setLong(3, offer.getSellingTeam().getId());
            statement.setDouble(4, offer.getAmount());
            statement.setString(5, offer.getOfferDate().toString());
            statement.setString(6, offer.getStatus().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) offer.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not save transfer offer.", e);
        }
    }

    public TransferOffer findById(long id) {
        String sql = "SELECT * FROM transfer_offers WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return null;
                TransferOffer offer = new TransferOffer();
                offer.setId(id);
                Player player = new Player(); player.setId(rs.getLong("player_id"));
                Team buyer = new Team(); buyer.setId(rs.getLong("buying_team_id"));
                Team seller = new Team(); seller.setId(rs.getLong("selling_team_id"));
                offer.setPlayer(player); offer.setBuyingTeam(buyer); offer.setSellingTeam(seller);
                offer.setAmount(rs.getDouble("amount"));
                offer.setOfferDate(LocalDate.parse(rs.getString("offer_date")));
                offer.setStatus(TransferOfferStatus.valueOf(rs.getString("status")));
                offer.setCounterAmount((Double) rs.getObject("counter_amount"));
                return offer;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find transfer offer.", e);
        }
    }

    public void updateStatus(long offerId, TransferOfferStatus status) {
        String sql = "UPDATE transfer_offers SET status = ? WHERE id = ? AND status = 'PENDING'";
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
                WHERE id = ? AND status = 'PENDING'
                """;
        updateExactlyOne(sql, counterAmount, offerId,
                "Offer is not available for a counteroffer.");
    }

    public void acceptCounterOffer(long offerId) {
        String sql = """
                UPDATE transfer_offers SET amount = counter_amount,
                    counter_amount = NULL, status = 'ACCEPTED'
                WHERE id = ? AND status = 'PENDING' AND counter_amount IS NOT NULL
                """;
        updateExactlyOne(sql, offerId, "Counteroffer is not available.");
    }

    public List<TransferOffer> findPendingBySellingTeam(long teamId) {
        String sql = """
                SELECT id FROM transfer_offers
                WHERE selling_team_id = ? AND status = 'PENDING'
                ORDER BY offer_date DESC, id DESC
                """;
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

    public boolean hasPendingOfferForPlayer(long playerId) {
        String sql = "SELECT 1 FROM transfer_offers WHERE player_id = ? AND status = 'PENDING' LIMIT 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, playerId);
            try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
        } catch (SQLException e) {
            throw new RuntimeException("Could not check pending player offers.", e);
        }
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
