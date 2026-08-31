package connectDB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DBConnection {
    private DBConnection() {
    }

    public static int smoke() {
        if (!configured()) {
            return 2;
        }

        DriverManager.setLoginTimeout(5);
        try (Connection connection = open();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT 1")) {
            if (result.next() && result.getInt(1) == 1) {
                return 0;
            }
            return 3;
    } catch (SQLException exception) {
        exception.printStackTrace();
        return 4;
    }
    }

    public static Connection open() throws SQLException {
        String url = required("POS_DB_URL");
        String user = required("POS_DB_USER");
        String password = required("POS_DB_PASSWORD");
        if (url == null || user == null || password == null) {
            throw new SQLException("Thiếu cấu hình kết nối cơ sở dữ liệu");
        }
        return DriverManager.getConnection(url, user, password);
    }

    public static boolean configured() {
        return required("POS_DB_URL") != null && required("POS_DB_USER") != null && required("POS_DB_PASSWORD") != null;
    }

    private static String required(String name) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? null : value;
    }
}
