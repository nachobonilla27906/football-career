package footballcareer;

import footballcareer.service.ManagerIdentityService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManagerIdentityServiceTest {
    @Test
    void everyIdentityHasOneSpecificEffectWithoutStackingOthers() {
        ManagerIdentityService service = new ManagerIdentityService();
        assertEquals(1, service.matchModifier("TACTICIAN"));
        assertEquals(0, service.trainingFormBonus("TACTICIAN"));
        assertEquals(1, service.trainingFormBonus("DEVELOPER"));
        assertEquals(2, service.conversationMoraleBonus("MOTIVATOR", 5));
        assertEquals(0, service.conversationMoraleBonus("MOTIVATOR", -3));
        assertEquals(0, service.matchModifier("GENERALIST"));
    }
}
