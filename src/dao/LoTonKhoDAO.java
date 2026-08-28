package dao;

import entity.LoTonKho;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class LoTonKhoDAO {
  public List<LoTonKho> lockFifo(String maSanPham, Connection c) throws SQLException {
    String sql = "SELECT "
        + "l.ma_lo,l.ma_phieu_nhap,l.ma_san_pham,l.so_luong_nhap,l.so_luong_con,l.thoi_diem_nhap,p."
        + "don_gia_nhap FROM LO_TON_KHO l JOIN CHI_TIET_PHIEU_NHAP p ON "
        + "p.ma_phieu_nhap=l.ma_phieu_nhap AND p.ma_san_pham=l.ma_san_pham WHERE l.ma_san_pham=? "
        + "AND l.so_luong_con>0 ORDER BY l.thoi_diem_nhap,l.ma_lo FOR UPDATE";
    List<LoTonKho> values = new ArrayList<LoTonKho>();
    try (PreparedStatement s = c.prepareStatement(sql)) {
      s.setString(1, maSanPham);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) values.add(map(r));
      }
    }
    return values;
  }
  public void create(String maPhieuNhap, String maSanPham, int soLuong,
      java.time.LocalDateTime time, Connection c) throws SQLException {
    try (PreparedStatement s = c.prepareStatement("INSERT INTO LO_TON_KHO "
             + "(ma_phieu_nhap,ma_san_pham,so_luong_nhap,so_luong_con,thoi_diem_"
             + "nhap) VALUES (?,?,?,?,?)")) {
      s.setString(1, maPhieuNhap);
      s.setString(2, maSanPham);
      s.setInt(3, soLuong);
      s.setInt(4, soLuong);
      s.setTimestamp(5, Timestamp.valueOf(time));
      s.executeUpdate();
    }
  }
  public void change(long maLo, int delta, Connection c) throws SQLException {
    try (PreparedStatement s =
             c.prepareStatement("UPDATE LO_TON_KHO SET so_luong_con=so_luong_con+? WHERE ma_lo=? "
                 + "AND so_luong_con+?>=0")) {
      s.setInt(1, delta);
      s.setLong(2, maLo);
      s.setInt(3, delta);
      if (s.executeUpdate() != 1)
        throw new IllegalStateException("Tồn kho không đủ");
    }
  }
  public int ton(String maSanPham, Connection c) throws SQLException {
    try (PreparedStatement s = c.prepareStatement(
             "SELECT COALESCE(SUM(so_luong_con),0) FROM LO_TON_KHO WHERE ma_san_pham=?")) {
      s.setString(1, maSanPham);
      try (ResultSet r = s.executeQuery()) {
        r.next();
        return r.getInt(1);
      }
    }
  }
  public List<LoTonKho> search(String keyword, Connection c) throws SQLException {
    List<LoTonKho> out = new ArrayList<LoTonKho>();
    String q = "SELECT "
        + "l.ma_lo,l.ma_phieu_nhap,l.ma_san_pham,l.so_luong_nhap,l.so_luong_con,l.thoi_diem_"
        + "nhap,p.don_gia_nhap FROM LO_TON_KHO l JOIN CHI_TIET_PHIEU_NHAP p ON "
        + "p.ma_phieu_nhap=l.ma_phieu_nhap AND p.ma_san_pham=l.ma_san_pham WHERE ? IS NULL "
        + "OR l.ma_san_pham LIKE ? ORDER BY l.thoi_diem_nhap,l.ma_lo";
    try (PreparedStatement s = c.prepareStatement(q)) {
      s.setString(1, keyword);
      s.setString(2, keyword == null ? null : "%" + keyword + "%");
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) out.add(map(r));
      }
    }
    return out;
  }
  private LoTonKho map(ResultSet r) throws SQLException {
    LoTonKho v = new LoTonKho();
    v.setMaLo(r.getLong("ma_lo"));
    v.setMaPhieuNhap(r.getString("ma_phieu_nhap"));
    v.setMaSanPham(r.getString("ma_san_pham"));
    v.setSoLuongNhap(r.getInt("so_luong_nhap"));
    v.setSoLuongCon(r.getInt("so_luong_con"));
    v.setDonGiaVon(r.getBigDecimal("don_gia_nhap"));
    v.setThoiDiemNhap(r.getTimestamp("thoi_diem_nhap").toLocalDateTime());
    return v;
  }
}
