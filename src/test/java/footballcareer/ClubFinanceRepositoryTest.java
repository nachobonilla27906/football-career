package footballcareer;

import footballcareer.database.*;
import footballcareer.model.ClubFinance;
import footballcareer.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClubFinanceRepositoryTest {
    private ClubFinanceRepository repository;
    private Team valencia;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetAndSeedForTests();
        repository = new ClubFinanceRepository();
        valencia = new TeamRepository().findByShortName("VCF");
    }

    @Test
    void shouldInitializeAndMoveTransferBudget() {
        ClubFinance initial = repository.findByTeam(valencia.getId());
        assertNotNull(initial);
        assertTrue(initial.getTransferBudget() > 0);
        assertTrue(initial.getWageBudget() >= initial.getCurrentWageSpend());

        repository.spendTransferBudget(valencia.getId(), 5_000_000);
        assertEquals(initial.getTransferBudget() - 5_000_000,
                repository.findByTeam(valencia.getId()).getTransferBudget());

        repository.receiveTransferFee(valencia.getId(), 2_000_000);
        assertEquals(initial.getTransferBudget() - 3_000_000,
                repository.findByTeam(valencia.getId()).getTransferBudget());
    }

    @Test
    void shouldRejectOverspending() {
        double budget = repository.findByTeam(valencia.getId()).getTransferBudget();
        assertThrows(IllegalStateException.class, () ->
                repository.spendTransferBudget(valencia.getId(), budget + 1));
    }
}
