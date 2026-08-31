package footballcareer.service;

import footballcareer.database.ClubFinanceRepository;
import footballcareer.database.PlayerMarketRepository;
import footballcareer.database.PlayerRepository;
import footballcareer.database.PlayerTeamRepository;
import footballcareer.database.TeamRepository;
import footballcareer.database.TransferRepository;
import footballcareer.model.ClubFinance;
import footballcareer.model.Player;
import footballcareer.model.Team;
import footballcareer.model.Transfer;
import footballcareer.model.TransferOffer;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class ClubTransferAiService {
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final PlayerMarketRepository marketRepository;
    private final ClubFinanceRepository financeRepository;
    private final TransferOfferService offerService;
    private final TransferExecutionService executionService;
    private final TransferRepository transferRepository;
    private final TransferWindowService windowService;
    private final Random random;

    public ClubTransferAiService() {
        this(new TeamRepository(), new PlayerRepository(),
                new PlayerTeamRepository(), new PlayerMarketRepository(),
                new ClubFinanceRepository(), new TransferOfferService(),
                new TransferExecutionService(), new TransferRepository(),
                new TransferWindowService(), new Random());
    }

    public ClubTransferAiService(TeamRepository teamRepository,
            PlayerRepository playerRepository,
            PlayerTeamRepository playerTeamRepository,
            PlayerMarketRepository marketRepository,
            ClubFinanceRepository financeRepository,
            TransferOfferService offerService,
            TransferExecutionService executionService,
            TransferRepository transferRepository,
            TransferWindowService windowService, Random random) {
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.playerTeamRepository = playerTeamRepository;
        this.marketRepository = marketRepository;
        this.financeRepository = financeRepository;
        this.offerService = offerService;
        this.executionService = executionService;
        this.transferRepository = transferRepository;
        this.windowService = windowService;
        this.random = random;
    }

    public List<Transfer> processMarket(LocalDate date, long seasonId,
            long controlledTeamId) {
        if (!windowService.isOpen(date) || date.getDayOfWeek() != DayOfWeek.MONDAY) {
            return List.of();
        }
        List<Team> aiTeams = teamRepository.findAll().stream()
                .filter(team -> team.getId() != controlledTeamId).toList();
        List<Team> weeklyActors = new ArrayList<>(aiTeams);
        java.util.Collections.shuffle(weeklyActors, random);
        weeklyActors = weeklyActors.stream().limit(12).toList();
        weeklyActors.forEach(this::listSurplusPlayer);
        createIncomingOffers(date, controlledTeamId, weeklyActors);

        List<Team> buyers = new ArrayList<>(weeklyActors);
        List<Transfer> completed = new ArrayList<>();
        for (Team buyer : buyers) {
            Player target = chooseAffordableTarget(buyer, controlledTeamId);
            if (target == null) continue;
            Double askingPrice = marketRepository.findAskingPrice(target.getId());
            if (askingPrice == null) continue;
            try {
                TransferOffer offer = offerService.makeOffer(target.getId(), buyer.getId(),
                        askingPrice, date);
                offerService.evaluate(offer.getId());
                executionService.completeTransfer(offer.getId(), target.getSalary(),
                        LocalDate.of(date.getYear() + 3, 6, 30), seasonId, date);
                completed.add(transferRepository.findByOffer(offer.getId()));
            } catch (IllegalArgumentException | IllegalStateException ignored) {
                // Another AI decision may have changed availability during this cycle.
            }
        }
        return completed;
    }

    private void createIncomingOffers(LocalDate date, long controlledTeamId,
            List<Team> aiTeams) {
        marketRepository.findTransferListed(-1).stream()
                .filter(player -> {
                    Long seller = playerTeamRepository.findCurrentTeamId(player.getId());
                    return seller != null && seller == controlledTeamId;
                })
                .filter(player -> !new footballcareer.database.TransferOfferRepository()
                        .hasPendingOfferForPlayer(player.getId()))
                .forEach(player -> {
                    Double asking = marketRepository.findAskingPrice(player.getId());
                    if (asking == null) return;
                    List<Team> possibleBuyers = aiTeams.stream().filter(team -> {
                        ClubFinance finance = financeRepository.findByTeam(team.getId());
                        return finance != null && finance.getTransferBudget() >= asking * 0.82
                                && finance.getAvailableWageBudget() >= player.getSalary();
                    }).toList();
                    if (possibleBuyers.isEmpty()) return;
                    Team buyer = possibleBuyers.get(random.nextInt(possibleBuyers.size()));
                    double amount = asking * (0.82 + random.nextDouble() * 0.20);
                    try {
                        offerService.makeOffer(player.getId(), buyer.getId(), amount, date);
                    } catch (IllegalArgumentException | IllegalStateException ignored) {
                        // The player or budget changed during this market cycle.
                    }
                });
    }

    public void ensureMarketSupply(long controlledTeamId) {
        teamRepository.findAll().stream()
                .filter(team -> team.getId() != controlledTeamId)
                .forEach(this::listMarketSample);
    }

    private void listMarketSample(Team team) {
        List<Player> squad = playerRepository.findCurrentPlayersByTeam(team.getId());
        for (int group = 0; group < 4; group++) {
            int selectedGroup = group;
            squad.stream().filter(player -> positionGroup(player) == selectedGroup)
                    .min(Comparator.comparingInt(Player::getOverall))
                    .ifPresent(player -> marketRepository.listForTransfer(
                            player.getId(), Math.max(100_000, player.getMarketValue())));
        }
    }

    private int positionGroup(Player player) {
        return switch (player.getPosition()) {
            case GK -> 0;
            case CB, LB, RB -> 1;
            case CDM, CM, CAM -> 2;
            case LW, RW, ST -> 3;
        };
    }

    private void listSurplusPlayer(Team team) {
        List<Player> squad = playerRepository.findCurrentPlayersByTeam(team.getId());
        if (squad.size() <= 25) return;
        squad.stream().filter(player -> player.getPosition()
                        != footballcareer.model.enums.Position.GK)
                .min(Comparator.comparingInt(Player::getOverall))
                .ifPresent(player -> marketRepository.listForTransfer(
                        player.getId(), Math.max(100_000, player.getMarketValue())));
    }

    private Player chooseAffordableTarget(Team buyer, long controlledTeamId) {
        ClubFinance finances = financeRepository.findByTeam(buyer.getId());
        if (finances == null) return null;
        return marketRepository.findTransferListed(buyer.getId()).stream()
                .filter(player -> {
                    Long seller = playerTeamRepository.findCurrentTeamId(player.getId());
                    return seller != null && seller != controlledTeamId;
                })
                .filter(player -> {
                    Double price = marketRepository.findAskingPrice(player.getId());
                    return price != null && price <= finances.getTransferBudget()
                            && player.getSalary() <= finances.getAvailableWageBudget();
                })
                .max(Comparator.comparingInt(Player::getOverall))
                .orElse(null);
    }
}
