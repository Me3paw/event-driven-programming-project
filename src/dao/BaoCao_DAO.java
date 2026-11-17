package dao;

import entity.HangThanhVien;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import service.SoLuongTheoHangThanhVien;
import service.DoanhThuTheoNgay;
import service.SanPhamSapHetHang;
import service.DoanhSoSanPham;
import service.TongHopBaoCao;

public class BaoCao_DAO {
  public TongHopBaoCao tongHop(LocalDate from, LocalDate to, Connection c) throws SQLException {
    String paid = "trang_thai='DA_THANH_TOAN' AND CAST(thoi_diem_lap AS DATE) BETWEEN ? AND ?";
    String q = "SELECT COALESCE((SELECT SUM(tong_thanh_toan) FROM HOA_DON WHERE " + paid
        + "),0),COALESCE((SELECT COUNT(*) FROM HOA_DON WHERE " + paid
        + ("),0),COALESCE((SELECT SUM(tong_tien) FROM PHIEU_NHAP WHERE trang_thai='HOAN_TAT' AND "
            + "CAST(COALESCE(thoi_diem_hoan_tat,thoi_diem_lap) AS DATE) BETWEEN ? AND "
            + "?),0),COALESCE((SELECT SUM(p.so_luong_xuat*p.don_gia_von) FROM PHAN_BO_XUAT_LO p "
            + "JOIN HOA_DON h ON h.ma_hoa_don=p.ma_hoa_don WHERE h.")
        + paid + "),0)";
    try (PreparedStatement s = c.prepareStatement(q)) {
      Date start = Date.valueOf(from), end = Date.valueOf(to);
      s.setDate(1, start);
      s.setDate(2, end);
      s.setDate(3, start);
      s.setDate(4, end);
      s.setDate(5, start);
      s.setDate(6, end);
      s.setDate(7, start);
      s.setDate(8, end);
      try (ResultSet r = s.executeQuery()) {
        r.next();
        return new TongHopBaoCao(
            r.getBigDecimal(1), r.getLong(2), r.getBigDecimal(3), r.getBigDecimal(4));
      }
    }
  }
  public List<DoanhThuTheoNgay> doanhThuTheoNgay(LocalDate from, LocalDate to, Connection c)
      throws SQLException {
    List<DoanhThuTheoNgay> out = new ArrayList<DoanhThuTheoNgay>();
    String q = "SELECT CAST(thoi_diem_lap AS DATE),SUM(tong_thanh_toan),COUNT(*) FROM HOA_DON WHERE "
        + "trang_thai='DA_THANH_TOAN' AND CAST(thoi_diem_lap AS DATE) BETWEEN ? AND ? GROUP BY "
        + "CAST(thoi_diem_lap AS DATE) ORDER BY CAST(thoi_diem_lap AS DATE)";
    try (PreparedStatement s = c.prepareStatement(q)) {
      s.setDate(1, Date.valueOf(from));
      s.setDate(2, Date.valueOf(to));
      try (ResultSet r = s.executeQuery()) {
        while (r.next())
          out.add(new DoanhThuTheoNgay(r.getDate(1).toLocalDate(), r.getBigDecimal(2), r.getLong(3)));
      }
    }
    return out;
  }
  public List<DoanhSoSanPham> sanPhamBanChay(LocalDate from, LocalDate to, Connection c)
      throws SQLException {
    List<DoanhSoSanPham> out = new ArrayList<DoanhSoSanPham>();
    String q = "SELECT d.ma_san_pham,s.ten_san_pham,SUM(d.so_luong),SUM(d.thanh_tien) FROM "
        + "CHI_TIET_HOA_DON d JOIN HOA_DON h ON h.ma_hoa_don=d.ma_hoa_don JOIN SAN_PHAM s "
        + "ON s.ma_san_pham=d.ma_san_pham WHERE h.trang_thai='DA_THANH_TOAN' "
        + "AND "
        + "CAST(h.thoi_diem_lap AS DATE) BETWEEN ? AND ? GROUP BY d.ma_san_pham,s.ten_san_pham "
        + "ORDER BY SUM(d.so_luong) DESC,d.ma_san_pham";
    try (PreparedStatement s = c.prepareStatement(q)) {
      s.setDate(1, Date.valueOf(from));
      s.setDate(2, Date.valueOf(to));
      try (ResultSet r = s.executeQuery()) {
        while (r.next())
          out.add(
              new DoanhSoSanPham(r.getString(1), r.getString(2), r.getLong(3), r.getBigDecimal(4)));
      }
    }
    return out;
  }
  public List<SanPhamSapHetHang> sanPhamSapHetHang(Connection c) throws SQLException {
    List<SanPhamSapHetHang> out = new ArrayList<SanPhamSapHetHang>();
    String q = "SELECT s.ma_san_pham,s.ten_san_pham,COALESCE(SUM(l.so_luong_con),0),s.nguong_ton "
        + "FROM SAN_PHAM s LEFT JOIN LO_TON_KHO l ON l.ma_san_pham=s.ma_san_pham "
        + "WHERE s.trang_thai='DANG_KINH_DOANH' GROUP BY "
        + "s.ma_san_pham,s.ten_san_pham,s.nguong_ton HAVING "
        + "COALESCE(SUM(l.so_luong_con),0)<=s.nguong_ton ORDER BY s.ma_san_pham";
    try (PreparedStatement s = c.prepareStatement(q); ResultSet r = s.executeQuery()) {
      while (r.next())
        out.add(new SanPhamSapHetHang(r.getString(1), r.getString(2), r.getInt(3), r.getInt(4)));
    }
    return out;
  }
  public List<SoLuongTheoHangThanhVien> soLuongTheoHangThanhVien(Connection c) throws SQLException {
    List<SoLuongTheoHangThanhVien> out = new ArrayList<SoLuongTheoHangThanhVien>();
    Map<HangThanhVien, Long> counts = new EnumMap<HangThanhVien, Long>(HangThanhVien.class);
    String q =
        "SELECT CASE WHEN diem_tich_luy>=1000000 THEN 'VANG' WHEN diem_tich_luy>=200000 THEN 'BAC' "
        + "WHEN diem_tich_luy>=50000 THEN 'DONG' ELSE 'CHUA_XEP_HANG' END,COUNT(*) FROM KHACH_HANG "
        + "WHERE ma_khach_hang <> 'KH000' "
        + "GROUP BY CASE WHEN diem_tich_luy>=1000000 THEN 'VANG' WHEN diem_tich_luy>=200000 "
        + "THEN 'BAC' WHEN diem_tich_luy>=50000 THEN 'DONG' ELSE 'CHUA_XEP_HANG' END "
        + "ORDER BY CASE CASE WHEN diem_tich_luy>=1000000 THEN 'VANG' WHEN diem_tich_luy>=200000 "
        + "THEN 'BAC' WHEN diem_tich_luy>=50000 THEN 'DONG' ELSE 'CHUA_XEP_HANG' END "
        + "WHEN 'CHUA_XEP_HANG' THEN 1 WHEN 'DONG' THEN 2 WHEN 'BAC' THEN 3 WHEN 'VANG' THEN 4 END";
    try (PreparedStatement s = c.prepareStatement(q); ResultSet r = s.executeQuery()) {
      while (r.next()) counts.put(HangThanhVien.valueOf(r.getString(1)), r.getLong(2));
    }
    for (HangThanhVien hangThanhVien : HangThanhVien.values())
      out.add(new SoLuongTheoHangThanhVien(hangThanhVien.name(), counts.containsKey(hangThanhVien) ? counts.get(hangThanhVien) : 0L));
    return out;
  }
}
