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
        + "tk.ma_nhan_vien, nv.trang_thai AS trang_thai_nhan_vien, "
        + "nv.ho_ten AS ten_nhan_vien, nv.chuc_vu AS chuc_vu_nhan_vien "
        + "FROM TAI_KHOAN tk LEFT JOIN NHAN_VIEN nv ON nv.ma_nhan_vien = tk.ma_nhan_vien "
        + "WHERE tk.ten_dang_nhap = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, tenDangNhap);
      try (ResultSet result = statement.executeQuery()) {
        return result.next() ? map(result) : null;
      }
    }
  }

  public void save(TaiKhoan taiKhoan, Connection connection) throws SQLException {
    String update = "UPDATE TAI_KHOAN SET mat_khau_bam=?,vai_tro=?,trang_thai=?,ma_nhan_vien=? "
        + "WHERE ten_dang_nhap=?";
    try (PreparedStatement statement = connection.prepareStatement(update)) {
      statement.setString(1, taiKhoan.getMatKhauBam());
      statement.setString(2, taiKhoan.getVaiTro());
      statement.setString(3, taiKhoan.getTrangThai());
      statement.setString(4, taiKhoan.getMaNhanVien());
      statement.setString(5, taiKhoan.getTenDangNhap());
      if (statement.executeUpdate() == 0) {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO TAI_KHOAN (ten_dang_nhap,mat_khau_bam,vai_tro,trang_thai,ma_nhan_vien) "
                    + "VALUES (?,?,?,?,?)")) {
          insert.setString(1, taiKhoan.getTenDangNhap());
          insert.setString(2, taiKhoan.getMatKhauBam());
          insert.setString(3, taiKhoan.getVaiTro());
          insert.setString(4, taiKhoan.getTrangThai());
          insert.setString(5, taiKhoan.getMaNhanVien());
          insert.executeUpdate();
        }
      }
    }
  }

  public List<TaiKhoan> search(
      String keyword, String vaiTro, String trangThai, Connection connection) throws SQLException {
    List<TaiKhoan> out = new ArrayList<TaiKhoan>();
    String sql = "SELECT tk.ten_dang_nhap,tk.mat_khau_bam,tk.vai_tro,tk.trang_thai,tk.ma_nhan_vien, "
        + "nv.trang_thai AS trang_thai_nhan_vien,nv.ho_ten AS ten_nhan_vien,"
        + "nv.chuc_vu AS chuc_vu_nhan_vien "
        + "FROM TAI_KHOAN tk LEFT JOIN NHAN_VIEN nv ON nv.ma_nhan_vien=tk.ma_nhan_vien "
        + "WHERE (? IS NULL OR tk.ten_dang_nhap LIKE ? OR tk.ma_nhan_vien LIKE ? "
        + "OR nv.ho_ten LIKE ?) AND (? IS NULL OR tk.vai_tro=?) AND (? IS NULL OR tk.trang_thai=?) "
        + "ORDER BY tk.ma_nhan_vien DESC, tk.ten_dang_nhap";
    try (PreparedStatement s = connection.prepareStatement(sql)) {
      s.setString(1, keyword);
      s.setString(2, keyword == null ? null : "%" + keyword + "%");
      s.setString(3, keyword == null ? null : "%" + keyword + "%");
      s.setString(4, keyword == null ? null : "%" + keyword + "%");
      s.setString(5, vaiTro);
      s.setString(6, vaiTro);
      s.setString(7, trangThai);
      s.setString(8, trangThai);
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
  public void updatePassword(String tenDangNhap, String matKhauBam, Connection connection)
      throws SQLException {
    try (PreparedStatement s = connection.prepareStatement(
             "UPDATE TAI_KHOAN SET mat_khau_bam=? WHERE ten_dang_nhap=?")) {
      s.setString(1, matKhauBam);
      s.setString(2, tenDangNhap);
      if (s.executeUpdate() != 1)
        throw new IllegalArgumentException("Không tìm thấy tài khoản");
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
    taiKhoan.setTrangThaiNhanVien(result.getString("trang_thai_nhan_vien"));
    taiKhoan.setTenNhanVien(result.getString("ten_nhan_vien"));
    taiKhoan.setChucVuNhanVien(result.getString("chuc_vu_nhan_vien"));
    return taiKhoan;
  }
}
