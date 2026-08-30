package footballcareer;

import footballcareer.database.*;
import footballcareer.model.Player;
import footballcareer.model.PlayerState;
import footballcareer.service.WorldSimulationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class WorldSimulationServiceTest {
    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetAndSeedForTests();
    }

    @Test
    void shouldRecoverFitnessDailyAndDevelopPlayersMonthly() {
        PlayerRepository players = new PlayerRepository();
        PlayerStateRepository states = new PlayerStateRepository();
        Player developing = players.findAll().stream()
                .filter(player -> player.getAge(LocalDate.of(2026, 9, 1)) <= 23)
                .filter(player -> player.getOverall() < player.getPotential())
                .findFirst().orElseThrow();
        int initialOverall = developing.getOverall();
        PlayerState state = states.findByPlayer(developing.getId());
        state.setFitness(70);
        states.update(state);
        WorldSimulationService world = new WorldSimulationService(null, players, states);

        world.processDate(LocalDate.of(2026, 8, 31));
        assertEquals(73, states.findByPlayer(developing.getId()).getFitness());
        assertEquals(initialOverall, players.findById(developing.getId()).getOverall());

        world.processDate(LocalDate.of(2026, 9, 1));
        assertEquals(76, states.findByPlayer(developing.getId()).getFitness());
        assertEquals(initialOverall + 1, players.findById(developing.getId()).getOverall());
    }
}
