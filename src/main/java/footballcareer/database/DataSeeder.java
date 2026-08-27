package footballcareer.database;

import footballcareer.model.League;
import footballcareer.model.Competition;
import footballcareer.model.Player;
import footballcareer.model.Season;
import footballcareer.model.Team;
import footballcareer.model.enums.Position;
import footballcareer.model.enums.PreferredFoot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class DataSeeder {

    public static void seed() {

        SeasonRepository seasonRepository = new SeasonRepository();
        LeagueRepository leagueRepository = new LeagueRepository();
        TeamRepository teamRepository = new TeamRepository();
        PlayerRepository playerRepository = new PlayerRepository();
        PlayerTeamRepository playerTeamRepository =
                new PlayerTeamRepository();
        CompetitionRepository competitionRepository =
                new CompetitionRepository();
        CompetitionTeamRepository competitionTeamRepository =
                new CompetitionTeamRepository();
        ContractRepository contractRepository = new ContractRepository();

        Map<String, Season> seasons =
                seedSeasons(seasonRepository);

        Map<String, League> leagues =
                seedLeagues(leagueRepository);

        Map<String, Team> teams =
                seedTeams(teamRepository);

        Map<String, Competition> competitions =
                seedCompetitions(
                        competitionRepository,
                        seasons,
                        leagues
                );

        seedCompetitionTeams(
                competitionTeamRepository,
                competitions,
                teams
        );

        seedPlayers(
                playerRepository,
                playerTeamRepository,
                seasons,
                teams,
                "data/players.csv"
        );

        seedPlayers(
                playerRepository,
                playerTeamRepository,
                seasons,
                teams,
                "data/players_premier_league.csv"
        );

        seedPlayers(
                playerRepository,
                playerTeamRepository,
                seasons,
                teams,
                "data/players_top5_2025_26.csv"
        );

        Season contractSeason = seasons.values().stream()
                .findFirst().orElseThrow();
        contractRepository.initializeMissingContracts(
                contractSeason.getStartDate(),
                LocalDate.of(2030, 6, 30)
        );

        new PlayerStateRepository().initializeMissingStates();
        new ClubFinanceRepository().initializeMissingFinances();
        new PlayerMarketRepository().initializeMissingStatuses();
    }

    private static Map<String, Competition> seedCompetitions(
            CompetitionRepository repository,
            Map<String, Season> seasons,
            Map<String, League> leagues
    ) {

        Map<String, Competition> competitions = new HashMap<>();

        try (BufferedReader reader =
                     openFile("data/competitions.csv")) {

            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {

                if (line.isBlank()) {
                    continue;
                }

                String[] data = line.split(",", -1);
                String name = data[0];
                String seasonKey = data[3];
                Season season = seasons.get(seasonKey);
                League league = leagues.get(data[4]);

                if (season == null) {
                    throw new RuntimeException(
                            "Season not found: " + seasonKey
                    );
                }

                if (league == null) {
                    throw new RuntimeException(
                            "League not found: " + data[4]
                    );
                }

                Competition competition =
                        repository.findByNameAndSeason(
                                name,
                                season.getId()
                        );

                if (competition == null) {
                    competition = new Competition(
                            0,
                            name,
                            data[1],
                            Integer.parseInt(data[2]),
                            season,
                            league
                    );

                    repository.save(competition);
                }

                competitions.put(
                        competitionKey(name, seasonKey),
                        competition
                );
            }

        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not seed competitions.",
                    e
            );
        }

        return competitions;
    }

    private static void seedCompetitionTeams(
            CompetitionTeamRepository repository,
            Map<String, Competition> competitions,
            Map<String, Team> teams
    ) {

        try (BufferedReader reader =
                     openFile("data/competition_teams.csv")) {

            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {

                if (line.isBlank()) {
                    continue;
                }

                String[] data = line.split(",", -1);
                Competition competition = competitions.get(
                        competitionKey(data[0], data[1])
                );
                Team team = teams.get(data[2]);

                if (competition == null) {
                    throw new RuntimeException(
                            "Competition not found: " + data[0]
                    );
                }

                if (team == null) {
                    throw new RuntimeException(
                            "Team not found: " + data[2]
                    );
                }

                repository.addTeamToCompetition(
                        competition.getId(),
                        team.getId()
                );
            }

        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not seed competition teams.",
                    e
            );
        }
    }

    private static String competitionKey(
            String competitionName,
            String seasonKey
    ) {
        return competitionName + "|" + seasonKey;
    }

    private static Map<String, Season> seedSeasons(
            SeasonRepository repository
    ) {

        Map<String, Season> seasons = new HashMap<>();

        try (BufferedReader reader =
                     openFile("data/seasons.csv")) {

            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {

                if (line.isBlank()) {
                    continue;
                }

                String[] data = line.split(",", -1);

                int startYear = Integer.parseInt(data[0]);
                int endYear = Integer.parseInt(data[1]);

                Season season = findSeason(
                        repository,
                        startYear,
                        endYear
                );

                if (season == null) {

                    season = new Season();

                    season.setStartYear(startYear);
                    season.setEndYear(endYear);
                    season.setStartDate(
                            LocalDate.parse(data[2])
                    );
                    season.setEndDate(
                            LocalDate.parse(data[3])
                    );
                    season.setFinished(
                            "1".equals(data[4])
                    );

                    repository.save(season);
                }

                String key =
                        startYear + "/" + endYear;

                seasons.put(key, season);
            }

        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not seed seasons.",
                    e
            );
        }

        return seasons;
    }

    private static Season findSeason(
            SeasonRepository repository,
            int startYear,
            int endYear
    ) {

        Season season = repository.findFirst();

        if (season != null
                && season.getStartYear() == startYear
                && season.getEndYear() == endYear) {

            return season;
        }

        return null;
    }

    private static Map<String, League> seedLeagues(
            LeagueRepository repository
    ) {

        Map<String, League> leagues = new HashMap<>();

        try (BufferedReader reader =
                     openFile("data/leagues.csv")) {

            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {

                if (line.isBlank()) {
                    continue;
                }

                String[] data = line.split(",", -1);

                String name = data[0];
                String country = data[1];
                int tier = Integer.parseInt(data[2]);

                League league =
                        repository.findByName(
                                name,
                                country
                        );

                if (league == null) {

                    league = new League();

                    league.setName(name);
                    league.setCountry(country);
                    league.setTier(tier);

                    repository.save(league);
                }

                leagues.put(name, league);
            }

        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not seed leagues.",
                    e
            );
        }

        return leagues;
    }

    private static Map<String, Team> seedTeams(
            TeamRepository repository
    ) {

        Map<String, Team> teams = new HashMap<>();

        try (BufferedReader reader =
                     openFile("data/teams.csv")) {

            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {

                if (line.isBlank()) {
                    continue;
                }

                String[] data = line.split(",", -1);

                String name = data[0];
                String shortName = data[1];
                String country = data[2];

                Team team =
                        repository.findByShortName(
                                shortName
                        );

                if (team == null) {

                    team = new Team();

                    team.setName(name);
                    team.setShortName(shortName);
                    team.setCountry(country);
                    team.setStadiumName(data[3]);
                    team.setStadiumCapacity(
                            Integer.parseInt(data[4])
                    );
                    team.setReputation(
                            Integer.parseInt(data[5])
                    );

                    repository.save(team);
                }

                teams.put(shortName, team);
            }

        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not seed teams.",
                    e
            );
        }

        return teams;
    }

    private static void seedPlayers(
            PlayerRepository playerRepository,
            PlayerTeamRepository playerTeamRepository,
            Map<String, Season> seasons,
            Map<String, Team> teams,
            String resourcePath
    ) {

        Season season = seasons.values()
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException(
                                "No season available for players."
                        )
                );

        try (BufferedReader reader =
                     openFile(resourcePath)) {

            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {

                if (line.isBlank()) {
                    continue;
                }

                String[] data = line.split(",", -1);

                String firstName = data[0];
                String lastName = data[1];
                LocalDate birthDate = LocalDate.parse(data[2]);

                Player player =
                        playerRepository.findByIdentity(
                                firstName,
                                lastName,
                                birthDate
                        );

                if (player == null) {

                    player = new Player(
                            0,
                            firstName,
                            lastName,
                            birthDate,
                            data[3],
                            Position.valueOf(data[4]),
                            PreferredFoot.valueOf(data[5]),
                            Integer.parseInt(data[6]),
                            Integer.parseInt(data[7]),
                            Integer.parseInt(data[8]),
                            Integer.parseInt(data[9]),
                            Integer.parseInt(data[10]),
                            Integer.parseInt(data[11]),
                            Integer.parseInt(data[12]),
                            Integer.parseInt(data[13]),
                            Double.parseDouble(data[14]),
                            Double.parseDouble(data[15])
                    );

                    playerRepository.save(player);
                }

                String teamShortName = data[16];

                Team team = teams.get(teamShortName);

                if (team == null) {
                    throw new RuntimeException(
                            "Team not found: "
                                    + teamShortName
                    );
                }

                Long currentTeamId =
                        playerTeamRepository
                                .findCurrentTeamId(
                                        player.getId()
                                );

                if (currentTeamId == null) {

                    playerTeamRepository.assignPlayerToTeam(
                            player.getId(),
                            team.getId(),
                            season.getStartDate()
                    );
                }

            }

        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not seed players.",
                    e
            );
        }
    }

    private static BufferedReader openFile(
            String path
    ) {

        InputStream inputStream =
                DataSeeder.class
                        .getClassLoader()
                        .getResourceAsStream(path);

        if (inputStream == null) {
            throw new RuntimeException(
                    "Could not find resource: " + path
            );
        }

        return new BufferedReader(
                new InputStreamReader(
                        inputStream,
                        StandardCharsets.UTF_8
                )
        );
    }
}
