package footballcareer;

import footballcareer.database.*;
import footballcareer.model.Career;
import footballcareer.model.Player;
import footballcareer.model.Season;
import footballcareer.model.Team;
import footballcareer.service.CareerInsightService;
import footballcareer.service.CareerService;
import footballcareer.service.FootballWorldService;
import footballcareer.service.MedicalTreatmentService;
import footballcareer.service.ManagerEvaluationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CareerInsightServiceTest {

    @Test
    void dashboardInsightsReflectTheLiveCareerState() {
        DatabaseInitializer.resetAndSeedForTests();
        Season season = new SeasonRepository().findFirst();
        new FootballWorldService().prepareSeason(season.getId());
        Team liverpool = new TeamRepository().findByShortName("LIV");
        Career career = new CareerService(new CareerRepository(), new TeamRepository(),
                new SeasonRepository()).createCareer("Insight Test", liverpool.getId(), season.getId());

        CareerInsightService service = new CareerInsightService();

        assertEquals(3, service.objectives(career).size());
        assertTrue(service.news(career).stream().anyMatch(news ->
                news.contains("Próximo partido")));
        assertTrue(service.notifications(career).stream().anyMatch(notification ->
                notification.type() == CareerInsightService.NotificationType.TRAINING));
        assertTrue(new CompetitionTeamRepository().findLeagueNamesByTeam(season.getId())
                .containsKey(liverpool.getId()));

        CareerPreferencesRepository preferences = new CareerPreferencesRepository();
        assertTrue(preferences.find(career.getId()).stopAtMatch());
        preferences.save(career.getId(), new CareerPreferencesRepository.Preferences(
                false, true, false, "EXPERT", "HARD", "TACTICIAN"));
        CareerPreferencesRepository.Preferences reloaded = preferences.find(career.getId());
        assertEquals("EXPERT", reloaded.assistanceLevel());
        assertEquals("HARD", reloaded.difficulty());
        assertEquals("TACTICIAN", reloaded.managerIdentity());
        assertTrue(reloaded.stopOnOffer());
        assertEquals(false, reloaded.stopAtMatch());

        Player patient = new PlayerRepository().findCurrentPlayersByTeam(liverpool.getId())
                .stream().filter(player -> player.getPosition()
                        != footballcareer.model.enums.Position.GK).findFirst().orElseThrow();
        PlayerStateRepository states = new PlayerStateRepository();
        states.setUnavailable(patient.getId(), career.getCurrentDate().plusDays(12), "INJURY");
        MedicalTreatmentService.Result treatment = new MedicalTreatmentService().treat(
                career, patient.getId(), MedicalTreatmentService.Treatment.SPECIALIST);
        assertEquals(career.getCurrentDate().plusDays(7), treatment.newReturn());
        assertTrue(new MedicalTreatmentService().treatedToday(career.getId(),
                patient.getId(), career.getCurrentDate()));
        assertThrows(IllegalStateException.class, () -> new MedicalTreatmentService().treat(
                career, patient.getId(), MedicalTreatmentService.Treatment.REHAB));

        var unhappy = states.findByPlayer(patient.getId());
        unhappy.setMorale(20);
        states.update(unhappy);
        assertTrue(service.notifications(career).stream().anyMatch(notification ->
                notification.type() == CareerInsightService.NotificationType.PLAYER));
        ManagerEvaluationService.Evaluation evaluation = new ManagerEvaluationService()
                .evaluate(career);
        assertTrue(evaluation.confidence() >= 0 && evaluation.confidence() <= 100);
        assertTrue(evaluation.status() != null && !evaluation.status().isBlank());

        career.setCurrentDate(career.getCurrentDate().plusDays(1));
        new CareerRepository().updateCurrentDate(career);
        patient.setOverall(patient.getOverall() + 1);
        new PlayerRepository().updateDevelopment(patient);
        var progress = new PlayerProgressRepository().find(patient.getId());
        assertEquals(2, progress.size());
        assertEquals(patient.getOverall(), progress.getLast().overall());
    }
}
