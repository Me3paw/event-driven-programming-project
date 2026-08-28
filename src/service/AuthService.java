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
      if (taiKhoan == null || !TrangThaiTaiKhoan.HOAT_DONG.name().equals(taiKhoan.getTrangThai())
          || !verify(matKhau, taiKhoan.getMatKhauBam()))
        throw new SecurityException("Đăng nhập không hợp lệ");
      return taiKhoan;
    }
  }

  public Actor dangNhapActor(String tenDangNhap, char[] matKhau) throws SQLException {
    return new Actor(dangNhap(tenDangNhap, matKhau));
  }

  TaiKhoan xacThucQuanLy(String tenDangNhap, char[] matKhau, Connection connection)
      throws SQLException {
    TaiKhoan taiKhoan = taiKhoanDAO.find(tenDangNhap, connection);
    if (taiKhoan == null || !VaiTro.QUAN_LY.name().equals(taiKhoan.getVaiTro())
        || !TrangThaiTaiKhoan.HOAT_DONG.name().equals(taiKhoan.getTrangThai())
        || !verify(matKhau, taiKhoan.getMatKhauBam()))
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
