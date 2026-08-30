package footballcareer.service;

import footballcareer.database.Database;
import footballcareer.model.Career;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CareerActivityService {
    public enum Type { MATCH, TRANSFER, LOAN, TRAINING, REPUTATION }
    public record Activity(LocalDate date, Type type, String title, String detail) {}

    public List<Activity> recent(Career career, int limit) {
        if (career == null || limit <= 0) throw new IllegalArgumentException("Career and limit are required.");
        List<Activity> activities = new ArrayList<>();
        long careerId = career.getId();
        long teamId = career.getControlledTeam().getId();
        try (Connection connection = Database.getConnection()) {
            read(connection, """
                    SELECT m.date, ht.short_name home, at.short_name away,
                           cms.home_goals, cms.away_goals
                    FROM career_match_states cms
                    JOIN matches m ON m.id = cms.match_id
                    JOIN teams ht ON ht.id = m.home_team_id
                    JOIN teams at ON at.id = m.away_team_id
                    WHERE cms.career_id = ? AND cms.played = 1
                      AND (m.home_team_id = ? OR m.away_team_id = ?)
                    """, rows -> activities.add(new Activity(
                    LocalDate.parse(rows.getString("date")), Type.MATCH, "PARTIDO",
                    rows.getString("home") + " " + rows.getInt("home_goals") + "–"
                            + rows.getInt("away_goals") + " " + rows.getString("away"))),
                    careerId, teamId, teamId);
            read(connection, """
                    SELECT tr.transfer_date date, p.first_name, p.last_name,
                           ft.short_name origin, tt.short_name destination, tr.amount
                    FROM transfers tr JOIN players p ON p.id = tr.player_id
                    JOIN teams ft ON ft.id = tr.from_team_id
                    JOIN teams tt ON tt.id = tr.to_team_id
                    WHERE tr.career_id = ? AND (tr.from_team_id = ? OR tr.to_team_id = ?)
                    """, rows -> activities.add(new Activity(
                    LocalDate.parse(rows.getString("date")), Type.TRANSFER, "TRASPASO",
                    rows.getString("first_name") + " " + rows.getString("last_name")
                            + "  •  " + rows.getString("origin") + " → "
                            + rows.getString("destination") + "  •  €"
                            + String.format("%.1fM", rows.getDouble("amount") / 1_000_000))),
                    careerId, teamId, teamId);
            read(connection, """
                    SELECT l.start_date date, p.first_name, p.last_name, l.end_date,
                           l.fee, pt.short_name parent, bt.short_name borrower
                    FROM career_loans l JOIN players p ON p.id = l.player_id
                    JOIN teams pt ON pt.id = l.parent_team_id
                    JOIN teams bt ON bt.id = l.borrowing_team_id
                    WHERE l.career_id = ? AND (l.parent_team_id = ? OR l.borrowing_team_id = ?)
                    """, rows -> activities.add(new Activity(
                    LocalDate.parse(rows.getString("date")), Type.LOAN, "CESIÓN",
                    rows.getString("first_name") + " " + rows.getString("last_name")
                            + "  •  " + rows.getString("parent") + " → "
                            + rows.getString("borrower") + "  •  hasta "
                            + rows.getString("end_date"))), careerId, teamId, teamId);
            read(connection, """
                    SELECT session_date date, training_type FROM training_sessions
                    WHERE career_id = ? AND team_id = ?
                    """, rows -> activities.add(new Activity(
                    LocalDate.parse(rows.getString("date")), Type.TRAINING, "ENTRENAMIENTO",
                    switch (rows.getString("training_type")) {
                        case "RECOVERY" -> "Sesión de recuperación";
                        case "INTENSIVE" -> "Sesión intensiva";
                        default -> "Sesión equilibrada";
                    })), careerId, teamId);
            read(connection, """
                    SELECT snapshot_date date, score FROM career_manager_reputation
                    WHERE career_id = ?
                    """, rows -> activities.add(new Activity(
                    LocalDate.parse(rows.getString("date")), Type.REPUTATION, "REPUTACIÓN",
                    "Valoración del mánager: " + rows.getInt("score") + "/100")), careerId);
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load career activity.", exception);
        }
        return activities.stream().sorted(Comparator.comparing(Activity::date).reversed()
                .thenComparing(activity -> activity.type().ordinal())).limit(limit).toList();
    }

    private void read(Connection connection, String sql, RowConsumer consumer,
            Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) consumer.accept(rows);
            }
        }
    }

    @FunctionalInterface
    private interface RowConsumer { void accept(ResultSet rows) throws SQLException; }
}
