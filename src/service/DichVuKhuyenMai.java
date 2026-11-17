package service;

import connectDB.ConnectDB;
import dao.KhuyenMai_DAO;
import dao.SanPham_DAO;
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

public class DichVuKhuyenMai {
  private final KhuyenMai_DAO dao = new KhuyenMai_DAO();
  private final SanPham_DAO sanPhamDAO = new SanPham_DAO();
  public List<KhuyenMai> tim(NguoiDung nguoiDung, String tuKhoa, String trangThai) throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    String filter = KiemTraQuyen.blankToNull(trangThai);
    if (filter != null && !KiemTraQuyen.is(filter, TrangThaiKhuyenMai.class))
      throw new IllegalArgumentException("Trạng thái khuyến mãi không hợp lệ");
    return tim(KiemTraQuyen.blankToNull(tuKhoa), filter);
  }
  public void capNhatTrangThai(NguoiDung nguoiDung, String maKhuyenMai, String trangThai)
      throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    xuLyCapNhatTrangThai(nguoiDung, maKhuyenMai, trangThai);
  }
  public void xoa(NguoiDung nguoiDung, String maKhuyenMai) throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    xuLyXoa(nguoiDung, maKhuyenMai);
  }
  public void luu(NguoiDung nguoiDung, KhuyenMai value) throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    xuLyLuu(nguoiDung, value);
  }
  public String maKhuyenMaiTiepTheo(NguoiDung nguoiDung) throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    try (Connection c = ConnectDB.open()) {
      return dao.maTiepTheo(c);
    }
  }
  private void xuLyLuu(NguoiDung nguoiDung, KhuyenMai value) throws SQLException {
    if (value == null || value.getMaKhuyenMai() == null
        || !QuyTacKiemTra.matches(value.getMaKhuyenMai(), QuyTacKiemTra.PROMOTION_ID)
        || value.getMaKhuyenMai().trim().length() > 20)
      throw new IllegalArgumentException("Mã khuyến mãi sai định dạng, phải có dạng KMxxx (đúng 3 chữ số, ví dụ: KM001)");
    if (value.getTenKhuyenMai() == null || value.getTenKhuyenMai().trim().isEmpty()
        || value.getTenKhuyenMai().trim().length() > 150)
      throw new IllegalArgumentException("Tên khuyến mãi không được để trống hoặc quá dài");
    value.setMaKhuyenMai(value.getMaKhuyenMai().trim());
    value.setTenKhuyenMai(value.getTenKhuyenMai().trim());
    if (!QuyTacKiemTra.matches(value.getTenKhuyenMai(), QuyTacKiemTra.PROMOTION_NAME))
      throw new IllegalArgumentException("Tên khuyến mãi sai định dạng, phải bắt đầu bằng \"Khuyến mãi\" (ví dụ: Khuyến mãi giảm 10%)");
    if (value.getTyLeGiam() == null || value.getTyLeGiam().signum() <= 0
        || value.getTyLeGiam().compareTo(new java.math.BigDecimal("100")) > 0)
      throw new IllegalArgumentException("Tỷ lệ giảm không hợp lệ");
    if (!KiemTraQuyen.is(value.getPhamVi(), PhamViKhuyenMai.class))
      throw new IllegalArgumentException("Phạm vi khuyến mãi không hợp lệ");
    if (value.getBatDau() == null || value.getKetThuc() == null
        || !value.getBatDau().isBefore(value.getKetThuc()))
      throw new IllegalArgumentException("Thời gian khuyến mãi không hợp lệ");
    if (!KiemTraQuyen.is(value.getTrangThai(), TrangThaiKhuyenMai.class))
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
    try (GiaoDichPhien transaction = new GiaoDichPhien(nguoiDung, true, false)) {
      Connection c = transaction.connection();
      if (value.getMaSanPham() != null)
        for (String id : value.getMaSanPham()) {
          String maSanPham = id == null ? null : id.trim();
          SanPham sanPham = maSanPham == null ? null : sanPhamDAO.timTheoMa(maSanPham, c);
          if (sanPham == null || !TrangThaiSanPham.DANG_KINH_DOANH.name().equals(sanPham.getTrangThai()))
            throw new IllegalArgumentException("Sản phẩm khuyến mãi không tồn tại hoặc đã ngừng kinh doanh: " + id);
        }
      try {
        dao.luu(value, c);
        transaction.commit();
      } catch (RuntimeException | SQLException e) {
        c.rollback();
        throw e;
      }
    }
  }
  private List<KhuyenMai> tim(String tuKhoa, String trangThai) throws SQLException {
    try (Connection c = ConnectDB.open()) {
      return dao.timKiem(tuKhoa, trangThai, c);
    }
  }
  private void xuLyCapNhatTrangThai(NguoiDung nguoiDung, String maKhuyenMai, String trangThai) throws SQLException {
    if (!KiemTraQuyen.is(trangThai, TrangThaiKhuyenMai.class))
      throw new IllegalArgumentException("Trạng thái khuyến mãi không hợp lệ");
    try (GiaoDichPhien transaction = new GiaoDichPhien(nguoiDung, true, false)) {
      Connection c = transaction.connection();
      dao.capNhatTrangThai(maKhuyenMai, trangThai, c);
      transaction.commit();
    }
  }
  private void xuLyXoa(NguoiDung nguoiDung, String maKhuyenMai) throws SQLException {
    try (GiaoDichPhien transaction = new GiaoDichPhien(nguoiDung, true, false)) {
      Connection c = transaction.connection();
      try {
        if (dao.daDuocThamChieu(maKhuyenMai, c))
          throw new IllegalStateException("Khuyến mãi đã được hóa đơn tham chiếu");
        dao.xoa(maKhuyenMai, c);
        transaction.commit();
      } catch (RuntimeException | SQLException e) {
        c.rollback();
        throw e;
      }
    }
  }
}
