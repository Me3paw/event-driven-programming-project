package dao;

import entity.TaiKhoan;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TaiKhoan_DAO {
  public TaiKhoan timTheoMa(String tenDangNhap, Connection connection) throws SQLException {
    String sql = "SELECT tk.ten_dang_nhap, tk.mat_khau_bam, tk.vai_tro, tk.trang_thai, "
        + "tk.session_version AS account_version,nv.session_version AS employee_version, "
        + "tk.ma_nhan_vien, nv.trang_thai AS trang_thai_nhan_vien, "
        + "nv.ho_ten AS ten_nhan_vien, nv.chuc_vu AS chuc_vu_nhan_vien "
        + "FROM TAI_KHOAN tk LEFT JOIN NHAN_VIEN nv ON nv.ma_nhan_vien = tk.ma_nhan_vien "
        + "WHERE tk.ten_dang_nhap = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, tenDangNhap);
      try (ResultSet result = statement.executeQuery()) {
        return result.next() ? taoTuKetQua(result) : null;
      }
    }
  }

  public void luu(TaiKhoan taiKhoan, Connection connection) throws SQLException {
    String update = "UPDATE TAI_KHOAN SET session_version=session_version+CASE "
        + "WHEN vai_tro<>? OR trang_thai<>? OR ma_nhan_vien<>? THEN 1 ELSE 0 END, "
        + "mat_khau_bam=?,vai_tro=?,trang_thai=?,ma_nhan_vien=? "
        + "WHERE ten_dang_nhap=?";
    try (PreparedStatement statement = connection.prepareStatement(update)) {
      statement.setString(1, taiKhoan.getVaiTro());
      statement.setString(2, taiKhoan.getTrangThai());
      statement.setString(3, taiKhoan.getMaNhanVien());
      statement.setString(4, taiKhoan.getMatKhauBam());
      statement.setString(5, taiKhoan.getVaiTro());
      statement.setString(6, taiKhoan.getTrangThai());
      statement.setString(7, taiKhoan.getMaNhanVien());
      statement.setString(8, taiKhoan.getTenDangNhap());
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

  public List<TaiKhoan> timKiem(
      String tuKhoa, String vaiTro, String trangThai, Connection connection) throws SQLException {
    List<TaiKhoan> out = new ArrayList<TaiKhoan>();
    String sql = "SELECT tk.ten_dang_nhap,tk.mat_khau_bam,tk.vai_tro,tk.trang_thai,tk.ma_nhan_vien,tk.session_version AS account_version,nv.session_version AS employee_version, "
        + "nv.trang_thai AS trang_thai_nhan_vien,nv.ho_ten AS ten_nhan_vien,"
        + "nv.chuc_vu AS chuc_vu_nhan_vien "
        + "FROM TAI_KHOAN tk LEFT JOIN NHAN_VIEN nv ON nv.ma_nhan_vien=tk.ma_nhan_vien "
        + "WHERE (? IS NULL OR tk.ten_dang_nhap LIKE ? OR tk.ma_nhan_vien LIKE ? "
        + "OR nv.ho_ten LIKE ?) AND (? IS NULL OR tk.vai_tro=?) AND (? IS NULL OR tk.trang_thai=?) "
        + "ORDER BY tk.ma_nhan_vien DESC, tk.ten_dang_nhap";
    try (PreparedStatement s = connection.prepareStatement(sql)) {
      s.setString(1, tuKhoa);
      s.setString(2, tuKhoa == null ? null : "%" + tuKhoa + "%");
      s.setString(3, tuKhoa == null ? null : "%" + tuKhoa + "%");
      s.setString(4, tuKhoa == null ? null : "%" + tuKhoa + "%");
      s.setString(5, vaiTro);
      s.setString(6, vaiTro);
      s.setString(7, trangThai);
      s.setString(8, trangThai);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) out.add(taoTuKetQua(r));
      }
    }
    return out;
  }
  public long demQuanLyHoatDong(Connection connection) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT COUNT(*) FROM TAI_KHOAN t JOIN NHAN_VIEN n ON n.ma_nhan_vien=t.ma_nhan_vien "
        + "WHERE t.vai_tro='QUAN_LY' AND t.trang_thai='HOAT_DONG' AND n.trang_thai='DANG_LAM'");
        ResultSet result = statement.executeQuery()) {
      result.next();
      return result.getLong(1);
    }
  }

  public void xoa(String tenDangNhap, Connection connection) throws SQLException {
    try (PreparedStatement s =
             connection.prepareStatement("DELETE FROM TAI_KHOAN WHERE ten_dang_nhap=?")) {
      s.setString(1, tenDangNhap);
      s.executeUpdate();
    }
  }
  public void capNhatMatKhau(String tenDangNhap, String matKhauBam, Connection connection)
      throws SQLException {
    try (PreparedStatement s = connection.prepareStatement(
             "UPDATE TAI_KHOAN SET mat_khau_bam=? WHERE ten_dang_nhap=?")) {
      s.setString(1, matKhauBam);
      s.setString(2, tenDangNhap);
      if (s.executeUpdate() != 1)
        throw new IllegalArgumentException("Không tìm thấy tài khoản");
    }
  }
  public void capNhatTrangThai(String tenDangNhap, String trangThai, Connection connection)
      throws SQLException {
    try (PreparedStatement s = connection.prepareStatement(
             "UPDATE TAI_KHOAN SET session_version=session_version+1,trang_thai=? WHERE ten_dang_nhap=?")) {
      s.setString(1, trangThai);
      s.setString(2, tenDangNhap);
      if (s.executeUpdate() != 1)
        throw new IllegalArgumentException("Không tìm thấy tài khoản");
    }
  }

  private TaiKhoan taoTuKetQua(ResultSet result) throws SQLException {
    TaiKhoan taiKhoan = new TaiKhoan();
    taiKhoan.setAccountVersion(result.getLong("account_version"));
    taiKhoan.setEmployeeVersion(result.getLong("employee_version"));
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
