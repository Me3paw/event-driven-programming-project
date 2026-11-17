package service;

import connectDB.DBConnection;
import java.sql.Connection;
import java.sql.SQLException;

/** Authorization lock is held until commit/rollback, before any business locks. */
final class SessionTransaction implements AutoCloseable {
  private final Connection connection;
  private boolean committed;

  SessionTransaction(Actor actor, boolean manager, boolean changesAuthorization) throws SQLException {
    connection = DBConnection.open();
    try {
      connection.setAutoCommit(false);
      Access.lock(connection, "POS_AUTHORIZATION", changesAuthorization ? "Exclusive" : "Shared");
      Access.validate(actor, manager, connection);
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
        java.util.logging.Logger.getLogger(SessionTransaction.class.getName())
            .log(java.util.logging.Level.WARNING, "Đã commit; không đóng được kết nối", exception);
      }
      return;
    }
    try { connection.rollback(); }
    finally { connection.close(); }
  }
}
