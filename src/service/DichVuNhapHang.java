package service;

import connectDB.ConnectDB;
import dao.LoTonKho_DAO;
import dao.NhaCungCap_DAO;
import dao.SanPham_DAO;
import dao.PhieuNhap_DAO;
import entity.ChiTietPhieuNhap;
import entity.PhieuNhap;
import entity.TrangThaiPhieuNhap;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import entity.NhaCungCap;
import entity.SanPham;
import entity.TrangThaiHoatDong;
import entity.TrangThaiSanPham;

public class DichVuNhapHang {
  private final PhieuNhap_DAO phieuNhapDAO = new PhieuNhap_DAO();
  private final LoTonKho_DAO loTonKhoDAO = new LoTonKho_DAO();
  private final NhaCungCap_DAO nhaCungCapDAO = new NhaCungCap_DAO();
  private final SanPham_DAO sanPhamDAO = new SanPham_DAO();
  public void luuNhap(NguoiDung nguoiDung, PhieuNhap phieuNhap) throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    phieuNhap.setMaNhanVien(nguoiDung.getMaNhanVien());
    xuLyLuuNhap(nguoiDung, phieuNhap);
  }
  public String maPhieuNhapTiepTheo(NguoiDung nguoiDung) throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    try (Connection c = ConnectDB.open()) {
      return phieuNhapDAO.maTiepTheo(c);
    }
  }
  public List<PhieuNhap> tim(NguoiDung nguoiDung, String tuKhoa, String trangThai) throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    String filter = KiemTraQuyen.blankToNull(trangThai);
    if (filter != null && !KiemTraQuyen.is(filter, TrangThaiPhieuNhap.class))
      throw new IllegalArgumentException("Trạng thái phiếu nhập không hợp lệ");
    return tim(KiemTraQuyen.blankToNull(tuKhoa), filter);
  }
  public PhieuNhap chiTiet(NguoiDung nguoiDung, String maPhieuNhap) throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    return chiTiet(maPhieuNhap);
  }
  public void hoanTat(NguoiDung nguoiDung, String maPhieuNhap) throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    xuLyHoanTat(nguoiDung, maPhieuNhap);
  }
  public void xoaNhap(NguoiDung nguoiDung, String maPhieuNhap) throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    try (Connection c = ConnectDB.open()) {
      c.setAutoCommit(false);
      try {
        KiemTraQuyen.lock(c, "POS_AUTHORIZATION", "Shared");
        KiemTraQuyen.validate(nguoiDung, true, c);
        phieuNhapDAO.xoaBanNhap(maPhieuNhap, c);
        c.commit();
      } catch (RuntimeException | SQLException e) {
        c.rollback();
        throw e;
      } finally {
        c.setAutoCommit(true);
        }
      }
    }
  public void capNhatChiTiet(NguoiDung nguoiDung, String maPhieuNhap, List<ChiTietPhieuNhap> chiTiet)
      throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    if (chiTiet == null)
      throw new IllegalArgumentException("Thiếu chi tiết nhập");
    PhieuNhap value = chiTiet(nguoiDung, maPhieuNhap);
    if (value == null || !TrangThaiPhieuNhap.NHAP.name().equals(value.getTrangThai()))
      throw new IllegalStateException("Chỉ sửa được phiếu nháp");
    value.setChiTiet(chiTiet);
    xuLyLuuNhap(nguoiDung, value);
  }
  private void xuLyLuuNhap(NguoiDung nguoiDung, PhieuNhap phieuNhap) throws SQLException {
    try (GiaoDichPhien transaction = new GiaoDichPhien(nguoiDung, true, false)) {
      Connection c = transaction.connection();
      try {
        kiemTraBanNhap(phieuNhap, c);
        PhieuNhap old = phieuNhapDAO.khoa(phieuNhap.getMaPhieuNhap(), c);
        if (old != null && !TrangThaiPhieuNhap.NHAP.name().equals(old.getTrangThai()))
          throw new IllegalStateException("Phiếu nhập đã hoàn tất");
        phieuNhapDAO.luuBanNhap(phieuNhap, c);
        transaction.commit();
      } catch (RuntimeException | SQLException e) {
        c.rollback();
        throw e;
      }
    }
  }
  private List<PhieuNhap> tim(String tuKhoa, String trangThai) throws SQLException {
    try (Connection c = ConnectDB.open()) {
      return phieuNhapDAO.timKiem(tuKhoa, trangThai, c);
    }
  }
  private PhieuNhap chiTiet(String maPhieuNhap) throws SQLException {
    try (Connection c = ConnectDB.open()) {
      return phieuNhapDAO.layTheoMa(maPhieuNhap, c);
    }
  }
  private void xuLyHoanTat(NguoiDung nguoiDung, String maPhieuNhap) throws SQLException {
    try (GiaoDichPhien transaction = new GiaoDichPhien(nguoiDung, true, false)) {
      Connection c = transaction.connection();
      try {
        PhieuNhap p = phieuNhapDAO.khoa(maPhieuNhap, c);
        if (p == null || !TrangThaiPhieuNhap.NHAP.name().equals(p.getTrangThai())
            || p.getChiTiet().isEmpty())
          throw new IllegalStateException("Phiếu nhập không hợp lệ");
        kiemTraBanNhap(p, c);
        LocalDateTime completedAt = LocalDateTime.now();
        for (ChiTietPhieuNhap d : p.getChiTiet())
          loTonKhoDAO.tao(p.getMaPhieuNhap(), d.getMaSanPham(), d.getSoLuong(), completedAt, c);
        phieuNhapDAO.hoanTat(maPhieuNhap, completedAt, c);
        transaction.commit();
      } catch (RuntimeException | SQLException e) {
        c.rollback();
        throw e;
      }
    }
  }

  private void kiemTraBanNhap(PhieuNhap value, Connection c) throws SQLException {
        if (value == null || value.getMaPhieuNhap() == null
            || !QuyTacKiemTra.matches(value.getMaPhieuNhap(), QuyTacKiemTra.PURCHASE_ID)
            || value.getMaPhieuNhap().trim().length() > 20)
          throw new IllegalArgumentException("Mã phiếu nhập sai định dạng, phải có dạng PN + số (ví dụ: PN001)");
        if (value.getMaNhaCungCap() == null || value.getMaNhaCungCap().trim().isEmpty())
          throw new IllegalArgumentException("Phải chọn nhà cung cấp");
        NhaCungCap nhaCungCap = nhaCungCapDAO.timTheoMa(value.getMaNhaCungCap().trim(), c);
        if (nhaCungCap == null || !TrangThaiHoatDong.HOAT_DONG.name().equals(nhaCungCap.getTrangThai()))
          throw new IllegalArgumentException("Nhà cung cấp không tồn tại hoặc đã ngừng hoạt động");
        if (value.getChiTiet() == null || value.getChiTiet().isEmpty())
          throw new IllegalArgumentException("Phiếu nhập phải có ít nhất một sản phẩm");
        Set<String> seen = new HashSet<String>();
        for (ChiTietPhieuNhap detail : value.getChiTiet()) {
          if (detail == null || detail.getMaSanPham() == null
              || !seen.add(detail.getMaSanPham().trim()))
            throw new IllegalArgumentException("Sản phẩm nhập bị trống hoặc trùng dòng");
          SanPham sanPham = sanPhamDAO.timTheoMa(detail.getMaSanPham().trim(), c);
          if (sanPham == null || !TrangThaiSanPham.DANG_KINH_DOANH.name().equals(sanPham.getTrangThai()))
            throw new IllegalArgumentException("Sản phẩm không tồn tại hoặc đã ngừng kinh doanh: "
                + detail.getMaSanPham());
          if (detail.getSoLuong() <= 0 || detail.getDonGiaNhap() == null
              || detail.getDonGiaNhap().signum() < 0)
            throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0 và đơn giá nhập không được âm");
        }
      }
}
