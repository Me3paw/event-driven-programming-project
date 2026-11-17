package service;

import connectDB.DBConnection;
import dao.KhuyenMaiDAO;
import dao.SanPhamDAO;
import entity.KhuyenMai;
import entity.PhamViKhuyenMai;
import entity.SanPham;
import entity.TrangThaiKhuyenMai;
import entity.TrangThaiSanPham;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

public class PromotionService {
  private final KhuyenMaiDAO dao = new KhuyenMaiDAO();
  private final SanPhamDAO sanPhamDAO = new SanPhamDAO();
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
    capNhatTrangThaiInternal(actor, maKhuyenMai, trangThai);
  }
  public void xoa(Actor actor, String maKhuyenMai) throws SQLException {
    Access.manager(actor);
    xoaInternal(actor, maKhuyenMai);
  }
  public void luu(Actor actor, KhuyenMai value) throws SQLException {
    Access.manager(actor);
    luuInternal(actor, value);
  }
  public String maKhuyenMaiTiepTheo(Actor actor) throws SQLException {
    Access.manager(actor);
    try (Connection c = DBConnection.open()) {
      return dao.nextId(c);
    }
  }
  private void luuInternal(Actor actor, KhuyenMai value) throws SQLException {
    if (value == null || value.getMaKhuyenMai() == null
        || !ValidationRules.matches(value.getMaKhuyenMai(), ValidationRules.PROMOTION_ID)
        || value.getMaKhuyenMai().trim().length() > 20)
      throw new IllegalArgumentException("Mã khuyến mãi sai định dạng, phải có dạng KMxxx (đúng 3 chữ số, ví dụ: KM001)");
    if (value.getTenKhuyenMai() == null || value.getTenKhuyenMai().trim().isEmpty()
        || value.getTenKhuyenMai().trim().length() > 150)
      throw new IllegalArgumentException("Tên khuyến mãi không được để trống hoặc quá dài");
    value.setMaKhuyenMai(value.getMaKhuyenMai().trim());
    value.setTenKhuyenMai(value.getTenKhuyenMai().trim());
    if (!ValidationRules.matches(value.getTenKhuyenMai(), ValidationRules.PROMOTION_NAME))
      throw new IllegalArgumentException("Tên khuyến mãi sai định dạng, phải bắt đầu bằng \"Khuyến mãi\" (ví dụ: Khuyến mãi giảm 10%)");
    if (value.getTyLeGiam() == null || value.getTyLeGiam().signum() <= 0
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
    if (PhamViKhuyenMai.TOAN_DON.name().equals(value.getPhamVi())) {
      if (value.getDonToiThieu() == null || value.getDonToiThieu().signum() <= 0)
        throw new IllegalArgumentException("Khuyến mãi toàn đơn cần giá trị tối thiểu lớn hơn 0");
    } else {
      value.setDonToiThieu(null);
    }
    if (value.getMaSanPham() != null)
      for (String id : value.getMaSanPham())
        if (id == null || id.trim().isEmpty())
          throw new IllegalArgumentException("Mã sản phẩm khuyến mãi không hợp lệ");
    Set<String> normalizedProducts = new LinkedHashSet<String>();
    if (value.getMaSanPham() != null)
      for (String id : value.getMaSanPham())
        if (id != null && !id.trim().isEmpty())
          normalizedProducts.add(id.trim());
    value.setMaSanPham(normalizedProducts);
    try (SessionTransaction transaction = new SessionTransaction(actor, true, false)) {
      Connection c = transaction.connection();
      if (value.getMaSanPham() != null)
        for (String id : value.getMaSanPham()) {
          String productId = id == null ? null : id.trim();
          SanPham product = productId == null ? null : sanPhamDAO.find(productId, c);
          if (product == null || !TrangThaiSanPham.DANG_KINH_DOANH.name().equals(product.getTrangThai()))
            throw new IllegalArgumentException("Sản phẩm khuyến mãi không tồn tại hoặc đã ngừng kinh doanh: " + id);
        }
      try {
        dao.save(value, c);
        transaction.commit();
      } catch (RuntimeException | SQLException e) {
        c.rollback();
        throw e;
      }
    }
  }
  private List<KhuyenMai> tim(String keyword, String trangThai) throws SQLException {
    try (Connection c = DBConnection.open()) {
      return dao.search(keyword, trangThai, c);
    }
  }
  private void capNhatTrangThaiInternal(Actor actor, String maKhuyenMai, String trangThai) throws SQLException {
    if (!Access.is(trangThai, TrangThaiKhuyenMai.class))
      throw new IllegalArgumentException("Trạng thái khuyến mãi không hợp lệ");
    try (SessionTransaction transaction = new SessionTransaction(actor, true, false)) {
      Connection c = transaction.connection();
      dao.updateStatus(maKhuyenMai, trangThai, c);
      transaction.commit();
    }
  }
  private void xoaInternal(Actor actor, String maKhuyenMai) throws SQLException {
    try (SessionTransaction transaction = new SessionTransaction(actor, true, false)) {
      Connection c = transaction.connection();
      try {
        if (dao.referenced(maKhuyenMai, c))
          throw new IllegalStateException("Khuyến mãi đã được hóa đơn tham chiếu");
        dao.delete(maKhuyenMai, c);
        transaction.commit();
      } catch (RuntimeException | SQLException e) {
        c.rollback();
        throw e;
      }
    }
  }
}
