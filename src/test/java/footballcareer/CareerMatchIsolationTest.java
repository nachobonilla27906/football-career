package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.service.CareerService;
import footballcareer.service.FootballWorldService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CareerMatchIsolationTest {

    @Test
    void aNewCareerNeverInheritsPlayedMatchesFromAnotherCareerOrLegacyWorld() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
        Season season = new SeasonRepository().findFirst();
        new FootballWorldService().prepareSeason(season.getId());
        Team madrid = new TeamRepository().findByShortName("RMA");
        Competition competition = new CompetitionTeamRepository()
                .findCompetitionsByTeam(madrid.getId()).getFirst();
        CareerService service = new CareerService(new CareerRepository(),
                new TeamRepository(), new SeasonRepository());
        MatchRepository matches = new MatchRepository();

        Career first = service.createCareer("Primera", madrid.getId(), season.getId());
        Match firstFixture = matches.findByCompetition(competition.getId()).getFirst();
        firstFixture.setResult(3, 1);
        matches.updateResult(firstFixture);
        assertTrue(matches.findById(firstFixture.getId()).isPlayed());

        Career second = service.createCareer("Segunda", madrid.getId(), season.getId());
        assertFalse(matches.findById(firstFixture.getId()).isPlayed());

        service.loadCareer(first.getId());
        Match restoredFirst = matches.findById(firstFixture.getId());
        assertTrue(restoredFirst.isPlayed());
        assertEquals(3, restoredFirst.getHomeGoals());

        service.loadCareer(second.getId());
        assertFalse(matches.findById(firstFixture.getId()).isPlayed());
    }
}
