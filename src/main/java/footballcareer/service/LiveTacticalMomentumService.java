package footballcareer.service;

import footballcareer.database.MatchTacticsRepository.TacticalSetup;

/** Readable tactical feedback for the manager during a live match. */
public class LiveTacticalMomentumService {
    public record Assessment(int momentum, String label, String risk) {}

    public Assessment assess(TacticalSetup setup, int minute, int goalDifference) {
        int value = switch (setup.mentality()) {
            case "ATTACKING" -> 18;
            case "DEFENSIVE" -> -14;
            default -> 0;
        };
        value += switch (setup.pressing()) {
            case "HIGH" -> minute >= 70 ? 4 : 12;
            case "LOW" -> -6;
            default -> 2;
        };
        value += switch (setup.tempo()) {
            case "FAST" -> 9;
            case "SLOW" -> -5;
            default -> 1;
        };
        if (goalDifference < 0) value += minute >= 70 ? 18 : 8;
        if (goalDifference > 0 && minute >= 70) value -= 8;
        value = Math.max(-50, Math.min(50, value));
        String label = value >= 25 ? "EMPUJE ALTO" : value >= 8 ? "INICIATIVA"
                : value <= -15 ? "BLOQUE BAJO" : "EQUILIBRIO";
        String risk = "HIGH".equals(setup.pressing()) && minute >= 70
                ? "Riesgo alto de fatiga" : "ATTACKING".equals(setup.mentality())
                ? "Espacios a la espalda" : "Riesgo controlado";
        return new Assessment(value, label, risk);
    }
}
