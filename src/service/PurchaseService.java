package service;

import connectDB.DBConnection;
import dao.LoTonKhoDAO;
import dao.PhieuNhapDAO;
import entity.ChiTietPhieuNhap;
import entity.PhieuNhap;
import entity.TrangThaiPhieuNhap;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class PurchaseService {
  private final PhieuNhapDAO phieuNhapDAO = new PhieuNhapDAO();
  private final LoTonKhoDAO loTonKhoDAO = new LoTonKhoDAO();
  public void luuNhap(Actor actor, PhieuNhap phieuNhap) throws SQLException {
    Access.manager(actor);
    phieuNhap.setMaNhanVien(actor.getMaNhanVien());
    luuNhap(phieuNhap);
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
}
