package footballcareer.ui;

/** Shared viewport breakpoints so layout behavior can be tested without launching JavaFX. */
public final class ViewportPolicy {
    public static final double COMPACT_WIDTH = 1320;
    public static final double COMPACT_HEIGHT = 760;

    private ViewportPolicy() {}

    public static boolean compact(double width, double height) {
        return width <= COMPACT_WIDTH || height <= COMPACT_HEIGHT;
    }

    public static double contentHeight(double viewportHeight) {
        double chrome = compact(1920, viewportHeight) ? 132 : 166;
        return Math.max(360, viewportHeight - chrome);
    }

    public static double centeredContentWidth(double viewportWidth) {
        double padding = viewportWidth <= COMPACT_WIDTH ? 32 : 64;
        return Math.max(320, viewportWidth - padding);
    }

    public static double overlayWidth(double viewportWidth) {
        return Math.min(620, centeredContentWidth(viewportWidth) - 24);
    }

    public static int dashboardColumns(double viewportWidth) {
        return viewportWidth < 1040 ? 2 : 4;
    }
}
