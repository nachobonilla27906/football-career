package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.service.CareerService;
import footballcareer.service.TrainingService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrainingServiceTest {

    @Test
    void trainingChangesSquadStateAndCanOnlyBeDoneOncePerDay() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
        Season season = new SeasonRepository().findFirst();
        Team team = new TeamRepository().findByShortName("LIV");
        Career career = new CareerService(new CareerRepository(), new TeamRepository(),
                new SeasonRepository()).createCareer("Training Test", team.getId(), season.getId());
        Player player = new PlayerRepository().findCurrentPlayersByTeam(team.getId()).getFirst();
        PlayerStateRepository states = new PlayerStateRepository();
        PlayerState before = states.findByPlayer(player.getId());
        TrainingService service = new TrainingService();

        TrainingService.TrainingResult result = service.train(
                career, TrainingService.TrainingType.BALANCED);
        PlayerState after = states.findByPlayer(player.getId());

        assertTrue(result.affectedPlayers() > 0);
        assertEquals(TrainingService.TrainingType.BALANCED, service.findToday(career));
        assertEquals(TrainingService.TrainingType.BALANCED,
                service.findByMonth(career, java.time.YearMonth.from(career.getCurrentDate()))
                        .get(career.getCurrentDate()));
        assertEquals(Math.min(100, before.getForm() + 2), after.getForm());
        assertEquals(Math.max(0, before.getFitness() - 3), after.getFitness());
        assertEquals(Math.min(100, before.getMorale() + 1), after.getMorale());
        assertThrows(IllegalStateException.class, () -> service.train(
                career, TrainingService.TrainingType.INTENSIVE));
    }
}
