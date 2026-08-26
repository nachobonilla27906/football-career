package footballcareer;

import footballcareer.database.DatabaseInitializer;
import footballcareer.database.TeamRepository;
import footballcareer.model.Team;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TeamRepositoryTest {

    private TeamRepository teamRepository;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.initialize();
        teamRepository = new TeamRepository();
    }

    @Test
    void shouldSaveAndFindTeam() {

        Team team = new Team();

        team.setName("Valencia CF");
        team.setShortName("VCF");
        team.setCountry("Spain");
        team.setStadiumName("Mestalla");
        team.setStadiumCapacity(49430);
        team.setReputation(80);

        teamRepository.save(team);

        assertTrue(team.getId() > 0);

        Team loadedTeam = teamRepository.findById(team.getId());

        assertNotNull(loadedTeam);
        assertEquals(team.getId(), loadedTeam.getId());
        assertEquals("Valencia CF", loadedTeam.getName());
        assertEquals("VCF", loadedTeam.getShortName());
        assertEquals("Spain", loadedTeam.getCountry());
        assertEquals("Mestalla", loadedTeam.getStadiumName());
        assertEquals(49430, loadedTeam.getStadiumCapacity());
        assertEquals(80, loadedTeam.getReputation());
    }
}