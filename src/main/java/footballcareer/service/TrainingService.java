package footballcareer.service;

import footballcareer.database.Database;
import footballcareer.database.CareerContext;
import footballcareer.model.Career;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

public class TrainingService {
    public enum TrainingType { RECOVERY, BALANCED, INTENSIVE }
    public record TrainingResult(TrainingType type, int affectedPlayers,
                                 int formChange, int fitnessChange, int moraleChange) {}

    public TrainingResult train(Career career, TrainingType type) {
        if (career == null || career.getId() <= 0 || type == null) {
            throw new IllegalArgumentException("La carrera y el entrenamiento son obligatorios.");
        }
        int form = switch (type) {
            case RECOVERY -> -1;
            case BALANCED -> 2;
            case INTENSIVE -> 4;
        };
        int fitness = switch (type) {
            case RECOVERY -> 10;
            case BALANCED -> -3;
            case INTENSIVE -> -8;
        };
        int morale = switch (type) {
            case RECOVERY, BALANCED -> 1;
            case INTENSIVE -> -1;
        };
        int coachLevel = new StaffService().level(career.getId(), StaffService.Role.COACH);
        if (form > 0) form += coachLevel / 2;
        if (form > 0) form += new ManagerIdentityService().trainingFormBonus(
                new ManagerIdentityService().identity(career.getId()));
        if (type == TrainingType.RECOVERY) fitness += coachLevel / 2;
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                insertSession(connection, career, type);
                int affected = updateSquad(connection, career.getControlledTeam().getId(),
                        form, fitness, morale);
                connection.commit();
                return new TrainingResult(type, affected, form, fitness, morale);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                if (exception instanceof SQLException sql
                        && sql.getMessage().contains("UNIQUE constraint failed")) {
                    throw new IllegalStateException("Ya has realizado el entrenamiento de hoy.");
                }
                throw exception;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("No se pudo completar el entrenamiento.", exception);
        }
    }

    public TrainingType findToday(Career career) {
        String sql = """
                SELECT training_type FROM training_sessions
                WHERE career_id = ? AND session_date = ?
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, career.getId());
            statement.setString(2, career.getCurrentDate().toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? TrainingType.valueOf(resultSet.getString("training_type")) : null;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("No se pudo consultar el entrenamiento.", exception);
        }
    }

    public Map<LocalDate, TrainingType> findByMonth(Career career, YearMonth month) {
        String sql = """
                SELECT session_date, training_type FROM training_sessions
                WHERE career_id = ? AND session_date BETWEEN ? AND ?
                ORDER BY session_date
                """;
        Map<LocalDate, TrainingType> sessions = new LinkedHashMap<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, career.getId());
            statement.setString(2, month.atDay(1).toString());
            statement.setString(3, month.atEndOfMonth().toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) sessions.put(
                        LocalDate.parse(resultSet.getString("session_date")),
                        TrainingType.valueOf(resultSet.getString("training_type")));
            }
            return sessions;
        } catch (SQLException exception) {
            throw new RuntimeException("No se pudo consultar el historial de entrenamiento.", exception);
        }
    }

    private void insertSession(Connection connection, Career career, TrainingType type)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO training_sessions (career_id, team_id, session_date, training_type)
                VALUES (?, ?, ?, ?)
                """)) {
            statement.setLong(1, career.getId());
            statement.setLong(2, career.getControlledTeam().getId());
            statement.setString(3, career.getCurrentDate().toString());
            statement.setString(4, type.name());
            statement.executeUpdate();
        }
    }

    private int updateSquad(Connection connection, long teamId, int form, int fitness,
            int morale) throws SQLException {
        Long careerId = CareerContext.getCareerId();
        String stateTable = careerId == null ? "player_state" : "career_player_state";
        String membership = careerId == null ? "player_team" : "career_player_team";
        String sql = """
                UPDATE %s
                SET form = MAX(0, MIN(100, form + ?)),
                    fitness = MAX(0, MIN(100, fitness + ?)),
                    morale = MAX(0, MIN(100, morale + ?))
                WHERE player_id IN (
                    SELECT player_id FROM %s
                    WHERE team_id = ? AND end_date IS NULL %s
                )
                %s
                """.formatted(stateTable, membership,
                careerId == null ? "" : "AND career_id = " + careerId,
                careerId == null ? "" : "AND career_id = " + careerId);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, form);
            statement.setInt(2, fitness);
            statement.setInt(3, morale);
            statement.setLong(4, teamId);
            return statement.executeUpdate();
        }
    }
}
