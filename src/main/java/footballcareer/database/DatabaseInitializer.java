package footballcareer.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    private static final Path TEST_DATABASE = Path.of("target", "football-career-test.db");
    private static final Path SEEDED_TEST_TEMPLATE =
            Path.of("target", "football-career-seeded-template.db");

    public static void initialize() {

        try (Connection connection = Database.getConnection();
             InputStream inputStream = DatabaseInitializer.class
                     .getClassLoader()
                     .getResourceAsStream("schema.sql")) {

            if (inputStream == null) {
                throw new RuntimeException(
                        "Could not find schema.sql."
                );
            }

            String schema = new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            try (Statement statement = connection.createStatement()) {

                statement.execute("PRAGMA foreign_keys = OFF");

                for (String sql : schema.split(";")) {

                    String trimmedSql = sql.trim();

                    if (!trimmedSql.isEmpty()) {
                        statement.execute(trimmedSql);
                    }
                }

                statement.execute("PRAGMA foreign_keys = ON");

                try {
                    statement.execute("ALTER TABLE transfer_offers ADD COLUMN counter_amount REAL");
                } catch (SQLException ignored) {
                    // Column already exists on new or previously migrated databases.
                }
                addColumnIfMissing(statement, "transfer_offers", "career_id INTEGER");
                addColumnIfMissing(statement, "transfer_offers", "resolution_reason TEXT");
                addColumnIfMissing(statement, "player_state", "unavailable_until TEXT");
                addColumnIfMissing(statement, "player_state", "unavailable_reason TEXT");
                addColumnIfMissing(statement, "career_player_state", "unavailable_until TEXT");
                addColumnIfMissing(statement, "career_player_state", "unavailable_reason TEXT");
                addColumnIfMissing(statement, "match_tactics", "mentality TEXT NOT NULL DEFAULT 'BALANCED'");
                addColumnIfMissing(statement, "match_tactics", "pressing TEXT NOT NULL DEFAULT 'MEDIUM'");
                addColumnIfMissing(statement, "match_tactics", "tempo TEXT NOT NULL DEFAULT 'NORMAL'");
                addColumnIfMissing(statement, "career_match_tactics", "mentality TEXT NOT NULL DEFAULT 'BALANCED'");
                addColumnIfMissing(statement, "career_match_tactics", "pressing TEXT NOT NULL DEFAULT 'MEDIUM'");
                addColumnIfMissing(statement, "career_match_tactics", "tempo TEXT NOT NULL DEFAULT 'NORMAL'");
                addColumnIfMissing(statement, "career_preferences", "difficulty TEXT NOT NULL DEFAULT 'NORMAL'");
                addColumnIfMissing(statement, "career_preferences", "manager_identity TEXT NOT NULL DEFAULT 'GENERALIST'");
                addColumnIfMissing(statement, "transfers", "career_id INTEGER");
                addColumnIfMissing(statement, "contracts", "signing_bonus REAL NOT NULL DEFAULT 0");
                addColumnIfMissing(statement, "contracts", "release_clause REAL");
                addColumnIfMissing(statement, "contracts", "squad_role TEXT NOT NULL DEFAULT 'ROTATION'");
                addColumnIfMissing(statement, "career_contracts", "signing_bonus REAL NOT NULL DEFAULT 0");
                addColumnIfMissing(statement, "career_contracts", "release_clause REAL");
                addColumnIfMissing(statement, "career_contracts", "squad_role TEXT NOT NULL DEFAULT 'ROTATION'");
                addColumnIfMissing(statement, "players", "height_cm INTEGER NOT NULL DEFAULT 180");
                addColumnIfMissing(statement, "players", "secondary_position TEXT");
                addColumnIfMissing(statement, "transfer_offers", "upfront_percent INTEGER NOT NULL DEFAULT 100");
                addColumnIfMissing(statement, "transfer_offers", "appearance_bonus REAL NOT NULL DEFAULT 0");
                for (String table : new String[]{"match_team_stats", "career_match_team_stats"}) {
                    addColumnIfMissing(statement, table, "expected_goals REAL NOT NULL DEFAULT 0");
                    addColumnIfMissing(statement, table, "passes INTEGER NOT NULL DEFAULT 0");
                    addColumnIfMissing(statement, table, "pass_accuracy INTEGER NOT NULL DEFAULT 0");
                    addColumnIfMissing(statement, table, "tackles INTEGER NOT NULL DEFAULT 0");
                }
                statement.executeUpdate("""
                        UPDATE players SET secondary_position = CASE position
                            WHEN 'GK' THEN NULL WHEN 'CB' THEN 'CDM'
                            WHEN 'LB' THEN 'CB' WHEN 'RB' THEN 'CB'
                            WHEN 'CDM' THEN 'CM' WHEN 'CM' THEN 'CAM'
                            WHEN 'CAM' THEN 'CM' WHEN 'LW' THEN 'RW'
                            WHEN 'RW' THEN 'LW' WHEN 'ST' THEN 'LW' END
                        WHERE secondary_position IS NULL
                        """);
            }

            System.out.println("Database initialized successfully.");

        } catch (IOException | SQLException e) {
            throw new RuntimeException(
                    "Could not initialize database.",
                    e
            );
        }
    }

    private static void addColumnIfMissing(Statement statement, String table, String definition) {
        try {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + definition);
        } catch (SQLException ignored) {
            // Present in a new schema or added by an earlier launch.
        }
    }

    public static void resetForTests() {

        CareerContext.clear();
        PlayerRepository.clearReadCache();

        String[] tables = {
                "training_sessions",
                "career_youth_candidates",
                "career_staff",
                "career_manager_reputation",
                "career_scouts",
                "medical_treatments",
                "player_conversations",
                "career_shortlist",
                "career_preferences",
                "careers",
                "career_loans",
                "transfers",
                "transfer_negotiation_rounds",
                "transfer_obligations",
                "transfer_offers",
                "career_contracts",
                "contracts",
                "career_player_state",
                "career_player_development",
                "career_player_progress_history",
                "player_state",
                "career_club_finances",
                "club_finances",
                "career_player_market_status",
                "player_market_status",
                "league_standings",
                "career_player_season_stats",
                "player_season_stats",
                "career_match_tactics",
                "career_match_roles",
                "career_team_sheet_players",
                "career_team_sheets",
                "career_match_lineups",
                "career_match_team_stats",
                "career_match_events",
                "match_tactics",
                "match_roles",
                "match_lineups",
                "match_team_stats",
                "match_events",
                "career_match_states",
                "matches",
                "career_player_team",
                "initial_player_team",
                "player_team",
                "competition_teams",
                "competitions",
                "players",
                "teams",
                "leagues",
                "seasons"
        };

        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA foreign_keys = OFF");

            for (String table : tables) {
                statement.executeUpdate(
                        "DROP TABLE IF EXISTS " + table
                );
            }

            statement.execute("PRAGMA foreign_keys = ON");

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not reset database for tests.",
                    e
            );
        }

        initialize();
    }

    /**
     * Restores a pristine, fully seeded test world. The expensive CSV import is
     * performed once per Maven build; subsequent tests copy the SQLite template.
     */
    public static synchronized void resetAndSeedForTests() {
        String url = System.getProperty(Database.DATABASE_URL_PROPERTY, "").replace('\\', '/');
        if (!url.endsWith("target/football-career-test.db")) {
            throw new IllegalStateException("Seeded test reset requires the isolated test database.");
        }
        try {
            CareerContext.clear();
            if (templateIsMissingOrStale()) {
                resetForTests();
                DataSeeder.seed();
                Files.copy(TEST_DATABASE, SEEDED_TEST_TEMPLATE,
                        StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.deleteIfExists(Path.of(TEST_DATABASE + "-wal"));
                Files.deleteIfExists(Path.of(TEST_DATABASE + "-shm"));
                Files.copy(SEEDED_TEST_TEMPLATE, TEST_DATABASE,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new RuntimeException("Could not restore the seeded test database.", exception);
        }
    }

    private static boolean templateIsMissingOrStale() throws IOException {
        if (Files.notExists(SEEDED_TEST_TEMPLATE)) return true;
        long templateTime = Files.getLastModifiedTime(SEEDED_TEST_TEMPLATE).toMillis();
        Path schema = Path.of("src", "main", "resources", "schema.sql");
        if (Files.exists(schema) && modifiedAfter(schema, templateTime)) return true;
        Path seedData = Path.of("src", "main", "resources", "data");
        try (var files = Files.walk(seedData)) {
            if (files.filter(Files::isRegularFile).anyMatch(path -> modifiedAfter(path, templateTime))) {
                return true;
            }
        }
        Path seederClass = Path.of("target", "classes", "footballcareer", "database",
                "DataSeeder.class");
        return Files.exists(seederClass) && modifiedAfter(seederClass, templateTime);
    }

    private static boolean modifiedAfter(Path path, long timestamp) {
        try {
            return Files.getLastModifiedTime(path).toMillis() > timestamp;
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
