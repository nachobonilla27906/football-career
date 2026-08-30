package footballcareer;

import footballcareer.database.*;
import footballcareer.model.Team;
import footballcareer.model.Transfer;
import footballcareer.model.Player;
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
        DatabaseInitializer.resetAndSeedForTests();
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

    @Test
    void shouldSendPendingOfferForControlledListedPlayer() {
        Player player = new PlayerRepository()
                .findCurrentPlayersByTeam(controlledTeam.getId()).stream()
                .filter(candidate -> candidate.getPosition()
                        != footballcareer.model.enums.Position.GK)
                .findFirst().orElseThrow();
        new PlayerMarketRepository().listForTransfer(player.getId(), 5_000_000);

        service.processMarket(LocalDate.of(2026, 8, 17),
                seasonId, controlledTeam.getId());

        List<footballcareer.model.TransferOffer> incoming =
                new TransferOfferRepository()
                        .findPendingBySellingTeam(controlledTeam.getId());
        assertTrue(incoming.stream().anyMatch(offer ->
                offer.getPlayer().getId() == player.getId()));
        assertEquals(controlledTeam.getId(),
                new PlayerTeamRepository().findCurrentTeamId(player.getId()));
    }

    @Test
    void shouldProvideAPositionallyVariedInitialMarket() {
        service.ensureMarketSupply(controlledTeam.getId());

        List<Player> listed = new PlayerMarketRepository()
                .findTransferListed(controlledTeam.getId());
        long goalkeepers = listed.stream().filter(player -> player.getPosition()
                == footballcareer.model.enums.Position.GK).count();
        long defenders = listed.stream().filter(player -> java.util.Set.of(
                footballcareer.model.enums.Position.CB,
                footballcareer.model.enums.Position.LB,
                footballcareer.model.enums.Position.RB).contains(player.getPosition())).count();
        long attackers = listed.stream().filter(player -> java.util.Set.of(
                footballcareer.model.enums.Position.LW,
                footballcareer.model.enums.Position.RW,
                footballcareer.model.enums.Position.ST).contains(player.getPosition())).count();
        assertTrue(goalkeepers > 0);
        assertTrue(defenders > 0);
        assertTrue(attackers > 0);

        java.util.Map<Long, Long> currentTeams = new PlayerTeamRepository()
                .findAllCurrentTeamIds();
        long globalCandidates = new PlayerRepository().findAll().stream()
                .filter(player -> currentTeams.containsKey(player.getId()))
                .filter(player -> currentTeams.get(player.getId()) != controlledTeam.getId())
                .count();
        assertTrue(globalCandidates > listed.size());
        assertFalse(new CompetitionTeamRepository().findLeagueNamesByTeam(seasonId).isEmpty());
    }
}
