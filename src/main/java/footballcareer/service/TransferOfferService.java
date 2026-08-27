package footballcareer.service;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.model.enums.TransferOfferStatus;
import java.time.LocalDate;

public class TransferOfferService {
    private final TransferOfferRepository offerRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final ClubFinanceRepository financeRepository;
    private final PlayerMarketRepository marketRepository;

    public TransferOfferService() {
        offerRepository = new TransferOfferRepository();
        playerRepository = new PlayerRepository();
        teamRepository = new TeamRepository();
        playerTeamRepository = new PlayerTeamRepository();
        financeRepository = new ClubFinanceRepository();
        marketRepository = new PlayerMarketRepository();
    }

    public TransferOffer makeOffer(long playerId, long buyingTeamId,
            double amount, LocalDate date) {
        if (amount <= 0) throw new IllegalArgumentException("Offer must be positive.");
        Player player = playerRepository.findById(playerId);
        Team buyer = teamRepository.findById(buyingTeamId);
        Long sellerId = playerTeamRepository.findCurrentTeamId(playerId);
        if (player == null || buyer == null || sellerId == null)
            throw new IllegalArgumentException("Player or club does not exist.");
        if (sellerId == buyingTeamId)
            throw new IllegalArgumentException("A club cannot buy its own player.");
        if (financeRepository.findByTeam(buyingTeamId).getTransferBudget() < amount)
            throw new IllegalStateException("Insufficient transfer budget.");

        TransferOffer offer = new TransferOffer();
        offer.setPlayer(player); offer.setBuyingTeam(buyer);
        offer.setSellingTeam(teamRepository.findById(sellerId));
        offer.setAmount(amount); offer.setOfferDate(date);
        offer.setStatus(TransferOfferStatus.PENDING);
        offerRepository.save(offer);
        return offer;
    }

    public TransferOffer evaluate(long offerId) {
        TransferOffer offer = offerRepository.findById(offerId);
        if (offer == null || offer.getStatus() != TransferOfferStatus.PENDING)
            throw new IllegalStateException("Offer is not pending.");
        Player player = playerRepository.findById(offer.getPlayer().getId());
        Double askingPrice = marketRepository.findAskingPrice(player.getId());
        double required = askingPrice != null ? askingPrice : player.getMarketValue() * 1.10;
        TransferOfferStatus decision = offer.getAmount() >= required
                ? TransferOfferStatus.ACCEPTED : TransferOfferStatus.REJECTED;
        offerRepository.updateStatus(offerId, decision);
        offer.setStatus(decision);
        return offer;
    }
}
