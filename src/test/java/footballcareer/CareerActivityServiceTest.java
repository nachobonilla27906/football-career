package footballcareer;

import footballcareer.database.CareerRepository;
import footballcareer.database.DatabaseInitializer;
import footballcareer.database.SeasonRepository;
import footballcareer.database.TeamRepository;
import footballcareer.model.Career;
import footballcareer.model.Season;
import footballcareer.model.Team;
import footballcareer.service.CareerActivityService;
import footballcareer.service.CareerService;
import footballcareer.service.TrainingService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CareerActivityServiceTest {
    @Test
    void dashboardTimelineCombinesAndOrdersCareerEvents() {
        DatabaseInitializer.resetAndSeedForTests();
        TeamRepository teams = new TeamRepository();
        Team arsenal = teams.findByShortName("ARS");
        Season season = new SeasonRepository().findFirst();
        Career career = new CareerService(new CareerRepository(), teams,
                new SeasonRepository()).createCareer("Activity Test", arsenal.getId(), season.getId());
        new TrainingService().train(career, TrainingService.TrainingType.BALANCED);

        var activity = new CareerActivityService().recent(career, 10);

        assertTrue(activity.stream().anyMatch(item ->
                item.type() == CareerActivityService.Type.TRAINING));
        assertTrue(activity.stream().anyMatch(item ->
                item.type() == CareerActivityService.Type.REPUTATION));
        assertEquals(career.getCurrentDate(), activity.getFirst().date());
        assertTrue(new CareerActivityService().recent(career, 1).size() <= 1);
    }
}
