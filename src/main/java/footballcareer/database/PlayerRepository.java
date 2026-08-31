package footballcareer.database;

import footballcareer.model.Player;
import footballcareer.model.enums.Position;
import footballcareer.model.enums.PreferredFoot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PlayerRepository {
    private static final java.util.Map<Long, List<Player>> ALL_PLAYERS_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    public void save(Player player) {

        String sql = """
                INSERT INTO players (
                    first_name,
                    last_name,
                    birth_date,
                    nationality,
                    position,
                    preferred_foot,
                    height_cm,
                    secondary_position,
                    overall,
                    potential,
                    pace,
                    shooting,
                    passing,
                    dribbling,
                    defending,
                    physical,
                    market_value,
                    salary
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     java.sql.Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setString(1, player.getFirstName());
            statement.setString(2, player.getLastName());
            statement.setString(3, player.getBirthDate().toString());
            statement.setString(4, player.getNationality());
            statement.setString(5, player.getPosition().name());
            statement.setString(6, player.getPreferredFoot().name());
            statement.setInt(7, player.getHeightCm());
            if (player.getSecondaryPosition() == null) statement.setNull(8, java.sql.Types.VARCHAR);
            else statement.setString(8, player.getSecondaryPosition().name());
            statement.setInt(9, player.getOverall());
            statement.setInt(10, player.getPotential());
            statement.setInt(11, player.getPace());
            statement.setInt(12, player.getShooting());
            statement.setInt(13, player.getPassing());
            statement.setInt(14, player.getDribbling());
            statement.setInt(15, player.getDefending());
            statement.setInt(16, player.getPhysical());
            statement.setDouble(17, player.getMarketValue());
            statement.setDouble(18, player.getSalary());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    player.setId(generatedKeys.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not save player.",
                    e
            );
        }
        clearReadCache();
    }

    public Player findById(long id) {
        String sql = playerSelect() + " WHERE p.id = ?";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                return mapPlayer(resultSet);
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not find player.",
                    e
            );
        }
    }

    public Player findByName(
            String firstName,
            String lastName
    ) {

        String sql = playerSelect() + """
                WHERE p.first_name = ?
                  AND p.last_name = ?
                LIMIT 1
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, firstName);
            statement.setString(2, lastName);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                return mapPlayer(resultSet);
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Could not find player by name.",
                    e
            );
        }
    }

    public void updateSeedData(Player player) {
        String sql = """
                UPDATE players SET nationality = ?, position = ?, preferred_foot = ?,
                    height_cm = ?, secondary_position = ?, overall = ?, potential = ?,
                    pace = ?, shooting = ?, passing = ?, dribbling = ?, defending = ?,
                    physical = ?, market_value = ?, salary = ? WHERE id = ?
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.getNationality());
            statement.setString(2, player.getPosition().name());
            statement.setString(3, player.getPreferredFoot().name());
            statement.setInt(4, player.getHeightCm());
            if (player.getSecondaryPosition() == null) statement.setNull(5, java.sql.Types.VARCHAR);
            else statement.setString(5, player.getSecondaryPosition().name());
            statement.setInt(6, player.getOverall()); statement.setInt(7, player.getPotential());
            statement.setInt(8, player.getPace()); statement.setInt(9, player.getShooting());
            statement.setInt(10, player.getPassing()); statement.setInt(11, player.getDribbling());
            statement.setInt(12, player.getDefending()); statement.setInt(13, player.getPhysical());
            statement.setDouble(14, player.getMarketValue()); statement.setDouble(15, player.getSalary());
            statement.setLong(16, player.getId()); statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not refresh seeded player.", exception);
        }
        clearReadCache();
    }

    public Player findByIdentity(
            String firstName,
            String lastName,
            LocalDate birthDate
    ) {
        String sql = playerSelect() + """
                WHERE p.first_name = ?
                  AND p.last_name = ?
                  AND p.birth_date = ?
                LIMIT 1
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, firstName);
            statement.setString(2, lastName);
            statement.setString(3, birthDate.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapPlayer(resultSet) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find player identity.", e);
        }
    }

    public List<Player> findCurrentPlayersByTeam(long teamId) {
        Long careerId = CareerContext.getCareerId();
        String membership = careerId == null ? "player_team" : "career_player_team";
        String sql = playerSelect() + """
                JOIN %s pt ON p.id = pt.player_id
                WHERE pt.team_id = ?
                  %s
                  AND pt.end_date IS NULL
                ORDER BY CASE p.position
                    WHEN 'GK' THEN 1
                    WHEN 'RB' THEN 2 WHEN 'CB' THEN 3 WHEN 'LB' THEN 4
                    WHEN 'CDM' THEN 5 WHEN 'CM' THEN 6 WHEN 'CAM' THEN 7
                    WHEN 'RW' THEN 8 WHEN 'LW' THEN 9 WHEN 'ST' THEN 10
                    ELSE 11 END,
                    p.overall DESC, p.last_name
                """.formatted(membership,
                careerId == null ? "" : "AND pt.career_id = " + careerId);
        List<Player> players = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teamId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    players.add(mapPlayer(resultSet));
                }
            }
            return players;
        } catch (SQLException e) {
            throw new RuntimeException("Could not find team players.", e);
        }
    }

    public List<Player> findAll() {
        long scope = CareerContext.getCareerId() == null ? -1L : CareerContext.getCareerId();
        List<Player> cached = ALL_PLAYERS_CACHE.get(scope);
        if (cached != null) return cached;
        String sql = playerSelect() + " ORDER BY p.id";
        List<Player> players = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) players.add(mapPlayer(resultSet));
            List<Player> snapshot = List.copyOf(players);
            ALL_PLAYERS_CACHE.put(scope, snapshot);
            return snapshot;
        } catch (SQLException e) {
            throw new RuntimeException("Could not find all players.", e);
        }
    }

    public void updateDevelopment(Player player) {
        Long careerId = CareerContext.getCareerId();
        String table = careerId == null ? "players" : "career_player_development";
        String sql = """
                UPDATE %s SET overall = ?, pace = ?, shooting = ?,
                    passing = ?, dribbling = ?, defending = ?, physical = ?
                WHERE %s = ? %s
                """.formatted(table, careerId == null ? "id" : "player_id",
                careerId == null ? "" : "AND career_id = " + careerId);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, player.getOverall());
            statement.setInt(2, player.getPace());
            statement.setInt(3, player.getShooting());
            statement.setInt(4, player.getPassing());
            statement.setInt(5, player.getDribbling());
            statement.setInt(6, player.getDefending());
            statement.setInt(7, player.getPhysical());
            statement.setLong(8, player.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not update player development.", e);
        }
        if (careerId != null) {
            var career = new CareerRepository().findById(careerId);
            if (career != null) new PlayerProgressRepository().record(player.getId(),
                    career.getCurrentDate(), player.getOverall(), player.getMarketValue());
        }
        clearReadCache();
    }

    public static void clearReadCache() { ALL_PLAYERS_CACHE.clear(); }
    static int cachedCareerScopes() { return ALL_PLAYERS_CACHE.size(); }

    private Player mapPlayer(ResultSet resultSet) throws SQLException {
        Player player = new Player(
                resultSet.getLong("id"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                LocalDate.parse(
                        resultSet.getString("birth_date")
                ),
                resultSet.getString("nationality"),
                Position.valueOf(
                        resultSet.getString("position")
                ),
                PreferredFoot.valueOf(
                        resultSet.getString("preferred_foot")
                ),
                resultSet.getInt("overall"),
                resultSet.getInt("potential"),
                resultSet.getInt("pace"),
                resultSet.getInt("shooting"),
                resultSet.getInt("passing"),
                resultSet.getInt("dribbling"),
                resultSet.getInt("defending"),
                resultSet.getInt("physical"),
                resultSet.getDouble("market_value"),
                resultSet.getDouble("salary")
        );
        player.setHeightCm(resultSet.getInt("height_cm"));
        String secondary = resultSet.getString("secondary_position");
        if (secondary != null && !secondary.isBlank())
            player.setSecondaryPosition(Position.valueOf(secondary));
        return player;
    }

    private String playerSelect() {
        Long careerId = CareerContext.getCareerId();
        if (careerId == null) return "SELECT p.* FROM players p ";
        return """
                SELECT p.id, p.first_name, p.last_name, p.birth_date, p.nationality,
                       p.position, p.preferred_foot, p.height_cm, p.secondary_position,
                       p.potential, p.market_value, p.salary,
                       COALESCE(cpd.overall, p.overall) AS overall,
                       COALESCE(cpd.pace, p.pace) AS pace,
                       COALESCE(cpd.shooting, p.shooting) AS shooting,
                       COALESCE(cpd.passing, p.passing) AS passing,
                       COALESCE(cpd.dribbling, p.dribbling) AS dribbling,
                       COALESCE(cpd.defending, p.defending) AS defending,
                       COALESCE(cpd.physical, p.physical) AS physical
                FROM players p
                LEFT JOIN career_player_development cpd
                  ON cpd.player_id = p.id AND cpd.career_id = %d
                """.formatted(careerId);
    }
}
