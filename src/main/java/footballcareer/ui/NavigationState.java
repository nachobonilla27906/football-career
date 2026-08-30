package footballcareer.ui;

import java.util.HashMap;
import java.util.Map;

/** Remembers viewport position when screens are rebuilt during navigation. */
public class NavigationState {
    public record ScrollPosition(double horizontal, double vertical) {
        public ScrollPosition {
            horizontal = clamp(horizontal);
            vertical = clamp(vertical);
        }

        private static double clamp(double value) {
            return Math.max(0, Math.min(1, value));
        }
    }

    private final Map<String, ScrollPosition> scrollByScreen = new HashMap<>();
    private final Map<String, Long> selectionByScreen = new HashMap<>();

    public void remember(String screen, double horizontal, double vertical) {
        if (screen == null || screen.isBlank()) return;
        scrollByScreen.put(screen, new ScrollPosition(horizontal, vertical));
    }

    public ScrollPosition position(String screen) {
        return scrollByScreen.getOrDefault(screen, new ScrollPosition(0, 0));
    }

    public void rememberSelection(String screen, long id) {
        if (screen != null && !screen.isBlank() && id > 0) selectionByScreen.put(screen, id);
    }

    public Long selection(String screen) { return selectionByScreen.get(screen); }

    public void clear() {
        scrollByScreen.clear();
        selectionByScreen.clear();
    }
}
