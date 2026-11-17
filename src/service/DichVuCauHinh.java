package service;

import connectDB.ConnectDB;
import dao.CauHinhCuaHang_DAO;
import entity.CauHinhCuaHang;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

public class DichVuCauHinh {
  private final CauHinhCuaHang_DAO dao = new CauHinhCuaHang_DAO();
  public CauHinhCuaHang lay(NguoiDung nguoiDung) throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    return lay();
  }
  public CauHinhCuaHang layChoHoaDon(NguoiDung nguoiDung) throws SQLException {
    KiemTraQuyen.actor(nguoiDung);
    return lay();
  }
  public void luu(NguoiDung nguoiDung, CauHinhCuaHang value) throws SQLException {
    KiemTraQuyen.manager(nguoiDung);
    xuLyLuu(nguoiDung, value);
  }
  private CauHinhCuaHang lay() throws SQLException {
    try (Connection c = ConnectDB.open()) {
      return dao.layCauHinh(c);
    }
  }
  private void xuLyLuu(NguoiDung nguoiDung, CauHinhCuaHang value) throws SQLException {
    if (value == null)
      throw new IllegalArgumentException("Thiếu cấu hình cửa hàng");
    if (rong(value.getTenCuaHang()) || value.getTenCuaHang().trim().length() > 100)
      throw new IllegalArgumentException("Tên cửa hàng phải từ 1 đến 100 ký tự");
    if (rong(value.getDiaChi()) || value.getDiaChi().trim().length() > 255)
      throw new IllegalArgumentException("Địa chỉ phải từ 1 đến 255 ký tự");
    if (rong(value.getSoDienThoai())
        || !value.getSoDienThoai().trim().matches(QuyTacKiemTra.STORE_PHONE))
      throw new IllegalArgumentException("Số điện thoại không hợp lệ");
    BigDecimal rate = value.getTyLeVat();
    if (rate == null || rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) > 0)
      throw new IllegalArgumentException("VAT không hợp lệ");
    try (GiaoDichPhien transaction = new GiaoDichPhien(nguoiDung, true, false)) {
      Connection c = transaction.connection();
      dao.luu(value, c);
      transaction.commit();
    }
  }
  private boolean rong(String value) {
    return value == null || value.trim().isEmpty();
  }
}
