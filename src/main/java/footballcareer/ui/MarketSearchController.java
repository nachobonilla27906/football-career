package footballcareer.ui;

import footballcareer.model.Player;
import footballcareer.model.Team;
import footballcareer.model.enums.Position;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MarketSearchController {
    public record Query(boolean global, String text, String league, String club,
                        String position, String minimumOverall, String maximumAge,
                        String maximumPriceMillions, String maximumSalaryMillions,
                        String sort, boolean shortlistOnly) {}
    public record Catalogue(List<Player> allPlayers, List<Player> listedPlayers,
                            Map<Long, Team> teamByPlayer, Map<Long, String> leagueByTeam,
                            Map<Long, Double> askingPrices, Set<Long> shortlist,
                            long controlledTeamId, LocalDate date) {}

    public List<Player> search(Catalogue catalogue, Query query) {
        String text = safe(query.text()).trim().toLowerCase();
        int overall = integer(query.minimumOverall(), 0);
        int age = integer(query.maximumAge(), Integer.MAX_VALUE);
        double price = millions(query.maximumPriceMillions(), Double.MAX_VALUE);
        double salary = millions(query.maximumSalaryMillions(), Double.MAX_VALUE);
        List<Player> source = query.global() ? catalogue.allPlayers() : catalogue.listedPlayers();
        return source.stream()
                .filter(player -> validOpponent(catalogue, player))
                .filter(player -> matchesText(catalogue, player, text))
                .filter(player -> matchesClub(catalogue, player, query.club()))
                .filter(player -> matchesLeague(catalogue, player, query.league()))
                .filter(player -> matchesPosition(player, query.position()))
                .filter(player -> !query.shortlistOnly()
                        || catalogue.shortlist().contains(player.getId()))
                .filter(player -> player.getOverall() >= overall)
                .filter(player -> player.getAge(catalogue.date()) <= age)
                .filter(player -> player.getSalary() <= salary)
                .filter(player -> marketPrice(catalogue, player) <= price)
                .sorted(comparator(catalogue, query.sort()))
                .toList();
    }

    private boolean validOpponent(Catalogue catalogue, Player player) {
        Team team = catalogue.teamByPlayer().get(player.getId());
        return team != null && team.getId() != catalogue.controlledTeamId();
    }

    private boolean matchesText(Catalogue catalogue, Player player, String text) {
        if (text.isEmpty()) return true;
        Team team = catalogue.teamByPlayer().get(player.getId());
        return player.getFullName().toLowerCase().contains(text)
                || team.getName().toLowerCase().contains(text);
    }

    private boolean matchesClub(Catalogue catalogue, Player player, String club) {
        return club == null || "TODOS LOS CLUBES".equals(club)
                || catalogue.teamByPlayer().get(player.getId()).getName().equals(club);
    }

    private boolean matchesLeague(Catalogue catalogue, Player player, String league) {
        Team team = catalogue.teamByPlayer().get(player.getId());
        return league == null || "TODAS LAS LIGAS".equals(league)
                || league.equals(catalogue.leagueByTeam().get(team.getId()));
    }

    private boolean matchesPosition(Player player, String group) {
        if (group == null || "TODAS".equals(group)) return true;
        return switch (group) {
            case "GK" -> player.getPosition() == Position.GK;
            case "DEFENSA" -> Set.of(Position.LB, Position.CB, Position.RB)
                    .contains(player.getPosition());
            case "MEDIO" -> Set.of(Position.CDM, Position.CM, Position.CAM)
                    .contains(player.getPosition());
            case "ATAQUE" -> Set.of(Position.LW, Position.RW, Position.ST)
                    .contains(player.getPosition());
            default -> true;
        };
    }

    private Comparator<Player> comparator(Catalogue catalogue, String sort) {
        return switch (sort == null ? "PRECIO ↑" : sort) {
            case "PRECIO ↓" -> Comparator.comparingDouble(
                    (Player player) -> marketPrice(catalogue, player)).reversed();
            case "GRL ↓" -> Comparator.comparingInt(Player::getOverall).reversed();
            case "EDAD ↑" -> Comparator.comparingInt(player -> player.getAge(catalogue.date()));
            default -> Comparator.comparingDouble(player -> marketPrice(catalogue, player));
        };
    }

    private double marketPrice(Catalogue catalogue, Player player) {
        return catalogue.askingPrices().getOrDefault(player.getId(), player.getMarketValue());
    }

    private int integer(String value, int fallback) {
        try { return safe(value).isBlank() ? fallback : Integer.parseInt(value.trim()); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private double millions(String value, double fallback) {
        try { return safe(value).isBlank() ? fallback
                : Double.parseDouble(value.replace(',', '.').trim()) * 1_000_000; }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private String safe(String value) { return value == null ? "" : value; }
}
