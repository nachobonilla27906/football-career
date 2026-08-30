package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.model.enums.MatchEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchEventRepositoryTest {
    private MatchEventRepository repository;
    private Match match;
    private Team arsenal;
    private List<Player> players;

    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetAndSeedForTests();
        repository = new MatchEventRepository();
        Season season = new SeasonRepository().findFirst();
        Competition competition = new CompetitionRepository()
                .findByNameAndSeason("Premier League", season.getId());
        TeamRepository teams = new TeamRepository();
        arsenal = teams.findByShortName("ARS");
        Team liverpool = teams.findByShortName("LIV");
        players = new PlayerRepository().findCurrentPlayersByTeam(arsenal.getId());
        match = new Match(0, competition, arsenal, liverpool, LocalDate.of(2026, 9, 12));
        new MatchRepository().save(match);
    }

    @Test
    void shouldSaveAndReturnEventsChronologically() {
        MatchEvent lateGoal = event(MatchEventType.GOAL, 72, players.get(0));
        lateGoal.setSecondaryPlayer(players.get(1));
        repository.save(lateGoal);
        repository.save(event(MatchEventType.YELLOW_CARD, 18, players.get(2)));

        List<MatchEvent> events = repository.findByMatch(match.getId());
        assertEquals(2, events.size());
        assertEquals(18, events.get(0).getMinute());
        assertEquals(MatchEventType.GOAL, events.get(1).getType());
        assertEquals(players.get(1).getId(), events.get(1).getSecondaryPlayer().getId());
        assertTrue(lateGoal.getId() > 0);
    }

    @Test
    void shouldValidateMinuteAndSubstitutionPlayers() {
        assertThrows(IllegalArgumentException.class, () ->
                repository.save(event(MatchEventType.GOAL, 0, players.get(0))));
        assertThrows(IllegalArgumentException.class, () ->
                repository.save(event(MatchEventType.SUBSTITUTION, 60, players.get(0))));
    }

    private MatchEvent event(MatchEventType type, int minute, Player player) {
        MatchEvent event = new MatchEvent();
        event.setMatch(match);
        event.setTeam(arsenal);
        event.setPlayer(player);
        event.setType(type);
        event.setMinute(minute);
        return event;
    }
}
