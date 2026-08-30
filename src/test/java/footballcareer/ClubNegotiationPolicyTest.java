package footballcareer;

import footballcareer.service.ClubNegotiationPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClubNegotiationPolicyTest {
    private final ClubNegotiationPolicy policy = new ClubNegotiationPolicy();

    @Test
    void clubPostureReflectsListingImportanceAndFinancialPressure() {
        var listed = policy.assess(true, 12_000_000d, 10_000_000,
                78, 85, 100_000_000, 77);
        var protectedStar = policy.assess(false, null, 50_000_000,
                87, 90, 200_000_000, 79);
        var pressured = policy.assess(false, null, 50_000_000,
                80, 75, 20_000_000, 78);

        assertEquals("ABIERTO A VENDER", listed.stance());
        assertEquals(12_000_000, listed.requiredAmount());
        assertEquals("INTRANSFERIBLE", protectedStar.stance());
        assertTrue(protectedStar.requiredAmount() > pressured.requiredAmount());
        assertTrue(protectedStar.counterFloor() > pressured.counterFloor());
    }
}
