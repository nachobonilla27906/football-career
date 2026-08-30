package footballcareer.service;

import footballcareer.database.PlayerRepository;
import footballcareer.database.TeamRepository;
import footballcareer.database.TransferOfferRepository;
import footballcareer.database.TransferRepository;

import java.time.LocalDate;
import java.util.List;

public final class TransferHistoryService {
    public record Entry(String direction, String player, String fromClub, String toClub,
                        double amount, LocalDate date, String status) {}

    public List<Entry> completed(long teamId) {
        PlayerRepository players = new PlayerRepository();
        TeamRepository teams = new TeamRepository();
        return new TransferRepository().findByTeam(teamId).stream().map(transfer -> new Entry(
                transfer.getToTeam().getId() == teamId ? "ALTA" : "BAJA",
                players.findById(transfer.getPlayer().getId()).getFullName(),
                teams.findById(transfer.getFromTeam().getId()).getShortName(),
                teams.findById(transfer.getToTeam().getId()).getShortName(),
                transfer.getAmount(), transfer.getTransferDate(), "COMPLETADA")).toList();
    }

    public List<Entry> negotiations(long teamId) {
        PlayerRepository players = new PlayerRepository();
        TeamRepository teams = new TeamRepository();
        return new TransferOfferRepository().findHistoryByTeam(teamId).stream().map(offer ->
                new Entry(offer.getBuyingTeam().getId() == teamId ? "ENVIADA" : "RECIBIDA",
                        players.findById(offer.getPlayer().getId()).getFullName(),
                        teams.findById(offer.getSellingTeam().getId()).getShortName(),
                        teams.findById(offer.getBuyingTeam().getId()).getShortName(),
                        offer.getAmount(), offer.getOfferDate(), offer.getStatus().name()))
                .toList();
    }
}
