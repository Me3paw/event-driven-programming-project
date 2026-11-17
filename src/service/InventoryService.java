package service;

import connectDB.DBConnection;
import dao.PhieuDieuChinhTonDAO;
import dao.LoTonKhoDAO;
import dao.SanPhamDAO;
import entity.SanPham;
import entity.TrangThaiSanPham;
import entity.PhieuDieuChinhTon;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InventoryService {
  private final PhieuDieuChinhTonDAO phieuDieuChinhDAO = new PhieuDieuChinhTonDAO();
  private final LoTonKhoDAO loDAO = new LoTonKhoDAO();
  private final SanPhamDAO sanPhamDAO = new SanPhamDAO();
  public List<entity.LoTonKho> tonKho(Actor actor, String keyword) throws SQLException {
    Access.actor(actor);
    return tonKho(Access.blankToNull(keyword));
  }
  public void giamTonFifo(Actor actor, String maDieuChinh, String maSanPham, int soLuong,
      String lyDo) throws SQLException {
    Access.manager(actor);
    if (!ValidationRules.matches(maDieuChinh, ValidationRules.ADJUSTMENT_ID)
        || maDieuChinh.trim().length() > 20)
      throw new IllegalArgumentException("Mã phiếu điều chỉnh sai định dạng, phải có dạng DC + số (ví dụ: DC001)");
    if (!ValidationRules.matches(maSanPham, ValidationRules.PRODUCT_ID)
        || maSanPham.trim().length() > 20)
      throw new IllegalArgumentException("Mã sản phẩm sai định dạng, phải là mã vạch 12 chữ số bắt đầu bằng 893 (ví dụ: 893000000001)");
    if (Access.blankToNull(lyDo) == null || lyDo.trim().length() > 255)
      throw new IllegalArgumentException("Phải nhập lý do điều chỉnh");
    giamTonFifo(actor, maDieuChinh, actor.getMaNhanVien(), maSanPham, soLuong, lyDo.trim());
  }
  public String maDieuChinhTiepTheo(Actor actor) throws SQLException {
    Access.manager(actor);
    try (Connection c = DBConnection.open()) {
      return phieuDieuChinhDAO.nextId(c);
    }
  }
  public List<PhieuDieuChinhTon> timPhieuDieuChinh(Actor actor, String keyword) throws SQLException {
    Access.manager(actor);
    try (Connection c = DBConnection.open()) {
      return phieuDieuChinhDAO.search(Access.blankToNull(keyword), c);
    }
  }
  public PhieuDieuChinhTon chiTietPhieuDieuChinh(Actor actor, String id) throws SQLException {
    Access.manager(actor);
    try (Connection c = DBConnection.open()) {
      PhieuDieuChinhTon value = phieuDieuChinhDAO.detail(id, c);
      if (value == null)
        throw new IllegalArgumentException("Không tìm thấy phiếu điều chỉnh");
      return value;
    }
  }
  private List<entity.LoTonKho> tonKho(String keyword) throws SQLException {
    try (Connection c = DBConnection.open()) {
      return loDAO.search(keyword, c);
    }
  }
  private void giamTonFifo(Actor actor, String maDieuChinh, String maNhanVien, String maSanPham, int soLuong,
      String lyDo) throws SQLException {
    if (soLuong <= 0)
      throw new IllegalArgumentException("Số lượng giảm không hợp lệ");
    try (SessionTransaction transaction = new SessionTransaction(actor, true, false)) {
      Connection c = transaction.connection();
      try {
        SanPham product = sanPhamDAO.find(maSanPham, c);
        if (product == null || !TrangThaiSanPham.DANG_KINH_DOANH.name().equals(product.getTrangThai()))
          throw new IllegalArgumentException("Sản phẩm không tồn tại hoặc đã ngừng kinh doanh");
        if (phieuDieuChinhDAO.detail(maDieuChinh, c) != null)
          throw new IllegalArgumentException("Mã phiếu điều chỉnh đã tồn tại");
        Map<Long, Integer> phanBo = new LinkedHashMap<Long, Integer>();
        int con = soLuong;
        List<entity.LoTonKho> lots = loDAO.lockFifo(maSanPham, c);
        for (entity.LoTonKho lo : lots) {
          int lay = Math.min(con, lo.getSoLuongCon());
          if (lay > 0) {
            phanBo.put(lo.getMaLo(), lay);
            con -= lay;
          }
        }
        if (con != 0)
          throw new IllegalStateException("Tồn kho không đủ");
        PhieuDieuChinhTon phieuDieuChinh = new PhieuDieuChinhTon();
        phieuDieuChinh.setMaDieuChinh(maDieuChinh);
        phieuDieuChinh.setMaNhanVien(maNhanVien);
        phieuDieuChinh.setThoiDiemLap(LocalDateTime.now());
        phieuDieuChinh.setLyDo(lyDo);
        phieuDieuChinh.setSoLuongGiamTheoLo(phanBo);
        for (Map.Entry<Long, Integer> e : phanBo.entrySet())
          loDAO.change(e.getKey(), -e.getValue(), c);
        phieuDieuChinhDAO.save(phieuDieuChinh, c);
        transaction.commit();
      } catch (RuntimeException | SQLException e) {
        c.rollback();
        throw e;
      }
    }
  }
}
