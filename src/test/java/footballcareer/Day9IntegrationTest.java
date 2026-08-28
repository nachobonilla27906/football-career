package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class Day9IntegrationTest {
    @BeforeEach
    void setUp() {
        DatabaseInitializer.resetForTests();
        DataSeeder.seed();
    }

    @Test
    void shouldReloadCareerAfterControlledMatchAndTransfer() {
        Season season = new SeasonRepository().findFirst();
        new FootballWorldService().prepareSeason(season.getId());
        Team controlled = new TeamRepository().findByShortName("VCF");
        MatchRepository matches = new MatchRepository();
        PlayerStateRepository states = new PlayerStateRepository();
        LineupService lineups = new LineupService(new PlayerRepository(), states);
        MatchEventRepository events = new MatchEventRepository();
        MatchDayService matchDays = new MatchDayService(matches,
                new LeagueStandingRepository(),
                new MatchSimulationService(new Random(91), lineups, states),
                new PlayerMatchService(lineups,
                        new PlayerSeasonStatsRepository(), states, events),
                new MatchEventGenerationService(lineups, events, new Random(92)),
                new MatchStatisticsService(events,
                        new MatchTeamStatsRepository(), new Random(93)));
        CareerRepository careers = new CareerRepository();
        CareerService service = new CareerService(careers,
                new TeamRepository(), new SeasonRepository(), matchDays);
        Career career = service.createCareer("Nacho", controlled.getId(), season.getId());

        service.advanceDaysForPlayer(career, 7);
        assertFalse(service.simulateControlledMatchesToday(career).isEmpty());
        Career afterMatch = service.loadCareer(career.getId());
        assertEquals(career.getCurrentDate(), afterMatch.getCurrentDate());
        Match played = matches.findByDate(career.getCurrentDate()).stream()
                .filter(match -> match.getHomeTeam().getId() == controlled.getId()
                        || match.getAwayTeam().getId() == controlled.getId())
                .findFirst().orElseThrow();
        assertNotNull(new MatchReportService().build(played.getId()));

        ClubTransferAiService ai = new ClubTransferAiService();
        ai.ensureMarketSupply(controlled.getId());
        Player target = new PlayerMarketRepository().findTransferListed(controlled.getId())
                .stream().filter(player -> player.getMarketValue() <= 10_000_000)
                .findFirst().orElseThrow();
        double price = new PlayerMarketRepository().findAskingPrice(target.getId());
        TransferOfferService offers = new TransferOfferService();
        TransferOffer offer = offers.makeOffer(target.getId(), controlled.getId(),
                price, LocalDate.of(2027, 1, 4));
        offers.evaluate(offer.getId());
        new TransferExecutionService().completeTransfer(offer.getId(), target.getSalary(),
                LocalDate.of(2030, 6, 30), season.getId(), LocalDate.of(2027, 1, 4));

        Career afterTransfer = service.loadCareer(career.getId());
        assertEquals(controlled.getId(), afterTransfer.getControlledTeam().getId());
        assertEquals(controlled.getId(), new PlayerTeamRepository()
                .findCurrentTeamId(target.getId()));
        assertFalse(new TransferRepository().findByTeam(controlled.getId()).isEmpty());
    }
}
