package footballcareer;

import footballcareer.ui.RetainedScreenStore;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class RetainedScreenStoreTest {
    @Test
    void reusesContentOnlyWhileCareerRevisionMatches() {
        RetainedScreenStore store = new RetainedScreenStore();
        Pane content = new Pane();
        Pane formerParent = new Pane(content);
        LocalDate date = LocalDate.of(2026, 8, 30);

        store.put("standings", date, content);

        assertSame(formerParent, content.getParent());
        assertSame(content, store.take("standings", date));
        assertNull(content.getParent());
        store.put("standings", date, content);
        assertNull(store.take("standings", date.plusDays(1)));
    }
}
