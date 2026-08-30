package footballcareer.ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Responsive wrappers shared by menus, forms and in-app dialogs. */
public final class ResponsiveContainer {
    public ScrollPane centered(Node node, String rootStyle) {
        VBox holder = new VBox(node);
        holder.setFillWidth(false);
        holder.setAlignment(Pos.CENTER);
        StackPane canvas = new StackPane(holder);
        canvas.getStyleClass().add(rootStyle);
        canvas.setPadding(new Insets(32));
        canvas.setMinSize(0, 0);
        if (node instanceof Region region) {
            double designedMaximum = region.getMaxWidth();
            region.setMinWidth(0);
            region.maxWidthProperty().bind(Bindings.createDoubleBinding(
                    () -> Math.min(designedMaximum,
                            ViewportPolicy.centeredContentWidth(canvas.getWidth())),
                    canvas.widthProperty()));
        }
        ScrollPane scroll = new ScrollPane(canvas);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setPannable(true);
        scroll.getStyleClass().add("screen-scroll");
        return scroll;
    }

    public Runnable overlay(Scene scene, Node content) {
        javafx.scene.Parent underlying = scene.getRoot();
        StackPane layer = new StackPane();
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.getStyleClass().add("overlay-scroll");
        scroll.maxWidthProperty().bind(Bindings.createDoubleBinding(
                () -> ViewportPolicy.overlayWidth(scene.getWidth()), scene.widthProperty()));
        scroll.maxHeightProperty().bind(Bindings.createDoubleBinding(
                () -> ViewportPolicy.contentHeight(scene.getHeight()), scene.heightProperty()));
        if (content instanceof Region region) region.setMinWidth(0);
        StackPane shade = new StackPane(scroll);
        shade.getStyleClass().add("in-app-overlay");
        scene.setRoot(layer);
        layer.getChildren().addAll(underlying, shade);
        return () -> {
            if (scene.getRoot() == layer) {
                layer.getChildren().remove(underlying);
                scene.setRoot(underlying);
            }
        };
    }
}
