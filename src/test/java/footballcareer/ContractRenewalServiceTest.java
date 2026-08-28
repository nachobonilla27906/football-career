package footballcareer;

import footballcareer.database.*;
import footballcareer.model.Contract;
import footballcareer.model.Player;
import footballcareer.model.Season;
import footballcareer.model.Team;
import footballcareer.service.ContractRenewalService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContractRenewalServiceTest {

    @Test
    void renewalUpdatesContractAndWageSpendAtomically() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
        Season season = new SeasonRepository().findFirst();
        Team liverpool = new TeamRepository().findByShortName("LIV");
        Player player = new PlayerRepository().findCurrentPlayersByTeam(liverpool.getId()).getFirst();
        ContractRepository contracts = new ContractRepository();
        Contract before = contracts.findActiveByPlayer(player.getId());
        double previousSpend = new ClubFinanceRepository().findByTeam(liverpool.getId())
                .getCurrentWageSpend();
        double raise = 100_000;

        new ContractRenewalService().renew(player.getId(), liverpool.getId(),
                before.getEndDate().plusYears(2), before.getSalary() + raise);

        Contract renewed = contracts.findActiveByPlayer(player.getId());
        assertEquals(before.getEndDate().plusYears(2), renewed.getEndDate());
        assertEquals(before.getSalary() + raise, renewed.getSalary());
        assertEquals(previousSpend + raise, new ClubFinanceRepository()
                .findByTeam(liverpool.getId()).getCurrentWageSpend());
        assertThrows(IllegalStateException.class, () -> new ContractRenewalService().renew(
                player.getId(), liverpool.getId(), renewed.getEndDate().plusYears(1),
                renewed.getSalary() + 1_000_000_000));
        assertEquals(renewed.getSalary(), contracts.findActiveByPlayer(player.getId()).getSalary());
    }
}
