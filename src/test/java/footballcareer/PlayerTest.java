package footballcareer;

import footballcareer.model.Player;
import footballcareer.model.enums.Position;
import footballcareer.model.enums.PreferredFoot;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void shouldCreatePlayer() {

        Player player = new Player(
                1,
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

        assertEquals(1, player.getId());
        assertEquals("Lamine", player.getFirstName());
        assertEquals("Yamal", player.getLastName());
        assertEquals("Lamine Yamal", player.getFullName());

        assertEquals(
                LocalDate.of(2007, 7, 13),
                player.getBirthDate()
        );

        assertEquals(
                19,
                player.getAge(LocalDate.of(2026, 8, 1))
        );

        assertEquals("Spain", player.getNationality());

        assertEquals(Position.RW, player.getPosition());
        assertEquals(PreferredFoot.LEFT, player.getPreferredFoot());

        assertEquals(91, player.getOverall());
        assertEquals(96, player.getPotential());

        assertEquals(90, player.getPace());
        assertEquals(86, player.getShooting());
        assertEquals(88, player.getPassing());
        assertEquals(94, player.getDribbling());
        assertEquals(35, player.getDefending());
        assertEquals(70, player.getPhysical());

        assertEquals(150_000_000, player.getMarketValue());
        assertEquals(10_000_000, player.getSalary());
    }
}