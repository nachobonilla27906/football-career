package footballcareer;

import footballcareer.ui.NavigationState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NavigationStateTest {
    @Test
    void remembersIndependentClampedPositionsForEveryScreen() {
        NavigationState state = new NavigationState();
        state.remember("market", 0.25, 0.72);
        state.remember("squad", -1, 2);
        state.rememberSelection("squad", 42);

        assertEquals(0.25, state.position("market").horizontal());
        assertEquals(0.72, state.position("market").vertical());
        assertEquals(0, state.position("squad").horizontal());
        assertEquals(1, state.position("squad").vertical());
        assertEquals(42, state.selection("squad"));
        assertEquals(0, state.position("calendar").vertical());
        state.clear();
        assertEquals(0, state.position("market").vertical());
        assertEquals(null, state.selection("squad"));
    }
}
