package footballcareer.ui;

import javafx.scene.Node;
import javafx.scene.shape.SVGPath;

public final class NavigationIcon {
    private NavigationIcon() {}

    public static Node create(String label) {
        String base = label.contains("  //  ") ? label.substring(0, label.indexOf("  //  ")) : label;
        SVGPath icon = new SVGPath();
        icon.setContent(switch (base) {
            case "CENTRAL" -> "M2 10 L10 3 L18 10 V18 H12 V13 H8 V18 H2 Z";
            case "PLANTILLA", "JUGADORES" -> "M10 2 A4 4 0 1 1 9.99 2 M3 18 C3 13 17 13 17 18 Z";
            case "TRASPASOS", "MERCADO", "VENTAS", "OFERTAS", "HISTORIAL" ->
                    "M2 6 H15 L12 3 L14 1 L20 7 L14 13 L12 11 L15 8 H2 Z M18 14 H5 L8 11 L6 9 L0 15 L6 21 L8 19 L5 16 H18 Z";
            case "OFICINA" -> "M3 19 V5 H17 V19 H13 V15 H7 V19 Z M6 8 H9 V11 H6 Z M11 8 H14 V11 H11 Z";
            case "PERSONALIZAR", "AJUSTES" -> "M8 2 H12 L13 5 L16 4 L18 8 L16 10 L18 13 L15 16 L12 15 L10 19 L7 17 L7 14 L3 13 L2 9 L5 7 L5 4 Z M10 7 A3 3 0 1 1 9.99 7";
            case "CALENDARIO" -> "M3 4 H17 V19 H3 Z M3 8 H17 M7 2 V6 M13 2 V6";
            case "CLASIFICACIÓN", "RESULTADOS" -> "M3 16 H6 V19 H3 Z M8 11 H11 V19 H8 Z M13 5 H16 V19 H13 Z";
            case "BANDEJA" -> "M3 4 H17 V17 H12 L10 20 L8 17 H3 Z M3 12 H7 L9 15 H11 L13 12 H17";
            case "ALINEACIÓN" -> "M10 2 A2 2 0 1 1 9.99 2 M4 8 A2 2 0 1 1 3.99 8 M16 8 A2 2 0 1 1 15.99 8 M7 15 A2 2 0 1 1 6.99 15 M13 15 A2 2 0 1 1 12.99 15";
            case "ENTRENAMIENTO", "MÉDICO" -> "M8 2 H12 V8 H18 V12 H12 V18 H8 V12 H2 V8 H8 Z";
            default -> "M4 4 H16 V16 H4 Z";
        });
        icon.getStyleClass().add("navigation-icon");
        icon.setScaleX(0.72); icon.setScaleY(0.72);
        return icon;
    }
}
