package dao;

import entity.LoaiSanPham;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LoaiSanPham_DAO {
  public void luu(LoaiSanPham value, Connection connection) throws SQLException {
    String sql = "MERGE LOAI_SAN_PHAM AS target USING (VALUES (?, ?, ?, ?)) AS source "
        + "(ma_loai,ten_loai,mo_ta,trang_thai) ON target.ma_loai=source.ma_loai "
        + "WHEN MATCHED THEN UPDATE SET ten_loai=source.ten_loai,mo_ta=source.mo_ta,"
        + "trang_thai=source.trang_thai WHEN NOT MATCHED THEN INSERT "
        + "(ma_loai,ten_loai,mo_ta,trang_thai) VALUES (source.ma_loai,source.ten_loai,"
        + "source.mo_ta,source.trang_thai);";
    try (PreparedStatement s = connection.prepareStatement(sql)) {
      s.setString(1, value.getMaLoai());
      s.setString(2, value.getTenLoai());
      s.setString(3, value.getMoTa());
      s.setString(4, value.getTrangThai());
      s.executeUpdate();
    }
  }
  public List<LoaiSanPham> timKiem(String tuKhoa, Connection connection) throws SQLException {
    List<LoaiSanPham> values = new ArrayList<LoaiSanPham>();
    try (PreparedStatement s =
             connection.prepareStatement("SELECT * FROM LOAI_SAN_PHAM WHERE ? IS NULL OR ma_loai "
                 + "LIKE ? OR ten_loai LIKE ? ORDER BY ma_loai")) {
      s.setString(1, tuKhoa);
      s.setString(2, tuKhoa == null ? null : "%" + tuKhoa + "%");
      s.setString(3, tuKhoa == null ? null : "%" + tuKhoa + "%");
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) {
          LoaiSanPham v = new LoaiSanPham();
          v.setMaLoai(r.getString("ma_loai"));
          v.setTenLoai(r.getString("ten_loai"));
          v.setMoTa(r.getString("mo_ta"));
          v.setTrangThai(r.getString("trang_thai"));
          values.add(v);
        }
      }
    }
    return values;
  }
  public String maTiepTheo(Connection connection) throws SQLException {
    return Id_DAO.taoMaTiepTheo("LSP", connection);
  }
  public void xoa(String maLoai, Connection connection) throws SQLException {
    try (PreparedStatement s =
             connection.prepareStatement("DELETE FROM LOAI_SAN_PHAM WHERE ma_loai = ?")) {
      s.setString(1, maLoai);
      s.executeUpdate();
    }
  }
}
