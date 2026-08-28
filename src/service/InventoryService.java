package service;

import connectDB.DBConnection;
import dao.DieuChinhTonDAO;
import dao.LoTonKhoDAO;
import entity.DieuChinhTon;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InventoryService {
  private final DieuChinhTonDAO dieuChinhDAO = new DieuChinhTonDAO();
  private final LoTonKhoDAO loDAO = new LoTonKhoDAO();
  public List<entity.LoTonKho> tonKho(Actor actor, String keyword) throws SQLException {
    Access.actor(actor);
    return tonKho(Access.blankToNull(keyword));
  }
  public void giamTonFifo(Actor actor, String maDieuChinh, String maSanPham, int soLuong,
      String lyDo) throws SQLException {
    Access.manager(actor);
    if (Access.blankToNull(lyDo) == null)
      throw new IllegalArgumentException("Phải nhập lý do điều chỉnh");
    giamTonFifo(maDieuChinh, actor.getMaNhanVien(), maSanPham, soLuong, lyDo.trim());
  }
  public List<DieuChinhTon> timDieuChinh(Actor actor, String keyword) throws SQLException {
    Access.manager(actor);
    try (Connection c = DBConnection.open()) {
      return dieuChinhDAO.search(Access.blankToNull(keyword), c);
    }
  }
  public DieuChinhTon chiTietDieuChinh(Actor actor, String id) throws SQLException {
    Access.manager(actor);
    try (Connection c = DBConnection.open()) {
      DieuChinhTon value = dieuChinhDAO.detail(id, c);
      if (value == null)
        throw new IllegalArgumentException("Không tìm thấy điều chỉnh");
      return value;
    }
  }
  private List<entity.LoTonKho> tonKho(String keyword) throws SQLException {
    try (Connection c = DBConnection.open()) {
      return loDAO.search(keyword, c);
    }
  }
  private void giamTonFifo(String maDieuChinh, String maNhanVien, String maSanPham, int soLuong,
      String lyDo) throws SQLException {
    if (soLuong <= 0)
      throw new IllegalArgumentException("Số lượng giảm không hợp lệ");
    try (Connection c = DBConnection.open()) {
      c.setAutoCommit(false);
      try {
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
        DieuChinhTon dieuChinh = new DieuChinhTon();
        dieuChinh.setMaDieuChinh(maDieuChinh);
        dieuChinh.setMaNhanVien(maNhanVien);
        dieuChinh.setThoiDiemLap(LocalDateTime.now());
        dieuChinh.setLyDo(lyDo);
        dieuChinh.setSoLuongGiamTheoLo(phanBo);
        for (Map.Entry<Long, Integer> e : phanBo.entrySet())
          loDAO.change(e.getKey(), -e.getValue(), c);
        dieuChinhDAO.save(dieuChinh, c);
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
