package footballcareer;

import footballcareer.database.*;
import footballcareer.model.Career;
import footballcareer.model.Season;
import footballcareer.model.Team;
import footballcareer.service.CareerInsightService;
import footballcareer.service.CareerService;
import footballcareer.service.FootballWorldService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CareerInsightServiceTest {

    @Test
    void dashboardInsightsReflectTheLiveCareerState() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
        Season season = new SeasonRepository().findFirst();
        new FootballWorldService().prepareSeason(season.getId());
        Team liverpool = new TeamRepository().findByShortName("LIV");
        Career career = new CareerService(new CareerRepository(), new TeamRepository(),
                new SeasonRepository()).createCareer("Insight Test", liverpool.getId(), season.getId());

        CareerInsightService service = new CareerInsightService();

        assertEquals(3, service.objectives(career).size());
        assertTrue(service.news(career).stream().anyMatch(news ->
                news.contains("Próximo partido")));
    }
}
