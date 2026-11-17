package dao;

import entity.NhanVien;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NhanVienDAO {
  public void save(NhanVien value, Connection connection) throws SQLException {
    String sql = "MERGE NHAN_VIEN AS target USING (VALUES (?, ?, ?, ?, ?)) AS source "
        + "(ma_nhan_vien,ho_ten,so_dien_thoai,chuc_vu,trang_thai) "
        + "ON target.ma_nhan_vien=source.ma_nhan_vien WHEN MATCHED THEN UPDATE SET "
        + "ho_ten=source.ho_ten,so_dien_thoai=source.so_dien_thoai,chuc_vu=source.chuc_vu,"
        + "session_version=target.session_version+CASE WHEN target.trang_thai<>source.trang_thai THEN 1 ELSE 0 END, "
        + "trang_thai=source.trang_thai WHEN NOT MATCHED THEN INSERT "
        + "(ma_nhan_vien,ho_ten,so_dien_thoai,chuc_vu,trang_thai) VALUES "
        + "(source.ma_nhan_vien,source.ho_ten,source.so_dien_thoai,source.chuc_vu,source.trang_thai);";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, value.getMaNhanVien());
      statement.setString(2, value.getHoTen());
      statement.setString(3, value.getSoDienThoai());
      statement.setString(4, value.getChucVu());
      statement.setString(5, value.getTrangThai());
      statement.executeUpdate();
    }
  }

  public NhanVien find(String maNhanVien, Connection connection) throws SQLException {
    try (PreparedStatement statement =
             connection.prepareStatement("SELECT * FROM NHAN_VIEN WHERE ma_nhan_vien = ?")) {
      statement.setString(1, maNhanVien);
      try (ResultSet result = statement.executeQuery()) {
        return result.next() ? map(result) : null;
      }
    }
  }

  public List<NhanVien> search(String keyword, String trangThai, String chucVu,
      Connection connection) throws SQLException {
    String sql =
        "SELECT * FROM NHAN_VIEN WHERE (? IS NULL OR ma_nhan_vien LIKE ? OR ho_ten LIKE ? OR "
        + "so_dien_thoai LIKE ?) AND (? IS NULL OR trang_thai = ?) "
        + "AND (? IS NULL OR chuc_vu LIKE ?) ORDER BY ma_nhan_vien DESC";
    List<NhanVien> values = new ArrayList<NhanVien>();
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, keyword);
      statement.setString(2, keyword == null ? null : "%" + keyword + "%");
      statement.setString(3, keyword == null ? null : "%" + keyword + "%");
      statement.setString(4, keyword == null ? null : "%" + keyword + "%");
      statement.setString(5, trangThai);
      statement.setString(6, trangThai);
      statement.setString(7, chucVu);
      statement.setString(8, chucVu == null ? null : "%" + chucVu + "%");
      try (ResultSet result = statement.executeQuery()) {
        while (result.next()) values.add(map(result));
      }
    }
    return values;
  }

  public String nextId(Connection connection) throws SQLException {
    return IdDAO.next("NV", connection);
  }

  public void delete(String maNhanVien, Connection connection) throws SQLException {
    try (PreparedStatement statement =
             connection.prepareStatement("DELETE FROM NHAN_VIEN WHERE ma_nhan_vien = ?")) {
      statement.setString(1, maNhanVien);
      statement.executeUpdate();
    }
  }

  public void updateStatus(String maNhanVien, String trangThai, Connection connection)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(
             "UPDATE NHAN_VIEN SET session_version=session_version+1,trang_thai = ? WHERE ma_nhan_vien = ?")) {
      statement.setString(1, trangThai);
      statement.setString(2, maNhanVien);
      if (statement.executeUpdate() != 1)
        throw new IllegalArgumentException("Không tìm thấy nhân viên");
    }
  }

  private NhanVien map(ResultSet result) throws SQLException {
    NhanVien value = new NhanVien();
    value.setMaNhanVien(result.getString("ma_nhan_vien"));
    value.setHoTen(result.getString("ho_ten"));
    value.setSoDienThoai(result.getString("so_dien_thoai"));
    value.setChucVu(result.getString("chuc_vu"));
    value.setTrangThai(result.getString("trang_thai"));
    return value;
  }
}
