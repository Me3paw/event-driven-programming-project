package dao;

import entity.ChiTietHoaDon;
import entity.HoaDon;
import entity.PhanBoXuatLo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HoaDonDAO {
  public void save(HoaDon h, Connection c) throws SQLException {
    String q = "INSERT INTO HOA_DON "
        + "(ma_hoa_don,ma_nhan_vien,ma_khach_hang,ma_khuyen_mai,ma_nhan_vien_huy,thoi_diem_"
        + "lap,thoi_diem_huy,phuong_thuc_tt,ma_tham_chieu_tt,tong_gia_goc,loai_giam_gia,ty_"
        + "le_giam,tien_giam,diem_su_dung,tien_giam_diem,diem_tich_luy_kiem,diem_thuong_"
        + "kiem,ty_le_vat,tien_vat,tong_thanh_toan,tien_khach_dua,tien_thua,trang_thai,ly_"
        + "do_huy) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    try (PreparedStatement s = c.prepareStatement(q)) {
      int i = 1;
      s.setString(i++, h.getMaHoaDon());
      s.setString(i++, h.getMaNhanVien());
      s.setString(i++, h.getMaKhachHang());
      s.setString(i++, h.getMaKhuyenMai());
      s.setString(i++, h.getMaNhanVienHuy());
      s.setTimestamp(i++, Timestamp.valueOf(h.getThoiDiemLap()));
      if (h.getThoiDiemHuy() == null)
        s.setTimestamp(i++, null);
      else
        s.setTimestamp(i++, Timestamp.valueOf(h.getThoiDiemHuy()));
      s.setString(i++, h.getPhuongThucTt());
      s.setString(i++, h.getMaThamChieuTt());
      s.setBigDecimal(i++, h.getTongGiaGoc());
      s.setString(i++, h.getLoaiGiamGia());
      s.setBigDecimal(i++, h.getTyLeGiam());
      s.setBigDecimal(i++, h.getTienGiam());
      s.setLong(i++, h.getDiemSuDung());
      s.setBigDecimal(i++, h.getTienGiamDiem());
      s.setLong(i++, h.getDiemTichLuyKiem());
      s.setLong(i++, h.getDiemThuongKiem());
      s.setBigDecimal(i++, h.getTyLeVat());
      s.setBigDecimal(i++, h.getTienVat());
      s.setBigDecimal(i++, h.getTongThanhToan());
      s.setBigDecimal(i++, h.getTienKhachDua());
      s.setBigDecimal(i++, h.getTienThua());
      s.setString(i++, h.getTrangThai());
      s.setString(i++, h.getLyDoHuy());
      s.executeUpdate();
    }
    try (PreparedStatement s = c.prepareStatement("INSERT INTO CHI_TIET_HOA_DON "
             + "(ma_hoa_don,ma_san_pham,so_luong,don_gia_ban,tien_giam_dong,"
             + "thanh_tien) VALUES (?,?,?,?,?,?)")) {
      for (ChiTietHoaDon d : h.getChiTiet()) {
        s.setString(1, h.getMaHoaDon());
        s.setString(2, d.getMaSanPham());
        s.setInt(3, d.getSoLuong());
        s.setBigDecimal(4, d.getDonGiaBan());
        s.setBigDecimal(5, d.getTienGiamDong());
        s.setBigDecimal(6, d.getThanhTien());
        s.addBatch();
      }
      s.executeBatch();
    }
    try (PreparedStatement s = c.prepareStatement(
             "INSERT INTO PHAN_BO_XUAT_LO (ma_hoa_don,ma_san_pham,ma_lo,so_luong_xuat,don_gia_von) "
             + "VALUES (?,?,?,?,?)")) {
      for (PhanBoXuatLo p : h.getPhanBo()) {
        s.setString(1, h.getMaHoaDon());
        s.setString(2, p.getMaSanPham());
        s.setLong(3, p.getMaLo());
        s.setInt(4, p.getSoLuongXuat());
        s.setBigDecimal(5, p.getDonGiaVon());
        s.addBatch();
      }
      s.executeBatch();
    }
  }
  public HoaDon lock(String maHoaDon, Connection c) throws SQLException {
    try (PreparedStatement s =
             c.prepareStatement("SELECT * FROM HOA_DON WHERE ma_hoa_don=? FOR UPDATE")) {
      s.setString(1, maHoaDon);
      try (ResultSet r = s.executeQuery()) {
        return r.next() ? map(r) : null;
      }
    }
  }
  public List<PhanBoXuatLo> allocations(String maHoaDon, Connection c) throws SQLException {
    List<PhanBoXuatLo> out = new ArrayList<PhanBoXuatLo>();
    try (PreparedStatement s =
             c.prepareStatement("SELECT ma_san_pham,ma_lo,so_luong_xuat,don_gia_von FROM "
                 + "PHAN_BO_XUAT_LO WHERE ma_hoa_don=?")) {
      s.setString(1, maHoaDon);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) {
          PhanBoXuatLo p = new PhanBoXuatLo();
          p.setMaSanPham(r.getString(1));
          p.setMaLo(r.getLong(2));
          p.setSoLuongXuat(r.getInt(3));
          p.setDonGiaVon(r.getBigDecimal(4));
          out.add(p);
        }
      }
    }
    return out;
  }
  public void cancel(String maHoaDon, String manager, String reason, Connection c)
      throws SQLException {
    try (
        PreparedStatement s = c.prepareStatement(
            "UPDATE HOA_DON SET trang_thai='DA_HUY',ma_nhan_vien_huy=?,thoi_diem_huy=?,ly_do_huy=? "
            + "WHERE ma_hoa_don=? AND trang_thai='DA_THANH_TOAN'")) {
      s.setString(1, manager);
      s.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
      s.setString(3, reason);
      s.setString(4, maHoaDon);
      if (s.executeUpdate() != 1)
        throw new IllegalStateException("Không thể hủy hóa đơn");
    }
  }
  public List<HoaDon> search(String keyword, String maNhanVien, String maKhachHang,
      String trangThai, String phuongThuc, LocalDate from, LocalDate to, Connection c)
      throws SQLException {
    List<String> ids = new ArrayList<String>();
    String q =
        "SELECT ma_hoa_don FROM HOA_DON WHERE (? IS NULL OR ma_hoa_don LIKE ?) AND (? IS NULL OR "
        + "ma_nhan_vien=?) AND (? IS NULL OR ma_khach_hang=?) AND (? IS NULL OR trang_thai=?) AND "
        + "(? IS NULL OR phuong_thuc_tt=?) AND (? IS NULL OR DATE(thoi_diem_lap)>=?) AND (? IS "
        + "NULL OR DATE(thoi_diem_lap)<=?) ORDER BY thoi_diem_lap DESC";
    try (PreparedStatement s = c.prepareStatement(q)) {
      s.setString(1, keyword);
      s.setString(2, keyword == null ? null : "%" + keyword + "%");
      s.setString(3, maNhanVien);
      s.setString(4, maNhanVien);
      s.setString(5, maKhachHang);
      s.setString(6, maKhachHang);
      s.setString(7, trangThai);
      s.setString(8, trangThai);
      s.setString(9, phuongThuc);
      s.setString(10, phuongThuc);
      if (from == null) {
        s.setDate(11, null);
        s.setDate(12, null);
      } else {
        s.setDate(11, java.sql.Date.valueOf(from));
        s.setDate(12, java.sql.Date.valueOf(from));
      }
      if (to == null) {
        s.setDate(13, null);
        s.setDate(14, null);
      } else {
        s.setDate(13, java.sql.Date.valueOf(to));
        s.setDate(14, java.sql.Date.valueOf(to));
      }
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) ids.add(r.getString(1));
      }
    }
    List<HoaDon> out = new ArrayList<HoaDon>();
    for (String id : ids) out.add(detail(id, c));
    return out;
  }
  public List<HoaDon> byCustomer(String maKhachHang, Connection c) throws SQLException {
    List<HoaDon> out = new ArrayList<HoaDon>();
    try (PreparedStatement s = c.prepareStatement(
             "SELECT ma_hoa_don FROM HOA_DON WHERE ma_khach_hang=? ORDER BY thoi_diem_lap DESC")) {
      s.setString(1, maKhachHang);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) out.add(detail(r.getString(1), c));
      }
    }
    return out;
  }
  public HoaDon detail(String maHoaDon, Connection c) throws SQLException {
    HoaDon h;
    try (PreparedStatement s = c.prepareStatement("SELECT * FROM HOA_DON WHERE ma_hoa_don=?")) {
      s.setString(1, maHoaDon);
      try (ResultSet r = s.executeQuery()) {
        if (!r.next())
          return null;
        h = map(r);
      }
    }
    try (PreparedStatement s =
             c.prepareStatement("SELECT ma_san_pham,so_luong,don_gia_ban,tien_giam_dong,thanh_tien "
                 + "FROM CHI_TIET_HOA_DON WHERE ma_hoa_don=?")) {
      s.setString(1, maHoaDon);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) {
          ChiTietHoaDon d = new ChiTietHoaDon();
          d.setMaSanPham(r.getString(1));
          d.setSoLuong(r.getInt(2));
          d.setDonGiaBan(r.getBigDecimal(3));
          d.setTienGiamDong(r.getBigDecimal(4));
          d.setThanhTien(r.getBigDecimal(5));
          h.getChiTiet().add(d);
        }
      }
    }
    h.setPhanBo(allocations(maHoaDon, c));
    return h;
  }
  private HoaDon map(ResultSet r) throws SQLException {
    HoaDon h = new HoaDon();
    h.setMaHoaDon(r.getString("ma_hoa_don"));
    h.setMaNhanVien(r.getString("ma_nhan_vien"));
    h.setMaKhachHang(r.getString("ma_khach_hang"));
    h.setMaKhuyenMai(r.getString("ma_khuyen_mai"));
    h.setMaNhanVienHuy(r.getString("ma_nhan_vien_huy"));
    h.setThoiDiemLap(r.getTimestamp("thoi_diem_lap").toLocalDateTime());
    Timestamp huy = r.getTimestamp("thoi_diem_huy");
    if (huy != null)
      h.setThoiDiemHuy(huy.toLocalDateTime());
    h.setPhuongThucTt(r.getString("phuong_thuc_tt"));
    h.setMaThamChieuTt(r.getString("ma_tham_chieu_tt"));
    h.setTongGiaGoc(r.getBigDecimal("tong_gia_goc"));
    h.setLoaiGiamGia(r.getString("loai_giam_gia"));
    h.setTyLeGiam(r.getBigDecimal("ty_le_giam"));
    h.setTienGiam(r.getBigDecimal("tien_giam"));
    h.setDiemSuDung(r.getLong("diem_su_dung"));
    h.setTienGiamDiem(r.getBigDecimal("tien_giam_diem"));
    h.setDiemTichLuyKiem(r.getLong("diem_tich_luy_kiem"));
    h.setDiemThuongKiem(r.getLong("diem_thuong_kiem"));
    h.setTyLeVat(r.getBigDecimal("ty_le_vat"));
    h.setTienVat(r.getBigDecimal("tien_vat"));
    h.setTongThanhToan(r.getBigDecimal("tong_thanh_toan"));
    h.setTienKhachDua(r.getBigDecimal("tien_khach_dua"));
    h.setTienThua(r.getBigDecimal("tien_thua"));
    h.setTrangThai(r.getString("trang_thai"));
    h.setLyDoHuy(r.getString("ly_do_huy"));
    return h;
  }
}
