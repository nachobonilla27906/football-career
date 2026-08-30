package footballcareer.service;

import footballcareer.database.Database;
import footballcareer.database.PlayerStateRepository;
import footballcareer.database.PlayerTeamRepository;
import footballcareer.model.Career;
import footballcareer.model.PlayerState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class PlayerConversationService {
    public enum Approach { SUPPORT, CHALLENGE, REST }
    public record Result(Approach approach, int moraleChange, int formChange,
                         int fitnessChange, LocalDate nextAvailable) {}

    public Result hold(Career career, long playerId, Approach approach) {
        if (career == null || approach == null)
            throw new IllegalArgumentException("Career and approach are required.");
        Long teamId = new PlayerTeamRepository().findCurrentTeamId(playerId);
        if (teamId == null || teamId != career.getControlledTeam().getId())
            throw new IllegalArgumentException("Player does not belong to the controlled club.");
        LocalDate next = nextAvailable(career.getId(), playerId);
        if (next != null && career.getCurrentDate().isBefore(next))
            throw new IllegalStateException("Next conversation available on " + next + ".");

        int morale = switch (approach) { case SUPPORT -> 8; case CHALLENGE -> -3; case REST -> 5; };
        morale += new ManagerIdentityService().conversationMoraleBonus(
                new ManagerIdentityService().identity(career.getId()), morale);
        int form = switch (approach) { case SUPPORT -> -1; case CHALLENGE -> 4; case REST -> -2; };
        int fitness = approach == Approach.REST ? 5 : 0;
        PlayerStateRepository states = new PlayerStateRepository();
        PlayerState state = states.findByPlayer(playerId);
        if (state == null) throw new IllegalStateException("Player state is unavailable.");
        state.setMorale(state.getMorale() + morale);
        state.setForm(state.getForm() + form);
        state.setFitness(state.getFitness() + fitness);
        states.update(state);
        save(career.getId(), playerId, career.getCurrentDate(), approach, morale, form, fitness);
        return new Result(approach, morale, form, fitness, career.getCurrentDate().plusDays(7));
    }

    public LocalDate nextAvailable(long careerId, long playerId) {
        String sql = "SELECT MAX(conversation_date) FROM player_conversations "
                + "WHERE career_id = ? AND player_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, careerId); statement.setLong(2, playerId);
            try (ResultSet result = statement.executeQuery()) {
                String date = result.next() ? result.getString(1) : null;
                return date == null ? null : LocalDate.parse(date).plusDays(7);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not load player conversations.", exception);
        }
    }

    private void save(long careerId, long playerId, LocalDate date, Approach approach,
            int morale, int form, int fitness) {
        String sql = "INSERT INTO player_conversations "
                + "(career_id, player_id, conversation_date, approach, morale_change, "
                + "form_change, fitness_change) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, careerId); statement.setLong(2, playerId);
            statement.setString(3, date.toString()); statement.setString(4, approach.name());
            statement.setInt(5, morale); statement.setInt(6, form); statement.setInt(7, fitness);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save player conversation.", exception);
        }
    }
}
