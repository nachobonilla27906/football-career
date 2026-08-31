package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.service.*;
import footballcareer.ui.ViewportPolicy;
import footballcareer.ui.NavigationState;
import footballcareer.ui.CareerShellView;
import footballcareer.ui.CareerNavigationController;
import footballcareer.ui.DashboardView;
import footballcareer.ui.MarketSearchController;
import footballcareer.ui.ResponsiveContainer;
import footballcareer.ui.PlayerConversationView;
import footballcareer.ui.TransferHistoryView;
import footballcareer.ui.FeedbackAnimator;
import footballcareer.ui.PlayerEvolutionView;
import footballcareer.ui.TransferContractFields;
import footballcareer.ui.RetainedScreenStore;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.Random;

public class Main extends Application {
    private final CareerRepository careers = new CareerRepository();
    private final TeamRepository teams = new TeamRepository();
    private final SeasonRepository seasons = new SeasonRepository();
    private final MatchRepository matches = new MatchRepository();
    private CareerService careerService;
    private Season initialSeason;
    private Career career;
    private Scene appScene;
    private String activeSection = "dashboard";
    private String lastAdvanceSummary;
    private String lastTrainingSummary;
    private Long selectedCompetitionId;
    private java.time.LocalDate selectedResultsDate;
    private java.time.YearMonth calendarMonth;
    private int selectedMarketTab;
    private Long requestedLineupPlayerId;
    private String squadSearchText = "";
    private String squadPositionFilter = "TODAS";
    private boolean marketGlobalMode;
    private String marketSearchText = "";
    private String marketLeagueFilter = "TODAS LAS LIGAS";
    private String marketClubFilter = "TODOS LOS CLUBES";
    private String marketPositionFilter = "TODAS";
    private String marketMaximumPrice = "";
    private String marketMinimumOverall = "";
    private String marketMaximumAge = "";
    private String marketMaximumSalary = "";
    private String marketSort = "PRECIO ↑";
    private boolean marketShortlistOnly;
    private javafx.animation.Animation activeAnimation;
    private final NavigationState navigationState = new NavigationState();
    private final CareerShellView careerShellView = new CareerShellView();
    private final CareerNavigationController navigationController =
            new CareerNavigationController();
    private final DashboardView dashboardView = new DashboardView();
    private final MarketSearchController marketSearchController =
            new MarketSearchController();
    private final ResponsiveContainer responsiveContainer = new ResponsiveContainer();
    private final PlayerConversationView playerConversationView = new PlayerConversationView();
    private final TransferHistoryView transferHistoryView = new TransferHistoryView();
    private final RetainedScreenStore retainedScreens = new RetainedScreenStore();
    private final FeedbackAnimator feedbackAnimator = new FeedbackAnimator();

