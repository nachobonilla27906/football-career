package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.service.CareerService;
import footballcareer.service.TransferHistoryService;
import footballcareer.service.TransferOfferService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TransferHistoryServiceTest {
    @Test
    void negotiationHistoryProvidesStructuredRowsScopedToCareer() {
        DatabaseInitializer.resetAndSeedForTests();
        Team buyer = new TeamRepository().findByShortName("VCF");
        Team seller = new TeamRepository().findByShortName("ARS");
        Season season = new SeasonRepository().findFirst();
        Career career = new CareerService(new CareerRepository(), new TeamRepository(),
                new SeasonRepository()).createCareer("History", buyer.getId(), season.getId());
        Player player = new PlayerRepository().findCurrentPlayersByTeam(seller.getId()).getFirst();
        TransferOfferService offers = new TransferOfferService();
        var offer = offers.makeOffer(player.getId(), buyer.getId(), 1_000_000,
                LocalDate.of(2026, 8, 20));
        offers.evaluate(offer.getId());

        TransferHistoryService service = new TransferHistoryService();
        var rows = service.negotiations(career.getControlledTeam().getId());

        assertEquals(1, rows.size());
        assertEquals("ENVIADA", rows.getFirst().direction());
        assertEquals(player.getFullName(), rows.getFirst().player());
        assertEquals("REJECTED", rows.getFirst().status());
        assertTrue(service.completed(career.getControlledTeam().getId()).isEmpty());
    }
}
