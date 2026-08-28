package dao;

import entity.TaiKhoan;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TaiKhoanDAO {
  public TaiKhoan find(String tenDangNhap, Connection connection) throws SQLException {
    String sql = "SELECT tk.ten_dang_nhap, tk.mat_khau_bam, tk.vai_tro, tk.trang_thai, "
        + "tk.ma_nhan_vien FROM TAI_KHOAN tk JOIN NHAN_VIEN nv ON nv.ma_nhan_vien = "
        + "tk.ma_nhan_vien WHERE tk.ten_dang_nhap = ? AND nv.trang_thai = 'DANG_LAM'";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, tenDangNhap);
      try (ResultSet result = statement.executeQuery()) {
        return result.next() ? map(result) : null;
      }
    }
  }

  public void save(TaiKhoan taiKhoan, Connection connection) throws SQLException {
    String sql = "INSERT INTO TAI_KHOAN (ten_dang_nhap, mat_khau_bam, vai_tro, trang_thai, "
        + "ma_nhan_vien) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE mat_khau_bam = "
        + "VALUES(mat_khau_bam), vai_tro = VALUES(vai_tro), trang_thai = "
        + "VALUES(trang_thai), ma_nhan_vien = VALUES(ma_nhan_vien)";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, taiKhoan.getTenDangNhap());
      statement.setString(2, taiKhoan.getMatKhauBam());
      statement.setString(3, taiKhoan.getVaiTro());
      statement.setString(4, taiKhoan.getTrangThai());
      statement.setString(5, taiKhoan.getMaNhanVien());
      statement.executeUpdate();
    }
  }

  public List<TaiKhoan> search(
      String keyword, String vaiTro, String trangThai, Connection connection) throws SQLException {
    List<TaiKhoan> out = new ArrayList<TaiKhoan>();
    String sql = "SELECT ten_dang_nhap,mat_khau_bam,vai_tro,trang_thai,ma_nhan_vien FROM TAI_KHOAN "
        + "WHERE (? IS NULL OR ten_dang_nhap LIKE ?) AND (? IS NULL OR vai_tro=?) AND (? "
        + "IS NULL OR trang_thai=?) ORDER BY ten_dang_nhap";
    try (PreparedStatement s = connection.prepareStatement(sql)) {
      s.setString(1, keyword);
      s.setString(2, keyword == null ? null : "%" + keyword + "%");
      s.setString(3, vaiTro);
      s.setString(4, vaiTro);
      s.setString(5, trangThai);
      s.setString(6, trangThai);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) out.add(map(r));
      }
    }
    return out;
  }
  public void delete(String tenDangNhap, Connection connection) throws SQLException {
    try (PreparedStatement s =
             connection.prepareStatement("DELETE FROM TAI_KHOAN WHERE ten_dang_nhap=?")) {
      s.setString(1, tenDangNhap);
      s.executeUpdate();
    }
  }
  public void updateStatus(String tenDangNhap, String trangThai, Connection connection)
      throws SQLException {
    try (PreparedStatement s = connection.prepareStatement(
             "UPDATE TAI_KHOAN SET trang_thai=? WHERE ten_dang_nhap=?")) {
      s.setString(1, trangThai);
      s.setString(2, tenDangNhap);
      if (s.executeUpdate() != 1)
        throw new IllegalArgumentException("Không tìm thấy tài khoản");
    }
  }

  private TaiKhoan map(ResultSet result) throws SQLException {
    TaiKhoan taiKhoan = new TaiKhoan();
    taiKhoan.setTenDangNhap(result.getString("ten_dang_nhap"));
    taiKhoan.setMatKhauBam(result.getString("mat_khau_bam"));
    taiKhoan.setVaiTro(result.getString("vai_tro"));
    taiKhoan.setTrangThai(result.getString("trang_thai"));
    taiKhoan.setMaNhanVien(result.getString("ma_nhan_vien"));
    return taiKhoan;
  }
}
