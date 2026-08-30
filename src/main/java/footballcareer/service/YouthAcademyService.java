package footballcareer.service;

import footballcareer.database.CareerContext;
import footballcareer.database.Database;
import footballcareer.model.Career;
import footballcareer.model.Player;
import footballcareer.model.enums.Position;
import footballcareer.model.enums.PreferredFoot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class YouthAcademyService {
    public record Scout(String name, int quality, LocalDate hiredDate) {}
    public record Prospect(long id, String firstName, String lastName, String nationality,
                           Position position, PreferredFoot foot, LocalDate birthDate,
                           int overall, int potential, LocalDate reportDate) {
        public String fullName() { return firstName + " " + lastName; }
    }

    public static double hiringCost(int quality) {
        if (quality < 1 || quality > 5) throw new IllegalArgumentException("Quality must be 1-5.");
        return quality * 750_000.0;
    }

    public Scout findScout(long careerId) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM career_scouts WHERE career_id = ?")) {
            statement.setLong(1, careerId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? new Scout(rows.getString("name"), rows.getInt("quality"),
                        LocalDate.parse(rows.getString("hired_date"))) : null;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load academy scout.", exception);
        }
    }

    public Scout hireScout(Career career, String name, int quality) {
        requireCareer(career);
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Scout name is required.");
        double cost = hiringCost(quality);
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                updateExactlyOne(connection, """
                        UPDATE career_club_finances
                        SET transfer_budget = transfer_budget - ?, balance = balance - ?
                        WHERE career_id = ? AND team_id = ? AND transfer_budget >= ?
                        """, "Insufficient budget to hire this scout.", cost, cost,
                        career.getId(), career.getControlledTeam().getId(), cost);
                update(connection, """
                        INSERT INTO career_scouts (career_id, name, quality, hired_date)
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT(career_id) DO UPDATE SET name = excluded.name,
                            quality = excluded.quality, hired_date = excluded.hired_date
                        """, career.getId(), name.trim(), quality,
                        career.getCurrentDate().toString());
                connection.commit();
                return new Scout(name.trim(), quality, career.getCurrentDate());
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not hire academy scout.", exception);
        }
    }

    public List<Prospect> generateReport(Career career) {
        requireCareer(career);
        Scout scout = findScout(career.getId());
        if (scout == null) throw new IllegalStateException("Hire a scout before requesting reports.");
        LocalDate latest = latestReport(career.getId());
        if (latest != null && latest.plusDays(30).isAfter(career.getCurrentDate())) {
            throw new IllegalStateException("The next youth report will be available on "
                    + latest.plusDays(30) + ".");
        }
        String[] first = {"Álex", "Hugo", "Iker", "Mateo", "Leo", "Nico", "Bruno", "Sergio"};
        String[] last = {"Santos", "Navarro", "Campos", "Vega", "Molina", "Ríos", "Pascual", "Soler"};
        Position[] positions = Position.values();
        Random random = new Random(career.getId() * 97 + career.getCurrentDate().toEpochDay());
        try (Connection connection = Database.getConnection()) {
            for (int index = 0; index < 5; index++) {
                int overall = 53 + scout.quality() * 2 + random.nextInt(7);
                int potential = Math.min(94, overall + 10 + scout.quality() * 2
                        + random.nextInt(8));
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO career_youth_candidates
                            (career_id, first_name, last_name, nationality, position,
                             preferred_foot, birth_date, overall, potential, report_date)
                        VALUES (?, ?, ?, 'España', ?, ?, ?, ?, ?, ?)
                        """)) {
                    statement.setLong(1, career.getId());
                    statement.setString(2, first[(index + random.nextInt(first.length)) % first.length]);
                    statement.setString(3, last[(index * 2 + random.nextInt(last.length)) % last.length]);
                    statement.setString(4, positions[random.nextInt(positions.length)].name());
                    statement.setString(5, random.nextBoolean() ? "RIGHT" : "LEFT");
                    statement.setString(6, career.getCurrentDate().minusYears(16 + random.nextInt(3))
                            .minusDays(random.nextInt(300)).toString());
                    statement.setInt(7, overall);
                    statement.setInt(8, potential);
                    statement.setString(9, career.getCurrentDate().toString());
                    statement.executeUpdate();
                }
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not generate youth report.", exception);
        }
        return findCandidates(career.getId());
    }

    public List<Prospect> findCandidates(long careerId) {
        List<Prospect> prospects = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT * FROM career_youth_candidates
                     WHERE career_id = ? AND promoted = 0
                     ORDER BY potential DESC, overall DESC
                     """)) {
            statement.setLong(1, careerId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) prospects.add(map(rows));
            }
            return prospects;
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load youth candidates.", exception);
        }
    }

    public Player promote(Career career, long prospectId) {
        requireCareer(career);
        Prospect prospect = findCandidates(career.getId()).stream()
                .filter(item -> item.id() == prospectId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Youth candidate is unavailable."));
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long playerId;
                int attribute = Math.max(40, prospect.overall() - 3);
                try (PreparedStatement player = connection.prepareStatement("""
                        INSERT INTO players
                            (first_name, last_name, birth_date, nationality, position,
                             preferred_foot, overall, potential, pace, shooting, passing,
                             dribbling, defending, physical, market_value, salary)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    bind(player, prospect.firstName(), prospect.lastName(),
                            prospect.birthDate().toString(), prospect.nationality(),
                            prospect.position().name(), prospect.foot().name(),
                            prospect.overall(), prospect.potential(), attribute, attribute,
                            attribute, attribute, attribute, attribute,
                            Math.max(250_000, (prospect.overall() - 50) * 150_000.0), 100_000.0);
                    player.executeUpdate();
                    try (ResultSet keys = player.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Missing academy player id.");
                        playerId = keys.getLong(1);
                    }
                }
                long team = career.getControlledTeam().getId();
                String date = career.getCurrentDate().toString();
                update(connection, "INSERT INTO career_player_team VALUES (?, ?, ?, ?, NULL)",
                        career.getId(), playerId, team, date);
                update(connection, """
                        INSERT INTO career_contracts
                            (career_id, player_id, team_id, start_date, end_date, salary,
                             squad_role, active)
                        VALUES (?, ?, ?, ?, ?, 100000, 'PROSPECT', 1)
                        """, career.getId(), playerId, team, date,
                        career.getCurrentDate().plusYears(3).toString());
                update(connection, "INSERT INTO career_player_state VALUES (?, ?, 50, 60, 100, NULL, NULL)",
                        career.getId(), playerId);
                update(connection, """
                        INSERT INTO career_player_development VALUES
                            (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, career.getId(), playerId, prospect.overall(), attribute,
                        attribute, attribute, attribute, attribute, attribute);
                update(connection, "INSERT INTO career_player_market_status VALUES (?, ?, 'NOT_LISTED', NULL)",
                        career.getId(), playerId);
                update(connection, """
                        INSERT INTO career_player_progress_history
                            (career_id, player_id, snapshot_date, overall, market_value)
                        VALUES (?, ?, ?, ?, ?)
                        """, career.getId(), playerId, date, prospect.overall(),
                        Math.max(250_000, (prospect.overall() - 50) * 150_000.0));
                updateExactlyOne(connection, """
                        UPDATE career_youth_candidates SET promoted = 1
                        WHERE id = ? AND career_id = ? AND promoted = 0
                        """, "Youth candidate is no longer available.", prospectId, career.getId());
                connection.commit();
                Player promoted = new Player();
                promoted.setId(playerId); promoted.setFirstName(prospect.firstName());
                promoted.setLastName(prospect.lastName()); promoted.setOverall(prospect.overall());
                return promoted;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not promote youth player.", exception);
        }
    }

    private LocalDate latestReport(long careerId) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT MAX(report_date) FROM career_youth_candidates WHERE career_id = ?")) {
            statement.setLong(1, careerId);
            try (ResultSet rows = statement.executeQuery()) {
                String value = rows.next() ? rows.getString(1) : null;
                return value == null ? null : LocalDate.parse(value);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load youth report date.", exception);
        }
    }

    private Prospect map(ResultSet row) throws SQLException {
        return new Prospect(row.getLong("id"), row.getString("first_name"),
                row.getString("last_name"), row.getString("nationality"),
                Position.valueOf(row.getString("position")),
                PreferredFoot.valueOf(row.getString("preferred_foot")),
                LocalDate.parse(row.getString("birth_date")), row.getInt("overall"),
                row.getInt("potential"), LocalDate.parse(row.getString("report_date")));
    }

    private void requireCareer(Career career) {
        if (career == null || career.getId() <= 0 || !Long.valueOf(career.getId())
                .equals(CareerContext.getCareerId())) {
            throw new IllegalStateException("This career must be active.");
        }
    }

    private void update(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values); statement.executeUpdate();
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
        for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
    }
}
