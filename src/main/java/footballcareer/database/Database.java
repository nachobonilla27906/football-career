package footballcareer.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    public static final String DATABASE_URL_PROPERTY = "footballcareer.database.url";
    private static final String DEFAULT_URL = "jdbc:sqlite:football-career.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(System.getProperty(
                DATABASE_URL_PROPERTY, DEFAULT_URL));
    }
}
