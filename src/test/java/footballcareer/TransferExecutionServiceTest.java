package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.model.enums.TransferOfferStatus;
import footballcareer.service.TransferExecutionService;
import footballcareer.service.TransferOfferService;
import footballcareer.service.TransferObligationService;
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
        DatabaseInitializer.resetAndSeedForTests();
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

        TransferExecutionService.ContractTerms terms =
                new TransferExecutionService.ContractTerms(
                        100_000, 500_000, 30_000_000.0, "IMPORTANT");
        new TransferExecutionService().completeTransfer(offer.getId(), terms,
                LocalDate.of(2030, 6, 30), season.getId(), LocalDate.of(2026, 8, 21));

        assertEquals(valencia.getId(), new PlayerTeamRepository().findCurrentTeamId(player.getId()));
        Contract contract = new ContractRepository().findActiveByPlayer(player.getId());
        assertEquals(valencia.getId(), contract.getTeam().getId());
        assertEquals(100_000, contract.getSalary());
        assertEquals(500_000, contract.getSigningBonus());
        assertEquals(30_000_000, contract.getReleaseClause());
        assertEquals("IMPORTANT", contract.getSquadRole());
        assertEquals(buyerBudget - 10_500_000,
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

    @Test
    void failedMedicalLeavesTheAcceptedDealUntouched() {
        new PlayerStateRepository().setUnavailable(player.getId(),
                LocalDate.of(2026, 9, 10), "INJURY");
        double budget = new ClubFinanceRepository().findByTeam(valencia.getId())
                .getTransferBudget();

        assertThrows(IllegalStateException.class, () ->
                new TransferExecutionService().completeTransfer(offer.getId(),
                        new TransferExecutionService.ContractTerms(
                                100_000, 500_000, 30_000_000.0, "IMPORTANT"),
                        LocalDate.of(2030, 6, 30), season.getId(),
                        LocalDate.of(2026, 8, 21)));

        assertEquals(arsenal.getId(), new PlayerTeamRepository()
                .findCurrentTeamId(player.getId()));
        assertEquals(budget, new ClubFinanceRepository().findByTeam(valencia.getId())
                .getTransferBudget());
        assertNull(new TransferRepository().findByOffer(offer.getId()));
    }

    @Test
    void installmentsAndAppearanceBonusSettleOnTheirRealConditions() throws Exception {
        Player second = new PlayerRepository().findCurrentPlayersByTeam(arsenal.getId()).get(1);
        new PlayerMarketRepository().listForTransfer(second.getId(), 8_000_000);
        TransferOfferService offers = new TransferOfferService();
        TransferOffer structured = offers.makeOffer(second.getId(), valencia.getId(),
                8_000_000, LocalDate.of(2026, 8, 20), 50, 2_000_000);
        offers.evaluate(structured.getId());
        ClubFinanceRepository finances = new ClubFinanceRepository();
        double buyerBalance = finances.findByTeam(valencia.getId()).getBalance();
        double sellerBalance = finances.findByTeam(arsenal.getId()).getBalance();

        new TransferExecutionService().completeTransfer(structured.getId(), 100_000,
                LocalDate.of(2030, 6, 30), season.getId(), LocalDate.of(2026, 8, 21));

        assertEquals(buyerBalance - 4_000_000,
                finances.findByTeam(valencia.getId()).getBalance());
        assertEquals(sellerBalance + 4_000_000,
                finances.findByTeam(arsenal.getId()).getBalance());
        assertEquals(1, new TransferObligationService().process(LocalDate.of(2027, 2, 21)));
        new PlayerSeasonStatsRepository().initializeForSeason(season.getId());
        try (var connection = Database.getConnection(); var statement = connection.prepareStatement(
                "UPDATE player_season_stats SET appearances=10 WHERE player_id=?")) {
            statement.setLong(1, second.getId()); statement.executeUpdate();
        }
        assertEquals(1, new TransferObligationService().process(LocalDate.of(2027, 2, 22)));
        assertEquals(1, new TransferObligationService().process(LocalDate.of(2027, 8, 21)));
        assertEquals(buyerBalance - 10_000_000,
                finances.findByTeam(valencia.getId()).getBalance());
        assertEquals(sellerBalance + 10_000_000,
                finances.findByTeam(arsenal.getId()).getBalance());
    }
}