    @Override
    public void start(Stage stage) {
        footballcareer.ui.UiTheme.loadFonts();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            AppDiagnostics.record(error, "uncaught:" + thread.getName());
            javafx.application.Platform.runLater(() -> {
                if (appScene != null) showMessage("ERROR INESPERADO",
                        "El fallo se ha registrado en Diagnóstico. Puedes continuar y revisar el informe.");
            });
        });
        stage.setTitle("FC//CAREER  —  Beta 0.1");
        stage.setMinWidth(800);
        stage.setMinHeight(560);
        stage.setMaximized(true);
        stage.setFullScreenExitHint("");
        showLoading(stage, "PREPARANDO EL MUNDO", "Cargando clubes, jugadores y competiciones...");
        stage.show();
        initializeApplication(stage);
    }

    private void initializeApplication(Stage stage) {
        showLoading(stage, "PREPARANDO EL MUNDO", "Cargando clubes, jugadores y competiciones...");
        runAsync(stage, () -> {
            DatabaseInitializer.initialize();
            DataSeeder.seed();
            initialSeason = seasons.findFirst();
            new FootballWorldService().prepareSeason(initialSeason.getId());
            careerService = createCareerService();
            return null;
        }, ignored -> showMainMenu(stage));
    }

    private CareerService createCareerService() {
        PlayerStateRepository states = new PlayerStateRepository();
        LineupService lineups = new LineupService(new PlayerRepository(), states);
        MatchEventRepository events = new MatchEventRepository();
        MatchDayService matchDays = new MatchDayService(matches,
                new LeagueStandingRepository(), new MatchSimulationService(),
                new PlayerMatchService(lineups,
                        new PlayerSeasonStatsRepository(), states, events),
                new MatchEventGenerationService(lineups, events, new Random()),
                new MatchStatisticsService(events,
                        new MatchTeamStatsRepository(), new Random()));
        return new CareerService(careers, teams, seasons, matchDays);
    }

    private void ensureTeamHasCalendar(long teamId, long seasonId,
            java.time.LocalDate currentDate) {
        boolean hasFixture = new CompetitionTeamRepository().findCompetitionsByTeam(teamId).stream()
                .filter(competition -> competition.getSeason().getId()
                        == seasonId)
                .flatMap(competition -> matches.findByCompetition(competition.getId()).stream())
                .anyMatch(match -> (match.getHomeTeam().getId() == teamId
                        || match.getAwayTeam().getId() == teamId)
                        && !match.getDate().isBefore(currentDate));
        if (!hasFixture) {
            throw new IllegalStateException("No se pudo generar el calendario del club seleccionado.");
        }
    }

    private void showMainMenu(Stage stage) {
        HBox version = new HBox(10, label("FC//CAREER", "menu-wordmark"),
                label("BETA 0.1", "alpha-badge"));
        version.setAlignment(Pos.CENTER_LEFT);
        VBox brand = new VBox(14, version,
                label("TU CLUB.\nTU HISTORIA.", "hero-title"),
                label("Dirección deportiva, táctica y competición en una experiencia\n"
                        + "de carrera construida decisión a decisión.", "hero-subtitle"));
        brand.setAlignment(Pos.CENTER_LEFT);
        VBox actions = new VBox(14);
        actions.setMaxWidth(330);
        Button newCareer = wideButton("NUEVA CARRERA", "primary-button");
        newCareer.setOnAction(event -> showNewCareer(stage));
        Button loadCareer = wideButton("CARGAR CARRERA", "secondary-button");
        loadCareer.setDisable(careers.findAll().isEmpty());
        loadCareer.setOnAction(event -> showLoadCareer(stage));
        actions.getChildren().addAll(newCareer, loadCareer);
        HBox featureStrip = new HBox(28,
                menuFeature("MUNDO", "5 ligas conectadas"),
                menuFeature("CARRERA", "Progreso persistente"),
                menuFeature("PARTIDOS", "Simulación en directo"));
        VBox menu = new VBox(36, brand, actions, featureStrip,
                label("BUILD BETA 0.1  //  JAVA 21  //  LOCAL CAREER ENGINE", "menu-build"));
        menu.setAlignment(Pos.CENTER_LEFT);
        menu.setMaxWidth(760);
        menu.getStyleClass().add("menu-content");
        setScene(stage, scrollableCentered(menu, "menu-root"));
    }

    private VBox menuFeature(String title, String detail) {
        VBox feature = new VBox(4, label(title, "feature-title"),
                label(detail, "muted-label"));
        feature.getStyleClass().add("menu-feature");
        return feature;
    }

    private void showNewCareer(Stage stage) {
        VBox card = formCard("CREAR NUEVA CARRERA",
                "Elige tu identidad y el club con el que empezarás.");
        card.setPrefWidth(780);
        card.setMaxWidth(900);
        TextField manager = new TextField();
        manager.setPromptText("Nombre del entrenador");
        ComboBox<String> difficulty = new ComboBox<>();
        difficulty.getItems().addAll("CASUAL", "NORMAL", "HARD", "LEGENDARY");
        difficulty.setValue("NORMAL");
        java.util.List<Team> availableTeams = careerService.getAvailableTeams();
        java.util.Map<Long, String> leagueNames = new CompetitionTeamRepository()
                .findLeagueNamesByTeam(initialSeason.getId());
        ClubFinanceRepository financeRepository = new ClubFinanceRepository();
        java.util.Map<Long, ClubFinance> teamFinances = availableTeams.stream().collect(
                java.util.stream.Collectors.toMap(Team::getId,
                        team -> financeRepository.findByTeam(team.getId())));
        TextField clubSearch = new TextField();
        clubSearch.setPromptText("Buscar club...");
        ComboBox<String> country = new ComboBox<>();
        country.getItems().add("TODOS LOS PAÍSES");
        country.getItems().addAll(availableTeams.stream().map(Team::getCountry)
                .distinct().sorted().toList());
        country.setValue("TODOS LOS PAÍSES");
        ListView<Team> club = new ListView<>();
        club.setPrefHeight(250);
        club.setCellFactory(list -> new footballcareer.ui.TeamSelectionCell(
                leagueNames, teamFinances));
        Runnable filterClubs = () -> {
            String query = clubSearch.getText() == null ? ""
                    : clubSearch.getText().trim().toLowerCase();
            club.getItems().setAll(availableTeams.stream()
                    .filter(team -> query.isEmpty()
                            || team.getName().toLowerCase().contains(query)
                            || team.getShortName().toLowerCase().contains(query))
                    .filter(team -> "TODOS LOS PAÍSES".equals(country.getValue())
                            || team.getCountry().equals(country.getValue()))
                    .toList());
        };
        clubSearch.textProperty().addListener((observable, oldValue, newValue) -> filterClubs.run());
        country.setOnAction(event -> filterClubs.run());
        filterClubs.run();
        Label clubDetails = label("Selecciona un club para consultar su proyecto.",
                "comparison-label");
        club.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> {
                    if (selected == null) return;
                    ClubFinance finance = teamFinances.get(selected.getId());
                    clubDetails.setText(selected.getName() + "  •  "
                            + leagueNames.getOrDefault(selected.getId(), selected.getCountry())
                            + "  •  Estadio " + selected.getStadiumName()
                            + " (" + selected.getStadiumCapacity() + ")"
                            + "  •  Reputación " + selected.getReputation()
                            + (finance == null ? "" : String.format(
                            "  •  Presupuesto €%.1fM  •  Margen salarial €%.1fM",
                            finance.getTransferBudget() / 1_000_000,
                            finance.getAvailableWageBudget() / 1_000_000)));
                });
        Label error = label("", "error-label");
        Button create = wideButton("COMENZAR CARRERA", "primary-button");
        create.setOnAction(event -> {
            try {
                if (manager.getText() == null || manager.getText().isBlank()) {
                    throw new IllegalArgumentException("Introduce el nombre del entrenador.");
                }
                if (club.getSelectionModel().getSelectedItem() == null) {
                    throw new IllegalArgumentException("Selecciona un club.");
                }
                String managerName = manager.getText().trim();
                long teamId = club.getSelectionModel().getSelectedItem().getId();
                showLoading(stage, "CREANDO CARRERA", "Preparando calendario, plantilla y finanzas...");
                runAsync(stage, () -> {
                    new FootballWorldService().prepareSeason(initialSeason.getId());
                    ensureTeamHasCalendar(teamId, initialSeason.getId(),
                            initialSeason.getStartDate());
                    return careerService.createCareer(managerName,
                            teamId, initialSeason.getId());
                }, created -> {
                    career = created;
                    CareerPreferencesRepository.Preferences defaults =
                            CareerPreferencesRepository.Preferences.defaults();
                    new CareerPreferencesRepository().save(created.getId(),
                            new CareerPreferencesRepository.Preferences(defaults.stopAtMatch(),
                                    defaults.stopOnOffer(), defaults.stopOnFatigue(),
                                    defaults.assistanceLevel(), difficulty.getValue(),
                                    defaults.managerIdentity()));
                    showDashboard(stage);
                });
            } catch (IllegalArgumentException exception) {
                error.setText(exception.getMessage());
            }
        });
        Button back = wideButton("VOLVER", "ghost-button");
        back.setOnAction(event -> showMainMenu(stage));
        card.getChildren().addAll(manager,
                new FlowPane(10, 10, label("DIFICULTAD", "objective-title"), difficulty),
                new FlowPane(10, 10, clubSearch, country), club, clubDetails,
                error, create, back);
        setScene(stage, scrollableCentered(card, "centered-root"));
    }

    private void showLoadCareer(Stage stage) {
        VBox card = formCard("CARGAR CARRERA", "Continúa una partida guardada.");
        ListView<Career> saves = new ListView<>();
        Runnable refreshSaves = () -> saves.getItems().setAll(careers.findAll());
        refreshSaves.run();
        saves.setCellFactory(list -> new footballcareer.ui.CareerSaveCell());
        saves.setPrefHeight(260);
        Label details = label("Selecciona una partida para ver sus detalles.", "comparison-label");
        TextField renameValue = new TextField();
        renameValue.setPromptText("Nuevo nombre del entrenador");
        saves.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> {
                    if (selected == null) {
                        details.setText("Selecciona una partida para ver sus detalles.");
                        renameValue.clear();
                    } else {
                        details.setText(selected.getControlledTeam().getName() + "  •  Temporada "
                                + selected.getCurrentSeason().getName() + "  •  Fecha "
                                + selected.getCurrentDate() + "  •  ID " + selected.getId());
                        renameValue.setText(selected.getManagerName());
                    }
                });
        Button rename = button("RENOMBRAR", "secondary-button");
        rename.setOnAction(event -> {
            Career selected = saves.getSelectionModel().getSelectedItem();
            if (selected != null && !renameValue.getText().isBlank()) {
                careers.rename(selected.getId(), renameValue.getText());
                refreshSaves.run();
            }
        });
        Button delete = button("ELIMINAR PARTIDA", "danger-button");
        delete.setOnAction(event -> {
            Career selected = saves.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            showDecision("ELIMINAR PARTIDA",
                    "¿Eliminar la carrera de " + selected.getManagerName()
                            + "? Esta acción borrará la partida guardada.", () -> {
                careers.delete(selected.getId());
                refreshSaves.run();
            });
        });
        Button duplicate = button("DUPLICAR PARTIDA", "secondary-button");
        duplicate.setOnAction(event -> {
            Career selected = saves.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            showDecision("DUPLICAR PARTIDA",
                    "Se copiará íntegramente el progreso de " + selected.getManagerName() + ".",
                    () -> {
                        careers.duplicate(selected.getId());
                        refreshSaves.run();
                        details.setText("Copia completa creada correctamente.");
                    });
        });
        Button repair = button("VERIFICAR Y REPARAR", "ghost-button");
        repair.setOnAction(event -> {
            Career selected = saves.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            try {
                CareerRepairService.RepairReport report = new CareerRepairService()
                        .repair(selected.getId());
                details.setText(report.detail() + "  •  Fecha válida: " + report.date());
                refreshSaves.run();
            } catch (IllegalArgumentException | IllegalStateException exception) {
                details.setText("No se pudo reparar: " + exception.getMessage());
            }
        });
        Button load = wideButton("CARGAR PARTIDA", "primary-button");
        load.setOnAction(event -> {
            Career selected = saves.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showLoading(stage, "CARGANDO CARRERA", "Recuperando el estado de la temporada...");
                runAsync(stage, () -> {
                    Career loaded = careerService.loadCareer(selected.getId());
                    new FootballWorldService().prepareSeason(loaded.getCurrentSeason().getId());
                    return loaded;
                }, loaded -> {
                    career = loaded;
                    showDashboard(stage);
                });
            }
        });
        Button back = wideButton("VOLVER", "ghost-button");
        back.setOnAction(event -> showMainMenu(stage));
        card.getChildren().addAll(saves, details, renameValue,
                new FlowPane(10, 10, rename, duplicate, repair, delete), load, back);
        setScene(stage, scrollableCentered(card, "centered-root"));
    }

    private void showDashboard(Stage stage) {
        activeSection = "dashboard";
        ManagerReputationService.Reputation reputation = new ManagerReputationService()
                .find(career);
        ClubFinance finance = new ClubFinanceRepository()
                .findByTeam(career.getControlledTeam().getId());
        Match next = findNextMatch();
        Match todayControlled = findControlledMatchToday();
        CareerInsightService insights = new CareerInsightService();
        DashboardView.Model model = new DashboardView.Model(
                career.getControlledTeam().getName(), career.getCurrentSeason().getName(),
                career.getCurrentDate(), reputation.display(), finance == null ? "—"
                : String.format("€%.1fM", finance.getTransferBudget() / 1_000_000),
                leaguePositionSummary(), next, career.getControlledTeam().getId(),
                todayControlled != null && !todayControlled.isPlayed(), squadStatusSummary(),
                recentFormData(), new CareerActivityService().recent(career, 10),
                insights.news(career), insights.notifications(career), lastAdvanceSummary);
        DashboardView.Actions actions = new DashboardView.Actions(
                () -> advanceCareer(stage, 1), () -> {
            Match upcoming = findNextMatch();
            long untilMatch = upcoming == null ? 7 : java.time.temporal.ChronoUnit.DAYS.between(
                    career.getCurrentDate(), upcoming.getDate());
            advanceCareer(stage, Math.max(1, (int) Math.min(7, untilMatch)));
        }, () -> {
            Match today = findControlledMatchToday();
            if (today != null && !today.isPlayed()) {
                showMatchPreview(stage, today);
                return;
            }
            Match upcoming = findNextMatch();
            if (upcoming != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(
                        career.getCurrentDate(), upcoming.getDate());
                if (days > 0) advanceCareer(stage, Math.toIntExact(days), true);
            }
        }, () -> showNotifications(stage), item -> openNotification(stage, item.type()));
        showCareerShell(stage, dashboardView.build(model, actions));
    }

    private java.util.List<DashboardView.FormResult> recentFormData() {
        long teamId = career.getControlledTeam().getId();
        java.util.List<Match> recent = currentCompetitions().stream()
                .flatMap(competition -> matches.findByCompetition(competition.getId()).stream())
                .filter(Match::isPlayed)
                .filter(match -> match.getHomeTeam().getId() == teamId
                        || match.getAwayTeam().getId() == teamId)
                .sorted(Comparator.comparing(Match::getDate).reversed()).limit(5).toList();
        return recent.reversed().stream().map(match -> {
            boolean home = match.getHomeTeam().getId() == teamId;
            int own = home ? match.getHomeGoals() : match.getAwayGoals();
            int rival = home ? match.getAwayGoals() : match.getHomeGoals();
            return new DashboardView.FormResult(own > rival ? "V" : own == rival ? "E" : "D",
                    match.getHomeTeam().getShortName() + " "
                    + match.getHomeGoals() + "–" + match.getAwayGoals() + " "
                    + match.getAwayTeam().getShortName());
        }).toList();
    }

    private void advanceCareer(Stage stage, int days) {
        advanceCareer(stage, days, false);
    }

    private void advanceCareer(Stage stage, int days, boolean matchOnly) {
        java.time.LocalDate from = career.getCurrentDate();
        showLoading(stage, "SIMULANDO EL MUNDO",
                days == 1 ? "Avanzando un día..." : "Avanzando hasta " + days + " días...");
        runAsync(stage, () -> {
            CareerPreferencesRepository.Preferences preferences =
                    new CareerPreferencesRepository().find(career.getId());
            int initialOffers = new TransferOfferRepository()
                    .countPendingBySellingTeam(career.getControlledTeam().getId());
            String stopReason = null;
            int advanced = 0;
            for (int index = 0; index < days; index++) {
                careerService.advanceDayForPlayer(career);
                advanced++;
                if ((matchOnly || preferences.stopAtMatch())
                        && findControlledMatchToday() != null) {
                    stopReason = "partido del equipo";
                    break;
                }
                int offers = new TransferOfferRepository()
                        .countPendingBySellingTeam(career.getControlledTeam().getId());
                if (!matchOnly && preferences.stopOnOffer() && offers > initialOffers) {
                    stopReason = "nueva oferta recibida";
                    break;
                }
                if (!matchOnly && preferences.stopOnFatigue() && tiredPlayerCount() >= 4) {
                    stopReason = "alerta de fatiga";
                    break;
                }
            }
            return new AdvanceOutcome(career, advanced, stopReason);
        }, outcome -> {
            career = outcome.career();
            lastAdvanceSummary = "Del " + from + " al " + career.getCurrentDate()
                    + "  •  " + outcome.daysAdvanced() + " día(s) avanzados. "
                    + (outcome.stopReason() == null ? "Periodo completado."
                    : "Avance detenido por " + outcome.stopReason() + ".")
                    + " La partida se ha guardado automáticamente.";
            showDashboard(stage);
        });
    }

    private record AdvanceOutcome(Career career, int daysAdvanced, String stopReason) {}

    private long tiredPlayerCount() {
        java.util.Map<Long, PlayerState> states = new PlayerStateRepository().findAll();
        return new PlayerRepository().findCurrentPlayersByTeam(
                career.getControlledTeam().getId()).stream()
                .map(player -> states.get(player.getId()))
                .filter(java.util.Objects::nonNull)
                .filter(state -> state.getFitness() < 70).count();
    }

    private String leaguePositionSummary() {
        if (currentCompetitions().isEmpty()) return "—";
        java.util.List<LeagueStanding> table = new LeagueStandingRepository()
                .findByCompetition(currentCompetitions().getFirst().getId());
        for (int index = 0; index < table.size(); index++) {
            if (table.get(index).getTeam().getId() == career.getControlledTeam().getId()) {
                return (index + 1) + "º de " + table.size();
            }
        }
        return "—";
    }

    private String squadStatusSummary() {
        java.util.List<Player> squad = new PlayerRepository()
                .findCurrentPlayersByTeam(career.getControlledTeam().getId());
        PlayerStateRepository repository = new PlayerStateRepository();
        java.util.Map<Long, PlayerState> allStates = repository.findAll();
        java.util.List<PlayerState> states = squad.stream().map(player ->
                allStates.get(player.getId())).filter(java.util.Objects::nonNull).toList();
        if (states.isEmpty()) return "No hay información de estado disponible.";
        double fitness = states.stream().mapToInt(PlayerState::getFitness).average().orElse(0);
        double morale = states.stream().mapToInt(PlayerState::getMorale).average().orElse(0);
        long tired = states.stream().filter(state -> state.getFitness() < 70).count();
        return String.format("Fitness medio %.0f  •  Moral media %.0f  •  %d jugadores fatigados",
                fitness, morale, tired);
    }

    private void showSquad(Stage stage) {
        activeSection = "squad";
        VBox content = page("PLANTILLA",
                career.getControlledTeam().getName() + "  •  Estado actual");
        java.util.List<Player> squad = new PlayerRepository()
                .findCurrentPlayersByTeam(career.getControlledTeam().getId());
        PlayerStateRepository states = new PlayerStateRepository();
        java.util.Map<Long, PlayerState> allStates = states.findAll();
        java.util.Map<Long, PlayerState> playerStates = new java.util.HashMap<>();
        squad.forEach(player -> playerStates.put(player.getId(), allStates.get(player.getId())));
        TextField search = new TextField();
        search.setPromptText("Buscar jugador...");
        search.setText(squadSearchText);
        ComboBox<String> position = new ComboBox<>();
        position.getItems().addAll("TODAS", "GK", "CB", "LB", "RB", "CDM", "CM", "CAM", "LW", "RW", "ST");
        position.setValue(squadPositionFilter);
        ListView<Player> table = new ListView<>();
        table.getStyleClass().add("squad-modern-list");
        table.setCellFactory(view -> new footballcareer.ui.SquadPlayerCell(career.getCurrentDate(),
                playerStates, player -> squadRole(player, squad)));
        footballcareer.ui.SquadPlayerDetailPane detailPane =
                new footballcareer.ui.SquadPlayerDetailPane();
        Runnable filter = () -> {
            squadSearchText = search.getText() == null ? "" : search.getText();
            squadPositionFilter = position.getValue();
            String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
            String selectedPosition = position.getValue();
            table.getItems().setAll(squad.stream()
                    .filter(player -> query.isEmpty()
                            || player.getFullName().toLowerCase().contains(query))
                    .filter(player -> "TODAS".equals(selectedPosition)
                            || player.getPosition().name().equals(selectedPosition)).toList());
            Long remembered = navigationState.selection("squad");
            if (remembered != null) table.getItems().stream()
                    .filter(player -> player.getId() == remembered).findFirst()
                    .ifPresent(player -> table.getSelectionModel().select(player));
        };
        table.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> {
                    if (selected != null) { navigationState.rememberSelection("squad", selected.getId());
                        detailPane.show(selected, playerStates.get(selected.getId()),
                                squadRole(selected, squad), career.getCurrentDate()); }
                });
        search.textProperty().addListener((observable, oldValue, newValue) -> filter.run());
        position.setOnAction(event -> filter.run());
        filter.run();
        VBox.setVgrow(table, Priority.ALWAYS);
        Button details = button("VER FICHA", "primary-button");
        details.setOnAction(event -> {
            Player selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) showPlayer(stage, selected, "squad");
        });
        Button compare = button("COMPARAR JUGADORES", "secondary-button");
        compare.setOnAction(event -> showPlayerComparison(squad, playerStates,
                table.getSelectionModel().getSelectedItem()));
        table.setOnMouseClicked(event -> { if (event.getClickCount() == 2
                && table.getSelectionModel().getSelectedItem() != null)
            showPlayer(stage, table.getSelectionModel().getSelectedItem(), "squad"); });
        Label squadCount = label(squad.size() + " JUGADORES", "market-result-count");
        HBox filters = new HBox(12, search, position, squadCount);
        VBox roster = new VBox(12, filters, table); roster.getStyleClass().add("squad-roster");
        VBox actions = new VBox(12, detailPane, details, compare);
        actions.getStyleClass().add("squad-actions"); actions.setPrefWidth(280);
        details.setMaxWidth(Double.MAX_VALUE); compare.setMaxWidth(Double.MAX_VALUE);
        HBox workspace = new HBox(16, roster, actions); HBox.setHgrow(roster, Priority.ALWAYS);
        VBox.setVgrow(workspace, Priority.ALWAYS); content.getChildren().add(workspace);
        showCareerShell(stage, content);
    }

    private void showPlayerComparison(java.util.List<Player> squad,
            java.util.Map<Long, PlayerState> states, Player initiallySelected) {
        ComboBox<Player> first = new ComboBox<>();
        ComboBox<Player> second = new ComboBox<>();
        first.getItems().addAll(squad);
        second.getItems().addAll(squad);
        first.setConverter(playerStringConverter());
        second.setConverter(playerStringConverter());
        first.setValue(initiallySelected == null ? squad.getFirst() : initiallySelected);
        second.setValue(squad.stream().filter(player -> player != first.getValue()).findFirst()
                .orElse(squad.getFirst()));
        GridPane comparison = new GridPane();
        comparison.getStyleClass().add("attributes-grid");
        comparison.setHgap(22);
        comparison.setVgap(10);
        Runnable refresh = () -> {
            comparison.getChildren().clear();
            Player left = first.getValue();
            Player right = second.getValue();
            if (left == null || right == null) return;
            String[] labels = {"MEDIA", "POTENCIAL", "RITMO", "TIRO", "PASE",
                    "REGATE", "DEFENSA", "FÍSICO", "FORMA", "FITNESS", "VALOR €M"};
            double[] leftValues = comparisonValues(left, states.get(left.getId()));
            double[] rightValues = comparisonValues(right, states.get(right.getId()));
            for (int row = 0; row < labels.length; row++) {
                Label leftValue = label(formatComparisonValue(leftValues[row], row),
                        leftValues[row] >= rightValues[row] ? "success-feedback" : "body-label");
                Label rightValue = label(formatComparisonValue(rightValues[row], row),
                        rightValues[row] >= leftValues[row] ? "success-feedback" : "body-label");
                comparison.add(leftValue, 0, row);
                comparison.add(label(labels[row], "muted-label"), 1, row);
                comparison.add(rightValue, 2, row);
            }
        };
        first.setOnAction(event -> refresh.run());
        second.setOnAction(event -> refresh.run());
        refresh.run();
        Button close = button("CERRAR", "ghost-button");
        VBox dialog = new VBox(14, label("COMPARADOR DE PLANTILLA", "form-title"),
                new HBox(12, first, second), comparison, close);
        dialog.getStyleClass().add("in-app-dialog");
        dialog.setPrefWidth(620);
        Runnable dismiss = showOverlay(dialog);
        close.setOnAction(event -> dismiss.run());
    }

    private javafx.util.StringConverter<Player> playerStringConverter() {
        return new javafx.util.StringConverter<>() {
            @Override public String toString(Player player) {
                return player == null ? "" : player.getPosition() + "  •  "
                        + player.getFullName() + "  •  " + player.getOverall();
            }
            @Override public Player fromString(String text) { return null; }
        };
    }

    private double[] comparisonValues(Player player, PlayerState state) {
        return new double[] {player.getOverall(), player.getPotential(), player.getPace(),
                player.getShooting(), player.getPassing(), player.getDribbling(),
                player.getDefending(), player.getPhysical(), state == null ? 0 : state.getForm(),
                state == null ? 0 : state.getFitness(), player.getMarketValue() / 1_000_000};
    }

    private String formatComparisonValue(double value, int row) {
        return row == 10 ? String.format("%.1f", value) : String.valueOf((int) value);
    }

    private String squadRole(Player player, java.util.List<Player> squad) {
        long stronger = squad.stream().filter(other -> other.getOverall() > player.getOverall()).count();
        if (stronger < 5) return "CLAVE";
        if (stronger < 11) return "TITULAR";
        if (stronger < 18) return "ROTACIÓN";
        return "PROMESA";
    }

    private String availabilityLabel(PlayerState state, java.time.LocalDate date) {
        if (state == null || state.isAvailableOn(date)) return "DISPONIBLE";
        String reason = "SUSPENSION".equals(state.getUnavailableReason())
                ? "SANCIONADO" : "LESIONADO";
        return reason + " HASTA " + state.getUnavailableUntil();
    }

    private void showCareerShell(Stage stage, Node content) {
        String area = navigationController.areaFor(activeSection);
        int notificationCount = new CareerInsightService().notifications(career).size();
        int incomingOffers = new TransferOfferRepository()
                .countPendingBySellingTeam(career.getControlledTeam().getId());
        java.util.List<CareerShellView.NavigationItem> mainItems = java.util.List.of(
                navItem("CENTRAL", "central".equals(area), () -> showDashboard(stage)),
                navItem("PLANTILLA", "squad".equals(area), () -> showSquad(stage)),
                navItem("TRASPASOS" + (incomingOffers == 0 ? "" : "  //  " + incomingOffers),
                        "transfers".equals(area), () -> {
                            selectedMarketTab = 0; showMarket(stage);
                        }),
                navItem("OFICINA", "office".equals(area), () -> showOffice(stage)),
                navItem("PERSONALIZAR", "customize".equals(area), () -> showSettings(stage)));
        BorderPane shell = careerShellView.build(career, notificationCount, mainItems,
                subNavigationItems(stage, area), () -> showNotifications(stage), () -> {
            showDecision("GUARDAR Y SALIR",
                    "¿Volver al menú principal? El progreso actual ya está guardado.", () -> {
                career = null;
                activeSection = "dashboard";
                navigationState.clear();
                showMainMenu(stage);
            });
        }, content, activeSection, navigationState);
        setScene(stage, shell);
    }

    private CareerShellView.NavigationItem navItem(String text, boolean selected,
            Runnable action) {
        return new CareerShellView.NavigationItem(text, selected, action);
    }

    private java.util.List<CareerShellView.NavigationItem> subNavigationItems(
            Stage stage, String area) {
        java.util.List<CareerShellView.NavigationItem> navigation = new java.util.ArrayList<>();
        if ("central".equals(area)) {
            navigation.add(subNav("CENTRAL", "dashboard", () -> showDashboard(stage)));
            navigation.add(subNav("CALENDARIO", "calendar", () -> showCalendar(stage)));
            navigation.add(subNav("CLASIFICACIÓN", "standings", () -> showStandings(stage)));
            navigation.add(subNav("RESULTADOS", "results", () -> showResults(stage)));
            navigation.add(subNav("BANDEJA", "notifications", () -> showNotifications(stage)));
        } else if ("squad".equals(area)) {
            navigation.add(subNav("JUGADORES", "squad", () -> showSquad(stage)));
            navigation.add(subNav("ALINEACIÓN", "lineup", () -> showLineup(stage)));
            navigation.add(subNav("ENTRENAMIENTO", "training", () -> showTraining(stage)));
            navigation.add(subNav("CENTRO MÉDICO", "medical", () -> showMedical(stage)));
        } else if ("transfers".equals(area)) {
            navigation.add(subNav("MERCADO", "market", () -> {
                selectedMarketTab = 0; showMarket(stage);
            }));
            navigation.add(subNav("VENTAS", "sales", () -> {
                selectedMarketTab = 1; showMarket(stage);
            }));
            navigation.add(subNav("OFERTAS", "offers", () -> {
                selectedMarketTab = 2; showMarket(stage);
            }));
            int incoming = new TransferOfferRepository()
                    .countPendingBySellingTeam(career.getControlledTeam().getId());
            navigation.add(subNav("RECIBIDAS" + (incoming == 0 ? "" : "  (" + incoming + ")"),
                    "incoming", () -> {
                selectedMarketTab = 3; showMarket(stage);
            }));
            navigation.add(subNav("HISTORIAL", "history", () -> showTransferHistory(stage)));
        } else if ("office".equals(area)) {
            navigation.add(subNav("VISIÓN GENERAL", "office", () -> showOffice(stage)));
        } else {
            navigation.add(subNav("AJUSTES", "settings", () -> showSettings(stage)));
        }
        return java.util.List.copyOf(navigation);
    }

    private CareerShellView.NavigationItem subNav(String text, String section, Runnable action) {
        return navItem(text, navigationController.isSelected(
                section, activeSection, selectedMarketTab), action);
    }

    private void showNotifications(Stage stage) {
        activeSection = "notifications";
        CareerInsightService service = new CareerInsightService();
        java.util.List<CareerInsightService.Notification> items = service.notifications(career);
        VBox content = page("BANDEJA", "Asuntos que requieren tu atención como manager");
        long urgent = items.stream().filter(CareerInsightService.Notification::urgent).count();
        HBox summary = new HBox(18, label(items.size() + " PENDIENTES", "inbox-count"),
                label(urgent + " URGENTES", urgent > 0 ? "inbox-urgent" : "muted-label"));
        summary.getStyleClass().add("inbox-summary");
        VBox inbox = panel("CENTRO DE NOTIFICACIONES");
        if (items.isEmpty()) {
            inbox.getChildren().addAll(label("TODO AL DÍA", "empty-title"),
                    label("No tienes partidos inminentes, ofertas ni tareas pendientes.",
                            "muted-label"));
        } else {
            items.forEach(item -> inbox.getChildren().add(notificationRow(stage, item, false)));
        }
        VBox.setVgrow(inbox, Priority.ALWAYS); content.getChildren().addAll(summary, inbox);
        showCareerShell(stage, content);
    }

    private HBox notificationRow(Stage stage, CareerInsightService.Notification item,
            boolean compact) {
        Label type = label(switch (item.type()) {
            case MATCH -> "PARTIDO";
            case TRANSFER -> "TRASPASO";
            case FITNESS -> "FÍSICO";
            case TRAINING -> "ENTRENO";
            case MEDICAL -> "MÉDICO";
            case PLAYER -> "JUGADOR";
            case BOARD -> "DIRECTIVA";
        }, item.urgent() ? "notification-type-urgent" : "notification-type");
        VBox copy = new VBox(3, label(item.title(), "notification-title"),
                label(item.detail(), "muted-label"));
        HBox.setHgrow(copy, Priority.ALWAYS);
        Button action = button(notificationActionText(item.type()), "ghost-button");
        action.setOnAction(event -> openNotification(stage, item.type()));
        HBox row = new HBox(12, type, copy, action);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("notification-row");
        if (compact) row.getStyleClass().add("notification-row-compact");
        return row;
    }

    private String notificationActionText(CareerInsightService.NotificationType type) {
        return switch (type) {
            case MATCH -> "PREPARAR";
            case TRANSFER -> "RESPONDER";
            case FITNESS -> "GESTIONAR";
            case TRAINING -> "PLANIFICAR";
            case MEDICAL -> "REVISAR";
            case PLAYER -> "GESTIONAR";
            case BOARD -> "REVISAR";
        };
    }

    private void openNotification(Stage stage, CareerInsightService.NotificationType type) {
        switch (type) {
            case TRANSFER -> {
                selectedMarketTab = 2;
                showMarket(stage);
            }
            case FITNESS, TRAINING -> showTraining(stage);
            case MEDICAL -> showMedical(stage);
            case PLAYER -> showSquad(stage);
            case BOARD -> showOffice(stage);
            case MATCH -> {
                Match next = findNextMatch();
                if (next != null && next.getDate().equals(career.getCurrentDate())) {
                    showMatchPreview(stage, next);
                } else {
                    calendarMonth = java.time.YearMonth.from(career.getCurrentDate());
                    showCalendar(stage);
                }
            }
        }
    }

    private void showCalendar(Stage stage) {
        activeSection = "calendar";
        if (calendarMonth == null) calendarMonth = java.time.YearMonth.from(career.getCurrentDate());
        VBox content = page("CALENDARIO", "Planificación mensual de la temporada");
        java.util.List<Match> allFixtures = currentCompetitions().stream()
                .flatMap(competition -> matches.findByCompetition(competition.getId()).stream())
                .sorted(Comparator.comparing(Match::getDate))
                .toList();
        ComboBox<String> competitionFilter = new ComboBox<>();
        competitionFilter.getItems().add("TODAS LAS COMPETICIONES");
        competitionFilter.getItems().addAll(currentCompetitions().stream()
                .map(Competition::getName).toList());
        competitionFilter.setValue("TODAS LAS COMPETICIONES");
        CheckBox myMatches = new CheckBox("Solo partidos de mi club");

        Button previous = button("‹", "secondary-button");
        Button next = button("›", "secondary-button");
        Button today = button("HOY", "secondary-button");
        Label monthTitle = label("", "calendar-month-title");
        Region monthSpacer = new Region();
        HBox.setHgrow(monthSpacer, Priority.ALWAYS);
        HBox toolbar = new HBox(10, previous, next, today, monthTitle, monthSpacer,
                competitionFilter, myMatches);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        GridPane calendar = new GridPane();
        calendar.getStyleClass().add("month-calendar");
        calendar.setMaxWidth(Double.MAX_VALUE);
        for (int column = 0; column < 7; column++) {
            ColumnConstraints constraint = new ColumnConstraints();
            constraint.setPercentWidth(100.0 / 7);
            constraint.setHgrow(Priority.ALWAYS);
            calendar.getColumnConstraints().add(constraint);
        }
        VBox dayDetail = panel("DETALLE DEL DÍA");
        java.time.LocalDate[] selectedDay = {career.getCurrentDate()};
        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            monthTitle.setText(calendarMonth.getMonth().getDisplayName(
                    java.time.format.TextStyle.FULL, java.util.Locale.of("es", "ES")).toUpperCase()
                    + "  " + calendarMonth.getYear());
            java.util.List<Match> visible = allFixtures.stream()
                    .filter(match -> "TODAS LAS COMPETICIONES".equals(competitionFilter.getValue())
                            || match.getCompetition().getName().equals(competitionFilter.getValue()))
                    .filter(match -> !myMatches.isSelected() || isControlledMatch(match)).toList();
            java.util.Map<java.time.LocalDate, TrainingService.TrainingType> training =
                    new TrainingService().findByMonth(career, calendarMonth);
            renderMonthCalendar(calendar, calendarMonth, selectedDay[0], visible, training, date -> {
                selectedDay[0] = date;
                if (!java.time.YearMonth.from(date).equals(calendarMonth)) {
                    calendarMonth = java.time.YearMonth.from(date);
                }
                refresh[0].run();
            });
            renderCalendarDayDetail(stage, dayDetail, selectedDay[0], visible,
                    training.get(selectedDay[0]));
        };
        previous.setOnAction(event -> { calendarMonth = calendarMonth.minusMonths(1); refresh[0].run(); });
        next.setOnAction(event -> { calendarMonth = calendarMonth.plusMonths(1); refresh[0].run(); });
        today.setOnAction(event -> {
            selectedDay[0] = career.getCurrentDate();
            calendarMonth = java.time.YearMonth.from(career.getCurrentDate());
            refresh[0].run();
        });
        competitionFilter.setOnAction(event -> refresh[0].run());
        myMatches.setOnAction(event -> refresh[0].run());
        refresh[0].run();
        content.getChildren().addAll(toolbar, calendar, dayDetail);
        showCareerShell(stage, content);
    }

    private void renderMonthCalendar(GridPane grid, java.time.YearMonth month,
            java.time.LocalDate selected, java.util.List<Match> fixtures,
            java.util.Map<java.time.LocalDate, TrainingService.TrainingType> training,
            java.util.function.Consumer<java.time.LocalDate> selection) {
        grid.getChildren().clear();
        String[] weekdays = {"LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM"};
        for (int column = 0; column < weekdays.length; column++) {
            Label heading = label(weekdays[column], "calendar-weekday");
            heading.setMaxWidth(Double.MAX_VALUE);
            grid.add(heading, column, 0);
        }
        java.time.LocalDate first = month.atDay(1);
        java.time.LocalDate start = first.minusDays(first.getDayOfWeek().getValue() - 1L);
        long controlledTeam = career.getControlledTeam().getId();
        for (int offset = 0; offset < 42; offset++) {
            java.time.LocalDate date = start.plusDays(offset);
            VBox cell = new VBox(7);
            cell.getStyleClass().add("calendar-day");
            if (!java.time.YearMonth.from(date).equals(month)) cell.getStyleClass().add("calendar-day-outside");
            if (date.equals(career.getCurrentDate())) cell.getStyleClass().add("calendar-day-today");
            if (date.equals(selected)) cell.getStyleClass().add("calendar-day-selected");
            Label number = label(String.valueOf(date.getDayOfMonth()), "calendar-day-number");
            cell.getChildren().add(number);
            java.util.List<Match> dayMatches = fixtures.stream()
                    .filter(match -> match.getDate().equals(date)).toList();
            dayMatches.stream().filter(this::isControlledMatch).findFirst().ifPresent(match -> {
                boolean home = match.getHomeTeam().getId() == controlledTeam;
                Team opponent = home ? match.getAwayTeam() : match.getHomeTeam();
                Label competition = label(footballcareer.ui.CompetitionVisuals.shortName(match.getCompetition()), "calendar-competition-tag");
                Label opponentName = label(opponent.getShortName(), "calendar-opponent");
                Label venue = label(home ? "LOCAL" : "VISITANTE", "calendar-venue");
                HBox crests = new HBox(5, footballcareer.ui.TeamCrestView.create(match.getHomeTeam(), 21),
                        label("VS", "calendar-versus"),
                        footballcareer.ui.TeamCrestView.create(match.getAwayTeam(), 21));
                crests.setAlignment(Pos.CENTER_LEFT);
                HBox identity = new HBox(6, crests, opponentName, venue);
                identity.setAlignment(Pos.CENTER_LEFT); VBox event = new VBox(3, competition, identity);
                event.getStyleClass().addAll("calendar-match-card", footballcareer.ui.CompetitionVisuals.calendarStyle(match.getCompetition()));
                if (match.isPlayed()) event.getChildren().add(label(match.getHomeGoals() + " — " + match.getAwayGoals(), "calendar-score"));
                cell.getChildren().add(event);
            });
            long otherMatches = dayMatches.stream().filter(match -> !isControlledMatch(match)).count();
            if (otherMatches > 0) cell.getChildren().add(label("+ " + otherMatches + (otherMatches == 1 ? " partido" : " partidos"), "calendar-other-count"));
            if (training.containsKey(date)) cell.getChildren().add(label("✓  "
                    + trainingName(training.get(date)), "calendar-training"));
            cell.setOnMouseClicked(event -> selection.accept(date));
            grid.add(cell, offset % 7, offset / 7 + 1);
        }
    }

    private void renderCalendarDayDetail(Stage stage, VBox detail, java.time.LocalDate date,
            java.util.List<Match> fixtures, TrainingService.TrainingType training) {
        detail.getChildren().setAll(label("DETALLE DEL DÍA  •  " + date, "panel-title"));
        if (training != null) detail.getChildren().add(label("Entrenamiento completado: "
                + trainingName(training), "calendar-training-detail"));
        java.util.List<Match> dayMatches = fixtures.stream()
                .filter(match -> match.getDate().equals(date)).toList();
        if (dayMatches.isEmpty()) {
            detail.getChildren().add(label(date.equals(career.getCurrentDate()) && training == null
                    ? "Hoy no hay partidos. Puedes entrenar o avanzar el calendario."
                    : "No hay encuentros programados.", "muted-label"));
            return;
        }
        for (Match match : dayMatches) {
            Label competition = label(footballcareer.ui.CompetitionVisuals.shortName(
                    match.getCompetition()), "calendar-competition-tag");
            Label fixture = label(match.getHomeTeam().getShortName() + "  "
                    + (match.isPlayed() ? match.getHomeGoals() + " - " + match.getAwayGoals() : "VS")
                    + "  " + match.getAwayTeam().getShortName(),
                    isControlledMatch(match) ? "calendar-detail-main" : "body-label");
            HBox matchup = new HBox(8, footballcareer.ui.TeamCrestView.create(match.getHomeTeam(), 30),
                    fixture, footballcareer.ui.TeamCrestView.create(match.getAwayTeam(), 30));
            matchup.setAlignment(Pos.CENTER_LEFT);
            HBox row = new HBox(14, competition, matchup);
            row.getStyleClass().addAll("calendar-fixture-card",
                    footballcareer.ui.CompetitionVisuals.calendarStyle(match.getCompetition()));
            row.setAlignment(Pos.CENTER_LEFT);
            if (match.isPlayed()) {
                Button report = button("VER INFORME", "secondary-button");
                report.setOnAction(event -> showMatchReport(stage, match));
                row.getChildren().add(report);
            } else if (isControlledMatch(match) && date.equals(career.getCurrentDate())) {
                Button play = button("IR AL PARTIDO", "primary-button");
                play.setOnAction(event -> showMatchPreview(stage, match));
                row.getChildren().add(play);
            }
            detail.getChildren().add(row);
        }
    }

    private boolean isControlledMatch(Match match) {
        long teamId = career.getControlledTeam().getId();
        return match.getHomeTeam().getId() == teamId || match.getAwayTeam().getId() == teamId;
    }

    private void showStandings(Stage stage) {
        activeSection = "standings";
        if (restoreRetainedScreen(stage, "standings")) return;
        VBox content = page("CLASIFICACIÓN", "Rendimiento, forma reciente y plazas continentales");
        ComboBox<Competition> selector = new ComboBox<>();
        selector.getItems().addAll(currentCompetitions());
        selector.setCellFactory(list -> competitionCell());
        selector.setButtonCell(competitionCell());
        selector.setPrefWidth(350);
        java.util.Map<Long, String> recentForm = new java.util.HashMap<>();
        ListView<LeagueStanding> standings = new ListView<>();
        standings.getStyleClass().add("standings-modern-list");
        standings.setCellFactory(view -> new footballcareer.ui.StandingsRowCell(
                selector::getValue, () -> standings.getItems().size(),
                career.getControlledTeam().getId(), recentForm));
        standings.setOnMouseClicked(event -> {
            LeagueStanding selected = standings.getSelectionModel().getSelectedItem();
            if (event.getClickCount() == 2 && selected != null)
                showTeamOverview(stage, selected.getTeam());
        });
        HBox legend = new HBox(14);
        Runnable refresh = () -> {
            standings.getItems().clear();
            recentForm.clear();
            Competition selected = selector.getValue();
            if (selected == null) return;
            selectedCompetitionId = selected.getId();
            legend.getChildren().setAll(footballcareer.ui.StandingsLegendView.items(selected));
            java.util.List<Match> competitionMatches = matches.findByCompetition(selected.getId());
            new CompetitionTeamRepository().findTeamsByCompetition(selected.getId())
                    .forEach(team -> recentForm.put(team.getId(),
                            teamRecentForm(team.getId(), competitionMatches)));
            standings.getItems().setAll(new LeagueStandingRepository()
                    .findByCompetition(selected.getId()));
        };
        selector.setOnAction(event -> refresh.run());
        if (!selector.getItems().isEmpty()) {
            Competition remembered = selector.getItems().stream()
                    .filter(item -> selectedCompetitionId != null
                            && item.getId() == selectedCompetitionId).findFirst()
                    .orElse(selector.getItems().getFirst());
            selector.setValue(remembered);
            refresh.run();
        }
        VBox competitionPicker = new VBox(7, label("COMPETICIÓN", "field-caption"), selector);
        HBox standingsHeader = new HBox(18, competitionPicker, legend);
        standingsHeader.setAlignment(Pos.BOTTOM_LEFT);
        VBox board = new VBox(12, standingsHeader, standings,
                label("Doble clic para abrir el club · Desempates: puntos, diferencia de goles y goles a favor.",
                        "muted-label"));
        board.getStyleClass().add("standings-board");
        VBox.setVgrow(standings, Priority.ALWAYS);
        VBox.setVgrow(board, Priority.ALWAYS);
        content.getChildren().add(board);
        retainedScreens.put("standings", career.getCurrentDate(), content);
        showCareerShell(stage, content);
    }

    private String teamRecentForm(long teamId, java.util.List<Match> competitionMatches) {
        java.util.List<Match> recent = competitionMatches.stream().filter(Match::isPlayed)
                .filter(match -> match.getHomeTeam().getId() == teamId
                        || match.getAwayTeam().getId() == teamId)
                .sorted(Comparator.comparing(Match::getDate).reversed()).limit(5).toList();
        if (recent.isEmpty()) return "—";
        return recent.reversed().stream().map(match -> {
            boolean home = match.getHomeTeam().getId() == teamId;
            int own = home ? match.getHomeGoals() : match.getAwayGoals();
            int rival = home ? match.getAwayGoals() : match.getHomeGoals();
            return own > rival ? "V" : own == rival ? "E" : "D";
        }).collect(java.util.stream.Collectors.joining("  "));
    }

    private void showTeamOverview(Stage stage, Team team) {
        activeSection = "standings";
        VBox content = page(team.getName(), team.getCountry() + "  •  " + team.getShortName());
        java.util.List<Player> squad = new PlayerRepository().findCurrentPlayersByTeam(team.getId());
        double average = squad.stream().mapToInt(Player::getOverall).average().orElse(0);
        ClubFinance finance = new ClubFinanceRepository().findByTeam(team.getId());
        FlowPane cards = new FlowPane(14, 14,
                statCard("REPUTACIÓN", String.valueOf(team.getReputation())),
                statCard("GRL PLANTILLA", String.format("%.1f", average)),
                statCard("JUGADORES", String.valueOf(squad.size())),
                statCard("PRESUPUESTO", finance == null ? "—"
                        : String.format("€%.1fM", finance.getTransferBudget() / 1_000_000)));
        VBox identity = panel("IDENTIDAD DEL CLUB");
        identity.getChildren().add(label("Estadio: " + team.getStadiumName() + "  •  Capacidad: "
                + team.getStadiumCapacity(), "body-label"));
        VBox keyPlayers = panel("JUGADORES DESTACADOS");
        squad.stream().sorted(Comparator.comparingInt(Player::getOverall).reversed()).limit(5)
                .forEach(player -> keyPlayers.getChildren().add(label(player.getPosition()
                        + "  •  " + player.getFullName() + "  •  GRL " + player.getOverall(),
                        "news-row")));
        Button back = button("VOLVER A CLASIFICACIÓN", "ghost-button");
        back.setOnAction(event -> showStandings(stage));
        content.getChildren().addAll(cards, identity, keyPlayers, back);
        showCareerShell(stage, content);
    }

    private void showResults(Stage stage) {
        activeSection = "results";
        if (restoreRetainedScreen(stage, "results")) return;
        VBox content = page("RESULTADOS DEL MUNDO",
                "Consulta cualquier fecha y filtra por competición");
        ListView<Match> resultTable = new ListView<>();
        resultTable.getStyleClass().add("results-modern-list");
        resultTable.setCellFactory(view -> new footballcareer.ui.MatchResultCell(
                career.getControlledTeam().getId()));
        resultTable.setPlaceholder(label("No hay resultados registrados con estos filtros.",
                "muted-label"));
        resultTable.setOnMouseClicked(event -> {
            Match selected = resultTable.getSelectionModel().getSelectedItem();
            if (event.getClickCount() == 2 && selected != null)
                showMatchReport(stage, selected);
        });
        DatePicker date = new DatePicker(selectedResultsDate == null
                ? career.getCurrentDate() : selectedResultsDate);
        date.setMaxWidth(180);
        ComboBox<String> competition = new ComboBox<>();
        competition.getItems().add("TODAS LAS COMPETICIONES");
        competition.setValue("TODAS LAS COMPETICIONES");
        Runnable refresh = () -> {
            if (date.getValue() == null) return;
            selectedResultsDate = date.getValue();
            java.util.List<Match> dayMatches = matches.findByDate(date.getValue()).stream()
                    .filter(Match::isPlayed).toList();
            String previousSelection = competition.getValue();
            java.util.List<String> names = dayMatches.stream()
                    .map(match -> match.getCompetition().getName()).distinct().sorted().toList();
            competition.getItems().setAll("TODAS LAS COMPETICIONES");
            competition.getItems().addAll(names);
            competition.setValue(previousSelection != null
                    && competition.getItems().contains(previousSelection)
                    ? previousSelection : "TODAS LAS COMPETICIONES");
            resultTable.getItems().setAll(dayMatches.stream()
                    .filter(match -> "TODAS LAS COMPETICIONES".equals(competition.getValue())
                            || match.getCompetition().getName().equals(competition.getValue()))
                    .toList());
        };
        date.setOnAction(event -> refresh.run());
        competition.setOnAction(event -> {
            if (date.getValue() == null) return;
            resultTable.getItems().setAll(matches.findByDate(date.getValue()).stream()
                    .filter(Match::isPlayed)
                    .filter(match -> "TODAS LAS COMPETICIONES".equals(competition.getValue())
                            || match.getCompetition().getName().equals(competition.getValue()))
                    .toList());
        });
        Button previous = button("← DÍA ANTERIOR", "ghost-button");
        previous.setOnAction(event -> { date.setValue(date.getValue().minusDays(1)); refresh.run(); });
        Button next = button("DÍA SIGUIENTE →", "ghost-button");
        next.setDisable(!date.getValue().isBefore(career.getCurrentDate()));
        next.setOnAction(event -> {
            date.setValue(date.getValue().plusDays(1));
            next.setDisable(!date.getValue().isBefore(career.getCurrentDate()));
            refresh.run();
        });
        refresh.run();
        VBox.setVgrow(resultTable, Priority.ALWAYS);
        content.getChildren().addAll(new FlowPane(12, 12, previous, date, next, competition),
                label("Doble clic sobre un partido para abrir su informe.", "muted-label"),
                resultTable);
        retainedScreens.put("results", career.getCurrentDate(), content);
        showCareerShell(stage, content);
    }

    private void showMatchReport(Stage stage, Match selectedMatch) {
        String origin = navigationController.reportReturnSection(activeSection);
        MatchReport report;
        try { new footballcareer.service.BackgroundMatchReportService().prepare(selectedMatch);
            report = new MatchReportService().build(selectedMatch.getId());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            navigateToSection(stage, origin);
            return;
        }
        Match match = report.getMatch();
        VBox content = page("INFORME DEL PARTIDO", match.getDate().toString());
        HBox score = new HBox(28,
                label(match.getHomeTeam().getName(), "score-team"),
                label(match.getHomeGoals() + "  —  " + match.getAwayGoals(), "score-number"),
                label(match.getAwayTeam().getName(), "score-team"));
        score.setAlignment(Pos.CENTER);
        score.getStyleClass().add("scoreboard");

        GridPane statistics = new GridPane();
        statistics.getStyleClass().add("statistics-grid");
        statistics.setHgap(28);
        statistics.setVgap(10);
        addStatRow(statistics, 0, "Posesión", report.getHomeStats().getPossession() + "%",
                report.getAwayStats().getPossession() + "%");
        addStatRow(statistics, 1, "Tiros", report.getHomeStats().getShots(),
                report.getAwayStats().getShots());
        addStatRow(statistics, 2, "A puerta", report.getHomeStats().getShotsOnTarget(),
                report.getAwayStats().getShotsOnTarget());
        addStatRow(statistics, 3, "Córners", report.getHomeStats().getCorners(),
                report.getAwayStats().getCorners());
        addStatRow(statistics, 4, "Faltas", report.getHomeStats().getFouls(),
                report.getAwayStats().getFouls());
        addStatRow(statistics, 5, "Amarillas", report.getHomeStats().getYellowCards(),
                report.getAwayStats().getYellowCards());
        addStatRow(statistics, 6, "Rojas", report.getHomeStats().getRedCards(),
                report.getAwayStats().getRedCards());
        addStatRow(statistics, 7, "Goles esperados (xG)",
                String.format("%.2f", report.getHomeStats().getExpectedGoals()),
                String.format("%.2f", report.getAwayStats().getExpectedGoals()));
        addStatRow(statistics, 8, "Pases", report.getHomeStats().getPasses(),
                report.getAwayStats().getPasses());
        addStatRow(statistics, 9, "Precisión de pase",
                report.getHomeStats().getPassAccuracy() + "%",
                report.getAwayStats().getPassAccuracy() + "%");
        addStatRow(statistics, 10, "Entradas", report.getHomeStats().getTackles(),
                report.getAwayStats().getTackles());

        VBox timeline = panel("CRONOLOGÍA");
        report.getEvents().forEach(event -> {
            String detail = event.getMinute() + "'  " + eventLabel(event.getType()) + "  •  "
                    + event.getPlayer().getFullName();
            if (event.getSecondaryPlayer() != null) {
                detail += "  (" + event.getSecondaryPlayer().getFullName() + ")";
            }
            timeline.getChildren().add(label(detail, "event-row"));
        });
        if (report.getEvents().isEmpty()) timeline.getChildren().add(
                label("Sin eventos destacados.", "body-label"));
        Player best = report.getPlayerOfTheMatch();
        VBox bestPlayer = panel("JUGADOR DEL PARTIDO");
        bestPlayer.getChildren().add(label(best.getFullName() + "  •  GRL "
                + best.getOverall(), "match-highlight"));
        boolean controlledMatch = match.getHomeTeam().getId() == career.getControlledTeam().getId()
                || match.getAwayTeam().getId() == career.getControlledTeam().getId();
        VBox impact = panel("IMPACTO EN TU CARRERA");
        if (controlledMatch) {
            impact.getChildren().addAll(label(matchOutcomeSummary(match), "match-highlight"),
                    label("Clasificación: " + leaguePositionSummary()
                            + "  •  " + squadStatusSummary(), "body-label"));
            java.util.Map<Long, PlayerState> states = new PlayerStateRepository().findAll();
            java.util.List<String> absences = new PlayerRepository()
                    .findCurrentPlayersByTeam(career.getControlledTeam().getId()).stream()
                    .filter(player -> {
                        PlayerState state = states.get(player.getId());
                        return state != null && !state.isAvailableOn(career.getCurrentDate());
                    }).map(player -> player.getFullName() + "  •  "
                            + availabilityLabel(states.get(player.getId()), career.getCurrentDate()))
                    .toList();
            if (absences.isEmpty()) impact.getChildren().add(
                    label("Sin bajas médicas o disciplinarias.", "success-feedback"));
            else absences.forEach(absence -> impact.getChildren().add(
                    label(absence, "warning-feedback")));
        } else {
            impact.getChildren().add(label(
                    "Este encuentro no afecta directamente a tu plantilla.", "muted-label"));
        }
        Button back = button("VOLVER A " + navigationController.sectionLabel(origin),
                "ghost-button");
        back.setOnAction(event -> navigateToSection(stage, origin));
        Button dashboard = button("IR AL CENTRO DE MANDO", "secondary-button");
        dashboard.setOnAction(event -> showDashboard(stage));
        content.getChildren().addAll(score, statistics, bestPlayer, impact, timeline,
                new FlowPane(12, 12, dashboard, back));
        showCareerShell(stage, new ScrollPane(content));
    }

    private void navigateToSection(Stage stage, String section) {
        switch (section) {
            case "results" -> showResults(stage);
            case "dashboard" -> showDashboard(stage);
            case "standings" -> showStandings(stage);
            default -> showCalendar(stage);
        }
    }

    private String matchOutcomeSummary(Match match) {
        boolean home = match.getHomeTeam().getId() == career.getControlledTeam().getId();
        int own = home ? match.getHomeGoals() : match.getAwayGoals();
        int rival = home ? match.getAwayGoals() : match.getHomeGoals();
        String opponent = home ? match.getAwayTeam().getName() : match.getHomeTeam().getName();
        String outcome = own > rival ? "Victoria" : own == rival ? "Empate" : "Derrota";
        return outcome + " ante " + opponent + "  •  " + own + "–" + rival;
    }

    private String eventLabel(footballcareer.model.enums.MatchEventType type) {
        return switch (type) {
            case GOAL -> "GOL";
            case YELLOW_CARD -> "TARJETA AMARILLA";
            case RED_CARD -> "TARJETA ROJA";
            case SUBSTITUTION -> "SUSTITUCIÓN";
        };
    }

    private void showMarket(Stage stage) {
        activeSection = "market";
        new TransferOfferService().expireOffers(career.getCurrentDate());
        boolean windowOpen = new TransferWindowService().isOpen(career.getCurrentDate());
        ClubFinance finance = new ClubFinanceRepository()
                .findByTeam(career.getControlledTeam().getId());
        String budget = finance == null ? "Sin datos financieros"
                : String.format("Presupuesto: €%.1fM  •  Margen salarial: €%.1fM",
                finance.getTransferBudget() / 1_000_000,
                finance.getAvailableWageBudget() / 1_000_000)
                + (windowOpen ? "  •  VENTANA ABIERTA" : "  •  VENTANA CERRADA");
        VBox content = page("MERCADO DE FICHAJES", budget);
        PlayerMarketRepository marketRepository = new PlayerMarketRepository();

        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("market-tabs");
        tabs.getTabs().addAll(new Tab("EXPLORAR"), new Tab("MIS VENTAS"),
                new Tab("ENVIADAS"), new Tab("RECIBIDAS"));
        tabs.getTabs().forEach(tab -> tab.setClosable(false));
        boolean[] loaded = new boolean[4];
        java.util.function.IntConsumer loadTab = index -> {
            if (loaded[index]) return;
            Node view = switch (index) {
                case 0 -> createBuyTab(stage, marketRepository);
                case 1 -> createSalesTab(marketRepository);
                case 2 -> createSentOffersTab(stage);
                default -> createIncomingOffersTab(stage);
            };
            tabs.getTabs().get(index).setContent(view); loaded[index] = true;
        };
        tabs.getSelectionModel().select(Math.min(selectedMarketTab, tabs.getTabs().size() - 1));
        loadTab.accept(tabs.getSelectionModel().getSelectedIndex());
        tabs.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            selectedMarketTab = newValue.intValue(); loadTab.accept(selectedMarketTab);
        });
        tabs.setMinHeight(680); VBox.setVgrow(tabs, Priority.ALWAYS);
        content.getChildren().add(tabs);
        ScrollPane marketScroll = new ScrollPane(content); marketScroll.setFitToWidth(true);
        marketScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        marketScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        showCareerShell(stage, marketScroll);
    }

    private Node createBuyTab(Stage stage, PlayerMarketRepository marketRepository) {
        VBox box = new VBox(12);
        new ClubTransferAiService().ensureMarketSupply(career.getControlledTeam().getId());
        java.util.List<Player> marketPlayers = marketRepository
                .findTransferListed(career.getControlledTeam().getId());
        java.util.List<Player> allPlayers = new PlayerRepository().findAll();
        java.util.Map<Long, Double> marketPrices = marketRepository.findAllAskingPrices();
        java.util.Map<Long, Team> marketTeams = new java.util.HashMap<>();
        CareerShortlistRepository shortlistRepository = new CareerShortlistRepository();
        java.util.Set<Long> shortlistIds = new java.util.HashSet<>(
                shortlistRepository.findPlayerIds(career.getId()));
        PlayerTeamRepository playerTeams = new PlayerTeamRepository();
        java.util.Map<Long, Long> currentTeamIds = playerTeams.findAllCurrentTeamIds();
        java.util.Map<Long, Team> teamsById = teams.findAll().stream().collect(
                java.util.stream.Collectors.toMap(Team::getId, team -> team));
        allPlayers.forEach(player -> {
            Long teamId = currentTeamIds.get(player.getId());
            if (teamId != null && teamsById.containsKey(teamId)) {
                marketTeams.put(player.getId(), teamsById.get(teamId));
            }
        });
        java.util.Map<Long, String> leagueByTeam = new CompetitionTeamRepository()
                .findLeagueNamesByTeam(career.getCurrentSeason().getId());
        MarketSearchController.Catalogue catalogueData =
                new MarketSearchController.Catalogue(allPlayers, marketPlayers, marketTeams,
                        leagueByTeam, marketPrices, shortlistIds,
                        career.getControlledTeam().getId(), career.getCurrentDate());
        ListView<Player> targets = marketPlayerList(marketPrices, marketTeams, shortlistIds);
        ToggleButton listedMode = new ToggleButton("EN EL MERCADO  //  " + marketPlayers.size());
        ToggleButton globalMode = new ToggleButton("SCOUTING GLOBAL");
        listedMode.getStyleClass().add("market-mode-button");
        globalMode.getStyleClass().add("market-mode-button");
        ToggleGroup marketMode = new ToggleGroup();
        listedMode.setToggleGroup(marketMode);
        globalMode.setToggleGroup(marketMode);
        (marketGlobalMode ? globalMode : listedMode).setSelected(true);
        TextField search = new TextField();
        search.setPromptText("Nombre del jugador...");
        search.setText(marketSearchText);
        ComboBox<String> league = new ComboBox<>();
        league.getItems().add("TODAS LAS LIGAS");
        league.getItems().addAll(leagueByTeam.values().stream().distinct().sorted().toList());
        league.setValue(league.getItems().contains(marketLeagueFilter)
                ? marketLeagueFilter : "TODAS LAS LIGAS");
        ComboBox<String> club = new ComboBox<>();
        club.getItems().add("TODOS LOS CLUBES");
        club.getItems().addAll(teamsById.values().stream()
                .filter(team -> team.getId() != career.getControlledTeam().getId())
                .map(Team::getName).sorted().toList());
        club.setValue(club.getItems().contains(marketClubFilter)
                ? marketClubFilter : "TODOS LOS CLUBES");
        ComboBox<String> position = new ComboBox<>();
        position.getItems().addAll("TODAS", "GK", "DEFENSA", "MEDIO", "ATAQUE");
        position.setValue(marketPositionFilter);
        TextField maximumPrice = new TextField();
        maximumPrice.setPromptText("Precio máximo (€M)");
        maximumPrice.setText(marketMaximumPrice);
        TextField minimumOverall = new TextField();
        minimumOverall.setPromptText("GRL mínimo");
        minimumOverall.setText(marketMinimumOverall);
        TextField maximumAge = new TextField();
        maximumAge.setPromptText("Edad máxima");
        maximumAge.setText(marketMaximumAge);
        TextField maximumSalary = new TextField();
        maximumSalary.setPromptText("Salario máximo (€M)");
        maximumSalary.setText(marketMaximumSalary);
        ComboBox<String> sort = new ComboBox<>();
        sort.getItems().addAll("PRECIO ↑", "PRECIO ↓", "GRL ↓", "EDAD ↑");
        sort.setValue(marketSort);
        CheckBox shortlistOnly = new CheckBox("Solo seguimiento");
        shortlistOnly.setSelected(marketShortlistOnly);
        Label resultCount = label("", "market-result-count");
        Runnable refreshTargets = () -> {
            marketGlobalMode = globalMode.isSelected();
            marketSearchText = search.getText() == null ? "" : search.getText();
            marketLeagueFilter = league.getValue();
            marketClubFilter = club.getValue();
            marketPositionFilter = position.getValue();
            marketMaximumPrice = maximumPrice.getText() == null ? "" : maximumPrice.getText();
            marketMinimumOverall = minimumOverall.getText() == null ? "" : minimumOverall.getText();
            marketMaximumAge = maximumAge.getText() == null ? "" : maximumAge.getText();
            marketMaximumSalary = maximumSalary.getText() == null ? "" : maximumSalary.getText();
            marketSort = sort.getValue();
            marketShortlistOnly = shortlistOnly.isSelected();
            MarketSearchController.Query query = new MarketSearchController.Query(
                    globalMode.isSelected(), search.getText(), league.getValue(), club.getValue(),
                    position.getValue(), minimumOverall.getText(), maximumAge.getText(),
                    maximumPrice.getText(), maximumSalary.getText(), sort.getValue(),
                    shortlistOnly.isSelected());
            java.util.List<Player> filtered = marketSearchController.search(catalogueData, query);
            targets.getItems().setAll(filtered);
            Long remembered = navigationState.selection("market");
            if (remembered != null) filtered.stream()
                    .filter(player -> player.getId() == remembered).findFirst()
                    .ifPresent(player -> targets.getSelectionModel().select(player));
            resultCount.setText(filtered.size() + " JUGADORES ENCONTRADOS");
        };
        search.textProperty().addListener((observable, oldValue, newValue) -> refreshTargets.run());
        marketMode.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) listedMode.setSelected(true);
            refreshTargets.run();
        });
        league.setOnAction(event -> refreshTargets.run());
        club.setOnAction(event -> refreshTargets.run());
        maximumPrice.textProperty().addListener((observable, oldValue, newValue) -> refreshTargets.run());
        minimumOverall.textProperty().addListener((observable, oldValue, newValue) -> refreshTargets.run());
        maximumAge.textProperty().addListener((observable, oldValue, newValue) -> refreshTargets.run());
        maximumSalary.textProperty().addListener((observable, oldValue, newValue) -> refreshTargets.run());
        position.setOnAction(event -> refreshTargets.run());
        sort.setOnAction(event -> refreshTargets.run());
        shortlistOnly.setOnAction(event -> refreshTargets.run());
        Button applySearch = button("BUSCAR / APLICAR", "primary-button");
        applySearch.setOnAction(event -> refreshTargets.run());
        Button clearSearch = button("LIMPIAR", "ghost-button");
        clearSearch.setOnAction(event -> {
            search.clear();
            league.setValue("TODAS LAS LIGAS");
            club.setValue("TODOS LOS CLUBES");
            position.setValue("TODAS");
            minimumOverall.clear();
            maximumAge.clear();
            maximumPrice.clear();
            maximumSalary.clear();
            shortlistOnly.setSelected(false);
            refreshTargets.run();
        });
        refreshTargets.run();
        Button details = button("VER FICHA", "ghost-button");
        details.setDisable(true);
        Button shortlist = button("AÑADIR A SEGUIMIENTO", "secondary-button");
        shortlist.setDisable(true);
        Label selectedName = label("SELECCIONA UN JUGADOR", "market-player-name");
        Label selectedClub = label("Explora el catálogo para consultar su situación.", "muted-label");
        Label selectedOverall = label("—", "market-player-overall");
        Label selectedData = label("Sin información seleccionada", "market-player-data");
        shortlist.setOnAction(event -> {
            Player selected = targets.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            if (shortlistIds.remove(selected.getId())) {
                shortlistRepository.remove(career.getId(), selected.getId());
            } else {
                shortlistIds.add(selected.getId());
                shortlistRepository.add(career.getId(), selected.getId(), career.getCurrentDate());
            }
            targets.refresh();
            refreshTargets.run();
        });
        details.setOnAction(event -> {
            Player selected = targets.getSelectionModel().getSelectedItem();
            if (selected != null) showPlayer(stage, selected, "market");
        });
        targets.setOnMouseClicked(event -> {
            Player selected = targets.getSelectionModel().getSelectedItem();
            if (event.getClickCount() == 2 && selected != null) {
                showTransferNegotiation(stage, selected);
            }
        });
        targets.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> {
                    if (selected != null) navigationState.rememberSelection("market", selected.getId());
                    shortlist.setDisable(selected == null);
                    details.setDisable(selected == null);
                    shortlist.setText(selected != null && shortlistIds.contains(selected.getId())
                            ? "QUITAR DE SEGUIMIENTO" : "AÑADIR A SEGUIMIENTO");
                    if (selected == null) return;
                    Team seller = marketTeams.get(selected.getId());
                    Double price = marketPrices.get(selected.getId());
                    selectedName.setText(selected.getFullName());
                    selectedClub.setText(seller == null ? "Club no identificado" : seller.getName());
                    selectedOverall.setText(String.valueOf(selected.getOverall()));
                    selectedData.setText(selected.getPosition() + "  ·  "
                            + selected.getAge(career.getCurrentDate()) + " años\n"
                            + String.format("Valor estimado  €%.1fM\n", selected.getMarketValue() / 1_000_000)
                            + (price == null ? "No está en venta: requerirá una oferta superior"
                            : String.format("Precio solicitado  €%.1fM", price / 1_000_000)));
                });
        VBox.setVgrow(targets, Priority.ALWAYS);
        Button negotiate = button("NEGOCIAR CON EL SELECCIONADO", "primary-button");
        negotiate.setDisable(true);
        negotiate.setOnAction(event -> {
            Player selected = targets.getSelectionModel().getSelectedItem();
            if (selected != null) showTransferNegotiation(stage, selected);
        });
        targets.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> negotiate.setDisable(selected == null));
        GridPane advancedGrid = new GridPane(); advancedGrid.setHgap(8); advancedGrid.setVgap(8);
        advancedGrid.add(label("GRL MÍN.", "field-caption"), 0, 0);
        advancedGrid.add(minimumOverall, 1, 0);
        advancedGrid.add(label("EDAD MÁX.", "field-caption"), 0, 1);
        advancedGrid.add(maximumAge, 1, 1);
        advancedGrid.add(label("PRECIO MÁX.", "field-caption"), 0, 2);
        advancedGrid.add(maximumPrice, 1, 2);
        advancedGrid.add(label("SALARIO MÁX.", "field-caption"), 0, 3);
        advancedGrid.add(maximumSalary, 1, 3);
        advancedGrid.add(sort, 0, 4, 2, 1);
        advancedGrid.add(shortlistOnly, 0, 5, 2, 1);
        TitledPane advanced = new TitledPane("MÁS FILTROS", advancedGrid);
        advanced.setExpanded(false);
        VBox filters = new VBox(10, label("DESCUBRIR", "market-column-title"),
                listedMode, globalMode, search,
                label("LIGA", "field-caption"), league,
                label("CLUB", "field-caption"), club,
                label("POSICIÓN", "field-caption"), position,
                advanced, new HBox(8, applySearch, clearSearch));
        filters.getStyleClass().add("market-filter-rail"); filters.setPrefWidth(245);
        listedMode.setMaxWidth(Double.MAX_VALUE); globalMode.setMaxWidth(Double.MAX_VALUE);

        VBox catalogue = new VBox(10, new HBox(10,
                label("CATÁLOGO", "market-column-title"), resultCount), targets,
                label("Doble clic para negociar.", "muted-label"));
        catalogue.getStyleClass().add("market-catalogue");
        HBox.setHgrow(catalogue, Priority.ALWAYS);
        VBox.setVgrow(targets, Priority.ALWAYS);

        VBox selectedCard = new VBox(12, label("INFORME RÁPIDO", "market-column-title"),
                selectedOverall, selectedName, selectedClub, selectedData,
                negotiate, details, shortlist);
        selectedCard.getStyleClass().add("market-selection-card"); selectedCard.setPrefWidth(285);
        negotiate.setMaxWidth(Double.MAX_VALUE); details.setMaxWidth(Double.MAX_VALUE);
        shortlist.setMaxWidth(Double.MAX_VALUE);

        HBox desk = new HBox(16, filters, catalogue, selectedCard);
        desk.getStyleClass().add("market-scouting-desk");
        VBox.setVgrow(desk, Priority.ALWAYS);
        box.getStyleClass().add("market-buy-root"); box.getChildren().add(desk);
        return box;
    }

    private void showTransferNegotiation(Stage stage, Player player) {
        TransferOfferService service = new TransferOfferService();
        TransferOfferService.NegotiationQuote quote = service.quote(player.getId());
        Long sellerId = new PlayerTeamRepository().findCurrentTeamId(player.getId());
        Team seller = sellerId == null ? null : teams.findById(sellerId);
        ClubFinance finances = new ClubFinanceRepository()
                .findByTeam(career.getControlledTeam().getId());
        String assistance = new CareerPreferencesRepository().find(career.getId())
                .assistanceLevel();

        Button send = button("ENVIAR OFERTA", "primary-button");
        Button loan = button("SOLICITAR CESIÓN", "secondary-button");
        Button cancel = button("CANCELAR", "ghost-button");
        TextField amount = new TextField();
        amount.setPromptText("Cantidad en millones de euros");
        amount.setText(String.format(java.util.Locale.ROOT, "%.1f",
                ("EXPERT".equals(assistance) ? quote.marketValue() : quote.requiredAmount())
                        / 1_000_000));
        ComboBox<Integer> upfront = new ComboBox<>();
        upfront.getItems().addAll(50, 75, 100); upfront.setValue(100);
        upfront.setPromptText("Pago inicial %");
        TextField appearanceBonus = new TextField("0");
        appearanceBonus.setPromptText("Prima tras 10 partidos (€M)");
        ComboBox<Integer> loanMonths = new ComboBox<>();
        loanMonths.getItems().addAll(6, 12);
        loanMonths.setValue(6);
        loanMonths.setPromptText("Duración de cesión");
        Label loanReference = label("", "comparison-label");
        Runnable refreshLoanQuote = () -> {
            LoanService.LoanQuote loanQuote = new LoanService()
                    .quote(player.getId(), loanMonths.getValue());
            loanReference.setText(String.format(
                    "CESIÓN %d MESES  •  Tarifa solicitada €%.2fM",
                    loanQuote.months(), loanQuote.requiredFee() / 1_000_000));
            loan.setDisable(!new TransferWindowService().isOpen(career.getCurrentDate())
                    || finances == null
                    || finances.getTransferBudget() < loanQuote.requiredFee());
        };
        loanMonths.setOnAction(event -> refreshLoanQuote.run());
        refreshLoanQuote.run();
        Label offerStatus = label("", "warning-feedback");
        Label financialProjection = label("", "comparison-label");
        Label negotiationReference = label("GUIDED".equals(assistance)
                ? String.format("Referencia negociadora del club: €%.1fM",
                quote.requiredAmount() / 1_000_000)
                : "STANDARD".equals(assistance)
                ? "El club espera una oferta coherente con valor, nivel y disponibilidad."
                : "Sin referencia negociadora: evalúa el riesgo con tus propios criterios.",
                "warning-feedback");
        Label squadComparison = label("EXPERT".equals(assistance)
                ? "Asistencia experta: comparación automática desactivada."
                : marketComparison(player), "muted-label");
        VBox content = new VBox(14,
                label("OFERTA AL CLUB", "form-title"),
                label(player.getFullName() + "  •  " + player.getPosition()
                        + "  •  GRL " + player.getOverall(), "match-highlight"),
                label(seller == null ? "Club desconocido" : seller.getName(), "objective-title"),
                label(String.format("Valor de mercado €%.1fM  •  %s",
                        quote.marketValue() / 1_000_000,
                        quote.transferListed() ? "JUGADOR EN VENTA" : "NO ESTÁ EN VENTA"),
                        "comparison-label"),
                label(quote.stance() + "  •  " + quote.explanation(), "warning-feedback"),
                negotiationReference,
                label(finances == null ? "Presupuesto no disponible"
                        : String.format("Tu presupuesto: €%.1fM",
                        finances.getTransferBudget() / 1_000_000), "body-label"),
                squadComparison,
                label("TU OFERTA (€M)", "objective-title"), amount,
                label("ESTRUCTURA DEL PAGO", "objective-title"),
                new FlowPane(10, 10, upfront, appearanceBonus),
                label("El contrato con el jugador se negociará únicamente si el club acepta.", "muted-label"),
                financialProjection, offerStatus,
                label("ALTERNATIVA: CESIÓN", "objective-title"),
                new FlowPane(10, 10, loanMonths, loan), loanReference,
                new FlowPane(10, 10, send, cancel));
        content.setPrefWidth(520);
        content.getStyleClass().add("in-app-dialog");
        Node sendButton = send;
        boolean windowOpen = new TransferWindowService().isOpen(career.getCurrentDate());
        Runnable validateOffer = () -> {
            try {
                double value = Double.parseDouble(amount.getText().replace(',', '.'))
                        * 1_000_000;
                double variable = Double.parseDouble(appearanceBonus.getText().replace(',', '.'))
                        * 1_000_000;
                boolean affordable = finances != null
                        && value + variable <= finances.getTransferBudget();
                if (finances != null) financialProjection.setText(String.format(
                        "OFERTA €%.1fM  •  PAGO INICIAL %d%%  •  VARIABLE €%.1fM",
                        value / 1_000_000, upfront.getValue(), variable / 1_000_000));
                sendButton.setDisable(!windowOpen || value <= 0 || variable < 0 || !affordable);
                if (!windowOpen) offerStatus.setText("La ventana de fichajes está cerrada.");
                else if (value <= 0) offerStatus.setText("La oferta debe ser mayor que cero.");
                else if (variable < 0) offerStatus.setText("La variable no puede ser negativa.");
                else if (!affordable) offerStatus.setText("La oferta supera tu presupuesto disponible.");
                else if ("EXPERT".equals(assistance))
                    offerStatus.setText("Oferta válida. No se mostrarán pistas de aceptación.");
                else if (value < quote.requiredAmount() * 0.80)
                    offerStatus.setText("Oferta muy baja: el rechazo es prácticamente seguro.");
                else if (value < quote.requiredAmount())
                    offerStatus.setText("Zona negociable: el club podría enviar una contraoferta.");
                else offerStatus.setText("Oferta fuerte: cumple la referencia del vendedor.");
            } catch (NumberFormatException exception) {
                sendButton.setDisable(true);
                offerStatus.setText("Introduce una cantidad numérica válida.");
            }
        };
        amount.textProperty().addListener((observable, oldValue, newValue) -> validateOffer.run());
        appearanceBonus.textProperty().addListener(
                (observable, oldValue, newValue) -> validateOffer.run());
        upfront.setOnAction(event -> validateOffer.run());
        validateOffer.run();
        Runnable close = showOverlay(content);
        cancel.setOnAction(event -> close.run());
        send.setOnAction(event -> {
            double value;
            double variable;
            try {
                value = Double.parseDouble(amount.getText().replace(',', '.')) * 1_000_000;
                variable = Double.parseDouble(appearanceBonus.getText().replace(',', '.')) * 1_000_000;
            } catch (NumberFormatException exception) {
                value = -1;
                variable = -1;
            }
            close.run();
            processTransferNegotiation(stage, player, value, upfront.getValue(), variable);
        });
        loan.setOnAction(event -> {
            LoanService.LoanQuote loanQuote = new LoanService()
                    .quote(player.getId(), loanMonths.getValue());
            close.run();
            processLoanNegotiation(stage, player, loanQuote.requiredFee(),
                    loanMonths.getValue());
        });
    }

    private void processLoanNegotiation(Stage stage, Player player, double fee, int months) {
        try {
            new LoanService().requestLoan(player.getId(), career.getControlledTeam().getId(),
                    fee, months, career.getCurrentDate());
            showMessage("CESIÓN COMPLETADA", String.format(
                    "%s se incorpora durante %d meses por €%.2fM.",
                    player.getFullName(), months, fee / 1_000_000),
                    () -> showMarket(stage));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage("NO SE PUDO COMPLETAR LA CESIÓN", exception.getMessage());
        }
    }

    private void processTransferNegotiation(Stage stage, Player player, double amount,
            int upfrontPercent, double appearanceBonus) {
        if (amount <= 0) {
            showMessage("OFERTA NO VÁLIDA", "Revisa la cantidad ofrecida al club.");
            return;
        }
        try {
            TransferOfferService service = new TransferOfferService();
            TransferOffer offer = service.evaluate(service.makeOffer(player.getId(),
                    career.getControlledTeam().getId(), amount, career.getCurrentDate(),
                    upfrontPercent, appearanceBonus).getId());
            if (offer.getStatus() == footballcareer.model.enums.TransferOfferStatus.ACCEPTED) {
                showPlayerContractNegotiation(stage, offer, player);
            } else if (offer.getCounterAmount() != null) {
                showNegotiationRound(stage, service, offer, player);
            } else {
                showMessage("OFERTA RECHAZADA",
                        "El club considera que la propuesta está demasiado lejos de su valoración.");
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage("NO SE PUDO ENVIAR LA OFERTA", exception.getMessage());
        }
    }

    private void showNegotiationRound(Stage stage, TransferOfferService service,
            TransferOffer offer, Player player) {
        double sellerRequest = offer.getCounterAmount();
        double midpoint = (offer.getAmount() + sellerRequest) / 2;
        TextField revised = new TextField(String.format(java.util.Locale.ROOT, "%.1f",
                midpoint / 1_000_000));
        revised.setPromptText("Nueva propuesta (€M)");
        Label feedback = label(String.format(
                "Tu oferta: €%.1fM  •  El club pide: €%.1fM",
                offer.getAmount() / 1_000_000, sellerRequest / 1_000_000),
                "warning-feedback");
        Button accept = button("ACEPTAR €" + String.format("%.1fM",
                sellerRequest / 1_000_000), "primary-button");
        Button negotiate = button("ENVIAR NUEVA PROPUESTA", "secondary-button");
        Button withdraw = button("RETIRARSE", "ghost-button");
        VBox card = new VBox(14, label("RONDA DE NEGOCIACIÓN", "form-title"),
                label(player.getFullName(), "match-highlight"), feedback, revised,
                new FlowPane(10, 10, accept, negotiate, withdraw));
        card.setPrefWidth(520); card.getStyleClass().add("in-app-dialog");
        Runnable close = showOverlay(card);
        accept.setOnAction(event -> {
            close.run();
            try {
                TransferOffer accepted = service.acceptCounterOffer(offer.getId());
                showPlayerContractNegotiation(stage, accepted, player);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                showMessage("NO SE PUDO CERRAR EL ACUERDO", exception.getMessage());
            }
        });
        negotiate.setOnAction(event -> {
            try {
                double amount = Double.parseDouble(revised.getText().replace(',', '.'))
                        * 1_000_000;
                TransferOffer response = service.submitBuyerCounter(offer.getId(), amount);
                close.run();
                if (response.getStatus()
                        == footballcareer.model.enums.TransferOfferStatus.ACCEPTED) {
                    showPlayerContractNegotiation(stage, response, player);
                } else if (response.getCounterAmount() != null) {
                    showNegotiationRound(stage, service, response, player);
                } else {
                    showMessage("NEGOCIACIÓN FINALIZADA", "El club ha rechazado la propuesta.");
                }
            } catch (NumberFormatException exception) {
                feedback.setText("Introduce una cantidad numérica válida.");
            } catch (IllegalArgumentException | IllegalStateException exception) {
                feedback.setText(exception.getMessage());
            }
        });
        withdraw.setOnAction(event -> {
            service.cancelOffer(offer.getId(), career.getControlledTeam().getId());
            close.run();
            showMessage("NEGOCIACIÓN CANCELADA", "Has retirado la oferta por "
                    + player.getFullName() + ".");
        });
    }

    private void showPlayerContractNegotiation(Stage stage, TransferOffer offer, Player player) {
        Runnable[] close = {null};
        VBox dialog = new footballcareer.ui.TransferContractDialog().build(player,
                offer.getAmount(), terms -> {
                    try {
                        new PlayerAgentService().requireAgreement(player, terms.salary(),
                                terms.signingBonus(), terms.years(), terms.releaseClause(), terms.role());
                        executeTransfer(offer, player, terms.salary(), terms.years(),
                                terms.signingBonus(), terms.releaseClause(), terms.role());
                        close[0].run(); showMessage("FICHAJE COMPLETADO", player.getFullName()
                                + " ha aceptado el contrato y se incorpora al club.", () -> showMarket(stage));
                    } catch (IllegalArgumentException | IllegalStateException exception) {
                        showMessage("EL JUGADOR NO ACEPTA", exception.getMessage());
                    }
                }, () -> { close[0].run(); showMarket(stage); });
        close[0] = showOverlay(dialog);
    }

    private void showMessage(String title, String detail) {
        showMessage(title, detail, () -> {});
    }

    private void showMessage(String title, String detail, Runnable afterClose) {
        Button closeButton = button("CONTINUAR", "primary-button");
        VBox card = new VBox(16, label(title, "form-title"),
                label(detail, "body-label"), closeButton);
        card.setPrefWidth(460);
        card.getStyleClass().add("in-app-dialog");
        Runnable close = showOverlay(card);
        closeButton.setOnAction(event -> {
            close.run();
            afterClose.run();
        });
    }

    private void showDecision(String title, String detail, Runnable onAccept) {
        Button accept = button("ACEPTAR", "primary-button");
        Button reject = button("RECHAZAR", "ghost-button");
        VBox card = new VBox(16, label(title, "form-title"),
                label(detail, "body-label"), new FlowPane(10, 10, accept, reject));
        card.setPrefWidth(460);
        card.getStyleClass().add("in-app-dialog");
        Runnable close = showOverlay(card);
        reject.setOnAction(event -> close.run());
        accept.setOnAction(event -> {
            close.run();
            onAccept.run();
        });
    }

    private Runnable showOverlay(Node content) {
        return responsiveContainer.overlay(appScene, content);
    }

    private String marketComparison(Player target) {
        TransferOfferService.NegotiationQuote quote = new TransferOfferService()
                .quote(target.getId());
        Player current = new PlayerRepository()
                .findCurrentPlayersByTeam(career.getControlledTeam().getId()).stream()
                .filter(player -> player.getPosition() == target.getPosition())
                .max(Comparator.comparingInt(Player::getOverall)).orElse(null);
        String posture = quote.transferListed()
                ? String.format("EN VENTA  •  Precio €%.1fM", quote.requiredAmount() / 1_000_000)
                : String.format("NO ESTÁ EN VENTA  •  Referencia del club €%.1fM",
                quote.requiredAmount() / 1_000_000);
        posture += "  •  " + quote.stance();
        if (current == null) return posture + "\nNo tienes otro " + target.getPosition()
                + " en plantilla. Sería una incorporación prioritaria.";
        int difference = target.getOverall() - current.getOverall();
        return posture + "\n" + target.getFullName() + "  GRL " + target.getOverall()
                + "  •  Mejor actual: "
                + current.getFullName() + "  GRL " + current.getOverall() + "  •  Diferencia "
                + (difference >= 0 ? "+" : "") + difference;
    }

    private boolean matchesPositionGroup(Player player, String group) {
        if (group == null || "TODAS".equals(group)) return true;
        String position = player.getPosition().name();
        return switch (group) {
            case "GK" -> "GK".equals(position);
            case "DEFENSA" -> java.util.Set.of("CB", "LB", "RB").contains(position);
            case "MEDIO" -> java.util.Set.of("CDM", "CM", "CAM").contains(position);
            case "ATAQUE" -> java.util.Set.of("LW", "RW", "ST").contains(position);
            default -> true;
        };
    }

    private Node createSalesTab(PlayerMarketRepository marketRepository) {
        VBox box = new VBox(12);
        box.getStyleClass().add("market-buy-root");
        java.util.Map<Long, Double> askingPrices = new java.util.HashMap<>(
                marketRepository.findAllAskingPrices());
        ListView<Player> ownPlayers = ownMarketPlayerList(askingPrices);
        java.util.List<Player> squad = new PlayerRepository()
                .findCurrentPlayersByTeam(career.getControlledTeam().getId());
        TextField search = new TextField();
        search.setPromptText("Buscar en mi plantilla...");
        ComboBox<String> position = new ComboBox<>();
        position.getItems().addAll("TODAS", "GK", "DEFENSA", "MEDIO", "ATAQUE");
        position.setValue("TODAS");
        ComboBox<String> sort = new ComboBox<>();
        sort.getItems().addAll("POSICIÓN", "VALOR ↓", "GRL ↓", "NOMBRE");
        sort.setValue("POSICIÓN");
        CheckBox listedOnly = new CheckBox("Solo en venta");
        Runnable refreshPlayers = () -> {
            String query = search.getText() == null ? ""
                    : search.getText().trim().toLowerCase();
            Comparator<Player> comparator = switch (sort.getValue()) {
                case "VALOR ↓" -> Comparator.comparingDouble(Player::getMarketValue).reversed();
                case "GRL ↓" -> Comparator.comparingInt(Player::getOverall).reversed();
                case "NOMBRE" -> Comparator.comparing(Player::getFullName);
                default -> Comparator.comparingInt(player -> PlayerOrdering.position(player.getPosition()));
            };
            ownPlayers.getItems().setAll(squad.stream()
                    .filter(player -> query.isEmpty()
                            || player.getFullName().toLowerCase().contains(query))
                    .filter(player -> matchesPositionGroup(player, position.getValue()))
                    .filter(player -> !listedOnly.isSelected()
                            || askingPrices.containsKey(player.getId()))
                    .sorted(comparator.thenComparing(Player::getFullName)).toList());
        };
        search.textProperty().addListener((observable, oldValue, newValue) -> refreshPlayers.run());
        position.setOnAction(event -> refreshPlayers.run());
        sort.setOnAction(event -> refreshPlayers.run());
        listedOnly.setOnAction(event -> refreshPlayers.run());
        refreshPlayers.run();
        TextField askingPrice = new TextField();
        askingPrice.setPromptText("Precio solicitado en millones");
        Label selectedSummary = label("Selecciona un jugador de tu plantilla.",
                "comparison-label");
        Label feedback = label("Selecciona un jugador para gestionar su estado.", "muted-label");
        ownPlayers.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> {
                    if (selected == null) return;
                    Double listedPrice = askingPrices.get(selected.getId());
                    double suggested = listedPrice == null
                            ? selected.getMarketValue() : listedPrice;
                    selectedSummary.setText(selected.getFullName() + "  •  "
                            + selected.getPosition() + "  •  GRL " + selected.getOverall()
                            + String.format("  •  Valor €%.1fM", selected.getMarketValue() / 1_000_000)
                            + (listedPrice == null ? "  •  NO LISTADO"
                            : String.format("  •  En venta €%.1fM", listedPrice / 1_000_000)));
                    askingPrice.setText(String.format(java.util.Locale.ROOT, "%.1f",
                            suggested / 1_000_000));
                    feedback.setText(listedPrice == null
                            ? "Precio sugerido según su valor de mercado."
                            : "Puedes modificar el precio solicitado actual.");
                });
        Button listPlayer = button("PONER EN VENTA", "secondary-button");
        listPlayer.setOnAction(event -> {
            Player selected = ownPlayers.getSelectionModel().getSelectedItem();
            try {
                if (selected == null) throw new IllegalArgumentException("Selecciona un jugador.");
                double price = Double.parseDouble(askingPrice.getText().replace(',', '.'))
                        * 1_000_000;
                marketRepository.listForTransfer(selected.getId(), price);
                askingPrices.put(selected.getId(), price);
                feedback.setText(selected.getFullName() + " está en venta.");
                ownPlayers.refresh();
                refreshPlayers.run();
                animateFeedback(feedback, true);
            } catch (NumberFormatException exception) {
                feedback.setText("Introduce un precio válido.");
                animateFeedback(feedback, false);
            } catch (IllegalArgumentException exception) {
                feedback.setText(exception.getMessage());
                animateFeedback(feedback, false);
            }
        });
        Button removePlayer = button("RETIRAR DEL MERCADO", "ghost-button");
        removePlayer.setOnAction(event -> {
            Player selected = ownPlayers.getSelectionModel().getSelectedItem();
            if (selected != null) {
                marketRepository.removeFromTransferList(selected.getId());
                askingPrices.remove(selected.getId());
                feedback.setText(selected.getFullName() + " ya no está en venta.");
                ownPlayers.refresh();
                refreshPlayers.run();
                animateFeedback(feedback, true);
            }
        });
        VBox.setVgrow(ownPlayers, Priority.ALWAYS);
        VBox roster = new VBox(12, label("MI PLANTILLA", "market-column-title"),
                new HBox(10, search, position, sort, listedOnly), ownPlayers);
        roster.getStyleClass().add("market-catalogue");
        HBox.setHgrow(roster, Priority.ALWAYS);
        VBox saleDesk = new VBox(14, label("DECISIÓN DE VENTA", "market-column-title"));
        saleDesk.getStyleClass().add("market-selection-card");
        saleDesk.setPrefWidth(350);
        saleDesk.getChildren().addAll(selectedSummary,
                label("PRECIO SOLICITADO (€M)", "field-caption"), askingPrice,
                listPlayer, removePlayer, feedback,
                label("Ponerlo en venta permite que la IA envíe ofertas durante "
                        + "las jornadas de mercado.", "muted-label"));
        listPlayer.setMaxWidth(Double.MAX_VALUE);
        removePlayer.setMaxWidth(Double.MAX_VALUE);
        HBox workspace = new HBox(16, roster, saleDesk);
        workspace.getStyleClass().add("market-scouting-desk");
        VBox.setVgrow(workspace, Priority.ALWAYS);
        box.getChildren().add(workspace);
        return box;
    }

    private Node createIncomingOffersTab(Stage stage) {
        VBox box = new VBox(12);
        box.getStyleClass().addAll("market-buy-root", "offer-workspace");
        Label feedback = label("Las ofertas de otros clubes aparecerán aquí.", "muted-label");
        java.util.Map<Long, Player> playersById = new PlayerRepository().findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Player::getId, player -> player));
        java.util.Map<Long, Team> teamsById = teams.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Team::getId, team -> team));
        java.util.Map<Long, Double> askingPrices = new PlayerMarketRepository()
                .findAllAskingPrices();
        ListView<TransferOffer> incoming = incomingOfferList(
                playersById, teamsById, askingPrices);
        incoming.getItems().addAll(new TransferOfferRepository()
                .findPendingBySellingTeam(career.getControlledTeam().getId()));
        Label comparison = label("Selecciona una oferta para valorar la propuesta.",
                "comparison-label");
        TextField counterAmount = new TextField();
        counterAmount.setPromptText("Contraoferta en millones");
        incoming.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> {
                    if (selected == null) return;
                    Player player = playersById.get(selected.getPlayer().getId());
                    Double asking = askingPrices.get(player.getId());
                    double reference = asking == null ? player.getMarketValue() : asking;
                    double difference = selected.getAmount() - reference;
                    comparison.setText(String.format("Oferta €%.1fM  •  %s €%.1fM  •  Diferencia %+.1fM",
                            selected.getAmount() / 1_000_000,
                            asking == null ? "Valor" : "Precio solicitado",
                            reference / 1_000_000, difference / 1_000_000));
                    counterAmount.setText(String.format(java.util.Locale.ROOT, "%.1f",
                            Math.max(reference, selected.getAmount() * 1.05) / 1_000_000));
                });
        Button accept = button("ACEPTAR OFERTA", "primary-button");
        accept.setOnAction(event -> respondToIncomingOffer(incoming,
                incoming.getSelectionModel().getSelectedItem(), true, feedback));
        Button reject = button("RECHAZAR", "ghost-button");
        reject.setOnAction(event -> respondToIncomingOffer(incoming,
                incoming.getSelectionModel().getSelectedItem(), false, feedback));
        Button counter = button("ENVIAR CONTRAOFERTA", "secondary-button");
        counter.setOnAction(event -> respondWithIncomingCounter(incoming,
                incoming.getSelectionModel().getSelectedItem(), counterAmount, feedback));
        VBox.setVgrow(incoming, Priority.ALWAYS);
        VBox offerActions = new VBox(12, label("RESPUESTA", "market-column-title"),
                comparison, label("CONTRAOFERTA (€M)", "field-caption"), counterAmount,
                accept, counter, reject, feedback);
        offerActions.getStyleClass().add("market-selection-card");
        offerActions.setPrefWidth(360);
        accept.setMaxWidth(Double.MAX_VALUE); counter.setMaxWidth(Double.MAX_VALUE);
        reject.setMaxWidth(Double.MAX_VALUE);
        VBox offerList = new VBox(10, label("OFERTAS ACTIVAS", "market-column-title"), incoming);
        offerList.getStyleClass().add("market-catalogue");
        HBox.setHgrow(offerList, Priority.ALWAYS);
        HBox workspace = new HBox(16, offerList, offerActions);
        VBox.setVgrow(workspace, Priority.ALWAYS);
        box.getChildren().add(workspace);
        return box;
    }

    private Node createSentOffersTab(Stage stage) {
        VBox box = new VBox(12);
        box.getStyleClass().addAll("market-buy-root", "offer-workspace");
        Label feedback = label("Selecciona una oferta pendiente para cancelarla.", "muted-label");
        java.util.Map<Long, Player> playersById = new PlayerRepository().findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Player::getId, player -> player));
        java.util.Map<Long, Team> teamsById = teams.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Team::getId, team -> team));
        ListView<TransferOffer> sent = new ListView<>();
        sent.getStyleClass().add("data-list");
        sent.setCellFactory(view -> new ListCell<>() {
            @Override protected void updateItem(TransferOffer offer, boolean empty) {
                super.updateItem(offer, empty);
                if (empty || offer == null) setText(null);
                else {
                    Player player = playersById.get(offer.getPlayer().getId());
                    Team seller = teamsById.get(offer.getSellingTeam().getId());
                    String counter = offer.getCounterAmount() == null ? ""
                            : String.format("  •  Contraoferta €%.1fM",
                            offer.getCounterAmount() / 1_000_000);
                    setText(player.getFullName() + "  •  " + seller.getName()
                            + String.format("  •  €%.1fM", offer.getAmount() / 1_000_000)
                            + "  •  " + offerStatusLabel(offer) + counter
                            + "  •  límite " + offer.getResponseDeadline());
                }
            }
        });
        sent.getItems().addAll(new TransferOfferRepository()
                .findByBuyingTeam(career.getControlledTeam().getId()));
        Button cancel = button("CANCELAR OFERTA PENDIENTE", "ghost-button");
        cancel.setOnAction(event -> {
            TransferOffer selected = sent.getSelectionModel().getSelectedItem();
            if (selected == null) {
                feedback.setText("Selecciona primero una oferta.");
                animateFeedback(feedback, false);
                return;
            }
            try {
                TransferOffer updated = new TransferOfferService().cancelOffer(selected.getId(),
                        career.getControlledTeam().getId());
                selected.setStatus(updated.getStatus());
                selected.setResolutionReason(updated.getResolutionReason());
                sent.refresh();
                feedback.setText("Oferta retirada. El historial conservará la negociación.");
                animateFeedback(feedback, true);
            } catch (IllegalStateException exception) {
                feedback.setText("Solo se pueden cancelar ofertas que siguen pendientes.");
                animateFeedback(feedback, false);
            }
        });
        if (sent.getItems().isEmpty()) sent.setPlaceholder(label(
                "Aún no has enviado ofertas en esta carrera.", "muted-label"));
        VBox.setVgrow(sent, Priority.ALWAYS);
        VBox negotiationList = new VBox(10,
                label("NEGOCIACIONES", "market-column-title"), sent);
        negotiationList.getStyleClass().add("market-catalogue");
        HBox.setHgrow(negotiationList, Priority.ALWAYS);
        VBox actions = new VBox(12, label("GESTIÓN", "market-column-title"),
                label("Selecciona una negociación pendiente para retirarla. Las resueltas permanecen en el historial.",
                        "muted-label"), cancel, feedback);
        actions.getStyleClass().add("market-selection-card");
        actions.setPrefWidth(330);
        cancel.setMaxWidth(Double.MAX_VALUE);
        HBox workspace = new HBox(16, negotiationList, actions);
        VBox.setVgrow(workspace, Priority.ALWAYS);
        box.getChildren().add(workspace);
        return box;
    }

    private String offerStatusLabel(TransferOffer offer) {
        if (offer.getStatus() == footballcareer.model.enums.TransferOfferStatus.WITHDRAWN) {
            return "EXPIRED".equals(offer.getResolutionReason()) ? "CADUCADA" : "CANCELADA";
        }
        return switch (offer.getStatus()) {
            case PENDING -> "PENDIENTE";
            case ACCEPTED -> "ACEPTADA";
            case REJECTED -> "RECHAZADA";
            case COMPLETED -> "COMPLETADA";
            case WITHDRAWN -> "CANCELADA";
        };
    }

    private void executeTransfer(TransferOffer offer, Player player) {
        executeTransfer(offer, player, player.getSalary(), 3);
    }

    private void executeTransfer(TransferOffer offer, Player player,
            double salary, int contractYears) {
        new TransferExecutionService().completeTransfer(offer.getId(),
                salary, career.getCurrentDate().plusYears(contractYears),
                career.getCurrentSeason().getId(), career.getCurrentDate());
    }

    private void executeTransfer(TransferOffer offer, Player player,
            double salary, int contractYears, double signingBonus,
            double releaseClause, String squadRole) {
        TransferExecutionService.ContractTerms terms =
                new TransferExecutionService.ContractTerms(salary, signingBonus,
                        releaseClause, squadRole);
        new TransferExecutionService().completeTransfer(offer.getId(), terms,
                career.getCurrentDate().plusYears(contractYears),
                career.getCurrentSeason().getId(), career.getCurrentDate());
    }

    private void respondToIncomingOffer(ListView<TransferOffer> incoming, TransferOffer offer,
            boolean accept, Label feedback) {
        if (offer == null) {
            feedback.setText("Selecciona una oferta recibida.");
            animateFeedback(feedback, false);
            return;
        }
        try {
            TransferOffer updated = new TransferOfferService()
                    .respondToIncomingOffer(offer.getId(), accept);
            if (accept) {
                Player player = new PlayerRepository().findById(offer.getPlayer().getId());
                executeTransfer(updated, player);
                feedback.setText("Venta completada: " + player.getFullName());
            } else feedback.setText("Oferta rechazada.");
            animateFeedback(feedback, accept);
            incoming.getItems().remove(offer);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            feedback.setText(exception.getMessage());
            animateFeedback(feedback, false);
        }
    }

    private void respondWithIncomingCounter(ListView<TransferOffer> incoming, TransferOffer offer,
            TextField counterAmount, Label feedback) {
        if (offer == null) {
            feedback.setText("Selecciona una oferta recibida.");
            animateFeedback(feedback, false);
            return;
        }
        try {
            double amount = Double.parseDouble(counterAmount.getText().replace(',', '.'))
                    * 1_000_000;
            TransferOffer updated = new TransferOfferService()
                    .respondWithCounterOffer(offer.getId(), amount);
            if (updated.getStatus()
                    == footballcareer.model.enums.TransferOfferStatus.ACCEPTED) {
                Player player = new PlayerRepository().findById(updated.getPlayer().getId());
                executeTransfer(updated, player);
                feedback.setText("Contraoferta aceptada. Venta completada: "
                        + player.getFullName());
                animateFeedback(feedback, true);
            } else {
                feedback.setText("El club comprador ha rechazado la contraoferta.");
                animateFeedback(feedback, false);
            }
            incoming.getItems().remove(offer);
        } catch (NumberFormatException exception) {
            feedback.setText("Introduce una contraoferta válida.");
            animateFeedback(feedback, false);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            feedback.setText(exception.getMessage());
            animateFeedback(feedback, false);
        }
    }

    private void animateFeedback(Label feedback, boolean success) {
        feedbackAnimator.animate(feedback, success);
    }

    private void showPlayer(Stage stage, Player player, String origin) {
        PlayerState state = new PlayerStateRepository().findByPlayer(player.getId());
        Contract contract = new ContractRepository().findActiveByPlayer(player.getId());
        PlayerSeasonStats seasonStats = new PlayerSeasonStatsRepository()
                .find(player.getId(), career.getCurrentSeason().getId());
        VBox content = page(player.getFullName(), player.getPosition() + "  •  "
                + player.getNationality() + "  •  "
                + player.getAge(career.getCurrentDate()) + " años");
        HBox headline = new HBox(16,
                statCard("MEDIA", String.valueOf(player.getOverall())),
                statCard("POTENCIAL", String.valueOf(player.getPotential())),
                statCard("FORMA", state == null ? "—" : String.valueOf(state.getForm())),
                statCard("FITNESS", state == null ? "—" : String.valueOf(state.getFitness())));
        headline.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
        GridPane attributes = new GridPane();
        attributes.getStyleClass().add("attributes-grid");
        attributes.setHgap(36);
        attributes.setVgap(14);
        addAttribute(attributes, 0, "Ritmo", player.getPace(), "Tiro", player.getShooting());
        addAttribute(attributes, 1, "Pase", player.getPassing(), "Regate", player.getDribbling());
        addAttribute(attributes, 2, "Defensa", player.getDefending(), "Físico", player.getPhysical());
        var assessment = new PlayerRoleAssessmentService().assess(player, state);
        VBox performance = panel("LECTURA FUTBOLÍSTICA");
        FlowPane impactChips = new FlowPane(9, 9);
        assessment.attributes().forEach(attribute -> {
            Label chip = label((attribute.key() ? "★ " : "") + attribute.name() + "  "
                    + attribute.value(), attribute.key() ? "status-chip" : "muted-chip");
            Tooltip.install(chip, new Tooltip(attribute.effect()));
            impactChips.getChildren().add(chip);
        });
        performance.getChildren().addAll(
                label("Nivel efectivo hoy  " + assessment.effectiveLevel() + "  •  "
                        + assessment.condition(), "comparison-label"),
                impactChips,
                label("Fortaleza clave: " + assessment.strongest()
                        + "  •  Aspecto prioritario: " + assessment.weakness()
                        + ". Las estrellas señalan los atributos de mayor peso para "
                        + player.getPosition() + ".", "muted-label"));
        VBox contractPanel = panel("CONTRATO");
        contractPanel.getChildren().add(label(contract == null ? "Sin contrato activo"
                : String.format("Hasta %s  •  Salario €%.2fM",
                contract.getEndDate(), contract.getSalary() / 1_000_000), "body-label"));
        Long currentTeamId = new PlayerTeamRepository().findCurrentTeamId(player.getId());
        if (contract != null && currentTeamId != null
                && currentTeamId == career.getControlledTeam().getId()) {
            ComboBox<Integer> extension = new ComboBox<>();
            extension.getItems().addAll(1, 2, 3, 4, 5);
            extension.setValue(2);
            TextField salary = new TextField(String.format("%.2f",
                    contract.getSalary() / 1_000_000));
            salary.setPromptText("Nuevo salario (€M/año)");
            Label renewalFeedback = label("La renovación utiliza el margen salarial disponible.",
                    "muted-label");
            Button renew = button("RENOVAR CONTRATO", "secondary-button");
            renew.setOnAction(event -> {
                try {
                    double newSalary = Double.parseDouble(salary.getText().replace(',', '.'))
                            * 1_000_000;
                    java.time.LocalDate newEndDate = contract.getEndDate()
                            .plusYears(extension.getValue());
                    new ContractRenewalService().renew(player.getId(), currentTeamId,
                            newEndDate, newSalary);
                    showPlayer(stage, new PlayerRepository().findById(player.getId()), origin);
                } catch (NumberFormatException exception) {
                    renewalFeedback.setText("Introduce un salario válido.");
                } catch (IllegalArgumentException | IllegalStateException exception) {
                    renewalFeedback.setText(exception.getMessage());
                }
            });
            contractPanel.getChildren().addAll(new FlowPane(10, 10,
                    label("AÑOS EXTRA", "muted-label"), extension, salary, renew),
                    renewalFeedback);
        }
        VBox statsPanel = panel("TEMPORADA ACTUAL");
        statsPanel.getChildren().add(label(seasonStats == null ? "Sin estadísticas"
                : "Partidos " + seasonStats.getAppearances() + "  •  Goles "
                + seasonStats.getGoals() + "  •  Asistencias " + seasonStats.getAssists()
                + "  •  Valoración " + String.format("%.2f", seasonStats.getAverageRating()),
                "body-label"));
        VBox evolution = new PlayerEvolutionView().build(player, career.getCurrentDate());
        java.util.List<Player> currentSquad = new PlayerRepository()
                .findCurrentPlayersByTeam(career.getControlledTeam().getId());
        boolean ownPlayer = currentTeamId != null
                && currentTeamId == career.getControlledTeam().getId();
        VBox profile = panel("PERFIL Y ESTATUS");
        String role = ownPlayer ? squadRole(player, currentSquad) : "RIVAL";
        String expectedMinutes = switch (role) {
            case "CLAVE" -> "La mayoría de partidos";
            case "TITULAR" -> "Titular habitual";
            case "ROTACIÓN" -> "Rotación frecuente";
            case "PROMESA" -> "Desarrollo progresivo";
            default -> "Sin compromiso";
        };
        profile.getChildren().addAll(
                label("Pierna preferida  " + player.getPreferredFoot()
                        + "  •  Altura  " + player.getHeightCm() + " cm", "body-label"),
                label("Posición natural  " + player.getPosition()
                        + "  •  Secundaria  " + (player.getSecondaryPosition() == null
                        ? "—" : player.getSecondaryPosition()), "body-label"),
                label("Rol  " + role + "  •  Minutos esperados  " + expectedMinutes,
                        "body-label"),
                label("Moral  " + (state == null ? "—" : state.getMorale())
                        + "/100  •  Valor  €" + String.format("%.1fM",
                        player.getMarketValue() / 1_000_000), "comparison-label"),
                label("Disponibilidad  " + availabilityLabel(state, career.getCurrentDate()),
                        state != null && !state.isAvailableOn(career.getCurrentDate())
                                ? "warning-feedback" : "success-feedback"));

        VBox actions = panel("ACCIONES");
        FlowPane actionButtons = new FlowPane(10, 10);
        if (ownPlayer) {
            TextField askingPrice = new TextField(String.format(java.util.Locale.ROOT, "%.1f",
                    player.getMarketValue() / 1_000_000));
            askingPrice.setPromptText("Precio de venta (€M)");
            Button sell = button("PONER EN VENTA", "secondary-button");
            Label actionFeedback = label("Gestiona al jugador sin abandonar su ficha.",
                    "muted-label");
            sell.setOnAction(event -> {
                try {
                    double price = Double.parseDouble(askingPrice.getText().replace(',', '.'))
                            * 1_000_000;
                    new PlayerMarketRepository().listForTransfer(player.getId(), price);
                    actionFeedback.setText("Jugador puesto en venta por €"
                            + String.format("%.1fM", price / 1_000_000) + ".");
                    animateFeedback(actionFeedback, true);
                } catch (IllegalArgumentException exception) {
                    actionFeedback.setText("Introduce un precio de venta válido.");
                    animateFeedback(actionFeedback, false);
                }
            });
            Button lineup = button("LLEVAR A ALINEACIÓN", "primary-button");
            lineup.setOnAction(event -> {
                requestedLineupPlayerId = player.getId();
                showLineup(stage);
            });
            actionButtons.getChildren().addAll(askingPrice, sell, lineup);
            actions.getChildren().addAll(actionButtons, actionFeedback,
                    playerConversationView.build(career, player));
        } else {
            Button negotiate = button("NEGOCIAR FICHAJE", "primary-button");
            negotiate.setOnAction(event -> showTransferNegotiation(stage, player));
            actionButtons.getChildren().add(negotiate);
            actions.getChildren().add(actionButtons);
        }
        Button back = button("market".equals(origin)
                ? "VOLVER AL MERCADO" : "VOLVER A LA PLANTILLA", "ghost-button");
        back.setOnAction(event -> {
            if ("market".equals(origin)) showMarket(stage);
            else showSquad(stage);
        });
        content.getChildren().addAll(headline, attributes, performance, profile, contractPanel,
                statsPanel, evolution, actions, back);
        showCareerShell(stage, content);
    }

    private void showTransferHistory(Stage stage) {
        activeSection = "history";
        showCareerShell(stage, transferHistoryView.build(
                career.getControlledTeam().getName(), career.getControlledTeam().getId()));
    }

    private void showTraining(Stage stage) {
        activeSection = "training";
        TrainingService service = new TrainingService();
        TrainingService.TrainingType completed = service.findToday(career);
        VBox content = page("ENTRENAMIENTO",
                career.getCurrentDate() + "  •  Planifica la carga diaria de la plantilla");

        VBox status = panel("ESTADO DE LA PLANTILLA");
        status.getChildren().add(label(squadStatusSummary(), "body-label"));
        if (lastTrainingSummary != null) {
            status.getChildren().add(label(lastTrainingSummary, "success-feedback"));
        }
        if (completed != null) {
            status.getChildren().add(label("Sesión de hoy completada: "
                    + trainingName(completed) + ". Avanza de día para volver a entrenar.",
                    "warning-feedback"));
        }

        HBox choices = new HBox(18);
        choices.getStyleClass().add("training-grid");
        choices.getChildren().addAll(
                trainingCard(stage, service, TrainingService.TrainingType.RECOVERY,
                        "RECUPERACIÓN", "Regenera al equipo tras un esfuerzo alto.",
                        "Fitness +10  •  Moral +1  •  Forma -1", completed != null),
                trainingCard(stage, service, TrainingService.TrainingType.BALANCED,
                        "EQUILIBRADA", "Trabajo general con desgaste moderado.",
                        "Forma +2  •  Moral +1  •  Fitness -3", completed != null),
                trainingCard(stage, service, TrainingService.TrainingType.INTENSIVE,
                        "INTENSIVA", "Mejora la forma a costa de energía y moral.",
                        "Forma +4  •  Fitness -8  •  Moral -1", completed != null));
        content.getChildren().addAll(status, choices);
        showCareerShell(stage, content);
    }

    private void showMedical(Stage stage) {
        activeSection = "medical";
        VBox content = page("CENTRO MÉDICO",
                "Diagnóstico, recuperación y sanciones de la plantilla");
        java.util.Map<Long, PlayerState> states = new PlayerStateRepository().findAll();
        java.util.List<Player> unavailable = new PlayerRepository()
                .findCurrentPlayersByTeam(career.getControlledTeam().getId()).stream()
                .filter(player -> {
                    PlayerState state = states.get(player.getId());
                    return state != null && !state.isAvailableOn(career.getCurrentDate());
                }).toList();
        ListView<Player> patients = new ListView<>();
        patients.getStyleClass().add("data-list");
        patients.setCellFactory(view -> new ListCell<>() {
            @Override protected void updateItem(Player player, boolean empty) {
                super.updateItem(player, empty);
                if (empty || player == null) setText(null);
                else {
                    PlayerState state = states.get(player.getId());
                    long days = java.time.temporal.ChronoUnit.DAYS.between(
                            career.getCurrentDate(), state.getUnavailableUntil());
                    setText(player.getPosition() + "  •  " + player.getFullName()
                            + "  •  " + availabilityLabel(state, career.getCurrentDate())
                            + "  •  " + Math.max(1, days) + " día(s)"
                            + "  •  Fitness " + state.getFitness());
                }
            }
        });
        patients.getItems().addAll(unavailable);
        patients.setPlaceholder(label("No hay jugadores lesionados ni sancionados.",
                "muted-label"));
        Label detail = label("Selecciona un lesionado para decidir su recuperación.",
                "comparison-label");
        patients.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> {
                    if (selected == null) return;
                    PlayerState state = states.get(selected.getId());
                    detail.setText("INJURY".equals(state.getUnavailableReason())
                            ? "Rehabilitación: -2 días y +6 fitness. Especialista: -5 días y +2 fitness."
                            : "Las sanciones son disciplinarias y no admiten tratamiento médico.");
                });
        Label feedback = label("Solo puede aplicarse un tratamiento por jugador y día.",
                "muted-label");
        Button rehab = button("REHABILITACIÓN", "primary-button");
        Button specialist = button("CONSULTAR ESPECIALISTA", "secondary-button");
        java.util.function.Consumer<MedicalTreatmentService.Treatment> treat = treatment -> {
            Player selected = patients.getSelectionModel().getSelectedItem();
            if (selected == null) {
                feedback.setText("Selecciona primero un jugador lesionado.");
                animateFeedback(feedback, false);
                return;
            }
            try {
                MedicalTreatmentService.Result result = new MedicalTreatmentService()
                        .treat(career, selected.getId(), treatment);
                feedback.setText("Regreso adelantado del " + result.previousReturn()
                        + " al " + result.newReturn() + ". Fitness +" + result.fitnessGain() + ".");
                animateFeedback(feedback, true);
                PlayerState refreshed = new PlayerStateRepository().findByPlayer(selected.getId());
                states.put(selected.getId(), refreshed);
                patients.refresh();
                rehab.setDisable(true);
                specialist.setDisable(true);
            } catch (IllegalStateException exception) {
                feedback.setText(exception.getMessage());
                animateFeedback(feedback, false);
            }
        };
        rehab.setOnAction(event -> treat.accept(MedicalTreatmentService.Treatment.REHAB));
        specialist.setOnAction(event -> treat.accept(
                MedicalTreatmentService.Treatment.SPECIALIST));
        patients.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> {
                    boolean disabled = selected == null;
                    if (selected != null) {
                        PlayerState state = states.get(selected.getId());
                        disabled = !"INJURY".equals(state.getUnavailableReason())
                                || new MedicalTreatmentService().treatedToday(career.getId(),
                                selected.getId(), career.getCurrentDate());
                    }
                    rehab.setDisable(disabled);
                    specialist.setDisable(disabled);
                });
        VBox medicalDesk = panel("PLAN DE RECUPERACIÓN");
        medicalDesk.getChildren().addAll(detail,
                new FlowPane(10, 10, rehab, specialist), feedback);
        VBox.setVgrow(patients, Priority.ALWAYS);
        content.getChildren().addAll(patients, medicalDesk);
        showCareerShell(stage, content);
    }

    private VBox trainingCard(Stage stage, TrainingService service,
            TrainingService.TrainingType type, String title, String detail,
            String effects, boolean disabled) {
        Label heading = label(title, "card-title");
        Label description = label(detail, "body-label");
        description.setWrapText(true);
        Label effectLabel = label(effects, "training-effects");
        Button select = button(disabled ? "COMPLETADA" : "REALIZAR SESIÓN", "primary-button");
        select.setDisable(disabled);
        select.setMaxWidth(Double.MAX_VALUE);
        select.setOnAction(event -> {
            TrainingService.TrainingResult result = service.train(career, type);
            lastTrainingSummary = trainingName(result.type()) + " completada para "
                    + result.affectedPlayers() + " jugadores.";
            showTraining(stage);
        });
        VBox card = new VBox(14, heading, description, effectLabel, select);
        card.getStyleClass().add("training-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private String trainingName(TrainingService.TrainingType type) {
        return switch (type) {
            case RECOVERY -> "Recuperación";
            case BALANCED -> "Equilibrada";
            case INTENSIVE -> "Intensiva";
        };
    }

    private void showLineup(Stage stage) {
        activeSection = "lineup";
        Match nextMatch = findNextMatch();
        VBox content = page("ALINEACIÓN", nextMatch == null
                ? "No hay un próximo partido disponible."
                : "Próximo partido: " + nextMatch.getHomeTeam().getShortName()
                + " vs " + nextMatch.getAwayTeam().getShortName());
        if (nextMatch == null) {
            showCareerShell(stage, content);
            return;
        }
        long teamId = career.getControlledTeam().getId();
        MatchLineupRepository repository = new MatchLineupRepository();
        MatchTacticsRepository tacticsRepository = new MatchTacticsRepository();
        LineupService lineupService = new LineupService(new PlayerRepository(),
                new PlayerStateRepository(), repository);
        MatchLineup savedLineup = repository.find(nextMatch.getId(), teamId);
        MatchLineup initial = lineupService.selectMatchLineup(nextMatch.getId(), teamId);
        ListView<Player> starters = playerList();
        starters.getItems().addAll(initial.getStarters());
        java.util.Set<Long> benchIds = initial.getSubstitutes().stream()
                .map(Player::getId).collect(java.util.stream.Collectors.toCollection(
                        java.util.LinkedHashSet::new));
        ListView<Player> available = lineupAvailableList(benchIds);
        java.util.Map<Long, PlayerState> lineupStates = new PlayerStateRepository().findAll();
        new PlayerRepository().findCurrentPlayersByTeam(teamId).stream()
                .filter(player -> starters.getItems().stream()
                .noneMatch(starter -> starter.getId() == player.getId()))
                .filter(player -> {
                    PlayerState state = lineupStates.get(player.getId());
                    return state != null && state.isAvailableOn(career.getCurrentDate());
                })
                .sorted(Comparator.comparingInt((Player player) -> PlayerOrdering.position(
                        player.getPosition())).thenComparing(
                        Comparator.comparingInt(Player::getOverall).reversed()))
                .forEach(available.getItems()::add);
        if (requestedLineupPlayerId != null) {
            available.getItems().stream()
                    .filter(player -> player.getId() == requestedLineupPlayerId)
                    .findFirst().ifPresent(player -> available.getSelectionModel().select(player));
            starters.getItems().stream()
                    .filter(player -> player.getId() == requestedLineupPlayerId)
                    .findFirst().ifPresent(player -> starters.getSelectionModel().select(player));
            requestedLineupPlayerId = null;
        }
        VBox tacticalPitch = new VBox(14);
        tacticalPitch.getStyleClass().add("tactical-pitch");
        ComboBox<String> formation = new ComboBox<>();
        formation.getItems().addAll("4-3-3", "4-2-3-1", "4-4-2");
        MatchTacticsRepository.TacticalSetup savedTactics = tacticsRepository.find(
                nextMatch.getId(), teamId);
        MatchRoleRepository.Assignment savedRoles = new MatchRoleRepository()
                .find(nextMatch.getId(), teamId);
        ComboBox<Player> captain = new ComboBox<>(); captain.setConverter(playerStringConverter());
        ComboBox<Player> penalties = new ComboBox<>(); penalties.setConverter(playerStringConverter());
        ComboBox<Player> corners = new ComboBox<>(); corners.setConverter(playerStringConverter());
        java.util.function.BiFunction<Long, java.util.Comparator<Player>, Player> rolePlayer =
                (savedId, fallback) -> starters.getItems().stream()
                        .filter(player -> savedId != null && player.getId() == savedId).findFirst()
                        .orElseGet(() -> starters.getItems().stream().max(fallback).orElse(null));
        formation.setValue(savedTactics.formation());
        if (savedLineup == null) starters.getItems().setAll(
                new footballcareer.ui.LineupSlotPlanner().arrange(
                        starters.getItems(), formation.getValue()));
        ComboBox<String> mentality = new ComboBox<>();
        mentality.getItems().addAll("DEFENSIVE", "BALANCED", "ATTACKING");
        mentality.setValue(savedTactics.mentality());
        ComboBox<String> pressing = new ComboBox<>(); pressing.getItems().addAll(
                "LOW", "MEDIUM", "HIGH");
        pressing.setValue(savedTactics.pressing());
        ComboBox<String> tempo = new ComboBox<>(); tempo.getItems().addAll(
                "SLOW", "NORMAL", "FAST");
        tempo.setValue(savedTactics.tempo());
        Label tacticalWarning = label("", "warning-feedback");
        Runnable[] refreshEditorRef = {null};
        Runnable refreshPitch = () -> {
            java.util.List<Player> selected = java.util.List.copyOf(starters.getItems());
            renderPitch(tacticalPitch, selected, formation.getValue());
            installPitchInteractions(tacticalPitch, starters, available, benchIds,
                    refreshEditorRef);
            tacticalWarning.setText(formationAssessment(selected, formation.getValue(), lineupStates)
                    + "  •  " + tacticalRiskSummary(mentality.getValue(), pressing.getValue(),
                    tempo.getValue()));
        };
        formation.setOnAction(event -> {
            starters.getItems().setAll(
                    new footballcareer.ui.LineupSlotPlanner().arrange(
                            starters.getItems(), formation.getValue()));
            refreshPitch.run();
        });
        mentality.setOnAction(event -> refreshPitch.run());
        pressing.setOnAction(event -> refreshPitch.run());
        tempo.setOnAction(event -> refreshPitch.run());
        refreshPitch.run();
        Label count = label("", "eyebrow");
        Label editorHint = label(
                "Arrastra un titular sobre otro para intercambiar sus puestos.",
                "muted-label");
        available.setMinHeight(170);
        available.setPrefHeight(290);
        Runnable refreshEditor = () -> {
            count.setText("TITULARES  " + starters.getItems().size() + " / 11");
            long benchCount = available.getItems().stream()
                    .filter(player -> benchIds.contains(player.getId())).count();
            editorHint.setText("Disponibles: " + available.getItems().size()
                    + "  •  Banquillo: " + benchCount + " / 7  •  Reservas: "
                    + (available.getItems().size() - benchCount));
            available.refresh();
            Player selectedCaptain = captain.getValue(); Player selectedPenalty = penalties.getValue();
            Player selectedCorner = corners.getValue();
            captain.getItems().setAll(starters.getItems()); penalties.getItems().setAll(starters.getItems());
            corners.getItems().setAll(starters.getItems());
            captain.setValue(starters.getItems().contains(selectedCaptain) ? selectedCaptain
                    : rolePlayer.apply(savedRoles == null ? null : savedRoles.captainId(),
                    Comparator.comparingInt(Player::getOverall)));
            penalties.setValue(starters.getItems().contains(selectedPenalty) ? selectedPenalty
                    : rolePlayer.apply(savedRoles == null ? null : savedRoles.penaltyTakerId(),
                    Comparator.comparingInt(Player::getShooting)));
            corners.setValue(starters.getItems().contains(selectedCorner) ? selectedCorner
                    : rolePlayer.apply(savedRoles == null ? null : savedRoles.cornerTakerId(),
                    Comparator.comparingInt(Player::getPassing)));
            refreshPitch.run();
        };
        refreshEditorRef[0] = refreshEditor;
        Button swap = button("INTERCAMBIAR", "primary-button");
        swap.setOnAction(event -> {
            Player outgoing = starters.getSelectionModel().getSelectedItem();
            Player incoming = available.getSelectionModel().getSelectedItem();
            if (outgoing == null || incoming == null) {
                editorHint.setText("Selecciona un titular y un jugador de la lista disponible.");
                return;
            }
            int slot = starters.getItems().indexOf(outgoing);
            starters.getItems().set(slot, incoming);
            available.getItems().remove(incoming);
            available.getItems().add(outgoing);
            if (benchIds.remove(incoming.getId())) benchIds.add(outgoing.getId());
            starters.getSelectionModel().select(slot);
            editorHint.setText(incoming.getFullName() + " entra por " + outgoing.getFullName() + ".");
            refreshEditor.run();
        });
        Button addStarter = button("COMPLETAR HUECO", "secondary-button");
        addStarter.setOnAction(event -> {
            Player incoming = available.getSelectionModel().getSelectedItem();
            if (incoming == null) {
                editorHint.setText("Selecciona primero un jugador disponible.");
            } else if (starters.getItems().size() >= 11) {
                editorHint.setText("El once está completo: selecciona un titular y usa INTERCAMBIAR.");
            } else {
                available.getItems().remove(incoming);
                benchIds.remove(incoming.getId());
                starters.getItems().add(incoming);
                editorHint.setText(incoming.getFullName() + " se incorpora al once.");
                refreshEditor.run();
            }
        });
        Button removeStarter = button("QUITAR DEL ONCE", "ghost-button");
        removeStarter.setOnAction(event -> {
            Player selected = starters.getSelectionModel().getSelectedItem();
            if (selected == null) {
                editorHint.setText("Selecciona el titular que quieres retirar.");
                return;
            }
            starters.getItems().remove(selected);
            benchIds.remove(selected.getId());
            available.getItems().add(selected);
            available.getSelectionModel().select(selected);
            editorHint.setText("Hueco libre en el once. Elige un sustituto y pulsa COMPLETAR HUECO.");
            refreshEditor.run();
        });
        Button toBench = button("AÑADIR AL BANQUILLO", "secondary-button");
        Button toReserves = button("PASAR A RESERVAS", "ghost-button");
        toBench.setOnAction(event -> {
            Player selected = available.getSelectionModel().getSelectedItem();
            if (selected != null && benchIds.size() < 7) {
                benchIds.add(selected.getId());
                refreshEditor.run();
            } else if (selected != null) {
                editorHint.setText("El banquillo ya tiene siete jugadores.");
            }
        });
        toReserves.setOnAction(event -> {
            Player selected = available.getSelectionModel().getSelectedItem();
            if (selected != null) {
                benchIds.remove(selected.getId());
                refreshEditor.run();
            }
        });
        Button recommended = button("RESTAURAR ONCE RECOMENDADO", "secondary-button");
        recommended.setOnAction(event -> {
            MatchLineup suggestion = lineupService.selectMatchLineup(teamId);
            starters.getItems().setAll(suggestion.getStarters());
            benchIds.clear();
            suggestion.getSubstitutes().forEach(player -> benchIds.add(player.getId()));
            java.util.Set<Long> starterIds = starters.getItems().stream()
                    .map(Player::getId).collect(java.util.stream.Collectors.toSet());
            available.getItems().setAll(new PlayerRepository().findCurrentPlayersByTeam(teamId)
                    .stream().filter(player -> !starterIds.contains(player.getId()))
                    .filter(player -> {
                        PlayerState state = lineupStates.get(player.getId());
                        return state != null && state.isAvailableOn(career.getCurrentDate());
                    })
                    .sorted(Comparator.comparingInt((Player player) -> PlayerOrdering.position(
                            player.getPosition())).thenComparing(
                            Comparator.comparingInt(Player::getOverall).reversed())).toList());
            editorHint.setText("Once recomendado restaurado según GRL, forma y fitness.");
            refreshEditor.run();
        });
        available.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                if (starters.getSelectionModel().getSelectedItem() == null) addStarter.fire();
                else swap.fire();
            }
        });
        available.setOnDragDetected(event -> {
            Player selected = available.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            javafx.scene.input.Dragboard dragboard = available.startDragAndDrop(
                    javafx.scene.input.TransferMode.MOVE);
            javafx.scene.input.ClipboardContent dragged = new javafx.scene.input.ClipboardContent();
            dragged.putString(Long.toString(selected.getId()));
            dragboard.setContent(dragged);
            event.consume();
        });
        VBox squadEditor = panel("EDITOR DE CONVOCATORIA");
        squadEditor.getStyleClass().add("lineup-editor");
        squadEditor.setMinWidth(390);
        long unavailableCount = lineupStates.values().stream()
                .filter(state -> !state.isAvailableOn(career.getCurrentDate())).count();
        squadEditor.getChildren().addAll(count,
                label(unavailableCount == 0 ? "Plantilla disponible al completo."
                        : unavailableCount + " baja(s) médica(s) o disciplinaria(s) excluidas.",
                        unavailableCount == 0 ? "success-feedback" : "warning-feedback"),
                label("Haz clic en un jugador del campo para seleccionarlo. "
                        + "También puedes arrastrar desde esta lista directamente al campo.",
                        "muted-label"),
                new FlowPane(8, 8, swap, addStarter, removeStarter),
                label("RESTO DE LA PLANTILLA", "panel-title"), available,
                new FlowPane(8, 8, toBench, toReserves, recommended), editorHint);
        SplitPane pitch = new SplitPane(tacticalPitch, squadEditor);
        pitch.setDividerPositions(0.56);
        pitch.setMinHeight(410);
        refreshEditor.run();
        Label feedback = label("El banquillo se gestiona explícitamente y admite siete jugadores.",
                "muted-label");
        Button save = button("GUARDAR ALINEACIÓN", "primary-button");
        save.setOnAction(event -> {
            try {
                validateFormation(java.util.List.copyOf(starters.getItems()),
                        formation.getValue(), lineupStates);
                var selectedStarters = java.util.List.copyOf(starters.getItems());
                var selectedSubstitutes = available.getItems().stream()
                        .filter(player -> benchIds.contains(player.getId())).limit(7).toList();
                var setup = new MatchTacticsRepository.TacticalSetup(formation.getValue(),
                        mentality.getValue(), pressing.getValue(), tempo.getValue());
                var roles = new MatchRoleRepository.Assignment(captain.getValue().getId(),
                        penalties.getValue().getId(), corners.getValue().getId());
                repository.save(nextMatch.getId(), teamId, selectedStarters, selectedSubstitutes);
                tacticsRepository.save(nextMatch.getId(), teamId, setup);
                new MatchRoleRepository().save(nextMatch.getId(), teamId, roles);
                new TeamSheetRepository().save(career.getId(), teamId,
                        new TeamSheetRepository.Sheet(selectedStarters, selectedSubstitutes,
                                setup, roles));
                feedbackAnimator.confirmSave(save, feedback, "Once base " + formation.getValue()
                        + " guardado a las " + java.time.LocalTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                        + ". Titulares, banquillo, roles y táctica se reutilizarán.");
            } catch (IllegalArgumentException exception) {
                feedback.setText(exception.getMessage());
                feedback.setAccessibleText("No se pudo guardar la alineación");
                animateFeedback(feedback, false);
            }
        });
        VBox.setVgrow(pitch, Priority.ALWAYS);
        FlowPane tacticalHeader = new FlowPane(12, 12,
                label("FORMACIÓN", "panel-title"), formation,
                label("MENTALIDAD", "panel-title"), mentality,
                label("PRESIÓN", "panel-title"), pressing,
                label("RITMO", "panel-title"), tempo);
        FlowPane roleHeader = new FlowPane(12, 12,
                label("CAPITÁN", "panel-title"), captain,
                label("PENALTIS", "panel-title"), penalties,
                label("CÓRNERS", "panel-title"), corners);
        tacticalHeader.getStyleClass().add("lineup-toolbar");
        TitledPane roleDrawer = new TitledPane("ROLES Y LANZADORES", roleHeader);
        roleDrawer.getStyleClass().add("lineup-role-drawer"); roleDrawer.setExpanded(false);
        content.getChildren().addAll(tacticalHeader, roleDrawer, tacticalWarning,
                pitch, save, feedback);
        showCareerShell(stage, content);
    }

    private String tacticalRiskSummary(String mentality, String pressing, String tempo) {
        String approach = switch (mentality) {
            case "ATTACKING" -> "más amenaza, mayor exposición";
            case "DEFENSIVE" -> "bloque protegido, menos llegada";
            default -> "equilibrio entre líneas";
        };
        String pressure = "HIGH".equals(pressing) ? "presión intensa"
                : "LOW".equals(pressing) ? "bloque bajo" : "presión media";
        String pace = "FAST".equals(tempo) ? "transiciones rápidas"
                : "SLOW".equals(tempo) ? "posesión paciente" : "ritmo normal";
        return approach + "; " + pressure + "; " + pace;
    }

    private String formationAssessment(java.util.List<Player> starters, String formation,
            java.util.Map<Long, PlayerState> states) {
        long goalkeepers = starters.stream().filter(player -> player.getPosition()
                == footballcareer.model.enums.Position.GK).count();
        long defenders = starters.stream().filter(player -> java.util.Set.of(
                footballcareer.model.enums.Position.CB,
                footballcareer.model.enums.Position.LB,
                footballcareer.model.enums.Position.RB).contains(player.getPosition())).count();
        long midfielders = starters.stream().filter(player -> java.util.Set.of(
                footballcareer.model.enums.Position.CDM,
                footballcareer.model.enums.Position.CM,
                footballcareer.model.enums.Position.CAM).contains(player.getPosition())).count();
        long attackers = starters.stream().filter(player -> java.util.Set.of(
                footballcareer.model.enums.Position.LW,
                footballcareer.model.enums.Position.RW,
                footballcareer.model.enums.Position.ST).contains(player.getPosition())).count();
        long holding = starters.stream().filter(player -> java.util.Set.of(
                footballcareer.model.enums.Position.CDM,
                footballcareer.model.enums.Position.CM).contains(player.getPosition())).count();
        long advanced = starters.stream().filter(player -> java.util.Set.of(
                footballcareer.model.enums.Position.CAM,
                footballcareer.model.enums.Position.LW,
                footballcareer.model.enums.Position.RW).contains(player.getPosition())).count();
        long strikers = starters.stream().filter(player -> player.getPosition()
                == footballcareer.model.enums.Position.ST).count();
        long fatigued = starters.stream().map(player -> states.get(player.getId()))
                .filter(java.util.Objects::nonNull)
                .filter(state -> state.getFitness() < 70).count();
        boolean shapeFits = switch (formation) {
            case "4-2-3-1" -> holding >= 2 && advanced >= 3 && strikers >= 1;
            case "4-4-2" -> midfielders >= 4 && attackers >= 2;
            default -> midfielders >= 3 && attackers >= 3;
        };
        if (starters.size() != 11 || goalkeepers < 1 || defenders < 4 || !shapeFits) {
            String requirement = switch (formation) {
                case "4-2-3-1" -> "2 mediocentros, 3 mediapuntas/extremos y 1 delantero";
                case "4-4-2" -> "4 centrocampistas y 2 atacantes";
                default -> "3 centrocampistas y 3 atacantes";
            };
            return "DESAJUSTE: " + formation + " requiere 1 POR, 4 defensas y "
                    + requirement + ". Revisa las posiciones del once.";
        }
        return fatigued == 0 ? "Estructura correcta y sin titulares fatigados."
                : "Estructura correcta, pero hay " + fatigued + " titulares con fitness inferior a 70.";
    }

    private void validateFormation(java.util.List<Player> starters, String formation,
            java.util.Map<Long, PlayerState> states) {
        String assessment = formationAssessment(starters, formation, states);
        if (assessment.startsWith("DESAJUSTE")) throw new IllegalArgumentException(assessment);
    }

    private void renderPitch(VBox pitch, java.util.List<Player> starters, String formation) {
        pitch.getChildren().clear();
        java.util.List<java.util.List<String>> slots =
                new footballcareer.ui.LineupSlotPlanner().slots(formation);
        int offset = 0;
        for (java.util.List<String> rowSlots : slots) {
            int end = Math.min(starters.size(), offset + rowSlots.size());
            java.util.List<Player> row = offset >= end ? java.util.List.of()
                    : starters.subList(offset, end);
            pitch.getChildren().add(pitchLine(row,
                    rowSlots.subList(0, row.size())));
            offset = end;
        }
        if (starters.isEmpty()) pitch.getChildren().add(label(
                "Selecciona futbolistas para construir el once.", "pitch-empty"));
    }

    private HBox pitchLine(java.util.List<Player> players, java.util.List<String> slots) {
        HBox line = new HBox(12);
        line.setAlignment(Pos.CENTER);
        for (int index = 0; index < players.size(); index++) {
            Player player = players.get(index);
            String slot = slots.get(index);
            VBox chip = new VBox(2, label(slot, "pitch-position"),
                    label(player.getLastName(), "pitch-player-name"),
                    label(String.valueOf(player.getOverall()), "pitch-rating"));
            chip.setAlignment(Pos.CENTER);
            chip.getStyleClass().add("pitch-player");
            chip.setUserData(player);
            Tooltip.install(chip, new Tooltip(player.getFullName() + "  •  Puesto " + slot
                    + "  •  Posición natural " + player.getPosition() + "  •  GRL "
                    + player.getOverall()));
            line.getChildren().add(chip);
        }
        return line;
    }

    private void installPitchInteractions(VBox pitch, ListView<Player> starters,
            ListView<Player> available, java.util.Set<Long> benchIds,
            Runnable[] refreshEditorRef) {
        pitch.setOnDragOver(event -> {
            if (event.getGestureSource() != pitch && event.getDragboard().hasString()
                    && starters.getItems().size() < 11) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
            }
            event.consume();
        });
        pitch.setOnDragDropped(event -> {
            Player incoming = draggedPlayer(event.getDragboard(), available);
            boolean completed = incoming != null && starters.getItems().size() < 11;
            if (completed) {
                available.getItems().remove(incoming);
                benchIds.remove(incoming.getId());
                starters.getItems().add(incoming);
                if (refreshEditorRef[0] != null) refreshEditorRef[0].run();
            }
            event.setDropCompleted(completed);
            event.consume();
        });
        for (Node rowNode : pitch.getChildren()) {
            if (!(rowNode instanceof HBox row)) continue;
            for (Node playerNode : row.getChildren()) {
                if (!(playerNode.getUserData() instanceof Player outgoing)) continue;
                Player selectedStarter = starters.getSelectionModel().getSelectedItem();
                if (selectedStarter != null && selectedStarter.getId() == outgoing.getId())
                    playerNode.getStyleClass().add("pitch-player-selected");
                playerNode.setOnMouseClicked(event -> {
                    starters.getSelectionModel().select(outgoing);
                    pitch.getChildren().stream().filter(HBox.class::isInstance)
                            .map(HBox.class::cast).flatMap(rowBox -> rowBox.getChildren().stream())
                            .forEach(node -> node.getStyleClass().remove("pitch-player-selected"));
                    playerNode.getStyleClass().add("pitch-player-selected");
                    event.consume();
                });
                playerNode.setOnDragDetected(event -> {
                    javafx.scene.input.Dragboard dragboard = playerNode.startDragAndDrop(
                            javafx.scene.input.TransferMode.MOVE);
                    javafx.scene.input.ClipboardContent dragged =
                            new javafx.scene.input.ClipboardContent();
                    dragged.putString(Long.toString(outgoing.getId()));
                    dragboard.setContent(dragged);
                    event.consume();
                });
                playerNode.setOnDragOver(event -> {
                    if (event.getDragboard().hasString())
                        event.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
                    event.consume();
                });
                playerNode.setOnDragDropped(event -> {
                    Player incomingStarter = draggedPlayer(event.getDragboard(), starters);
                    Player incoming = draggedPlayer(event.getDragboard(), available);
                    boolean completed = incomingStarter != null
                            && incomingStarter.getId() != outgoing.getId();
                    if (completed) {
                        int from = starters.getItems().indexOf(incomingStarter);
                        int to = starters.getItems().indexOf(outgoing);
                        java.util.Collections.swap(starters.getItems(), from, to);
                        starters.getSelectionModel().select(to);
                        if (refreshEditorRef[0] != null) refreshEditorRef[0].run();
                    } else if (incoming != null) {
                        completed = true;
                        int slot = starters.getItems().indexOf(outgoing);
                        starters.getItems().set(slot, incoming);
                        available.getItems().remove(incoming);
                        available.getItems().add(outgoing);
                        if (benchIds.remove(incoming.getId())) benchIds.add(outgoing.getId());
                        starters.getSelectionModel().select(slot);
                        if (refreshEditorRef[0] != null) refreshEditorRef[0].run();
                    }
                    event.setDropCompleted(completed);
                    event.consume();
                });
            }
        }
    }

    private Player draggedPlayer(javafx.scene.input.Dragboard dragboard,
            ListView<Player> available) {
        if (!dragboard.hasString()) return null;
        try {
            long playerId = Long.parseLong(dragboard.getString());
            return available.getItems().stream()
                    .filter(player -> player.getId() == playerId).findFirst().orElse(null);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void showSettings(Stage stage) {
        activeSection = "settings";
        VBox content = page("AJUSTES", "Pantalla y experiencia de juego");
        VBox display = panel("PANTALLA");
        Label mode = label(stage.isFullScreen()
                ? "Modo actual: pantalla completa" : "Modo actual: ventana maximizada",
                "body-label");
        Button fullscreen = button("ACTIVAR PANTALLA COMPLETA", "primary-button");
        fullscreen.setDisable(stage.isFullScreen());
        fullscreen.setOnAction(event -> {
            stage.setFullScreen(true);
            showSettings(stage);
        });
        Button windowed = button("SALIR DE PANTALLA COMPLETA", "secondary-button");
        windowed.setDisable(!stage.isFullScreen());
        windowed.setOnAction(event -> {
            stage.setFullScreen(false);
            stage.setMaximized(true);
            showSettings(stage);
        });
        FlowPane screenActions = new FlowPane(12, 12, fullscreen, windowed);
        display.getChildren().addAll(mode, screenActions,
                label("También puedes usar F11 para alternar el modo de pantalla.", "muted-label"));
        CareerPreferencesRepository repository = new CareerPreferencesRepository();
        CareerPreferencesRepository.Preferences saved = repository.find(career.getId());
        VBox progression = panel("AVANCE E INTERRUPCIONES");
        CheckBox stopAtMatch = new CheckBox("Detenerse cuando llegue un partido del equipo");
        CheckBox stopOnOffer = new CheckBox("Detenerse cuando llegue una nueva oferta");
        CheckBox stopOnFatigue = new CheckBox("Detenerse si cuatro jugadores están fatigados");
        stopAtMatch.setSelected(saved.stopAtMatch());
        stopOnOffer.setSelected(saved.stopOnOffer());
        stopOnFatigue.setSelected(saved.stopOnFatigue());
        ComboBox<String> assistance = new ComboBox<>();
        assistance.getItems().addAll("GUIDED", "STANDARD", "EXPERT");
        assistance.setValue(saved.assistanceLevel());
        ComboBox<String> difficulty = new ComboBox<>();
        difficulty.getItems().addAll("CASUAL", "NORMAL", "HARD", "LEGENDARY");
        difficulty.setValue(saved.difficulty());
        ComboBox<String> identity = new ComboBox<>();
        identity.getItems().addAll("GENERALIST", "TACTICIAN", "DEVELOPER", "MOTIVATOR");
        identity.setValue(saved.managerIdentity());
        Label savedFeedback = label("Estas preferencias son exclusivas de esta carrera.",
                "muted-label");
        Button savePreferences = button("GUARDAR PREFERENCIAS", "primary-button");
        savePreferences.setOnAction(event -> {
            repository.save(career.getId(), new CareerPreferencesRepository.Preferences(
                    stopAtMatch.isSelected(), stopOnOffer.isSelected(),
                    stopOnFatigue.isSelected(), assistance.getValue(), difficulty.getValue(),
                    identity.getValue()));
            savedFeedback.setText("Preferencias guardadas correctamente.");
            animateFeedback(savedFeedback, true);
        });
        progression.getChildren().addAll(stopAtMatch, stopOnOffer, stopOnFatigue,
                new FlowPane(10, 10, label("ASISTENCIA", "objective-title"), assistance),
                new FlowPane(10, 10, label("DIFICULTAD", "objective-title"), difficulty),
                new FlowPane(10, 10, label("IDENTIDAD", "objective-title"), identity),
                label("Guiado muestra referencias; estándar reduce ayudas; experto evita pistas de negociación.",
                        "muted-label"), savePreferences, savedFeedback);
        VBox diagnostics = panel("DIAGNÓSTICO");
        ListView<AppDiagnostics.Entry> errors = new ListView<>();
        errors.getStyleClass().add("data-list");
        errors.setPrefHeight(180);
        errors.setCellFactory(view -> new ListCell<>() {
            @Override protected void updateItem(AppDiagnostics.Entry entry, boolean empty) {
                super.updateItem(entry, empty);
                setText(empty || entry == null ? null : entry.time() + "  •  "
                        + entry.context() + "  •  " + entry.type() + "  •  " + entry.message());
            }
        });
        errors.getItems().addAll(AppDiagnostics.recent());
        errors.setPlaceholder(label("No se han detectado errores durante esta ejecución.",
                "success-feedback"));
        Button clearDiagnostics = button("LIMPIAR LISTA", "ghost-button");
        clearDiagnostics.setOnAction(event -> {
            AppDiagnostics.clearMemory();
            errors.getItems().clear();
        });
        diagnostics.getChildren().addAll(
                label("ARCHIVO DE REGISTRO", "objective-title"),
                label(AppDiagnostics.logPath().toString(), "comparison-label"), errors,
                label("La lista puede limpiarse, pero el archivo conserva el historial para investigar fallos.",
                        "muted-label"), clearDiagnostics);
        TabPane settingsTabs = new TabPane(); settingsTabs.getStyleClass().add("workspace-tabs");
        settingsTabs.getTabs().addAll(new Tab("PANTALLA", display),
                new Tab("EXPERIENCIA", progression), new Tab("DIAGNÓSTICO", diagnostics));
        settingsTabs.getTabs().forEach(tab -> tab.setClosable(false));
        VBox.setVgrow(settingsTabs, Priority.ALWAYS); content.getChildren().add(settingsTabs);
        showCareerShell(stage, content);
    }

    private void showOffice(Stage stage) {
        activeSection = "office";
        ClubFinance finance = new ClubFinanceRepository()
                .findByTeam(career.getControlledTeam().getId());
        VBox content = page("OFICINA DEL MÁNAGER",
                "Directiva, objetivos y control económico del club");
        ManagerEvaluationService.Evaluation evaluation = new ManagerEvaluationService()
                .evaluate(career);
        CareerInsightService insights = new CareerInsightService();
        VBox objectives = panel("EXPECTATIVAS DE LA DIRECTIVA");
        insights.objectives(career).forEach(objective -> {
            Label status = label(switch (objective.status()) {
                case ON_TRACK -> "EN OBJETIVO";
                case AT_RISK -> "EN RIESGO";
                case PENDING -> "PENDIENTE";
            }, switch (objective.status()) {
                case ON_TRACK -> "objective-good";
                case AT_RISK -> "objective-risk";
                case PENDING -> "objective-pending";
            });
            VBox description = new VBox(4, label(objective.title(), "objective-title"),
                    label(objective.detail(), "muted-label"));
            HBox row = new HBox(14, status, description);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("objective-row");
            objectives.getChildren().add(row);
        });

        VBox finances = panel("PRESUPUESTOS");
        if (finance == null) {
            finances.getChildren().add(label("No hay información financiera disponible.",
                    "muted-label"));
        } else {
            finances.getChildren().addAll(
                    officeBudgetRow("FICHAJES", finance.getTransferBudget(),
                            Math.max(finance.getTransferBudget(), finance.getBalance())),
                    officeBudgetRow("SALARIOS DISPONIBLES", finance.getAvailableWageBudget(),
                            finance.getWageBudget()),
                    officeBudgetRow("GASTO SALARIAL", finance.getCurrentWageSpend(),
                            finance.getWageBudget()));
        }
        HBox columns = new HBox(18, objectives, finances);
        HBox.setHgrow(objectives, Priority.ALWAYS);
        HBox.setHgrow(finances, Priority.ALWAYS);
        objectives.setMaxWidth(Double.MAX_VALUE);
        finances.setMaxWidth(Double.MAX_VALUE);

        VBox messages = panel("BANDEJA DE LA DIRECTIVA");
        if (evaluation.reasons().isEmpty()) messages.getChildren().add(
                label("La directiva considera estable la marcha del proyecto.", "body-label"));
        else evaluation.reasons().forEach(reason -> messages.getChildren().add(
                label("•  " + reason, evaluation.dismissalRisk()
                        ? "warning-feedback" : "body-label")));
        if (evaluation.dismissalRisk()) messages.getChildren().add(label(
                "AVISO: tu puesto corre peligro si no reviertes la situación.",
                "warning-feedback"));

        VBox dressingRoom = panel("VESTUARIO");
        java.util.List<SquadDynamicsService.Concern> concerns = new SquadDynamicsService()
                .concerns(career.getControlledTeam().getId());
        if (concerns.isEmpty()) dressingRoom.getChildren().add(
                label("No existen conflictos relevantes en la plantilla.", "body-label"));
        else concerns.stream().limit(6).forEach(concern -> dressingRoom.getChildren().add(
                label(concern.player().getFullName() + "  •  " + concern.role()
                        + "  •  Moral " + concern.morale() + "  •  " + concern.message(),
                        concern.morale() < 30 ? "warning-feedback" : "body-label")));
        YouthAcademyService academyService = new YouthAcademyService();
        YouthAcademyService.Scout scout = academyService.findScout(career.getId());
        VBox academy = panel("CANTERA Y OJEADORES");
        Label academyStatus = label(scout == null
                ? "No tienes un ojeador de cantera contratado."
                : scout.name() + "  •  Calidad " + scout.quality() + "/5  •  Desde "
                + scout.hiredDate(), scout == null ? "warning-feedback" : "success-feedback");
        TextField scoutName = new TextField(scout == null ? "Director de cantera" : scout.name());
        scoutName.setPromptText("Nombre del ojeador");
        ComboBox<Integer> scoutQuality = new ComboBox<>();
        scoutQuality.getItems().addAll(1, 2, 3, 4, 5);
        scoutQuality.setValue(scout == null ? 3 : scout.quality());
        Label scoutCost = label("", "comparison-label");
        Runnable refreshScoutCost = () -> scoutCost.setText(String.format(
                "Coste de contratación: €%.2fM",
                YouthAcademyService.hiringCost(scoutQuality.getValue()) / 1_000_000));
        scoutQuality.setOnAction(event -> refreshScoutCost.run());
        refreshScoutCost.run();
        Button hireScout = button(scout == null ? "CONTRATAR OJEADOR" : "CAMBIAR OJEADOR",
                "secondary-button");
        hireScout.setOnAction(event -> {
            try {
                academyService.hireScout(career, scoutName.getText(), scoutQuality.getValue());
                showOffice(stage);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                showMessage("NO SE PUDO CONTRATAR", exception.getMessage());
            }
        });
        ListView<YouthAcademyService.Prospect> prospects = new ListView<>();
        prospects.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(YouthAcademyService.Prospect item,
                    boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.position() + "  •  "
                        + item.fullName() + "  •  GRL " + item.overall()
                        + "  •  POT " + item.potential() + "  •  "
                        + item.birthDate().getYear());
            }
        });
        prospects.getItems().addAll(academyService.findCandidates(career.getId()));
        prospects.setPrefHeight(190);
        Button requestReport = button("SOLICITAR INFORME", "primary-button");
        requestReport.setDisable(scout == null);
        requestReport.setOnAction(event -> {
            try {
                academyService.generateReport(career);
                showOffice(stage);
            } catch (IllegalStateException exception) {
                showMessage("INFORME NO DISPONIBLE", exception.getMessage());
            }
        });
        Button promote = button("PROMOCIONAR AL PRIMER EQUIPO", "secondary-button");
        promote.setDisable(true);
        prospects.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> promote.setDisable(selected == null));
        promote.setOnAction(event -> {
            YouthAcademyService.Prospect selected = prospects.getSelectionModel()
                    .getSelectedItem();
            if (selected == null) return;
            Player promoted = academyService.promote(career, selected.id());
            showMessage("JUVENIL PROMOCIONADO", promoted.getFullName()
                    + " ya forma parte de la plantilla profesional.", () -> showOffice(stage));
        });
        academy.getChildren().addAll(academyStatus,
                new FlowPane(10, 10, scoutName, scoutQuality, hireScout), scoutCost,
                label("INFORMES DE JUVENILES", "objective-title"), prospects,
                new FlowPane(10, 10, requestReport, promote));
        StaffService staffService = new StaffService();
        java.util.Map<StaffService.Role, StaffService.Staff> employees =
                staffService.findAll(career.getId());
        HBox staffCards = new HBox(12);
        for (StaffService.Role role : StaffService.Role.values()) {
            VBox card = staffCard(stage, staffService, role, employees.get(role));
            HBox.setHgrow(card, Priority.ALWAYS);
            card.setMaxWidth(Double.MAX_VALUE);
            staffCards.getChildren().add(card);
        }
        VBox technicalStaff = panel("CUERPO TÉCNICO");
        technicalStaff.getChildren().addAll(label(
                "El preparador mejora sesiones, el fisio acelera recuperaciones y el analista amplía la previa.",
                "muted-label"), staffCards);
        VBox boardOverview = new VBox(16, columns, messages);
        TabPane officeTabs = new TabPane(); officeTabs.getStyleClass().add("workspace-tabs");
        officeTabs.getTabs().addAll(new Tab("DIRECTIVA", boardOverview),
                new Tab("VESTUARIO", dressingRoom), new Tab("CANTERA", academy),
                new Tab("CUERPO TÉCNICO", technicalStaff));
        officeTabs.getTabs().forEach(tab -> tab.setClosable(false));
        VBox executive = new footballcareer.ui.OfficeExecutiveView().build(
                new footballcareer.ui.OfficeExecutiveView.Model(evaluation, finance,
                        leaguePositionSummary(), insights.objectives(career),
                        insights.notifications(career).size()),
                new footballcareer.ui.OfficeExecutiveView.Actions(
                        () -> { selectedMarketTab = 0; showMarket(stage); },
                        () -> showSquad(stage), () -> showTraining(stage)));
        VBox.setVgrow(officeTabs, Priority.ALWAYS);
        content.getChildren().addAll(executive, officeTabs);
        ScrollPane officeScroll = new ScrollPane(content);
        officeScroll.setFitToWidth(true);
        officeScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        officeScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        officeScroll.getStyleClass().add("office-scroll");
        showCareerShell(stage, officeScroll);
    }

    private VBox staffCard(Stage stage, StaffService service, StaffService.Role role,
            StaffService.Staff current) {
        String title = switch (role) {
            case COACH -> "PREPARADOR";
            case PHYSIO -> "FISIOTERAPEUTA";
            case ANALYST -> "ANALISTA";
        };
        TextField name = new TextField(current == null ? "" : current.name());
        name.setPromptText("Nombre");
        ComboBox<Integer> level = new ComboBox<>();
        level.getItems().addAll(1, 2, 3, 4, 5);
        level.setValue(current == null ? 2 : current.level());
        Label cost = label("", "comparison-label");
        Runnable refresh = () -> cost.setText(String.format("Nivel %d  •  Coste €%.2fM",
                level.getValue(), StaffService.hiringCost(level.getValue()) / 1_000_000));
        level.setOnAction(event -> refresh.run());
        refresh.run();
        Button hire = button(current == null ? "CONTRATAR" : "SUSTITUIR", "secondary-button");
        hire.setOnAction(event -> {
            try {
                service.hire(career, role, name.getText(), level.getValue());
                showOffice(stage);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                showMessage("CONTRATACIÓN FALLIDA", exception.getMessage());
            }
        });
        VBox card = new VBox(8, label(title, "objective-title"),
                label(current == null ? "Vacante" : current.name() + "  •  Nivel "
                        + current.level(), current == null ? "warning-feedback" : "success-feedback"),
                name, level, cost, hire);
        card.getStyleClass().add("training-card");
        return card;
    }

    private HBox officeBudgetRow(String title, double value, double maximum) {
        double safeMaximum = Math.max(1, maximum);
        ProgressBar bar = new ProgressBar(Math.max(0, Math.min(1, value / safeMaximum)));
        bar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(bar, Priority.ALWAYS);
        Label amount = label(String.format("€%.1fM", value / 1_000_000), "budget-value");
        HBox row = new HBox(14, label(title, "budget-title"), bar, amount);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("budget-row");
        return row;
    }

    private void addAttribute(GridPane grid, int row, String leftName,
            int leftValue, String rightName, int rightValue) {
        grid.add(label(leftName, "muted-label"), 0, row);
        grid.add(label(String.valueOf(leftValue), "attribute-value"), 1, row);
        grid.add(label(rightName, "muted-label"), 2, row);
        grid.add(label(String.valueOf(rightValue), "attribute-value"), 3, row);
    }

    private void addStatRow(GridPane grid, int row, String name,
            Object homeValue, Object awayValue) {
        Label home = label(String.valueOf(homeValue), "stat-number");
        Label statName = label(name, "muted-label");
        Label away = label(String.valueOf(awayValue), "stat-number");
        grid.add(home, 0, row);
        grid.add(statName, 1, row);
        grid.add(away, 2, row);
    }

    private java.util.List<Competition> currentCompetitions() {
        return new CompetitionTeamRepository()
                .findCompetitionsByTeam(career.getControlledTeam().getId()).stream()
                .filter(competition -> competition.getSeason().getId()
                        == career.getCurrentSeason().getId()).toList();
    }

    private Match findNextMatch() {
        return matches.findNextForTeam(career.getControlledTeam().getId(),
                career.getCurrentSeason().getId(), career.getCurrentDate());
    }

    private Match findControlledMatchToday() {
        return matches.findByDate(career.getCurrentDate()).stream()
                .filter(match -> match.getDate().equals(career.getCurrentDate()))
                .filter(match -> match.getHomeTeam().getId() == career.getControlledTeam().getId()
                        || match.getAwayTeam().getId() == career.getControlledTeam().getId())
                .findFirst().orElse(null);
    }

    private void showMatchPreview(Stage stage, Match match) {
        long controlledTeamId = career.getControlledTeam().getId();
        PlayerStateRepository states = new PlayerStateRepository();
        java.util.Map<Long, PlayerState> allStates = states.findAll();
        LineupService lineups = new LineupService(new PlayerRepository(), states);
        MatchLineup home = lineups.selectMatchLineup(match.getId(), match.getHomeTeam().getId());
        MatchLineup away = lineups.selectMatchLineup(match.getId(), match.getAwayTeam().getId());
        MatchTacticsRepository tactics = new MatchTacticsRepository();
        String homeFormation = tactics.findFormation(match.getId(), match.getHomeTeam().getId());
        String awayFormation = tactics.findFormation(match.getId(), match.getAwayTeam().getId());
        VBox content = page("PREVIA DEL PARTIDO", match.getCompetition().getName() + "  •  "
                + match.getDate() + "  •  " + match.getHomeTeam().getStadiumName());
        int analystLevel = new StaffService().level(career.getId(), StaffService.Role.ANALYST);
        VBox homePanel = previewTeam(match.getHomeTeam(), home, homeFormation, allStates,
                match.getHomeTeam().getId() == controlledTeamId);
        VBox awayPanel = previewTeam(match.getAwayTeam(), away, awayFormation, allStates,
                match.getAwayTeam().getId() == controlledTeamId);
        VBox versus = new VBox(8, label("VS", "versus"),
                label("Todo preparado", "match-countdown"));
        versus.setAlignment(Pos.CENTER);
        HBox matchup = new HBox(18, homePanel, versus, awayPanel);
        matchup.setAlignment(Pos.CENTER);
        HBox.setHgrow(homePanel, Priority.ALWAYS);
        HBox.setHgrow(awayPanel, Priority.ALWAYS);
        Button lineup = button("REVISAR MI ALINEACIÓN", "secondary-button");
        lineup.setOnAction(event -> showLineup(stage));
        Button start = button("SIMULAR PARTIDO", "primary-button");
        start.setOnAction(event -> showLiveMatch(stage, match));
        MatchLineup rivalLineup = match.getHomeTeam().getId() == controlledTeamId ? away : home;
        String rivalFormation = match.getHomeTeam().getId() == controlledTeamId
                ? awayFormation : homeFormation;
        if (analystLevel > 0) content.getChildren().add(label(
                "INFORME DEL ANALISTA  •  Nivel " + analystLevel
                        + "  •  Rival observado: formación " + rivalFormation
                        + ", once medio " + String.format("%.1f", rivalLineup.getStarters().stream()
                        .mapToInt(Player::getOverall).average().orElse(0)), "success-feedback"));
        content.getChildren().addAll(matchup, new FlowPane(12, 12, lineup, start));
        showCareerShell(stage, new ScrollPane(content));
    }

    private VBox previewTeam(Team team, MatchLineup lineup, String formation,
            java.util.Map<Long, PlayerState> states, boolean controlled) {
        double average = lineup.getStarters().stream().mapToInt(Player::getOverall)
                .average().orElse(0);
        double fitness = lineup.getStarters().stream()
                .map(player -> states.get(player.getId()))
                .filter(java.util.Objects::nonNull).mapToInt(PlayerState::getFitness)
                .average().orElse(0);
        VBox players = new VBox(5);
        lineup.getStarters().forEach(player -> players.getChildren().add(label(
                player.getPosition() + "  •  " + player.getFullName() + "  •  GRL "
                        + player.getOverall(), "preview-player")));
        VBox panel = panel(controlled ? "TU EQUIPO" : "RIVAL");
        panel.getStyleClass().add(controlled ? "preview-user-team" : "preview-team");
        panel.getChildren().addAll(footballcareer.ui.TeamCrestView.create(team, 88),
                label(team.getName(), "match-team-name"),
                label(formation + "  •  GRL medio " + String.format("%.1f", average)
                        + "  •  Fitness " + String.format("%.0f", fitness), "match-metadata"),
                players);
        return panel;
    }

    private void showLiveMatch(Stage stage, Match pendingMatch) {
        IncrementalLiveMatchService.Session liveSession =
                new IncrementalLiveMatchService().start(pendingMatch.getId());
        Match match = liveSession.match();
        VBox content = page("PARTIDO EN DIRECTO", match.getDate().toString());
        Label minute = label("0'", "live-minute");
        Label score = label("0  —  0", "score-number");
        VBox eventFeed = panel("ACCIONES");
        Label possession = label("50%  POSESIÓN  50%", "live-stat-main");
        Label shots = label("0  TIROS  0", "live-stat-row");
        Label onTarget = label("0  A PUERTA  0", "live-stat-row");
        Label cards = label("0  TARJETAS  0", "live-stat-row");
        VBox liveStats = panel("ESTADÍSTICAS EN VIVO");
        liveStats.setAlignment(Pos.CENTER);
        liveStats.getChildren().addAll(possession, shots, onTarget, cards);
        Button reportButton = button("VER INFORME COMPLETO", "primary-button");
        reportButton.setDisable(true);
        reportButton.setOnAction(event -> showMatchReport(stage, match));
        Button pause = button("PAUSAR", "secondary-button");
        Button finish = button("IR AL FINAL", "ghost-button");
        Button substitution = button("HACER CAMBIO", "secondary-button");
        ComboBox<String> speed = new ComboBox<>();
        speed.getItems().addAll("0.5x", "1x", "2x", "4x");
        speed.setValue("1x");
        VBox homeIdentity = new VBox(8,
                footballcareer.ui.TeamCrestView.create(match.getHomeTeam(), 92),
                label(match.getHomeTeam().getName(), "live-team-full-name"));
        VBox awayIdentity = new VBox(8,
                footballcareer.ui.TeamCrestView.create(match.getAwayTeam(), 92),
                label(match.getAwayTeam().getName(), "live-team-full-name"));
        homeIdentity.setAlignment(Pos.CENTER); awayIdentity.setAlignment(Pos.CENTER);
        VBox scoreCenter = new VBox(3, minute, score,
                label(match.getCompetition().getName(), "live-competition-name"));
        scoreCenter.setAlignment(Pos.CENTER);
        HBox scoreboard = new HBox(36, homeIdentity, scoreCenter, awayIdentity);
        scoreboard.setAlignment(Pos.CENTER);
        scoreboard.getStyleClass().add("live-scoreboard");
        Label goalBanner = label("", "live-goal-banner");
        goalBanner.setVisible(false); goalBanner.setManaged(false);

        int[] currentMinute = {0};
        int[] homeGoals = {0};
        int[] awayGoals = {0};
        int[] substitutionsMade = {0};
        long controlledTeamId = career.getControlledTeam().getId();
        boolean controlledAtHome = match.getHomeTeam().getId() == controlledTeamId;
        LiveMatchNarrator narrator = new LiveMatchNarrator();
        LiveTacticalMomentumService momentumService = new LiveTacticalMomentumService();
        MatchTacticsRepository.TacticalSetup initialTactics =
                liveSession.tactics(controlledTeamId);
        ComboBox<String> mentality = new ComboBox<>(); mentality.getItems().addAll(
                "DEFENSIVE", "BALANCED", "ATTACKING");
        mentality.setValue(initialTactics.mentality());
        ComboBox<String> pressing = new ComboBox<>(); pressing.getItems().addAll(
                "LOW", "MEDIUM", "HIGH");
        pressing.setValue(initialTactics.pressing());
        ComboBox<String> tempo = new ComboBox<>();
        tempo.getItems().addAll("SLOW", "NORMAL", "FAST");
        tempo.setValue(initialTactics.tempo());
        Label tacticalReading = label("", "tactical-reading");
        ProgressBar momentumBar = new ProgressBar(); momentumBar.setMaxWidth(Double.MAX_VALUE);
        Button applyTactics = button("APLICAR INSTRUCCIONES", "secondary-button");
        VBox tacticalCenter = panel("CENTRO TÁCTICO"); tacticalCenter.getStyleClass().add("live-tactical-center");
        tacticalCenter.getChildren().addAll(new FlowPane(10, 10, mentality, pressing,
                tempo, applyTactics), momentumBar, tacticalReading);
        Runnable refreshTacticalReading = () -> {
            int controlledGoals = controlledAtHome ? homeGoals[0] : awayGoals[0];
            int rivalGoals = controlledAtHome ? awayGoals[0] : homeGoals[0];
            var setup = new MatchTacticsRepository.TacticalSetup(initialTactics.formation(),
                    mentality.getValue(), pressing.getValue(), tempo.getValue());
            var assessment = momentumService.assess(setup, currentMinute[0],
                    controlledGoals - rivalGoals);
            momentumBar.setProgress((assessment.momentum() + 50) / 100.0);
            tacticalReading.setText(assessment.label() + "  •  " + assessment.risk()
                    + "  •  Impulso " + (assessment.momentum() >= 0 ? "+" : "")
                    + assessment.momentum());
        };
        refreshTacticalReading.run();
        applyTactics.setOnAction(event -> {
            var setup = new MatchTacticsRepository.TacticalSetup(initialTactics.formation(),
                    mentality.getValue(), pressing.getValue(), tempo.getValue());
            liveSession.updateTactics(controlledTeamId, setup);
            refreshTacticalReading.run();
            Label instruction = label(Math.max(1, currentMinute[0])
                    + "'  INDICACIÓN  •  " + mentality.getValue() + " / "
                    + pressing.getValue() + " / " + tempo.getValue(),
                    "live-event-commentary");
            HBox row = new HBox(instruction);
            row.getStyleClass().addAll("live-event-row", "live-event-tactics");
            eventFeed.getChildren().add(row);
        });
        MatchLineup controlledLineup = liveSession.lineup(controlledTeamId);
        java.util.List<Player> liveStarters = controlledLineup == null
                ? new java.util.ArrayList<>()
                : new java.util.ArrayList<>(controlledLineup.getStarters());
        java.util.List<Player> liveSubstitutes = controlledLineup == null
                ? new java.util.ArrayList<>()
                : new java.util.ArrayList<>(controlledLineup.getSubstitutes());
        java.util.List<MatchEvent> liveEvents = new java.util.ArrayList<>();
        java.util.Set<Long> revealed = new java.util.HashSet<>();
        java.util.function.IntConsumer revealUntil = targetMinute -> {
            liveEvents.addAll(liveSession.advanceTo(targetMinute));
            liveEvents.stream()
                .filter(matchEvent -> matchEvent.getMinute() <= targetMinute)
                .filter(matchEvent -> revealed.add(matchEvent.getId()))
                .forEach(matchEvent -> {
                    Player player = matchEvent.getPlayer();
                    Player secondary = matchEvent.getSecondaryPlayer();
                    Label badge = label(matchEvent.getMinute() + "'",
                            "live-event-minute");
                    boolean homeEvent = matchEvent.getTeam().getId()
                            == match.getHomeTeam().getId();
                    Label commentary = label(narrator.describe(matchEvent, player, secondary,
                                    homeGoals[0], awayGoals[0], homeEvent),
                            "live-event-commentary");
                    HBox eventRow = new HBox(12, badge, commentary);
                    eventRow.setAlignment(Pos.CENTER_LEFT);
                    eventRow.getStyleClass().add("live-event-row");
                    eventRow.getStyleClass().add("live-event-"
                            + matchEvent.getType().name().toLowerCase());
                    eventFeed.getChildren().add(eventRow);
                    if (matchEvent.getType()
                            == footballcareer.model.enums.MatchEventType.GOAL) {
                        if (matchEvent.getTeam().getId() == match.getHomeTeam().getId())
                            homeGoals[0]++;
                        else awayGoals[0]++;
                        score.setText(homeGoals[0] + "  —  " + awayGoals[0]);
                        String scorer = player == null ? "GOL" : player.getFullName();
                        goalBanner.setText("¡GOL!  " + matchEvent.getTeam().getName()
                                + "  ·  " + scorer + "  " + matchEvent.getMinute() + "'");
                        goalBanner.setManaged(true); goalBanner.setVisible(true);
                        goalBanner.setOpacity(0); goalBanner.setTranslateY(-18);
                        javafx.animation.FadeTransition appear = new javafx.animation.FadeTransition(
                                javafx.util.Duration.millis(180), goalBanner);
                        appear.setFromValue(0); appear.setToValue(1);
                        javafx.animation.TranslateTransition enter =
                                new javafx.animation.TranslateTransition(
                                        javafx.util.Duration.millis(220), goalBanner);
                        enter.setFromY(-18); enter.setToY(0);
                        javafx.animation.PauseTransition hold = new javafx.animation.PauseTransition(
                                javafx.util.Duration.seconds(2.2));
                        javafx.animation.FadeTransition leave = new javafx.animation.FadeTransition(
                                javafx.util.Duration.millis(320), goalBanner);
                        leave.setFromValue(1); leave.setToValue(0);
                        javafx.animation.SequentialTransition notice =
                                new javafx.animation.SequentialTransition(
                                        new javafx.animation.ParallelTransition(appear, enter),
                                        hold, leave);
                        notice.setOnFinished(done -> {
                            goalBanner.setVisible(false); goalBanner.setManaged(false);
                        });
                        notice.play();
                    }
                });
        };
        java.util.function.IntConsumer updateLiveStats = targetMinute -> {
            int homePossession = liveSession.homeStats().getPossession();
            possession.setText(homePossession + "%  POSESIÓN  "
                    + (100 - homePossession) + "%");
            shots.setText(liveSession.homeStats().getShots()
                    + "  TIROS  " + liveSession.awayStats().getShots());
            onTarget.setText(liveSession.homeStats().getShotsOnTarget()
                    + "  A PUERTA  "
                    + liveSession.awayStats().getShotsOnTarget());
            int homeCards = liveSession.homeStats().getYellowCards()
                    + liveSession.homeStats().getRedCards();
            int awayCards = liveSession.awayStats().getYellowCards()
                    + liveSession.awayStats().getRedCards();
            cards.setText(homeCards + "  TARJETAS  " + awayCards);
        };
        Runnable showFinal = () -> {
            revealUntil.accept(90);
            liveSession.finish();
            updateLiveStats.accept(90);
            currentMinute[0] = 90;
            minute.setText("FINAL");
            score.setText(match.getHomeGoals() + "  —  " + match.getAwayGoals());
            pause.setDisable(true);
            finish.setDisable(true);
            speed.setDisable(true);
            substitution.setDisable(true);
            applyTactics.setDisable(true);
            mentality.setDisable(true);
            pressing.setDisable(true);
            tempo.setDisable(true);
            reportButton.setDisable(false);
        };
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(110), event -> {
                    currentMinute[0] = Math.min(90, currentMinute[0] + 2);
                    minute.setText(currentMinute[0] + "'");
                    revealUntil.accept(currentMinute[0]);
                    updateLiveStats.accept(currentMinute[0]);
                    refreshTacticalReading.run();
                }));
        timeline.setCycleCount(45);
        timeline.setOnFinished(event -> showFinal.run());
        substitution.setDisable(controlledLineup == null || liveSubstitutes.isEmpty());
        substitution.setOnAction(event -> {
            timeline.pause();
            pause.setText("REANUDAR");
            showSubstitutionOverlay(match, liveStarters, liveSubstitutes, currentMinute[0],
                    substitutionsMade[0], completed -> {
                        substitutionsMade[0]++;
                        liveSession.recordSubstitution(controlledTeamId, liveStarters,
                                liveSubstitutes, completed);
                        liveEvents.add(completed);
                        revealUntil.accept(Math.max(1, currentMinute[0]));
                        if (substitutionsMade[0] >= 3
                                || liveSubstitutes.isEmpty()) {
                            substitution.setDisable(true);
                        }
                    });
        });
        pause.setOnAction(event -> {
            if (timeline.getStatus() == javafx.animation.Animation.Status.RUNNING) {
                timeline.pause();
                pause.setText("REANUDAR");
            } else {
                timeline.play();
                pause.setText("PAUSAR");
            }
        });
        speed.setOnAction(event -> timeline.setRate(Double.parseDouble(
                speed.getValue().replace("x", ""))));
        finish.setOnAction(event -> { timeline.stop(); showFinal.run(); });
        FlowPane matchControls = new FlowPane(12, 12,
                label("VELOCIDAD", "control-caption"), speed, pause, substitution, finish);
        content.getChildren().add(new footballcareer.ui.LiveMatchLayout().build(scoreboard,
                goalBanner, matchControls, liveStats, tacticalCenter, eventFeed, reportButton));
        showCareerShell(stage, content);
        activeAnimation = timeline;
        timeline.play();
    }

    private void showSubstitutionOverlay(Match match, java.util.List<Player> starters,
            java.util.List<Player> substitutes, int currentMinute,
            int substitutionsMade, java.util.function.Consumer<MatchEvent> completed) {
        if (substitutionsMade >= 3) {
            showMessage("CAMBIOS AGOTADOS", "Ya has realizado los tres cambios permitidos.");
            return;
        }
        ComboBox<Player> outgoing = new ComboBox<>();
        ComboBox<Player> incoming = new ComboBox<>();
        outgoing.getItems().addAll(starters);
        incoming.getItems().addAll(substitutes);
        outgoing.setConverter(playerStringConverter());
        incoming.setConverter(playerStringConverter());
        if (!outgoing.getItems().isEmpty()) outgoing.setValue(outgoing.getItems().getFirst());
        if (!incoming.getItems().isEmpty()) incoming.setValue(incoming.getItems().getFirst());
        Label feedback = label("El cambio quedará registrado en el informe del partido.",
                "muted-label");
        Button confirm = button("CONFIRMAR CAMBIO", "primary-button");
        Button cancel = button("CANCELAR", "ghost-button");
        VBox dialog = new VBox(14, label("SUSTITUCIÓN", "form-title"),
                label("Minuto " + Math.max(1, currentMinute) + "  •  Cambio "
                        + (substitutionsMade + 1) + " de 3", "comparison-label"),
                label("SALE", "objective-title"), outgoing,
                label("ENTRA", "objective-title"), incoming, feedback,
                new FlowPane(10, 10, confirm, cancel));
        dialog.getStyleClass().add("in-app-dialog");
        dialog.setPrefWidth(500);
        Runnable close = showOverlay(dialog);
        cancel.setOnAction(event -> close.run());
        confirm.setOnAction(event -> {
            Player leaving = outgoing.getValue();
            Player entering = incoming.getValue();
            if (leaving == null || entering == null) {
                feedback.setText("Selecciona el jugador que sale y el que entra.");
                animateFeedback(feedback, false);
                return;
            }
            int slot = starters.indexOf(leaving);
            starters.set(slot, entering);
            substitutes.remove(entering);
            substitutes.add(leaving);
            MatchEvent change = new MatchEvent();
            change.setMatch(match);
            change.setTeam(career.getControlledTeam());
            change.setPlayer(leaving);
            change.setSecondaryPlayer(entering);
            change.setMinute(Math.max(1, currentMinute));
            change.setType(footballcareer.model.enums.MatchEventType.SUBSTITUTION);
            close.run();
            completed.accept(change);
        });
    }
    private boolean restoreRetainedScreen(Stage stage, String screen) { Node content =
            retainedScreens.take(screen, career.getCurrentDate()); if (content == null) return false;
        showCareerShell(stage, content); return true;
    }
    private VBox page(String title, String subtitle) {
        VBox heading = new VBox(5, label(title, "page-title"), label(subtitle, "page-subtitle"));
        heading.getStyleClass().add("page-header");
        VBox page = new VBox(20, heading);
        page.getStyleClass().add("page");
        return page;
    }

    private void showLoading(Stage stage, String title, String detail) {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setMaxSize(64, 64);
        VBox card = new VBox(18, indicator, label(title, "form-title"),
                label(detail, "page-subtitle"));
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("loading-card");
        setScene(stage, scrollableCentered(card, "centered-root"));
    }

    private <T> void runAsync(Stage stage, java.util.concurrent.Callable<T> operation,
            java.util.function.Consumer<T> onSuccess) {
        javafx.concurrent.Task<T> task = new javafx.concurrent.Task<>() {
            @Override protected T call() throws Exception { return operation.call(); }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> {
            Throwable failure = task.getException();
            AppDiagnostics.record(failure, "background-operation");
            VBox card = formCard("NO SE PUDO COMPLETAR LA OPERACIÓN",
                    failure == null || failure.getMessage() == null
                            ? "Ha ocurrido un error inesperado."
                            : failure.getMessage());
            Button menu = wideButton("VOLVER AL MENÚ", "primary-button");
            if (careerService == null) {
                menu.setText("REINTENTAR INICIALIZACIÓN");
                menu.setOnAction(action -> initializeApplication(stage));
            } else menu.setOnAction(action -> showMainMenu(stage));
            card.getChildren().add(menu);
            setScene(stage, scrollableCentered(card, "centered-root"));
        });
        Thread worker = new Thread(task, "football-career-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private VBox panel(String title) {
        VBox panel = new VBox(12, label(title, "panel-title"));
        panel.getStyleClass().add("panel");
        return panel;
    }

    private VBox statCard(String title, String value) {
        VBox card = new VBox(8, label(title, "stat-title"), label(value, "stat-value"));
        card.getStyleClass().add("stat-card");
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private VBox formCard(String title, String subtitle) {
        VBox card = new VBox(18, label(title, "form-title"), label(subtitle, "page-subtitle"));
        card.getStyleClass().add("form-card");
        card.setMinWidth(0);
        card.setPrefWidth(520);
        card.setMaxWidth(520);
        return card;
    }

    private ScrollPane scrollableCentered(Node node, String rootStyle) {
        return responsiveContainer.centered(node, rootStyle);
    }

    private Label label(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        label.setWrapText(true);
        return label;
    }

    private Button button(String text, String style) {
        Button button = new Button(text);
        button.getStyleClass().add(style);
        button.setTooltip(new Tooltip(text));
        return button;
    }

    private Button wideButton(String text, String style) {
        Button button = button(text, style);
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private ListCell<Competition> competitionCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Competition competition, boolean empty) {
                super.updateItem(competition, empty);
                setText(empty || competition == null ? null : competition.getName());
            }
        };
    }

    private ListView<Player> playerList() {
        ListView<Player> list = new ListView<>();
        list.setCellFactory(view -> new ListCell<>() {
            @Override protected void updateItem(Player player, boolean empty) {
                super.updateItem(player, empty);
                setText(empty || player == null ? null : player.getPosition() + "  •  "
                        + player.getFullName() + "  •  GRL " + player.getOverall()
                        + "  •  €" + String.format("%.1fM",
                        player.getMarketValue() / 1_000_000));
            }
        });
        return list;
    }

    private ListView<Player> lineupAvailableList(java.util.Set<Long> benchIds) {
        java.util.Map<Long, PlayerState> states = new PlayerStateRepository().findAll();
        ListView<Player> list = new ListView<>();
        list.setCellFactory(view -> new ListCell<>() {
            @Override protected void updateItem(Player player, boolean empty) {
                super.updateItem(player, empty);
                getStyleClass().removeAll("lineup-bench-cell", "lineup-reserve-cell");
                if (empty || player == null) {
                    setText(null);
                    return;
                }
                PlayerState state = states.get(player.getId());
                String role = benchIds.contains(player.getId()) ? "BANQUILLO" : "RESERVA";
                setText(role + "  •  " + player.getPosition() + "  •  "
                        + player.getFullName() + "  •  GRL " + player.getOverall()
                        + (state == null ? "" : "  •  FORMA " + state.getForm()
                        + "  •  FIT " + state.getFitness()));
                getStyleClass().add(benchIds.contains(player.getId())
                        ? "lineup-bench-cell" : "lineup-reserve-cell");
            }
        });
        return list;
    }

    private ListView<Player> marketPlayerList(java.util.Map<Long, Double> marketPrices,
            java.util.Map<Long, Team> marketTeams, java.util.Set<Long> shortlistIds) {
        ListView<Player> list = new ListView<>();
        list.setCellFactory(view -> new footballcareer.ui.MarketPlayerCell(marketPrices,
                marketTeams, shortlistIds, career.getCurrentDate()));
        return list;
    }

    private ListView<Player> ownMarketPlayerList(java.util.Map<Long, Double> askingPrices) {
        ListView<Player> list = new ListView<>();
        list.setCellFactory(view -> new ListCell<>() {
            @Override protected void updateItem(Player player, boolean empty) {
                super.updateItem(player, empty);
                if (empty || player == null) setText(null);
                else {
                    Double price = askingPrices.get(player.getId());
                    String market = price == null ? "NO LISTADO"
                            : "EN VENTA  €" + String.format("%.1fM", price / 1_000_000);
                    setText(player.getPosition() + "  •  " + player.getFullName()
                            + "  •  GRL " + player.getOverall()
                            + "  •  Valor €" + String.format("%.1fM",
                            player.getMarketValue() / 1_000_000) + "  •  " + market);
                }
            }
        });
        return list;
    }

    private ListView<TransferOffer> incomingOfferList(
            java.util.Map<Long, Player> playersById,
            java.util.Map<Long, Team> teamsById,
            java.util.Map<Long, Double> askingPrices) {
        ListView<TransferOffer> list = new ListView<>();
        list.setCellFactory(view -> new ListCell<>() {
            @Override protected void updateItem(TransferOffer offer, boolean empty) {
                super.updateItem(offer, empty);
                if (empty || offer == null) setText(null);
                else {
                    Player player = playersById.get(offer.getPlayer().getId());
                    Team buyer = teamsById.get(offer.getBuyingTeam().getId());
                    Double asking = askingPrices.get(player.getId());
                    setText(player.getFullName() + "  •  " + buyer.getName()
                            + "  •  €" + String.format("%.1fM", offer.getAmount() / 1_000_000)
                            + (asking == null ? "" : " / Pide €"
                            + String.format("%.1fM", asking / 1_000_000))
                            + "  •  " + offer.getOfferDate()
                            + "  •  límite " + offer.getResponseDeadline());
                }
            }
        });
        return list;
    }

    private void setScene(Stage stage, javafx.scene.Parent root) {
        rememberCurrentScroll();
        if (activeAnimation != null) {
            activeAnimation.stop();
            activeAnimation = null;
        }
        if (appScene == null) {
            appScene = new Scene(root, 1180, 760);
            footballcareer.ui.UiTheme.install(appScene);
            appScene.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.F11) {
                    stage.setFullScreen(!stage.isFullScreen());
                }
            });
            appScene.widthProperty().addListener((observable, previous, current) ->
                    applyViewportMode());
            appScene.heightProperty().addListener((observable, previous, current) ->
                    applyViewportMode());
            stage.setScene(appScene);
        } else {
            appScene.setRoot(root);
        }
        applyViewportMode();
    }

    private void rememberCurrentScroll() {
        if (appScene == null || appScene.getRoot() == null) return;
        Node node = appScene.getRoot().lookup("#career-content-scroll");
        if (node instanceof ScrollPane scroll && scroll.getUserData() instanceof String screen) {
            navigationState.remember(screen, scroll.getHvalue(), scroll.getVvalue());
        }
    }

    private void applyViewportMode() {
        if (appScene == null || appScene.getRoot() == null) return;
        boolean compact = ViewportPolicy.compact(appScene.getWidth(), appScene.getHeight());
        appScene.getRoot().getStyleClass().remove("compact-viewport");
        if (compact) appScene.getRoot().getStyleClass().add("compact-viewport");
    }

    public static void main(String[] args) { launch(); }
}
