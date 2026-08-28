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
        MatchLineup matchLineup = new LineupService(
                new PlayerRepository(), new PlayerStateRepository()
        ).selectMatchLineup(arsenal.getId());
        List<Player> lineup = matchLineup.getStarters();

        assertEquals(11, lineup.size());
        assertEquals(11, lineup.stream().map(Player::getId).distinct().count());
        assertTrue(lineup.stream().anyMatch(p -> p.getPosition() == Position.GK));
        assertEquals(7, matchLineup.getSubstitutes().size());
        assertTrue(matchLineup.getSubstitutes().stream()
                .noneMatch(substitute -> lineup.contains(substitute)));
    }

    @Test
    void shouldExcludePlayersWhoAreNotFitToPlay() {
        Player unavailable = new PlayerRepository()
                .findCurrentPlayersByTeam(arsenal.getId()).stream()
                .filter(player -> player.getPosition() != Position.GK)
                .findFirst().orElseThrow();
        PlayerStateRepository states = new PlayerStateRepository();
        PlayerState state = states.findByPlayer(unavailable.getId());
        state.setFitness(10);
        states.update(state);

        MatchLineup lineup = new LineupService(new PlayerRepository(), states)
                .selectMatchLineup(arsenal.getId());

        assertTrue(lineup.getStarters().stream()
                .noneMatch(player -> player.getId() == unavailable.getId()));
        assertTrue(lineup.getSubstitutes().stream()
                .noneMatch(player -> player.getId() == unavailable.getId()));
    }

    @Test
    void shouldPersistSelectedLineupForAMatch() {
        new footballcareer.service.FootballWorldService().prepareSeason(season.getId());
        Competition competition = new CompetitionRepository()
                .findByNameAndSeason("Premier League", season.getId());
        Match match = new MatchRepository().findByCompetition(competition.getId()).getFirst();
        MatchLineupRepository repository = new MatchLineupRepository();
        LineupService lineups = new LineupService(new PlayerRepository(),
                new PlayerStateRepository(), repository);
        MatchLineup selected = lineups.selectMatchLineup(arsenal.getId());

        repository.save(match.getId(), arsenal.getId(),
                selected.getStarters(), selected.getSubstitutes());
        MatchLineup loaded = lineups.selectMatchLineup(match.getId(), arsenal.getId());

        assertEquals(11, loaded.getStarters().size());
        assertEquals(selected.getStarters().stream().map(Player::getId).toList(),
                loaded.getStarters().stream().map(Player::getId).toList());
        assertEquals(7, loaded.getSubstitutes().size());
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
