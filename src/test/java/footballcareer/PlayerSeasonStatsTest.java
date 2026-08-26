package footballcareer;

import footballcareer.model.Player;
import footballcareer.model.PlayerSeasonStats;
import footballcareer.model.Season;
import footballcareer.model.Team;
import footballcareer.model.enums.Position;
import footballcareer.model.enums.PreferredFoot;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PlayerSeasonStatsTest {

    @Test
    void shouldCreateSeasonStats() {

        Player player = new Player(
                1,
                "Lamine",
                "Yamal",
                LocalDate.of(2007, 7, 13),
                "Spain",
                Position.RW,
                PreferredFoot.LEFT,
                91,
                96,
                90,
                86,
                88,
                94,
                35,
                70,
                150_000_000,
                10_000_000
        );

        Team team = new Team(
                1,
                "Barcelona",
                "BAR",
                "Spain",
                "Spotify Camp Nou",
                99354,
                95
        );

        Season season = new Season(
                1,
                2026,
                2027,
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2027, 5, 30)
        );

        PlayerSeasonStats stats = new PlayerSeasonStats(
                1,
                player,
                season,
                team
        );

        stats.setAppearances(38);
        stats.setStarts(34);
        stats.setMinutes(3012);
        stats.setGoals(17);
        stats.setAssists(14);
        stats.setYellowCards(4);
        stats.setRedCards(0);
        stats.setAverageRating(8.2);

        assertEquals(player, stats.getPlayer());
        assertEquals(season, stats.getSeason());
        assertEquals(team, stats.getTeam());

        assertEquals(38, stats.getAppearances());
        assertEquals(34, stats.getStarts());
        assertEquals(3012, stats.getMinutes());
        assertEquals(17, stats.getGoals());
        assertEquals(14, stats.getAssists());
        assertEquals(4, stats.getYellowCards());
        assertEquals(0, stats.getRedCards());
        assertEquals(8.2, stats.getAverageRating());
    }
}