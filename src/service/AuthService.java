package service;

import connectDB.DBConnection;
import dao.TaiKhoanDAO;
import entity.TaiKhoan;
import entity.TrangThaiTaiKhoan;
import entity.VaiTro;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class AuthService {
  private static final int ITERATIONS = 120000;
  private static final int KEY_BITS = 256;
  private final TaiKhoanDAO taiKhoanDAO = new TaiKhoanDAO();

  private TaiKhoan dangNhap(String tenDangNhap, char[] matKhau) throws SQLException {
    try (Connection connection = DBConnection.open()) {
      TaiKhoan taiKhoan = taiKhoanDAO.find(tenDangNhap, connection);
      if (taiKhoan == null || !verify(matKhau, taiKhoan.getMatKhauBam()))
        throw new SecurityException("Tên đăng nhập hoặc mật khẩu không đúng");
      if (!TrangThaiTaiKhoan.HOAT_DONG.name().equals(taiKhoan.getTrangThai()))
        throw new SecurityException("Tài khoản đang bị khóa hoặc ngừng hoạt động");
      if (!"DANG_LAM".equals(taiKhoan.getTrangThaiNhanVien()))
        throw new SecurityException("Tài khoản không thể sử dụng vì nhân viên đã nghỉ việc");
      return taiKhoan;
    }
  }

  public Actor dangNhapActor(String tenDangNhap, char[] matKhau) throws SQLException {
    return new Actor(dangNhap(tenDangNhap, matKhau));
  }

  public void doiMatKhau(Actor actor, char[] matKhauCu, char[] matKhauMoi) throws SQLException {
    if (actor == null || actor.getTenDangNhap() == null)
      throw new SecurityException("Chưa đăng nhập");
    try (SessionTransaction transaction = new SessionTransaction(actor, false, false)) {
      Connection connection = transaction.connection();
      TaiKhoan taiKhoan = taiKhoanDAO.find(actor.getTenDangNhap(), connection);
      if (taiKhoan == null || !verify(matKhauCu, taiKhoan.getMatKhauBam()))
        throw new SecurityException("Mật khẩu hiện tại không đúng");
      if (matKhauMoi == null || matKhauMoi.length < 6)
        throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự");
      taiKhoanDAO.updatePassword(actor.getTenDangNhap(), hash(matKhauMoi), connection);
      transaction.commit();
    } finally {
      if (matKhauCu != null)
        Arrays.fill(matKhauCu, '\0');
      if (matKhauMoi != null)
        Arrays.fill(matKhauMoi, '\0');
    }
  }

  TaiKhoan xacThucQuanLy(String tenDangNhap, char[] matKhau, Connection connection)
      throws SQLException {
    TaiKhoan taiKhoan = taiKhoanDAO.find(tenDangNhap, connection);
    if (taiKhoan == null || !verify(matKhau, taiKhoan.getMatKhauBam())
        || !VaiTro.QUAN_LY.name().equals(taiKhoan.getVaiTro())
        || !TrangThaiTaiKhoan.HOAT_DONG.name().equals(taiKhoan.getTrangThai())
        || !"DANG_LAM".equals(taiKhoan.getTrangThaiNhanVien()))
      throw new SecurityException("Cần xác thực quản lý");
    return taiKhoan;
  }

  String hash(char[] matKhau) {
    byte[] salt = new byte[16], derived = null;
    new SecureRandom().nextBytes(salt);
    try {
      derived = derive(matKhau, salt, ITERATIONS);
      return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$"
          + Base64.getEncoder().encodeToString(derived);
    } finally {
      Arrays.fill(salt, (byte) 0);
      if (derived != null)
        Arrays.fill(derived, (byte) 0);
    }
  }

  private boolean verify(char[] matKhau, String encoded) {
    try {
      String[] parts = encoded.split("\\$");
      if (parts.length != 4 || !"pbkdf2".equals(parts[0]))
        return false;
      byte[] expected = Base64.getDecoder().decode(parts[3]),
             salt = Base64.getDecoder().decode(parts[2]), actual = null;
      try {
        actual = derive(matKhau, salt, Integer.parseInt(parts[1]));
        return MessageDigest.isEqual(expected, actual);
      } finally {
        Arrays.fill(expected, (byte) 0);
        Arrays.fill(salt, (byte) 0);
        if (actual != null)
          Arrays.fill(actual, (byte) 0);
      }
    } catch (RuntimeException exception) {
      return false;
    }
  }

  private byte[] derive(char[] password, byte[] salt, int iterations) {
    PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (Exception exception) {
      throw new IllegalStateException("Không thể băm mật khẩu", exception);
    } finally {
      spec.clearPassword();
    }
  }
}
