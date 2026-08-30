package footballcareer.ui;

public final class CareerNavigationController {
    public String areaFor(String section) {
        return switch (section) {
            case "squad", "lineup", "training", "medical" -> "squad";
            case "market", "history" -> "transfers";
            case "notifications" -> "central";
            case "office" -> "office";
            case "settings" -> "customize";
            default -> "central";
        };
    }

    public boolean isSelected(String target, String activeSection, int marketTab) {
        if (target.equals(activeSection)) return true;
        if (!"market".equals(activeSection)) return false;
        return switch (target) {
            case "market" -> marketTab == 0;
            case "sales" -> marketTab == 1;
            case "offers" -> marketTab == 2;
            case "incoming" -> marketTab == 3;
            default -> false;
        };
    }

    public String reportReturnSection(String activeSection) {
        return switch (activeSection) {
            case "results", "dashboard", "standings", "calendar" -> activeSection;
            default -> "calendar";
        };
    }

    public String sectionLabel(String section) {
        return switch (reportReturnSection(section)) {
            case "results" -> "RESULTADOS";
            case "dashboard" -> "CENTRAL";
            case "standings" -> "CLASIFICACIÓN";
            default -> "CALENDARIO";
        };
    }
}
