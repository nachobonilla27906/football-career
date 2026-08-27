package footballcareer;

import footballcareer.database.*;
import footballcareer.model.Contract;
import footballcareer.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContractRepositoryTest {
    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
    }

    @Test
    void shouldSeedOneActiveContractForPlayer() {
        Player player = new PlayerRepository()
                .findCurrentPlayersByTeam(
                        new TeamRepository().findByShortName("ARS").getId())
                .getFirst();

        Contract contract = new ContractRepository()
                .findActiveByPlayer(player.getId());

        assertNotNull(contract);
        assertTrue(contract.isActive());
        assertEquals(player.getSalary(), contract.getSalary());
        assertEquals(2030, contract.getEndDate().getYear());
    }
}
