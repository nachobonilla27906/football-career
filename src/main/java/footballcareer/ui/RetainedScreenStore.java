package footballcareer.ui;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;

import java.util.HashMap;
import java.util.Map;

/** Keeps expensive, read-only screens alive while navigating within the same game date. */
public final class RetainedScreenStore {
    private final Map<String, Entry> screens = new HashMap<>();

    public void put(String screen, Object revision, Node content) {
        screens.put(screen, new Entry(revision, content));
    }

    public Node take(String screen, Object revision) {
        Entry entry = screens.get(screen);
        if (entry == null || !java.util.Objects.equals(entry.revision(), revision)) {
            screens.remove(screen);
            return null;
        }
        Node content = entry.content();
        Parent parent = content.getParent();
        if (parent instanceof ScrollPane scroll) scroll.setContent(null);
        else if (parent instanceof Pane pane) pane.getChildren().remove(content);
        return content;
    }

    public void clear() { screens.clear(); }
    public int size() { return screens.size(); }

    private record Entry(Object revision, Node content) {}
}
