package footballcareer;

import footballcareer.database.*;
import footballcareer.model.Player;
import footballcareer.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerMarketRepositoryTest {
    private PlayerMarketRepository repository;
    private Team valencia;
    private Player arsenalPlayer;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
        repository = new PlayerMarketRepository();
        valencia = new TeamRepository().findByShortName("VCF");
        Team arsenal = new TeamRepository().findByShortName("ARS");
        arsenalPlayer = new PlayerRepository()
                .findCurrentPlayersByTeam(arsenal.getId()).getFirst();
    }

    @Test
    void shouldListAndFindPlayerForAnotherClub() {
        repository.listForTransfer(arsenalPlayer.getId(), 25_000_000);

        assertEquals(25_000_000, repository.findAskingPrice(arsenalPlayer.getId()));
        assertTrue(repository.findTransferListed(valencia.getId()).stream()
                .anyMatch(player -> player.getId() == arsenalPlayer.getId()));

        repository.removeFromTransferList(arsenalPlayer.getId());
        assertNull(repository.findAskingPrice(arsenalPlayer.getId()));
    }
}
