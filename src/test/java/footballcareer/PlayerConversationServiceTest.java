package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.service.CareerService;
import footballcareer.service.PlayerConversationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerConversationServiceTest {
    @Test
    void conversationChangesStateAndEnforcesSevenDayCooldownPerCareer() {
        DatabaseInitializer.resetAndSeedForTests();
        Season season = new SeasonRepository().findFirst();
        Team team = new TeamRepository().findByShortName("LIV");
        Career career = new CareerService(new CareerRepository(), new TeamRepository(),
                new SeasonRepository()).createCareer("Dynamics", team.getId(), season.getId());
        Player player = new PlayerRepository().findCurrentPlayersByTeam(team.getId()).getFirst();
        PlayerStateRepository states = new PlayerStateRepository();
        PlayerState before = states.findByPlayer(player.getId());
        PlayerConversationService service = new PlayerConversationService();

        var result = service.hold(career, player.getId(),
                PlayerConversationService.Approach.SUPPORT);
        PlayerState after = states.findByPlayer(player.getId());

        assertEquals(Math.min(100, before.getMorale() + 8), after.getMorale());
        assertEquals(Math.max(0, before.getForm() - 1), after.getForm());
        assertEquals(career.getCurrentDate().plusDays(7), result.nextAvailable());
        assertThrows(IllegalStateException.class, () -> service.hold(career, player.getId(),
                PlayerConversationService.Approach.CHALLENGE));
    }
}
