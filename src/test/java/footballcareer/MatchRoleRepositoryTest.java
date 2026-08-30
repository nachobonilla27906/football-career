package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.service.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatchRoleRepositoryTest {
    @Test
    void rolesMustBelongToSavedStartingElevenAndRemainCareerScoped() {
        DatabaseInitializer.resetAndSeedForTests();
        Season season = new SeasonRepository().findFirst();
        new FootballWorldService().prepareSeason(season.getId());
        Team team = new TeamRepository().findByShortName("LIV");
        new CareerService(new CareerRepository(), new TeamRepository(), new SeasonRepository())
                .createCareer("Roles", team.getId(), season.getId());
        Competition competition = new CompetitionTeamRepository()
                .findCompetitionsByTeam(team.getId()).getFirst();
        Match match = new MatchRepository().findByCompetition(competition.getId()).stream()
                .filter(candidate -> candidate.getHomeTeam().getId() == team.getId()
                        || candidate.getAwayTeam().getId() == team.getId()).findFirst().orElseThrow();
        LineupService lineups = new LineupService(new PlayerRepository(),
                new PlayerStateRepository());
        MatchLineup lineup = lineups.selectMatchLineup(team.getId());
        new MatchLineupRepository().save(match.getId(), team.getId(),
                lineup.getStarters(), lineup.getSubstitutes());
        Player captain = lineup.getStarters().getFirst();
        Player penalty = lineup.getStarters().stream().max(
                java.util.Comparator.comparingInt(Player::getShooting)).orElseThrow();
        Player corner = lineup.getStarters().stream().max(
                java.util.Comparator.comparingInt(Player::getPassing)).orElseThrow();
        MatchRoleRepository repository = new MatchRoleRepository();

        repository.save(match.getId(), team.getId(), new MatchRoleRepository.Assignment(
                captain.getId(), penalty.getId(), corner.getId()));

        assertEquals(captain.getId(), repository.find(match.getId(), team.getId()).captainId());
        Player reserve = new PlayerRepository().findCurrentPlayersByTeam(team.getId()).stream()
                .filter(player -> lineup.getStarters().stream().noneMatch(
                        starter -> starter.getId() == player.getId())).findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> repository.save(match.getId(),
                team.getId(), new MatchRoleRepository.Assignment(
                        reserve.getId(), penalty.getId(), corner.getId())));
    }
}
