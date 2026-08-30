package footballcareer.service;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.model.enums.TransferOfferStatus;
import java.time.LocalDate;

public class TransferOfferService {
    public record NegotiationQuote(boolean transferListed, double marketValue,
                                   double requiredAmount, double counterFloor,
                                   String stance, String explanation) {}
    private final TransferOfferRepository offerRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final ClubFinanceRepository financeRepository;
    private final PlayerMarketRepository marketRepository;
    private final TransferWindowService windowService;

    public TransferOfferService() {
        offerRepository = new TransferOfferRepository();
        playerRepository = new PlayerRepository();
        teamRepository = new TeamRepository();
        playerTeamRepository = new PlayerTeamRepository();
        financeRepository = new ClubFinanceRepository();
        marketRepository = new PlayerMarketRepository();
        windowService = new TransferWindowService();
    }

    public TransferOffer makeOffer(long playerId, long buyingTeamId,
            double amount, LocalDate date) {
        return makeOffer(playerId, buyingTeamId, amount, date, 100, 0);
    }

    public TransferOffer makeOffer(long playerId, long buyingTeamId,
            double amount, LocalDate date, int upfrontPercent, double appearanceBonus) {
        windowService.requireOpen(date);
        if (amount <= 0) throw new IllegalArgumentException("Offer must be positive.");
        if (!java.util.Set.of(50, 75, 100).contains(upfrontPercent) || appearanceBonus < 0)
            throw new IllegalArgumentException("Invalid transfer payment structure.");
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
        offer.setUpfrontPercent(upfrontPercent); offer.setAppearanceBonus(appearanceBonus);
        offer.setStatus(TransferOfferStatus.PENDING);
        offerRepository.save(offer);
        offerRepository.saveRound(offer.getId(), 1, "BUYER", amount, date);
        return offer;
    }

    public TransferOffer evaluate(long offerId) {
        TransferOffer offer = offerRepository.findById(offerId);
        if (offer == null || offer.getStatus() != TransferOfferStatus.PENDING)
            throw new IllegalStateException("Offer is not pending.");
        Player player = playerRepository.findById(offer.getPlayer().getId());
        NegotiationQuote quote = quote(player.getId());
        double required = quote.requiredAmount();
        if (offer.getAmount() < required
                && offer.getAmount() >= required * quote.counterFloor()) {
            double counterAmount = Math.max(offer.getAmount() * 1.05, required * 0.95);
            offerRepository.setCounterOffer(offerId, counterAmount);
            offerRepository.saveRound(offerId, offerRepository.countBuyerRounds(offerId),
                    "SELLER", counterAmount, offer.getOfferDate());
            offer.setCounterAmount(counterAmount);
            return offer;
        }
        TransferOfferStatus decision = offer.getAmount() >= required
                ? TransferOfferStatus.ACCEPTED : TransferOfferStatus.REJECTED;
        offerRepository.updateStatus(offerId, decision);
        offer.setStatus(decision);
        return offer;
    }

    public NegotiationQuote quote(long playerId) {
        Player player = playerRepository.findById(playerId);
        if (player == null) throw new IllegalArgumentException("Player does not exist.");
        Double askingPrice = marketRepository.findAskingPrice(playerId);
        Long sellerId = playerTeamRepository.findCurrentTeamId(playerId);
        Team seller = sellerId == null ? null : teamRepository.findById(sellerId);
        ClubFinance sellerFinance = sellerId == null ? null : financeRepository.findByTeam(sellerId);
        double squadAverage = sellerId == null ? player.getOverall() : playerRepository
                .findCurrentPlayersByTeam(sellerId).stream().mapToInt(Player::getOverall)
                .average().orElse(player.getOverall());
        ClubNegotiationPolicy.Position position = new ClubNegotiationPolicy().assess(
                askingPrice != null, askingPrice, player.getMarketValue(), player.getOverall(),
                seller == null ? 70 : seller.getReputation(),
                sellerFinance == null ? player.getMarketValue() * 2 : sellerFinance.getTransferBudget(),
                squadAverage);
        return new NegotiationQuote(askingPrice != null, player.getMarketValue(),
                position.requiredAmount(), position.counterFloor(), position.stance(),
                position.explanation());
    }

