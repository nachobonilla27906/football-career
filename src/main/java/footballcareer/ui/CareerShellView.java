package footballcareer.ui;

import footballcareer.model.Career;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public final class CareerShellView {
    public record NavigationItem(String text, boolean selected, Runnable action) {}

    public BorderPane build(Career career, int notificationCount,
            List<NavigationItem> mainItems, List<NavigationItem> subItems,
            Runnable notificationsAction, Runnable exitAction, Node content,
            String screen, NavigationState navigationState) {
        HBox header = header(career, notificationCount, notificationsAction);
        HBox mainNavigation = navigation(mainItems, "main-navigation", "area-button",
                "area-button-active");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        mainNavigation.getChildren().add(spacer);
        VBox subNavigation = verticalNavigation(subItems);
        Button exit = button("GUARDAR Y SALIR", "exit-button", exitAction);
        VBox club = new VBox(8, TeamCrestView.create(career.getControlledTeam(), 92),
                label(career.getControlledTeam().getName(), "sidebar-club-name"),
                label(career.getManagerName() + "  ·  MÁNAGER", "sidebar-manager"));
        club.setAlignment(Pos.CENTER); Region sideSpacer = new Region(); VBox.setVgrow(sideSpacer, Priority.ALWAYS);
        VBox sidebar = new VBox(18, club, subNavigation, sideSpacer, exit);
        sidebar.getStyleClass().add("career-sidebar"); sidebar.setPrefWidth(205);

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("app-shell");
        shell.setTop(new VBox(header, mainNavigation));
        shell.setLeft(sidebar);
        ScrollPane scroll = content instanceof ScrollPane existing
                ? existing : new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setPannable(true);
        scroll.setId("career-content-scroll");
        scroll.setUserData(screen);
        NavigationState.ScrollPosition saved = navigationState.position(screen);
        Platform.runLater(() -> {
            scroll.setHvalue(saved.horizontal());
            scroll.setVvalue(saved.vertical());
        });
        shell.setCenter(scroll);
        return shell;
    }

    private HBox header(Career career, int notificationCount, Runnable notificationAction) {
        Label wordmark = label("FC//CAREER", "shell-wordmark");
        Label alpha = label("BETA 0.1", "shell-alpha");
        VBox identity = new VBox(2,
                label(career.getControlledTeam().getShortName() + "  //  "
                        + career.getControlledTeam().getName(), "shell-club"),
                label(career.getManagerName() + "  •  "
                        + career.getCurrentSeason().getName(), "shell-manager"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button notifications = button("NOTIFICACIONES  //  " + notificationCount,
                notificationCount > 0 ? "notification-button-active"
                        : "notification-button", notificationAction);
        HBox header = new HBox(13, wordmark, alpha, identity, spacer, notifications,
                label(career.getCurrentDate().toString(), "shell-date"));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("career-header");
        return header;
    }

    private HBox navigation(List<NavigationItem> items, String containerStyle,
            String buttonStyle, String selectedStyle) {
        HBox navigation = new HBox("main-navigation".equals(containerStyle) ? 4 : 6);
        navigation.setAlignment(Pos.CENTER_LEFT);
        navigation.getStyleClass().add(containerStyle);
        for (NavigationItem item : items) {
            Button button = button(navigationLabel(item.text()), buttonStyle, item.action());
            button.setGraphic(NavigationIcon.create(item.text()));
            if (item.selected()) button.getStyleClass().add(selectedStyle);
            navigation.getChildren().add(button);
        }
        return navigation;
    }

    private VBox verticalNavigation(List<NavigationItem> items) {
        VBox navigation = new VBox(3);
        navigation.getStyleClass().add("side-navigation");
        for (NavigationItem item : items) {
            Button button = button(navigationLabel(item.text()), "side-nav-button", item.action());
            button.setGraphic(NavigationIcon.create(item.text()));
            button.setMaxWidth(Double.MAX_VALUE);
            if (item.selected()) button.getStyleClass().add("side-nav-button-active");
            navigation.getChildren().add(button);
        }
        return navigation;
    }

    public static String navigationLabel(String text) {
        return text;
    }

    private Label label(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        label.setWrapText(true);
        return label;
    }

    private Button button(String text, String style, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add(style);
        button.setTooltip(new Tooltip(text));
        button.setOnAction(event -> action.run());
        return button;
    }
}
