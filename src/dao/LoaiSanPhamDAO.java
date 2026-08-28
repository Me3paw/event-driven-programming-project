package dao;

import entity.LoaiSanPham;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LoaiSanPhamDAO {
  public void save(LoaiSanPham value, Connection connection) throws SQLException {
    String sql = "INSERT INTO LOAI_SAN_PHAM (ma_loai, ten_loai, mo_ta, trang_thai) VALUES (?, ?, "
        + "?, ?) ON DUPLICATE KEY UPDATE ten_loai = VALUES(ten_loai), mo_ta = "
        + "VALUES(mo_ta), trang_thai = VALUES(trang_thai)";
    try (PreparedStatement s = connection.prepareStatement(sql)) {
      s.setString(1, value.getMaLoai());
      s.setString(2, value.getTenLoai());
      s.setString(3, value.getMoTa());
      s.setString(4, value.getTrangThai());
      s.executeUpdate();
    }
  }
  public List<LoaiSanPham> search(String keyword, Connection connection) throws SQLException {
    List<LoaiSanPham> values = new ArrayList<LoaiSanPham>();
    try (PreparedStatement s =
             connection.prepareStatement("SELECT * FROM LOAI_SAN_PHAM WHERE ? IS NULL OR ma_loai "
                 + "LIKE ? OR ten_loai LIKE ? ORDER BY ma_loai")) {
      s.setString(1, keyword);
      s.setString(2, keyword == null ? null : "%" + keyword + "%");
      s.setString(3, keyword == null ? null : "%" + keyword + "%");
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
  public void delete(String maLoai, Connection connection) throws SQLException {
    try (PreparedStatement s =
             connection.prepareStatement("DELETE FROM LOAI_SAN_PHAM WHERE ma_loai = ?")) {
      s.setString(1, maLoai);
      s.executeUpdate();
    }
  }
}
