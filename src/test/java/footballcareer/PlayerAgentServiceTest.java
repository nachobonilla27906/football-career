package footballcareer;

import footballcareer.model.Player;
import footballcareer.service.PlayerAgentService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PlayerAgentServiceTest {
    private final PlayerAgentService service = new PlayerAgentService();
    private final Player player = player();

    @Test
    void agentConsidersWholePackageAndReturnsCounterInsteadOfBinaryThreshold() {
        var accepted = service.evaluate(player, 2_000_000, 2_000_000,
                4, 80_000_000d, "IMPORTANT");
        var counter = service.evaluate(player, 1_700_000, 0, 3,
                80_000_000d, "IMPORTANT");
        var rejected = service.evaluate(player, 500_000, 0, 1,
                20_000_000d, "ROTATION");

        assertEquals(PlayerAgentService.Decision.ACCEPTED, accepted.decision());
        assertEquals(PlayerAgentService.Decision.COUNTER, counter.decision());
        assertEquals(PlayerAgentService.Decision.REJECTED, rejected.decision());
        assertTrue(counter.message().contains("contraoferta"));
        assertThrows(IllegalStateException.class, () -> service.requireAgreement(
                player, 500_000, 0, 1, 20_000_000d, "ROTATION"));
    }

    private Player player() {
        Player player = new Player();
        player.setBirthDate(LocalDate.of(2000, 1, 1));
        player.setSalary(2_000_000);
        player.setMarketValue(40_000_000);
        return player;
    }
}
