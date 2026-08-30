package footballcareer;

import footballcareer.model.Player;
import footballcareer.model.PlayerState;
import footballcareer.model.enums.Position;
import footballcareer.service.PlayerRoleAssessmentService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerRoleAssessmentServiceTest {
    @Test
    void strikerAssessmentUsesRoleAttributesAndCurrentCondition() {
        Player player = new Player();
        player.setPosition(Position.ST); player.setOverall(80);
        player.setPace(84); player.setShooting(91); player.setPassing(70);
        player.setDribbling(82); player.setDefending(30); player.setPhysical(76);
        PlayerState state = new PlayerState();
        state.setForm(75); state.setFitness(90);

        var assessment = new PlayerRoleAssessmentService().assess(player, state);

        assertEquals("Tiro", assessment.strongest());
        assertEquals("Físico", assessment.weakness());
        assertTrue(assessment.effectiveLevel() > player.getOverall());
        assertEquals(3, assessment.attributes().stream().filter(
                PlayerRoleAssessmentService.AttributeImpact::key).count());
        assertEquals("En gran momento de forma", assessment.condition());
    }
}
