package footballcareer.service;

import footballcareer.model.Player;

public final class PlayerAgentService {
    public enum Decision { ACCEPTED, COUNTER, REJECTED }
    public record Response(Decision decision, double requiredSalary, String message) {}

    public Response evaluate(Player player, double salary, double signingBonus,
            int years, Double releaseClause, String role) {
        if (player == null || salary <= 0 || signingBonus < 0 || years < 1 || years > 5)
            throw new IllegalArgumentException("Invalid contract proposal.");
        double roleFactor = switch (role) {
            case "CRUCIAL" -> 0.94;
            case "IMPORTANT" -> 1.0;
            case "ROTATION" -> 1.14;
            case "PROSPECT" -> player.getAge(java.time.LocalDate.now()) <= 22 ? 0.98 : 1.18;
            default -> throw new IllegalArgumentException("Invalid squad role.");
        };
        double durationFactor = switch (years) {
            case 1 -> 1.12;
            case 2 -> 1.06;
            case 4 -> 0.97;
            case 5 -> 0.95;
            default -> 1.0;
        };
        double clauseFactor = releaseClause != null
                && releaseClause < player.getMarketValue() * 1.5 ? 1.08 : 1.0;
        double required = Math.max(100_000, player.getSalary())
                * roleFactor * durationFactor * clauseFactor;
        double packageValue = salary + signingBonus / years;
        if (packageValue >= required) return new Response(Decision.ACCEPTED, required,
                "El agente acepta el salario, la prima, el rol y la duración propuestos.");
        if (packageValue >= required * 0.82) return new Response(Decision.COUNTER, required,
                String.format("El agente contraoferta: solicita un paquete equivalente a €%.2fM anuales.",
                        required / 1_000_000));
        return new Response(Decision.REJECTED, required,
                "El agente rechaza la propuesta: salario, prima o rol están muy lejos de sus expectativas.");
    }

    public void requireAgreement(Player player, double salary, double signingBonus,
            int years, Double releaseClause, String role) {
        Response response = evaluate(player, salary, signingBonus, years, releaseClause, role);
        if (response.decision() != Decision.ACCEPTED)
            throw new IllegalStateException(response.message());
    }
}
