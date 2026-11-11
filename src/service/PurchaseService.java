package service;

import connectDB.DBConnection;
import dao.LoTonKhoDAO;
import dao.NhaCungCapDAO;
import dao.SanPhamDAO;
import dao.PhieuNhapDAO;
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

public class PurchaseService {
  private final PhieuNhapDAO phieuNhapDAO = new PhieuNhapDAO();
  private final LoTonKhoDAO loTonKhoDAO = new LoTonKhoDAO();
  private final NhaCungCapDAO nhaCungCapDAO = new NhaCungCapDAO();
  private final SanPhamDAO sanPhamDAO = new SanPhamDAO();
  public void luuNhap(Actor actor, PhieuNhap phieuNhap) throws SQLException {
    Access.manager(actor);
    phieuNhap.setMaNhanVien(actor.getMaNhanVien());
    luuNhap(phieuNhap);
  }
  public String maPhieuNhapTiepTheo(Actor actor) throws SQLException {
    Access.manager(actor);
    try (Connection c = DBConnection.open()) {
      return phieuNhapDAO.nextId(c);
    }
  }
  public List<PhieuNhap> tim(Actor actor, String keyword, String trangThai) throws SQLException {
    Access.manager(actor);
    String filter = Access.blankToNull(trangThai);
    if (filter != null && !Access.is(filter, TrangThaiPhieuNhap.class))
      throw new IllegalArgumentException("Trạng thái phiếu nhập không hợp lệ");
    return tim(Access.blankToNull(keyword), filter);
  }
  public PhieuNhap chiTiet(Actor actor, String maPhieuNhap) throws SQLException {
    Access.manager(actor);
    return chiTiet(maPhieuNhap);
  }
  public void hoanTat(Actor actor, String maPhieuNhap) throws SQLException {
    Access.manager(actor);
    hoanTat(maPhieuNhap);
  }
  public void xoaNhap(Actor actor, String maPhieuNhap) throws SQLException {
    Access.manager(actor);
    try (Connection c = DBConnection.open()) {
      c.setAutoCommit(false);
      try {
        phieuNhapDAO.deleteDraft(maPhieuNhap, c);
        c.commit();
      } catch (RuntimeException | SQLException e) {
        c.rollback();
        throw e;
      } finally {
        c.setAutoCommit(true);
        }
      }
    }
  public void capNhatChiTiet(Actor actor, String maPhieuNhap, List<ChiTietPhieuNhap> chiTiet)
      throws SQLException {
    Access.manager(actor);
    if (chiTiet == null)
      throw new IllegalArgumentException("Thiếu chi tiết nhập");
    PhieuNhap value = chiTiet(actor, maPhieuNhap);
    if (value == null || !TrangThaiPhieuNhap.NHAP.name().equals(value.getTrangThai()))
      throw new IllegalStateException("Chỉ sửa được phiếu nháp");
    value.setChiTiet(chiTiet);
    luuNhap(actor, value);
  }
  private void luuNhap(PhieuNhap phieuNhap) throws SQLException {
    try (Connection c = DBConnection.open()) {
      c.setAutoCommit(false);
      try {
        validateDraft(phieuNhap, c);
        PhieuNhap old = phieuNhapDAO.lock(phieuNhap.getMaPhieuNhap(), c);
        if (old != null && !TrangThaiPhieuNhap.NHAP.name().equals(old.getTrangThai()))
          throw new IllegalStateException("Phiếu nhập đã hoàn tất");
        phieuNhapDAO.saveDraft(phieuNhap, c);
        c.commit();
      } catch (RuntimeException | SQLException e) {
        c.rollback();
        throw e;
      } finally {
        c.setAutoCommit(true);
      }
    }
  }
  private List<PhieuNhap> tim(String keyword, String trangThai) throws SQLException {
    try (Connection c = DBConnection.open()) {
      return phieuNhapDAO.search(keyword, trangThai, c);
    }
  }
  private PhieuNhap chiTiet(String maPhieuNhap) throws SQLException {
    try (Connection c = DBConnection.open()) {
      return phieuNhapDAO.load(maPhieuNhap, c);
    }
  }
  private void hoanTat(String maPhieuNhap) throws SQLException {
    try (Connection c = DBConnection.open()) {
      c.setAutoCommit(false);
      try {
        PhieuNhap p = phieuNhapDAO.lock(maPhieuNhap, c);
        if (p == null || !TrangThaiPhieuNhap.NHAP.name().equals(p.getTrangThai())
            || p.getChiTiet().isEmpty())
          throw new IllegalStateException("Phiếu nhập không hợp lệ");
        validateDraft(p, c);
        LocalDateTime completedAt = LocalDateTime.now();
        for (ChiTietPhieuNhap d : p.getChiTiet())
          loTonKhoDAO.create(p.getMaPhieuNhap(), d.getMaSanPham(), d.getSoLuong(), completedAt, c);
        phieuNhapDAO.complete(maPhieuNhap, completedAt, c);
        c.commit();
      } catch (RuntimeException | SQLException e) {
        c.rollback();
        throw e;
      } finally {
        c.setAutoCommit(true);
      }
    }
  }

  private void validateDraft(PhieuNhap value, Connection c) throws SQLException {
        if (value == null || value.getMaPhieuNhap() == null
            || !ValidationRules.matches(value.getMaPhieuNhap(), ValidationRules.PURCHASE_ID)
            || value.getMaPhieuNhap().trim().length() > 20)
          throw new IllegalArgumentException("Mã phiếu nhập sai định dạng, phải có dạng PN + số (ví dụ: PN001)");
        if (value.getMaNhaCungCap() == null || value.getMaNhaCungCap().trim().isEmpty())
          throw new IllegalArgumentException("Phải chọn nhà cung cấp");
        NhaCungCap supplier = nhaCungCapDAO.find(value.getMaNhaCungCap().trim(), c);
        if (supplier == null || !TrangThaiHoatDong.HOAT_DONG.name().equals(supplier.getTrangThai()))
          throw new IllegalArgumentException("Nhà cung cấp không tồn tại hoặc đã ngừng hoạt động");
        if (value.getChiTiet() == null || value.getChiTiet().isEmpty())
          throw new IllegalArgumentException("Phiếu nhập phải có ít nhất một sản phẩm");
        Set<String> seen = new HashSet<String>();
        for (ChiTietPhieuNhap detail : value.getChiTiet()) {
          if (detail == null || detail.getMaSanPham() == null
              || !seen.add(detail.getMaSanPham().trim()))
            throw new IllegalArgumentException("Sản phẩm nhập bị trống hoặc trùng dòng");
          SanPham product = sanPhamDAO.find(detail.getMaSanPham().trim(), c);
          if (product == null || !TrangThaiSanPham.DANG_KINH_DOANH.name().equals(product.getTrangThai()))
            throw new IllegalArgumentException("Sản phẩm không tồn tại hoặc đã ngừng kinh doanh: "
                + detail.getMaSanPham());
          if (detail.getSoLuong() <= 0 || detail.getDonGiaNhap() == null
              || detail.getDonGiaNhap().signum() <= 0)
            throw new IllegalArgumentException("Số lượng và đơn giá nhập phải lớn hơn 0");
        }
      }
}
