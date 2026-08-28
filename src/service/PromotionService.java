package service;

import connectDB.DBConnection;
import dao.KhuyenMaiDAO;
import entity.KhuyenMai;
import entity.PhamViKhuyenMai;
import entity.TrangThaiKhuyenMai;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class PromotionService {
  private final KhuyenMaiDAO dao = new KhuyenMaiDAO();
  public List<KhuyenMai> tim(Actor actor, String keyword, String trangThai) throws SQLException {
    Access.manager(actor);
    String filter = Access.blankToNull(trangThai);
    if (filter != null && !Access.is(filter, TrangThaiKhuyenMai.class))
      throw new IllegalArgumentException("Trạng thái khuyến mãi không hợp lệ");
    return tim(Access.blankToNull(keyword), filter);
  }
  public void capNhatTrangThai(Actor actor, String maKhuyenMai, String trangThai)
      throws SQLException {
    Access.manager(actor);
    capNhatTrangThai(maKhuyenMai, trangThai);
  }
  public void xoa(Actor actor, String maKhuyenMai) throws SQLException {
    Access.manager(actor);
    xoa(maKhuyenMai);
  }
  public void luu(Actor actor, KhuyenMai value) throws SQLException {
    Access.manager(actor);
    luu(value);
  }
  private void luu(KhuyenMai value) throws SQLException {
    if (value == null || value.getTyLeGiam() == null || value.getTyLeGiam().signum() <= 0
        || value.getTyLeGiam().compareTo(new java.math.BigDecimal("100")) > 0)
      throw new IllegalArgumentException("Tỷ lệ giảm không hợp lệ");
    if (!Access.is(value.getPhamVi(), PhamViKhuyenMai.class))
      throw new IllegalArgumentException("Phạm vi khuyến mãi không hợp lệ");
    if (value.getBatDau() == null || value.getKetThuc() == null
        || !value.getBatDau().isBefore(value.getKetThuc()))
      throw new IllegalArgumentException("Thời gian khuyến mãi không hợp lệ");
    if (!Access.is(value.getTrangThai(), TrangThaiKhuyenMai.class))
      throw new IllegalArgumentException("Trạng thái khuyến mãi không hợp lệ");
    if (PhamViKhuyenMai.THEO_SAN_PHAM.name().equals(value.getPhamVi())
        && (value.getMaSanPham() == null || value.getMaSanPham().isEmpty()))
      throw new IllegalArgumentException("Khuyến mãi theo sản phẩm cần sản phẩm");
    if (value.getMaSanPham() != null)
      for (String id : value.getMaSanPham())
        if (id == null || id.trim().isEmpty())
          throw new IllegalArgumentException("Mã sản phẩm khuyến mãi không hợp lệ");
    try (Connection c = DBConnection.open()) {
      c.setAutoCommit(false);
      try {
        dao.save(value, c);
        c.commit();
      } catch (RuntimeException | SQLException e) {
        c.rollback();
        throw e;
      } finally {
        c.setAutoCommit(true);
      }
    }
  }
  private List<KhuyenMai> tim(String keyword, String trangThai) throws SQLException {
    try (Connection c = DBConnection.open()) {
      return dao.search(keyword, trangThai, c);
    }
  }
  private void capNhatTrangThai(String maKhuyenMai, String trangThai) throws SQLException {
    if (!Access.is(trangThai, TrangThaiKhuyenMai.class))
      throw new IllegalArgumentException("Trạng thái khuyến mãi không hợp lệ");
    try (Connection c = DBConnection.open()) {
      dao.updateStatus(maKhuyenMai, trangThai, c);
    }
  }
  private void xoa(String maKhuyenMai) throws SQLException {
    try (Connection c = DBConnection.open()) {
      c.setAutoCommit(false);
      try {
        if (dao.referenced(maKhuyenMai, c))
          throw new IllegalStateException("Khuyến mãi đã được hóa đơn tham chiếu");
        dao.delete(maKhuyenMai, c);
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
