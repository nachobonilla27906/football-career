package footballcareer;

import footballcareer.database.*;
import footballcareer.model.Player;
import footballcareer.service.PlayerDevelopmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerDevelopmentServiceTest {
    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
    }

    @Test
    void shouldDevelopYoungPlayerWithoutExceedingPotential() {
        PlayerRepository repository = new PlayerRepository();
        Player player = repository.findByIdentity(
                "Lamine", "Yamal", LocalDate.of(2007, 7, 13));
        int previousOverall = player.getOverall();

        new PlayerDevelopmentService(repository)
                .applyMonthlyDevelopment(player, LocalDate.of(2026, 9, 1));

        Player updated = repository.findById(player.getId());
        assertEquals(previousOverall + 1, updated.getOverall());
        assertEquals(92, updated.getOverall());
    }
}
