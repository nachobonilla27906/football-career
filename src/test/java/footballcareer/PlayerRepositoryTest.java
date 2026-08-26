package footballcareer;

import footballcareer.database.DatabaseInitializer;
import footballcareer.database.PlayerRepository;
import footballcareer.model.Player;
import footballcareer.model.enums.Position;
import footballcareer.model.enums.PreferredFoot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PlayerRepositoryTest {

    private PlayerRepository repository;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.initialize();
        repository = new PlayerRepository();
    }

    @Test
    void shouldSaveAndFindPlayer() {

        Player player = new Player(
                0,
                "Lamine",
                "Yamal",
                LocalDate.of(2007, 7, 13),
                "Spain",
                Position.RW,
                PreferredFoot.LEFT,
                91,
                96,
                90,
                86,
                88,
                94,
                35,
                70,
                150_000_000,
                10_000_000
        );

        repository.save(player);

        Player savedPlayer = repository.findById(1);

        assertNotNull(savedPlayer);

        assertEquals("Lamine", savedPlayer.getFirstName());
        assertEquals("Yamal", savedPlayer.getLastName());

        assertEquals(
                LocalDate.of(2007, 7, 13),
                savedPlayer.getBirthDate()
        );

        assertEquals("Spain", savedPlayer.getNationality());

        assertEquals(Position.RW, savedPlayer.getPosition());
        assertEquals(
                PreferredFoot.LEFT,
                savedPlayer.getPreferredFoot()
        );

        assertEquals(91, savedPlayer.getOverall());
        assertEquals(96, savedPlayer.getPotential());

        assertEquals(90, savedPlayer.getPace());
        assertEquals(86, savedPlayer.getShooting());
        assertEquals(88, savedPlayer.getPassing());
        assertEquals(94, savedPlayer.getDribbling());
        assertEquals(35, savedPlayer.getDefending());
        assertEquals(70, savedPlayer.getPhysical());

        assertEquals(150_000_000, savedPlayer.getMarketValue());
        assertEquals(10_000_000, savedPlayer.getSalary());
    }
}