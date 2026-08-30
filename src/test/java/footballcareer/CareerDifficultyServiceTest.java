package footballcareer;

import footballcareer.service.CareerDifficultyService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CareerDifficultyServiceTest {
    @Test
    void difficultyOnlyChangesControlledTeamStrength() {
        CareerDifficultyService service = new CareerDifficultyService();
        assertEquals(3, service.strengthModifier("CASUAL", true));
        assertEquals(0, service.strengthModifier("NORMAL", true));
        assertEquals(-2, service.strengthModifier("HARD", true));
        assertEquals(-4, service.strengthModifier("LEGENDARY", true));
        assertEquals(0, service.strengthModifier("LEGENDARY", false));
    }
}
