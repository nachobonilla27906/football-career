package footballcareer.database;

import footballcareer.model.Contract;
import footballcareer.model.Player;
import footballcareer.model.Team;

import java.sql.*;
import java.time.LocalDate;

public class ContractRepository {
    public void initializeMissingContracts(
            LocalDate startDate,
            LocalDate endDate
    ) {
        String sql = """
                INSERT INTO contracts
                    (player_id, team_id, start_date, end_date, salary, active)
                SELECT p.id, pt.team_id, ?, ?, p.salary, 1
                FROM players p
                JOIN player_team pt ON p.id = pt.player_id
                WHERE pt.end_date IS NULL
                  AND NOT EXISTS (
                      SELECT 1 FROM contracts c
                      WHERE c.player_id = p.id AND c.active = 1
                  )
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, startDate.toString());
            statement.setString(2, endDate.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not initialize contracts.", e);
        }
    }

    public void saveIfAbsent(Contract contract) {
        if (findActiveByPlayer(contract.getPlayer().getId()) != null) return;
        Long careerId = CareerContext.getCareerId();
        String sql = careerId == null ? """
                INSERT INTO contracts
                (player_id, team_id, start_date, end_date, salary, signing_bonus,
                 release_clause, squad_role, active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """ : """
                INSERT INTO career_contracts
                (career_id, player_id, team_id, start_date, end_date, salary,
                 signing_bonus, release_clause, squad_role, active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            int offset = 0;
            if (careerId != null) statement.setLong(++offset, careerId);
            statement.setLong(++offset, contract.getPlayer().getId());
            statement.setLong(++offset, contract.getTeam().getId());
            statement.setString(++offset, contract.getStartDate().toString());
            statement.setString(++offset, contract.getEndDate().toString());
            statement.setDouble(++offset, contract.getSalary());
            statement.setDouble(++offset, contract.getSigningBonus());
            statement.setObject(++offset, contract.getReleaseClause());
            statement.setString(++offset, contract.getSquadRole());
            statement.setInt(++offset, contract.isActive() ? 1 : 0);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) contract.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not save contract.", e);
        }
    }

    public Contract findActiveByPlayer(long playerId) {
        Long careerId = CareerContext.getCareerId();
        String sql = careerId == null
                ? "SELECT * FROM contracts WHERE player_id = ? AND active = 1"
                : "SELECT * FROM career_contracts WHERE player_id = ? AND active = 1 AND career_id = " + careerId;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, playerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return null;
                Contract contract = new Contract();
                contract.setId(resultSet.getLong("id"));
                Player player = new Player(); player.setId(playerId);
                Team team = new Team(); team.setId(resultSet.getLong("team_id"));
                contract.setPlayer(player);
                contract.setTeam(team);
                contract.setStartDate(LocalDate.parse(resultSet.getString("start_date")));
                contract.setEndDate(LocalDate.parse(resultSet.getString("end_date")));
                contract.setSalary(resultSet.getDouble("salary"));
                contract.setSigningBonus(resultSet.getDouble("signing_bonus"));
                contract.setReleaseClause((Double) resultSet.getObject("release_clause"));
                contract.setSquadRole(resultSet.getString("squad_role"));
                contract.setActive(true);
                return contract;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find active contract.", e);
        }
    }
}
