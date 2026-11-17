package dao;

import entity.NhaCungCap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NhaCungCapDAO {
  public NhaCungCap find(String id, Connection c) throws SQLException {
    try (PreparedStatement s = c.prepareStatement(
             "SELECT * FROM NHA_CUNG_CAP WHERE ma_nha_cung_cap=?")) {
      s.setString(1, id);
      try (ResultSet r = s.executeQuery()) {
        if (!r.next())
          return null;
        NhaCungCap v = new NhaCungCap();
        v.setMaNhaCungCap(r.getString("ma_nha_cung_cap"));
        v.setTenNhaCungCap(r.getString("ten_nha_cung_cap"));
        v.setSoDienThoai(r.getString("so_dien_thoai"));
        v.setDiaChi(r.getString("dia_chi"));
        v.setTrangThai(r.getString("trang_thai"));
        return v;
      }
    }
  }
  public void save(NhaCungCap v, Connection c) throws SQLException {
    String q = "MERGE NHA_CUNG_CAP AS target USING (VALUES (?,?,?,?,?)) AS source "
        + "(ma_nha_cung_cap,ten_nha_cung_cap,so_dien_thoai,dia_chi,trang_thai) "
        + "ON target.ma_nha_cung_cap=source.ma_nha_cung_cap WHEN MATCHED THEN UPDATE SET "
        + "ten_nha_cung_cap=source.ten_nha_cung_cap,so_dien_thoai=source.so_dien_thoai,"
        + "dia_chi=source.dia_chi,trang_thai=source.trang_thai WHEN NOT MATCHED THEN INSERT "
        + "(ma_nha_cung_cap,ten_nha_cung_cap,so_dien_thoai,dia_chi,trang_thai) VALUES "
        + "(source.ma_nha_cung_cap,source.ten_nha_cung_cap,source.so_dien_thoai,"
        + "source.dia_chi,source.trang_thai);";
    try (PreparedStatement s = c.prepareStatement(q)) {
      s.setString(1, v.getMaNhaCungCap());
      s.setString(2, v.getTenNhaCungCap());
      s.setString(3, v.getSoDienThoai());
      s.setString(4, v.getDiaChi());
      s.setString(5, v.getTrangThai());
      s.executeUpdate();
    }
  }
  public List<NhaCungCap> search(String keyword, String trangThai, Connection c) throws SQLException {
    List<NhaCungCap> out = new ArrayList<NhaCungCap>();
    try (PreparedStatement s = c.prepareStatement(
             "SELECT * FROM NHA_CUNG_CAP WHERE (? IS NULL OR ma_nha_cung_cap LIKE ? OR "
             + "ten_nha_cung_cap LIKE ? OR so_dien_thoai LIKE ?) AND (? IS NULL OR trang_thai=?) "
             + "ORDER BY ma_nha_cung_cap")) {
      s.setString(1, keyword);
      s.setString(2, keyword == null ? null : "%" + keyword + "%");
      s.setString(3, keyword == null ? null : "%" + keyword + "%");
      s.setString(4, keyword == null ? null : "%" + keyword + "%");
      s.setString(5, trangThai);
      s.setString(6, trangThai);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) {
          NhaCungCap v = new NhaCungCap();
          v.setMaNhaCungCap(r.getString("ma_nha_cung_cap"));
          v.setTenNhaCungCap(r.getString("ten_nha_cung_cap"));
          v.setSoDienThoai(r.getString("so_dien_thoai"));
          v.setDiaChi(r.getString("dia_chi"));
          v.setTrangThai(r.getString("trang_thai"));
          out.add(v);
        }
      }
    }
    return out;
  }
  public String nextId(Connection c) throws SQLException {
    return IdDAO.next("NCC", c);
  }
  public void delete(String id, Connection c) throws SQLException {
    try (PreparedStatement s =
             c.prepareStatement("DELETE FROM NHA_CUNG_CAP WHERE ma_nha_cung_cap=?")) {
      s.setString(1, id);
      s.executeUpdate();
    }
  }
}
