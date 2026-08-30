package footballcareer.service;

import footballcareer.model.MatchEvent;
public class LivePitchPositionService {
    public record Position(double x, double y, String zone) {}

    public Position position(MatchEvent event, boolean homeTeam) {
        double attackingX = switch (event.getType()) {
            case GOAL -> 0.88;
            case YELLOW_CARD, RED_CARD -> 0.45 + Math.floorMod(event.getMinute(), 25) / 100.0;
            case SUBSTITUTION -> 0.08;
        };
        double x = homeTeam ? attackingX : 1 - attackingX;
        double y = 0.20 + Math.floorMod((int) (event.getId() * 17 + event.getMinute()), 61) / 100.0;
        String zone = attackingX >= 0.78 ? "ÁREA RIVAL"
                : attackingX <= 0.18 ? "BANQUILLO" : "ZONA CENTRAL";
        return new Position(x, y, zone);
    }
}
