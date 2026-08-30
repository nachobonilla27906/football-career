package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.model.enums.TransferOfferStatus;
import footballcareer.service.TransferOfferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class TransferOfferServiceTest {
    private Player player;
    private Team valencia;
    private TransferOfferService service;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetAndSeedForTests();
        Team arsenal = new TeamRepository().findByShortName("ARS");
        valencia = new TeamRepository().findByShortName("VCF");
        player = new PlayerRepository().findCurrentPlayersByTeam(arsenal.getId()).getFirst();
        service = new TransferOfferService();
    }

    @Test
    void shouldAcceptOfferMeetingListedPrice() {
        new PlayerMarketRepository().listForTransfer(player.getId(), 10_000_000);
        TransferOffer offer = service.makeOffer(player.getId(), valencia.getId(),
                10_000_000, LocalDate.of(2026, 8, 20));
        assertTrue(offer.getId() > 0);
        assertEquals(TransferOfferStatus.ACCEPTED,
                service.evaluate(offer.getId()).getStatus());
    }

    @Test
    void shouldRejectLowOfferAndOwnPlayerOffer() {
        TransferOffer low = service.makeOffer(player.getId(), valencia.getId(),
                1_000_000, LocalDate.of(2026, 8, 20));
        assertEquals(TransferOfferStatus.REJECTED,
                service.evaluate(low.getId()).getStatus());

        assertThrows(IllegalArgumentException.class, () ->
                service.makeOffer(player.getId(),
                        new TeamRepository().findByShortName("ARS").getId(),
                        1_000_000, LocalDate.of(2026, 8, 20)));
    }

    @Test
    void unlistedPlayerRequiresASubstantialPremium() {
        TransferOfferService.NegotiationQuote quote = service.quote(player.getId());

        assertFalse(quote.transferListed());
        assertTrue(quote.requiredAmount() >= player.getMarketValue() * 1.35);
        TransferOffer regularValue = service.makeOffer(player.getId(), valencia.getId(),
                player.getMarketValue(), LocalDate.of(2026, 8, 20));
        assertEquals(TransferOfferStatus.REJECTED,
                service.evaluate(regularValue.getId()).getStatus());
    }

    @Test
    void offersAreBlockedOutsideTransferWindows() {
        assertThrows(IllegalStateException.class, () -> service.makeOffer(
                player.getId(), valencia.getId(), player.getMarketValue(),
                LocalDate.of(2026, 10, 10)));
    }

    @Test
    void shouldCreateAndAcceptCounterOfferForCloseBid() {
        new PlayerMarketRepository().listForTransfer(player.getId(), 10_000_000);
        TransferOffer offer = service.makeOffer(player.getId(), valencia.getId(),
                8_500_000, LocalDate.of(2026, 8, 20));

        TransferOffer countered = service.evaluate(offer.getId());

        assertEquals(TransferOfferStatus.PENDING, countered.getStatus());
        assertNotNull(countered.getCounterAmount());
        assertTrue(countered.getCounterAmount() > offer.getAmount());
        assertEquals(TransferOfferStatus.ACCEPTED,
                service.acceptCounterOffer(offer.getId()).getStatus());
    }

    @Test
    void buyerCanNegotiateSeveralPersistentRoundsBeforeSellerWalksAway() {
        new PlayerMarketRepository().listForTransfer(player.getId(), 10_000_000);
        TransferOffer offer = service.makeOffer(player.getId(), valencia.getId(),
                8_500_000, LocalDate.of(2026, 8, 20));
        TransferOffer firstSellerResponse = service.evaluate(offer.getId());
        assertNotNull(firstSellerResponse.getCounterAmount());

        TransferOffer secondSellerResponse = service.submitBuyerCounter(
                offer.getId(), 9_000_000);
        assertNotNull(secondSellerResponse.getCounterAmount());
        TransferOffer thirdSellerResponse = service.submitBuyerCounter(
                offer.getId(), 9_300_000);
        assertNotNull(thirdSellerResponse.getCounterAmount());
        assertEquals(3, new TransferOfferRepository().countBuyerRounds(offer.getId()));

        assertThrows(IllegalStateException.class, () ->
                service.submitBuyerCounter(offer.getId(), 9_500_000));
        assertEquals(TransferOfferStatus.REJECTED,
                new TransferOfferRepository().findById(offer.getId()).getStatus());
    }

    @Test
    void sellingClubCanCounterAnIncomingOffer() {
        new PlayerMarketRepository().listForTransfer(player.getId(), 10_000_000);
        TransferOffer incoming = service.makeOffer(player.getId(), valencia.getId(),
                9_000_000, LocalDate.of(2026, 8, 20));

        TransferOffer accepted = service.respondWithCounterOffer(
                incoming.getId(), 10_000_000);

        assertEquals(TransferOfferStatus.ACCEPTED, accepted.getStatus());
        assertEquals(10_000_000, accepted.getAmount());
        assertThrows(IllegalStateException.class, () ->
                service.respondWithCounterOffer(incoming.getId(), 11_000_000));

        Player secondPlayer = new PlayerRepository()
                .findCurrentPlayersByTeam(new TeamRepository().findByShortName("ARS").getId())
                .stream().filter(candidate -> candidate.getId() != player.getId())
                .findFirst().orElseThrow();
        new PlayerMarketRepository().listForTransfer(secondPlayer.getId(), 10_000_000);
        TransferOffer secondIncoming = service.makeOffer(secondPlayer.getId(), valencia.getId(),
                9_000_000, LocalDate.of(2026, 8, 20));
        TransferOffer rejected = service.respondWithCounterOffer(
                secondIncoming.getId(), 20_000_000);

        assertEquals(TransferOfferStatus.REJECTED, rejected.getStatus());
        assertEquals(new TeamRepository().findByShortName("ARS").getId(),
                new PlayerTeamRepository().findCurrentTeamId(secondPlayer.getId()));
    }

    @Test
    void buyerCanCancelOnlyItsPendingOfferAndHistoryKeepsIt() {
        new PlayerMarketRepository().listForTransfer(player.getId(), 10_000_000);
        TransferOffer offer = service.makeOffer(player.getId(), valencia.getId(),
                8_500_000, LocalDate.of(2026, 8, 20));
        service.evaluate(offer.getId());
        TransferOfferRepository repository = new TransferOfferRepository();
        long sellerId = offer.getSellingTeam().getId();
        assertEquals(1, repository.countPendingBySellingTeam(sellerId));

        TransferOffer cancelled = service.cancelOffer(offer.getId(), valencia.getId());

        assertEquals(TransferOfferStatus.WITHDRAWN, cancelled.getStatus());
        assertEquals("CANCELLED_BY_BUYER", cancelled.getResolutionReason());
        assertEquals(0, repository.countPendingBySellingTeam(sellerId));
        assertTrue(repository.findByBuyingTeam(valencia.getId()).stream()
                .anyMatch(candidate -> candidate.getId() == offer.getId()));
        assertThrows(IllegalStateException.class,
                () -> service.cancelOffer(offer.getId(), valencia.getId()));
    }

    @Test
    void pendingOffersExpireAfterTheirSevenDayDeadline() {
        new PlayerMarketRepository().listForTransfer(player.getId(), 10_000_000);
        TransferOffer offer = service.makeOffer(player.getId(), valencia.getId(),
                8_500_000, LocalDate.of(2026, 8, 20));

        assertEquals(0, service.expireOffers(LocalDate.of(2026, 8, 27)));
        assertEquals(1, service.expireOffers(LocalDate.of(2026, 8, 28)));
        TransferOffer expired = new TransferOfferRepository().findById(offer.getId());
        assertEquals(TransferOfferStatus.WITHDRAWN, expired.getStatus());
        assertEquals("EXPIRED", expired.getResolutionReason());
        assertEquals(LocalDate.of(2026, 8, 27), expired.getResponseDeadline());
    }
}