    public TransferOffer acceptCounterOffer(long offerId) {
        TransferOffer offer = offerRepository.findById(offerId);
        if (offer == null || offer.getCounterAmount() == null) {
            throw new IllegalStateException("There is no counteroffer to accept.");
        }
        if (financeRepository.findByTeam(offer.getBuyingTeam().getId())
                .getTransferBudget() < offer.getCounterAmount()) {
            throw new IllegalStateException("Insufficient transfer budget.");
        }
        offerRepository.acceptCounterOffer(offerId);
        return offerRepository.findById(offerId);
    }

    public TransferOffer submitBuyerCounter(long offerId, double amount) {
        TransferOffer offer = offerRepository.findById(offerId);
        if (offer == null || offer.getStatus() != TransferOfferStatus.PENDING
                || offer.getCounterAmount() == null) {
            throw new IllegalStateException("There is no seller counteroffer to negotiate.");
        }
        int completedBuyerRounds = offerRepository.countBuyerRounds(offerId);
        if (completedBuyerRounds >= 3) {
            offerRepository.updateStatus(offerId, TransferOfferStatus.REJECTED);
            throw new IllegalStateException("The seller ended negotiations after three rounds.");
        }
        if (amount <= offer.getAmount() || amount >= offer.getCounterAmount()) {
            throw new IllegalArgumentException(
                    "The new offer must improve your bid and remain below the seller request.");
        }
        if (financeRepository.findByTeam(offer.getBuyingTeam().getId())
                .getTransferBudget() < amount) {
            throw new IllegalStateException("Insufficient transfer budget.");
        }
        int round = completedBuyerRounds + 1;
        offerRepository.reviseBuyerAmount(offerId, amount);
        offerRepository.saveRound(offerId, round, "BUYER", amount, offer.getOfferDate());
        return evaluate(offerId);
    }

    public TransferOffer respondToIncomingOffer(long offerId, boolean accept) {
        TransferOffer offer = offerRepository.findById(offerId);
        if (offer == null || offer.getStatus() != TransferOfferStatus.PENDING) {
            throw new IllegalStateException("Offer is not pending.");
        }
        offerRepository.updateStatus(offerId, accept
                ? TransferOfferStatus.ACCEPTED : TransferOfferStatus.REJECTED);
        return offerRepository.findById(offerId);
    }

    public TransferOffer respondWithCounterOffer(long offerId, double counterAmount) {
        TransferOffer offer = offerRepository.findById(offerId);
        if (offer == null || offer.getStatus() != TransferOfferStatus.PENDING) {
            throw new IllegalStateException("Offer is not pending.");
        }
        if (counterAmount <= offer.getAmount()) {
            throw new IllegalArgumentException(
                    "Counteroffer must be higher than the received offer.");
        }
        ClubFinance buyerFinance = financeRepository.findByTeam(offer.getBuyingTeam().getId());
        if (buyerFinance == null) throw new IllegalStateException("Buyer finances do not exist.");
        Player player = playerRepository.findById(offer.getPlayer().getId());
        Double askingPrice = marketRepository.findAskingPrice(player.getId());
        double reference = askingPrice == null ? player.getMarketValue() : askingPrice;
        double buyerLimit = reference * 1.05;

        offerRepository.setCounterOffer(offerId, counterAmount);
        if (counterAmount <= buyerLimit
                && counterAmount <= buyerFinance.getTransferBudget()) {
            offerRepository.acceptCounterOffer(offerId);
        } else {
            offerRepository.updateStatus(offerId, TransferOfferStatus.REJECTED);
        }
        return offerRepository.findById(offerId);
    }

    public TransferOffer cancelOffer(long offerId, long buyingTeamId) {
        offerRepository.withdraw(offerId, buyingTeamId, "CANCELLED_BY_BUYER");
        return offerRepository.findById(offerId);
    }

    public int expireOffers(LocalDate currentDate) {
        if (currentDate == null) throw new IllegalArgumentException("Current date is required.");
        return offerRepository.expirePending(currentDate);
    }
}
