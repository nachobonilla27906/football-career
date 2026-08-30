package footballcareer;

import footballcareer.database.MatchTacticsRepository.TacticalSetup;
import footballcareer.model.MatchEvent;
import footballcareer.model.Player;
import footballcareer.model.Team;
import footballcareer.model.enums.MatchEventType;
import footballcareer.service.LiveMatchNarrator;
import footballcareer.service.LiveTacticalMomentumService;
import footballcareer.service.LivePitchPositionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LiveMatchExperienceTest {
    @Test
    void commentaryUsesScoreTimeAndAssisterContext() {
        Team team = new Team();
        team.setShortName("ATM");
        Player scorer = player(7, "Julián", "Álvarez");
        Player assister = player(11, "Antoine", "Griezmann");
        MatchEvent goal = event(19, 86, MatchEventType.GOAL, team, scorer, assister);

        String text = new LiveMatchNarrator().describe(goal, scorer, assister,
                1, 2, true);

        assertTrue(text.contains("Griezmann"));
        assertTrue(text.contains("Empata"));
        assertNotEquals(text, new LiveMatchNarrator().describe(
                event(20, 86, MatchEventType.GOAL, team, scorer, assister),
                scorer, assister, 1, 2, true));
    }

    @Test
    void tacticalAssessmentReflectsUrgencyAndLateFatigue() {
        LiveTacticalMomentumService service = new LiveTacticalMomentumService();
        var aggressive = service.assess(
                new TacticalSetup("4-3-3", "ATTACKING", "HIGH", "FAST"),
                78, -1);
        var conservative = service.assess(
                new TacticalSetup("4-3-3", "DEFENSIVE", "LOW", "SLOW"),
                78, 1);

        assertTrue(aggressive.momentum() > conservative.momentum());
        assertEquals("Riesgo alto de fatiga", aggressive.risk());
        assertEquals("BLOQUE BAJO", conservative.label());
    }

    @Test
    void pitchMirrorsAttacksAndPlacesGoalsInsideTheRivalArea() {
        Team team = new Team(); team.setShortName("ATM");
        MatchEvent goal = event(9, 72, MatchEventType.GOAL, team,
                player(7, "Julián", "Álvarez"), null);
        var home = new LivePitchPositionService().position(goal, true);
        var away = new LivePitchPositionService().position(goal, false);
        assertEquals("ÁREA RIVAL", home.zone());
        assertTrue(home.x() > 0.8);
        assertEquals(1, home.x() + away.x(), 0.001);
        assertTrue(home.y() >= 0.2 && home.y() <= 0.8);
    }

    private MatchEvent event(long id, int minute, MatchEventType type, Team team,
            Player player, Player secondary) {
        MatchEvent event = new MatchEvent();
        event.setId(id);
        event.setMinute(minute);
        event.setType(type);
        event.setTeam(team);
        event.setPlayer(player);
        event.setSecondaryPlayer(secondary);
        return event;
    }

    private Player player(long id, String firstName, String lastName) {
        Player player = new Player();
        player.setId(id);
        player.setFirstName(firstName);
        player.setLastName(lastName);
        return player;
    }
}
