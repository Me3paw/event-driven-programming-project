package dao;

import entity.KhuyenMai;
import entity.PhamViKhuyenMai;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class KhuyenMaiDAO {
  public KhuyenMai active(String ma, LocalDateTime time, Connection c) throws SQLException {
    String q = "SELECT * FROM KHUYEN_MAI WHERE ma_khuyen_mai=? AND trang_thai='DANG_HOAT_DONG' AND "
        + "bat_dau<=? AND ket_thuc>=?";
    try (PreparedStatement s = c.prepareStatement(q)) {
      s.setString(1, ma);
      s.setTimestamp(2, Timestamp.valueOf(time));
      s.setTimestamp(3, Timestamp.valueOf(time));
      try (ResultSet r = s.executeQuery()) {
        if (!r.next())
          return null;
        KhuyenMai v = map(r);
        if (PhamViKhuyenMai.THEO_SAN_PHAM.name().equals(v.getPhamVi()))
          loadProducts(v, c);
        return v;
      }
    }
  }
  public List<KhuyenMai> activeForProducts(Set<String> productIds, LocalDateTime time, Connection c)
      throws SQLException {
    List<KhuyenMai> out = new ArrayList<KhuyenMai>();
    StringBuilder q = new StringBuilder(
        "SELECT DISTINCT k.* FROM KHUYEN_MAI k LEFT JOIN KHUYEN_MAI_SAN_PHAM p ON "
        + "p.ma_khuyen_mai=k.ma_khuyen_mai WHERE k.trang_thai='DANG_HOAT_DONG' AND k.bat_dau<=? "
        + "AND k.ket_thuc>=? AND (k.pham_vi='TOAN_DON'");
    if (productIds != null && !productIds.isEmpty()) {
      q.append(" OR p.ma_san_pham IN (");
      for (int i = 0; i < productIds.size(); i++) q.append(i == 0 ? "?" : ",?");
      q.append(")");
    }
    q.append(") ORDER BY k.ty_le_giam DESC,k.ma_khuyen_mai");
    try (PreparedStatement s = c.prepareStatement(q.toString())) {
      int i = 1;
      s.setTimestamp(i++, Timestamp.valueOf(time));
      s.setTimestamp(i++, Timestamp.valueOf(time));
      if (productIds != null)
        for (String id : productIds) s.setString(i++, id);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) {
          KhuyenMai v = map(r);
          if (PhamViKhuyenMai.THEO_SAN_PHAM.name().equals(v.getPhamVi()))
            loadProducts(v, c);
          out.add(v);
        }
      }
    }
    return out;
  }
  public void save(KhuyenMai v, Connection c) throws SQLException {
    String q = "INSERT INTO KHUYEN_MAI "
        + "(ma_khuyen_mai,ten_khuyen_mai,pham_vi,ty_le_giam,bat_dau,ket_thuc,trang_thai) VALUES "
        + "(?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "
        + "ten_khuyen_mai=VALUES(ten_khuyen_mai),pham_vi=VALUES(pham_vi),ty_le_giam=VALUES(ty_le_"
        + "giam),bat_dau=VALUES(bat_dau),ket_thuc=VALUES(ket_thuc),trang_thai=VALUES(trang_thai)";
    try (PreparedStatement s = c.prepareStatement(q)) {
      s.setString(1, v.getMaKhuyenMai());
      s.setString(2, v.getTenKhuyenMai());
      s.setString(3, v.getPhamVi());
      s.setBigDecimal(4, v.getTyLeGiam());
      s.setTimestamp(5, Timestamp.valueOf(v.getBatDau()));
      s.setTimestamp(6, Timestamp.valueOf(v.getKetThuc()));
      s.setString(7, v.getTrangThai());
      s.executeUpdate();
    }
    try (PreparedStatement d =
             c.prepareStatement("DELETE FROM KHUYEN_MAI_SAN_PHAM WHERE ma_khuyen_mai=?")) {
      d.setString(1, v.getMaKhuyenMai());
      d.executeUpdate();
    }
    if (PhamViKhuyenMai.THEO_SAN_PHAM.name().equals(v.getPhamVi()))
      try (PreparedStatement i = c.prepareStatement(
               "INSERT INTO KHUYEN_MAI_SAN_PHAM (ma_khuyen_mai,ma_san_pham) VALUES (?,?)")) {
        for (String id : v.getMaSanPham()) {
          i.setString(1, v.getMaKhuyenMai());
          i.setString(2, id);
          i.addBatch();
        }
        i.executeBatch();
      }
  }
  public List<KhuyenMai> search(String keyword, String trangThai, Connection c)
      throws SQLException {
    List<KhuyenMai> out = new ArrayList<KhuyenMai>();
    String q = "SELECT * FROM KHUYEN_MAI WHERE (? IS NULL OR ma_khuyen_mai LIKE ? OR "
        + "ten_khuyen_mai LIKE ?) AND (? IS NULL OR trang_thai=?) ORDER BY bat_dau DESC";
    try (PreparedStatement s = c.prepareStatement(q)) {
      s.setString(1, keyword);
      s.setString(2, keyword == null ? null : "%" + keyword + "%");
      s.setString(3, keyword == null ? null : "%" + keyword + "%");
      s.setString(4, trangThai);
      s.setString(5, trangThai);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) {
          KhuyenMai v = map(r);
          if (PhamViKhuyenMai.THEO_SAN_PHAM.name().equals(v.getPhamVi()))
            loadProducts(v, c);
          out.add(v);
        }
      }
    }
    return out;
  }
  public void updateStatus(String maKhuyenMai, String trangThai, Connection c) throws SQLException {
    try (PreparedStatement s =
             c.prepareStatement("UPDATE KHUYEN_MAI SET trang_thai=? WHERE ma_khuyen_mai=?")) {
      s.setString(1, trangThai);
      s.setString(2, maKhuyenMai);
      if (s.executeUpdate() != 1)
        throw new IllegalArgumentException("Không tìm thấy khuyến mãi");
    }
  }
  public boolean referenced(String maKhuyenMai, Connection c) throws SQLException {
    try (PreparedStatement s =
             c.prepareStatement("SELECT 1 FROM HOA_DON WHERE ma_khuyen_mai=? LIMIT 1")) {
      s.setString(1, maKhuyenMai);
      try (ResultSet r = s.executeQuery()) {
        return r.next();
      }
    }
  }
  public void delete(String maKhuyenMai, Connection c) throws SQLException {
    try (PreparedStatement s =
             c.prepareStatement("DELETE FROM KHUYEN_MAI_SAN_PHAM WHERE ma_khuyen_mai=?")) {
      s.setString(1, maKhuyenMai);
      s.executeUpdate();
    }
    try (PreparedStatement s = c.prepareStatement("DELETE FROM KHUYEN_MAI WHERE ma_khuyen_mai=?")) {
      s.setString(1, maKhuyenMai);
      if (s.executeUpdate() != 1)
        throw new IllegalArgumentException("Không tìm thấy khuyến mãi");
    }
  }
  private void loadProducts(KhuyenMai v, Connection c) throws SQLException {
    try (PreparedStatement s = c.prepareStatement(
             "SELECT ma_san_pham FROM KHUYEN_MAI_SAN_PHAM WHERE ma_khuyen_mai=?")) {
      s.setString(1, v.getMaKhuyenMai());
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) v.getMaSanPham().add(r.getString(1));
      }
    }
  }
  private KhuyenMai map(ResultSet r) throws SQLException {
    KhuyenMai v = new KhuyenMai();
    v.setMaKhuyenMai(r.getString("ma_khuyen_mai"));
    v.setTenKhuyenMai(r.getString("ten_khuyen_mai"));
    v.setPhamVi(r.getString("pham_vi"));
    v.setTyLeGiam(r.getBigDecimal("ty_le_giam"));
    v.setBatDau(r.getTimestamp("bat_dau").toLocalDateTime());
    v.setKetThuc(r.getTimestamp("ket_thuc").toLocalDateTime());
    v.setTrangThai(r.getString("trang_thai"));
    return v;
  }
}
