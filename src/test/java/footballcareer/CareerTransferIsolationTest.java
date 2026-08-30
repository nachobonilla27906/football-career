package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.model.enums.TransferOfferStatus;
import footballcareer.service.CareerService;
import footballcareer.service.CareerRepairService;
import footballcareer.service.ContractRenewalService;
import footballcareer.service.TransferExecutionService;
import footballcareer.service.TransferOfferService;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CareerTransferIsolationTest {

    @Test
    void duplicatedCareerKeepsProgressAndRemapsTransferOffers() throws Exception {
        DatabaseInitializer.resetAndSeedForTests();
        Team seller = new TeamRepository().findByShortName("ARS");
        Team buyer = new TeamRepository().findByShortName("RMA");
        Season season = new SeasonRepository().findFirst();
        CareerService service = new CareerService(new CareerRepository(),
                new TeamRepository(), new SeasonRepository());
        Career source = service.createCareer("Original", buyer.getId(), season.getId());
        Player player = new PlayerRepository().findCurrentPlayersByTeam(seller.getId()).getFirst();
        PlayerState state = new PlayerStateRepository().findByPlayer(player.getId());
        state.setMorale(23);
        new PlayerStateRepository().update(state);
        TransferOffer offer = new TransferOffer();
        offer.setPlayer(player); offer.setBuyingTeam(buyer); offer.setSellingTeam(seller);
        offer.setAmount(20_000_000); offer.setOfferDate(LocalDate.of(2026, 8, 20));
        offer.setStatus(TransferOfferStatus.PENDING);
        new TransferOfferRepository().save(offer);
        insertTransfer(source.getId(), offer.getId(), player.getId(), seller.getId(),
                buyer.getId(), season.getId());

        long copyId = new CareerRepository().duplicate(source.getId());
        Career copy = service.loadCareer(copyId);

        assertEquals("Original (copia)", copy.getManagerName());
        assertEquals(23, new PlayerStateRepository().findByPlayer(player.getId()).getMorale());
        assertEquals(1, new TransferRepository().findByTeam(buyer.getId()).size());
        Transfer copiedTransfer = new TransferRepository().findByTeam(buyer.getId()).getFirst();
        assertNotEquals(offer.getId(), copiedTransfer.getOfferId());
        assertNotNull(new TransferOfferRepository().findById(copiedTransfer.getOfferId()));

        try (Connection connection = Database.getConnection();
             PreparedStatement corrupt = connection.prepareStatement(
                     "UPDATE careers SET current_date = '2099-01-01' WHERE id = ?")) {
            corrupt.setLong(1, copyId);
            corrupt.executeUpdate();
        }
        CareerRepairService.RepairReport repaired = new CareerRepairService().repair(copyId);
        assertTrue(repaired.dateAdjusted());
        assertEquals(season.getEndDate(), repaired.date());
    }

    @Test
    void loadingCareerRepairsMissingActiveContracts() throws Exception {
        DatabaseInitializer.resetAndSeedForTests();
        Team team = new TeamRepository().findByShortName("RMA");
        Season season = new SeasonRepository().findFirst();
        CareerService service = new CareerService(new CareerRepository(),
                new TeamRepository(), new SeasonRepository());
        Career career = service.createCareer("Repair contracts", team.getId(), season.getId());
        Player player = new PlayerRepository().findAll().stream()
                .filter(candidate -> {
                    Long currentTeam = new PlayerTeamRepository()
                            .findCurrentTeamId(candidate.getId());
                    return currentTeam != null && currentTeam != team.getId();
                })
                .findFirst().orElseThrow();
        try (Connection connection = Database.getConnection();
             PreparedStatement delete = connection.prepareStatement(
                     "DELETE FROM career_contracts WHERE career_id = ? AND player_id = ?")) {
            delete.setLong(1, career.getId());
            delete.setLong(2, player.getId());
            delete.executeUpdate();
        }
        assertNull(new ContractRepository().findActiveByPlayer(player.getId()));

        service.loadCareer(career.getId());

        assertNotNull(new ContractRepository().findActiveByPlayer(player.getId()));
        new PlayerMarketRepository().listForTransfer(player.getId(), 1_000_000);
        TransferOfferService offers = new TransferOfferService();
        TransferOffer offer = offers.makeOffer(player.getId(), team.getId(),
                1_000_000, LocalDate.of(2026, 8, 20));
        offers.evaluate(offer.getId());
        new TransferExecutionService().completeTransfer(offer.getId(), player.getSalary(),
                LocalDate.of(2029, 8, 20), season.getId(), LocalDate.of(2026, 8, 20));
        assertEquals(team.getId(), new PlayerTeamRepository()
                .findCurrentTeamId(player.getId()));
    }

    @Test
    void transferHistoryAndOffersBelongOnlyToTheirCareer() throws Exception {
        DatabaseInitializer.resetAndSeedForTests();
        Team seller = new TeamRepository().findByShortName("ARS");
        Team buyer = new TeamRepository().findByShortName("RMA");
        Season season = new SeasonRepository().findFirst();
        Player player = new PlayerRepository().findCurrentPlayersByTeam(seller.getId()).getFirst();
        CareerService careers = new CareerService(new CareerRepository(),
                new TeamRepository(), new SeasonRepository());

        Career first = careers.createCareer("Mercado uno", buyer.getId(), season.getId());
        int originalOverall = player.getOverall();
        Player developed = new PlayerRepository().findById(player.getId());
        developed.setOverall(originalOverall + 1);
        new PlayerRepository().updateDevelopment(developed);
        new PlayerSeasonStatsRepository().recordAppearance(player.getId(), season.getId(),
                true, 90, 1, 0, 0, 0, 8.2);
        double originalBudget = new ClubFinanceRepository().findByTeam(buyer.getId())
                .getTransferBudget();
        new ClubFinanceRepository().spendTransferBudget(buyer.getId(), 1_000_000);
        Contract originalContract = new ContractRepository().findActiveByPlayer(player.getId());
        LocalDate originalEnd = originalContract.getEndDate();
        double originalSalary = originalContract.getSalary();
        new ContractRenewalService().renew(player.getId(), seller.getId(),
                originalEnd.plusYears(1), originalSalary + 1_000);
        PlayerState firstState = new PlayerStateRepository().findByPlayer(player.getId());
        firstState.setFitness(12); firstState.setMorale(34); firstState.setForm(56);
        new PlayerStateRepository().update(firstState);
        new PlayerMarketRepository().listForTransfer(player.getId(), 25_000_000);
        assertEquals(25_000_000, new PlayerMarketRepository().findAskingPrice(player.getId()));
        new PlayerTeamRepository().transferPlayer(player.getId(), buyer.getId(),
                LocalDate.of(2026, 8, 21));
        assertEquals(buyer.getId(), new PlayerTeamRepository().findCurrentTeamId(player.getId()));
        TransferOffer offer = new TransferOffer();
        offer.setPlayer(player); offer.setBuyingTeam(buyer); offer.setSellingTeam(seller);
        offer.setAmount(20_000_000); offer.setOfferDate(LocalDate.of(2026, 8, 20));
        offer.setStatus(TransferOfferStatus.PENDING);
        new TransferOfferRepository().save(offer);
        insertTransfer(first.getId(), offer.getId(), player.getId(), seller.getId(),
                buyer.getId(), season.getId());
        assertEquals(1, new TransferRepository().findByTeam(buyer.getId()).size());
        assertEquals(1, new TransferOfferRepository().findPendingBySellingTeam(seller.getId()).size());

        Career second = careers.createCareer("Mercado dos", buyer.getId(), season.getId());
        assertEquals(originalOverall, new PlayerRepository().findById(player.getId()).getOverall());
        assertEquals(0, new PlayerSeasonStatsRepository().find(player.getId(), season.getId())
                .getAppearances());
        assertEquals(originalBudget, new ClubFinanceRepository().findByTeam(buyer.getId())
                .getTransferBudget());
        assertEquals(originalEnd, new ContractRepository().findActiveByPlayer(player.getId())
                .getEndDate());
        assertEquals(100, new PlayerStateRepository().findByPlayer(player.getId()).getFitness());
        assertNull(new PlayerMarketRepository().findAskingPrice(player.getId()));
        assertEquals(seller.getId(), new PlayerTeamRepository().findCurrentTeamId(player.getId()));
        assertTrue(new TransferRepository().findByTeam(buyer.getId()).isEmpty());
        assertTrue(new TransferOfferRepository().findPendingBySellingTeam(seller.getId()).isEmpty());
        assertNull(new TransferOfferRepository().findById(offer.getId()));

        careers.loadCareer(first.getId());
        assertEquals(originalOverall + 1,
                new PlayerRepository().findById(player.getId()).getOverall());
        assertEquals(1, new PlayerSeasonStatsRepository().find(player.getId(), season.getId())
                .getAppearances());
        assertEquals(originalBudget - 1_000_000,
                new ClubFinanceRepository().findByTeam(buyer.getId()).getTransferBudget());
        assertEquals(originalEnd.plusYears(1),
                new ContractRepository().findActiveByPlayer(player.getId()).getEndDate());
        assertEquals(12, new PlayerStateRepository().findByPlayer(player.getId()).getFitness());
        assertEquals(25_000_000, new PlayerMarketRepository().findAskingPrice(player.getId()));
        assertEquals(buyer.getId(), new PlayerTeamRepository().findCurrentTeamId(player.getId()));
        assertEquals(1, new TransferRepository().findByTeam(buyer.getId()).size());
        assertNotNull(new TransferOfferRepository().findById(offer.getId()));

        careers.loadCareer(second.getId());
        assertEquals(originalOverall, new PlayerRepository().findById(player.getId()).getOverall());
        assertEquals(originalBudget, new ClubFinanceRepository().findByTeam(buyer.getId())
                .getTransferBudget());
        assertEquals(100, new PlayerStateRepository().findByPlayer(player.getId()).getFitness());
        assertEquals(seller.getId(), new PlayerTeamRepository().findCurrentTeamId(player.getId()));
        assertTrue(new TransferRepository().findByTeam(buyer.getId()).isEmpty());
    }

    private void insertTransfer(long careerId, long offerId, long playerId,
            long sellerId, long buyerId, long seasonId) throws Exception {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO transfers (career_id, player_id, from_team_id, to_team_id,
                         amount, transfer_date, season_id, offer_id)
                     VALUES (?, ?, ?, ?, 20000000, '2026-08-21', ?, ?)
                     """)) {
            statement.setLong(1, careerId); statement.setLong(2, playerId);
            statement.setLong(3, sellerId); statement.setLong(4, buyerId);
            statement.setLong(5, seasonId); statement.setLong(6, offerId);
            statement.executeUpdate();
        }
    }
}
