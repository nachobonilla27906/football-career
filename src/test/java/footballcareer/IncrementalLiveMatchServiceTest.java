package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.service.*;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class IncrementalLiveMatchServiceTest {
    @Test
    void resultDoesNotExistUntilIncrementalSessionFinishes() {
        DatabaseInitializer.resetAndSeedForTests();
        Season season = new SeasonRepository().findFirst();
        new FootballWorldService().prepareSeason(season.getId());
        Team team = new TeamRepository().findByShortName("LIV");
        Career career = new CareerService(new CareerRepository(), new TeamRepository(),
                new SeasonRepository()).createCareer("Live", team.getId(), season.getId());
        Match match = new CompetitionTeamRepository().findCompetitionsByTeam(team.getId()).stream()
                .flatMap(competition -> new MatchRepository().findByCompetition(
                        competition.getId()).stream())
                .filter(candidate -> candidate.getHomeTeam().getId() == team.getId()
                        || candidate.getAwayTeam().getId() == team.getId()).findFirst().orElseThrow();
        var session = new IncrementalLiveMatchService(new Random(713)).start(match.getId());

        session.advanceTo(45);

        assertFalse(new MatchRepository().findById(match.getId()).isPlayed());
        assertEquals(45, session.minute());
        assertTrue(session.homeStats().getPasses() > 0);
        var attacking = new MatchTacticsRepository.TacticalSetup(
                "4-3-3", "ATTACKING", "HIGH", "FAST");
        session.updateTactics(team.getId(), attacking);
        session.advanceTo(80);
        assertFalse(new MatchRepository().findById(match.getId()).isPlayed());

        session.finish();

        Match completed = new MatchRepository().findById(match.getId());
        assertTrue(completed.isPlayed());
        assertEquals(90, session.minute());
        assertEquals(2, new MatchTeamStatsRepository().findByMatch(match.getId()).size());
        assertNotNull(new MatchReportService().build(match.getId()));
        assertEquals(session.events().size(), new MatchEventRepository()
                .findByMatch(match.getId()).size());
        assertEquals(career.getId(), CareerContext.getCareerId());
    }
}
