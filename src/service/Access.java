package service;

final class Access {
  private Access() {}

  static Actor actor(Actor actor) throws java.sql.SQLException {
    return check(actor, false);
  }

  static Actor manager(Actor actor) throws java.sql.SQLException {
    return check(actor, true);
  }

  private static Actor check(Actor actor, boolean manager) throws java.sql.SQLException {
    if (actor == null) throw new SessionExpiredException("Cần đăng nhập");
    try (java.sql.Connection connection = connectDB.DBConnection.open()) {
      validate(actor, manager, connection);
    }
    return actor;
  }

  static void validate(Actor actor, boolean manager, java.sql.Connection connection)
      throws java.sql.SQLException {
    if (actor == null || actor.isInvalid()) throw new SessionExpiredException("Cần đăng nhập lại");
    entity.TaiKhoan account = new dao.TaiKhoanDAO().find(actor.getTenDangNhap(), connection);
    if (account == null || !"HOAT_DONG".equals(account.getTrangThai())
        || !"DANG_LAM".equals(account.getTrangThaiNhanVien())
        || !actor.getVaiTro().equals(account.getVaiTro())
        || !actor.getMaNhanVien().equals(account.getMaNhanVien())
        || actor.getAccountVersion() != account.getAccountVersion()
        || actor.getEmployeeVersion() != account.getEmployeeVersion()) {
      actor.invalidate();
      throw new SessionExpiredException("Phiên đăng nhập đã hết hiệu lực. Hãy đăng nhập lại.");
    }
    if (manager && !actor.isQuanLy()) throw new SecurityException("Cần quyền quản lý");
  }

  static void lock(java.sql.Connection connection, String resource, String mode)
      throws java.sql.SQLException {
    try (java.sql.PreparedStatement statement = connection.prepareStatement(
        // A table SELECT starts JDBC's implicit transaction without nesting BEGIN TRAN.
        "DECLARE @r int; IF @@TRANCOUNT=0 SELECT @r=COUNT(*) FROM dbo.CAU_HINH_CUA_HANG; "
        + "EXEC @r=sys.sp_getapplock @Resource=?, @LockMode=?, "
        + "@LockOwner='Transaction', @LockTimeout=15000; SELECT @r")) {
      statement.setString(1, resource);
      statement.setString(2, mode);
      try (java.sql.ResultSet result = statement.executeQuery()) {
        if (!result.next() || result.getInt(1) < 0)
          throw new java.sql.SQLException("Không thể khóa giao dịch; hãy thử lại.", "40001");
      }
    }
  }

  static String blankToNull(String value) {
    if (value == null)
      return null;
    String trimmed = value.trim();
    return trimmed.length() == 0 ? null : trimmed;
  }

  static <E extends Enum<E>> boolean is(String value, Class<E> type) {
    try {
      Enum.valueOf(type, value);
      return true;
    } catch (RuntimeException exception) {
      return false;
    }
  }
}
