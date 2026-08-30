package footballcareer.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerRepositoryCacheTest {
    @Test
    void cachesPerCareerAndInvalidatesWhenPlayerDataChanges() {
        DatabaseInitializer.resetAndSeedForTests();
        PlayerRepository.clearReadCache();
        PlayerRepository repository = new PlayerRepository();
        var first = repository.findAll();
        var second = repository.findAll();
        assertSame(first, second);
        assertEquals(1, PlayerRepository.cachedCareerScopes());

        var player = first.getFirst();
        player.setOverall(player.getOverall() + 1);
        repository.updateDevelopment(player);
        assertEquals(0, PlayerRepository.cachedCareerScopes());
        assertNotSame(first, repository.findAll());
    }
}
