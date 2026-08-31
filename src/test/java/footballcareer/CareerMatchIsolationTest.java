package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.service.CareerService;
import footballcareer.service.FootballWorldService;
import footballcareer.service.LineupService;
import footballcareer.model.enums.MatchEventType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CareerMatchIsolationTest {

    @Test
    void aNewCareerNeverInheritsPlayedMatchesFromAnotherCareerOrLegacyWorld() {
        DatabaseInitializer.resetAndSeedForTests();
        Season season = new SeasonRepository().findFirst();
        new FootballWorldService().prepareSeason(season.getId());
        Team madrid = new TeamRepository().findByShortName("RMA");
        Competition competition = new CompetitionTeamRepository()
                .findCompetitionsByTeam(madrid.getId()).getFirst();
        CareerService service = new CareerService(new CareerRepository(),
                new TeamRepository(), new SeasonRepository());
        MatchRepository matches = new MatchRepository();

        Career first = service.createCareer("Primera", madrid.getId(), season.getId());
        Match nextForActiveCareer = matches.findNextForTeam(madrid.getId(), season.getId(),
                first.getCurrentDate());
        assertNotNull(nextForActiveCareer);
        assertTrue(nextForActiveCareer.getHomeTeam().getId() == madrid.getId()
                || nextForActiveCareer.getAwayTeam().getId() == madrid.getId());
        Match firstFixture = matches.findByCompetition(competition.getId()).getFirst();
        firstFixture.setResult(3, 1);
        matches.updateResult(firstFixture);
        new MatchTacticsRepository().saveFormation(firstFixture.getId(), madrid.getId(), "4-4-2");
        Player scorer = new PlayerRepository().findCurrentPlayersByTeam(
                firstFixture.getHomeTeam().getId()).getFirst();
        MatchEvent goal = new MatchEvent();
        goal.setMatch(firstFixture); goal.setTeam(firstFixture.getHomeTeam());
        goal.setPlayer(scorer); goal.setMinute(24); goal.setType(MatchEventType.GOAL);
        new MatchEventRepository().save(goal);
        MatchTeamStats stats = new MatchTeamStats();
        stats.setMatch(firstFixture); stats.setTeam(firstFixture.getHomeTeam());
        stats.setPossession(55); stats.setShots(10); stats.setShotsOnTarget(5);
        stats.setCorners(4); stats.setFouls(8); stats.setYellowCards(1); stats.setRedCards(0);
        new MatchTeamStatsRepository().save(stats);
        MatchLineup lineup = new LineupService(new PlayerRepository(),
                new PlayerStateRepository()).selectMatchLineup(firstFixture.getHomeTeam().getId());
        new MatchLineupRepository().save(firstFixture.getId(), firstFixture.getHomeTeam().getId(),
                lineup.getStarters(), lineup.getSubstitutes());
        assertTrue(matches.findById(firstFixture.getId()).isPlayed());
        assertEquals(1, new LeagueStandingRepository()
                .findByCompetition(competition.getId()).stream()
                .filter(row -> row.getTeam().getId() == firstFixture.getHomeTeam().getId())
                .findFirst().orElseThrow().getPlayed());

        Career second = service.createCareer("Segunda", madrid.getId(), season.getId());
        assertFalse(matches.findById(firstFixture.getId()).isPlayed());
        assertEquals("4-3-3", new MatchTacticsRepository()
                .findFormation(firstFixture.getId(), madrid.getId()));
        assertTrue(new MatchEventRepository().findByMatch(firstFixture.getId()).isEmpty());
        assertTrue(new MatchTeamStatsRepository().findByMatch(firstFixture.getId()).isEmpty());
        assertNull(new MatchLineupRepository().find(firstFixture.getId(),
                firstFixture.getHomeTeam().getId()));
        assertTrue(new LeagueStandingRepository().findByCompetition(competition.getId())
                .stream().allMatch(row -> row.getPlayed() == 0 && row.getPoints() == 0));

        service.loadCareer(first.getId());
        Match restoredFirst = matches.findById(firstFixture.getId());
        assertTrue(restoredFirst.isPlayed());
        assertEquals(3, restoredFirst.getHomeGoals());
        assertEquals("4-4-2", new MatchTacticsRepository()
                .findFormation(firstFixture.getId(), madrid.getId()));
        assertEquals(1, new MatchEventRepository().findByMatch(firstFixture.getId()).size());
        assertEquals(1, new MatchTeamStatsRepository().findByMatch(firstFixture.getId()).size());
        assertNotNull(new MatchLineupRepository().find(firstFixture.getId(),
                firstFixture.getHomeTeam().getId()));
        assertEquals(3, new LeagueStandingRepository()
                .findByCompetition(competition.getId()).stream()
                .filter(row -> row.getTeam().getId() == restoredFirst.getHomeTeam().getId())
                .findFirst().orElseThrow().getPoints());

        service.loadCareer(second.getId());
        assertFalse(matches.findById(firstFixture.getId()).isPlayed());
    }
}
