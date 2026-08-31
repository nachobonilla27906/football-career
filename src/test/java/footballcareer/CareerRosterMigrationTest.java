package footballcareer;

import footballcareer.database.*;
import footballcareer.model.Career;
import footballcareer.model.Player;
import footballcareer.model.Season;
import footballcareer.model.Team;
import footballcareer.service.CareerService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CareerRosterMigrationTest {
    @Test
    void removesObsoleteSeedMembershipWhenCanonicalMembershipAlsoExists() throws Exception {
        DatabaseInitializer.resetAndSeedForTests();
        Season season = new SeasonRepository().findFirst();
        Team canonical = new TeamRepository().findByShortName("VCF");
        CareerService service = new CareerService(new CareerRepository(),
                new TeamRepository(), new SeasonRepository());
        Career career = service.createCareer("Roster migration", canonical.getId(), season.getId());
        Player player = new PlayerRepository().findCurrentPlayersByTeam(canonical.getId()).getFirst();
        Team obsolete = new Team(0, "Valencia legacy", "VALX", "Spain",
                "Mestalla", 49_430, 80);
        new TeamRepository().save(obsolete);
        try (var connection = Database.getConnection();
             var insert = connection.prepareStatement("""
                     INSERT INTO career_player_team
                         (career_id, player_id, team_id, start_date, end_date)
                     VALUES (?, ?, ?, ?, NULL)
                     """)) {
            insert.setLong(1, career.getId());
            insert.setLong(2, player.getId());
            insert.setLong(3, obsolete.getId());
            insert.setString(4, season.getStartDate().toString());
            insert.executeUpdate();
        }

        service.loadCareer(career.getId());

        try (var connection = Database.getConnection();
             var count = connection.prepareStatement("""
                     SELECT COUNT(*) FROM career_player_team
                     WHERE career_id = ? AND player_id = ? AND end_date IS NULL
                     """)) {
            count.setLong(1, career.getId());
            count.setLong(2, player.getId());
            try (var rows = count.executeQuery()) {
                rows.next();
                assertEquals(1, rows.getInt(1));
            }
        }
    }
}
