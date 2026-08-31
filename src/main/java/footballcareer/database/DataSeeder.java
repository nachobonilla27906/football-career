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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class DataSeeder {

    public static void seed() {

        boolean compact = Boolean.getBoolean("footballcareer.seed.compact");

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

        Map<String, Team> teams = seedTeams(teamRepository, compact
                ? "data/teams.csv" : "data/teams_top5_2026_27.csv");

        Map<String, Competition> competitions =
                seedCompetitions(
                        competitionRepository,
                        seasons,
                        leagues
                );

        seedCompetitionTeams(
                competitionTeamRepository,
                competitions,
                teams,
                compact ? "data/competition_teams.csv"
                        : "data/competition_teams_top5_2026_27.csv"
        );

        if (compact) {
            seedPlayers(playerRepository, playerTeamRepository, seasons, teams, "data/players.csv");
            seedPlayers(playerRepository, playerTeamRepository, seasons, teams, "data/players_premier_league.csv");
            seedPlayers(playerRepository, playerTeamRepository, seasons, teams, "data/players_top5_2025_26.csv");
        } else {
            seedPlayersBulk(seasons, teams, "data/players_top5_2026_27.csv");
        }

        if (!compact) {
            seasons.values().forEach(season -> new EuropeanCompetitionSeeder().seed(season));
        }

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
            Map<String, Team> teams,
            String resourcePath
    ) {

        competitions.values().forEach(competition ->
                repository.clearTeams(competition.getId()));

        try (BufferedReader reader =
                     openFile(resourcePath)) {

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
            TeamRepository repository,
            String resourcePath
    ) {

        Map<String, Team> teams = new HashMap<>();

        try (BufferedReader reader =
                     openFile(resourcePath)) {

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

                Player player = playerRepository.findByIdentity(
                                firstName,
                                lastName,
                                birthDate
                        );

                Player seedPlayer = new Player(
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
                seedPlayer.setHeightCm(data.length > 17 && !data[17].isBlank()
                        ? Integer.parseInt(data[17])
                        : heightFor(seedPlayer.getPosition(), firstName + lastName));
                seedPlayer.setSecondaryPosition(secondaryPosition(seedPlayer.getPosition()));

                if (player == null) {
                    player = seedPlayer;
                    playerRepository.save(player);
                } else {
                    seedPlayer.setId(player.getId());
                    playerRepository.updateSeedData(seedPlayer);
                    player = seedPlayer;
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

                playerTeamRepository.ensureInitialAssignment(
                        player.getId(), team.getId(), season.getStartDate());

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

    private static int heightFor(Position position, String identity) {
        int base = switch (position) {
            case GK -> 189; case CB, ST -> 184; case CDM -> 181;
            case LB, RB -> 177; default -> 175;
        };
        return base + Math.floorMod(identity.hashCode(), 7) - 3;
    }

    private static void seedPlayersBulk(Map<String, Season> seasons, Map<String, Team> teams,
            String resourcePath) {
        Season season = seasons.values().stream().findFirst().orElseThrow();
        String insertSql = """
                INSERT INTO players (first_name, last_name, birth_date, nationality, position,
                    preferred_foot, height_cm, secondary_position, overall, potential, pace,
                    shooting, passing, dribbling, defending, physical, market_value, salary)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String updateSql = """
                UPDATE players SET nationality=?, position=?, preferred_foot=?, height_cm=?,
                    secondary_position=?, overall=?, potential=?, pace=?, shooting=?, passing=?,
                    dribbling=?, defending=?, physical=?, market_value=?, salary=? WHERE id=?
                """;
        try (BufferedReader reader = openFile(resourcePath);
             Connection connection = Database.getConnection();
             PreparedStatement find = connection.prepareStatement(
                     "SELECT id FROM players WHERE first_name=? AND last_name=? AND birth_date=?");
             PreparedStatement insert = connection.prepareStatement(insertSql,
                     Statement.RETURN_GENERATED_KEYS);
             PreparedStatement update = connection.prepareStatement(updateSql);
             PreparedStatement initial = connection.prepareStatement("""
                     INSERT INTO initial_player_team (player_id, team_id, start_date) VALUES (?, ?, ?)
                     ON CONFLICT(player_id) DO UPDATE SET team_id=excluded.team_id,
                         start_date=excluded.start_date
                     """);
             PreparedStatement closeOld = connection.prepareStatement("""
                     UPDATE player_team SET end_date=? WHERE player_id=? AND end_date IS NULL
                         AND team_id<>?
                     """);
             PreparedStatement assign = connection.prepareStatement("""
                     INSERT INTO player_team (player_id, team_id, start_date, end_date)
                     VALUES (?, ?, ?, NULL)
                     ON CONFLICT(player_id, team_id, start_date) DO UPDATE SET end_date=NULL
                     """)) {
            connection.setAutoCommit(false);
            java.util.Set<Long> desiredPlayerIds = new java.util.HashSet<>();
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] data = line.split(",", -1);
                Player player = playerFromData(data);
                find.setString(1, player.getFirstName()); find.setString(2, player.getLastName());
                find.setString(3, player.getBirthDate().toString());
                try (ResultSet result = find.executeQuery()) {
                    if (result.next()) {
                        player.setId(result.getLong(1)); bindPlayerUpdate(update, player);
                        update.executeUpdate();
                    } else {
                        bindPlayerInsert(insert, player); insert.executeUpdate();
                        try (ResultSet keys = insert.getGeneratedKeys()) {
                            if (!keys.next()) throw new SQLException("Missing generated player id");
                            player.setId(keys.getLong(1));
                        }
                    }
                }
                desiredPlayerIds.add(player.getId());
                Team team = teams.get(data[16]);
                if (team == null) throw new IllegalStateException("Team not found: " + data[16]);
                String date = season.getStartDate().toString();
                initial.setLong(1, player.getId()); initial.setLong(2, team.getId());
                initial.setString(3, date); initial.executeUpdate();
                closeOld.setString(1, date); closeOld.setLong(2, player.getId());
                closeOld.setLong(3, team.getId()); closeOld.executeUpdate();
                assign.setLong(1, player.getId()); assign.setLong(2, team.getId());
                assign.setString(3, date); assign.executeUpdate();
            }
            removeLegacySeedMemberships(connection, desiredPlayerIds);
            connection.commit();
            PlayerRepository.clearReadCache();
        } catch (IOException | SQLException exception) {
            throw new RuntimeException("Could not bulk seed players.", exception);
        }
    }

    /**
     * A full population replaces the old compact roster. Players are retained as historical
     * entities, but obsolete seed memberships must not leak into new or existing careers.
     */
    private static void removeLegacySeedMemberships(Connection connection,
            java.util.Set<Long> desiredPlayerIds) throws SQLException {
        try (Statement setup = connection.createStatement()) {
            setup.executeUpdate("DROP TABLE IF EXISTS temp.desired_seed_players");
            setup.executeUpdate("CREATE TEMP TABLE desired_seed_players "
                    + "(player_id INTEGER PRIMARY KEY)");
        }
        try (PreparedStatement desired = connection.prepareStatement(
                "INSERT INTO desired_seed_players (player_id) VALUES (?)")) {
            for (Long playerId : desiredPlayerIds) {
                desired.setLong(1, playerId);
                desired.addBatch();
            }
            desired.executeBatch();
        }
        String stalePlayers = """
                SELECT ip.player_id FROM initial_player_team ip
                LEFT JOIN desired_seed_players desired ON desired.player_id = ip.player_id
                WHERE desired.player_id IS NULL
                """;
        try (Statement cleanup = connection.createStatement()) {
            cleanup.executeUpdate("DELETE FROM career_player_team WHERE player_id IN ("
                    + stalePlayers + ")");
            cleanup.executeUpdate("DELETE FROM player_team WHERE player_id IN ("
                    + stalePlayers + ")");
            cleanup.executeUpdate("DELETE FROM initial_player_team WHERE player_id IN ("
                    + stalePlayers + ")");
        }
    }

    private static Player playerFromData(String[] data) {
        Player player = new Player(0, data[0], data[1], LocalDate.parse(data[2]), data[3],
                Position.valueOf(data[4]), PreferredFoot.valueOf(data[5]),
                Integer.parseInt(data[6]), Integer.parseInt(data[7]), Integer.parseInt(data[8]),
                Integer.parseInt(data[9]), Integer.parseInt(data[10]), Integer.parseInt(data[11]),
                Integer.parseInt(data[12]), Integer.parseInt(data[13]), Double.parseDouble(data[14]),
                Double.parseDouble(data[15]));
        player.setHeightCm(data.length > 17 ? Integer.parseInt(data[17])
                : heightFor(player.getPosition(), data[0] + data[1]));
        player.setSecondaryPosition(secondaryPosition(player.getPosition()));
        return player;
    }

    private static void bindPlayerInsert(PreparedStatement statement, Player player)
            throws SQLException {
        statement.setString(1, player.getFirstName()); statement.setString(2, player.getLastName());
        statement.setString(3, player.getBirthDate().toString());
        statement.setString(4, player.getNationality()); statement.setString(5, player.getPosition().name());
        statement.setString(6, player.getPreferredFoot().name()); statement.setInt(7, player.getHeightCm());
        bindNullablePosition(statement, 8, player); statement.setInt(9, player.getOverall());
        statement.setInt(10, player.getPotential()); statement.setInt(11, player.getPace());
        statement.setInt(12, player.getShooting()); statement.setInt(13, player.getPassing());
        statement.setInt(14, player.getDribbling()); statement.setInt(15, player.getDefending());
        statement.setInt(16, player.getPhysical()); statement.setDouble(17, player.getMarketValue());
        statement.setDouble(18, player.getSalary());
    }

    private static void bindPlayerUpdate(PreparedStatement statement, Player player)
            throws SQLException {
        statement.setString(1, player.getNationality()); statement.setString(2, player.getPosition().name());
        statement.setString(3, player.getPreferredFoot().name()); statement.setInt(4, player.getHeightCm());
        bindNullablePosition(statement, 5, player); statement.setInt(6, player.getOverall());
        statement.setInt(7, player.getPotential()); statement.setInt(8, player.getPace());
        statement.setInt(9, player.getShooting()); statement.setInt(10, player.getPassing());
        statement.setInt(11, player.getDribbling()); statement.setInt(12, player.getDefending());
        statement.setInt(13, player.getPhysical()); statement.setDouble(14, player.getMarketValue());
        statement.setDouble(15, player.getSalary()); statement.setLong(16, player.getId());
    }

    private static void bindNullablePosition(PreparedStatement statement, int index, Player player)
            throws SQLException {
        if (player.getSecondaryPosition() == null) statement.setNull(index, java.sql.Types.VARCHAR);
        else statement.setString(index, player.getSecondaryPosition().name());
    }

    private static Position secondaryPosition(Position position) {
        return switch (position) {
            case GK -> null; case CB -> Position.CDM; case LB, RB -> Position.CB;
            case CDM -> Position.CM; case CM -> Position.CAM; case CAM -> Position.CM;
            case LW -> Position.RW; case RW -> Position.LW; case ST -> Position.LW;
        };
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
