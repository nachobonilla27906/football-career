package footballcareer.database;

import footballcareer.model.Transfer;
import footballcareer.model.Player;
import footballcareer.model.Season;
import footballcareer.model.Team;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransferRepository {
    public Transfer findByOffer(long offerId) {
        String sql = "SELECT * FROM transfers WHERE offer_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, offerId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return null;
                return mapTransfer(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find transfer.", e);
        }
    }

    public List<Transfer> findByTeam(long teamId) {
        String sql = """
                SELECT * FROM transfers
                WHERE from_team_id = ? OR to_team_id = ?
                ORDER BY transfer_date DESC, id DESC
                """;
        List<Transfer> transfers = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teamId);
            statement.setLong(2, teamId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) transfers.add(mapTransfer(rs));
            }
            return transfers;
        } catch (SQLException e) {
            throw new RuntimeException("Could not find club transfer history.", e);
        }
    }

    private Transfer mapTransfer(ResultSet rs) throws SQLException {
        Transfer transfer = new Transfer();
        transfer.setId(rs.getLong("id"));
        Player player = new Player(); player.setId(rs.getLong("player_id"));
        Team from = new Team(); from.setId(rs.getLong("from_team_id"));
        Team to = new Team(); to.setId(rs.getLong("to_team_id"));
        Season season = new Season(); season.setId(rs.getLong("season_id"));
        transfer.setPlayer(player);
        transfer.setFromTeam(from);
        transfer.setToTeam(to);
        transfer.setSeason(season);
        transfer.setAmount(rs.getDouble("amount"));
        transfer.setTransferDate(LocalDate.parse(rs.getString("transfer_date")));
        transfer.setOfferId(rs.getLong("offer_id"));
        return transfer;
    }
}
