package footballcareer.service;

import footballcareer.database.Database;
import footballcareer.model.Career;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ManagerReputationService {
    public record Reputation(int score, int change) {
        public String display() {
            return score + (change > 0 ? "  ▲ +" + change
                    : change < 0 ? "  ▼ " + change : "  —");
        }
    }

    public Reputation record(Career career) {
        int confidence = new ManagerEvaluationService().evaluate(career).confidence();
        int score = Math.max(1, Math.min(100, career.getControlledTeam().getReputation()
                + Math.round((confidence - 70) * 0.15f)));
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO career_manager_reputation (career_id, snapshot_date, score)
                     VALUES (?, ?, ?)
                     ON CONFLICT(career_id, snapshot_date) DO UPDATE SET score = excluded.score
                     """)) {
            statement.setLong(1, career.getId());
            statement.setString(2, career.getCurrentDate().toString());
            statement.setInt(3, score);
            statement.executeUpdate();
            return find(career);
        } catch (SQLException exception) {
            throw new RuntimeException("Could not record manager reputation.", exception);
        }
    }

    public Reputation find(Career career) {
        int latest = career.getControlledTeam().getReputation();
        int previous = latest;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT score FROM career_manager_reputation
                     WHERE career_id = ? ORDER BY snapshot_date DESC LIMIT 2
                     """)) {
            statement.setLong(1, career.getId());
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) latest = rows.getInt(1);
                if (rows.next()) previous = rows.getInt(1);
            }
            return new Reputation(latest, latest - previous);
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load manager reputation.", exception);
        }
    }
}
