package footballcareer;

import footballcareer.database.*;
import footballcareer.model.Career;
import footballcareer.model.Match;
import footballcareer.model.Season;
import footballcareer.model.Team;
import footballcareer.service.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {
    private CareerService careerService;
    private MatchRepository matchRepository;
    private Season season;
    private Career career;

    @Override
    public void start(Stage stage) {
        DatabaseInitializer.initialize();
        DataSeeder.seed();
        season = new SeasonRepository().findFirst();
        new FootballWorldService().prepareSeason(season.getId());

        matchRepository = new MatchRepository();
        MatchDayService matchDayService = new MatchDayService(
                matchRepository,
                new LeagueStandingRepository(),
                new MatchSimulationService(),
                new PlayerMatchService(
                        new LineupService(
                                new PlayerRepository(),
                                new PlayerStateRepository()
                        ),
                        new PlayerSeasonStatsRepository(),
                        new PlayerStateRepository()
                )
        );
        careerService = new CareerService(
                new CareerRepository(), new TeamRepository(),
                new SeasonRepository(), matchDayService
        );

        stage.setTitle("Football Career - Alpha");
        showNewCareer(stage);
        stage.show();
    }

    private void showNewCareer(Stage stage) {
        Label title = new Label("FOOTBALL CAREER");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        TextField managerName = new TextField();
        managerName.setPromptText("Nombre del entrenador");
        ComboBox<Team> teamSelector = new ComboBox<>();
        teamSelector.getItems().addAll(careerService.getAvailableTeams());
        teamSelector.setPromptText("Elige un club");
        teamSelector.setCellFactory(list -> teamCell());
        teamSelector.setButtonCell(teamCell());
        Label error = new Label();
        error.setStyle("-fx-text-fill: #c62828;");

        Button create = new Button("Crear carrera");
        create.setOnAction(event -> {
            try {
                Team selectedTeam = teamSelector.getValue();
                if (selectedTeam == null) {
                    throw new IllegalArgumentException("Debes elegir un club.");
                }
                career = careerService.createCareer(
                        managerName.getText(), selectedTeam.getId(), season.getId()
                );
                showDashboard(stage);
            } catch (IllegalArgumentException exception) {
                error.setText(exception.getMessage());
            }
        });

        VBox root = new VBox(14, title, managerName, teamSelector, create, error);
        root.setPadding(new Insets(40));
        stage.setScene(new Scene(root, 900, 600));
    }

    private ListCell<Team> teamCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Team team, boolean empty) {
                super.updateItem(team, empty);
                setText(empty || team == null ? null
                        : team.getName() + " (" + team.getCountry() + ")");
            }
        };
    }

    private void showDashboard(Stage stage) {
        Label title = new Label(career.getControlledTeam().getName());
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");
        Label manager = new Label("Entrenador: " + career.getManagerName());
        Label seasonLabel = new Label("Temporada: " + season.getName());
        Label date = new Label();
        ListView<String> results = new ListView<>();
        results.setPrefHeight(300);

        Runnable refresh = () -> {
            date.setText("Fecha: " + career.getCurrentDate());
            results.getItems().clear();
            for (Match match : matchRepository.findByDate(career.getCurrentDate())) {
                if (match.isPlayed()) {
                    results.getItems().add(match.getHomeTeam().getName() + " "
                            + match.getHomeGoals() + " - " + match.getAwayGoals()
                            + " " + match.getAwayTeam().getName());
                }
            }
            if (results.getItems().isEmpty()) {
                results.getItems().add("No hay partidos jugados hoy.");
            }
        };

        Button advance = new Button("Avanzar un día");
        advance.setOnAction(event -> {
            careerService.advanceDay(career);
            refresh.run();
        });

        Button squad = new Button("Ver plantilla");
        squad.setOnAction(event -> showSquad(stage));

        VBox root = new VBox(12, title, manager, seasonLabel, date, advance, squad,
                new Label("Resultados del día"), results);
        root.setPadding(new Insets(30));
        stage.setScene(new Scene(root, 900, 600));
        refresh.run();
    }

    private void showSquad(Stage stage) {
        Label title = new Label("Plantilla - " + career.getControlledTeam().getName());
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        ListView<String> players = new ListView<>();
        PlayerRepository playerRepository = new PlayerRepository();
        PlayerStateRepository stateRepository = new PlayerStateRepository();

        playerRepository.findCurrentPlayersByTeam(career.getControlledTeam().getId())
                .forEach(player -> {
                    var state = stateRepository.findByPlayer(player.getId());
                    players.getItems().add(
                            player.getPosition() + " | " + player.getFullName()
                                    + " | " + player.getOverall()
                                    + " | Edad " + player.getAge(career.getCurrentDate())
                                    + " | Forma " + state.getForm()
                                    + " | Física " + state.getFitness()
                    );
                });

        Button back = new Button("Volver");
        back.setOnAction(event -> showDashboard(stage));
        VBox root = new VBox(12, title, players, back);
        root.setPadding(new Insets(30));
        stage.setScene(new Scene(root, 900, 600));
    }

    public static void main(String[] args) {
        launch();
    }
}
