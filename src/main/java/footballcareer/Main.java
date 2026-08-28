package footballcareer;

import footballcareer.database.*;
import footballcareer.model.*;
import footballcareer.service.*;
import javafx.application.Application;
import javafx.geometry.Insets;
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
    private javafx.animation.Animation activeAnimation;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Football Career");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setMaximized(true);
        stage.setFullScreenExitHint("");
        showLoading(stage, "PREPARANDO EL MUNDO", "Cargando clubes, jugadores y competiciones...");
        stage.show();
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
        VBox brand = new VBox(8, label("CAREER MANAGEMENT SIMULATOR", "eyebrow"),
                label("FOOTBALL\nCAREER", "hero-title"),
                label("Construye una dinastía. Gestiona tu plantilla.\nDomina cada temporada.",
                        "hero-subtitle"));
        brand.setAlignment(Pos.CENTER_LEFT);
        VBox actions = new VBox(14);
        actions.setMaxWidth(330);
        Button newCareer = wideButton("NUEVA CARRERA", "primary-button");
        newCareer.setOnAction(event -> showNewCareer(stage));
        Button loadCareer = wideButton("CARGAR CARRERA", "secondary-button");
        loadCareer.setDisable(careers.findAll().isEmpty());
        loadCareer.setOnAction(event -> showLoadCareer(stage));
        actions.getChildren().addAll(newCareer, loadCareer,
                label("ALPHA 0.8  •  JAVA 21", "muted-label"));
        VBox menu = new VBox(28, brand, actions);
        menu.setAlignment(Pos.CENTER_LEFT);
        menu.setMaxWidth(760);
        menu.getStyleClass().add("menu-content");
        setScene(stage, scrollableCentered(menu, "menu-root"));
    }

    private void showNewCareer(Stage stage) {
        VBox card = formCard("CREAR NUEVA CARRERA",
                "Elige tu identidad y el club con el que empezarás.");
        TextField manager = new TextField();
        manager.setPromptText("Nombre del entrenador");
        ComboBox<Team> club = new ComboBox<>();
        club.getItems().addAll(careerService.getAvailableTeams());
        club.setPromptText("Selecciona un club");
        club.setCellFactory(list -> teamCell());
        club.setButtonCell(teamCell());
        club.setMaxWidth(Double.MAX_VALUE);
        Label error = label("", "error-label");
        Button create = wideButton("COMENZAR CARRERA", "primary-button");
        create.setOnAction(event -> {
            try {
                if (manager.getText() == null || manager.getText().isBlank()) {
                    throw new IllegalArgumentException("Introduce el nombre del entrenador.");
                }
                if (club.getValue() == null) {
                    throw new IllegalArgumentException("Selecciona un club.");
                }
                String managerName = manager.getText().trim();
                long teamId = club.getValue().getId();
                showLoading(stage, "CREANDO CARRERA", "Preparando calendario, plantilla y finanzas...");
                runAsync(stage, () -> {
                    new FootballWorldService().prepareSeason(initialSeason.getId());
                    ensureTeamHasCalendar(teamId, initialSeason.getId(),
                            initialSeason.getStartDate());
                    return careerService.createCareer(managerName,
                            teamId, initialSeason.getId());
                }, created -> {
                    career = created;
                    showDashboard(stage);
                });
            } catch (IllegalArgumentException exception) {
                error.setText(exception.getMessage());
            }
        });
        Button back = wideButton("VOLVER", "ghost-button");
        back.setOnAction(event -> showMainMenu(stage));
        card.getChildren().addAll(manager, club, error, create, back);
        setScene(stage, scrollableCentered(card, "centered-root"));
    }

    private void showLoadCareer(Stage stage) {
        VBox card = formCard("CARGAR CARRERA", "Continúa una partida guardada.");
        ListView<Career> saves = new ListView<>();
        Runnable refreshSaves = () -> saves.getItems().setAll(careers.findAll());
        refreshSaves.run();
        saves.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Career item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getManagerName() + "  •  "
                        + item.getControlledTeam().getName() + "  •  " + item.getCurrentDate());
            }
        });
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
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Eliminar partida");
            confirmation.setHeaderText("¿Eliminar la carrera de " + selected.getManagerName() + "?");
            confirmation.setContentText("La carrera desaparecerá de la lista de partidas guardadas.");
            confirmation.showAndWait().filter(ButtonType.OK::equals).ifPresent(result -> {
                careers.delete(selected.getId());
                refreshSaves.run();
            });
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
                new FlowPane(10, 10, rename, delete), load, back);
        setScene(stage, scrollableCentered(card, "centered-root"));
    }

    private void showDashboard(Stage stage) {
        activeSection = "dashboard";
        VBox content = page("CENTRO DE MANDO", career.getControlledTeam().getName()
                + "  •  " + career.getCurrentSeason().getName());
        FlowPane cards = new FlowPane(16, 16, statCard("FECHA", career.getCurrentDate().toString()),
                statCard("REPUTACIÓN", String.valueOf(career.getControlledTeam().getReputation())),
                financeCard(), statCard("POSICIÓN", leaguePositionSummary()));
        cards.getChildren().forEach(node -> {
            if (node instanceof Region region) region.setPrefWidth(210);
        });
        VBox nextMatch = panel("PRÓXIMO PARTIDO");
        nextMatch.getStyleClass().add("featured-match");
        Match next = findNextMatch();
        if (next == null) {
            nextMatch.getChildren().add(label("No hay encuentros próximos.", "empty-title"));
        } else {
            long daysAway = java.time.temporal.ChronoUnit.DAYS.between(
                    career.getCurrentDate(), next.getDate());
            VBox home = matchTeam(next.getHomeTeam(), next.getHomeTeam().getId()
                    == career.getControlledTeam().getId());
            VBox away = matchTeam(next.getAwayTeam(), next.getAwayTeam().getId()
                    == career.getControlledTeam().getId());
            VBox versus = new VBox(2, label("VS", "versus"),
                    label(daysAway == 0 ? "HOY" : "EN " + daysAway + " DÍAS", "match-countdown"));
            versus.setAlignment(Pos.CENTER);
            HBox pairing = new HBox(24, home, versus, away);
            pairing.setAlignment(Pos.CENTER);
            HBox.setHgrow(home, Priority.ALWAYS);
            HBox.setHgrow(away, Priority.ALWAYS);
            Label metadata = label(next.getCompetition().getName() + "  •  " + next.getDate()
                    + "  •  " + next.getHomeTeam().getStadiumName(), "match-metadata");
            nextMatch.getChildren().addAll(pairing, metadata);
        }
        VBox activity = panel("ACTIVIDAD DEL DÍA");
        java.util.Set<Long> competitionIds = currentCompetitions().stream()
                .map(Competition::getId).collect(java.util.stream.Collectors.toSet());
        var played = matches.findByDate(career.getCurrentDate()).stream()
                .filter(Match::isPlayed)
                .filter(match -> competitionIds.contains(match.getCompetition().getId()))
                .toList();
        if (played.isEmpty()) activity.getChildren().add(
                label("No hay resultados registrados hoy.", "body-label"));
        else played.forEach(match -> activity.getChildren().add(label(
                match.getHomeTeam().getShortName() + "  " + match.getHomeGoals()
                        + " — " + match.getAwayGoals() + "  "
                        + match.getAwayTeam().getShortName(), "result-row")));
        Button advance = button("AVANZAR UN DÍA", "primary-button");
        Match todayControlled = findControlledMatchToday();
        advance.setDisable(todayControlled != null && !todayControlled.isPlayed());
        advance.setOnAction(event -> advanceCareer(stage, 1));
        Button advanceWeek = button("AVANZAR HASTA 7 DÍAS", "secondary-button");
        advanceWeek.setDisable(todayControlled != null && !todayControlled.isPlayed());
        advanceWeek.setOnAction(event -> {
            Match upcoming = findNextMatch();
            long untilMatch = upcoming == null ? 7 : java.time.temporal.ChronoUnit.DAYS.between(
                    career.getCurrentDate(), upcoming.getDate());
            advanceCareer(stage, Math.max(1, (int) Math.min(7, untilMatch)));
        });
        Button simulateNext = button(todayControlled != null && !todayControlled.isPlayed()
                ? "SIMULAR PARTIDO" : "IR AL PRÓXIMO PARTIDO", "secondary-button");
        simulateNext.setDisable(next == null && todayControlled == null);
        simulateNext.setOnAction(event -> {
            Match today = findControlledMatchToday();
            if (today != null && !today.isPlayed()) {
                showMatchPreview(stage, today);
                return;
            }
            Match upcoming = findNextMatch();
            if (upcoming != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(
                        career.getCurrentDate(), upcoming.getDate());
                if (days > 0) advanceCareer(stage, Math.toIntExact(days));
            }
        });
        VBox squadStatus = panel("ESTADO DE LA PLANTILLA");
        squadStatus.getChildren().add(label(squadStatusSummary(), "body-label"));
        VBox recentForm = panel("ÚLTIMOS PARTIDOS");
        FlowPane formChips = recentFormChips();
        recentForm.getChildren().add(formChips);
        FlowPane actions = new FlowPane(12, 12, advance, advanceWeek, simulateNext);
        HBox secondary = new HBox(16, squadStatus, recentForm);
        HBox.setHgrow(squadStatus, Priority.ALWAYS);
        HBox.setHgrow(recentForm, Priority.ALWAYS);
        content.getChildren().addAll(cards, nextMatch, actions, secondary);
        CareerInsightService insights = new CareerInsightService();
        VBox objectives = panel("OBJETIVOS DE LA DIRECTIVA");
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
            VBox copy = new VBox(3, label(objective.title(), "objective-title"),
                    label(objective.detail(), "muted-label"));
            HBox row = new HBox(14, status, copy);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("objective-row");
            objectives.getChildren().add(row);
        });
        VBox news = panel("NOTICIAS Y ALERTAS");
        insights.news(career).forEach(item -> news.getChildren().add(
                label("•  " + item, "news-row")));
        HBox management = new HBox(16, objectives, news);
        HBox.setHgrow(objectives, Priority.ALWAYS);
        HBox.setHgrow(news, Priority.ALWAYS);
        content.getChildren().add(management);
        if (lastAdvanceSummary != null) {
            VBox summary = panel("RESUMEN DEL AVANCE");
            summary.getChildren().add(label(lastAdvanceSummary, "success-feedback"));
            content.getChildren().add(summary);
        }
        content.getChildren().add(activity);
        showCareerShell(stage, content);
    }

    private VBox matchTeam(Team team, boolean controlled) {
        Label badge = label(team.getShortName(), controlled ? "team-badge-user" : "team-badge");
        VBox box = new VBox(8, badge, label(team.getName(), "match-team-name"));
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private FlowPane recentFormChips() {
        long teamId = career.getControlledTeam().getId();
        java.util.List<Match> recent = currentCompetitions().stream()
                .flatMap(competition -> matches.findByCompetition(competition.getId()).stream())
                .filter(Match::isPlayed)
                .filter(match -> match.getHomeTeam().getId() == teamId
                        || match.getAwayTeam().getId() == teamId)
                .sorted(Comparator.comparing(Match::getDate).reversed()).limit(5).toList();
        FlowPane chips = new FlowPane(8, 8);
        if (recent.isEmpty()) {
            chips.getChildren().add(label("Aún no hay partidos disputados.", "muted-label"));
            return chips;
        }
        recent.reversed().forEach(match -> {
            boolean home = match.getHomeTeam().getId() == teamId;
            int own = home ? match.getHomeGoals() : match.getAwayGoals();
            int rival = home ? match.getAwayGoals() : match.getHomeGoals();
            Label chip = label(own > rival ? "V" : own == rival ? "E" : "D",
                    own > rival ? "form-win" : own == rival ? "form-draw" : "form-loss");
            Tooltip.install(chip, new Tooltip(match.getHomeTeam().getShortName() + " "
                    + match.getHomeGoals() + "–" + match.getAwayGoals() + " "
                    + match.getAwayTeam().getShortName()));
            chips.getChildren().add(chip);
        });
        return chips;
    }

    private void advanceCareer(Stage stage, int days) {
        java.time.LocalDate from = career.getCurrentDate();
        showLoading(stage, "SIMULANDO EL MUNDO",
                days == 1 ? "Avanzando un día..." : "Avanzando hasta " + days + " días...");
        runAsync(stage, () -> {
            careerService.advanceDaysForPlayer(career, days);
            return career;
        }, updated -> {
            career = updated;
            lastAdvanceSummary = "Del " + from + " al " + career.getCurrentDate()
                    + ". La partida se ha guardado automáticamente.";
            showDashboard(stage);
        });
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
        java.util.List<PlayerState> states = squad.stream().map(player ->
                repository.findByPlayer(player.getId())).filter(java.util.Objects::nonNull).toList();
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
        java.util.Map<Long, PlayerState> playerStates = new java.util.HashMap<>();
        squad.forEach(player -> playerStates.put(player.getId(), states.findByPlayer(player.getId())));
        TextField search = new TextField();
        search.setPromptText("Buscar jugador...");
        ComboBox<String> position = new ComboBox<>();
        position.getItems().addAll("TODAS", "GK", "CB", "LB", "RB", "CDM", "CM", "CAM", "LW", "RW", "ST");
        position.setValue("TODAS");
        TableView<Player> table = new TableView<>();
        table.getStyleClass().add("squad-table");
        addPlayerColumn(table, "POS", 65, player -> player.getPosition());
        addPlayerColumn(table, "JUGADOR", 220, Player::getFullName);
        addPlayerColumn(table, "GRL", 65, player -> player.getOverall());
        addPlayerColumn(table, "EDAD", 65, player -> player.getAge(career.getCurrentDate()));
        addPlayerColumn(table, "FORMA", 75, player -> playerStates.get(player.getId()).getForm());
        addPlayerColumn(table, "MORAL", 75, player -> playerStates.get(player.getId()).getMorale());
        addPlayerColumn(table, "FITNESS", 80, player -> playerStates.get(player.getId()).getFitness());
        addPlayerColumn(table, "VALOR", 105, player -> String.format("€%.1fM",
                player.getMarketValue() / 1_000_000));
        Runnable filter = () -> {
            String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
            String selectedPosition = position.getValue();
            table.getItems().setAll(squad.stream()
                    .filter(player -> query.isEmpty()
                            || player.getFullName().toLowerCase().contains(query))
                    .filter(player -> "TODAS".equals(selectedPosition)
                            || player.getPosition().name().equals(selectedPosition)).toList());
        };
        search.textProperty().addListener((observable, oldValue, newValue) -> filter.run());
        position.setOnAction(event -> filter.run());
        filter.run();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);
        Button details = button("VER FICHA", "primary-button");
        details.setOnAction(event -> {
            Player selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) showPlayer(stage, selected, "squad");
        });
        table.setRowFactory(view -> {
            TableRow<Player> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty())
                    showPlayer(stage, row.getItem(), "squad");
            });
            return row;
        });
        FlowPane filters = new FlowPane(12, 12, search, position);
        content.getChildren().addAll(filters, table, details);
        showCareerShell(stage, content);
    }

    private <T> void addPlayerColumn(TableView<Player> table, String title, double width,
            java.util.function.Function<Player, T> value) {
        TableColumn<Player, T> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyObjectWrapper<>(
                value.apply(cell.getValue())));
        table.getColumns().add(column);
    }

    private void showPlaceholder(Stage stage, String title) {
        VBox content = page(title, "La pantalla se conectará en el siguiente bloque del día 8.");
        VBox construction = panel("EN CONSTRUCCIÓN");
        construction.getChildren().add(label(
                "La lógica ya existe y está probada. Ahora estamos construyendo su presentación.",
                "body-label"));
        content.getChildren().add(construction);
        showCareerShell(stage, content);
    }

    private void showCareerShell(Stage stage, Node content) {
        VBox sidebar = new VBox(10);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(250);
        HBox brand = new HBox(12, label("FC", "sidebar-logo"),
                new VBox(2, label("FOOTBALL", "sidebar-brand"),
                        label("CAREER", "sidebar-brand-accent")));
        brand.setAlignment(Pos.CENTER_LEFT);
        VBox clubIdentity = new VBox(4,
                label(career.getControlledTeam().getShortName(), "club-code"),
                label(career.getControlledTeam().getName(), "club-name"),
                label("Mánager  •  " + career.getManagerName(), "muted-label"));
        clubIdentity.getStyleClass().add("club-identity");
        Label navigationTitle = label("NAVEGACIÓN", "nav-section-title");
        Button dashboard = navButton("Dashboard", "dashboard");
        dashboard.setOnAction(event -> showDashboard(stage));
        Button squad = navButton("Plantilla", "squad");
        squad.setOnAction(event -> showSquad(stage));
        Button lineup = navButton("Alineación", "lineup");
        lineup.setOnAction(event -> showLineup(stage));
        Button training = navButton("Entrenamiento", "training");
        training.setOnAction(event -> showTraining(stage));
        Button calendar = navButton("Calendario", "calendar");
        calendar.setOnAction(event -> showCalendar(stage));
        Button standings = navButton("Clasificación", "standings");
        standings.setOnAction(event -> showStandings(stage));
        Button results = navButton("Resultados", "results");
        results.setOnAction(event -> showResults(stage));
        Button market = navButton("Mercado", "market");
        market.setOnAction(event -> showMarket(stage));
        Button history = navButton("Historial", "history");
        history.setOnAction(event -> showTransferHistory(stage));
        Button settings = navButton("Ajustes", "settings");
        settings.setOnAction(event -> showSettings(stage));
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Button menu = navButton("Guardar y salir", "menu");
        menu.setOnAction(event -> {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Guardar y salir");
            confirmation.setHeaderText("¿Volver al menú principal?");
            confirmation.setContentText("El progreso actual ya está guardado.");
            confirmation.showAndWait().filter(ButtonType.OK::equals).ifPresent(result -> {
                career = null;
                activeSection = "dashboard";
                showMainMenu(stage);
            });
        });
        VBox footer = new VBox(3, label("TEMPORADA " + career.getCurrentSeason().getName(),
                "sidebar-season"), label(career.getCurrentDate().toString(), "sidebar-date"));
        footer.getStyleClass().add("sidebar-footer");
        sidebar.getChildren().addAll(brand, clubIdentity, navigationTitle,
                dashboard, squad, lineup, training, calendar, standings, results,
                market, history, settings, spacer, footer, menu);
        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("app-shell");
        shell.setLeft(sidebar);
        Node center = content;
        if (!(content instanceof ScrollPane)) {
            ScrollPane scroll = new ScrollPane(content);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(true);
            scroll.setPannable(true);
            center = scroll;
        }
        shell.setCenter(center);
        setScene(stage, shell);
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
                    java.time.format.TextStyle.FULL, new java.util.Locale("es", "ES")).toUpperCase()
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
                Label event = label((home ? "LOCAL  " : "FUERA  ") + opponent.getShortName(),
                        "calendar-club-match");
                event.setMaxWidth(Double.MAX_VALUE);
                cell.getChildren().add(event);
                if (match.isPlayed()) cell.getChildren().add(label(match.getHomeGoals() + " - "
                        + match.getAwayGoals(), "calendar-score"));
            });
            long otherMatches = dayMatches.stream().filter(match -> !isControlledMatch(match)).count();
            if (otherMatches > 0) cell.getChildren().add(label(otherMatches + " partidos", "muted-label"));
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
            Label fixture = label(match.getCompetition().getName() + "  •  "
                    + match.getHomeTeam().getShortName() + "  "
                    + (match.isPlayed() ? match.getHomeGoals() + " - " + match.getAwayGoals() : "VS")
                    + "  " + match.getAwayTeam().getShortName(),
                    isControlledMatch(match) ? "calendar-detail-main" : "body-label");
            HBox row = new HBox(14, fixture);
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
        VBox content = page("CLASIFICACIÓN", "Tabla de la competición");
        ComboBox<Competition> selector = new ComboBox<>();
        selector.getItems().addAll(currentCompetitions());
        selector.setCellFactory(list -> competitionCell());
        selector.setButtonCell(competitionCell());
        selector.setMaxWidth(360);
        TableView<LeagueStanding> table = new TableView<>();
        table.getStyleClass().add("standings-table");
        TableColumn<LeagueStanding, Number> positionColumn = new TableColumn<>("POS");
        positionColumn.setPrefWidth(60);
        positionColumn.setSortable(false);
        positionColumn.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyIntegerWrapper(
                table.getItems().indexOf(cell.getValue()) + 1));
        addStandingColumn(table, "CLUB", 240, row -> row.getTeam().getName());
        addStandingColumn(table, "PJ", 55, LeagueStanding::getPlayed);
        addStandingColumn(table, "G", 50, LeagueStanding::getWins);
        addStandingColumn(table, "E", 50, LeagueStanding::getDraws);
        addStandingColumn(table, "P", 50, LeagueStanding::getLosses);
        addStandingColumn(table, "GF", 55, LeagueStanding::getGoalsFor);
        addStandingColumn(table, "GC", 55, LeagueStanding::getGoalsAgainst);
        addStandingColumn(table, "DG", 60, LeagueStanding::getGoalDifference);
        addStandingColumn(table, "PTS", 65, LeagueStanding::getPoints);
        table.getColumns().addFirst(positionColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setRowFactory(view -> new TableRow<>() {
            @Override protected void updateItem(LeagueStanding item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("controlled-team-row");
                if (!empty && item != null && item.getTeam().getId()
                        == career.getControlledTeam().getId()) {
                    getStyleClass().add("controlled-team-row");
                }
            }
        });
        Runnable refresh = () -> {
            table.getItems().clear();
            Competition selected = selector.getValue();
            if (selected == null) return;
            selectedCompetitionId = selected.getId();
            table.getItems().setAll(new LeagueStandingRepository()
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
        VBox.setVgrow(table, Priority.ALWAYS);
        content.getChildren().addAll(selector, table);
        showCareerShell(stage, content);
    }

    private <T> void addStandingColumn(TableView<LeagueStanding> table, String title,
            double width, java.util.function.Function<LeagueStanding, T> value) {
        TableColumn<LeagueStanding, T> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyObjectWrapper<>(
                value.apply(cell.getValue())));
        table.getColumns().add(column);
    }

    private void showResults(Stage stage) {
        activeSection = "results";
        VBox content = page("RESULTADOS DEL MUNDO",
                "Consulta cualquier fecha y filtra por competición");
        ListView<String> resultList = new ListView<>();
        resultList.getStyleClass().add("data-list");
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
            resultList.getItems().setAll(dayMatches.stream()
                    .filter(match -> "TODAS LAS COMPETICIONES".equals(competition.getValue())
                            || match.getCompetition().getName().equals(competition.getValue()))
                    .map(match -> match.getCompetition().getName() + "  •  "
                            + match.getHomeTeam().getShortName() + "  "
                            + match.getHomeGoals() + " — " + match.getAwayGoals()
                            + "  " + match.getAwayTeam().getShortName()).toList());
            if (resultList.getItems().isEmpty()) resultList.getItems().add(
                    "No hay resultados registrados con estos filtros.");
        };
        date.setOnAction(event -> refresh.run());
        competition.setOnAction(event -> {
            if (date.getValue() == null) return;
            resultList.getItems().setAll(matches.findByDate(date.getValue()).stream()
                    .filter(Match::isPlayed)
                    .filter(match -> "TODAS LAS COMPETICIONES".equals(competition.getValue())
                            || match.getCompetition().getName().equals(competition.getValue()))
                    .map(match -> match.getCompetition().getName() + "  •  "
                            + match.getHomeTeam().getShortName() + "  " + match.getHomeGoals()
                            + " — " + match.getAwayGoals() + "  "
                            + match.getAwayTeam().getShortName()).toList());
            if (resultList.getItems().isEmpty()) resultList.getItems().add(
                    "No hay resultados registrados con estos filtros.");
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
        VBox.setVgrow(resultList, Priority.ALWAYS);
        content.getChildren().addAll(new FlowPane(12, 12, previous, date, next, competition),
                resultList);
        showCareerShell(stage, content);
    }

    private void showMatchReport(Stage stage, Match selectedMatch) {
        MatchReport report;
        try {
            report = new MatchReportService().build(selectedMatch.getId());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showCalendar(stage);
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

        VBox timeline = panel("CRONOLOGÍA");
        PlayerRepository playerRepository = new PlayerRepository();
        report.getEvents().forEach(event -> {
            Player mainPlayer = playerRepository.findById(event.getPlayer().getId());
            String detail = event.getMinute() + "'  " + eventLabel(event.getType()) + "  •  "
                    + mainPlayer.getFullName();
            if (event.getSecondaryPlayer() != null) {
                Player second = playerRepository.findById(event.getSecondaryPlayer().getId());
                detail += "  (" + second.getFullName() + ")";
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
        } else {
            impact.getChildren().add(label(
                    "Este encuentro no afecta directamente a tu plantilla.", "muted-label"));
        }
        Button back = button("VOLVER AL CALENDARIO", "ghost-button");
        back.setOnAction(event -> showCalendar(stage));
        Button dashboard = button("IR AL CENTRO DE MANDO", "secondary-button");
        dashboard.setOnAction(event -> showDashboard(stage));
        content.getChildren().addAll(score, statistics, bestPlayer, impact, timeline,
                new FlowPane(12, 12, dashboard, back));
        showCareerShell(stage, new ScrollPane(content));
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
        ClubFinance finance = new ClubFinanceRepository()
                .findByTeam(career.getControlledTeam().getId());
        String budget = finance == null ? "Sin datos financieros"
                : String.format("Presupuesto: €%.1fM  •  Margen salarial: €%.1fM",
                finance.getTransferBudget() / 1_000_000,
                finance.getAvailableWageBudget() / 1_000_000);
        VBox content = page("MERCADO DE FICHAJES", budget);
        PlayerMarketRepository marketRepository = new PlayerMarketRepository();
        new ClubTransferAiService().ensureMarketSupply(career.getControlledTeam().getId());

        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("market-tabs");
        tabs.getTabs().addAll(
                new Tab("COMPRAR", createBuyTab(stage, marketRepository)),
                new Tab("MIS VENTAS", createSalesTab(marketRepository)),
                new Tab("OFERTAS RECIBIDAS", createIncomingOffersTab(stage)));
        tabs.getTabs().forEach(tab -> tab.setClosable(false));
        tabs.getSelectionModel().select(Math.min(selectedMarketTab, tabs.getTabs().size() - 1));
        tabs.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) ->
                selectedMarketTab = newValue.intValue());
        VBox.setVgrow(tabs, Priority.ALWAYS);
        content.getChildren().add(tabs);
        showCareerShell(stage, content);
    }

    private Node createBuyTab(Stage stage, PlayerMarketRepository marketRepository) {
        VBox box = panel("BUSCAR Y NEGOCIAR");
        java.util.List<Player> marketPlayers = marketRepository
                .findTransferListed(career.getControlledTeam().getId());
        java.util.Map<Long, Double> marketPrices = new java.util.HashMap<>();
        java.util.Map<Long, Team> marketTeams = new java.util.HashMap<>();
        CareerShortlistRepository shortlistRepository = new CareerShortlistRepository();
        java.util.Set<Long> shortlistIds = new java.util.HashSet<>(
                shortlistRepository.findPlayerIds(career.getId()));
        PlayerTeamRepository playerTeams = new PlayerTeamRepository();
        marketPlayers.forEach(player -> marketPrices.put(player.getId(),
                marketRepository.findAskingPrice(player.getId())));
        marketPlayers.forEach(player -> {
            Long teamId = playerTeams.findCurrentTeamId(player.getId());
            if (teamId != null) marketTeams.put(player.getId(), teams.findById(teamId));
        });
        ListView<Player> targets = marketPlayerList(marketPrices, marketTeams, shortlistIds);
        TextField search = new TextField();
        search.setPromptText("Buscar jugador...");
        ComboBox<String> position = new ComboBox<>();
        position.getItems().addAll("TODAS", "GK", "DEFENSA", "MEDIO", "ATAQUE");
        position.setValue("TODAS");
        TextField maximumPrice = new TextField();
        maximumPrice.setPromptText("Precio máximo (€M)");
        TextField minimumOverall = new TextField();
        minimumOverall.setPromptText("GRL mínimo");
        TextField maximumAge = new TextField();
        maximumAge.setPromptText("Edad máxima");
        TextField maximumSalary = new TextField();
        maximumSalary.setPromptText("Salario máximo (€M)");
        ComboBox<String> sort = new ComboBox<>();
        sort.getItems().addAll("PRECIO ↑", "PRECIO ↓", "GRL ↓", "EDAD ↑");
        sort.setValue("PRECIO ↑");
        CheckBox shortlistOnly = new CheckBox("Solo seguimiento");
        Label comparison = label("Selecciona un jugador para compararlo con tu plantilla.",
                "comparison-label");
        Runnable refreshTargets = () -> {
            String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
            double maximum;
            try {
                maximum = maximumPrice.getText() == null || maximumPrice.getText().isBlank()
                        ? Double.MAX_VALUE
                        : Double.parseDouble(maximumPrice.getText().replace(',', '.')) * 1_000_000;
            } catch (NumberFormatException exception) {
                maximum = Double.MAX_VALUE;
            }
            int overallLimit = parseOptionalInteger(minimumOverall.getText(), 0);
            int ageLimit = parseOptionalInteger(maximumAge.getText(), Integer.MAX_VALUE);
            double salaryLimit = parseOptionalMillions(maximumSalary.getText(), Double.MAX_VALUE);
            final double priceLimit = maximum;
            Comparator<Player> comparator = switch (sort.getValue()) {
                case "PRECIO ↓" -> Comparator.comparingDouble(
                        (Player player) -> marketPrices.get(player.getId())).reversed();
                case "GRL ↓" -> Comparator.comparingInt(Player::getOverall).reversed();
                case "EDAD ↑" -> Comparator.comparingInt(
                        player -> player.getAge(career.getCurrentDate()));
                default -> Comparator.comparingDouble(
                        player -> marketPrices.get(player.getId()));
            };
            targets.getItems().setAll(marketPlayers.stream()
                    .filter(player -> query.isEmpty()
                            || player.getFullName().toLowerCase().contains(query)
                            || (marketTeams.get(player.getId()) != null
                            && marketTeams.get(player.getId()).getName().toLowerCase().contains(query)))
                    .filter(player -> matchesPositionGroup(player, position.getValue()))
                    .filter(player -> !shortlistOnly.isSelected()
                            || shortlistIds.contains(player.getId()))
                    .filter(player -> player.getOverall() >= overallLimit)
                    .filter(player -> player.getAge(career.getCurrentDate()) <= ageLimit)
                    .filter(player -> player.getSalary() <= salaryLimit)
                    .filter(player -> {
                        Double price = marketPrices.get(player.getId());
                        return price != null && price <= priceLimit;
                    }).sorted(comparator).toList());
        };
        search.textProperty().addListener((observable, oldValue, newValue) -> refreshTargets.run());
        maximumPrice.textProperty().addListener((observable, oldValue, newValue) -> refreshTargets.run());
        minimumOverall.textProperty().addListener((observable, oldValue, newValue) -> refreshTargets.run());
        maximumAge.textProperty().addListener((observable, oldValue, newValue) -> refreshTargets.run());
        maximumSalary.textProperty().addListener((observable, oldValue, newValue) -> refreshTargets.run());
        position.setOnAction(event -> refreshTargets.run());
        sort.setOnAction(event -> refreshTargets.run());
        shortlistOnly.setOnAction(event -> refreshTargets.run());
        refreshTargets.run();
        TextField offerAmount = new TextField();
        offerAmount.setPromptText("Oferta en millones de euros");
        Label feedback = label("Selecciona un jugador y plantea una oferta.", "muted-label");
        TransferOffer[] counterOffer = {null};
        Button acceptCounter = button("ACEPTAR CONTRAOFERTA", "secondary-button");
        acceptCounter.setVisible(false);
        acceptCounter.setManaged(false);
        Button buy = button("REALIZAR OFERTA", "primary-button");
        Button details = button("VER FICHA", "ghost-button");
        Button shortlist = button("AÑADIR A SEGUIMIENTO", "secondary-button");
        shortlist.setDisable(true);
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
                showPlayer(stage, selected, "market");
            }
        });
        targets.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> {
                    comparison.setText(selected == null
                            ? "Selecciona un jugador para compararlo con tu plantilla."
                            : marketComparison(selected));
                    shortlist.setDisable(selected == null);
                    shortlist.setText(selected != null && shortlistIds.contains(selected.getId())
                            ? "QUITAR DE SEGUIMIENTO" : "AÑADIR A SEGUIMIENTO");
                });
        buy.setOnAction(event -> {
            Player target = targets.getSelectionModel().getSelectedItem();
            try {
                if (target == null) throw new IllegalArgumentException("Selecciona un jugador.");
                double amount = Double.parseDouble(offerAmount.getText().replace(',', '.'))
                        * 1_000_000;
                TransferOfferService service = new TransferOfferService();
                TransferOffer offer = service.evaluate(service.makeOffer(target.getId(),
                        career.getControlledTeam().getId(), amount,
                        career.getCurrentDate()).getId());
                if (offer.getStatus() == footballcareer.model.enums.TransferOfferStatus.ACCEPTED) {
                    executeTransfer(offer, target);
                    feedback.setText("Fichaje completado: " + target.getFullName());
                    animateFeedback(feedback, true);
                    refreshMarketAfter(stage);
                } else if (offer.getCounterAmount() != null) {
                    counterOffer[0] = offer;
                    feedback.setText(String.format("Contraoferta: €%.1fM",
                            offer.getCounterAmount() / 1_000_000));
                    acceptCounter.setManaged(true);
                    acceptCounter.setVisible(true);
                    animateFeedback(feedback, false);
                } else {
                    feedback.setText("Oferta rechazada: está demasiado lejos del precio.");
                    animateFeedback(feedback, false);
                }
            } catch (NumberFormatException exception) {
                feedback.setText("Introduce una cantidad válida.");
                animateFeedback(feedback, false);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                feedback.setText(exception.getMessage());
                animateFeedback(feedback, false);
            }
        });
        acceptCounter.setOnAction(event -> {
            try {
                TransferOffer accepted = new TransferOfferService()
                        .acceptCounterOffer(counterOffer[0].getId());
                executeTransfer(accepted,
                        new PlayerRepository().findById(accepted.getPlayer().getId()));
                feedback.setText("Contraoferta aceptada. Fichaje completado.");
                animateFeedback(feedback, true);
                refreshMarketAfter(stage);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                feedback.setText(exception.getMessage());
                animateFeedback(feedback, false);
            }
        });
        FlowPane actions = new FlowPane(10, 10, buy, acceptCounter, details, shortlist);
        VBox.setVgrow(targets, Priority.ALWAYS);
        box.getChildren().addAll(new FlowPane(10, 10, search, position, maximumPrice,
                        minimumOverall, maximumAge, maximumSalary, sort, shortlistOnly),
                targets, comparison, offerAmount, actions, feedback);
        return box;
    }

    private int parseOptionalInteger(String value, int fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private double parseOptionalMillions(String value, double fallback) {
        try {
            return value == null || value.isBlank() ? fallback
                    : Double.parseDouble(value.replace(',', '.').trim()) * 1_000_000;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String marketComparison(Player target) {
        Player current = new PlayerRepository()
                .findCurrentPlayersByTeam(career.getControlledTeam().getId()).stream()
                .filter(player -> player.getPosition() == target.getPosition())
                .max(Comparator.comparingInt(Player::getOverall)).orElse(null);
        if (current == null) return "No tienes otro " + target.getPosition()
                + " en plantilla. Sería una incorporación prioritaria.";
        int difference = target.getOverall() - current.getOverall();
        return target.getFullName() + "  GRL " + target.getOverall() + "  •  Mejor actual: "
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
        VBox box = panel("JUGADORES EN VENTA");
        ListView<Player> ownPlayers = ownMarketPlayerList(marketRepository);
        ownPlayers.getItems().addAll(new PlayerRepository()
                .findCurrentPlayersByTeam(career.getControlledTeam().getId()));
        TextField askingPrice = new TextField();
        askingPrice.setPromptText("Precio solicitado en millones");
        Label feedback = label("Selecciona un jugador para gestionar su estado.", "muted-label");
        Button listPlayer = button("PONER EN VENTA", "secondary-button");
        listPlayer.setOnAction(event -> {
            Player selected = ownPlayers.getSelectionModel().getSelectedItem();
            try {
                if (selected == null) throw new IllegalArgumentException("Selecciona un jugador.");
                double price = Double.parseDouble(askingPrice.getText().replace(',', '.'))
                        * 1_000_000;
                marketRepository.listForTransfer(selected.getId(), price);
                feedback.setText(selected.getFullName() + " está en venta.");
                ownPlayers.refresh();
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
                feedback.setText(selected.getFullName() + " ya no está en venta.");
                ownPlayers.refresh();
                animateFeedback(feedback, true);
            }
        });
        VBox.setVgrow(ownPlayers, Priority.ALWAYS);
        box.getChildren().addAll(ownPlayers, askingPrice,
                new FlowPane(10, 10, listPlayer, removePlayer), feedback);
        return box;
    }

    private Node createIncomingOffersTab(Stage stage) {
        VBox box = panel("BANDEJA DE OFERTAS");
        Label feedback = label("Las ofertas de otros clubes aparecerán aquí.", "muted-label");
        ListView<TransferOffer> incoming = incomingOfferList();
        incoming.getItems().addAll(new TransferOfferRepository()
                .findPendingBySellingTeam(career.getControlledTeam().getId()));
        Button accept = button("ACEPTAR OFERTA", "primary-button");
        accept.setOnAction(event -> respondToIncomingOffer(stage,
                incoming.getSelectionModel().getSelectedItem(), true, feedback));
        Button reject = button("RECHAZAR", "ghost-button");
        reject.setOnAction(event -> respondToIncomingOffer(stage,
                incoming.getSelectionModel().getSelectedItem(), false, feedback));
        VBox.setVgrow(incoming, Priority.ALWAYS);
        box.getChildren().addAll(incoming, new FlowPane(10, 10, accept, reject), feedback);
        return box;
    }

    private void executeTransfer(TransferOffer offer, Player player) {
        new TransferExecutionService().completeTransfer(offer.getId(),
                player.getSalary(), java.time.LocalDate.of(
                        career.getCurrentDate().getYear() + 3, 6, 30),
                career.getCurrentSeason().getId(), career.getCurrentDate());
    }

    private void respondToIncomingOffer(Stage stage, TransferOffer offer,
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
            refreshMarketAfter(stage);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            feedback.setText(exception.getMessage());
            animateFeedback(feedback, false);
        }
    }

    private void animateFeedback(Label feedback, boolean success) {
        feedback.getStyleClass().removeAll("success-feedback", "warning-feedback");
        feedback.getStyleClass().add(success ? "success-feedback" : "warning-feedback");
        feedback.setOpacity(0.15);
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(450), feedback);
        fade.setToValue(1);
        fade.play();
    }

    private void refreshMarketAfter(Stage stage) {
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(1.1));
        pause.setOnFinished(event -> showMarket(stage));
        pause.play();
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
        Button back = button("market".equals(origin)
                ? "VOLVER AL MERCADO" : "VOLVER A LA PLANTILLA", "ghost-button");
        back.setOnAction(event -> {
            if ("market".equals(origin)) showMarket(stage);
            else showSquad(stage);
        });
        content.getChildren().addAll(headline, attributes, contractPanel, statsPanel, back);
        showCareerShell(stage, content);
    }

    private void showTransferHistory(Stage stage) {
        activeSection = "history";
        VBox content = page("HISTORIAL DE FICHAJES",
                career.getControlledTeam().getName() + "  •  Altas y bajas");
        ListView<String> history = new ListView<>();
        history.getStyleClass().add("data-list");
        PlayerRepository playerRepository = new PlayerRepository();
        TeamRepository teamRepository = new TeamRepository();
        for (Transfer transfer : new TransferRepository()
                .findByTeam(career.getControlledTeam().getId())) {
            Player player = playerRepository.findById(transfer.getPlayer().getId());
            Team from = teamRepository.findById(transfer.getFromTeam().getId());
            Team to = teamRepository.findById(transfer.getToTeam().getId());
            String direction = transfer.getToTeam().getId()
                    == career.getControlledTeam().getId() ? "ALTA" : "BAJA";
            history.getItems().add(String.format(
                    "%s  •  %s  •  %s → %s  •  €%.1fM  •  %s",
                    direction, player.getFullName(), from.getShortName(), to.getShortName(),
                    transfer.getAmount() / 1_000_000, transfer.getTransferDate()));
        }
        if (history.getItems().isEmpty()) history.getItems().add(
                "Todavía no hay fichajes registrados para este club.");
        VBox.setVgrow(history, Priority.ALWAYS);
        content.getChildren().add(history);
        showCareerShell(stage, content);
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
        MatchLineup initial = lineupService.selectMatchLineup(nextMatch.getId(), teamId);
        ListView<Player> starters = playerList();
        starters.getItems().addAll(initial.getStarters());
        ListView<Player> substitutes = playerList();
        substitutes.getItems().addAll(initial.getSubstitutes());
        ListView<Player> reserves = playerList();
        new PlayerRepository().findCurrentPlayersByTeam(teamId).stream()
                .filter(player -> starters.getItems().stream()
                .noneMatch(starter -> starter.getId() == player.getId()))
                .filter(player -> substitutes.getItems().stream()
                .noneMatch(substitute -> substitute.getId() == player.getId()))
                .forEach(reserves.getItems()::add);
        VBox tacticalPitch = new VBox(14);
        tacticalPitch.getStyleClass().add("tactical-pitch");
        ComboBox<String> formation = new ComboBox<>();
        formation.getItems().addAll("4-3-3", "4-2-3-1", "4-4-2");
        formation.setValue(tacticsRepository.findFormation(nextMatch.getId(), teamId));
        Label tacticalWarning = label("", "warning-feedback");
        Runnable refreshPitch = () -> {
            java.util.List<Player> selected = java.util.List.copyOf(starters.getItems());
            renderPitch(tacticalPitch, selected, formation.getValue());
            tacticalWarning.setText(formationAssessment(selected, formation.getValue()));
        };
        formation.setOnAction(event -> refreshPitch.run());
        refreshPitch.run();
        Label count = label("Titulares: 11 / 11", "eyebrow");
        Button removeStarter = button("BAJAR DEL ONCE  →", "secondary-button");
        removeStarter.setOnAction(event -> {
            Player selected = starters.getSelectionModel().getSelectedItem();
            if (selected != null) {
                starters.getItems().remove(selected);
                reserves.getItems().add(selected);
                count.setText("Titulares: " + starters.getItems().size() + " / 11");
                refreshPitch.run();
            }
        });
        Button addStarter = button("←  SUBIR AL ONCE", "primary-button");
        addStarter.setOnAction(event -> {
            Player selected = substitutes.getSelectionModel().getSelectedItem();
            if (selected == null) selected = reserves.getSelectionModel().getSelectedItem();
            if (selected != null && starters.getItems().size() < 11) {
                substitutes.getItems().remove(selected);
                reserves.getItems().remove(selected);
                starters.getItems().add(selected);
                count.setText("Titulares: " + starters.getItems().size() + " / 11");
                refreshPitch.run();
            }
        });
        VBox controls = new VBox(12, count, addStarter, removeStarter);
        controls.setAlignment(Pos.CENTER);
        VBox startersPanel = panel("ONCE TITULAR");
        startersPanel.getChildren().add(starters);
        VBox substitutesPanel = panel("BANQUILLO  •  MÁXIMO 7");
        substitutesPanel.getChildren().add(substitutes);
        VBox reservesPanel = panel("RESERVAS");
        reservesPanel.getChildren().add(reserves);
        Button toBench = button("AÑADIR AL BANQUILLO", "secondary-button");
        Button toReserves = button("PASAR A RESERVAS", "ghost-button");
        toBench.setOnAction(event -> {
            Player selected = reserves.getSelectionModel().getSelectedItem();
            if (selected != null && substitutes.getItems().size() < 7) {
                reserves.getItems().remove(selected);
                substitutes.getItems().add(selected);
            }
        });
        toReserves.setOnAction(event -> {
            Player selected = substitutes.getSelectionModel().getSelectedItem();
            if (selected != null) {
                substitutes.getItems().remove(selected);
                reserves.getItems().add(selected);
            }
        });
        VBox benchManagement = new VBox(10, substitutesPanel,
                new FlowPane(8, 8, toBench, toReserves), reservesPanel);
        HBox pitch = new HBox(14, startersPanel, controls, benchManagement);
        HBox.setHgrow(startersPanel, Priority.ALWAYS);
        HBox.setHgrow(benchManagement, Priority.ALWAYS);
        Label feedback = label("El banquillo se gestiona explícitamente y admite siete jugadores.",
                "muted-label");
        Button save = button("GUARDAR ALINEACIÓN", "primary-button");
        save.setOnAction(event -> {
            try {
                validateFormation(java.util.List.copyOf(starters.getItems()),
                        formation.getValue());
                repository.save(nextMatch.getId(), teamId,
                        java.util.List.copyOf(starters.getItems()),
                        java.util.List.copyOf(substitutes.getItems()));
                tacticsRepository.saveFormation(nextMatch.getId(), teamId, formation.getValue());
                feedback.setText("Once y formación " + formation.getValue()
                        + " guardados para este partido.");
            } catch (IllegalArgumentException exception) {
                feedback.setText(exception.getMessage());
            }
        });
        VBox.setVgrow(pitch, Priority.ALWAYS);
        FlowPane tacticalHeader = new FlowPane(12, 12,
                label("FORMACIÓN", "panel-title"), formation,
                label("Se guarda específicamente para el próximo partido.", "muted-label"));
        content.getChildren().addAll(tacticalHeader, tacticalPitch, tacticalWarning,
                pitch, save, feedback);
        showCareerShell(stage, content);
    }

    private String formationAssessment(java.util.List<Player> starters, String formation) {
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
        long fatigued = starters.stream().map(player -> new PlayerStateRepository()
                .findByPlayer(player.getId())).filter(java.util.Objects::nonNull)
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

    private void validateFormation(java.util.List<Player> starters, String formation) {
        String assessment = formationAssessment(starters, formation);
        if (assessment.startsWith("DESAJUSTE")) throw new IllegalArgumentException(assessment);
    }

    private void renderPitch(VBox pitch, java.util.List<Player> starters, String formation) {
        pitch.getChildren().clear();
        java.util.List<Player> pool = new java.util.ArrayList<>(starters);
        java.util.List<Player> goalkeeper = takePlayers(pool, 1,
                java.util.Set.of(footballcareer.model.enums.Position.GK));
        java.util.List<java.util.List<Player>> rows = new java.util.ArrayList<>();
        if ("4-2-3-1".equals(formation)) {
            rows.add(takePlayers(pool, 1, java.util.Set.of(
                    footballcareer.model.enums.Position.ST)));
            rows.add(takePlayers(pool, 3, java.util.Set.of(
                    footballcareer.model.enums.Position.CAM,
                    footballcareer.model.enums.Position.LW,
                    footballcareer.model.enums.Position.RW)));
            rows.add(takePlayers(pool, 2, java.util.Set.of(
                    footballcareer.model.enums.Position.CDM,
                    footballcareer.model.enums.Position.CM)));
        } else {
            int forwards = "4-4-2".equals(formation) ? 2 : 3;
            int midfielders = "4-4-2".equals(formation) ? 4 : 3;
            rows.add(takePlayers(pool, forwards, java.util.Set.of(
                    footballcareer.model.enums.Position.ST,
                    footballcareer.model.enums.Position.LW,
                    footballcareer.model.enums.Position.RW)));
            rows.add(takePlayers(pool, midfielders, java.util.Set.of(
                    footballcareer.model.enums.Position.CDM,
                    footballcareer.model.enums.Position.CM,
                    footballcareer.model.enums.Position.CAM)));
        }
        rows.add(takePlayers(pool, 4, java.util.Set.of(
                footballcareer.model.enums.Position.CB,
                footballcareer.model.enums.Position.LB,
                footballcareer.model.enums.Position.RB)));
        rows.add(goalkeeper);
        rows.forEach(row -> pitch.getChildren().add(pitchLine(row)));
        if (starters.isEmpty()) pitch.getChildren().add(label(
                "Selecciona futbolistas para construir el once.", "pitch-empty"));
    }

    private java.util.List<Player> takePlayers(java.util.List<Player> pool, int amount,
            java.util.Set<footballcareer.model.enums.Position> preferred) {
        java.util.List<Player> selected = new java.util.ArrayList<>();
        for (Player player : java.util.List.copyOf(pool)) {
            if (selected.size() == amount) break;
            if (preferred.contains(player.getPosition())) {
                selected.add(player);
                pool.remove(player);
            }
        }
        while (selected.size() < amount && !pool.isEmpty()) selected.add(pool.removeFirst());
        return selected;
    }

    private HBox pitchLine(java.util.List<Player> players) {
        HBox line = new HBox(12);
        line.setAlignment(Pos.CENTER);
        for (Player player : players) {
            VBox chip = new VBox(2, label(player.getPosition().name(), "pitch-position"),
                    label(player.getLastName(), "pitch-player-name"),
                    label(String.valueOf(player.getOverall()), "pitch-rating"));
            chip.setAlignment(Pos.CENTER);
            chip.getStyleClass().add("pitch-player");
            Tooltip.install(chip, new Tooltip(player.getFullName() + "  •  GRL "
                    + player.getOverall()));
            line.getChildren().add(chip);
        }
        return line;
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
        content.getChildren().add(display);
        showCareerShell(stage, content);
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

    private VBox financeCard() {
        ClubFinance finance = new ClubFinanceRepository()
                .findByTeam(career.getControlledTeam().getId());
        return statCard("PRESUPUESTO", finance == null ? "—"
                : String.format("€%.1fM", finance.getTransferBudget() / 1_000_000));
    }

    private Match findNextMatch() {
        return new CompetitionTeamRepository()
                .findCompetitionsByTeam(career.getControlledTeam().getId()).stream()
                .filter(c -> c.getSeason().getId() == career.getCurrentSeason().getId())
                .flatMap(c -> matches.findByCompetition(c.getId()).stream())
                .filter(match -> !match.isPlayed()
                        && !match.getDate().isBefore(career.getCurrentDate()))
                .min(Comparator.comparing(Match::getDate)).orElse(null);
    }

    private Match findControlledMatchToday() {
        return currentCompetitions().stream()
                .flatMap(competition -> matches.findByCompetition(competition.getId()).stream())
                .filter(match -> match.getDate().equals(career.getCurrentDate()))
                .filter(match -> match.getHomeTeam().getId() == career.getControlledTeam().getId()
                        || match.getAwayTeam().getId() == career.getControlledTeam().getId())
                .findFirst().orElse(null);
    }

    private void showMatchPreview(Stage stage, Match match) {
        long controlledTeamId = career.getControlledTeam().getId();
        PlayerStateRepository states = new PlayerStateRepository();
        LineupService lineups = new LineupService(new PlayerRepository(), states);
        MatchLineup home = lineups.selectMatchLineup(match.getId(), match.getHomeTeam().getId());
        MatchLineup away = lineups.selectMatchLineup(match.getId(), match.getAwayTeam().getId());
        MatchTacticsRepository tactics = new MatchTacticsRepository();
        String homeFormation = tactics.findFormation(match.getId(), match.getHomeTeam().getId());
        String awayFormation = tactics.findFormation(match.getId(), match.getAwayTeam().getId());
        VBox content = page("PREVIA DEL PARTIDO", match.getCompetition().getName() + "  •  "
                + match.getDate() + "  •  " + match.getHomeTeam().getStadiumName());
        VBox homePanel = previewTeam(match.getHomeTeam(), home, homeFormation, states,
                match.getHomeTeam().getId() == controlledTeamId);
        VBox awayPanel = previewTeam(match.getAwayTeam(), away, awayFormation, states,
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
        content.getChildren().addAll(matchup, new FlowPane(12, 12, lineup, start));
        showCareerShell(stage, new ScrollPane(content));
    }

    private VBox previewTeam(Team team, MatchLineup lineup, String formation,
            PlayerStateRepository states, boolean controlled) {
        double average = lineup.getStarters().stream().mapToInt(Player::getOverall)
                .average().orElse(0);
        double fitness = lineup.getStarters().stream()
                .map(player -> states.findByPlayer(player.getId()))
                .filter(java.util.Objects::nonNull).mapToInt(PlayerState::getFitness)
                .average().orElse(0);
        VBox players = new VBox(5);
        lineup.getStarters().forEach(player -> players.getChildren().add(label(
                player.getPosition() + "  •  " + player.getFullName() + "  •  GRL "
                        + player.getOverall(), "preview-player")));
        VBox panel = panel(controlled ? "TU EQUIPO" : "RIVAL");
        panel.getStyleClass().add(controlled ? "preview-user-team" : "preview-team");
        panel.getChildren().addAll(label(team.getName(), "match-team-name"),
                label(formation + "  •  GRL medio " + String.format("%.1f", average)
                        + "  •  Fitness " + String.format("%.0f", fitness), "match-metadata"),
                players);
        return panel;
    }

    private void showLiveMatch(Stage stage, Match pendingMatch) {
        careerService.simulateControlledMatchesToday(career);
        MatchReport report = new MatchReportService().build(pendingMatch.getId());
        Match match = report.getMatch();
        VBox content = page("PARTIDO EN DIRECTO", match.getDate().toString());
        Label minute = label("0'", "live-minute");
        Label score = label(match.getHomeTeam().getShortName() + "  0 — 0  "
                + match.getAwayTeam().getShortName(), "score-number");
        VBox eventFeed = panel("ACCIONES");
        Button reportButton = button("VER INFORME COMPLETO", "primary-button");
        reportButton.setDisable(true);
        reportButton.setOnAction(event -> showMatchReport(stage, match));
        Button pause = button("PAUSAR", "secondary-button");
        Button finish = button("IR AL FINAL", "ghost-button");
        ComboBox<String> speed = new ComboBox<>();
        speed.getItems().addAll("0.5x", "1x", "2x", "4x");
        speed.setValue("1x");
        VBox livePanel = panel("MARCADOR");
        livePanel.setAlignment(Pos.CENTER);
        livePanel.getChildren().addAll(minute, score);

        int[] currentMinute = {0};
        int[] homeGoals = {0};
        int[] awayGoals = {0};
        java.util.Set<Long> revealed = new java.util.HashSet<>();
        PlayerRepository playerRepository = new PlayerRepository();
        java.util.function.IntConsumer revealUntil = targetMinute -> report.getEvents().stream()
                .filter(matchEvent -> matchEvent.getMinute() <= targetMinute)
                .filter(matchEvent -> revealed.add(matchEvent.getId()))
                .forEach(matchEvent -> {
                    Player player = playerRepository.findById(matchEvent.getPlayer().getId());
                    eventFeed.getChildren().add(label(matchEvent.getMinute() + "'  "
                            + eventLabel(matchEvent.getType()) + "  •  "
                            + player.getFullName(), "event-row"));
                    if (matchEvent.getType()
                            == footballcareer.model.enums.MatchEventType.GOAL) {
                        if (matchEvent.getTeam().getId() == match.getHomeTeam().getId())
                            homeGoals[0]++;
                        else awayGoals[0]++;
                        score.setText(match.getHomeTeam().getShortName() + "  "
                                + homeGoals[0] + " — " + awayGoals[0] + "  "
                                + match.getAwayTeam().getShortName());
                    }
                });
        Runnable showFinal = () -> {
            revealUntil.accept(90);
            currentMinute[0] = 90;
            minute.setText("FINAL");
            score.setText(match.getHomeTeam().getShortName() + "  "
                    + match.getHomeGoals() + " — " + match.getAwayGoals() + "  "
                    + match.getAwayTeam().getShortName());
            pause.setDisable(true);
            finish.setDisable(true);
            speed.setDisable(true);
            reportButton.setDisable(false);
        };
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(110), event -> {
                    currentMinute[0] = Math.min(90, currentMinute[0] + 2);
                    minute.setText(currentMinute[0] + "'");
                    revealUntil.accept(currentMinute[0]);
                }));
        timeline.setCycleCount(45);
        timeline.setOnFinished(event -> showFinal.run());
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
        content.getChildren().addAll(livePanel,
                new FlowPane(12, 12, pause, speed, finish), eventFeed, reportButton);
        showCareerShell(stage, content);
        activeAnimation = timeline;
        timeline.play();
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
            VBox card = formCard("NO SE PUDO COMPLETAR LA OPERACIÓN",
                    failure == null || failure.getMessage() == null
                            ? "Ha ocurrido un error inesperado."
                            : failure.getMessage());
            Button menu = wideButton("VOLVER AL MENÚ", "primary-button");
            menu.setOnAction(action -> showMainMenu(stage));
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
        VBox holder = new VBox(node);
        holder.setFillWidth(false);
        holder.setAlignment(Pos.CENTER);
        StackPane canvas = new StackPane(holder);
        canvas.getStyleClass().add(rootStyle);
        canvas.setPadding(new Insets(32));
        canvas.setMinSize(0, 0);
        ScrollPane scroll = new ScrollPane(canvas);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setPannable(true);
        scroll.getStyleClass().add("screen-scroll");
        return scroll;
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
        return button;
    }

    private Button wideButton(String text, String style) {
        Button button = button(text, style);
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private Button navButton(String text, String section) {
        Button button = wideButton(text, "nav-button");
        if (section.equals(activeSection)) button.getStyleClass().add("nav-button-active");
        return button;
    }

    private ListCell<Team> teamCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Team team, boolean empty) {
                super.updateItem(team, empty);
                setText(empty || team == null ? null
                        : team.getName() + "  •  " + team.getCountry());
            }
        };
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

    private ListView<Player> marketPlayerList(java.util.Map<Long, Double> marketPrices,
            java.util.Map<Long, Team> marketTeams, java.util.Set<Long> shortlistIds) {
        ListView<Player> list = new ListView<>();
        list.setCellFactory(view -> new ListCell<>() {
            @Override protected void updateItem(Player player, boolean empty) {
                super.updateItem(player, empty);
                Double price = empty || player == null ? null
                        : marketPrices.get(player.getId());
                Team seller = empty || player == null ? null : marketTeams.get(player.getId());
                setText(empty || player == null ? null
                        : (shortlistIds.contains(player.getId()) ? "★  " : "")
                        + player.getPosition() + "  •  "
                        + player.getFullName() + "  •  GRL " + player.getOverall()
                        + (seller == null ? "" : "  •  " + seller.getShortName())
                        + "  •  Precio €" + String.format("%.1fM", price / 1_000_000));
            }
        });
        return list;
    }

    private ListView<Player> ownMarketPlayerList(PlayerMarketRepository marketRepository) {
        ListView<Player> list = new ListView<>();
        list.setCellFactory(view -> new ListCell<>() {
            @Override protected void updateItem(Player player, boolean empty) {
                super.updateItem(player, empty);
                if (empty || player == null) setText(null);
                else {
                    Double price = marketRepository.findAskingPrice(player.getId());
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

    private ListView<TransferOffer> incomingOfferList() {
        ListView<TransferOffer> list = new ListView<>();
        list.setCellFactory(view -> new ListCell<>() {
            @Override protected void updateItem(TransferOffer offer, boolean empty) {
                super.updateItem(offer, empty);
                if (empty || offer == null) setText(null);
                else {
                    Player player = new PlayerRepository().findById(offer.getPlayer().getId());
                    Team buyer = new TeamRepository().findById(offer.getBuyingTeam().getId());
                    setText(player.getFullName() + "  •  " + buyer.getName()
                            + "  •  €" + String.format("%.1fM", offer.getAmount() / 1_000_000)
                            + "  •  " + offer.getOfferDate());
                }
            }
        });
        return list;
    }

    private void setScene(Stage stage, javafx.scene.Parent root) {
        if (activeAnimation != null) {
            activeAnimation.stop();
            activeAnimation = null;
        }
        if (appScene == null) {
            appScene = new Scene(root, 1180, 760);
            appScene.getStylesheets().add(
                    getClass().getResource("/styles/app.css").toExternalForm());
            appScene.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.F11) {
                    stage.setFullScreen(!stage.isFullScreen());
                }
            });
            stage.setScene(appScene);
        } else {
            appScene.setRoot(root);
        }
    }

    public static void main(String[] args) { launch(); }
}
