package footballcareer;

import footballcareer.database.ClubFinanceRepository;
import footballcareer.database.CareerRepository;
import footballcareer.database.DatabaseInitializer;
import footballcareer.database.PlayerRepository;
import footballcareer.database.PlayerTeamRepository;
import footballcareer.database.SeasonRepository;
import footballcareer.database.TeamRepository;
import footballcareer.model.Career;
import footballcareer.model.Player;
import footballcareer.model.Season;
import footballcareer.model.Team;
import footballcareer.service.CareerService;
import footballcareer.service.LoanService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoanServiceTest {
    @Test
    void loanMovesPlayerChargesFeeAndReturnsHimToParentClub() {
        DatabaseInitializer.resetAndSeedForTests();
        TeamRepository teams = new TeamRepository();
        Team arsenal = teams.findByShortName("ARS");
        Team valencia = teams.findByShortName("VCF");
        Season season = new SeasonRepository().findFirst();
        Career career = new CareerService(new CareerRepository(), teams,
                new SeasonRepository()).createCareer(
                "Loan Test", valencia.getId(), season.getId());
        Player player = new PlayerRepository().findCurrentPlayersByTeam(arsenal.getId())
                .getFirst();
        ClubFinanceRepository finances = new ClubFinanceRepository();
        double buyerBefore = finances.findByTeam(valencia.getId()).getTransferBudget();
        double sellerBefore = finances.findByTeam(arsenal.getId()).getTransferBudget();
        LoanService loans = new LoanService();
        LoanService.LoanQuote quote = loans.quote(player.getId(), 6);

        loans.requestLoan(player.getId(), valencia.getId(), quote.requiredFee(),
                6, career.getCurrentDate());

        assertEquals(valencia.getId(), new PlayerTeamRepository()
                .findCurrentTeamId(player.getId()));
        assertEquals(buyerBefore - quote.requiredFee(),
                finances.findByTeam(valencia.getId()).getTransferBudget());
        assertEquals(sellerBefore + quote.requiredFee(),
                finances.findByTeam(arsenal.getId()).getTransferBudget());
        assertThrows(IllegalArgumentException.class, () -> loans.requestLoan(
                player.getId(), valencia.getId(), quote.requiredFee(), 6,
                career.getCurrentDate()));

        assertEquals(0, loans.processReturns(career.getCurrentDate().plusMonths(6).minusDays(1)));
        assertEquals(1, loans.processReturns(career.getCurrentDate().plusMonths(6)));
        assertEquals(arsenal.getId(), new PlayerTeamRepository()
                .findCurrentTeamId(player.getId()));
        assertEquals(0, loans.processReturns(career.getCurrentDate().plusMonths(7)));
    }
}
