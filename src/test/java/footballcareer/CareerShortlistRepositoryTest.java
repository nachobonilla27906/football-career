package footballcareer;

import footballcareer.database.*;
import footballcareer.model.Career;
import footballcareer.model.Player;
import footballcareer.model.Season;
import footballcareer.model.Team;
import footballcareer.service.CareerService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CareerShortlistRepositoryTest {

    @Test
    void shortlistIsPersistedPerCareer() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
        Season season = new SeasonRepository().findFirst();
        Team liverpool = new TeamRepository().findByShortName("LIV");
        Career career = new CareerService(new CareerRepository(), new TeamRepository(),
                new SeasonRepository()).createCareer("Shortlist", liverpool.getId(), season.getId());
        Player target = new PlayerRepository().findCurrentPlayersByTeam(
                new TeamRepository().findByShortName("MCI").getId()).getFirst();
        CareerShortlistRepository repository = new CareerShortlistRepository();

        repository.add(career.getId(), target.getId(), career.getCurrentDate());
        assertTrue(repository.contains(career.getId(), target.getId()));
        assertTrue(repository.findPlayerIds(career.getId()).contains(target.getId()));

        repository.remove(career.getId(), target.getId());
        assertFalse(repository.contains(career.getId(), target.getId()));
    }
}
