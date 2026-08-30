package footballcareer;

import footballcareer.database.PlayerProgressRepository;
import footballcareer.service.PlayerProgressSummaryService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerProgressSummaryServiceTest {
    @Test
    void summarizesDevelopmentAcrossTheWholeCareer() {
        var history = List.of(
                new PlayerProgressRepository.Snapshot(LocalDate.of(2026, 8, 1), 72, 8_000_000),
                new PlayerProgressRepository.Snapshot(LocalDate.of(2027, 1, 1), 75, 12_500_000));
        var summary = new PlayerProgressSummaryService().summarize(history);
        assertEquals(3, summary.overallChange());
        assertEquals(4_500_000, summary.valueChange());
        assertEquals("EN ASCENSO", summary.trend());
    }
}
