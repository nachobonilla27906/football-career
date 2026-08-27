package footballcareer.database;

import footballcareer.model.*;
import footballcareer.model.enums.TransferOfferStatus;
import java.sql.*;
import java.time.LocalDate;

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
}
