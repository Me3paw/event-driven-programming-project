package dao;

import entity.KhachHang;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class KhachHangDAO {
  public void save(KhachHang value, Connection c) throws SQLException {
    String sql = "INSERT INTO KHACH_HANG (ma_khach_hang, ho_ten, so_dien_thoai, diem_hien_co, "
        + "diem_tich_luy, trang_thai) VALUES (?, ?, ?, 0, 0, ?) ON DUPLICATE KEY UPDATE "
        + "ho_ten=VALUES(ho_ten), so_dien_thoai=VALUES(so_dien_thoai), "
        + "trang_thai=VALUES(trang_thai)";
    try (PreparedStatement s = c.prepareStatement(sql)) {
      s.setString(1, value.getMaKhachHang());
      s.setString(2, value.getHoTen());
      s.setString(3, value.getSoDienThoai());
      s.setString(4, value.getTrangThai());
      s.executeUpdate();
    }
  }
  public KhachHang lock(String maKhachHang, Connection c) throws SQLException {
    try (PreparedStatement s =
             c.prepareStatement("SELECT * FROM KHACH_HANG WHERE ma_khach_hang = ? FOR UPDATE")) {
      s.setString(1, maKhachHang);
      try (ResultSet r = s.executeQuery()) {
        return r.next() ? map(r) : null;
      }
    }
  }
  public KhachHang find(String maKhachHang, Connection c) throws SQLException {
    try (PreparedStatement s =
             c.prepareStatement("SELECT * FROM KHACH_HANG WHERE ma_khach_hang = ?")) {
      s.setString(1, maKhachHang);
      try (ResultSet r = s.executeQuery()) {
        return r.next() ? map(r) : null;
      }
    }
  }
  public BigDecimal actualSpend(String maKhachHang, Connection c) throws SQLException {
    try (PreparedStatement s =
             c.prepareStatement("SELECT COALESCE(SUM(tong_thanh_toan),0) FROM HOA_DON WHERE "
                 + "ma_khach_hang=? AND trang_thai='DA_THANH_TOAN'")) {
      s.setString(1, maKhachHang);
      try (ResultSet r = s.executeQuery()) {
        r.next();
        return r.getBigDecimal(1);
      }
    }
  }
  public List<KhachHang> byTier(String tier, Connection c) throws SQLException {
    List<KhachHang> out = new ArrayList<KhachHang>();
    String q = "SELECT * FROM KHACH_HANG WHERE CASE WHEN diem_tich_luy>=1000000 THEN 'VANG' WHEN "
        + "diem_tich_luy>=200000 THEN 'BAC' WHEN diem_tich_luy>=50000 THEN 'DONG' ELSE "
        + "'CHUA_XEP_HANG' END=? ORDER BY ma_khach_hang";
    try (PreparedStatement s = c.prepareStatement(q)) {
      s.setString(1, tier);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) out.add(map(r));
      }
    }
    return out;
  }
  public List<KhachHang> search(String keyword, String trangThai, Connection c)
      throws SQLException {
    List<KhachHang> values = new ArrayList<KhachHang>();
    try (PreparedStatement s = c.prepareStatement(
             "SELECT * FROM KHACH_HANG WHERE (? IS NULL OR ma_khach_hang LIKE ? OR ho_ten LIKE ? "
             + "OR so_dien_thoai LIKE ?) "
             + "AND (? IS NULL OR trang_thai=?) ORDER BY ma_khach_hang")) {
      s.setString(1, keyword);
      s.setString(2, keyword == null ? null : "%" + keyword + "%");
      s.setString(3, keyword == null ? null : "%" + keyword + "%");
      s.setString(4, keyword == null ? null : "%" + keyword + "%");
      s.setString(5, trangThai);
      s.setString(6, trangThai);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) values.add(map(r));
      }
    }
    return values;
  }
  public void updatePoints(String maKhachHang, long rewardDelta, long loyaltyDelta, Connection c)
      throws SQLException {
    try (PreparedStatement s =
             c.prepareStatement("UPDATE KHACH_HANG SET diem_hien_co=diem_hien_co+?, "
                 + "diem_tich_luy=diem_tich_luy+? WHERE ma_khach_hang=?")) {
      s.setLong(1, rewardDelta);
      s.setLong(2, loyaltyDelta);
      s.setString(3, maKhachHang);
      s.executeUpdate();
    }
  }
  public void delete(String id, Connection c) throws SQLException {
    try (PreparedStatement s = c.prepareStatement("DELETE FROM KHACH_HANG WHERE ma_khach_hang=?")) {
      s.setString(1, id);
      s.executeUpdate();
    }
  }
  private KhachHang map(ResultSet r) throws SQLException {
    KhachHang v = new KhachHang();
    v.setMaKhachHang(r.getString("ma_khach_hang"));
    v.setHoTen(r.getString("ho_ten"));
    v.setSoDienThoai(r.getString("so_dien_thoai"));
    v.setDiemHienCo(r.getLong("diem_hien_co"));
    v.setDiemTichLuy(r.getLong("diem_tich_luy"));
    v.setTrangThai(r.getString("trang_thai"));
    return v;
  }
}
