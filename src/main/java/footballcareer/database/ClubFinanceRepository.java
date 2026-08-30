package footballcareer.database;

import footballcareer.model.ClubFinance;
import footballcareer.model.Team;

import java.sql.*;

public class ClubFinanceRepository {
    public void initializeMissingFinances() {
        String sql = """
                INSERT OR IGNORE INTO club_finances
                    (team_id, transfer_budget, wage_budget,
                     current_wage_spend, balance)
                SELECT t.id,
                       MAX(20000000, (t.reputation - 70) * 10000000),
                       MAX(COALESCE(SUM(c.salary), 0) * 1.25, 30000000),
                       COALESCE(SUM(c.salary), 0),
                       MAX(50000000, (t.reputation - 65) * 15000000)
                FROM teams t
                LEFT JOIN contracts c ON t.id = c.team_id AND c.active = 1
                GROUP BY t.id
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not initialize club finances.", e);
        }
    }

    public ClubFinance findByTeam(long teamId) {
        Long careerId = CareerContext.getCareerId();
        String sql = careerId == null ? "SELECT * FROM club_finances WHERE team_id = ?"
                : "SELECT * FROM career_club_finances WHERE team_id = ? AND career_id = " + careerId;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teamId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return null;
                Team team = new Team(); team.setId(teamId);
                ClubFinance finance = new ClubFinance();
                finance.setTeam(team);
                finance.setTransferBudget(rs.getDouble("transfer_budget"));
                finance.setWageBudget(rs.getDouble("wage_budget"));
                finance.setCurrentWageSpend(rs.getDouble("current_wage_spend"));
                finance.setBalance(rs.getDouble("balance"));
                return finance;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find club finances.", e);
        }
    }

    public void spendTransferBudget(long teamId, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");
        Long careerId = CareerContext.getCareerId();
        String table = careerId == null ? "club_finances" : "career_club_finances";
        String sql = """
                UPDATE %s
                SET transfer_budget = transfer_budget - ?, balance = balance - ?
                WHERE team_id = ? AND transfer_budget >= ? %s
                """.formatted(table, careerId == null ? "" : "AND career_id = " + careerId);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, amount);
            statement.setDouble(2, amount);
            statement.setLong(3, teamId);
            statement.setDouble(4, amount);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("Insufficient transfer budget.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not spend transfer budget.", e);
        }
    }

    public void receiveTransferFee(long teamId, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");
        Long careerId = CareerContext.getCareerId();
        String table = careerId == null ? "club_finances" : "career_club_finances";
        String sql = """
                UPDATE %s
                SET transfer_budget = transfer_budget + ?, balance = balance + ?
                WHERE team_id = ? %s
                """.formatted(table, careerId == null ? "" : "AND career_id = " + careerId);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, amount);
            statement.setDouble(2, amount);
            statement.setLong(3, teamId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Club finances do not exist.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not receive transfer fee.", e);
        }
    }
}
