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
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
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
}
