package footballcareer.service;

import footballcareer.database.CareerContext;
import footballcareer.database.Database;
import footballcareer.database.PlayerStateRepository;
import footballcareer.model.Career;
import footballcareer.model.PlayerState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

public class MedicalTreatmentService {
    public enum Treatment { REHAB, SPECIALIST }
    public record Result(Treatment treatment, LocalDate previousReturn,
                         LocalDate newReturn, int fitnessGain) {}

    public Result treat(Career career, long playerId, Treatment treatment) {
        if (career == null || CareerContext.getCareerId() == null
                || CareerContext.getCareerId() != career.getId()) {
            throw new IllegalStateException("A loaded career is required.");
        }
        PlayerStateRepository repository = new PlayerStateRepository();
        PlayerState state = repository.findByPlayer(playerId);
        if (state == null || state.isAvailableOn(career.getCurrentDate())
                || !"INJURY".equals(state.getUnavailableReason())) {
            throw new IllegalStateException("Player does not have a treatable injury.");
        }
        ensureNotTreatedToday(career.getId(), playerId, career.getCurrentDate());
        LocalDate previous = state.getUnavailableUntil();
        int reduction = treatment == Treatment.SPECIALIST ? 5 : 2;
        int fitnessGain = treatment == Treatment.REHAB ? 6 : 2;
        int physioLevel = new StaffService().level(career.getId(), StaffService.Role.PHYSIO);
        reduction += physioLevel / 2;
        fitnessGain += physioLevel / 2;
        LocalDate minimum = career.getCurrentDate().plusDays(1);
        LocalDate updated = previous.minusDays(reduction);
        if (updated.isBefore(minimum)) updated = minimum;
        repository.setUnavailable(playerId, updated, "INJURY");
        state.setFitness(Math.min(100, state.getFitness() + fitnessGain));
        repository.update(state);
        saveTreatment(career.getId(), playerId, career.getCurrentDate(), treatment);
        return new Result(treatment, previous, updated, fitnessGain);
    }

    public boolean treatedToday(long careerId, long playerId, LocalDate date) {
        String sql = "SELECT 1 FROM medical_treatments WHERE career_id = ? "
                + "AND player_id = ? AND treatment_date = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, careerId);
            statement.setLong(2, playerId);
            statement.setString(3, date.toString());
            try (var rs = statement.executeQuery()) { return rs.next(); }
        } catch (SQLException e) {
            throw new RuntimeException("Could not check medical treatment.", e);
        }
    }

    private void ensureNotTreatedToday(long careerId, long playerId, LocalDate date) {
        if (treatedToday(careerId, playerId, date)) {
            throw new IllegalStateException("Player has already been treated today.");
        }
    }

    private void saveTreatment(long careerId, long playerId, LocalDate date,
            Treatment treatment) {
        String sql = "INSERT INTO medical_treatments "
                + "(career_id, player_id, treatment_date, treatment_type) VALUES (?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, careerId);
            statement.setLong(2, playerId);
            statement.setString(3, date.toString());
            statement.setString(4, treatment.name());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not save medical treatment.", e);
        }
    }
}
