package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.model.enums.TransferOfferStatus;
import footballcareer.service.TransferExecutionService;
import footballcareer.service.TransferOfferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TransferExecutionServiceTest {
    private Player player;
    private Team arsenal;
    private Team valencia;
    private Season season;
    private TransferOffer offer;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
        TeamRepository teams = new TeamRepository();
        arsenal = teams.findByShortName("ARS");
        valencia = teams.findByShortName("VCF");
        player = new PlayerRepository().findCurrentPlayersByTeam(arsenal.getId()).getFirst();
        season = new SeasonRepository().findFirst();

        new PlayerMarketRepository().listForTransfer(player.getId(), 10_000_000);
        TransferOfferService offers = new TransferOfferService();
        offer = offers.makeOffer(player.getId(), valencia.getId(),
                10_000_000, LocalDate.of(2026, 8, 20));
        offers.evaluate(offer.getId());
    }

    @Test
    void shouldCompleteEveryPartOfTransferAtomically() {
        ClubFinanceRepository finances = new ClubFinanceRepository();
        double buyerBudget = finances.findByTeam(valencia.getId()).getTransferBudget();
        double sellerBudget = finances.findByTeam(arsenal.getId()).getTransferBudget();

        new TransferExecutionService().completeTransfer(offer.getId(), 100_000,
                LocalDate.of(2030, 6, 30), season.getId(), LocalDate.of(2026, 8, 21));

        assertEquals(valencia.getId(), new PlayerTeamRepository().findCurrentTeamId(player.getId()));
        Contract contract = new ContractRepository().findActiveByPlayer(player.getId());
        assertEquals(valencia.getId(), contract.getTeam().getId());
        assertEquals(100_000, contract.getSalary());
        assertEquals(buyerBudget - 10_000_000,
                finances.findByTeam(valencia.getId()).getTransferBudget());
        assertEquals(sellerBudget + 10_000_000,
                finances.findByTeam(arsenal.getId()).getTransferBudget());
        assertEquals(TransferOfferStatus.COMPLETED,
                new TransferOfferRepository().findById(offer.getId()).getStatus());
        TransferRepository transfers = new TransferRepository();
        Transfer completed = transfers.findByOffer(offer.getId());
        assertNotNull(completed);
        assertEquals(player.getId(), completed.getPlayer().getId());
        assertEquals(arsenal.getId(), completed.getFromTeam().getId());
        assertEquals(valencia.getId(), completed.getToTeam().getId());
        assertEquals(season.getId(), completed.getSeason().getId());
        assertEquals(1, transfers.findByTeam(arsenal.getId()).size());
        assertEquals(1, transfers.findByTeam(valencia.getId()).size());
        assertThrows(IllegalStateException.class, () ->
                new TransferExecutionService().completeTransfer(offer.getId(), 100_000,
                        LocalDate.of(2030, 6, 30), season.getId(), LocalDate.of(2026, 8, 22)));
    }

    @Test
    void shouldRejectClosedWindowWithoutChangingSquad() {
        assertThrows(IllegalStateException.class, () ->
                new TransferExecutionService().completeTransfer(offer.getId(), 100_000,
                        LocalDate.of(2030, 6, 30), season.getId(), LocalDate.of(2026, 3, 10)));
        assertEquals(arsenal.getId(), new PlayerTeamRepository().findCurrentTeamId(player.getId()));
        assertNull(new TransferRepository().findByOffer(offer.getId()));
    }
}
