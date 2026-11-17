package dao;

import entity.SanPham;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SanPham_DAO {
  public void luu(SanPham value, Connection connection) throws SQLException {
    String sql = "MERGE SAN_PHAM AS target USING (VALUES (?, ?, ?, ?, ?, ?, ?)) AS source "
        + "(ma_san_pham, ma_loai, ten_san_pham, don_vi_tinh, gia_ban, nguong_ton, trang_thai) "
        + "ON target.ma_san_pham = source.ma_san_pham WHEN MATCHED THEN UPDATE SET "
        + "ma_loai=source.ma_loai, ten_san_pham=source.ten_san_pham, "
        + "don_vi_tinh=source.don_vi_tinh, gia_ban=source.gia_ban, nguong_ton=source.nguong_ton, "
        + "trang_thai=source.trang_thai WHEN NOT MATCHED THEN INSERT "
        + "(ma_san_pham,ma_loai,ten_san_pham,don_vi_tinh,gia_ban,nguong_ton,trang_thai) "
        + "VALUES (source.ma_san_pham,source.ma_loai,source.ten_san_pham,source.don_vi_tinh,"
        + "source.gia_ban,source.nguong_ton,source.trang_thai);";
    try (PreparedStatement s = connection.prepareStatement(sql)) {
      s.setString(1, value.getMaSanPham());
      s.setString(2, value.getMaLoai());
      s.setString(3, value.getTenSanPham());
      s.setString(4, value.getDonViTinh());
      s.setBigDecimal(5, value.getGiaBan());
      s.setInt(6, value.getNguongTon());
      s.setString(7, value.getTrangThai());
      s.executeUpdate();
    }
  }
  public SanPham timTheoMa(String maSanPham, Connection connection) throws SQLException {
    try (PreparedStatement s =
             connection.prepareStatement("SELECT * FROM SAN_PHAM WHERE ma_san_pham = ?")) {
      s.setString(1, maSanPham);
      try (ResultSet r = s.executeQuery()) {
        return r.next() ? taoTuKetQua(r) : null;
      }
    }
  }
  public List<SanPham> timKiem(String tuKhoa, String maLoai, String trangThai, BigDecimal tuGia,
      BigDecimal denGia, Connection connection) throws SQLException {
    String sql =
        "SELECT * FROM SAN_PHAM WHERE (? IS NULL OR ten_san_pham LIKE ? OR ma_san_pham LIKE ?) AND "
        + "(? IS NULL OR ma_loai = ?) AND (? IS NULL OR trang_thai = ?) AND (? IS NULL OR gia_ban "
        + ">= ?) AND (? IS NULL OR gia_ban <= ?) ORDER BY ma_san_pham";
    List<SanPham> values = new ArrayList<SanPham>();
    try (PreparedStatement s = connection.prepareStatement(sql)) {
      s.setString(1, tuKhoa);
      s.setString(2, tuKhoa == null ? null : "%" + tuKhoa + "%");
      s.setString(3, tuKhoa == null ? null : "%" + tuKhoa + "%");
      s.setString(4, maLoai);
      s.setString(5, maLoai);
      s.setString(6, trangThai);
      s.setString(7, trangThai);
      s.setBigDecimal(8, tuGia);
      s.setBigDecimal(9, tuGia);
      s.setBigDecimal(10, denGia);
      s.setBigDecimal(11, denGia);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) values.add(taoTuKetQua(r));
      }
    }
    return values;
  }
  public void xoa(String maSanPham, Connection connection) throws SQLException {
    try (PreparedStatement s =
             connection.prepareStatement("DELETE FROM SAN_PHAM WHERE ma_san_pham = ?")) {
      s.setString(1, maSanPham);
      s.executeUpdate();
    }
  }
  private SanPham taoTuKetQua(ResultSet r) throws SQLException {
    SanPham v = new SanPham();
    v.setMaSanPham(r.getString("ma_san_pham"));
    v.setMaLoai(r.getString("ma_loai"));
    v.setTenSanPham(r.getString("ten_san_pham"));
    v.setDonViTinh(r.getString("don_vi_tinh"));
    v.setGiaBan(r.getBigDecimal("gia_ban"));
    v.setNguongTon(r.getInt("nguong_ton"));
    v.setTrangThai(r.getString("trang_thai"));
    return v;
  }
}
