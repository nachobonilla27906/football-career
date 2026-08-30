package footballcareer.service;

import footballcareer.database.CareerPreferencesRepository;

public final class ManagerIdentityService {
    public String identity(long careerId) {
        return new CareerPreferencesRepository().find(careerId).managerIdentity();
    }

    public double matchModifier(String identity) {
        return "TACTICIAN".equals(identity) ? 1.0 : 0;
    }

    public int trainingFormBonus(String identity) {
        return "DEVELOPER".equals(identity) ? 1 : 0;
    }

    public int conversationMoraleBonus(String identity, int baseChange) {
        return "MOTIVATOR".equals(identity) && baseChange > 0 ? 2 : 0;
    }
}
