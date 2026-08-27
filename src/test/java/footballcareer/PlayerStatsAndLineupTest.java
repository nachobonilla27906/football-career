package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.model.enums.Position;
import footballcareer.service.LineupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerStatsAndLineupTest {
    private Season season;
    private Team arsenal;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
        season = new SeasonRepository().findFirst();
        arsenal = new TeamRepository().findByShortName("ARS");
    }

    @Test
    void shouldSelectElevenIncludingGoalkeeper() {
        List<Player> lineup = new LineupService(
                new PlayerRepository(), new PlayerStateRepository()
        ).selectStartingEleven(arsenal.getId());

        assertEquals(11, lineup.size());
        assertEquals(11, lineup.stream().map(Player::getId).distinct().count());
        assertTrue(lineup.stream().anyMatch(p -> p.getPosition() == Position.GK));
    }

    @Test
    void shouldRecordAndAverageAppearances() {
        Player player = new PlayerRepository()
                .findCurrentPlayersByTeam(arsenal.getId()).getFirst();
        PlayerSeasonStatsRepository repository =
                new PlayerSeasonStatsRepository();
        repository.initializeForSeason(season.getId());

        repository.recordAppearance(
                player.getId(), season.getId(), true, 90, 1, 0, 0, 0, 8.0);
        repository.recordAppearance(
                player.getId(), season.getId(), true, 90, 0, 1, 1, 0, 6.0);

        PlayerSeasonStats stats = repository.find(player.getId(), season.getId());
        assertEquals(2, stats.getAppearances());
        assertEquals(180, stats.getMinutes());
        assertEquals(1, stats.getGoals());
        assertEquals(1, stats.getAssists());
        assertEquals(7.0, stats.getAverageRating());
    }
}
