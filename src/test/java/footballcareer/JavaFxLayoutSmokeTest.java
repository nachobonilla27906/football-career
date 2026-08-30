package footballcareer;

import footballcareer.model.Career;
import footballcareer.model.Season;
import footballcareer.model.Team;
import footballcareer.ui.CareerShellView;
import footballcareer.ui.NavigationState;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class JavaFxLayoutSmokeTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        if (!Platform.isFxApplicationThread()) {
            CountDownLatch started = new CountDownLatch(1);
            try { Platform.startup(started::countDown); }
            catch (IllegalStateException alreadyStarted) { started.countDown(); }
            assertTrue(started.await(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void realShellFitsAndNavigatesAt1280x720() throws Exception {
        AtomicBoolean navigated = new AtomicBoolean();
        AtomicBoolean exited = new AtomicBoolean();
        runFx(() -> {
            Team team = new Team(1, "Liverpool", "LIV", "England", "Anfield", 61_000, 88);
            Season season = new Season(1, 2026, 2027, LocalDate.of(2026, 8, 1),
                    LocalDate.of(2027, 6, 30));
            Career career = new Career(1, "Manager", team, season, LocalDate.of(2026, 8, 15));
            VBox content = new VBox(12, new Label("CENTRO DE MANDO"), new Label("Contenido"));
            BorderPane root = new CareerShellView().build(career, 3, List.of(
                    new CareerShellView.NavigationItem("CENTRAL", true,
                            () -> navigated.set(true)),
                    new CareerShellView.NavigationItem("PLANTILLA", false, () -> {}),
                    new CareerShellView.NavigationItem("TRASPASOS", false, () -> {}),
                    new CareerShellView.NavigationItem("OFICINA", false, () -> {}),
                    new CareerShellView.NavigationItem("PERSONALIZAR", false, () -> {})),
                    List.of(new CareerShellView.NavigationItem("CALENDARIO", false, () -> {})),
                    () -> {}, () -> exited.set(true), content, "dashboard", new NavigationState());
            Scene scene = new Scene(root, 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            Stage stage = new Stage(); stage.setScene(scene); stage.show();
            root.applyCss(); root.layout();
            Button exit = (Button) root.lookup(".exit-button");
            Button central = (Button) root.lookup(".area-button-active");
            assertNotNull(exit); assertNotNull(central);
            assertTrue(exit.localToScene(exit.getBoundsInLocal()).getMaxX() <= 1280);
            assertTrue(exit.localToScene(exit.getBoundsInLocal()).getMaxY() <= 720);
            WritableImage image = scene.snapshot(null);
            assertEquals(1280, (int) image.getWidth()); assertEquals(720, (int) image.getHeight());
            central.fire(); exit.fire();
            assertTrue(navigated.get()); assertTrue(exited.get());
            stage.close();
        });
    }

    private void runFx(Runnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try { action.run(); } catch (Throwable throwable) { failure.set(throwable); }
            finally { done.countDown(); }
        });
        assertTrue(done.await(10, TimeUnit.SECONDS), "JavaFX layout test timed out.");
        if (failure.get() != null) throw new AssertionError(failure.get());
    }
}
