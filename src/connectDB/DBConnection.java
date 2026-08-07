package connectDB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Minimal MariaDB connectivity check; it does not mutate application data. */
public final class DBConnection {
    private DBConnection() {
    }

    public static int smoke() {
        String url = required("POS_DB_URL");
        String user = required("POS_DB_USER");
        String password = required("POS_DB_PASSWORD");
        if (url == null || user == null || password == null) {
            return 2;
        }

        DriverManager.setLoginTimeout(5);
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT 1")) {
            if (result.next() && result.getInt(1) == 1) {
                return 0;
            }
            return 3;
        } catch (SQLException exception) {
            return 4;
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? null : value;
    }
}
