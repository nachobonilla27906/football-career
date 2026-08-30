package footballcareer.service;

import footballcareer.database.CareerContext;
import footballcareer.database.CareerPreferencesRepository;
import footballcareer.database.CareerRepository;
import footballcareer.model.Career;

public final class CareerDifficultyService {
    private Long cachedCareerId;
    private long controlledTeamId;
    private String difficulty = "NORMAL";

    public double modifier(long teamId) {
        Long careerId = CareerContext.getCareerId();
        if (careerId == null) return 0;
        if (!careerId.equals(cachedCareerId)) load(careerId);
        if (teamId != controlledTeamId) return 0;
        var preferences = new CareerPreferencesRepository().find(careerId);
        difficulty = preferences.difficulty();
        String identity = preferences.managerIdentity();
        return strengthModifier(difficulty, true)
                + new ManagerIdentityService().matchModifier(identity);
    }

    public double strengthModifier(String difficulty, boolean controlledTeam) {
        if (!controlledTeam) return 0;
        return switch (difficulty) {
            case "CASUAL" -> 3.0;
            case "HARD" -> -2.0;
            case "LEGENDARY" -> -4.0;
            default -> 0;
        };
    }

    private void load(long careerId) {
        Career career = new CareerRepository().findById(careerId);
        if (career == null) return;
        cachedCareerId = careerId;
        controlledTeamId = career.getControlledTeam().getId();
        difficulty = new CareerPreferencesRepository().find(careerId).difficulty();
    }
}
