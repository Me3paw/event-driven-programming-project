package testsupport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/** Wraps the real JDBC driver only in tests; all SQL still executes in SQL Server. */
final class JdbcFaults implements Driver, AutoCloseable {
  static final ThreadLocal<Boolean> LOSE_COMMIT = new ThreadLocal<Boolean>();
  static final ThreadLocal<Runnable> BEFORE_STOCK = new ThreadLocal<Runnable>();
  static final java.util.concurrent.atomic.AtomicBoolean FAIL_RECEIPT_AFTER_SALE = new java.util.concurrent.atomic.AtomicBoolean();
  static final java.util.concurrent.atomic.AtomicBoolean LOSE_NEXT_SALE_COMMIT = new java.util.concurrent.atomic.AtomicBoolean();
  private static final java.util.concurrent.atomic.AtomicBoolean RECEIPT_ARMED = new java.util.concurrent.atomic.AtomicBoolean();
  private static final java.util.concurrent.atomic.AtomicInteger FAILED_CONNECTIONS = new java.util.concurrent.atomic.AtomicInteger();
  private final Driver delegate;
  JdbcFaults() throws SQLException {
    delegate = DriverManager.getDriver(System.getenv("POS_DB_URL"));
    DriverManager.deregisterDriver(delegate); DriverManager.registerDriver(this);
  }
  @Override public Connection connect(String url, Properties properties) throws SQLException {
    if (!acceptsURL(url)) return null;
    if (FAILED_CONNECTIONS.getAndUpdate(value -> Math.max(0, value - 1)) > 0)
      throw new SQLException("Injected unavailable response resolution", "08001");
    Connection real = delegate.connect(url, properties);
    final boolean[] wroteInvoice = {false};
    return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] {Connection.class},
        (proxy, method, args) -> {
          try {
            if (method.getName().equals("prepareStatement") && args != null && args[0] instanceof String
                && ((String) args[0]).contains("FROM LO_TON_KHO l WITH") && BEFORE_STOCK.get() != null) {
              Runnable hook = BEFORE_STOCK.get(); BEFORE_STOCK.remove(); hook.run();
            }
            if (method.getName().equals("prepareStatement") && args != null && args[0] instanceof String) {
              String sql = (String) args[0];
              if (sql.startsWith("INSERT INTO HOA_DON ")) wroteInvoice[0] = true;
              if (sql.contains("CAU_HINH_CUA_HANG WHERE id=1") && RECEIPT_ARMED.compareAndSet(true, false))
                throw new SQLException("Injected receipt read failure", "42000");
            }
            Object value = method.invoke(real, args);
            if (method.getName().equals("commit") && wroteInvoice[0]) {
              if (FAIL_RECEIPT_AFTER_SALE.compareAndSet(true, false)) RECEIPT_ARMED.set(true);
              if (LOSE_NEXT_SALE_COMMIT.compareAndSet(true, false)) {
                FAILED_CONNECTIONS.set(2);
                throw new SQLException("Injected lost sale response and unavailable resolution", "08007");
              }
            }
            if (method.getName().equals("commit") && Boolean.TRUE.equals(LOSE_COMMIT.get())) {
              LOSE_COMMIT.remove(); throw new SQLException("Injected lost commit acknowledgement", "08007");
            }
            return value;
          } catch (InvocationTargetException exception) { throw exception.getCause(); }
        });
  }
  @Override public boolean acceptsURL(String url) throws SQLException { return delegate.acceptsURL(url); }
  @Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties properties) throws SQLException { return delegate.getPropertyInfo(url, properties); }
  @Override public int getMajorVersion() { return delegate.getMajorVersion(); }
  @Override public int getMinorVersion() { return delegate.getMinorVersion(); }
  @Override public boolean jdbcCompliant() { return delegate.jdbcCompliant(); }
  @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { return delegate.getParentLogger(); }
  @Override public void close() throws SQLException {
    LOSE_COMMIT.remove(); BEFORE_STOCK.remove(); FAIL_RECEIPT_AFTER_SALE.set(false); LOSE_NEXT_SALE_COMMIT.set(false);
    RECEIPT_ARMED.set(false); FAILED_CONNECTIONS.set(0); DriverManager.deregisterDriver(this); DriverManager.registerDriver(delegate);
  }
}
