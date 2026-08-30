package footballcareer.service;

import footballcareer.model.enums.Position;

public final class PlayerOrdering {
    private PlayerOrdering() {}
    public static int position(Position position) {
        return switch (position) {
            case GK -> 0; case LB, CB, RB -> 1; case CDM, CM, CAM -> 2;
            case LW, RW, ST -> 3;
        };
    }
}
