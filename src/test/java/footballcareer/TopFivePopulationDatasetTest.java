package footballcareer;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopFivePopulationDatasetTest {
    private static final Path DATA = Path.of("src/main/resources/data");

    @Test
    void fullPopulationHasEveryLeagueAndPlayableSquadDepth() throws Exception {
        List<String[]> teams = rows("teams_top5_2026_27.csv");
        List<String[]> memberships = rows("competition_teams_top5_2026_27.csv");
        List<String[]> players = rows("players_top5_2026_27.csv");

        assertEquals(96, teams.size());
        Map<String, Long> leagueSizes = memberships.stream().collect(Collectors.groupingBy(
                row -> row[0], Collectors.counting()));
        assertEquals(Map.of("Premier League", 20L, "LaLiga", 20L, "Serie A", 20L,
                "Bundesliga", 18L, "Ligue 1", 18L), leagueSizes);
        assertTrue(players.size() >= 2_600);

        Map<String, List<String[]>> squads = players.stream().collect(
                Collectors.groupingBy(row -> row[16]));
        assertEquals(96, squads.size());
        squads.forEach((club, squad) -> {
            assertTrue(squad.size() >= 18, club + " has an incomplete squad");
            assertTrue(squad.stream().filter(row -> "GK".equals(row[4])).count() >= 2,
                    club + " needs at least two goalkeepers");
            assertTrue(squad.stream().noneMatch(row -> "Unknown".equalsIgnoreCase(row[3])));
        });

        List<String[]> madridKeepers = squads.get("RMA").stream()
                .filter(row -> "GK".equals(row[4])).toList();
        int courtois = madridKeepers.stream().filter(row -> row[1].contains("Courtois"))
                .mapToInt(row -> Integer.parseInt(row[6])).findFirst().orElseThrow();
        int bestOtherKeeper = madridKeepers.stream().filter(row -> !row[1].contains("Courtois"))
                .mapToInt(row -> Integer.parseInt(row[6])).max().orElseThrow();
        assertTrue(courtois > bestOtherKeeper,
                "Courtois must outrank Real Madrid's reserve goalkeepers");
    }

    private List<String[]> rows(String file) throws Exception {
        try (var lines = Files.lines(DATA.resolve(file))) {
            return lines.skip(1).filter(line -> !line.isBlank())
                    .map(line -> line.split(",", -1)).toList();
        }
    }
}
