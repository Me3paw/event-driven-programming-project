package dao;

import entity.ChiTietPhieuNhap;
import entity.PhieuNhap;
import entity.TrangThaiPhieuNhap;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PhieuNhap_DAO {
  public void luuBanNhap(PhieuNhap p, Connection c) throws SQLException {
    BigDecimal tong = BigDecimal.ZERO;
    for (ChiTietPhieuNhap d : p.getChiTiet()) {
      if (d == null || d.getMaSanPham() == null || d.getMaSanPham().trim().isEmpty()
          || d.getDonGiaNhap() == null || d.getSoLuong() <= 0 || d.getDonGiaNhap().signum() < 0)
        throw new IllegalArgumentException("Chi tiết nhập không hợp lệ");
      tong = tong.add(d.getDonGiaNhap().multiply(BigDecimal.valueOf(d.getSoLuong())));
    }
    p.setTongTien(tong);
    String q = "MERGE PHIEU_NHAP AS target USING (VALUES (?,?,?,?,?)) AS source "
        + "(ma_phieu_nhap,ma_nha_cung_cap,ma_nhan_vien,thoi_diem_lap,tong_tien) "
        + "ON target.ma_phieu_nhap=source.ma_phieu_nhap WHEN MATCHED THEN UPDATE SET "
        + "ma_nha_cung_cap=source.ma_nha_cung_cap,ma_nhan_vien=source.ma_nhan_vien,"
        + "tong_tien=source.tong_tien WHEN NOT MATCHED THEN INSERT "
        + "(ma_phieu_nhap,ma_nha_cung_cap,ma_nhan_vien,thoi_diem_lap,trang_thai,tong_tien) "
        + "VALUES (source.ma_phieu_nhap,source.ma_nha_cung_cap,source.ma_nhan_vien,"
        + "source.thoi_diem_lap,'NHAP',source.tong_tien);";
    try (PreparedStatement s = c.prepareStatement(q)) {
      s.setString(1, p.getMaPhieuNhap());
      s.setString(2, p.getMaNhaCungCap());
      s.setString(3, p.getMaNhanVien());
      s.setTimestamp(4,
          Timestamp.valueOf(p.getThoiDiemLap() == null ? LocalDateTime.now() : p.getThoiDiemLap()));
      s.setBigDecimal(5, tong);
      s.executeUpdate();
    }
    try (PreparedStatement d =
             c.prepareStatement("DELETE FROM CHI_TIET_PHIEU_NHAP WHERE ma_phieu_nhap=?")) {
      d.setString(1, p.getMaPhieuNhap());
      d.executeUpdate();
    }
    try (PreparedStatement i = c.prepareStatement(
             "INSERT INTO CHI_TIET_PHIEU_NHAP (ma_phieu_nhap,ma_san_pham,so_luong,don_gia_nhap) "
             + "VALUES (?,?,?,?)")) {
      for (ChiTietPhieuNhap d : p.getChiTiet()) {
        i.setString(1, p.getMaPhieuNhap());
        i.setString(2, d.getMaSanPham());
        i.setInt(3, d.getSoLuong());
        i.setBigDecimal(4, d.getDonGiaNhap());
        i.addBatch();
      }
      i.executeBatch();
    }
  }
  public PhieuNhap khoa(String id, Connection c) throws SQLException {
    try (PreparedStatement s =
             c.prepareStatement("SELECT * FROM PHIEU_NHAP WITH (UPDLOCK, ROWLOCK) WHERE ma_phieu_nhap=?")) {
      s.setString(1, id);
      try (ResultSet r = s.executeQuery()) {
        if (!r.next())
          return null;
        PhieuNhap p = new PhieuNhap();
        p.setMaPhieuNhap(r.getString("ma_phieu_nhap"));
        p.setMaNhaCungCap(r.getString("ma_nha_cung_cap"));
        p.setMaNhanVien(r.getString("ma_nhan_vien"));
        p.setThoiDiemLap(r.getTimestamp("thoi_diem_lap").toLocalDateTime());
        p.setTrangThai(r.getString("trang_thai"));
        p.setTongTien(r.getBigDecimal("tong_tien"));
      }
    }
    return layTheoMa(id, c);
  }
  public PhieuNhap layTheoMa(String id, Connection c) throws SQLException {
    PhieuNhap p = new PhieuNhap();
    try (PreparedStatement s = c.prepareStatement("SELECT "
             + "ma_nha_cung_cap,ma_nhan_vien,thoi_diem_lap,thoi_diem_hoan_tat,"
             + "trang_thai,tong_tien FROM PHIEU_NHAP WHERE ma_phieu_nhap=?")) {
      s.setString(1, id);
      try (ResultSet r = s.executeQuery()) {
        if (!r.next())
          return null;
        p.setMaPhieuNhap(id);
        p.setMaNhaCungCap(r.getString(1));
        p.setMaNhanVien(r.getString(2));
        p.setThoiDiemLap(r.getTimestamp(3).toLocalDateTime());
        Timestamp completed = r.getTimestamp(4);
        if (completed != null)
          p.setThoiDiemHoanTat(completed.toLocalDateTime());
        p.setTrangThai(r.getString(5));
        p.setTongTien(r.getBigDecimal(6));
      }
    }
    try (PreparedStatement s = c.prepareStatement("SELECT ma_san_pham,so_luong,don_gia_nhap FROM "
             + "CHI_TIET_PHIEU_NHAP WHERE ma_phieu_nhap=?")) {
      s.setString(1, id);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) {
          ChiTietPhieuNhap d = new ChiTietPhieuNhap();
          d.setMaSanPham(r.getString(1));
          d.setSoLuong(r.getInt(2));
          d.setDonGiaNhap(r.getBigDecimal(3));
          p.getChiTiet().add(d);
        }
      }
    }
    return p;
  }
  public void hoanTat(String id, LocalDateTime completedAt, Connection c) throws SQLException {
    try (PreparedStatement s =
             c.prepareStatement("UPDATE PHIEU_NHAP SET trang_thai='HOAN_TAT',thoi_diem_hoan_tat=? "
                 + "WHERE ma_phieu_nhap=? AND trang_thai='NHAP'")) {
      s.setTimestamp(1, Timestamp.valueOf(completedAt));
      s.setString(2, id);
      if (s.executeUpdate() != 1)
        throw new IllegalStateException("Phiếu nhập không thể hoàn tất");
    }
  }
  public void xoaBanNhap(String id, Connection c) throws SQLException {
    PhieuNhap value = khoa(id, c);
    if (value == null || !TrangThaiPhieuNhap.NHAP.name().equals(value.getTrangThai()))
      throw new IllegalStateException("Chỉ xóa được phiếu nháp");
    try (PreparedStatement s =
             c.prepareStatement("DELETE FROM CHI_TIET_PHIEU_NHAP WHERE ma_phieu_nhap=?")) {
      s.setString(1, id);
      s.executeUpdate();
    }
    try (PreparedStatement s = c.prepareStatement("DELETE FROM PHIEU_NHAP WHERE ma_phieu_nhap=?")) {
      s.setString(1, id);
      s.executeUpdate();
    }
  }
  public String maTiepTheo(Connection c) throws SQLException {
    return Id_DAO.taoMaTiepTheo("PN", c);
  }
  public List<PhieuNhap> timKiem(String tuKhoa, String trangThai, Connection c)
      throws SQLException {
    List<PhieuNhap> out = new ArrayList<PhieuNhap>();
    String q = "SELECT ma_phieu_nhap FROM PHIEU_NHAP WHERE (? IS NULL OR ma_phieu_nhap LIKE ?) AND "
        + "(? IS NULL OR trang_thai=?) ORDER BY thoi_diem_lap DESC";
    try (PreparedStatement s = c.prepareStatement(q)) {
      s.setString(1, tuKhoa);
      s.setString(2, tuKhoa == null ? null : "%" + tuKhoa + "%");
      s.setString(3, trangThai);
      s.setString(4, trangThai);
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) out.add(layTheoMa(r.getString(1), c));
      }
    }
    return out;
  }
}
