package service;

import connectDB.ConnectDB;
import java.sql.Connection;
import java.sql.SQLException;

/** Authorization lock is held until commit/rollback, before any business locks. */
final class GiaoDichPhien implements AutoCloseable {
  private final Connection connection;
  private boolean committed;

  GiaoDichPhien(NguoiDung nguoiDung, boolean quanLy, boolean changesAuthorization) throws SQLException {
    connection = ConnectDB.open();
    try {
      connection.setAutoCommit(false);
      KiemTraQuyen.lock(connection, "POS_AUTHORIZATION", changesAuthorization ? "Exclusive" : "Shared");
      KiemTraQuyen.validate(nguoiDung, quanLy, connection);
    } catch (RuntimeException | SQLException exception) {
      connection.close();
      throw exception;
    }
  }

  Connection connection() { return connection; }

  void commit() throws SQLException {
    connection.commit();
    committed = true;
  }

  @Override public void close() throws SQLException {
    if (committed) {
      try { connection.close(); }
      catch (SQLException exception) {
        java.util.logging.Logger.getLogger(GiaoDichPhien.class.getName())
            .log(java.util.logging.Level.WARNING, "Đã commit; không đóng được kết nối", exception);
      }
      return;
    }
    try { connection.rollback(); }
    finally { connection.close(); }
  }
}
