package footballcareer;

import footballcareer.model.Competition;
import footballcareer.ui.CompetitionVisuals;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CompetitionVisualsTest {
    @Test
    void domesticZonesIncludeChampionAndMatchEuropeanAllocation() {
        Competition premier = competition("Premier League", "England");
        assertEquals("zone-champions", CompetitionVisuals.standingZone(premier, 0, 20));
        assertEquals("zone-champions", CompetitionVisuals.standingZone(premier, 4, 20));
        assertEquals("zone-europa", CompetitionVisuals.standingZone(premier, 9, 20));
        assertEquals("zone-conference", CompetitionVisuals.standingZone(premier, 14, 20));
        assertNull(CompetitionVisuals.standingZone(premier, 15, 20));

        Competition serieA = competition("Serie A", "Italy");
        assertEquals("zone-conference", CompetitionVisuals.standingZone(serieA, 13, 20));
        assertNull(CompetitionVisuals.standingZone(serieA, 14, 20));
    }

    private Competition competition(String name, String country) {
        Competition competition = new Competition();
        competition.setName(name); competition.setCountry(country);
        return competition;
    }
}
