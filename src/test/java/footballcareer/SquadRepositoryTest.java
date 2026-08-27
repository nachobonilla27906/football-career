package footballcareer;

import footballcareer.database.*;
import footballcareer.model.Player;
import footballcareer.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SquadRepositoryTest {
    private PlayerRepository playerRepository;
    private PlayerTeamRepository playerTeamRepository;
    private Team arsenal;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
        playerRepository = new PlayerRepository();
        playerTeamRepository = new PlayerTeamRepository();
        arsenal = new TeamRepository().findByShortName("ARS");
    }

    @Test
    void shouldFindCompleteCurrentSquad() {
        List<Player> squad =
                playerRepository.findCurrentPlayersByTeam(arsenal.getId());

        assertEquals(28, squad.size());
        assertTrue(squad.stream().anyMatch(player ->
                player.getFullName().contains("Saka")));
        assertTrue(squad.stream().allMatch(player -> player.getOverall() > 0));
    }

    @Test
    void shouldStopReturningTransferredPlayerForFormerClub() {
        Team liverpool = new TeamRepository().findByShortName("LIV");
        Player player = playerRepository
                .findCurrentPlayersByTeam(arsenal.getId()).getFirst();

        playerTeamRepository.transferPlayer(
                player.getId(), liverpool.getId(), LocalDate.of(2027, 1, 1)
        );

        assertFalse(playerRepository.findCurrentPlayersByTeam(arsenal.getId())
                .stream().anyMatch(found -> found.getId() == player.getId()));
        assertTrue(playerRepository.findCurrentPlayersByTeam(liverpool.getId())
                .stream().anyMatch(found -> found.getId() == player.getId()));
    }
}
