package footballcareer.service;

import footballcareer.model.MatchEvent;
import footballcareer.model.Player;

/** Builds stable but varied commentary from the state visible at each event. */
public class LiveMatchNarrator {
    public String describe(MatchEvent event, Player player, Player secondary,
            int homeGoals, int awayGoals, boolean homeEvent) {
        String team = event.getTeam().getShortName();
        String name = player == null ? "un jugador" : player.getFullName();
        int variant = Math.floorMod((int) (event.getId() + event.getMinute()
                + (player == null ? 0 : player.getId())), 3);
        return switch (event.getType()) {
            case GOAL -> goal(event, name, secondary, team, homeGoals, awayGoals,
                    homeEvent, variant);
            case YELLOW_CARD -> pick(variant,
                    "Amarilla para " + name + " (" + team + ").",
                    name + " llega tarde y ve la amarilla.",
                    "El colegiado amonesta a " + name + ".");
            case RED_CARD -> pick(variant,
                    "¡Roja para " + name + "! " + team + " jugará con diez.",
                    "Expulsado " + name + ". Se complica el partido para " + team + ".",
                    "El árbitro no duda: " + name + " se marcha antes de tiempo.");
            case SUBSTITUTION -> secondary == null
                    ? "Mueve el banquillo " + team + ": participa " + name + "."
                    : pick(variant,
                    "Cambio en " + team + ": entra " + secondary.getFullName()
                            + " por " + name + ".",
                    secondary.getFullName() + " salta al campo; se retira " + name + ".",
                    team + " refresca el equipo con " + secondary.getFullName() + ".");
        };
    }

    private String goal(MatchEvent event, String name, Player secondary, String team,
            int homeGoals, int awayGoals, boolean homeEvent, int variant) {
        int nextHome = homeGoals + (homeEvent ? 1 : 0);
        int nextAway = awayGoals + (homeEvent ? 0 : 1);
        String context = nextHome == nextAway ? " Empata el partido."
                : event.getMinute() >= 80 && Math.abs(nextHome - nextAway) == 1
                ? " Gol decisivo en el tramo final." : "";
        String assist = secondary == null ? ""
                : " Asistencia de " + secondary.getFullName() + ".";
        return pick(variant,
                "¡Gol de " + name + " para " + team + "!" + assist + context,
                name + " encuentra la red. ¡Marca " + team + "!" + assist + context,
                "¡Dentro! " + name + " culmina la jugada de " + team + "."
                        + assist + context);
    }

    private String pick(int variant, String first, String second, String third) {
        return switch (variant) {
            case 0 -> first;
            case 1 -> second;
            default -> third;
        };
    }
}
