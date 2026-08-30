package footballcareer.database;

import footballcareer.model.Player;
import footballcareer.model.enums.TransferStatus;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlayerMarketRepository {
    private final PlayerRepository playerRepository = new PlayerRepository();

    public void initializeMissingStatuses() {
        String sql = """
                INSERT OR IGNORE INTO player_market_status (player_id)
                SELECT id FROM players
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not initialize market statuses.", e);
        }
    }

    public void listForTransfer(long playerId, double askingPrice) {
        if (askingPrice <= 0) {
            throw new IllegalArgumentException("Asking price must be positive.");
        }
        updateStatus(playerId, TransferStatus.TRANSFER_LISTED, askingPrice);
    }

    public void removeFromTransferList(long playerId) {
        updateStatus(playerId, TransferStatus.NOT_LISTED, null);
    }

    public List<Player> findTransferListed(long excludedTeamId) {
        Long careerId = CareerContext.getCareerId();
        String market = careerId == null ? "player_market_status" : "career_player_market_status";
        String membership = careerId == null ? "player_team" : "career_player_team";
        String sql = """
                SELECT pms.player_id
                FROM %s pms
                JOIN %s pt ON pms.player_id = pt.player_id
                WHERE pms.status = 'TRANSFER_LISTED'
                  %s
                  AND pt.end_date IS NULL
                  AND pt.team_id <> ?
                ORDER BY pms.asking_price, pms.player_id
                """.formatted(market, membership, careerId == null ? ""
                : "AND pms.career_id = " + careerId + " AND pt.career_id = " + careerId);
        Set<Long> listedIds = new java.util.HashSet<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, excludedTeamId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) listedIds.add(rs.getLong(1));
            }
            return playerRepository.findAll().stream()
                    .filter(player -> listedIds.contains(player.getId())).toList();
        } catch (SQLException e) {
            throw new RuntimeException("Could not find transfer-listed players.", e);
        }
    }

    public Map<Long, Double> findAllAskingPrices() {
        Long careerId = CareerContext.getCareerId();
        String table = careerId == null ? "player_market_status" : "career_player_market_status";
        String sql = "SELECT player_id, asking_price FROM " + table
                + " WHERE asking_price IS NOT NULL"
                + (careerId == null ? "" : " AND career_id = " + careerId);
        Map<Long, Double> prices = new HashMap<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) prices.put(resultSet.getLong("player_id"),
                    resultSet.getDouble("asking_price"));
            return prices;
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load market prices.", exception);
        }
    }

    public Double findAskingPrice(long playerId) {
        Long careerId = CareerContext.getCareerId();
        String sql = careerId == null
                ? "SELECT asking_price FROM player_market_status WHERE player_id = ?"
                : "SELECT asking_price FROM career_player_market_status WHERE player_id = ? AND career_id = " + careerId;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, playerId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next() || rs.getObject(1) == null) return null;
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find asking price.", e);
        }
    }

    private void updateStatus(long playerId, TransferStatus status, Double price) {
        Long careerId = CareerContext.getCareerId();
        String table = careerId == null ? "player_market_status" : "career_player_market_status";
        String sql = "UPDATE " + table + " SET status = ?, asking_price = ? WHERE player_id = ?"
                + (careerId == null ? "" : " AND career_id = " + careerId);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            if (price == null) statement.setNull(2, Types.REAL);
            else statement.setDouble(2, price);
            statement.setLong(3, playerId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Player market status does not exist.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not update market status.", e);
        }
    }
}
