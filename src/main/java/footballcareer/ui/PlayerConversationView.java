package footballcareer.ui;

import footballcareer.model.Career;
import footballcareer.model.Player;
import footballcareer.service.PlayerConversationService;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public final class PlayerConversationView {
    public VBox build(Career career, Player player) {
        PlayerConversationService service = new PlayerConversationService();
        Label status = label(availabilityText(service, career, player), "muted-label");
        FlowPane choices = new FlowPane(10, 10);
        for (PlayerConversationService.Approach approach
                : PlayerConversationService.Approach.values()) {
            Button action = button(buttonText(approach), approach == PlayerConversationService.Approach.REST
                    ? "ghost-button" : "secondary-button");
            action.setOnAction(event -> hold(service, career, player, approach, choices, status));
            choices.getChildren().add(action);
        }
        boolean blocked = service.nextAvailable(career.getId(), player.getId()) != null
                && career.getCurrentDate().isBefore(
                service.nextAvailable(career.getId(), player.getId()));
        choices.getChildren().forEach(node -> node.setDisable(blocked));
        VBox view = new VBox(8, label("CONVERSACIÓN INDIVIDUAL", "objective-title"),
                choices, status);
        view.getStyleClass().add("conversation-desk");
        return view;
    }

    private void hold(PlayerConversationService service, Career career, Player player,
            PlayerConversationService.Approach approach, FlowPane choices, Label status) {
        try {
            var result = service.hold(career, player.getId(), approach);
            status.setText("Completada  •  Moral " + signed(result.moraleChange())
                    + "  •  Forma " + signed(result.formChange()) + "  •  Fitness "
                    + signed(result.fitnessChange()) + "  •  Disponible "
                    + result.nextAvailable());
            status.getStyleClass().setAll("label", "success-feedback");
            choices.getChildren().forEach(node -> node.setDisable(true));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            status.setText(exception.getMessage());
            status.getStyleClass().setAll("label", "warning-feedback");
        }
    }

    private String availabilityText(PlayerConversationService service, Career career,
            Player player) {
        var next = service.nextAvailable(career.getId(), player.getId());
        return next != null && career.getCurrentDate().isBefore(next)
                ? "Próxima conversación: " + next
                : "Apoyar sube moral; retar mejora forma; descanso recupera fitness.";
    }

    private String buttonText(PlayerConversationService.Approach approach) {
        return switch (approach) {
            case SUPPORT -> "APOYAR";
            case CHALLENGE -> "RETAR";
            case REST -> "DAR DESCANSO";
        };
    }

    private String signed(int value) { return value >= 0 ? "+" + value : String.valueOf(value); }

    private Label label(String text, String style) {
        Label label = new Label(text); label.getStyleClass().add(style); label.setWrapText(true);
        return label;
    }

    private Button button(String text, String style) {
        Button button = new Button(text); button.getStyleClass().add(style);
        button.setTooltip(new Tooltip(text)); return button;
    }
}
