package service;

import connectDB.ConnectDB;
import dao.BaoCao_DAO;
import dao.KhachHang_DAO;
import entity.HangThanhVien;
import entity.KhachHang;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class DichVuBaoCao {
  private final BaoCao_DAO dao = new BaoCao_DAO();
  private final KhachHang_DAO khachHangDAO = new KhachHang_DAO();
  public TongHopBaoCao tongHop(NguoiDung nguoiDung, LocalDate tuNgay, LocalDate denNgay)
      throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    return tongHopNoiBo(tuNgay, denNgay);
  }
  public List<DoanhThuTheoNgay> doanhThuTheoNgay(NguoiDung nguoiDung, LocalDate tuNgay, LocalDate denNgay)
      throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    return doanhThuTheoNgayNoiBo(tuNgay, denNgay);
  }
  public List<DoanhSoSanPham> sanPhamBanChay(NguoiDung nguoiDung, LocalDate tuNgay, LocalDate denNgay)
      throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    return sanPhamBanChayNoiBo(tuNgay, denNgay);
  }
  public List<SanPhamSapHetHang> sanPhamSapHetHang(NguoiDung nguoiDung) throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    try (Connection c = ConnectDB.open()) {
      return dao.sanPhamSapHetHang(c);
    }
  }
  public List<SoLuongTheoHangThanhVien> soLuongTheoHangThanhVien(NguoiDung nguoiDung) throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    try (Connection c = ConnectDB.open()) {
      return dao.soLuongTheoHangThanhVien(c);
    }
  }
  public List<KhachHang> khachHangTheoHang(NguoiDung nguoiDung, String hangThanhVien) throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    if (!KiemTraQuyen.is(hangThanhVien, HangThanhVien.class))
      throw new IllegalArgumentException("Hạng khách hàng không hợp lệ");
    try (Connection c = ConnectDB.open()) {
      return khachHangDAO.timTheoHang(hangThanhVien, c);
    }
  }
  private TongHopBaoCao tongHopNoiBo(LocalDate tuNgay, LocalDate denNgay) throws SQLException {
    kiemTraKhoangNgay(tuNgay, denNgay);
    try (Connection c = ConnectDB.open()) {
      return dao.tongHop(tuNgay, denNgay, c);
    }
  }
  private List<DoanhThuTheoNgay> doanhThuTheoNgayNoiBo(LocalDate tuNgay, LocalDate denNgay) throws SQLException {
    kiemTraKhoangNgay(tuNgay, denNgay);
    try (Connection c = ConnectDB.open()) {
      return dao.doanhThuTheoNgay(tuNgay, denNgay, c);
    }
  }
  private List<DoanhSoSanPham> sanPhamBanChayNoiBo(LocalDate tuNgay, LocalDate denNgay) throws SQLException {
    kiemTraKhoangNgay(tuNgay, denNgay);
    try (Connection c = ConnectDB.open()) {
      return dao.sanPhamBanChay(tuNgay, denNgay, c);
    }
  }
  private void kiemTraKhoangNgay(LocalDate from, LocalDate to) {
    if (from == null || to == null || from.isAfter(to))
      throw new IllegalArgumentException("Khoảng ngày không hợp lệ");
    if (from.plusYears(1).isBefore(to))
      throw new IllegalArgumentException("Khoảng báo cáo không được vượt quá 1 năm");
  }
}
