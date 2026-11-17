package service;

final class KiemTraQuyen {
  private KiemTraQuyen() {}

  static NguoiDung actor(NguoiDung nguoiDung) throws java.sql.SQLException {
    return check(nguoiDung, false);
  }

  static NguoiDung manager(NguoiDung nguoiDung) throws java.sql.SQLException {
    return check(nguoiDung, true);
  }

  private static NguoiDung check(NguoiDung nguoiDung, boolean quanLy) throws java.sql.SQLException {
    if (nguoiDung == null) throw new SessionExpiredException("Cần đăng nhập");
    try (java.sql.Connection connection = connectDB.ConnectDB.open()) {
      validate(nguoiDung, quanLy, connection);
    }
    return nguoiDung;
  }

  static void validate(NguoiDung nguoiDung, boolean quanLy, java.sql.Connection connection)
      throws java.sql.SQLException {
    if (nguoiDung == null || nguoiDung.isInvalid()) throw new SessionExpiredException("Cần đăng nhập lại");
    entity.TaiKhoan taiKhoan = new dao.TaiKhoan_DAO().timTheoMa(nguoiDung.getTenDangNhap(), connection);
    if (taiKhoan == null || !"HOAT_DONG".equals(taiKhoan.getTrangThai())
        || !"DANG_LAM".equals(taiKhoan.getTrangThaiNhanVien())
        || !nguoiDung.getVaiTro().equals(taiKhoan.getVaiTro())
        || !nguoiDung.getMaNhanVien().equals(taiKhoan.getMaNhanVien())
        || nguoiDung.getPhienBanTaiKhoan() != taiKhoan.getPhienBanTaiKhoan()
        || nguoiDung.getPhienBanNhanVien() != taiKhoan.getPhienBanNhanVien()) {
      nguoiDung.invalidate();
      throw new SessionExpiredException("Phiên đăng nhập đã hết hiệu lực. Hãy đăng nhập lại.");
    }
    if (quanLy && !nguoiDung.isQuanLy()) throw new SecurityException("Cần quyền quản lý");
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
