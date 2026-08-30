package footballcareer;

import footballcareer.database.DatabaseInitializer;
import footballcareer.database.PlayerRepository;
import footballcareer.database.PlayerTeamRepository;
import footballcareer.database.TeamRepository;
import footballcareer.model.Player;
import footballcareer.model.Team;
import footballcareer.model.enums.Position;
import footballcareer.model.enums.PreferredFoot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTeamRepositoryTest {

    private PlayerTeamRepository repository;
    private PlayerRepository playerRepository;
    private TeamRepository teamRepository;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();

        repository = new PlayerTeamRepository();
        playerRepository = new PlayerRepository();
        teamRepository = new TeamRepository();
    }

    @Test
    void shouldAssignPlayerToTeam() {

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

        playerRepository.save(player);

        Team team = new Team();

        team.setName("Barcelona");
        team.setShortName("BAR");
        team.setCountry("Spain");
        team.setStadiumName("Spotify Camp Nou");
        team.setStadiumCapacity(99354);
        team.setReputation(94);

        teamRepository.save(team);

        LocalDate startDate =
                LocalDate.of(2026, 8, 15);

        repository.assignPlayerToTeam(
                player.getId(),
                team.getId(),
                startDate
        );

        Long currentTeamId =
                repository.findCurrentTeamId(player.getId());

        assertNotNull(currentTeamId);
        assertEquals(team.getId(), currentTeamId);
        assertEquals(team.getId(), repository.findAllCurrentTeamIds().get(player.getId()));
    }

    @Test
    void shouldTransferPlayerToAnotherTeam() {

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

        playerRepository.save(player);

        Team firstTeam = new Team();

        firstTeam.setName("Barcelona");
        firstTeam.setShortName("BAR");
        firstTeam.setCountry("Spain");
        firstTeam.setStadiumName("Spotify Camp Nou");
        firstTeam.setStadiumCapacity(99354);
        firstTeam.setReputation(94);

        teamRepository.save(firstTeam);

        Team secondTeam = new Team();

        secondTeam.setName("Real Madrid");
        secondTeam.setShortName("RMA");
        secondTeam.setCountry("Spain");
        secondTeam.setStadiumName("Santiago Bernabéu");
        secondTeam.setStadiumCapacity(83186);
        secondTeam.setReputation(95);

        teamRepository.save(secondTeam);

        LocalDate firstStartDate =
                LocalDate.of(2026, 8, 15);

        repository.assignPlayerToTeam(
                player.getId(),
                firstTeam.getId(),
                firstStartDate
        );

        LocalDate transferDate =
                LocalDate.of(2027, 7, 1);

        repository.transferPlayer(
                player.getId(),
                secondTeam.getId(),
                transferDate
        );

        Long currentTeamId =
                repository.findCurrentTeamId(player.getId());

        assertNotNull(currentTeamId);
        assertEquals(secondTeam.getId(), currentTeamId);
    }
}
