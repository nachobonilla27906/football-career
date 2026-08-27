package footballcareer;

import footballcareer.database.*;
import footballcareer.model.Team;
import footballcareer.model.Transfer;
import footballcareer.service.ClubTransferAiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClubTransferAiServiceTest {
    private ClubTransferAiService service;
    private Team controlledTeam;
    private long seasonId;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
        service = new ClubTransferAiService();
        controlledTeam = new TeamRepository().findByShortName("VCF");
        seasonId = new SeasonRepository().findFirst().getId();
    }

    @Test
    void shouldTradeOnlyInWeeklyWindowAndProtectControlledClub() {
        assertTrue(service.processMarket(LocalDate.of(2026, 9, 7),
                seasonId, controlledTeam.getId()).isEmpty());
        assertTrue(service.processMarket(LocalDate.of(2026, 8, 18),
                seasonId, controlledTeam.getId()).isEmpty());

        List<Transfer> transfers = service.processMarket(LocalDate.of(2026, 8, 17),
                seasonId, controlledTeam.getId());

        assertFalse(transfers.isEmpty());
        assertTrue(transfers.stream().allMatch(transfer ->
                transfer.getFromTeam().getId() != controlledTeam.getId()
                        && transfer.getToTeam().getId() != controlledTeam.getId()));
        assertTrue(transfers.stream().allMatch(transfer ->
                transfer.getTransferDate().equals(LocalDate.of(2026, 8, 17))));
    }
}
