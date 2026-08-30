package footballcareer;

import footballcareer.database.CareerRepository;
import footballcareer.database.ClubFinanceRepository;
import footballcareer.database.DatabaseInitializer;
import footballcareer.database.SeasonRepository;
import footballcareer.database.TeamRepository;
import footballcareer.model.Career;
import footballcareer.model.Season;
import footballcareer.model.Team;
import footballcareer.service.CareerService;
import footballcareer.service.StaffService;
import footballcareer.service.TrainingService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StaffServiceTest {
    @Test
    void technicalStaffCostsMoneyAndImprovesTraining() {
        DatabaseInitializer.resetAndSeedForTests();
        TeamRepository teams = new TeamRepository();
        Team arsenal = teams.findByShortName("ARS");
        Season season = new SeasonRepository().findFirst();
        Career career = new CareerService(new CareerRepository(), teams,
                new SeasonRepository()).createCareer("Staff Test", arsenal.getId(), season.getId());
        ClubFinanceRepository finances = new ClubFinanceRepository();
        double before = finances.findByTeam(arsenal.getId()).getTransferBudget();
        StaffService staff = new StaffService();

        staff.hire(career, StaffService.Role.COACH, "Marta León", 4);

        assertEquals(4, staff.level(career.getId(), StaffService.Role.COACH));
        assertEquals(before - StaffService.hiringCost(4),
                finances.findByTeam(arsenal.getId()).getTransferBudget());
        TrainingService.TrainingResult result = new TrainingService().train(
                career, TrainingService.TrainingType.BALANCED);
        assertEquals(4, result.formChange());
    }
}
