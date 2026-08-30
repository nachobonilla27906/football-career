package footballcareer.service;

public final class ClubNegotiationPolicy {
    public record Position(String stance, double requiredAmount, double counterFloor,
                           String explanation) {}

    public Position assess(boolean listed, Double askingPrice, double marketValue,
            int playerOverall, int sellerReputation, double sellerBudget,
            double squadAverage) {
        if (listed) return new Position("ABIERTO A VENDER",
                askingPrice == null ? marketValue : askingPrice, 0.78,
                "El jugador está en el mercado y el club escuchará propuestas razonables.");
        boolean cornerstone = playerOverall >= squadAverage + 4 || playerOverall >= 84;
        boolean wealthy = sellerBudget >= marketValue * 3;
        if (cornerstone && sellerReputation >= 82 && wealthy)
            return new Position("INTRANSFERIBLE", marketValue * 1.78, 0.92,
                    "Es una pieza central y el club no necesita vender.");
        if (sellerBudget < marketValue * 1.5)
            return new Position("NEGOCIADOR", marketValue * 1.28, 0.80,
                    "La situación económica permite negociar pese a no estar en venta.");
        if (cornerstone)
            return new Position("PROTECTOR", marketValue * 1.58, 0.88,
                    "El club exige una prima importante por un jugador clave.");
        double reputationPremium = Math.max(0, sellerReputation - 70) * 0.004;
        return new Position("FIRME", marketValue * (1.36 + reputationPremium), 0.84,
                "No está en venta, pero una propuesta alta puede abrir conversaciones.");
    }
}
