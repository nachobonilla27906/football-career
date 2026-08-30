package footballcareer.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;

public class FeedbackAnimator {
    public void animate(Label feedback, boolean success) {
        feedback.getStyleClass().removeAll("success-feedback", "warning-feedback");
        feedback.getStyleClass().add(success ? "success-feedback" : "warning-feedback");
        feedback.setOpacity(0.15);
        FadeTransition fade = new FadeTransition(Duration.millis(450), feedback);
        fade.setToValue(1); fade.play();
    }

    public void confirmSave(Button button, Label feedback, String message) {
        feedback.setText(message); feedback.setAccessibleText("Alineación guardada correctamente");
        animate(feedback, true); button.setText("✓ ALINEACIÓN GUARDADA"); button.setDisable(true);
        PauseTransition restore = new PauseTransition(Duration.seconds(1.4));
        restore.setOnFinished(event -> {
            button.setText("GUARDAR ALINEACIÓN"); button.setDisable(false);
        });
        restore.play();
    }
}
