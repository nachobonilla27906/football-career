package footballcareer.service;

import footballcareer.database.PlayerRepository;
import footballcareer.model.Player;

import java.time.LocalDate;

public class PlayerDevelopmentService {
    private final PlayerRepository playerRepository;

    public PlayerDevelopmentService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public void applyMonthlyDevelopment(Player player, LocalDate date) {
        int age = player.getAge(date);
        int change = 0;
        if (age <= 23 && player.getOverall() < player.getPotential()) change = 1;
        if (age >= 33) change = -1;
        if (change == 0) return;

        player.setOverall(clamp(player.getOverall() + change));
        player.setPace(clamp(player.getPace() + change));
        player.setShooting(clamp(player.getShooting() + change));
        player.setPassing(clamp(player.getPassing() + change));
        player.setDribbling(clamp(player.getDribbling() + change));
        player.setDefending(clamp(player.getDefending() + change));
        player.setPhysical(clamp(player.getPhysical() + change));
        playerRepository.updateDevelopment(player);
    }

    private int clamp(int value) {
        return Math.max(1, Math.min(99, value));
    }
}
