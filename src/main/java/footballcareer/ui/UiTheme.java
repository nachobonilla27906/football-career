package footballcareer.ui;

import javafx.scene.Scene;
import javafx.scene.text.Font;

import java.util.List;

public final class UiTheme {
    private static final List<String> FONTS = List.of(
            "Barlow-Regular.ttf", "Barlow-Medium.ttf",
            "BarlowCondensed-SemiBold.ttf", "BarlowCondensed-Black.ttf");

    private UiTheme() {}

    public static void loadFonts() {
        FONTS.forEach(file -> Font.loadFont(UiTheme.class.getResourceAsStream(
                "/assets/fonts/" + file), 14));
    }

    public static void install(Scene scene) {
        scene.getStylesheets().setAll(stylesheets());
    }

    public static List<String> stylesheets() {
        return List.of(resource("tokens.css"), resource("screens.css"), resource("foundation.css"));
    }

    private static String resource(String file) {
        return UiTheme.class.getResource("/styles/" + file).toExternalForm();
    }
}
