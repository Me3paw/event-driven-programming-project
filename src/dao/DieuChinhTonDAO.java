package dao;

import entity.DieuChinhTon;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DieuChinhTonDAO {
  public void save(DieuChinhTon value, Connection c) throws SQLException {
    try (PreparedStatement s = c.prepareStatement(
             "INSERT INTO DIEU_CHINH_TON (ma_dieu_chinh,ma_nhan_vien,thoi_diem_lap,ly_do) VALUES "
             + "(?,?,?,?)")) {
      s.setString(1, value.getMaDieuChinh());
      s.setString(2, value.getMaNhanVien());
      s.setTimestamp(3,
          Timestamp.valueOf(
              value.getThoiDiemLap() == null ? LocalDateTime.now() : value.getThoiDiemLap()));
      s.setString(4, value.getLyDo());
      s.executeUpdate();
    }
    try (
        PreparedStatement s = c.prepareStatement(
            "INSERT INTO CHI_TIET_DIEU_CHINH (ma_dieu_chinh,ma_lo,so_luong_giam) VALUES (?,?,?)")) {
      for (Map.Entry<Long, Integer> entry : value.getSoLuongGiamTheoLo().entrySet()) {
        s.setString(1, value.getMaDieuChinh());
        s.setLong(2, entry.getKey());
        s.setInt(3, entry.getValue());
        s.addBatch();
      }
      s.executeBatch();
    }
  }
  public List<DieuChinhTon> search(String keyword, Connection c) throws SQLException {
    List<DieuChinhTon> out = new ArrayList<DieuChinhTon>();
    try (PreparedStatement s = c.prepareStatement(
             "SELECT ma_dieu_chinh FROM DIEU_CHINH_TON WHERE ? IS NULL OR ma_dieu_chinh LIKE ? OR "
             + "ly_do LIKE ? ORDER BY thoi_diem_lap DESC")) {
      s.setString(1, keyword);
      s.setString(2, keyword == null ? null : "%" + keyword + "%");
      s.setString(3, keyword == null ? null : "%" + keyword + "%");
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) out.add(detail(r.getString(1), c));
      }
    }
    return out;
  }
  public DieuChinhTon detail(String id, Connection c) throws SQLException {
    DieuChinhTon value = new DieuChinhTon();
    try (PreparedStatement s = c.prepareStatement(
             "SELECT ma_nhan_vien,thoi_diem_lap,ly_do FROM DIEU_CHINH_TON WHERE ma_dieu_chinh=?")) {
      s.setString(1, id);
      try (ResultSet r = s.executeQuery()) {
        if (!r.next())
          return null;
        value.setMaDieuChinh(id);
        value.setMaNhanVien(r.getString(1));
        value.setThoiDiemLap(r.getTimestamp(2).toLocalDateTime());
        value.setLyDo(r.getString(3));
      }
    }
    try (PreparedStatement s = c.prepareStatement(
             "SELECT ma_lo,so_luong_giam FROM CHI_TIET_DIEU_CHINH WHERE ma_dieu_chinh=?")) {
      s.setString(1, id);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) value.getSoLuongGiamTheoLo().put(r.getLong(1), r.getInt(2));
      }
    }
    return value;
  }
}
