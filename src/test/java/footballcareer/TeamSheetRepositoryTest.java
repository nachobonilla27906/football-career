package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.service.*;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TeamSheetRepositoryTest {
    @Test
    void savedBaseSheetSeedsTheNextMatchWithBenchTacticsAndRoles() {
        DatabaseInitializer.resetAndSeedForTests();
        Season season = new SeasonRepository().findFirst();
        new FootballWorldService().prepareSeason(season.getId());
        Team team = new TeamRepository().findByShortName("LIV");
        Career career = new CareerService(new CareerRepository(), new TeamRepository(),
                new SeasonRepository()).createCareer("Base XI", team.getId(), season.getId());
        Competition competition = new CompetitionTeamRepository()
                .findCompetitionsByTeam(team.getId()).getFirst();
        List<Match> teamMatches = new MatchRepository().findByCompetition(competition.getId())
                .stream().filter(match -> match.getHomeTeam().getId() == team.getId()
                        || match.getAwayTeam().getId() == team.getId()).limit(2).toList();
        assertEquals(2, teamMatches.size());

        LineupService service = new LineupService(new PlayerRepository(),
                new PlayerStateRepository());
        MatchLineup recommended = service.selectMatchLineup(team.getId());
        Player captain = recommended.getStarters().getFirst();
        Player penalty = recommended.getStarters().stream()
                .max(Comparator.comparingInt(Player::getShooting)).orElseThrow();
        Player corner = recommended.getStarters().stream()
                .max(Comparator.comparingInt(Player::getPassing)).orElseThrow();
        var tactics = new MatchTacticsRepository.TacticalSetup(
                "4-2-3-1", "ATTACKING", "HIGH", "FAST");
        var roles = new MatchRoleRepository.Assignment(
                captain.getId(), penalty.getId(), corner.getId());
        new TeamSheetRepository().save(career.getId(), team.getId(),
                new TeamSheetRepository.Sheet(recommended.getStarters(),
                        recommended.getSubstitutes(), tactics, roles));

        Match target = teamMatches.get(1);
        MatchLineup applied = service.selectMatchLineup(target.getId(), team.getId());

        assertEquals(recommended.getStarters().stream().map(Player::getId).toList(),
                applied.getStarters().stream().map(Player::getId).toList());
        assertEquals(recommended.getSubstitutes().stream().map(Player::getId).toList(),
                applied.getSubstitutes().stream().map(Player::getId).toList());
        assertEquals(tactics, new MatchTacticsRepository().find(target.getId(), team.getId()));
        assertEquals(roles, new MatchRoleRepository().find(target.getId(), team.getId()));
    }
}
