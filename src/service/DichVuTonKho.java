package service;

import connectDB.ConnectDB;
import dao.PhieuDieuChinhTon_DAO;
import dao.LoTonKho_DAO;
import dao.SanPham_DAO;
import entity.SanPham;
import entity.TrangThaiSanPham;
import entity.PhieuDieuChinhTon;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DichVuTonKho {
  private final PhieuDieuChinhTon_DAO phieuDieuChinhDAO = new PhieuDieuChinhTon_DAO();
  private final LoTonKho_DAO loDAO = new LoTonKho_DAO();
  private final SanPham_DAO sanPhamDAO = new SanPham_DAO();
  public List<entity.LoTonKho> tonKho(NguoiDung nguoiDung, String tuKhoa) throws SQLException {
    KiemTraQuyen.actor(nguoiDung);
    return tonKho(KiemTraQuyen.blankToNull(tuKhoa));
  }
  public void giamTonFifo(NguoiDung nguoiDung, String maDieuChinh, String maSanPham, int soLuong,
      String lyDo) throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    if (!QuyTacKiemTra.matches(maDieuChinh, QuyTacKiemTra.ADJUSTMENT_ID)
        || maDieuChinh.trim().length() > 20)
      throw new IllegalArgumentException("Mã phiếu điều chỉnh sai định dạng, phải có dạng DC + số (ví dụ: DC001)");
    if (!QuyTacKiemTra.matches(maSanPham, QuyTacKiemTra.PRODUCT_ID)
        || maSanPham.trim().length() > 20)
      throw new IllegalArgumentException("Mã sản phẩm sai định dạng, phải là mã vạch 12 chữ số bắt đầu bằng 893 (ví dụ: 893000000001)");
    if (KiemTraQuyen.blankToNull(lyDo) == null || lyDo.trim().length() > 255)
      throw new IllegalArgumentException("Phải nhập lý do điều chỉnh");
    giamTonFifo(nguoiDung, maDieuChinh, nguoiDung.getMaNhanVien(), maSanPham, soLuong, lyDo.trim());
  }
  public String maDieuChinhTiepTheo(NguoiDung nguoiDung) throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    try (Connection c = ConnectDB.open()) {
      return phieuDieuChinhDAO.maTiepTheo(c);
    }
  }
  public List<PhieuDieuChinhTon> timPhieuDieuChinh(NguoiDung nguoiDung, String tuKhoa) throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    try (Connection c = ConnectDB.open()) {
      return phieuDieuChinhDAO.timKiem(KiemTraQuyen.blankToNull(tuKhoa), c);
    }
  }
  public PhieuDieuChinhTon chiTietPhieuDieuChinh(NguoiDung nguoiDung, String id) throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    try (Connection c = ConnectDB.open()) {
      PhieuDieuChinhTon value = phieuDieuChinhDAO.chiTiet(id, c);
      if (value == null)
        throw new IllegalArgumentException("Không tìm thấy phiếu điều chỉnh");
      return value;
    }
  }
  private List<entity.LoTonKho> tonKho(String tuKhoa) throws SQLException {
    try (Connection c = ConnectDB.open()) {
      return loDAO.timKiem(tuKhoa, c);
    }
  }
  private void giamTonFifo(NguoiDung nguoiDung, String maDieuChinh, String maNhanVien, String maSanPham, int soLuong,
      String lyDo) throws SQLException {
    if (soLuong <= 0)
      throw new IllegalArgumentException("Số lượng giảm không hợp lệ");
    try (GiaoDichPhien transaction = new GiaoDichPhien(nguoiDung, true, false)) {
      Connection c = transaction.connection();
      try {
        SanPham sanPham = sanPhamDAO.timTheoMa(maSanPham, c);
        if (sanPham == null || !TrangThaiSanPham.DANG_KINH_DOANH.name().equals(sanPham.getTrangThai()))
          throw new IllegalArgumentException("Sản phẩm không tồn tại hoặc đã ngừng kinh doanh");
        if (phieuDieuChinhDAO.chiTiet(maDieuChinh, c) != null)
          throw new IllegalArgumentException("Mã phiếu điều chỉnh đã tồn tại");
        Map<Long, Integer> phanBo = new LinkedHashMap<Long, Integer>();
        int con = soLuong;
        List<entity.LoTonKho> lots = loDAO.khoaTheoFifo(maSanPham, c);
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
          loDAO.thayDoiSoLuong(e.getKey(), -e.getValue(), c);
        phieuDieuChinhDAO.luu(phieuDieuChinh, c);
        transaction.commit();
      } catch (RuntimeException | SQLException e) {
        c.rollback();
        throw e;
      }
    }
  }
}
