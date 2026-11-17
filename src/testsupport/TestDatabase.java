package testsupport;

import connectDB.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

final class TestDatabase {
  private TestDatabase() {}
  static void requireIsolated() throws Exception {
    try (Connection connection = DBConnection.open(); Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery("SELECT DB_NAME()")) {
      result.next();
      if (!result.getString(1).matches("pos_test_[A-Za-z0-9_]+"))
        throw new IllegalStateException("Refusing writes outside a pos_test_* database");
    }
  }
}
