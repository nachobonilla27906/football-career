package footballcareer;

import footballcareer.database.*;
import footballcareer.model.Player;
import footballcareer.model.PlayerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerStateRepositoryTest {
    private PlayerStateRepository repository;
    private Player player;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
        repository = new PlayerStateRepository();
        player = new PlayerRepository().findCurrentPlayersByTeam(
                new TeamRepository().findByShortName("ARS").getId()
        ).getFirst();
    }

    @Test
    void shouldInitializeAndUpdateStateWithinLimits() {
        PlayerState state = repository.findByPlayer(player.getId());
        assertEquals(50, state.getForm());
        assertEquals(50, state.getMorale());
        assertEquals(100, state.getFitness());

        state.setForm(110);
        state.setMorale(-5);
        state.setFitness(82);
        repository.update(state);

        PlayerState updated = repository.findByPlayer(player.getId());
        assertEquals(100, updated.getForm());
        assertEquals(0, updated.getMorale());
        assertEquals(82, updated.getFitness());
    }
}
